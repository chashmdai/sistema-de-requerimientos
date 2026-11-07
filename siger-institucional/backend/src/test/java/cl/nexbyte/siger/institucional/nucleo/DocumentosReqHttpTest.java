package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.formulario.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;import static org.junit.jupiter.api.Assertions.*;

/** HTTP/sesión, H2 y custodia reales; antivirus/inspector aislados mediante dobles, sin E2E físico. */
@SpringBootTest(classes=DocumentosReqHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true","institucional.documental.habilitada=true"}) @AutoConfigureMockMvc @DirtiesContext
class DocumentosReqHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,DocumentosRequerimientoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") DocumentosReqFixture fixture()throws Exception{return new DocumentosReqFixture(Files.createTempDirectory("req-documento-http-"));}
  @Bean IdentidadInstitucional ids(DocumentosReqFixture f){return f.f.ids;}@Bean CatalogoLaboratorio cat(DocumentosReqFixture f){return f.f.cat;}@Bean NucleoInstitucional core(DocumentosReqFixture f){return f.f.core;}
  @Bean(destroyMethod="") BaseJdbc db(DocumentosReqFixture f){return f.d.db;}@Bean(destroyMethod="") ArchivoInstitucional files(DocumentosReqFixture f){return f.files;}@Bean RecepcionInstitucionalJdbc receiver(DocumentosReqFixture f){return f.d.recepcion;}@Bean DocumentosRequerimiento gestion(DocumentosReqFixture f){return f.docs;}
  @Bean VisorDocumento visor(DocumentosReqFixture f){return new VisorDocumento(f.d.db,f.files,p->true,(p,e)->e.equals("png")?new ProcesamientoDocumental.Examen("image/png","PAGINAS",Files.readAllBytes(p)):f.inspector.examinar(p,e),new ProcesamientoDocumental.Renderizador(){public int paginas(Path p){throw new AssertionError("Sin worker real en este fixture");}public byte[] pagina(Path p,int n){throw new AssertionError("Sin worker real en este fixture");}},f.clock);}
  @Bean AutoridadesInstitucionales authority(DocumentosReqFixture f){return f.f.f.autoridades;}@Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired DocumentosReqFixture f;
 MockHttpSession login(String actor)throws Exception{String name=f.d.db.sql.queryForObject("SELECT username FROM staff_account WHERE actor_id=?",String.class,actor);var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 String base(){return "/nucleo/requerimientos/"+f.req+"/documentos-gestion";}
 @BeforeEach void reloj(){f.clock.now=Instant.parse("2026-10-25T03:00:00Z");}
 @Test void cargaCsrfOriginalRangeHistorialYRevocacionEnLaMismaSesion()throws Exception{
  var owner=login(f.f.profA);var other=login(f.f.profB);byte[] bytes=f.bytes().readAllBytes();var file=new MockMultipartFile("archivo","sintetico.txt","text/plain",bytes);UUID key=f.key();
  http.perform(multipart(base()).file(file).secure(true).session(owner).header("Idempotency-Key",key)).andExpect(status().isForbidden());
  var result=http.perform(multipart(base()).file(file).secure(true).session(owner).with(csrf()).header("Idempotency-Key",key)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.contextos[0].requerimiento").value(f.req.toString())).andExpect(jsonPath("$.contextos[0].caso").doesNotExist()).andReturn();
  String id=f.d.mapper.readTree(result.getResponse().getContentAsString()).path("id").asString(),url=base()+"/"+id;
  http.perform(get(base()).secure(true).session(other)).andExpect(status().isNotFound());http.perform(get(url+"/historial").secure(true).session(other)).andExpect(status().isNotFound());
  var download=http.perform(get(url+"/original").secure(true).session(owner).header("Range","bytes=0-7")).andExpect(request().asyncStarted()).andReturn();
  http.perform(asyncDispatch(download)).andExpect(status().isPartialContent()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Security-Policy","default-src 'none'; sandbox")).andExpect(content().bytes(Arrays.copyOf(bytes,8)));
  http.perform(head(url+"/original").secure(true).session(owner)).andExpect(status().isOk()).andExpect(header().longValue("Content-Length",bytes.length)).andExpect(content().bytes(new byte[0]));
  http.perform(get(url+"/original").secure(true).session(owner).header("Range","bytes=999-1000")).andExpect(status().isRequestedRangeNotSatisfiable());
  http.perform(get(url+"/historial").secure(true).session(owner)).andExpect(status().isOk()).andExpect(jsonPath("$[0].tipo").value("REQ_DOCUMENTO_CREADO"));
  UUID protectionKey=f.key();String protection=f.d.mapper.writeValueAsString(new DocumentosRequerimiento.Proteccion(0,"BLOQUEADO","Restricción preventiva sintética"));
  for(int retry=0;retry<2;retry++)http.perform(post(url+"/proteccion").secure(true).session(owner).with(csrf()).header("Idempotency-Key",protectionKey).contentType("application/json").content(protection)).andExpect(status().isNoContent());
  http.perform(get(base()).secure(true).session(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.documentos.length()").value(0));
  f.clock.avanzar(1801);http.perform(get(base()).secure(true).session(owner)).andExpect(status().isNotFound());http.perform(get(url+"/original").secure(true).session(owner)).andExpect(status().isNotFound());
 }
 @Test void multipartNoAceptaCasoOrigenDeclaradoNiMasDeUnArchivo()throws Exception{
  var owner=login(f.f.profA);var file=new MockMultipartFile("archivo","sintetico.txt","text/plain",f.bytes().readAllBytes());
  http.perform(multipart(base()).file(file).secure(true).session(owner).with(csrf()).header("Idempotency-Key",f.key()).param("caso",f.f.legacyCase.toString())).andExpect(status().isBadRequest());
  http.perform(multipart(base()).file(file).file(file).secure(true).session(owner).with(csrf()).header("Idempotency-Key",f.key())).andExpect(status().isBadRequest());
 }
 @Test void vistaYPaginaPngUsanAutorizacionReqYNoUnaRutaDeCaso()throws Exception{
  var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);byte[] png=out.toByteArray();
  UUID doc=f.docs.cargar(f.f.a(f.f.profA),f.key(),f.req,null,"imagen.png",new ByteArrayInputStream(png)).id();var owner=login(f.f.profA);String url=base()+"/"+doc;
  http.perform(get(url+"/vista").secure(true).session(owner)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.representacion").value("PAGINAS")).andExpect(jsonPath("$.paginas").value(1));
  var page=http.perform(get(url+"/paginas/1").secure(true).session(owner)).andExpect(request().asyncStarted()).andReturn();http.perform(asyncDispatch(page)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
  http.perform(head(url+"/paginas/1").secure(true).session(owner)).andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
  f.docs.proteger(f.f.a(f.f.headA),f.key(),f.req,doc,new DocumentosRequerimiento.Proteccion(0,"BLOQUEADO","Restricción incluye derivadas persistidas"));http.perform(get(url+"/vista").secure(true).session(owner)).andExpect(status().isNotFound());http.perform(get(url+"/paginas/1").secure(true).session(owner)).andExpect(status().isNotFound());
 }

}
