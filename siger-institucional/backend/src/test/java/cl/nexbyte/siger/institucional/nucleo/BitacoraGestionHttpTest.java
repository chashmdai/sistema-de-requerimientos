package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.SeguridadInstitucional;
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

/** Sesiones/CSRF/persistencia reales de prueba; no navegador ni entorno operacional. */
@SpringBootTest(classes=BitacoraGestionHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.habilitada=true"})
@AutoConfigureMockMvc @DirtiesContext
class BitacoraGestionHttpTest {
    @SpringBootConfiguration @EnableAutoConfiguration
    @Import({SeguridadInstitucional.class,NucleoController.class,NucleoU2Controller.class,BitacoraGestionController.class,ErroresTecnicos.class,JsonEstricto.class,MateriasHttpTest.Fixture.class})
    static class Application {}
    @Autowired MockMvc http;
    @Autowired MateriasFixture m;
    MockHttpSession login(String usuario)throws Exception{
        var session=new MockHttpSession();http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username",usuario).param("password",m.f.passwords.get(usuario))).andExpect(status().isNoContent());return session;
    }
    BitacoraGestion.Registrar registro(long rev,String texto,String rectifica){return new BitacoraGestion.Registrar("1.0",rev,Instant.now().minusSeconds(3600),texto,rectifica,rectifica==null?null:"Rectifica con fundamento explícito");}
    void enviar(MockHttpSession s,String path,Object body,int status)throws Exception{http.perform(post(path).secure(true).session(s).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(m.d.mapper.writeValueAsString(body))).andExpect(status().is(status));}
    @Test void sesionCsrfMateriaYAreaNoSeConviertenEnGestion()throws Exception{
        UUID c=m.caso();m.clasificar(c,0,"MAT_UPR",true);var pro=login("prof.a");var head=login("jefe.a");var reader=login("direccion.upr");var other=login("prof.b");var ti=login("ti.areas");String path="/nucleo/casos/"+c+"/bitacora";
        var input=registro(1,"Gestión ordinaria acreditada",null);
        http.perform(post(path).secure(true).session(pro).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(m.d.mapper.writeValueAsString(input))).andExpect(status().isForbidden());
        enviar(pro,path,input,200);
        http.perform(get(path).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.puedeRegistrar").value(false)).andExpect(jsonPath("$.entradas[0].ejecutor").value(m.f.profA));
        enviar(reader,path,registro(2,"Leer no es gestionar",null),404);http.perform(get(path).secure(true).session(other)).andExpect(status().isNotFound());http.perform(get(path).secure(true).session(ti)).andExpect(status().isNotFound());
        String original=m.d.db.sql.queryForObject("SELECT id FROM core_action WHERE case_id=?",String.class,c.toString());String text=m.d.db.sql.queryForObject("SELECT body_text FROM core_action WHERE id=?",String.class,original);
        enviar(head,path,registro(2,"Constancia corregida",original),200);http.perform(get(path+"/constancias/"+original).secure(true).session(reader)).andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value(text));assertEquals(text,m.d.db.sql.queryForObject("SELECT body_text FROM core_action WHERE id=?",String.class,original));
        http.perform(put(path).secure(true).session(head).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isMethodNotAllowed());
        m.clasificar(c,3,"MAT_UPD",true);http.perform(get(path+"/constancias/"+original).secure(true).session(reader)).andExpect(status().isNotFound());http.perform(get(path).secure(true).session(reader)).andExpect(status().isNotFound());
    }
    @Test void reqSinCasoTieneRutaRealYNoAceptaEjecutorImpostadoNiCamposLibres()throws Exception{
        var flow=m.f.core.u2();var sol=flow.distribuir(m.f.a(m.f.dist),UUID.randomUUID(),m.d.nuevaWeb(),new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",m.f.headA,"AREA_A"),"Clasificación de atención",null)).id();
        UUID req=flow.asumir(m.f.a(m.f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,m.f.profA,"Atención asignada","Sin domicilio utilizable; jefatura decide su ámbito sintético")).id();
        var pro=login("prof.a");String path="/nucleo/institucional/requerimientos/"+req+"/bitacora";var input=registro(0,"Gestión sin crear Caso",null);
        String extra=m.d.mapper.writeValueAsString(input);extra=extra.substring(0,extra.length()-1)+",\"ejecutor\":\"titular.ausente\"}";
        http.perform(post(path).secure(true).session(pro).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(extra)).andExpect(status().isBadRequest());
        enviar(pro,path,input,200);http.perform(get(path).secure(true).session(pro)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.entradas[0].ejecutor").value(m.f.profA));
        assertEquals(0,m.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,req.toString()));
        enviar(pro,"/nucleo/institucional/requerimientos/"+req+"/actuaciones",new ContratosNucleo.Decision(1,"No omite la fecha por la ruta anterior"),409);
    }
    @Test void rutaAnteriorInstitucionalSoloReproduceElComandoHistoricoConfirmado()throws Exception{
        var flow=m.f.core.u2();var sol=flow.distribuir(m.f.a(m.f.dist),UUID.randomUUID(),m.d.nuevaWeb(),new NucleoU2.Encaminar(0,new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",m.f.headA,"AREA_A"),"Clasificación",null)).id();
        UUID req=flow.asumir(m.f.a(m.f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,m.f.profA,"Atención","Decisión territorial fundada sintética")).id();
        UUID caso=flow.abrirCaso(m.f.a(m.f.headA),UUID.randomUUID(),req,new NucleoU2.Apertura(0,m.f.profA,"Gestión propia")).id();
        UUID key=UUID.randomUUID();var body=new ContratosNucleo.Decision(0,"Constancia confirmada antes del cambio de interfaz");
        var result=m.f.core.actuar(m.f.a(m.f.profA),key,caso,body);var before=m.d.db.sql.queryForList("SELECT * FROM core_action WHERE case_id=?",caso.toString());
        var session=login("prof.a");String path="/nucleo/casos/"+caso+"/actuaciones";
        enviar(session,path,new ContratosNucleo.Decision(1,"Un comando nuevo necesita fecha efectiva"),409);
        http.perform(post(path).secure(true).session(session).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(m.d.mapper.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(result.revision()));
        assertEquals(before,m.d.db.sql.queryForList("SELECT * FROM core_action WHERE case_id=?",caso.toString()));
        enviar(session,"/nucleo/casos/"+caso+"/bitacora",registro(1,"Nueva constancia con fecha efectiva",null),200);
    }
}
