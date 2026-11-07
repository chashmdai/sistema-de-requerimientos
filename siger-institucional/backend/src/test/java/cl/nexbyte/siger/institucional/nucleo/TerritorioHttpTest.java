package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=TerritorioHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class TerritorioHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("territorio-http-"));}
  @Bean TerritorioFixture territorio(DatosNucleoTest d)throws Exception{return new TerritorioFixture(d,Clock.systemUTC());}
  @Bean AreasFixture fixture(TerritorioFixture t){return t.f;}
  @Bean IdentidadInstitucional ids(AreasFixture f){return f.ids;}@Bean CatalogoLaboratorio cat(AreasFixture f){return f.cat;}@Bean NucleoInstitucional core(AreasFixture f){return f.core;}
  @Bean AutoridadesInstitucionales authority(AreasFixture f){return f.f.autoridades;}@Bean ConcesionesInstitucionales grants(AreasFixture f){return f.f.concesiones;}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }

 @Autowired MockMvc http;@Autowired AreasFixture f;@Autowired TerritorioFixture t;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 @Test void webSolTerritorialReqSinCasoConCsrfYLecturaRestringida()throws Exception{
  t.publicar();var com=f.d.nuevaWeb();var dist=login("distribuidor");var head=login("jefe.a");var prof=login("prof.a");var ti=login("ti.areas");
  String path="/nucleo/institucional/comunicaciones/"+com+"/distribuir",body=f.d.mapper.writeValueAsString(new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación sintética","REG_LAB_1"));
  var result=http.perform(post(path).secure(true).session(dist).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();String sol=f.d.mapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
  http.perform(get("/nucleo/institucional/solicitudes/"+sol).secure(true).session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.territorio.estado").value("COBERTURA_ACREDITADA"));
  path="/nucleo/institucional/solicitudes/"+sol+"/asumir/territorio";body=f.d.mapper.writeValueAsString(new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Asunción territorial legítima","UTILIZABLE",null,null));
  http.perform(post(path).secure(true).session(head).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  http.perform(post(path).secure(true).session(ti).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  result=http.perform(post(path).secure(true).session(head).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();String req=f.d.mapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
  http.perform(get("/nucleo/institucional/requerimientos/"+req).secure(true).session(prof)).andExpect(status().isOk()).andExpect(jsonPath("$.territorioInicial.regionDeclarada").value("REG_LAB_1")).andExpect(jsonPath("$.territorioInicial.modo").value("COBERTURA_ACREDITADA")).andExpect(jsonPath("$.sinCasos").value(true));
  http.perform(get("/nucleo/institucional/requerimientos/"+req).secure(true).session(ti)).andExpect(status().isNotFound());
 }
 @Test void preflightPropioProtegeComYSoloLaJefaturaCreadoraPuedeAsumir()throws Exception{
  t.publicar();var com=f.core.registrarManual(f.a(f.headA),UUID.randomUUID(),new ContratosNucleo.Manual("Ingreso propio sintético","TELÉFONO",Instant.now().minusSeconds(60),f.a(f.headA).sede(),null)).id();var head=login("jefe.a");var other=login("jefe.b");var ti=login("ti.areas");
  String path="/nucleo/institucional/comunicaciones/"+com+"/territorio";
  http.perform(get(path).secure(true).session(head).param("unidad","UPR").param("sede","DEMO_SEDE_A").param("area","AREA_A").param("region","REG_LAB_1")).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COBERTURA_ACREDITADA"));
  for(var s:List.of(other,ti))http.perform(get(path).secure(true).session(s).param("unidad","UPR").param("sede","DEMO_SEDE_A").param("area","AREA_A").param("region","REG_LAB_1")).andExpect(status().isNotFound());
  String body=f.d.mapper.writeValueAsString(new NucleoU2.AsuncionPropiaTerritorial("1.0",0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),f.profA,"Evalúa sin paso central","REG_LAB_1","UTILIZABLE",null,null));
  http.perform(post("/nucleo/institucional/comunicaciones/"+com+"/asumir-propia/territorio").secure(true).session(head).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isOk());
 }
}
