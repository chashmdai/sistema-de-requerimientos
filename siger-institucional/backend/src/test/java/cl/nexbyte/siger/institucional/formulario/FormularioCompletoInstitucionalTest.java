package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.nucleo.*;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.CsvSource;import static org.junit.jupiter.api.Assertions.*;

class FormularioCompletoInstitucionalTest {
    @TempDir Path dir;DatosFormularioTest d;
    @BeforeEach void open()throws Exception{d=new DatosFormularioTest(dir);}
    @AfterEach void close()throws Exception{if(d!=null)d.close();}
    @ParameterizedTest @CsvSource({"1.0,ADULTO_CU,true","1.1,ADULTO_CU,true","1.0,ADULTO_DECLARADO,true","1.1,ADULTO_DECLARADO,true","1.0,NNA,false","1.1,NNA,false","1.0,NNA,true","1.1,NNA,true"})
    void registraDocumentoIntegroConVersionAckPorCanalYNingunCasoAutomatico(String version,String type,boolean run)throws Exception{
        var e=d.delivery(version,type,List.of(),run,1);var ack=d.receiver.guardarCompleta(e,d.files);e.validarRegistro(ack,d.own,d.mapper);
        assertEquals(type.equals("NNA")?"1.1":"1.0",ack.registro().version());assertEquals(type.equals("NNA")?"FORMULARIO_WEB_NNA":"FORMULARIO_WEB_PUBLICO",ack.registro().canalOrigen());
        assertEquals(e.comunicacion(),d.receiver.completaRegistrada(ack.registro().comunicacionId()).comunicacion());
        assertEquals(ack,d.receiver.guardarCompleta(e,d.files));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Integer.class));
        var detail=d.core.detalle(d.catalog.operador("demo.distribucion"),ack.registro().comunicacionId());assertEquals(5000,detail.contenido().texto().length());assertEquals(e.comunicacion(),detail.contenido().formularioCompleto());
    }
    @Test void bodySuperaTextMysqlSinTruncarPersonasFuentesNiCamposDesconocidos(){var e=d.delivery("1.1","ADULTO_DECLARADO",List.of(),true,30);assertTrue(d.mapper.writeValueAsString(e).length()>65535);var ack=d.receiver.guardarCompleta(e,d.files);assertEquals(e.comunicacion(),d.receiver.completaRegistrada(ack.registro().comunicacionId()).comunicacion());}
    @Test void rechazaFirmaFalsaCambioDeContenidoYAutenticacionInventada()throws Exception{
        var e=d.delivery("1.0","ADULTO_DECLARADO",List.of(),true,1);var other=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();var p=e.custodia();var fake=ComprobanteDigital.firmar(p.verificar(d.trust,d.mapper),"public-test",other.getPrivate(),d.mapper);assertThrows(IllegalArgumentException.class,()->d.receiver.guardarCompleta(new EntregaCompleta(e.comunicacion(),e.sha256(),e.huellaProcedencia(),fake),d.files));
        ((tools.jackson.databind.node.ObjectNode)e.comunicacion().path("datos").path("campos")).put("alterado","sí");assertThrows(IllegalArgumentException.class,()->d.receiver.guardarCompleta(e,d.files));
        var invented=d.delivery("1.0","ADULTO_CU",List.of(),true,1);((tools.jackson.databind.node.ObjectNode)invented.comunicacion().path("personas").get(0).path("atributos").path("nombres")).put("fuente","SRCEI");var signed=d.sign(invented.comunicacion());assertThrows(IllegalArgumentException.class,()->d.receiver.guardarCompleta(signed,d.files));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));
    }
    @Test void originalIncompletoCorruptoOAusenteImpideRegistroYNoPublicaTemporal()throws Exception{
        byte[] bytes="Documento sintético".getBytes(java.nio.charset.StandardCharsets.UTF_8);var a=attachment(bytes);var e=d.delivery("1.1","NNA",List.of(a),false,1);
        assertThrows(FalloTecnico.class,()->d.receiver.guardarCompleta(e,d.files));assertThrows(IOException.class,()->d.files.guardar(e.id(),a,new ByteArrayInputStream(new byte[3])));assertThrows(IOException.class,()->d.files.guardar(e.id(),a,new ByteArrayInputStream(new byte[bytes.length])));
        assertFalse(d.files.disponible(e.id(),a));try(var entries=Files.list(dir.resolve("archivo/tmp"))){assertEquals(0,entries.count());}
        d.files.guardar(e.id(),a,new ByteArrayInputStream(bytes));d.receiver.guardarCompleta(e,d.files);Files.write(dir.resolve("archivo/objects/"+d.files.key(e.id(),a.id())+".bin"),new byte[bytes.length]);assertThrows(FalloTecnico.class,()->d.receiver.guardarCompleta(e,d.files));
    }
    @Test void reinicioRecuperaMismoComFolioFirmaYOriginalesSinCopiaFisica()throws Exception{
        var a=attachment(new byte[]{1,2,3});var e=d.delivery("1.1","ADULTO_DECLARADO",List.of(a),true,1);d.files.guardar(e.id(),a,new ByteArrayInputStream(new byte[]{1,2,3}));var ack=d.receiver.guardarCompleta(e,d.files);d.restart();assertEquals(ack,d.receiver.guardarCompleta(e,d.files));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document",Integer.class));assertTrue(d.files.disponible(e.id(),a));
        var dist=d.catalog.operador("demo.distribucion");d.core.distribuir(dist,UUID.randomUUID(),ack.registro().comunicacionId(),new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A","demo.jefatura.upr.a"))));var head=d.catalog.operador("demo.jefatura.upr.a");var destination=d.core.detalle(head,ack.registro().comunicacionId()).destinos().getFirst();
        d.core.abrirCaso(head,UUID.randomUUID(),destination.id(),new ContratosNucleo.Asignacion(destination.revision(),"demo.profesional.upr.a",null));destination=d.core.detalle(head,ack.registro().comunicacionId()).destinos().getFirst();d.core.abrirCaso(head,UUID.randomUUID(),destination.id(),new ContratosNucleo.Asignacion(destination.revision(),"demo.profesional2.upr.a",null));assertEquals(2,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document_case",Integer.class));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document",Integer.class));
        assertThrows(FalloTecnico.class,()->d.core.detalle(d.catalog.operador("demo.profesional.upd.a"),ack.registro().comunicacionId()));
    }
    @Test void falloSqlDespuesDeFsyncNoConfirmaRegistroYReintentoReutilizaOriginal()throws Exception{
        byte[] bytes={1,2,3};var a=attachment(bytes);var e=d.delivery("1.1","ADULTO_DECLARADO",List.of(a),true,1);d.files.guardar(e.id(),a,new ByteArrayInputStream(bytes));d.ddl("ALTER TABLE institution_document ADD CONSTRAINT test_block_documents CHECK (size_bytes<0)");
        try{assertThrows(RuntimeException.class,()->d.receiver.guardarCompleta(e,d.files));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Integer.class));assertTrue(d.files.disponible(e.id(),a));}finally{d.ddl("ALTER TABLE institution_document DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" test_block_documents");}
        var ack=d.receiver.guardarCompleta(e,d.files);assertNotNull(ack);assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));
    }
    static EntregaCompleta.Adjunto attachment(byte[] bytes)throws Exception{return new EntregaCompleta.Adjunto(UUID.randomUUID(),"sintetico.txt",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"text/plain","VALIDADO");}
    @Test void dosReceptoresConcurrentesReutilizanUnaComunicacion()throws Exception{
        {var e=d.delivery("1.1","NNA",List.of(),false,1);
            try(var workers=java.util.concurrent.Executors.newFixedThreadPool(2)){var start=new java.util.concurrent.CountDownLatch(1);var a=workers.submit(()->{start.await();return d.receiver.guardarCompleta(e,d.files);});var b=workers.submit(()->{start.await();return d.receiver.guardarCompleta(e,d.files);});start.countDown();assertEquals(a.get(),b.get());}
            assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));
        }
    }
    @Test void conservaInstitucionesConCamposOpcionalesNulosSinReinterpretarFirma(){
        var e=d.delivery("1.0","ADULTO_DECLARADO",List.of(),true,1);var tree=e.comunicacion().deepCopy();var institution=((tools.jackson.databind.node.ArrayNode)tree.path("datos").path("instituciones")).addObject();institution.put("nombre","Entidad sintética");institution.putNull("region");institution.putNull("comuna");
        var signed=d.sign(tree);var ack=d.receiver.guardarCompleta(signed,d.files);assertEquals(tree,d.receiver.completaRegistrada(ack.registro().comunicacionId()).comunicacion());
    }
    @Test void nivelRegistralNoPuedeAfirmarAutenticacionAunqueEsteFirmado(){
        var e=d.delivery("1.1","NNA",List.of(),false,1);var tree=e.comunicacion().deepCopy();var attribute=(tools.jackson.databind.node.ObjectNode)tree.path("personas").get(0).path("atributos").path("primerNombre");attribute.put("fuente","SRCEI");attribute.put("nivel","IDENTIDAD_AUTENTICADA");assertThrows(IllegalArgumentException.class,()->d.receiver.guardarCompleta(d.sign(tree),d.files));
        var cu=d.delivery("1.1","ADULTO_CU",List.of(),true,1).comunicacion().deepCopy();((tools.jackson.databind.node.ObjectNode)cu.path("personas").get(0).path("atributos").path("runNumero")).put("valor","87654321");assertThrows(IllegalArgumentException.class,()->d.receiver.guardarCompleta(d.sign(cu),d.files));
    }
}
