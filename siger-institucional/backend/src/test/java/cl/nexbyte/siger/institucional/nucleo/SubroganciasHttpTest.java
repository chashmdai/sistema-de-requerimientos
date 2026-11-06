package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(classes=SubroganciasHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class SubroganciasHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,SubroganciasController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("subrogancia-http-"));}
  @Bean SubroganciasFixture fixture(DatosNucleoTest d)throws Exception{return new SubroganciasFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(SubroganciasFixture s){return s.f.ids;}@Bean CatalogoLaboratorio cat(SubroganciasFixture s){return s.f.cat;}@Bean NucleoInstitucional core(SubroganciasFixture s){return s.f.core;}
  @Bean AutoridadesInstitucionales authority(SubroganciasFixture s){return s.f.f.autoridades;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired SubroganciasFixture s;
 MockHttpSession login(String name)throws Exception{var session=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username",name).param("password",name.equals("zz.autoridad.fixture")?s.f.f.claveRevisor:s.f.passwords.get(name))).andExpect(status().isNoContent());return session;}
 String send(MockHttpSession session,String path,Object body,int expected)throws Exception{return http.perform(post(path).secure(true).session(session).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(s.f.d.mapper.writeValueAsString(body))).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();}
 @Test void solicitudAprobacionEjecucionRetornoYRevocacionDeSesionDesdeApi()throws Exception{
  UUID cas=s.f.caso("AREA_A",s.f.headA,s.f.profA);var head=login("jefe.a");var sub=login("prof.a");var auth=login("zz.autoridad.fixture");var ti=login("ti.areas");var alias=login("alias.subrogante");var body=s.solicitud(s.f.clock.instant(),s.f.clock.instant().plusSeconds(600),null,null);
  http.perform(post("/nucleo/subrogancias").secure(true).session(sub).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(s.f.d.mapper.writeValueAsString(body))).andExpect(status().isForbidden());
  UUID id=UUID.fromString(s.f.d.mapper.readTree(send(sub,"/nucleo/subrogancias",body,200)).path("id").asString());String path="/nucleo/subrogancias/"+id;
  send(sub,path+"/aprobar",new SubroganciasInstitucionales.Decision(0,"No puede autoaprobar"),404);send(auth,path+"/aprobar",new SubroganciasInstitucionales.Decision(0,"Autoriza función sintética"),200);send(ti,path+"/ejecutar",new SubroganciasController.Ejecutar(1),200);
  http.perform(get("/nucleo/casos/"+cas).secure(true).session(head)).andExpect(status().isUnauthorized());
  sub=login("prof.a");http.perform(get("/nucleo/casos/"+cas).secure(true).session(sub)).andExpect(status().isOk());http.perform(get("/nucleo/casos/"+cas).secure(true).session(alias)).andExpect(status().isUnauthorized());
  head=login("jefe.a");send(head,path+"/retornar",new SubroganciasInstitucionales.Decision(2,"Retorno fundado"),200);
  http.perform(get("/nucleo/casos/"+cas).secure(true).session(sub)).andExpect(status().isUnauthorized());head=login("jefe.a");http.perform(get("/nucleo/subrogancias").secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.solicitudes[0].state").value("FINALIZADA"));
  var own=s.f.caso("AREA_A",s.f.headA,s.f.headA);http.perform(get("/nucleo/casos/"+own+"/autocierre-regional").secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.permitido").value(true));
  send(head,"/nucleo/casos/"+own+"/autocierre-regional",new ContratosNucleo.Decision(0,"Autocierre titular regional por API"),200);http.perform(get("/nucleo/casos/"+own).secure(true).session(head)).andExpect(jsonPath("$.estado").value("CERRADO"));
 }
}
