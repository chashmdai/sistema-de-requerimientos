package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.*;import java.util.*;import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class ContinuidadMigracionTest {
 @TempDir Path temp;
 @Test void actualiza157SinInferirPrincipalAcuseNiReescribirCierre(){
  String mysql=System.getProperty("siger.test.autoridad.mysql");if(mysql!=null&&!mysql.matches("jdbc:mysql://127\\.0\\.0\\.1:[5-6][0-9]{4}/siger_u15_test_[a-f0-9]{32}"))throw new IllegalStateException("MySQL nuevo y descartable requerido");String vendor=mysql==null?"h2":"mysql";
  String url=mysql==null?"jdbc:h2:file:"+temp.resolve("historico").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE":mysql+"_continuidad?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";
  try(var db=new BaseJdbc(url,mysql==null?"sa":"root",mysql==null?"":System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"),2)){
   Flyway.configure().dataSource(db.sql.getDataSource()).locations("classpath:db/institucional/"+vendor).target("15.7").cleanDisabled(true).load().migrate();new CatalogoLaboratorio().instalar(db);
   String com=UUID.randomUUID().toString(),dest=UUID.randomUUID().toString(),req=UUID.randomUUID().toString(),cas=UUID.randomUUID().toString();String head="demo.jefatura.upr.a",pro="demo.profesional.upr.a";
   // Representación de historia ya persistida, no un camino de alta ni una aprobación ficticia.
   db.sql.update("INSERT INTO communication_registry(comunicacion_id,source,operacion_id,registrada_ms,canal,content_hash,provenance_hash,folio_prefix,folio_width) VALUES(?,'REGISTRO_FUNCIONARIO',NULL,1,'MANUAL',?,?,'COM',6)",com,"a".repeat(64),"b".repeat(64));db.sql.update("INSERT INTO core_communication(id,state,revision) VALUES(?,'ABIERTA',8)",com);
   db.sql.update("INSERT INTO core_destination(id,communication_id,unit_code,site_id,evaluator_id,state,revision,created_ms,created_by) VALUES(?,?,'UPR','DEMO_SEDE_A',?,'EVALUANDO',2,1,?)",dest,com,head,head);db.sql.update("INSERT INTO core_requirement(id,communication_id,state,revision,created_ms,created_by,ended_ms) VALUES(?,?,'TERMINADO',7,1,?,9)",req,com,head);
   db.sql.update("INSERT INTO core_case(id,communication_id,requirement_id,destination_id,responsible_id,state,revision,created_ms,created_by,closed_ms,closed_by,closure_mode) VALUES(?,?,?,?,?,'CERRADO',6,1,?,9,?,'JEFATURA')",cas,com,req,dest,pro,head,head);db.sql.update("INSERT INTO core_assignment VALUES(?,?,?,NULL,?,1)",UUID.randomUUID().toString(),cas,pro,head);
   var before=new LinkedHashMap<String,List<Map<String,Object>>>();for(String table:List.of("communication_registry","core_communication","core_requirement","core_case","core_destination","core_assignment"))before.put(table,db.sql.queryForList("SELECT * FROM "+table));
   db.migrate("institucional");db.migrate("institucional");for(var entry:before.entrySet())assertEquals(entry.getValue(),db.sql.queryForList("SELECT * FROM "+entry.getKey()));
   for(String table:List.of("institution_collaboration_end","institution_req","institution_case","institution_req_transfer","institution_req_attention","institution_case_derivation","institution_reopen","institution_assignment_current","institution_assignment_receipt","institution_case_closure"))assertEquals(0,db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class),table);
  }
 }
 @Test void ddlH2MySqlIdentico()throws Exception{for(String script:List.of("V15_8__continuidad_institucional.sql","V15_9__ambito_atencion_requerimiento.sql","V15_11__termino_colaboracion.sql"))try(var h=getClass().getResourceAsStream("/db/institucional/h2/"+script);var m=getClass().getResourceAsStream("/db/institucional/mysql/"+script)){assertArrayEquals(Objects.requireNonNull(h).readAllBytes(),Objects.requireNonNull(m).readAllBytes());}}
}
