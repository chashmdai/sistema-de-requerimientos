package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=ColaboracionHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext
class ColaboracionHttpTest {
    @SpringBootConfiguration @EnableAutoConfiguration
    @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,ContinuidadU2Controller.class,TerminoColaboracionController.class,ErroresTecnicos.class,JsonEstricto.class,AreasHttpTest.Fixture.class})
    static class Application{}
    @Autowired MockMvc http;@Autowired AreasFixture f;
    MockHttpSession login(String name)throws Exception{var s=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(s).with(csrf()).param("username",name).param("password",f.passwords.get(name))).andExpect(status().isNoContent());return s;}
    String enviar(MockHttpSession s,String path,Object data,int status)throws Exception{return http.perform(post(path).secure(true).session(s).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(f.d.mapper.writeValueAsString(data))).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();}
    long rev(String table,UUID id){return f.d.db.sql.queryForObject("SELECT revision FROM "+table+" WHERE id=?",Long.class,id.toString());}
    @Test void recorridoHttpDePrincipalAreaAHastaCierreExplicitoDeCasoAreaB()throws Exception{
        var flow=f.core.u2();var sol=flow.distribuir(f.a(f.dist),UUID.randomUUID(),f.d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación sintética",null));
        var q=flow.asumir(f.a(f.headA),UUID.randomUUID(),sol.id(),new NucleoU2.Asumir(0,f.profA,"Asunción",null)).id();
        var col=flow.colaborar(f.a(f.headA),UUID.randomUUID(),q,new NucleoU2.Colaborar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Objetivo del Caso colaborador")).id();
        flow.resolver(f.a(f.headB),UUID.randomUUID(),col,new NucleoU2.ResolverColaboracion(0,"ACEPTADA",f.profB,"Acepta destino"));
        var c=UUID.fromString(f.d.db.sql.queryForObject("SELECT case_id FROM institution_collaboration WHERE id=?",String.class,col.toString()));
        var origin=login("jefe.a");var target=login("jefe.b");var pro=login("prof.b");var ti=login("ti.areas");
        String path="/nucleo/institucional/colaboraciones/"+col+"/solicitar-termino";
        var body=new TerminoColaboracion.Solicitar(rev("core_requirement",q),1,"La principal propone finalizar; no clausura Caso ajeno");
        http.perform(post(path).secure(true).session(origin).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(f.d.mapper.writeValueAsString(body))).andExpect(status().isForbidden());
        var id=UUID.fromString(f.d.mapper.readTree(enviar(origin,path,body,200)).path("id").asString());
        http.perform(get("/nucleo/institucional/requerimientos/"+q+"/terminos-colaboracion").secure(true).session(origin)).andExpect(jsonPath("$[0].puedeRetirar").value(true)).andExpect(jsonPath("$[0].puedeDecidir").value(false));
        http.perform(get("/nucleo/institucional/colaboraciones/terminos").secure(true).session(target)).andExpect(jsonPath("$[0].puedeDecidir").value(true));
        http.perform(get("/nucleo/institucional/colaboraciones/terminos").secure(true).session(ti)).andExpect(jsonPath("$.length()").value(0));
        http.perform(get("/nucleo/casos/"+c).secure(true).session(origin)).andExpect(status().isNotFound());
        String decide="/nucleo/institucional/colaboraciones/terminos/"+id+"/decidir";
        var decision=new TerminoColaboracion.Decidir(0,"ACEPTADA","La jefatura destinataria acepta la propuesta");
        enviar(origin,decide,decision,404);enviar(pro,decide,decision,404);enviar(target,decide,decision,200);
        http.perform(get("/nucleo/casos/"+c).secure(true).session(pro)).andExpect(jsonPath("$.estado").value("EN_GESTION"));
        http.perform(get("/nucleo/institucional/continuidad/casos/"+c).secure(true).session(pro)).andExpect(jsonPath("$.terminosColaboracion[0].state").value("ACEPTADA"));
        enviar(pro,"/nucleo/casos/"+c+"/solicitar-termino",new Decision(rev("core_case",c),"Profesional solicita cierre"),200);
        enviar(target,"/nucleo/casos/"+c+"/cerrar",new Decision(rev("core_case",c),"Jefatura revisa y confirma"),200);
        http.perform(get("/nucleo/institucional/requerimientos/"+q).secure(true).session(origin)).andExpect(jsonPath("$.state").value("ACTIVO")).andExpect(jsonPath("$.terminosColaboracion[0].estadoCaso").value("CERRADO"));
        assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE case_id=?",Long.class,c.toString()));
    }
}
