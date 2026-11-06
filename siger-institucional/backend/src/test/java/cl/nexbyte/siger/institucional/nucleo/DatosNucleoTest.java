package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.integracion.SobreComunicacion;
import cl.nexbyte.siger.contrato.DeclaracionNna;
import java.nio.file.*;import java.time.*;import java.util.*;import java.security.*;
import tools.jackson.databind.json.JsonMapper;

final class DatosNucleoTest implements AutoCloseable {
    final BaseJdbc db;final CatalogoLaboratorio catalogo=new CatalogoLaboratorio();final JsonMapper mapper=JsonMapper.builder().build();
    final RecepcionInstitucionalJdbc recepcion;final NucleoInstitucional core;final EntregaVerificada historica;
    final KeyPair publicKeys,institutionKeys;
    static final List<String> LIMPIEZA=List.of("mail_attempt","notification_outbox","institution_notification","task_command","task_history","institution_task","request_command","request_history","request_followup","institution_request","workflow_event","notification_source","mail_campaign","distribution_event","communication_origin","core_management_period","core_reopen_evidence","core_reopen","core_link_evaluation","core_transfer","core_complexity","core_command","core_event","core_notice","core_link","core_review","core_action","core_assignment","core_case","core_requirement","core_destination","core_manual","core_communication","communication_registry","institutional_inbox","control_audit","external_operation","operational_event");
    DatosNucleoTest(Path dir)throws Exception{this(dir,null);}
    DatosNucleoTest(Path dir,String isolatedUrl)throws Exception{
        String supplied=System.getenv("ROBUST_DB_URL");
        String url=supplied==null?"jdbc:h2:file:"+dir.resolve("nucleo").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":supplied;
        if(isolatedUrl!=null){if(supplied!=null||!isolatedUrl.matches("jdbc:mysql://127\\.0\\.0\\.1:[5-6][0-9]{4}/siger_u15_test_[a-f0-9]{32}_(?:area|materia|contenido|compartir|u2)\\?.*"))throw new IllegalStateException("Destino aislado U16 requerido");url=isolatedUrl;}
        if(supplied!=null)cl.nexbyte.siger.institucional.robustez.DestinoJdbcPruebas.validar(url);
        try(var migration=new BaseJdbc(url,isolatedUrl!=null?"root":supplied==null?"sa":System.getenv("ROBUST_DB_MIGRATION_USER"),isolatedUrl!=null?System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"):supplied==null?"":System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){migration.migrate("institucional");}
        db=new BaseJdbc(url,isolatedUrl!=null?"root":supplied==null?"sa":System.getenv("ROBUST_DB_USER"),isolatedUrl!=null?System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"):supplied==null?"":System.getenv("ROBUST_DB_PASSWORD"),12);
        if(supplied!=null)limpiar();catalogo.instalar(db);
        db.sql.update("UPDATE operation_control SET revision=0,accept_new=FALSE,pull_enabled=FALSE,contingency='SOLO_CONSULTA'");new ControlesOperacion(db).cambiar(0,true,true,"NORMAL","PRUEBA_LOCAL");
        publicKeys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();institutionKeys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var trust=new HashMap<String,PublicKey>();trust.put("nucleo-test",publicKeys.getPublic());
        var fixtures=mapper.readTree(getClass().getResourceAsStream("/contratos/receipt-public-test.json"));fixtures.properties().forEach(e->trust.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));
        recepcion=new RecepcionInstitucionalJdbc(db,mapper,trust,"institution-test",institutionKeys.getPrivate(),Map.of("institution-test",institutionKeys.getPublic()),"COM",6,Clock.systemUTC());
        core=new NucleoInstitucional(db,catalogo,recepcion,mapper,Clock.systemUTC());
        historica=mapper.readValue(getClass().getResourceAsStream("/contratos/entrega-v2.json"),EntregaVerificada.class);
    }
    void limpiar(){for(var t:List.of("institution_assignment_receipt","institution_assignment_current","institution_case_closure_request","institution_case_closure","institution_reopen","institution_case_derivation","institution_req_transfer","institution_flow_history","institution_collaboration","institution_case","institution_req","institution_sol"))db.sql.update("DELETE FROM "+t);db.sql.update("UPDATE case_evidence_assessment SET supersedes_seq=NULL");for(var t:List.of("document_case_consent","document_disclosure","document_content_history","document_content_policy","case_evidence_assessment","communication_pertinence_target","communication_pertinence_history","communication_pertinence","authority_requirement_principal","document_event","document_page","document_representation","document_command","management_document_link","institution_document_access","institution_document","matter_access_event","case_matter_history","case_matter","staff_assignment_matter","instruction_matter","authority_matter_mandate","matter_regional_site","institution_matter","case_area_history","case_area","destination_area","staff_assignment_area","instruction_area","authority_area_mandate","institution_area","authority_command","authority_event","approved_assignment","access_instruction","authority_mandate","authority_binding","authority_batch"))db.sql.update("DELETE FROM "+t);db.sql.update("UPDATE authority_registry SET revision=0 WHERE id=1");for(var table:LIMPIEZA){if(table.equals("core_reopen"))db.sql.update("UPDATE core_reopen SET supersedes_id=NULL");if(table.equals("core_case"))db.sql.update("UPDATE core_case SET origin_case_id=NULL");db.sql.update("DELETE FROM "+table);}}
    CatalogoLaboratorio.Operador actor(String name){return catalogo.operador(name);}
    UUID web(){return recepcion.guardar(historica).registro().comunicacionId();}
    UUID nuevaWeb(){
        var id=UUID.randomUUID();var now=Instant.now();var datos=new DeclaracionNna("Nombre sintético compuesto",null,"Apellido sintético compuesto",null,null,null,null,null,"Región declarada sintética");
        var body=SobreComunicacion.crearNna(id,now,datos,"Relato sintético del núcleo","laboratorio-v1");var attrs=EntregaVerificada.procedenciaNna(body);
        var content=new ComprobanteDigital.Contenido("1.0",id,now,"CUSTODIA_PUBLICA",body.sha256(),EntregaVerificada.huella(attrs),"FORMULARIO_PUBLICO_DDN","laboratorio-v1");
        return recepcion.guardar(new EntregaVerificada("2.1",body,attrs,ComprobanteDigital.firmar(content,"nucleo-test",publicKeys.getPrivate(),mapper))).registro().comunicacionId();
    }
    void ddl(java.util.function.Consumer<org.springframework.jdbc.core.JdbcTemplate> work){
        if(System.getenv("ROBUST_DB_URL")==null){work.accept(db.sql);return;}
        try(var ddl=new BaseJdbc(System.getenv("ROBUST_DB_URL"),System.getenv("ROBUST_DB_MIGRATION_USER"),System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){work.accept(ddl.sql);}
    }
    public void close(){if(System.getenv("ROBUST_DB_URL")!=null)limpiar();ConcesionFixture.cerrar(db);db.close();}
}


