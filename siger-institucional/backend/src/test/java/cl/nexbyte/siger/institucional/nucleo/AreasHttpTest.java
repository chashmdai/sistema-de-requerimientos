package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=AreasHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class AreasHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,ConcesionesController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("areas-http-"));}
  @Bean AreasFixture fixture(DatosNucleoTest d)throws Exception{return new AreasFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(AreasFixture f){return f.ids;}@Bean CatalogoLaboratorio cat(AreasFixture f){return f.cat;}@Bean NucleoInstitucional core(AreasFixture f){return f.core;}
  @Bean AutoridadesInstitucionales authority(AreasFixture f){return f.f.autoridades;}@Bean ConcesionesInstitucionales grants(AreasFixture f){return f.f.concesiones;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired AreasFixture f;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",name.equals("zz.autoridad.fixture")?f.f.claveRevisor:f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 @Test void apiNoAdmiteAreaOmitidaNiLaCreaPorPerfil()throws Exception{
  var dist=login("distribuidor");var legacy=login("jefe.historico");var com=f.d.nuevaWeb();String body=f.d.mapper.writeValueAsString(new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.legacy))));
  http.perform(get("/nucleo/catalogo").secure(true).session(dist)).andExpect(status().isOk()).andExpect(jsonPath("$.ambitosConAreas[0].unidad").value("UPR"));
  http.perform(post("/nucleo/comunicaciones/"+com+"/distribuir").secure(true).session(dist).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("AREA_REQUERIDA"));
  http.perform(post("/nucleo/destinos/"+f.emptyDestination+"/abrir-caso").secure(true).session(legacy).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(f.d.mapper.writeValueAsString(new ContratosNucleo.Asignacion(0,f.legacy,null)))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("AREA_REQUERIDA"));
 }
 @Test void circuitoPorAreaConSesionesRealesCsrfListadosYCambioControlado()throws Exception{
  var ca=f.caso("AREA_A",f.headA,f.profA);var cb=f.caso("AREA_B",f.headB,f.profB);MockHttpSession a=login("jefe.a"),b=login("jefe.b"),ti=login("ti.areas"),old=login("jefe.historico");
  http.perform(get("/nucleo/casos/"+ca).secure(true).session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.area").value("AREA_A"));
  for(var s:List.of(b,ti,old))http.perform(get("/nucleo/casos/"+ca).secure(true).session(s)).andExpect(status().isNotFound());
  http.perform(get("/nucleo/casos").secure(true).session(b)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(cb.toString()));
  http.perform(get("/nucleo/catalogo").secure(true).session(ti)).andExpect(status().isOk()).andExpect(jsonPath("$.areas.length()").value(2));
  String body=f.d.mapper.writeValueAsString(new ContratosNucleo.CambiarArea(0,"AREA_B","Cambio acreditado por jefaturas","AREA_A"));String route="/nucleo/casos/"+ca+"/area";
  http.perform(post(route).secure(true).session(a).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  http.perform(post(route).secure(true).session(a).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  f.otorgar(f.headA,"COORDINADOR","AREA_B");f.otorgar(f.profA,"PROFESIONAL","AREA_B");http.perform(get("/nucleo/session").secure(true).session(a)).andExpect(status().isUnauthorized());a=login("jefe.a");
  var key=UUID.randomUUID();for(int n=0;n<2;n++)http.perform(post(route).secure(true).session(a).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
  http.perform(get("/nucleo/casos/"+ca).secure(true).session(b)).andExpect(status().isOk()).andExpect(jsonPath("$.area").value("AREA_B"));http.perform(get("/nucleo/casos/"+ca+"/areas/historial").secure(true).session(b)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
 }
}
