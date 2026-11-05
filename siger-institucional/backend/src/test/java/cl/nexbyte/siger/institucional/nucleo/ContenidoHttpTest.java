package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=ContenidoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true","institucional.documental.habilitada=true","institucional.formulario-completo.habilitado=true"}) @AutoConfigureMockMvc @DirtiesContext
class ContenidoHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,ContenidoController.class,DocumentosGestionController.class,cl.nexbyte.siger.institucional.formulario.DocumentosIngresoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  @Bean(destroyMethod="close") ContenidoFixture datos()throws Exception{return new ContenidoFixture(Files.createTempDirectory("contenido-http-"),Clock.systemUTC(),null);}
  @Bean IdentidadInstitucional ids(ContenidoFixture x){return x.f.ids;}@Bean CatalogoLaboratorio cat(ContenidoFixture x){return x.f.cat;}@Bean NucleoInstitucional core(ContenidoFixture x){return x.f.core;}
  @Bean DocumentosGestion documentos(ContenidoFixture x){return x.docs;}
  @Bean(destroyMethod="") BaseJdbc db(ContenidoFixture x){return x.d.db;}@Bean(destroyMethod="") cl.nexbyte.siger.institucional.formulario.ArchivoInstitucional files(ContenidoFixture x){return x.files;}@Bean RecepcionInstitucionalJdbc receiver(ContenidoFixture x){return x.d.recepcion;}@Bean AutorizacionDocumental policy(ContenidoFixture x){return new AutorizacionNucleo(x.f.core);}
  @Bean VisorDocumento visor(ContenidoFixture x){return new VisorDocumento(x.d.db,x.files,p->true,(p,e)->new ProcesamientoDocumental.Examen(FormatosDocumento.TIPOS.get(e),"PAGINAS",null),new ProcesamientoDocumental.Renderizador(){public int paginas(Path p){return 1;}public byte[] pagina(Path p,int n)throws IOException{var image=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);return out.toByteArray();}},x.clock);}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}@Bean Clock relojLimitesLogin(){return Clock.systemUTC();}
 }
 @Autowired MockMvc http;@Autowired ContenidoFixture x;
 MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",x.f.passwords.get(name))).andExpect(status().isNoContent());return s;}
 @Test void principalPertinenciaYValoracionExigenIdentidadCsrfYAmbito()throws Exception{
  var p=x.par();var jefe=login("jefe.a");var otro=login("jefe.b");var upr=login("direccion.upr");var upd=login("direccion.upd");
  String url="/nucleo/requerimientos/"+p[2]+"/pertinencia/"+p[3];String body=x.d.mapper.writeValueAsString(new ContenidoInstitucional.Pertinencia(0,List.of(p[1]),true,"Pertinencia por materia UPD"));
  http.perform(post(url).secure(true).session(jefe).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  http.perform(post(url).secure(true).session(otro).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  http.perform(post(url).secure(true).session(jefe).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isOk());
  http.perform(get("/nucleo/casos/"+p[0]+"/antecedentes-pertinentes").secure(true).session(upr)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  http.perform(get("/nucleo/casos/"+p[1]+"/antecedentes-pertinentes").secure(true).session(upd)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  http.perform(post("/nucleo/casos/"+p[1]+"/antecedentes-pertinentes/"+p[3]+"/valoraciones").secure(true).session(upd).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":1,\"anterior\":0,\"decision\":\"ADMITIDA\",\"fundamento\":\"No gestionar por lectura\"}")).andExpect(status().isNotFound());
 }
 @Test void metadatosVistaPaginasYDescargaTienenPermisosDistintos()throws Exception{
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);
  UUID doc=x.docs.cargar(x.f.a(x.f.profA),UUID.randomUUID(),c,null,"sintetico.pdf",new ByteArrayInputStream("PDF SINTETICO CON DOBLES".getBytes())).id();x.ordinario(c,doc,0);
  var reader=login("direccion.upr");String base="/nucleo/casos/"+c+"/documentos-gestion";x.c().publicar(x.f.a(x.f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,false,false,"ACTIVA"));
  http.perform(get(base).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$[0].consultable").value(false)).andExpect(jsonPath("$[0].descargable").value(false));
  for(String route:List.of("/original","/vista","/paginas/1","/historial"))http.perform(get(base+"/"+doc+route).secure(true).session(reader)).andExpect(status().isNotFound());
  http.perform(head(base+"/"+doc+"/original").secure(true).session(reader)).andExpect(status().isNotFound());
  x.c().publicar(x.f.a(x.f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(1,true,false,"ACTIVA"));
  http.perform(get(base+"/"+doc+"/vista").secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.descargable").value(false));
  var page=http.perform(get(base+"/"+doc+"/paginas/1").secure(true).session(reader)).andExpect(request().asyncStarted()).andReturn();http.perform(asyncDispatch(page)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  http.perform(get(base+"/"+doc+"/original").header("Range","bytes=0-3").secure(true).session(reader)).andExpect(status().isNotFound());
  x.c().proteger(x.f.a(x.f.headA),UUID.randomUUID(),c,doc,new ContenidoInstitucional.Proteccion(1,"FIR","Reserva declarada por jefatura"));
  http.perform(get(base).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void originalCiudadanoExcluidoNoFiltraNombreNiReferenciaYConservaFirma()throws Exception{
  var o=x.original();var reader=login("direccion.upr");var dist=login("distribuidor");
  x.c().proteger(x.f.a(x.f.headA),UUID.randomUUID(),o.caso(),o.documento(),new ContenidoInstitucional.Proteccion(0,"BLOQUEADO","Adjunto restringido por jefatura"));
  http.perform(get("/nucleo/comunicaciones/"+o.comunicacion()).secure(true).session(dist)).andExpect(status().isOk()).andExpect(jsonPath("$.contenido.formularioCompleto.adjuntos.length()").value(0)).andExpect(jsonPath("$.contenido.formularioCompleto.datos.adjuntos.length()").value(0));
  String base="/nucleo/comunicaciones/"+o.comunicacion()+"/documentos";
  http.perform(get(base).param("caso",o.caso().toString()).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  http.perform(head(base+"/"+o.documento()+"/original").secure(true).session(dist)).andExpect(status().isNotFound());
  x.c().proteger(x.f.a(x.f.headA),UUID.randomUUID(),o.caso(),o.documento(),new ContenidoInstitucional.Proteccion(1,"ORDINARIO","Clasificación del original conservado"));
  x.c().publicar(x.f.a(x.f.headA),UUID.randomUUID(),o.caso(),o.documento(),o.caso(),x.publicacion(0,true,true,"ACTIVA"));
  var result=http.perform(get(base+"/"+o.documento()+"/original").param("caso",o.caso().toString()).header("Range","bytes=0-3").secure(true).session(reader)).andExpect(request().asyncStarted()).andReturn();http.perform(asyncDispatch(result)).andExpect(status().isPartialContent()).andExpect(content().bytes(Arrays.copyOf(o.bytes(),4)));
  Assertions.assertEquals(x.d.mapper.writeValueAsString(o.entrega()),x.d.mapper.writeValueAsString(x.d.recepcion.completaRegistrada(o.comunicacion())));
 }
 @Test void revocacionInvalidaSesionYEnlaceDocumental()throws Exception{
  UUID c=x.m.caso();x.m.clasificar(c,0,"MAT_UPR",true);UUID doc=x.cargar(c).id();x.ordinario(c,doc,0);x.c().publicar(x.f.a(x.f.headA),UUID.randomUUID(),c,doc,c,x.publicacion(0,true,true,"ACTIVA"));var session=login("direccion.upr");String base="/nucleo/casos/"+c+"/documentos-gestion";
  http.perform(head(base+"/"+doc+"/original").secure(true).session(session)).andExpect(status().isOk());
  var instruction=x.d.db.sql.queryForObject("SELECT id FROM access_instruction WHERE beneficiary_id=? AND scope_code='MATERIA' AND state='EJECUTADA'",String.class,x.m.upr);
  x.f.f.concesiones.revocar(x.f.f.revisor,UUID.randomUUID(),UUID.fromString(instruction),new ConcesionesInstitucionales.Decision(2,"Retiro institucional de consulta"));
  try{http.perform(head(base+"/"+doc+"/original").secure(true).session(session)).andExpect(status().isUnauthorized());http.perform(get(base).secure(true).session(login("direccion.upr"))).andExpect(status().isNotFound());}finally{x.m.otorgar(x.m.upr,"MAT_UPR");}
 }
 @Test void cuerposIncompletosSeRechazanSinCambios()throws Exception{
  UUID c=x.m.caso(),doc=x.cargar(c).id();var jefe=login("jefe.a");String route="/nucleo/casos/"+c+"/proteccion-documental/"+doc;
  http.perform(post(route).secure(true).session(jefe).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":0,\"fundamento\":\"Falta clasificación\"}")).andExpect(status().isBadRequest());
  Assertions.assertEquals(0L,x.c().control(x.f.a(x.f.headA),c,doc).get("revision"));
 }
}

