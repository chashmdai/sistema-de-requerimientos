package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(classes=AccesoReservadoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class AccesoReservadoHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,AccesoReservadoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("reserva-http-"));}
  @Bean AccesoReservadoFixture fixture(DatosNucleoTest d)throws Exception{return new AccesoReservadoFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(AccesoReservadoFixture f){return f.x.f.ids;}@Bean CatalogoLaboratorio cat(AccesoReservadoFixture f){return f.x.f.cat;}@Bean NucleoInstitucional core(AccesoReservadoFixture f){return f.x.f.core;}
  @Bean AutoridadesInstitucionales authority(AccesoReservadoFixture f){return f.x.f.f.autoridades;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired AccesoReservadoFixture f;
 MockHttpSession login(String actor)throws Exception{String name=f.d.db.sql.queryForObject("SELECT username FROM staff_account WHERE actor_id=?",String.class,actor);var session=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username",name).param("password",actor.equals(f.x.f.f.revisor)?f.x.f.f.claveRevisor:f.x.f.passwords.get(name))).andExpect(status().isNoContent());return session;}
 @Test void propuestaYDecisionRealProtegenCsrfCuentasYListas()throws Exception{
  var x=f.x;UUID doc=x.documento("FIR");var head=login(x.owner);var gestion=login(f.gestion);var reviewer=login(x.reviewer);var ti=login(x.f.ti);String path="/nucleo/casos/"+x.cas+"/proteccion-documental/"+doc+"/acceso-reservado";
  http.perform(get(path).secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.gestionAcreditada").value(true));http.perform(get(path).secure(true).session(reviewer)).andExpect(status().isNotFound());
  var request=new AccesoReservado.Proponer(1,x.reviewer,f.clock.instant(),f.clock.instant().plusSeconds(120),true,false,"Respaldo sintético por jefatura competente");String body=f.d.mapper.writeValueAsString(request);http.perform(post(path).secure(true).session(head).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  var result=http.perform(post(path).secure(true).session(head).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();String id=f.d.mapper.readTree(result).path("id").asString();String decision="/nucleo/accesos-reservados/"+id+"/aprobar";String dec=f.d.mapper.writeValueAsString(new AccesoReservado.Decidir(0,"Respaldo independiente de Gestión"));
  http.perform(get("/nucleo/accesos-reservados").secure(true).session(ti)).andExpect(status().isOk()).andExpect(content().json("[]"));http.perform(get("/nucleo/accesos-reservados").secure(true).session(reviewer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].document_id").doesNotExist()).andExpect(jsonPath("$[0].aprobar").value(false));
  http.perform(post(decision).secure(true).session(reviewer).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(dec)).andExpect(status().isNotFound());http.perform(post(decision).secure(true).session(gestion).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(dec)).andExpect(status().isOk());org.junit.jupiter.api.Assertions.assertTrue(f.x.f.core.contenidos().permisos(x.f.a(x.reviewer),x.cas,doc).contenido());
 }
}
