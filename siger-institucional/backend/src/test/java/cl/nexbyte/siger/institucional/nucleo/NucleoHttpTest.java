package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.SeguridadInstitucional;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=NucleoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.identidad.legado-pruebas=true","institucional.nucleo.password-laboratorio=SoloPruebasSinteticasClaveDeMasDe32Caracteres"})
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class NucleoHttpTest {
    @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,NucleoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class}) static class Application{}
    @Configuration static class Fixture {
        @Bean(destroyMethod="close") DatosNucleoTest datos()throws Exception{return new DatosNucleoTest(Files.createTempDirectory("nucleo-http-"));}
        @Bean HabilitacionNucleo gate(){return new HabilitacionNucleo(HabilitacionNucleoTest.valid());}
        @Bean CatalogoLaboratorio catalogo(DatosNucleoTest d){return d.catalogo;}
        @Bean NucleoInstitucional core(DatosNucleoTest d){return d.core;}
    }
    @Autowired MockMvc http;@Autowired DatosNucleoTest data;
    @Test void credencialesNoSeBorranDelCatalogoTrasLoginYLogout()throws Exception{
        for(int attempt=0;attempt<3;attempt++){
            var session=new MockHttpSession();
            http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username","demo.profesional.upd.a").param("password","SoloPruebasSinteticasClaveDeMasDe32Caracteres")).andExpect(status().isNoContent());
            http.perform(post("/nucleo/logout").secure(true).session(session).with(csrf())).andExpect(status().isNoContent());
        }
    }
    @Test void cuerpoManipuladoNoConcedeBajaYCierreRealExigeResponsableCsrf()throws Exception{
        var cid=data.nuevaWeb();var head=data.actor("demo.jefatura.upr.a");
        data.core.distribuir(data.actor("demo.distribucion"),UUID.randomUUID(),cid,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));var d=data.core.detalle(head,cid).destinos().getFirst();
        var id=data.core.abrirCaso(head,UUID.randomUUID(),d.id(),new ContratosNucleo.Asignacion(d.revision(),"demo.profesional.upr.a","BAJA escrita sin clasificación")).id();
        http.perform(post("/nucleo/casos/"+id+"/cerrar").secure(true).with(user("demo.profesional.upr.a").roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":0,\"texto\":\"Intento sintético\",\"clasificacion\":\"BAJA\",\"responsable\":\"demo.profesional.upr.a\"}")).andExpect(status().isBadRequest());
        http.perform(post("/nucleo/casos/"+id+"/clasificar").secure(true).with(user("demo.profesional.upr.a").roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":0,\"clasificacion\":\"BAJA\",\"fundamento\":\"No autorizado\"}")).andExpect(status().isNotFound());
        data.core.clasificar(head,UUID.randomUUID(),id,new ContratosNucleo.Clasificacion(0,"BAJA","Decisión de jefatura sintética"));
        http.perform(post("/nucleo/casos/"+id+"/cerrar").secure(true).with(user("demo.profesional.upr.a").roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"revision\":1,\"texto\":\"Fundamento sintético\"}")).andExpect(status().isOk());
        assertThat(data.core.caso(head,id).modalidadCierre()).isEqualTo("PROFESIONAL_BAJA");
    }
    @Test void evaluacionDeDerivacionNoOtorgaAccesoGeneralYRechazoSigueBloqueado()throws Exception{
        var cid=data.nuevaWeb();var head=data.actor("demo.jefatura.upr.a");data.core.distribuir(data.actor("demo.distribucion"),UUID.randomUUID(),cid,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));var d=data.core.detalle(head,cid).destinos().getFirst();
        var id=data.core.abrirCaso(head,UUID.randomUUID(),d.id(),new ContratosNucleo.Asignacion(d.revision(),"demo.profesional.upr.a",null)).id();
        var r=data.core.solicitarDerivacion(head,UUID.randomUUID(),id,new ContratosNucleo.SolicitarDerivacion(0,"UPD","DEMO_SEDE_A","Motivo sintético","Solo antecedente expresamente asociado")).id();
        http.perform(get("/nucleo/derivaciones/"+r).secure(true).with(user("demo.jefatura.upd.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isOk()).andExpect(jsonPath("$.antecedentesEvaluacion").value("Solo antecedente expresamente asociado")).andExpect(jsonPath("$.actuaciones").doesNotExist()).andExpect(jsonPath("$.contenido").doesNotExist());
        http.perform(get("/nucleo/casos/"+id).secure(true).with(user("demo.jefatura.upd.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        http.perform(get("/nucleo/derivaciones/"+r).secure(true).with(user("demo.jefatura.uep.b").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        http.perform(post("/nucleo/derivaciones/"+r+"/rechazar").secure(true).with(user("demo.jefatura.upd.a").roles("FUNCIONARIO_LAB")).with(csrf())).andExpect(status().isConflict());assertThat(data.core.caso(head,id).estado()).isEqualTo("EN_GESTION");
    }
    @Test void antecedentesYReaperturaTienenCsrfAlcanceYDecisionesSeparadas()throws Exception{
        var core=data.core;var dist=data.actor("demo.distribucion");var head=data.actor("demo.jefatura.upr.a");var prof=data.actor("demo.profesional.upr.a");var root=data.nuevaWeb();var child=data.nuevaWeb();
        core.distribuir(dist,UUID.randomUUID(),root,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));var d=core.detalle(head,root).destinos().getFirst();var c=core.abrirCaso(head,UUID.randomUUID(),d.id(),new ContratosNucleo.Asignacion(d.revision(),prof.id(),null)).id();
        core.clasificar(head,UUID.randomUUID(),c,new ContratosNucleo.Clasificacion(0,"BAJA","Fundamento sintético"));core.revisarCaso(prof,UUID.randomUUID(),c,new ContratosNucleo.Decision(1,"Cierre sintético"),"CIERRE");core.vincular(dist,UUID.randomUUID(),child,new ContratosNucleo.Vinculacion(root,c,"Vínculo sintético"));
        http.perform(get("/nucleo/antecedentes/"+child).secure(true).with(user("demo.profesional2.upr.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        var body=new ContratosNucleo.EvaluarAntecedente(0,core.caso(head,c).revision(),"ADMITIDA","Pertinencia sintética");String json=data.mapper.writeValueAsString(body);
        http.perform(post("/nucleo/antecedentes/"+child+"/evaluar").secure(true).with(user(prof.id()).roles("FUNCIONARIO_LAB")).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(json)).andExpect(status().isForbidden());
        http.perform(post("/nucleo/antecedentes/"+child+"/evaluar").secure(true).with(user(prof.id()).roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(json)).andExpect(status().isOk());
        var r=core.solicitarReapertura(prof,UUID.randomUUID(),c,new ContratosNucleo.SolicitarReapertura(core.caso(head,c).revision(),List.of(child),"Solicitar autorización",null,null));String decision=data.mapper.writeValueAsString(new ContratosNucleo.DecidirReapertura(0,core.caso(head,c).revision(),"Rechazo sintético"));
        http.perform(post("/nucleo/reaperturas/"+r.id()+"/aprobar").secure(true).with(user(prof.id()).roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(decision)).andExpect(status().isNotFound());
        http.perform(post("/nucleo/reaperturas/"+r.id()+"/rechazar").secure(true).with(user(head.id()).roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(decision)).andExpect(status().isOk());assertThat(core.caso(prof,c).estado()).isEqualTo("CERRADO");assertThat(core.antecedente(prof,child).estado()).isEqualTo("ADMITIDA");
    }
    @Test void loginRequiereHttpsCsrfYCredencialYLogoutInvalidaSesion()throws Exception{
        http.perform(post("/nucleo/login").secure(true).param("username","demo.distribucion").param("password","SoloPruebasSinteticasClaveDeMasDe32Caracteres")).andExpect(status().isForbidden());
        http.perform(post("/nucleo/login").with(csrf()).param("username","demo.distribucion").param("password","SoloPruebasSinteticasClaveDeMasDe32Caracteres")).andExpect(status().isForbidden());
        var session=new MockHttpSession();String previous=session.getId();
        http.perform(post("/nucleo/login").secure(true).session(session).with(csrf()).param("username","demo.distribucion").param("password","SoloPruebasSinteticasClaveDeMasDe32Caracteres")).andExpect(status().isNoContent());
        assertThat(session.getId()).isNotEqualTo(previous);
        http.perform(get("/nucleo/session").secure(true).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.operador.id").value("demo.distribucion"));
        http.perform(post("/nucleo/logout").secure(true).session(session).with(csrf())).andExpect(status().isNoContent());assertThat(session.isInvalid()).isTrue();
    }
    @Test void autorizacionPorRecursoYMutacionExigeCsrfSinUsarTokenTecnico()throws Exception{
        var cid=data.web();
        http.perform(get("/nucleo/comunicaciones").secure(true).header("Authorization","Bearer token-tecnico-no-es-login")).andExpect(status().isUnauthorized());
        http.perform(get("/nucleo/comunicaciones/"+cid).secure(true).with(user("demo.profesional.upd.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        var url="/nucleo/comunicaciones/"+cid+"/distribuir";String body="{\"revision\":0,\"destinos\":[{\"unidad\":\"UPR\",\"sede\":\"DEMO_SEDE_A\",\"evaluador\":\"demo.jefatura.upr.a\"}]}";
        http.perform(post(url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB")).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        String key=UUID.randomUUID().toString();
        for(int retry=0;retry<2;retry++)http.perform(post(url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB")).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isOk());
        http.perform(get("/nucleo/comunicaciones/"+cid).secure(true).with(user("demo.jefatura.uep.b").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        assertThat(data.db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Long.class,cid.toString())).isEqualTo(1);
    }
}
