package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.*;
import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(classes=CredencialTemporalHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CredencialTemporalHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration
 @Import({SeguridadInstitucional.class,NucleoController.class,CuentaPersonalController.class,IdentidadController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})
 static class Application{}
 @Configuration static class Fixture {
  static String ti;
  @Bean Clock relojLimitesLogin(){return Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC);}
  @Bean(destroyMethod="close") DatosNucleoTest data()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("clave-temporal-http-"));}
  @Bean IdentidadInstitucional identidad(DatosNucleoTest d){var ids=new IdentidadInstitucional(d.db,Clock.systemUTC());ti=ids.bootstrap(new IdentidadInstitucional.Crear("ti.temporal","TI sintético","ti.temporal@example.invalid","TI",null,null,password())).id();return ids;}
  @Bean CatalogoLaboratorio catalogo(IdentidadInstitucional i){return new CatalogoLaboratorio(i);}
  @Bean OrganizacionInstitucional organizacion(DatosNucleoTest d,IdentidadInstitucional i){return new OrganizacionInstitucional(d.db,i,Clock.systemUTC());}
  @Bean NucleoInstitucional core(DatosNucleoTest d,CatalogoLaboratorio c){return new NucleoInstitucional(d.db,c,d.recepcion,d.mapper,Clock.systemUTC());}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}
 }
 @Autowired MockMvc http;@Autowired DatosNucleoTest data;@Autowired IdentidadInstitucional ids;
 static String password(){return UUID.randomUUID()+"aZ";}
 IdentidadInstitucional.Cuenta cuenta(String pass){String name="p."+UUID.randomUUID();return ids.crear(Fixture.ti,new IdentidadInstitucional.Crear(name,"Persona sintética",name+"@example.invalid","Sin permisos por cargo","UPR","DEMO_SEDE_A",pass));}
 MockHttpSession login(String user,String pass)throws Exception{var session=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username",user).param("password",pass)).andExpect(status().isNoContent());return session;}
 String body(String actual,String nueva){return data.mapper.writeValueAsString(Map.of("actual",actual,"nueva",nueva));}
 int cambiar(MockHttpSession session,String actual,String nueva)throws Exception{return http.perform(post("/nucleo/clave-personal").secure(true).session(session).with(csrf()).contentType("application/json").content(body(actual,nueva))).andReturn().getResponse().getStatus();}
 long epoch(String id){return data.db.sql.queryForObject("SELECT security_epoch FROM staff_account WHERE actor_id=?",Long.class,id);}
 int eventos(String id){return data.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event WHERE subject_id=? AND event_code='CLAVE_PERSONAL_CAMBIADA'",Integer.class,id);}

 @Test void acc04SesionTemporalSoloPermiteCambioCsrfYCierreSinContenidoOperativo()throws Exception{
  String pass=password();var account=cuenta(pass);assertTrue(account.cambioClavePendiente());var s=login(account.usuario(),pass);
  var response=http.perform(get("/nucleo/session").secure(true).session(s)).andExpect(jsonPath("$.autenticado").value(true)).andExpect(jsonPath("$.cambioClaveObligatorio").value(true)).andExpect(jsonPath("$.operador").isEmpty()).andReturn().getResponse().getContentAsString();assertFalse(response.contains(account.correo()));
  for(String route:List.of("/nucleo/catalogo","/nucleo/ti/funcionarios","/nucleo/comunicaciones","/nucleo/casos/"+UUID.randomUUID(),"/nucleo/comunicaciones/"+UUID.randomUUID()+"/documentos"))http.perform(get(route).secure(true).session(s)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CAMBIO_CLAVE_REQUERIDO"));
  http.perform(post("/nucleo/comunicaciones/manual").secure(true).session(s).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  assertEquals(0,data.db.sql.queryForObject("SELECT COUNT(*) FROM core_manual WHERE actor_id=?",Integer.class,account.id()));
  http.perform(get("/nucleo/csrf").secure(true).session(s)).andExpect(status().isOk());http.perform(post("/nucleo/logout").secure(true).session(s).with(csrf())).andExpect(status().isNoContent());assertTrue(s.isInvalid());
 }
 @Test void cambioCorrectoExigeNuevoLoginYNoConcedeRolesNiReescribeIngreso()throws Exception{
  String pass=password(),next=password();var account=cuenta(pass);var s=login(account.usuario(),pass);long before=epoch(account.id());
  assertEquals(204,cambiar(s,pass,next));assertTrue(s.isInvalid());assertEquals(before+1,epoch(account.id()));assertEquals(1,eventos(account.id()));
  http.perform(post("/nucleo/login").secure(true).with(csrf()).param("username",account.usuario()).param("password",pass)).andExpect(status().isUnauthorized());
  var fresh=login(account.usuario(),next);http.perform(get("/nucleo/session").secure(true).session(fresh)).andExpect(jsonPath("$.cambioClaveObligatorio").value(false)).andExpect(jsonPath("$.operador.id").value(account.id())).andExpect(jsonPath("$.operador.accesos").isEmpty());
  http.perform(get("/nucleo/catalogo").secure(true).session(fresh)).andExpect(status().isOk());http.perform(get("/nucleo/ti/funcionarios").secure(true).session(fresh)).andExpect(status().isForbidden());
  http.perform(post("/nucleo/comunicaciones/manual").secure(true).session(fresh).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(data.mapper.writeValueAsString(new ContratosNucleo.Manual("Atención sintética por cuenta habilitada","TELÉFONO",Instant.now().minusSeconds(1),"DEMO_SEDE_A",null)))).andExpect(status().isOk());
 }
 @Test void erroresNoConsumenLaCredencialNiAlteranEpoch()throws Exception{
  String pass=password();var account=cuenta(pass);var s=login(account.usuario(),pass);long before=epoch(account.id());
  assertEquals(403,cambiar(s,password(),password()));assertEquals(400,cambiar(s,pass,pass));assertEquals(400,cambiar(s,pass,"corta"));assertEquals(400,cambiar(s,pass,"é".repeat(40)));
  http.perform(post("/nucleo/clave-personal").secure(true).session(s).with(csrf()).contentType("application/json").content("{\"actual\":\"sintetica\",\"nueva\":\"sintetica\",\"actor\":\"otro\"}")).andExpect(status().isBadRequest());
  assertEquals(before,epoch(account.id()));assertEquals(0,eventos(account.id()));http.perform(get("/nucleo/session").secure(true).session(s)).andExpect(jsonPath("$.cambioClaveObligatorio").value(true));
 }
 @Test void cambioExigeHttpsSesionYCsrf()throws Exception{
  String pass=password(),next=password();var account=cuenta(pass);var s=login(account.usuario(),pass);var body=body(pass,next);
  http.perform(post("/nucleo/clave-personal").secure(true).session(s).contentType("application/json").content(body)).andExpect(status().isForbidden());
  http.perform(post("/nucleo/clave-personal").session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  http.perform(post("/nucleo/clave-personal").secure(true).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());assertEquals(0,eventos(account.id()));
 }
 @Test void resetDeTiInvalidaSesionesYVuelveAExigirCambioSinTocarPermisos()throws Exception{
  String pass=password(),next=password(),temporal=password();var account=cuenta(pass);assertEquals(204,cambiar(login(account.usuario(),pass),pass,next));var s=login(account.usuario(),next);
  ids.clave(Fixture.ti,account.id(),new IdentidadInstitucional.Clave(temporal,epoch(account.id())));
  http.perform(get("/nucleo/catalogo").secure(true).session(s)).andExpect(status().isUnauthorized());assertTrue(s.isInvalid());
  var fresh=login(account.usuario(),temporal);http.perform(get("/nucleo/session").secure(true).session(fresh)).andExpect(jsonPath("$.cambioClaveObligatorio").value(true));assertEquals(1,eventos(account.id()));
 }
 @Test void dosSesionesConcurrentesNoCambianLaMismaRevisionDosVeces()throws Exception{
  String pass=password();var account=cuenta(pass);var sessions=List.of(login(account.usuario(),pass),login(account.usuario(),pass));var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
  try(var workers=Executors.newFixedThreadPool(2)){
   var futures=new ArrayList<Future<Integer>>();for(var s:sessions)futures.add(workers.submit(()->{ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));return cambiar(s,pass,password());}));assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();
   var results=new ArrayList<Integer>();for(var f:futures)results.add(f.get(5,TimeUnit.SECONDS));Collections.sort(results);assertEquals(List.of(204,401),results);
  }
  assertEquals(1,eventos(account.id()));assertEquals(1,epoch(account.id()));
 }
 @Test void intentosDeCambioTienenLimitePorCuentaSinPersistirSecretos()throws Exception{
  String pass=password();var account=cuenta(pass);var s=login(account.usuario(),pass);
  for(int i=0;i<8;i++)assertEquals(403,cambiar(s,password(),password()));assertEquals(429,cambiar(s,pass,password()));assertEquals(0,eventos(account.id()));assertEquals(0,epoch(account.id()));
 }
}
