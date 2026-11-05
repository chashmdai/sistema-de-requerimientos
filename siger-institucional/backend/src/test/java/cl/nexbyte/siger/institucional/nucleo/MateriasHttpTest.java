package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=MateriasHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class MateriasHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,ConcesionesController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("materias-http-"));}
  @Bean MateriasFixture fixture(DatosNucleoTest d)throws Exception{return new MateriasFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(MateriasFixture m){return m.f.ids;}@Bean CatalogoLaboratorio cat(MateriasFixture m){return m.f.cat;}@Bean NucleoInstitucional core(MateriasFixture m){return m.f.core;}
  @Bean AutoridadesInstitucionales authority(MateriasFixture m){return m.f.f.autoridades;}@Bean ConcesionesInstitucionales grants(MateriasFixture m){return m.f.f.concesiones;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired MateriasFixture m;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",m.f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 @Test void consultaAutenticadaNoPermiteGestionYRectificacionRetiraAcceso()throws Exception{
  var c=m.caso();var jefe=login("jefe.a");var reader=login("direccion.upr");var body=new ContratosNucleo.RectificarMateria(0,"MAT_UPR",true,"Acreditación ordinaria sintética");var route="/nucleo/casos/"+c+"/materia";
  http.perform(post(route).secure(true).session(jefe).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(m.d.mapper.writeValueAsString(body))).andExpect(status().isForbidden());
  http.perform(post(route).secure(true).session(jefe).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(m.d.mapper.writeValueAsString(body))).andExpect(status().isOk());
  http.perform(get("/nucleo/casos/"+c).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.soloLectura").value(true)).andExpect(jsonPath("$.materia").value("MAT_UPR"));
  http.perform(get("/nucleo/casos").secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
  http.perform(post("/nucleo/casos/"+c+"/actuaciones").secure(true).session(reader).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":1,\"texto\":\"No modifica\"}")).andExpect(status().isNotFound());
  http.perform(get(route).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.puedeRectificar").value(false)).andExpect(jsonPath("$.historial.length()").value(0));
  m.clasificar(c,1,"MAT_UPD",true);http.perform(get("/nucleo/casos/"+c).secure(true).session(reader)).andExpect(status().isNotFound());http.perform(get("/nucleo/casos").secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
 }
}
