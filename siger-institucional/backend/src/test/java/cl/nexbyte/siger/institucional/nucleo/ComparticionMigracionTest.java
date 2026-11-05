package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.*;import java.time.*;import java.util.*;import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class ComparticionMigracionTest {
 @TempDir Path temp;
 @Test void actualiza155YConsultaExigeConsentimientoExplicito()throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");if(mysql!=null&&!mysql.matches("jdbc:mysql://127\\.0\\.0\\.1:[5-6][0-9]{4}/siger_u15_test_[a-f0-9]{32}"))throw new IllegalStateException("MySQL descartable requerido");
  String url=mysql==null?"jdbc:h2:file:"+temp.resolve("nucleo").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":mysql+"_compartir?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";
  try(var before=new BaseJdbc(url,mysql==null?"sa":"root",mysql==null?"":System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"),2)){Flyway.configure().dataSource(before.sql.getDataSource()).locations("classpath:db/institucional/"+(mysql==null?"h2":"mysql")).target("15.5").cleanDisabled(true).load().migrate();}
  try(var x=new ContenidoFixture(temp,Clock.systemUTC(),mysql==null?null:url)){
   UUID a=x.m.caso(),b=x.f.caso("AREA_B",x.f.headA,x.f.profB),doc=x.cargar(a).id();var head=x.f.a(x.f.headA);
   x.docs.vincular(head,UUID.randomUUID(),doc,a,b,null);assertTrue(x.docs.listar(x.f.a(x.f.profB),b).isEmpty());x.ordinario(a,doc,0);
   var body=x.publicacion(0,true,false,"ACTIVA");var key=UUID.randomUUID();var result=x.c().compartir(head,key,a,doc,b,body);assertEquals(result,x.c().compartir(head,key,a,doc,b,body));
   assertNotNull(x.docs.referencia(x.f.a(x.f.profB),b,doc));assertEquals(404,assertThrows(FalloTecnico.class,()->x.docs.referencia(x.f.a(x.f.profB),b,doc,"DESCARGA")).status);
   assertEquals(404,assertThrows(FalloTecnico.class,()->x.docs.listar(x.f.a(x.m.upr),b)).status);
   x.m.clasificar(a,0,"MAT_UPR",true);assertTrue(x.docs.listar(x.f.a(x.f.profB),b).isEmpty());
   assertEquals(1,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM document_case_consent WHERE document_id=?",Long.class,doc.toString()));
   assertEquals(1,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM document_content_history WHERE document_id=? AND action_code='CONTEXTO_CAMBIADO'",Long.class,doc.toString()));
  }
 }
 @Test void ddlH2MySqlIguales()throws Exception{try(var h=getClass().getResourceAsStream("/db/institucional/h2/V15_6__consulta_documental_por_caso.sql");var m=getClass().getResourceAsStream("/db/institucional/mysql/V15_6__consulta_documental_por_caso.sql")){assertNotNull(h);assertNotNull(m);assertArrayEquals(h.readAllBytes(),m.readAllBytes());}}
}
