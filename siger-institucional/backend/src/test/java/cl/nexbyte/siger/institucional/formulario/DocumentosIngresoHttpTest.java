package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.SeguridadInstitucional;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.util.*;import java.io.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=DocumentosIngresoHttpTest.Application.class,properties={"institucional.nucleo.habilitado=true","institucional.formulario-completo.habilitado=true","institucional.nucleo.password-laboratorio=SoloPruebasSinteticasClaveDeMasDe32Caracteres"})
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class DocumentosIngresoHttpTest {
    @SpringBootConfiguration @EnableAutoConfiguration @Import({SeguridadInstitucional.class,DocumentosIngresoController.class,NucleoController.class,ErroresTecnicos.class,JsonEstricto.class,Fixture.class}) static class Application{}
    @Configuration static class Fixture {
        @Bean(destroyMethod="close") DatosFormularioTest datos()throws Exception{return new DatosFormularioTest(Files.createTempDirectory("documentos-http-"));}
        @Bean HabilitacionNucleo gate(){var e=new MockEnvironment().withProperty("institucional.nucleo.habilitado","true").withProperty("institucional.nucleo.entorno","LABORATORIO").withProperty("server.address","127.0.0.1").withProperty("datos.mysql-local","true").withProperty("server.servlet.session.cookie.secure","true").withProperty("institucional.db.url","jdbc:mysql://127.0.0.1:13306/robust_inst_test?sslMode=DISABLED");e.setActiveProfiles("nucleo-lab","jdbc-reception");return new HabilitacionNucleo(e);}
        @Bean CatalogoLaboratorio catalog(DatosFormularioTest d){return d.catalog;}
        @Bean NucleoInstitucional core(DatosFormularioTest d){return d.core;}
        @Bean(destroyMethod="") BaseJdbc base(DatosFormularioTest d){return d.db;}
        @Bean RecepcionInstitucionalJdbc receiver(DatosFormularioTest d){return d.receiver;}
        @Bean(destroyMethod="") ArchivoInstitucional files(DatosFormularioTest d){return d.files;}
    }
    @Autowired MockMvc http;@Autowired DatosFormularioTest d;
    record Saved(UUID com,UUID doc,String url){}
    Saved create(String version,String mime)throws Exception{
        byte[] bytes="ORIGINAL-SINTETICO".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var a=new EntregaCompleta.Adjunto(UUID.randomUUID(),"sintetico.bin",bytes.length,java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)),mime,"VALIDADO");
        var e=d.delivery(version,"NNA",List.of(a),false,1);d.files.guardar(e.id(),a,new ByteArrayInputStream(bytes));var receipt=d.receiver.guardarCompleta(e,d.files);var com=receipt.registro().comunicacionId();
        var id=UUID.fromString(d.db.sql.queryForObject("SELECT id FROM institution_document WHERE communication_id=?",String.class,com.toString()));return new Saved(com,id,"/nucleo/comunicaciones/"+com+"/documentos");
    }
    @Test void exigeSesionHttpsYAlcancePorComunicacion()throws Exception{
        var s=create("1.1","audio/wav");
        http.perform(get(s.url).secure(true)).andExpect(status().isUnauthorized());
        http.perform(get(s.url).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isForbidden());
        http.perform(get(s.url).secure(true).with(user("demo.jefatura.uep.b").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        http.perform(get(s.url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].origen").value("INGRESO_CIUDADANO")).andExpect(jsonPath("$[0].representacion").value("AUDIO")).andExpect(jsonPath("$[0].storage_key").doesNotExist());
    }
    @Test void descargaStreamingRangeHeadYAuditoria()throws Exception{
        var s=create("1.1","audio/wav");var url=s.url+"/"+s.doc+"/representacion";
        var r=http.perform(get(url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB")).header("Range","bytes=0-7")).andExpect(request().asyncStarted()).andReturn();
        http.perform(asyncDispatch(r)).andExpect(status().isPartialContent()).andExpect(header().string("Content-Range","bytes 0-7/18")).andExpect(header().string("Cache-Control","no-store")).andExpect(content().string("ORIGINAL"));
        http.perform(head(url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isOk()).andExpect(header().longValue("Content-Length",18)).andExpect(content().string(""));
        http.perform(get(url).secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB")).header("Range","bytes=200-")).andExpect(status().isRequestedRangeNotSatisfiable());
        assertThat(d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document_access WHERE document_id=?",Long.class,s.doc.toString())).isEqualTo(3);
    }
    @Test void documentoAjenoNoSeResuelvePorUuidYCorruptoNoSeSirve()throws Exception{
        var s=create("1.1","audio/wav");var other=create("1.1","audio/wav");
        http.perform(get(other.url+"/"+s.doc+"/original").secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        var key=d.db.sql.queryForObject("SELECT storage_key FROM institution_document WHERE id=?",String.class,s.doc.toString());var path=d.dir.resolve("archivo/objects").resolve(key+".bin");Files.writeString(path,"ALTERADO");
        http.perform(get(s.url+"/"+s.doc+"/original").secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isConflict());
    }
    @Test void noActivaPdfOfficeNiMediosHistoricos()throws Exception{
        for(var pair:List.of(new String[]{"1.0","audio/wav"},new String[]{"1.1","application/pdf"},new String[]{"1.1","application/vnd.openxmlformats-officedocument.wordprocessingml.document"})){
            var s=create(pair[0],pair[1]);
            http.perform(get(s.url+"/"+s.doc+"/representacion").secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
            var r=http.perform(get(s.url+"/"+s.doc+"/original").secure(true).with(user("demo.distribucion").roles("FUNCIONARIO_LAB"))).andExpect(request().asyncStarted()).andReturn();
            http.perform(asyncDispatch(r)).andExpect(status().isOk()).andExpect(content().contentType("application/octet-stream")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        }
    }
    @Test void antecedenteDocumentalRespetaCasoYEvaluadorExistentes()throws Exception{
        var root=create("1.1","text/plain");var child=create("1.1","audio/wav");var head=d.catalog.operador("demo.jefatura.upr.a");var dist=d.catalog.operador("demo.distribucion");
        d.core.distribuir(dist,UUID.randomUUID(),root.com,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",head.id()))));var dest=d.core.detalle(head,root.com).destinos().getFirst();
        var caso=d.core.abrirCaso(head,UUID.randomUUID(),dest.id(),new ContratosNucleo.Asignacion(dest.revision(),"demo.profesional.upr.a",null)).id();
        d.core.vincular(dist,UUID.randomUUID(),child.com,new ContratosNucleo.Vinculacion(root.com,caso,"Antecedente sintético"));
        http.perform(get(child.url).param("caso",caso.toString()).secure(true).with(user("demo.profesional.upr.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isOk());
        http.perform(get(child.url).param("caso",caso.toString()).secure(true).with(user("demo.profesional2.upr.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
        http.perform(get(child.url).param("caso",UUID.randomUUID().toString()).secure(true).with(user("demo.profesional.upr.a").roles("FUNCIONARIO_LAB"))).andExpect(status().isNotFound());
    }
}