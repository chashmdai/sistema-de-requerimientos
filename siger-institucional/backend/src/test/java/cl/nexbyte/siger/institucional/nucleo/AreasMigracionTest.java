package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class AreasMigracionTest {
 @TempDir Path temp;
 @Test void actualizacion152AAreasConservaDatosYEjecutaElCircuitoFirmado()throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");if(mysql!=null&&!mysql.matches("jdbc:mysql://127\\.0\\.0\\.1:[5-6][0-9]{4}/siger_u15_test_[a-f0-9]{32}"))throw new IllegalStateException("MySQL descartable requerido");String url=mysql==null?"jdbc:h2:file:"+temp.resolve("nucleo").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":mysql+"_area?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";
  try(var before=new BaseJdbc(url,mysql==null?"sa":"root",mysql==null?"":System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"),2)){Flyway.configure().dataSource(before.sql.getDataSource()).locations("classpath:db/institucional/"+(mysql==null?"h2":"mysql")).target("15.2").cleanDisabled(true).load().migrate();}
  try(var d=new DatosNucleoTest(temp,mysql==null?null:url)){
   var f=new AreasFixture(d,Clock.systemUTC());var c=f.caso("AREA_A",f.headA,f.profA);assertEquals("AREA_A",f.core.caso(f.a(f.headA),c).area());assertEquals(404,assertThrows(FalloTecnico.class,()->f.core.caso(f.a(f.headB),c)).status);assertEquals(1,f.core.casos(f.a(f.legacy),"",0,20).total());
   var fresh=d.nuevaWeb();assertEquals(409,assertThrows(FalloTecnico.class,()->f.core.distribuir(f.a(f.dist),UUID.randomUUID(),fresh,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.legacy))))).status);assertEquals(409,assertThrows(FalloTecnico.class,()->f.core.abrirCaso(f.a(f.legacy),UUID.randomUUID(),f.emptyDestination,new ContratosNucleo.Asignacion(0,f.legacy,null))).status);assertEquals(f.legacyCase,f.core.abrirCaso(f.a(f.legacy),f.legacyCaseKey,f.legacyDestination,new ContratosNucleo.Asignacion(0,f.legacy,null)).id());
   assertEquals(400,assertThrows(FalloTecnico.class,()->f.core.equipo().incorporar(f.a(f.headA),UUID.randomUUID(),c,new EquipoCaso.Incorporar(0,f.profB,"Sin ámbito acreditado"))).status);f.otorgar(f.profB,"PROFESIONAL","AREA_A");f.core.equipo().incorporar(f.a(f.headA),UUID.randomUUID(),c,new EquipoCaso.Incorporar(0,f.profB,"Colaboración expresamente habilitada"));assertNotNull(f.core.caso(f.a(f.profB),c));
   f.otorgar(f.headA,"COORDINADOR","AREA_B");f.otorgar(f.profA,"PROFESIONAL","AREA_B");f.core.cambiarArea(f.a(f.headA),UUID.randomUUID(),c,new ContratosNucleo.CambiarArea(1,"AREA_B","Cambio sintético probado en motor real","AREA_A"));assertEquals("AREA_B",f.core.caso(f.a(f.headB),c).area());assertEquals(2,f.core.historialArea(f.a(f.headB),c).size());assertEquals(1,f.core.casos(f.a(f.dist),"",0,20).total());
   f.core.revisarCaso(f.a(f.profA),UUID.randomUUID(),c,new ContratosNucleo.Decision(2,"Solicitud de término"),"SOLICITUD_TERMINO");f.core.revisarCaso(f.a(f.headB),UUID.randomUUID(),c,new ContratosNucleo.Decision(3,"Revisor independiente acreditado"),"CIERRE");assertEquals("CERRADO",f.core.caso(f.a(f.headB),c).estado());
  }
 }
 @Test void sqlAmbosMotoresCoincide()throws Exception{try(var h=getClass().getResourceAsStream("/db/institucional/h2/V15_3__ambitos_de_area.sql");var m=getClass().getResourceAsStream("/db/institucional/mysql/V15_3__ambitos_de_area.sql")){assertNotNull(h);assertNotNull(m);assertArrayEquals(h.readAllBytes(),m.readAllBytes());}}
}
