package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.SeguridadInstitucional;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=IngresoGestionHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext
class IngresoGestionHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,BitacoraGestionController.class,ErroresTecnicos.class,JsonEstricto.class,AreasHttpTest.Fixture.class})static class Application{}
 @Autowired MockMvc http;@Autowired AreasFixture f;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 NucleoU2.PrimeraGestion body(){var now=Instant.ofEpochMilli(System.currentTimeMillis());return new NucleoU2.PrimeraGestion("1.0",new ContratosNucleo.Manual("Consulta sintética recibida","TELÉFONO",now.minusSeconds(600),f.a(f.profA).sede(),null),new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Orientación efectivamente entregada","ORIENTACION",now.minusSeconds(300));}
 @Test void telefonoComReqFinalizadoSinCasoConFechasRealesYAutorizacion()throws Exception{
  long reqBefore=f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Long.class),casesBefore=f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Long.class);var pro=login("prof.a");var ti=login("ti.areas");var input=body();String path="/nucleo/institucional/primera-atencion/gestion",json=f.d.mapper.writeValueAsString(input);UUID key=UUID.randomUUID();
  http.perform(post(path).secure(true).session(pro).header("Idempotency-Key",key).contentType("application/json").content(json)).andExpect(status().isForbidden());
  http.perform(post(path).secure(true).session(ti).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(json)).andExpect(status().isForbidden());
  var result=http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(json)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();String req=f.d.mapper.readTree(result).path("id").asString();
  http.perform(get("/nucleo/institucional/requerimientos/"+req).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("TERMINADO")).andExpect(jsonPath("$.sinCasos").value(true)).andExpect(jsonPath("$.primeraGestion.tipoResultado").value("ORIENTACION"));
  http.perform(get("/nucleo/institucional/requerimientos/"+req+"/bitacora").secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.entradas[0].realizadaEn").value(input.realizadaEn().toString())).andExpect(jsonPath("$.entradas[0].ejecutor").value(f.profA));
  http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(json)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(req));
  assertEquals(reqBefore+1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Long.class));assertEquals(casesBefore,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Long.class));
  assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,req));
 }
 @Test void rutaAnteriorNoCreaAtencionesNuevasYConservaElReintentoConfirmado()throws Exception{
  var b=body();var legacy=new NucleoU2.PrimeraAtencion(b.comunicacion(),b.ambito(),b.respuesta());UUID key=UUID.randomUUID();var result=f.core.u2().primeraAtencion(f.a(f.profA),key,legacy);var before=f.d.db.sql.queryForList("SELECT * FROM communication_registry ORDER BY numero");var pro=login("prof.a");String path="/nucleo/institucional/primera-atencion";
  http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(f.d.mapper.writeValueAsString(legacy))).andExpect(status().isConflict());
  http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(f.d.mapper.writeValueAsString(legacy))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(result.id().toString()));
  assertEquals(before,f.d.db.sql.queryForList("SELECT * FROM communication_registry ORDER BY numero"));
 }
}
