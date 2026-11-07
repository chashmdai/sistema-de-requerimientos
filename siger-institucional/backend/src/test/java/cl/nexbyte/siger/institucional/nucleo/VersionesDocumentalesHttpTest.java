package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(classes=VersionesDocumentalesHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true","institucional.documental.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class VersionesDocumentalesHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,VersionesDocumentalesController.class,ErroresTecnicos.class,JsonEstricto.class,DocumentosReqHttpTest.Fixture.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean DocumentosGestion casos(DocumentosReqFixture f){return new DocumentosGestion(f.d.db,f.files,new AutorizacionNucleo(f.f.core),p->true,f.inspector,f.clock);}
  @Bean VersionesDocumentales versiones(DocumentosReqFixture f,DocumentosGestion casos){return new VersionesDocumentales(f.d.db,f.f.core,f.f.cat,casos,f.docs,f.clock);}
 }
 @Autowired MockMvc http;@Autowired DocumentosReqFixture f;
 MockHttpSession login(String actor)throws Exception{String user=f.d.db.sql.queryForObject("SELECT username FROM staff_account WHERE actor_id=?",String.class,actor);var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",user).param("password",f.f.passwords.get(user))).andExpect(status().isNoContent());return s;}
 String base(UUID doc){return "/nucleo/requerimientos/"+f.req+"/documentos-gestion/"+doc+"/versiones";}
 @Test void declararCorregirFinalYSucesionUsanSesionCsrfRevisionYAutorizacionDeAmbosDocumentos()throws Exception{
  UUID a=f.cargar().id(),b=f.cargar().id();var owner=login(f.f.profA);var ti=login(f.f.ti);var other=login(f.f.profB);
  http.perform(get(base(a)).secure(true).session(owner)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.fase").value("SIN_ACREDITAR")).andExpect(jsonPath("$.numero").isEmpty());
  for(UUID doc:List.of(a,b)){String body=f.d.mapper.writeValueAsString(new VersionesDocumentales.Metadatos(-1,"Informe sintético","Tipo declarado","Declaración explícita por responsable"));UUID key=f.key();
   http.perform(post(base(doc)+"/metadatos").secure(true).session(owner).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isForbidden());
   http.perform(post(base(doc)+"/metadatos").secure(true).session(owner).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.fase").value("BORRADOR")).andExpect(jsonPath("$.historial[0].autor").value(f.f.profA));}
  http.perform(get(base(a)).secure(true).session(ti)).andExpect(status().isNotFound());http.perform(get(base(a)).secure(true).session(other)).andExpect(status().isNotFound());
  http.perform(post(base(a)+"/finalizar").secure(true).session(owner).with(csrf()).header("Idempotency-Key",f.key()).contentType("application/json").content(f.d.mapper.writeValueAsString(new VersionesDocumentales.Finalizar(0,null,"Versión final declarada")))).andExpect(status().isOk()).andExpect(jsonPath("$.fase").value("FINAL")).andExpect(jsonPath("$.declaradoPor").value(f.f.profA));
  String next=f.d.mapper.writeValueAsString(new VersionesDocumentales.Sucesor(0,1,a,"Nueva versión enlazada sin sobrescribir"));http.perform(post(base(b)+"/sucesion").secure(true).session(owner).with(csrf()).header("Idempotency-Key",f.key()).contentType("application/json").content(next)).andExpect(status().isOk()).andExpect(jsonPath("$.numero").value(2)).andExpect(jsonPath("$.anterior").value(a.toString()));
  http.perform(get(base(a)).secure(true).session(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.fase").value("FINAL")).andExpect(jsonPath("$.siguiente").value(b.toString()));
  f.clock.avanzar(1801);http.perform(get(base(a)).secure(true).session(owner)).andExpect(status().isNotFound());
 }
}
