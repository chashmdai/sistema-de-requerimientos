package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.smr.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;import org.springframework.mock.env.MockEnvironment;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(classes=SmrHttpTest.Application.class,properties={"institucional.smr.habilitado=true","institucional.formulario-completo.origen-publico=https://localhost:19443","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SmrHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,OperacionSecurity.class,SmrController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture {
  static String tiId,tiPassword=UUID.randomUUID()+"Az";
  @Bean(destroyMethod="close") DatosFormularioTest datos()throws Exception{return new DatosFormularioTest(Files.createTempDirectory("smr-http-"));}
  @Bean(destroyMethod="") BaseJdbc db(DatosFormularioTest d){return d.db;}
  @Bean(destroyMethod="") ArchivoInstitucional files(DatosFormularioTest d){return d.files;}
  @Bean RecepcionInstitucionalJdbc receiver(DatosFormularioTest d){return d.receiver;}
  @Bean ControlesOperacion control(BaseJdbc db){return new ControlesOperacion(db);}
  @Bean IdentidadInstitucional identidad(BaseJdbc db){var i=new IdentidadInstitucional(db,Clock.systemUTC());tiId=i.bootstrap(new IdentidadInstitucional.Crear("ti.smr","TI sintético","ti.smr@example.invalid","Pruebas",null,null,tiPassword)).id();return i;}
  @Bean CatalogoLaboratorio catalog(IdentidadInstitucional i){return new CatalogoLaboratorio(i);}
  @Bean AccesoSmr acceso(BaseJdbc db,IdentidadInstitucional i){return new AccesoSmr(db,i);}
  @Bean HabilitacionNucleo gate(){var e=new MockEnvironment().withProperty("institucional.nucleo.habilitado","true").withProperty("institucional.nucleo.entorno","LABORATORIO").withProperty("server.address","127.0.0.1").withProperty("datos.mysql-local","true").withProperty("institucional.db.url","jdbc:mysql://127.0.0.1:13306/robust_inst_test?sslMode=DISABLED");e.setActiveProfiles("nucleo-lab","jdbc-reception");return new HabilitacionNucleo(e);}
 }
 @Autowired MockMvc http;@Autowired IdentidadInstitucional identity;@Autowired AccesoSmr access;@Autowired DatosFormularioTest d;
 MockHttpSession login(String name,String password)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",password)).andExpect(status().isNoContent());return s;}
 record User(IdentidadInstitucional.Cuenta cuenta,String pass){}
 User crear(){String password=UUID.randomUUID()+"Az",name="smr."+UUID.randomUUID();return new User(identity.crear(Fixture.tiId,new IdentidadInstitucional.Crear(name,"Prueba SMR",name+"@example.invalid","Pruebas",null,null,password)),password);}
 @Test void tiSinFacultadNoLeeContenidoNiOriginalesYNoHayOperacionesDeNegocio()throws Exception{
  var s=login("ti.smr",Fixture.tiPassword);http.perform(get("/nucleo/smr/comunicaciones").secure(true).session(s)).andExpect(status().isForbidden());http.perform(get("/nucleo/smr/estado").secure(true).session(s)).andExpect(status().isForbidden());
  http.perform(get("/nucleo/comunicaciones/"+UUID.randomUUID()+"/documentos").secure(true).session(s)).andExpect(status().isForbidden());
  for(String path:List.of("/nucleo/comunicaciones/manual","/nucleo/casos","/nucleo/solicitudes","/nucleo/derivaciones/entrantes","/nucleo/ti/funcionarios"))http.perform(post(path).secure(true).session(s).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  http.perform(get("/operacion/v1/estado").secure(true).header("Authorization","Bearer "+"t".repeat(64))).andExpect(status().isForbidden());
 }
 @Test void lecturaNoConcedeOperacionYCsrfSigueObligatorio()throws Exception{
  var u=crear();access.cambiar(Fixture.tiId,u.cuenta.id(),"LECTURA",true);var s=login(u.cuenta.usuario(),u.pass);http.perform(get("/nucleo/smr/comunicaciones").secure(true).session(s)).andExpect(status().isOk());
  http.perform(post("/nucleo/smr/control").secure(true).session(s).with(csrf()).header("If-Match",0).contentType("application/json").content("{\"recuperar\":false,\"motivo\":\"MANTENIMIENTO\"}")).andExpect(status().isForbidden());
  http.perform(post("/nucleo/smr/conciliar").secure(true).session(s)).andExpect(status().isForbidden());http.perform(get("/nucleo/smr/estado").session(s)).andExpect(status().isForbidden());
 }
 @Test void revocacionInvalidaSesionAnteriorYNoHayFallbackDeIdentidad()throws Exception{
  var u=crear();access.cambiar(Fixture.tiId,u.cuenta.id(),"LECTURA",true);var s=login(u.cuenta.usuario(),u.pass);http.perform(get("/nucleo/session").secure(true).session(s)).andExpect(jsonPath("$.lectura").value(true));access.cambiar(Fixture.tiId,u.cuenta.id(),"LECTURA",false);http.perform(get("/nucleo/smr/comunicaciones").secure(true).session(s)).andExpect(status().isUnauthorized());var fresh=login(u.cuenta.usuario(),u.pass);http.perform(get("/nucleo/smr/comunicaciones").secure(true).session(fresh)).andExpect(status().isForbidden());
 }
 @Test void documentoIntegroComprobanteYOriginalExigenFacultadYNoExponenRutas()throws Exception{
  var u=crear();access.cambiar(Fixture.tiId,u.cuenta.id(),"LECTURA",true);var s=login(u.cuenta.usuario(),u.pass);byte[] content="Prueba Unicode ñ 🎈".getBytes(java.nio.charset.StandardCharsets.UTF_8);var a=FormularioCompletoInstitucionalTest.attachment(content);var e=d.delivery("1.1","NNA",List.of(a),false,1);d.files.guardar(e.id(),a,new java.io.ByteArrayInputStream(content));var receipt=d.receiver.guardarCompleta(e,d.files,UUID.randomUUID());String id=receipt.registro().comunicacionId().toString();
  http.perform(get("/nucleo/smr/comunicaciones/"+id).secure(true).session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.entrega.comunicacion.personas[0].autenticada").value(false)).andExpect(jsonPath("$.recepcion.registro.folio").value(receipt.registro().folio()));
  http.perform(get("/nucleo/comunicaciones/"+id+"/documentos").secure(true).session(s)).andExpect(status().isOk()).andExpect(jsonPath("$[0].storage_key").doesNotExist()).andExpect(jsonPath("$[0].representacion").value("DESCARGA"));
  http.perform(get("/nucleo/smr/comunicaciones/"+id+"/comprobante").secure(true).session(s)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));http.perform(get("/nucleo/smr/comunicaciones/"+id).secure(true)).andExpect(status().isUnauthorized());
 }
}
