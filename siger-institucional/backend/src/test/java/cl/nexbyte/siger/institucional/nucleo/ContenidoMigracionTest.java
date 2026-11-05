package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.*;import java.time.*;import java.util.*;import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class ContenidoMigracionTest {
 @TempDir Path temp;
 @Test void actualiza154YPruebaPertinenciaYProteccion()throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");if(mysql!=null&&!mysql.matches("jdbc:mysql://127\\.0\\.0\\.1:[5-6][0-9]{4}/siger_u15_test_[a-f0-9]{32}"))throw new IllegalStateException("MySQL descartable requerido");
  String url=mysql==null?"jdbc:h2:file:"+temp.resolve("nucleo").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":mysql+"_contenido?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";
  try(var before=new BaseJdbc(url,mysql==null?"sa":"root",mysql==null?"":System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"),2)){Flyway.configure().dataSource(before.sql.getDataSource()).locations("classpath:db/institucional/"+(mysql==null?"h2":"mysql")).target("15.4").cleanDisabled(true).load().migrate();}
  try(var x=new ContenidoFixture(temp,Clock.systemUTC(),mysql==null?null:url)){
   var p=x.par();x.c().determinar(x.f.a(x.f.headA),UUID.randomUUID(),p[2],p[3],new ContenidoInstitucional.Pertinencia(0,List.of(p[1]),true,"Pertinencia en motor aislado"));
   assertEquals(0,x.c().antecedentes(x.f.a(x.m.upr),p[0]).size());assertEquals(1,x.c().antecedentes(x.f.a(x.m.upd),p[1]).size());
   var doc=x.cargar(p[0]);var bytes=Files.readAllBytes(x.docs.verificarArchivo(x.docs.referencia(x.f.a(x.f.profA),p[0],doc.id())));
   x.ordinario(p[0],doc.id(),0);x.c().publicar(x.f.a(x.f.headA),UUID.randomUUID(),p[0],doc.id(),p[0],x.publicacion(0,true,false,"ACTIVA"));
   assertNotNull(x.docs.referencia(x.f.a(x.m.upr),p[0],doc.id()));
   assertEquals(404,assertThrows(FalloTecnico.class,()->x.docs.referencia(x.f.a(x.m.upr),p[0],doc.id(),"DESCARGA")).status);
   x.c().proteger(x.f.a(x.f.headA),UUID.randomUUID(),p[0],doc.id(),new ContenidoInstitucional.Proteccion(1,"FIR","Reserva en motor aislado"));assertTrue(x.docs.listar(x.f.a(x.m.upr),p[0]).isEmpty());
   assertArrayEquals(bytes,Files.readAllBytes(x.docs.verificarArchivo(x.docs.referencia(x.f.a(x.f.profA),p[0],doc.id()))));assertEquals(2L,x.c().control(x.f.a(x.f.headA),p[0],doc.id()).get("revision"));
  }
 }
 @Test void ddlH2MySqlIguales()throws Exception{try(var h=getClass().getResourceAsStream("/db/institucional/h2/V15_5__pertinencia_y_proteccion_documental.sql");var m=getClass().getResourceAsStream("/db/institucional/mysql/V15_5__pertinencia_y_proteccion_documental.sql")){assertNotNull(h);assertNotNull(m);assertArrayEquals(h.readAllBytes(),m.readAllBytes());}}
}

