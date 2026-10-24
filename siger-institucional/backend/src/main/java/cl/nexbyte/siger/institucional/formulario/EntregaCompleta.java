package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Contrato a84d2e7: conserva también campos desconocidos del documento firmado. */
public record EntregaCompleta(JsonNode comunicacion,String sha256,String huellaProcedencia,ComprobanteDigital custodia) {
    public record Adjunto(UUID id,String nombre,long tamanio,String sha256,String tipo,String estado){}
    public static JsonMapper lector(){return JsonMapper.builder(tools.jackson.core.json.JsonFactory.builder()
        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .streamReadConstraints(tools.jackson.core.StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(20000).maxNumberLength(30).build()).build()).build();}
    public UUID id(){return UUID.fromString(comunicacion.path("id").asString());}
    public String tipo(){return comunicacion.path("tipo").asString();}
    public String canal(){return tipo().equals("NNA")?"FORMULARIO_WEB_NNA":"FORMULARIO_WEB_PUBLICO";}
    public String versionAck(){return tipo().equals("NNA")?"1.1":"1.0";}
    public List<Adjunto> adjuntos(){var list=new ArrayList<Adjunto>();for(var a:comunicacion.path("adjuntos"))list.add(lector().treeToValue(a,Adjunto.class));return List.copyOf(list);}
    public void validar(Map<String,PublicKey> trust,JsonMapper mapper){
        try{
            if(comunicacion==null||!comunicacion.isObject()||!Set.of("1.0","1.1").contains(comunicacion.path("version").asString())
                ||!Set.of("ADULTO_CU","ADULTO_DECLARADO","NNA").contains(tipo())||!comunicacion.path("aceptaCanalDigital").isBoolean()
                ||!comunicacion.path("datos").isObject()||!comunicacion.path("datos").path("campos").isObject()
                ||!comunicacion.path("personas").isArray()||comunicacion.path("personas").size()>30||!comunicacion.path("adjuntos").isArray()
                ||!comunicacion.path("datos").path("personas").isArray()||!comunicacion.path("datos").path("instituciones").isArray()
                ||!comunicacion.path("datos").path("adjuntos").isArray())throw new IllegalArgumentException();
            UUID.fromString(comunicacion.path("recorridoId").asString());var received=Instant.parse(comunicacion.path("recibidaEn").asString());
            if(!tipo().equals("NNA")&&!comunicacion.path("aceptaCanalDigital").asBoolean())throw new IllegalArgumentException();
            var sources=mapper.createObjectNode();var persons=new HashSet<String>();int presenters=0;
            for(var p:comunicacion.path("personas")){
                String person=p.path("id").asString();if(person.isBlank()||!persons.add(person)||!p.path("atributos").isObject()||!p.path("discrepancias").isObject()||!p.path("autenticada").isBoolean())throw new IllegalArgumentException();
                boolean presenter="PRESENTADOR".equals(p.path("rol").asString());if(presenter)presenters++;
                if(!Set.of("PRESENTADOR","AFECTADO_NNA","ADULTO_RELACIONADO","PERSONA_SENALADA").contains(p.path("rol").asString()))throw new IllegalArgumentException();
                if(p.path("autenticada").asBoolean())for(var key:List.of("runNumero","runDv","nombres","apellidos"))if(!p.path("atributos").path(key).path("fuente").asString().equals("CLAVE_UNICA_USERINFO")||!p.path("atributos").path(key).path("valor").isString()||p.path("atributos").path(key).path("valor").asString().isBlank())throw new IllegalArgumentException();
                if(p.path("autenticada").asBoolean()!=(presenter&&tipo().equals("ADULTO_CU")))throw new IllegalArgumentException();
                if(p.path("autenticada").asBoolean())for(var key:List.of("runNumero","runDv"))if(!p.path(key).equals(p.path("atributos").path(key).path("valor")))throw new IllegalArgumentException();
                for(var group:List.of(p.path("atributos"),p.path("discrepancias")))for(var entry:group.properties()){
                    var a=entry.getValue();if(!Set.of("CLAVE_UNICA_USERINFO","SRCEI","DECLARACION_CIUDADANA","SIN_DATO").contains(a.path("fuente").asString()))throw new IllegalArgumentException();
                    if(a.path("fuente").asString().equals("CLAVE_UNICA_USERINFO")&&!p.path("autenticada").asBoolean())throw new IllegalArgumentException();
                    if(!a.path("nivel").isString()||a.path("nivel").asString().isBlank()||a.path("nivel").asString().equals("IDENTIDAD_AUTENTICADA")&&!a.path("fuente").asString().equals("CLAVE_UNICA_USERINFO"))throw new IllegalArgumentException();
                    for(var key:List.of("valor","codigo"))if(!a.has(key)||!(a.path(key).isNull()||a.path(key).isString()))throw new IllegalArgumentException();
                    UUID.fromString(a.path("evidencia").asString());Instant.parse(a.path("observadoEn").asString());
                }
                sources.set(person,mapper.createArrayNode().add(p.path("atributos")).add(p.path("discrepancias")));
            }
            if(presenters!=1)throw new IllegalArgumentException();
            for(var entry:comunicacion.path("datos").path("campos").properties())if(!entry.getValue().isString())throw new IllegalArgumentException();
            var detailsIds=new HashSet<String>();
            for(var detail:comunicacion.path("datos").path("personas")){
                var person=detail.path("personaId").asString();if(!persons.contains(person)||!detailsIds.add(person)||!detail.path("detalles").isObject())throw new IllegalArgumentException();
                for(var v:detail.path("detalles").properties())if(!v.getValue().isString())throw new IllegalArgumentException();
            }
            for(var institution:comunicacion.path("datos").path("instituciones")){if(!institution.isObject())throw new IllegalArgumentException();for(var v:institution.properties())if(!(v.getValue().isString()||v.getValue().isNull()))throw new IllegalArgumentException();}
            var ids=new HashSet<UUID>();long total=0;var files=adjuntos();if(files.size()>12)throw new IllegalArgumentException();
            for(var a:files){if(a.id()==null||!ids.add(a.id())||a.nombre()==null||a.nombre().isBlank()||a.nombre().length()>180||a.nombre().codePoints().anyMatch(Character::isISOControl)
                ||a.tamanio()<1||a.tamanio()>250L*1024*1024||a.sha256()==null||!a.sha256().matches("[a-f0-9]{64}")||a.tipo()==null||!a.tipo().matches("[a-z0-9.+-]+/[a-z0-9.+-]+")||!"VALIDADO".equals(a.estado()))throw new IllegalArgumentException();total=Math.addExact(total,a.tamanio());}
            if(total>250L*1024*1024||comunicacion.path("version").asString().equals("1.0")&&(files.size()>5||total>25L*1024*1024||files.stream().anyMatch(a->a.tamanio()>10L*1024*1024)))throw new IllegalArgumentException();
            var refs=new HashSet<UUID>();for(var a:comunicacion.path("datos").path("adjuntos"))if(!refs.add(UUID.fromString(a.asString())))throw new IllegalArgumentException();if(!refs.equals(ids))throw new IllegalArgumentException();
            if(!huella("CONTENIDO",comunicacion,mapper).equals(sha256)||!huella("PROCEDENCIA",sources,mapper).equals(huellaProcedencia)||custodia==null)throw new IllegalArgumentException();
            var proof=custodia.verificar(trust,mapper);
            if(!proof.id().equals(id())||!proof.recibidaEn().equals(received)||!proof.avisoVersion().equals(comunicacion.path("avisoVersion").asString())||!proof.huellaContenido().equals(sha256)||!proof.huellaProcedencia().equals(huellaProcedencia))throw new IllegalArgumentException();
        }catch(RuntimeException e){throw new IllegalArgumentException("Documento ciudadano completo inválido");}
    }
    public static String huella(String dominio,JsonNode value,JsonMapper mapper){try{var d=MessageDigest.getInstance("SHA-256");d.update(("SIGER_FORMULARIO_COMPLETO_1\0"+dominio+"\0").getBytes(java.nio.charset.StandardCharsets.UTF_8));return HexFormat.of().formatHex(d.digest(mapper.writeValueAsBytes(orden(value,mapper))));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    private static JsonNode orden(JsonNode n,JsonMapper m){if(n.isObject()){var r=m.createObjectNode();var keys=new TreeSet<String>();n.properties().forEach(e->keys.add(e.getKey()));keys.forEach(k->r.set(k,orden(n.get(k),m)));return r;}if(n.isArray()){var r=m.createArrayNode();for(var e:n)r.add(orden(e,m));return r;}return n;}
    public void validarRegistro(RegistroInstitucional ack,Map<String,PublicKey> trust,JsonMapper mapper){var r=ack.registro();if(!r.equals(ack.comprobante().verificar(trust,mapper))||!r.version().equals(versionAck())||!r.canalOrigen().equals(canal())||!r.operacionPublicaId().equals(id())||!r.sha256().equals(sha256)||!r.huellaProcedencia().equals(huellaProcedencia))throw new IllegalArgumentException("ACK no corresponde");}
    @Override public String toString(){return "EntregaCompleta[protegida]";}
}
