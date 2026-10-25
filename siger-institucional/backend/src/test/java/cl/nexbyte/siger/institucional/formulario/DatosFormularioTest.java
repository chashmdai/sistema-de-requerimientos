package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.nucleo.*;
import java.nio.file.*;import java.security.*;import java.time.*;import java.util.*;import tools.jackson.databind.json.JsonMapper;

final class DatosFormularioTest implements AutoCloseable {
    final Path dir;BaseJdbc db;ArchivoInstitucional files;RecepcionInstitucionalJdbc receiver;NucleoInstitucional core;
    final CatalogoLaboratorio catalog=new CatalogoLaboratorio();final JsonMapper mapper=EntregaCompleta.lector();
    final KeyPair publicKeys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair(),institutionKeys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    final Map<String,PublicKey> trust=Map.of("public-test",publicKeys.getPublic()),own=Map.of("institution-test",institutionKeys.getPublic());
    Instant now=Instant.parse("2026-10-24T20:00:00Z");final Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}};
    final String url,user,password;
    static final List<String> TABLES=List.of("mail_attempt","notification_outbox","institution_notification","task_command","task_history","institution_task","workflow_event","notification_source","mail_campaign","distribution_event","communication_origin","document_event","document_page","document_representation","document_command","management_document_link","institution_document_access","institution_document","core_management_period","core_reopen_evidence","core_reopen","core_link_evaluation","core_transfer","core_complexity","core_command","core_event","core_notice","core_link","core_review","core_action","core_assignment","core_case","core_requirement","core_destination","core_manual","core_communication","communication_registry","institutional_inbox","control_audit","external_operation","operational_event");
    DatosFormularioTest(Path dir)throws Exception{
        this.dir=dir;String supplied=System.getenv("ROBUST_DB_URL");url=supplied==null?"jdbc:h2:file:"+dir.resolve("db").toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":supplied;
        if(supplied!=null&&!url.startsWith("jdbc:mysql://127.0.0.1:13306/robust_inst_test?"))throw new IllegalStateException("Solo base de pruebas aislada");user=supplied==null?"sa":System.getenv("ROBUST_DB_USER");password=supplied==null?"":System.getenv("ROBUST_DB_PASSWORD");
        try(var migration=new BaseJdbc(url,supplied==null?user:System.getenv("ROBUST_DB_MIGRATION_USER"),supplied==null?password:System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){migration.migrate("institucional");}
        open();if(supplied!=null)clean();catalog.instalar(db);db.sql.update("UPDATE operation_control SET revision=0,accept_new=TRUE,pull_enabled=TRUE,contingency='NORMAL'");
    }
    void open()throws Exception{db=new BaseJdbc(url,user,password,4);files=new ArchivoInstitucional(dir.resolve("archivo"));receiver=new RecepcionInstitucionalJdbc(db,mapper,trust,"institution-test",institutionKeys.getPrivate(),own,"COM",6,clock);core=new NucleoInstitucional(db,catalog,receiver,mapper,clock);}
    void restart()throws Exception{files.close();db.close();open();}
    void clean(){for(var t:TABLES){if(t.equals("core_reopen"))db.sql.update("UPDATE core_reopen SET supersedes_id=NULL");if(t.equals("core_case"))db.sql.update("UPDATE core_case SET origin_case_id=NULL");db.sql.update("DELETE FROM "+t);}}
    void ddl(String sql){if(System.getenv("ROBUST_DB_URL")==null){db.sql.execute(sql);return;}try(var admin=new BaseJdbc(url,System.getenv("ROBUST_DB_MIGRATION_USER"),System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){admin.sql.execute(sql);}}
    EntregaCompleta delivery(String version,String type,List<EntregaCompleta.Adjunto> attachments,boolean run,int persons){
        var c=mapper.createObjectNode();c.put("version",version);c.put("id",UUID.randomUUID().toString());c.put("recorridoId",UUID.randomUUID().toString());c.put("tipo",type);c.put("recibidaEn",now.toString());c.put("avisoVersion","SINTETICO_1");c.put("aceptaCanalDigital",!type.equals("NNA"));
        var data=c.putObject("datos");var fields=data.putObject("campos");fields.put("motivo","consulta");fields.put(type.equals("NNA")?"nna-story":"consulta-general-detalle","R".repeat(5000));fields.put("campo-futuro-opcional","valor íntegro");data.putArray("personas");data.putArray("instituciones");var ids=data.putArray("adjuntos");var descriptors=c.putArray("adjuntos");for(var a:attachments){ids.add(a.id().toString());descriptors.add(mapper.valueToTree(a));}
        var people=c.putArray("personas");for(int i=0;i<persons;i++){
            var p=people.addObject();p.put("id",i==0?(type.equals("NNA")?"presentador-nna":"presentador-adulto"):"relacionado-"+i);p.put("rol",i==0?"PRESENTADOR":"ADULTO_RELACIONADO");p.put("autenticada",i==0&&type.equals("ADULTO_CU"));p.put("modulo11",run);if(run){p.put("runNumero","12345678");p.put("runDv","5");}else{p.putNull("runNumero");p.putNull("runDv");}p.put("estadoConsulta","NO_HABILITADO");p.putNull("consultadaEn");p.putNull("consultaRef");p.putObject("discrepancias");var attrs=p.putObject("atributos");
            String source=i==0&&type.equals("ADULTO_CU")?"CLAVE_UNICA_USERINFO":"DECLARACION_CIUDADANA";
            var keys=source.startsWith("CLAVE")?List.of("runNumero","runDv","nombres","apellidos"):List.of("primerNombre","segundoNombre","primerApellido","segundoApellido");
            for(var k:keys){var a=attrs.putObject(k);a.put("valor",k.equals("runNumero")?"12345678":k.equals("runDv")?"5":persons>1?"N".repeat(500):"NOMBRE SINTETICO");a.putNull("codigo");a.put("fuente",source);a.put("nivel",source.startsWith("CLAVE")?"IDENTIDAD_AUTENTICADA":"DECLARADO_NO_CONTRASTADO");a.put("observadoEn",now.toString());a.put("evidencia",UUID.randomUUID().toString());}
        }return sign(c);
    }
    EntregaCompleta sign(tools.jackson.databind.JsonNode c){var sources=mapper.createObjectNode();for(var p:c.path("personas"))sources.set(p.path("id").asString(),mapper.createArrayNode().add(p.path("atributos")).add(p.path("discrepancias")));String hash=EntregaCompleta.huella("CONTENIDO",c,mapper),provenance=EntregaCompleta.huella("PROCEDENCIA",sources,mapper);var proof=new ComprobanteDigital.Contenido("1.0",UUID.fromString(c.path("id").asString()),Instant.parse(c.path("recibidaEn").asString()),"CUSTODIA_PUBLICA",hash,provenance,"FORMULARIO_PUBLICO_DDN",c.path("avisoVersion").asString());return new EntregaCompleta(c,hash,provenance,ComprobanteDigital.firmar(proof,"public-test",publicKeys.getPrivate(),mapper));}
    public void close()throws Exception{if(System.getenv("ROBUST_DB_URL")!=null)clean();files.close();db.close();}
}
