package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Raíz de autoridad separada de los roles administrados por TI. Sin confianza explícita no admite importaciones. */
public final class AutoridadesInstitucionales {
    public record Vinculo(String actor,String persona,Instant desde,Instant hasta,String autenticacion){@Override public String toString(){return "VinculoAutoridad[reservado]";}}
    public record Mandato(String id,String persona,String funcion,String rol,String alcance,String unidad,String sede,
                          Instant desde,Instant hasta,boolean aprobar,boolean revocar,String referencia,String sha256){}
    public record Registro(String version,long revision,Instant emitida,Instant expira,List<Vinculo> vinculos,List<Mandato> mandatos){@Override public String toString(){return "RegistroAutoridad[reservado]";}}
    public record Sobre(String keyId,String payload,String firma){@Override public String toString(){return "SobreAutoridad[reservado]";}}
    private static final byte[] DOMAIN="SIGER_AUTORIDAD_1\0".getBytes(StandardCharsets.UTF_8);
    private final BaseJdbc db;
    private final IdentidadInstitucional ids;
    private final JsonMapper json;
    private final Clock clock;
    private final Map<String,PublicKey> trust;
    public static JsonMapper lectorPrivado(){return JsonMapper.builder(tools.jackson.core.json.JsonFactory.builder()
        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .streamReadConstraints(tools.jackson.core.StreamReadConstraints.builder().maxNestingDepth(12).maxStringLength(44000).maxNumberLength(30).build()).build()).enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();}
    public AutoridadesInstitucionales(BaseJdbc db,IdentidadInstitucional ids,JsonMapper json,Clock clock,Map<String,PublicKey> trust){
        this.db=db;this.ids=ids;this.json=json;this.clock=clock;this.trust=Map.copyOf(trust);
    }
    static String str(Map<String,Object> r,String key){return r.get(key)==null?null:r.get(key).toString();}
    static long num(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
    static Map<String,Object> salida(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
    static void texto(String value,int max){if(value==null||value.isBlank()||value.length()>max||value.chars().anyMatch(Character::isISOControl))throw new FalloTecnico(400);}
    static void codigo(String value){if(value==null||!value.matches("[A-Za-z0-9_.-]{1,64}"))throw new FalloTecnico(400);}
    static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static byte[] contenidoFirmado(byte[] payload){byte[] result=new byte[DOMAIN.length+payload.length];System.arraycopy(DOMAIN,0,result,0,DOMAIN.length);System.arraycopy(payload,0,result,DOMAIN.length,payload.length);return result;}
    void guard(){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);}
    long revision(){return db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);}
    void evento(String actor,String resource,String action,String code,Object snapshot){
        db.sql.update("INSERT INTO authority_event(actor_id,resource_id,action_code,result_code,at_ms,snapshot_doc) VALUES(?,?,?,?,?,?)",actor,resource,action,code,clock.millis(),snapshot==null?null:json.writeValueAsString(snapshot));
    }
    /** Solo canal privado/configuración; no hay endpoint HTTP que configure confianza o importe designaciones. */
    public long incorporar(Sobre sobre){
        if(sobre==null||sobre.payload()==null||sobre.payload().length()>44000||sobre.firma()==null||sobre.firma().length()>128)throw new FalloTecnico(400);
        codigo(sobre.keyId());var key=trust.get(sobre.keyId());if(key==null)throw new FalloTecnico(403,"FIRMANTE_AUTORIDAD_NO_CONFIABLE");
        byte[] bytes;
        Registro registro;
        try{
            bytes=Base64.getDecoder().decode(sobre.payload());if(bytes.length>32768)throw new FalloTecnico(400);
            var verifier=Signature.getInstance("Ed25519");verifier.initVerify(key);verifier.update(contenidoFirmado(bytes));
            if(!verifier.verify(Base64.getDecoder().decode(sobre.firma())))throw new FalloTecnico(403,"FIRMA_AUTORIDAD_INVALIDA");
            registro=lectorPrivado().readValue(bytes,Registro.class);
        }catch(FalloTecnico e){throw e;}catch(GeneralSecurityException|RuntimeException e){throw new FalloTecnico(400);}
        validar(registro);String hash=sha(bytes);
        return db.transaction(()->{
            guard();long current=revision();
            if(registro.revision()==current){var old=db.sql.queryForMap("SELECT payload_sha256 FROM authority_batch WHERE revision=?",current);if(hash.equals(old.get("payload_sha256")))return current;throw new FalloTecnico(409,"REGISTRO_AUTORIDAD_CONFLICTIVO");}
            if(registro.revision()!=current+1)throw new FalloTecnico(409,"VERSION_OBSOLETA");
            for(var v:registro.vinculos()){
                ids.operador(v.actor());
                if(db.sql.queryForObject("SELECT COUNT(*) FROM authority_binding WHERE actor_id=? AND person_id<>?",Long.class,v.actor(),v.persona())>0)throw new FalloTecnico(409,"IDENTIDAD_NO_REASIGNABLE");
            }
            db.sql.update("INSERT INTO authority_batch VALUES(?,?,?,?,?,?,?,?)",registro.revision(),hash,sobre.keyId(),sobre.payload(),sobre.firma(),registro.emitida().toEpochMilli(),registro.expira().toEpochMilli(),clock.millis());
            for(var v:registro.vinculos())db.sql.update("INSERT INTO authority_binding VALUES(?,?,?,?,?)",registro.revision(),v.actor(),v.persona(),v.desde().toEpochMilli(),v.hasta().toEpochMilli());
            for(var m:registro.mandatos()){
                ids.validarConcesion(new IdentidadInstitucional.Otorgar(m.rol(),m.alcance(),m.unidad(),m.sede(),m.desde(),m.hasta()));
                if(registro.vinculos().stream().noneMatch(v->v.persona().equals(m.persona())))throw new FalloTecnico(400);
                db.sql.update("INSERT INTO authority_mandate VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",registro.revision(),m.id(),m.persona(),m.funcion(),m.rol(),m.alcance(),m.unidad(),m.sede(),m.desde().toEpochMilli(),m.hasta().toEpochMilli(),m.aprobar(),m.revocar(),m.referencia(),m.sha256());
            }
            db.sql.update("UPDATE authority_registry SET revision=? WHERE id=1",registro.revision());evento(null,Long.toString(registro.revision()),"REGISTRO_IMPORTADO","FIRMA_VERIFICADA",Map.of("sha256",hash,"keyId",sobre.keyId()));return registro.revision();
        });
    }
    private void validar(Registro r){
        if(r==null||!"1.0".equals(r.version())||r.revision()<1||r.emitida()==null||r.expira()==null||r.emitida().isAfter(clock.instant())||!r.expira().isAfter(clock.instant())||r.vinculos()==null||r.mandatos()==null||r.vinculos().size()>128||r.mandatos().size()>32)throw new FalloTecnico(400);
        intervalo(r.emitida(),r.expira(),r.expira());Set<String> actors=new HashSet<>(),mandates=new HashSet<>();
        for(var v:r.vinculos()){
            if(v==null)throw new FalloTecnico(400);codigo(v.actor());codigo(v.persona());intervalo(v.desde(),v.hasta(),r.expira());if(v.autenticacion()==null||!v.autenticacion().matches("[a-f0-9]{64}")||!actors.add(v.actor()))throw new FalloTecnico(400);
        }
        for(var m:r.mandatos()){
            if(m==null)throw new FalloTecnico(400);codigo(m.id());codigo(m.persona());texto(m.funcion(),80);texto(m.referencia(),180);
            if(m.sha256()==null||!m.sha256().matches("[a-f0-9]{64}")||!mandates.add(m.id())||!m.aprobar()&&!m.revocar())throw new FalloTecnico(400);
            intervalo(m.desde(),m.hasta(),r.expira());
        }
    }
    static void intervalo(Instant desde,Instant hasta,Instant limite){try{if(desde==null||hasta==null||!hasta.isAfter(desde)||hasta.toEpochMilli()<=desde.toEpochMilli()||hasta.isAfter(limite))throw new FalloTecnico(400);}catch(ArithmeticException e){throw new FalloTecnico(400);}}
    void actor(String actor){ids.exigirCuentaHabilitada(actor);persona(actor);}
    private Registro vigente(){
        var rows=db.sql.queryForList("SELECT r.* FROM authority_batch r JOIN authority_registry c ON c.revision=r.revision AND c.id=1");
        try{
            if(rows.size()!=1)throw new IllegalArgumentException();var row=rows.getFirst();var key=trust.get(str(row,"key_id"));if(key==null)throw new IllegalArgumentException();
            byte[] bytes=Base64.getDecoder().decode(str(row,"signed_payload"));if(bytes.length>32768||!sha(bytes).equals(str(row,"payload_sha256")))throw new IllegalArgumentException();
            var verifier=Signature.getInstance("Ed25519");verifier.initVerify(key);verifier.update(contenidoFirmado(bytes));if(!verifier.verify(Base64.getDecoder().decode(str(row,"signature_text"))))throw new IllegalArgumentException();
            var r=lectorPrivado().readValue(bytes,Registro.class);validar(r);if(r.revision()!=num(row,"revision"))throw new IllegalArgumentException();return r;
        }catch(GeneralSecurityException|RuntimeException e){throw new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA");}
    }
    private String persona(String actor,Registro r){return r.vinculos().stream().filter(v->v.actor().equals(actor)&&!v.desde().isAfter(clock.instant())&&v.hasta().isAfter(clock.instant())).map(Vinculo::persona).findFirst().orElseThrow(()->new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA"));}
    public String persona(String actor){
        return persona(actor,vigente());
    }
    List<Map<String,Object>> mandatos(String actor){
        var r=vigente();String person=persona(actor,r);
        // TI administra contraseñas locales, pero no puede heredar la autoridad del funcionario al restablecerla.
        var credential=db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,actor);
        String fingerprint=sha(credential.getBytes(StandardCharsets.UTF_8));
        if(r.mandatos().stream().anyMatch(m->m.persona().equals(person))&&r.vinculos().stream().noneMatch(v->v.actor().equals(actor)&&v.autenticacion().equals(fingerprint)))throw new FalloTecnico(409,"ACREDITACION_CREDENCIAL_OBSOLETA");
        return r.mandatos().stream().filter(m->m.persona().equals(person)&&!m.desde().isAfter(clock.instant())&&m.hasta().isAfter(clock.instant())).sorted(Comparator.comparing(Mandato::id)).map(m->{
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",m.id());row.put("batch_revision",r.revision());row.put("person_id",m.persona());row.put("function_code",m.funcion());row.put("role_code",m.rol());row.put("scope_code",m.alcance());row.put("unit_code",m.unidad());row.put("site_id",m.sede());row.put("valid_from",m.desde().toEpochMilli());row.put("valid_until",m.hasta().toEpochMilli());row.put("can_approve",m.aprobar());row.put("can_revoke",m.revocar());row.put("basis_ref",m.referencia());row.put("basis_sha256",m.sha256());return row;
        }).toList();
    }
    Map<String,Object> mandato(String actor,IdentidadInstitucional.Otorgar grant,boolean revoke){
        return mandatos(actor).stream().filter(m->Boolean.TRUE.equals(m.get(revoke?"can_revoke":"can_approve"))&&cubre(m,grant)).findFirst().orElseThrow(()->new FalloTecnico(403,"AUTORIDAD_NO_ACREDITADA"));
    }
    private boolean cubre(Map<String,Object> m,IdentidadInstitucional.Otorgar grant){
        return Objects.equals(m.get("role_code"),grant.rol())&&Objects.equals(m.get("scope_code"),grant.alcance())&&Objects.equals(m.get("unit_code"),grant.unidad())&&Objects.equals(m.get("site_id"),grant.sede())
            &&num(m,"valid_from")<=Math.max(clock.millis(),grant.desde().toEpochMilli())&&num(m,"valid_until")>=grant.hasta().toEpochMilli();
    }
    void comprobarDecision(String approver,long batch,String mandateId,IdentidadInstitucional.Otorgar grant){
        try{actor(approver);var m=mandato(approver,grant,false);
            if(batch!=revision()||!mandateId.equals(m.get("id")))throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");
        }catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");throw e;}
    }
    public Map<String,Object> contexto(String actor){
        ids.exigirCuentaHabilitada(actor);String state="ACREDITADA";List<Map<String,Object>> mandates=List.of();
        try{mandates=mandatos(actor);}catch(FalloTecnico e){if(e.status!=409)throw e;state="NO_ACREDITADA";}
        return Map.of("revisionRegistro",revision(),"identidad",state,"mandatos",mandates.stream().map(AutoridadesInstitucionales::salida).toList());
    }
}
