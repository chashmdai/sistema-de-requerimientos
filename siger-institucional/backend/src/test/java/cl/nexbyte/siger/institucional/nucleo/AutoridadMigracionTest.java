package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Siempre H2 en CI. La misma prueba admite MySQL solo con destino nuevo, efímero y explícito. No limpia bases. */
class AutoridadMigracionTest {
 @TempDir Path temp;
 @ParameterizedTest @ValueSource(strings={"14","15.1","15.2","15.3","15.4","15.5","15.6","15.7","15.8","15.9"})
 void expansionPreservaHistoriaYConcesionTransaccional(String previous)throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");
  if(mysql!=null&&!mysql.matches("jdbc:mysql://127\\.0\\.0\\.1:(?:[5-6][0-9]{4})/siger_u15_test_[a-f0-9]{32}"))throw new IllegalStateException("Se exige una instancia MySQL descartable en puerto efímero y esquema exclusivo U15");
  String url=mysql==null?"jdbc:h2:file:"+temp.resolve("evolucion").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE":mysql+"_"+previous.replace(".","")+"?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";
  String vendor=mysql==null?"h2":"mysql";
  try(var db=new BaseJdbc(url,mysql==null?"sa":"root",mysql==null?"":System.getenv("SIGER_U15_TEST_MYSQL_PASSWORD"),4)){
   var base=Flyway.configure().dataSource(db.sql.getDataSource()).locations("classpath:db/institucional/"+vendor).target(previous).cleanDisabled(true).load();base.migrate();new CatalogoLaboratorio().instalar(db);
   // Registro previo al circuito: se conserva tal cual, sin fabricar una aprobación retroactiva.
   String ti="demo.distribucion";
   db.sql.update("INSERT INTO staff_account VALUES(?,?,?,?,?,?,TRUE,7,?,?,?,?)",ti,"historico.lab","historico@example.invalid","Cargo",null,null,"{bcrypt}hash-sintetico-no-autenticable",1L,2L,ti);
   String legacy=UUID.randomUUID().toString();db.sql.update("INSERT INTO staff_assignment(id,actor_id,role_code,scope_code,valid_from,granted_by) VALUES(?,?,'TI','INSTITUCION',1,?)",legacy,ti,ti);
   var before=db.sql.queryForMap("SELECT * FROM staff_account");var assignment=db.sql.queryForMap("SELECT * FROM staff_assignment");
   db.migrate("institucional");assertEquals(before,db.sql.queryForMap("SELECT * FROM staff_account"));assertEquals(assignment,db.sql.queryForMap("SELECT * FROM staff_assignment"));assertEquals(0,db.sql.queryForObject("SELECT COUNT(*) FROM authority_batch",Long.class));
   assertEquals(0,db.sql.queryForObject("SELECT COUNT(*) FROM approved_assignment",Long.class));assertEquals("15.10",Flyway.configure().dataSource(db.sql.getDataSource()).locations("classpath:db/institucional/"+vendor).load().info().current().getVersion().toString());
   var clock=Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC);var ids=new IdentidadInstitucional(db,clock);
   String target=ids.crear(ti,new IdentidadInstitucional.Crear("beneficiario.mysql","Persona sintética","beneficiario@example.invalid","Sin facultad por cargo",null,null,UUID.randomUUID()+"AZ")).id();
   var grant=new IdentidadInstitucional.Otorgar("PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",clock.instant(),clock.instant().plusSeconds(3600));
   var id=ConcesionFixture.preparar(db,ids,ti,target,grant,clock);var f=ConcesionFixture.contexto(db,ids,ti,clock);var key=UUID.randomUUID();var start=new CountDownLatch(1);
   try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->{start.await();return f.concesiones.ejecutar(ti,key,id,new ConcesionesInstitucionales.Ejecucion(1));});var b=pool.submit(()->{start.await();return f.concesiones.ejecutar(ti,key,id,new ConcesionesInstitucionales.Ejecucion(1));});start.countDown();assertEquals(a.get(8,TimeUnit.SECONDS),b.get(8,TimeUnit.SECONDS));}
   assertTrue(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));assertEquals(1,db.sql.queryForObject("SELECT COUNT(*) FROM approved_assignment",Long.class));
   f.concesiones.revocar(f.revisor,UUID.randomUUID(),id,new ConcesionesInstitucionales.Decision(2,"Retiro sintético"));assertFalse(ids.operador(target).profesionalEn("UPR","DEMO_SEDE_A"));assertEquals(assignment,db.sql.queryForMap("SELECT * FROM staff_assignment WHERE id=?",legacy));
   assertEquals("REVOCADA",f.concesiones.leer(ti,id).get("state"));assertTrue(f.concesiones.historial(ti,id).size()>=4);ConcesionFixture.cerrar(db);
  }
 }
 @Test void scriptsH2YMySqlIdenticos()throws Exception{try(var h=getClass().getResourceAsStream("/db/institucional/h2/V15_2__autoridad_y_concesiones.sql");var m=getClass().getResourceAsStream("/db/institucional/mysql/V15_2__autoridad_y_concesiones.sql")){assertNotNull(h);assertNotNull(m);assertArrayEquals(h.readAllBytes(),m.readAllBytes());}}
}
