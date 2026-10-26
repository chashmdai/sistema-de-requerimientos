package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=IdentidadHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class IdentidadHttpTest {
 @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,IdentidadController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class})static class Application{}
 @Configuration static class Fixture{
  @Bean Clock relojLimitesLogin(){return Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"),ZoneOffset.UTC);}
  static String tiPassword=UUID.randomUUID()+"AZ",tiId;
  @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("identidad-http-"));}
  @Bean IdentidadInstitucional identidad(DatosNucleoTest d){for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);var i=new IdentidadInstitucional(d.db,Clock.systemUTC());tiId=i.bootstrap(new IdentidadInstitucional.Crear("ti.http","TI HTTP sintético","ti.http@example.invalid","TI",null,null,tiPassword)).id();return i;}
  @Bean CatalogoLaboratorio catalogo(IdentidadInstitucional i){return new CatalogoLaboratorio(i);}
  @Bean OrganizacionInstitucional org(DatosNucleoTest d,IdentidadInstitucional i){return new OrganizacionInstitucional(d.db,i,Clock.systemUTC());}
  @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}
  @Bean NucleoInstitucional core(DatosNucleoTest d,CatalogoLaboratorio c){return new NucleoInstitucional(d.db,c,d.recepcion,d.mapper,Clock.systemUTC());}
 }
 @Autowired MockMvc http;@Autowired DatosNucleoTest data;@Autowired IdentidadInstitucional ids;@Autowired CatalogoLaboratorio cat;@Autowired NucleoInstitucional core;
 MockHttpSession login(String user,String password)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",user).param("password",password)).andExpect(status().isNoContent());return s;}
 IdentidadInstitucional.Cuenta user(String pass){String name="u."+UUID.randomUUID();return ids.crear(Fixture.tiId,new IdentidadInstitucional.Crear(name,"Funcionario sintético",name+"@example.invalid","Cargo no concede permisos","UPR","DEMO_SEDE_A",pass));}
 void grant(String id){ids.otorgar(Fixture.tiId,id,new IdentidadInstitucional.Otorgar("PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",Instant.now().minusSeconds(1),null));}
 @Test void tiCreaPorHttpNoDevuelveHashYFuncionarioNoAdministra()throws Exception{
  var ti=login("ti.http",Fixture.tiPassword);String pass=UUID.randomUUID()+"aZ",name="nuevo."+UUID.randomUUID();var json=data.mapper.writeValueAsString(new IdentidadInstitucional.Crear(name,"Nombre sintético",name+"@example.invalid","Administrativo","UPR","DEMO_SEDE_A",pass));
  var response=http.perform(post("/nucleo/ti/funcionarios").secure(true).session(ti).with(csrf()).contentType("application/json").content(json)).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true)).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.password_hash").doesNotExist()).andReturn().getResponse().getContentAsString();assertFalse(response.contains(pass));
  var staff=login(name,pass);http.perform(get("/nucleo/session").secure(true).session(staff)).andExpect(jsonPath("$.operador.cargo").value("Administrativo")).andExpect(jsonPath("$.operador.accesos").isEmpty());http.perform(get("/nucleo/ti/funcionarios").secure(true).session(staff)).andExpect(status().isForbidden());
  http.perform(post("/nucleo/comunicaciones/manual").secure(true).session(staff).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(data.mapper.writeValueAsString(new ContratosNucleo.Manual("Registro manual por funcionario","TELÉFONO",Instant.now().minusSeconds(10),"DEMO_SEDE_A",null)))).andExpect(status().isOk());
 }
 @Test void revocarRolPorHttpInvalidaSesionExistenteYLaNuevaNoConservaAcceso()throws Exception{
  String pass=UUID.randomUUID()+"aZ";var u=user(pass);grant(u.id());var staff=login(u.usuario(),pass);http.perform(get("/nucleo/session").secure(true).session(staff)).andExpect(status().isOk());
  var g=ids.cuentas(Fixture.tiId).stream().filter(x->x.id().equals(u.id())).findFirst().orElseThrow().asignaciones().getFirst().get("id");var ti=login("ti.http",Fixture.tiPassword);
  http.perform(delete("/nucleo/ti/funcionarios/"+u.id()+"/roles/"+g).secure(true).session(ti).with(csrf())).andExpect(status().isOk());
  http.perform(get("/nucleo/session").secure(true).session(staff)).andExpect(status().isUnauthorized());assertTrue(staff.isInvalid());
  var fresh=login(u.usuario(),pass);http.perform(get("/nucleo/session").secure(true).session(fresh)).andExpect(jsonPath("$.operador.accesos").isEmpty());
 }
 @Test void desactivarInvalidaSesionYCredencialYNoSeAdmitePrincipalInyectado()throws Exception{
  String pass=UUID.randomUUID()+"aZ";var u=user(pass);var s=login(u.usuario(),pass);ids.actualizar(Fixture.tiId,u.id(),new IdentidadInstitucional.Perfil(u.nombre(),u.correo(),u.cargo(),u.unidad(),u.sede(),false,u.revision()));
  http.perform(get("/nucleo/comunicaciones").secure(true).session(s)).andExpect(status().isUnauthorized());assertTrue(s.isInvalid());
  http.perform(post("/nucleo/login").secure(true).with(csrf()).param("username",u.usuario()).param("password",pass)).andExpect(status().isUnauthorized());
  http.perform(get("/nucleo/ti/funcionarios").secure(true).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(Fixture.tiId).roles("FUNCIONARIO_LAB"))).andExpect(status().isUnauthorized());
 }
 @Test void credencialDistintaCsrfHttpsYLimiteSeAplicanAlLoginIndividual()throws Exception{
  String pass=UUID.randomUUID()+"aZ";var u=user(pass);
  http.perform(post("/nucleo/login").secure(true).param("username",u.usuario()).param("password",pass)).andExpect(status().isForbidden());
  http.perform(post("/nucleo/login").with(csrf()).param("username",u.usuario()).param("password",pass)).andExpect(status().isForbidden());
  for(int n=0;n<8;n++)http.perform(post("/nucleo/login").secure(true).with(csrf()).param("username",u.usuario()).param("password","incorrecta-individual")).andExpect(status().isUnauthorized());
  http.perform(post("/nucleo/login").secure(true).with(csrf()).param("username",u.usuario()).param("password",pass)).andExpect(status().isTooManyRequests());
 }
 @Test void tiNoObtieneDocumentoOCasoPorIdentificadorNiRolDeContenido()throws Exception{
  var ti=login("ti.http",Fixture.tiPassword);UUID com=data.nuevaWeb();
  http.perform(get("/nucleo/comunicaciones/"+com).secure(true).session(ti)).andExpect(status().isNotFound());
  http.perform(get("/nucleo/casos/"+UUID.randomUUID()).secure(true).session(ti)).andExpect(status().isNotFound());
  http.perform(get("/nucleo/comunicaciones").secure(true).session(ti)).andExpect(status().isOk());assertFalse(cat.operador(Fixture.tiId).jefatura());
 }
}
