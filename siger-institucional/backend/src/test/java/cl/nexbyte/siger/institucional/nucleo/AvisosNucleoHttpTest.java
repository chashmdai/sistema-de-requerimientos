package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=AvisosNucleoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true","institucional.notificaciones.habilitadas=true"}) @AutoConfigureMockMvc @DirtiesContext
class AvisosNucleoHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,cl.nexbyte.siger.institucional.notificaciones.NotificacionesController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("avisos-u2-http-"));}
  @Bean AreasFixture fixture(DatosNucleoTest d)throws Exception{return new AreasFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(AreasFixture f){return f.ids;}@Bean CatalogoLaboratorio cat(AreasFixture f){return f.cat;}@Bean NucleoInstitucional core(AreasFixture f){return f.core;}
  @Bean AutoridadesInstitucionales authority(AreasFixture f){return f.f.autoridades;}@Bean ConcesionesInstitucionales grants(AreasFixture f){return f.f.concesiones;}
  @Bean cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion recursos(DatosNucleoTest d,AreasFixture f){return new cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion(d.db,f.cat,f.core);}
  @Bean cl.nexbyte.siger.institucional.notificaciones.TareasPersonales tareas(DatosNucleoTest d,AreasFixture f,cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion r){return new cl.nexbyte.siger.institucional.notificaciones.TareasPersonales(d.db,r,d.mapper,Clock.systemUTC());}
  @Bean cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales notices(DatosNucleoTest d,AreasFixture f,cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion r){return new cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales(d.db,f.cat,r,Clock.systemUTC());}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }

 @Autowired MockMvc http;@Autowired AreasFixture f;@Autowired cl.nexbyte.siger.institucional.notificaciones.NotificacionesInstitucionales n;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 UUID sol(){return f.core.u2().distribuir(f.a(f.dist),UUID.randomUUID(),f.d.nuevaWeb(),new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación sintética",null)).id();}
 UUID aviso(UUID sol,String tipo,String persona){return UUID.fromString(f.d.db.sql.queryForObject("SELECT id FROM institution_notification WHERE resource_id=? AND resource_type='AVISO_U2' AND category=? AND recipient_id=?",String.class,sol.toString(),tipo,persona));}
 @Test void consultarYMarcarAvisoPropioNoAsumeSolicitud()throws Exception{
  var sol=sol();n.proyectar();var id=aviso(sol,"SOL_DISTRIBUIDA",f.headA);var head=login("jefe.a");var ti=login("ti.areas");String path="/nucleo/notificaciones/"+id;
  http.perform(get(path+"/contexto-institucional").secure(true)).andExpect(status().isUnauthorized());http.perform(get(path+"/contexto-institucional").secure(true).session(ti)).andExpect(status().isNotFound());
  http.perform(get(path+"/contexto-institucional").secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.ruta").value("sol/"+sol)).andExpect(jsonPath("$.destino.area").value("AREA_A")).andExpect(jsonPath("$.relato").doesNotExist()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
  http.perform(post(path+"/leer").secure(true).session(head)).andExpect(status().isForbidden());http.perform(post(path+"/leer").secure(true).session(head).with(csrf())).andExpect(status().isOk());assertEquals("PENDIENTE",f.core.u2().solicitud(f.a(f.headA),sol).get("state"));assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_req",Long.class));
 }
 @Test void origenRedireccionAccedePaqueteSinAbrirSolDeOtraArea()throws Exception{
  var sol=sol();f.core.u2().redirigir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Redirige por competencia",null));n.proyectar();var id=aviso(sol,"SOL_REDIRIGIDA",f.headA);var head=login("jefe.a");
  http.perform(get("/nucleo/notificaciones/"+id+"/contexto-institucional").secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.ruta").isEmpty()).andExpect(jsonPath("$.origen.area").value("AREA_A")).andExpect(jsonPath("$.destino.area").value("AREA_B")).andExpect(jsonPath("$.contenido").doesNotExist());
  http.perform(get("/nucleo/institucional/solicitudes/"+sol).secure(true).session(head)).andExpect(status().isNotFound());
 }
}
