package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.*;

class CredencialTemporalTransaccionTest {
 @TempDir Path temp;
 @Test void falloDeAuditoriaRevierteHashEstadoYEpoch()throws Exception{
  try(var d=new DatosNucleoTest(temp)){
   var ids=new IdentidadInstitucional(d.db,Clock.systemUTC());String actual=UUID.randomUUID()+"AZ",nueva=UUID.randomUUID()+"AZ";
   var a=ids.bootstrap(new Crear("ti.rollback","TI sintético","rollback@example.invalid","TI",null,null,actual));var principal=(Principal)ids.autenticar(a.usuario());var before=d.db.sql.queryForMap("SELECT * FROM staff_account WHERE actor_id=?",a.id());
   d.ddl(sql->sql.execute("ALTER TABLE identity_event ADD CONSTRAINT test_clave_audit CHECK (event_code<>'CLAVE_PERSONAL_CAMBIADA')"));
   try{assertThrows(org.springframework.dao.DataAccessException.class,()->ids.cambiarClavePersonal(principal,new CambioClave(actual,nueva)));}
   finally{d.ddl(sql->sql.execute("ALTER TABLE identity_event DROP CONSTRAINT test_clave_audit"));}
   assertEquals(before,d.db.sql.queryForMap("SELECT * FROM staff_account WHERE actor_id=?",a.id()));assertTrue(d.db.sql.queryForObject("SELECT must_change_password FROM staff_credential_state WHERE actor_id=?",Boolean.class,a.id()));assertTrue(ids.vigente(principal));
   ids.cambiarClavePersonal(principal,new CambioClave(actual,nueva));assertFalse(ids.vigente(principal));assertEquals("CambioClave[reservado]",new CambioClave(actual,nueva).toString());
  }
 }
 @Test void resetTiYProcesoPersonalCompitenSobreElMismoEpoch()throws Exception{
  try(var d=new DatosNucleoTest(temp)){
   var ids=new IdentidadInstitucional(d.db,Clock.systemUTC());String actual=UUID.randomUUID()+"AZ",nueva=UUID.randomUUID()+"AZ",reset=UUID.randomUUID()+"AZ";
   var a=ids.bootstrap(new Crear("ti.carrera","TI sintético","carrera@example.invalid","TI",null,null,actual));var principal=(Principal)ids.autenticar(a.usuario());var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
   int cambio,restablecimiento;
   try(var executor=Executors.newFixedThreadPool(2)){
    var personal=executor.submit(()->{ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));try{ids.cambiarClavePersonal(principal,new CambioClave(actual,nueva));return 204;}catch(FalloTecnico e){return e.status;}});
    var admin=executor.submit(()->{ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));try{ids.clave(a.id(),a.id(),new Clave(reset,a.revision()));return 200;}catch(FalloTecnico e){return e.status;}});
    assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();cambio=personal.get(5,TimeUnit.SECONDS);restablecimiento=admin.get(5,TimeUnit.SECONDS);
   }
   assertTrue(cambio==204&&restablecimiento==409||cambio==401&&restablecimiento==200);
   assertEquals(a.revision()+1,d.db.sql.queryForObject("SELECT security_epoch FROM staff_account WHERE actor_id=?",Long.class,a.id()));
   var fresh=(Principal)ids.autenticar(a.usuario());assertEquals(restablecimiento==200,fresh.cambioClavePendiente());
   assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(restablecimiento==200?reset:nueva,fresh.getPassword().substring(8)));
  }
 }
}
