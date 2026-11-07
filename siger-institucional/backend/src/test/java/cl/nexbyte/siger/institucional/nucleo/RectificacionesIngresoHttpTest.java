package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=RectificacionesIngresoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class RectificacionesIngresoHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,RectificacionesIngresoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("rectificaciones-http-"));}
  @Bean AreasFixture fixture(DatosNucleoTest d)throws Exception{return new AreasFixture(d,Clock.systemUTC());}
  @Bean IdentidadInstitucional ids(AreasFixture f){return f.ids;}@Bean CatalogoLaboratorio cat(AreasFixture f){return f.cat;}@Bean NucleoInstitucional core(AreasFixture f){return f.core;}
  @Bean AutoridadesInstitucionales authority(AreasFixture f){return f.f.autoridades;}@Bean ConcesionesInstitucionales grants(AreasFixture f){return f.f.concesiones;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }

 @Autowired MockMvc http;@Autowired AreasFixture f;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 UUID manual() {return f.core.registrarManual(f.a(f.profA),UUID.randomUUID(),new ContratosNucleo.Manual("Antecedente original sintético","TELÉFONO",Instant.ofEpochMilli(System.currentTimeMillis()-120000),f.a(f.profA).sede(),null)).id();}
 RectificacionesIngreso.Solicitud body(long revision){return new RectificacionesIngreso.Solicitud("1.0",revision,new RectificacionesIngreso.Datos("Captura institucional corregida","CORREO",Instant.ofEpochMilli(System.currentTimeMillis()-180000),"REG_LAB_1"),"Se contrasta la transcripción original");}
 @Test void capturaManualCorreccionYConsultaIndividualRealesConCsrfEIdempotencia()throws Exception{
  var pro=login("prof.a");var com=manual();String path="/nucleo/comunicaciones/"+com+"/rectificaciones",input=f.d.mapper.writeValueAsString(body(0));var key=UUID.randomUUID();
  http.perform(get(path).secure(true)).andExpect(status().isUnauthorized());http.perform(post(path).secure(true).session(pro).header("Idempotency-Key",key).contentType("application/json").content(input)).andExpect(status().isForbidden());
  for(int n=0;n<2;n++)http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(input)).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
  var result=http.perform(get(path).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.original.texto").value("Antecedente original sintético")).andExpect(jsonPath("$.vigente.texto").value("Captura institucional corregida")).andReturn();long seq=f.d.mapper.readTree(result.getResponse().getContentAsString()).path("entradas").get(0).path("secuencia").asLong();
  http.perform(get(path+"/"+seq).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.autor").value(f.profA));http.perform(get("/nucleo/comunicaciones/"+com).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.contenido.texto").value("Antecedente original sintético"));
 }
 @Test void distribucionRevocaConsultaYAsignacionPermiteSinAbrirCasos()throws Exception{
  var com=manual();var pro=login("prof.a");var head=login("jefe.a");var ti=login("ti.areas");var other=login("jefe.b");
  var sol=f.core.u2().distribuir(f.a(f.dist),UUID.randomUUID(),com,new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación sintética",null)).id();String path="/nucleo/comunicaciones/"+com+"/rectificaciones";
  for(var session:List.of(pro,ti,other))http.perform(get(path).secure(true).session(session)).andExpect(status().isNotFound());
  var q=f.core.u2().asumir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,f.profA,"Asigna responsable",null)).id();
  http.perform(get(path).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));http.perform(post(path).secure(true).session(head).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(f.d.mapper.writeValueAsString(body(2)))).andExpect(status().isOk());
  http.perform(get(path+"?tamanio=101").secure(true).session(pro)).andExpect(status().isBadRequest());http.perform(get("/nucleo/institucional/requerimientos/"+q).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.sinCasos").value(true)).andExpect(jsonPath("$.state").value("ACTIVO"));
 }
}
