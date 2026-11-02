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
    @ParameterizedTest @CsvSource({"1.0,PASAPORTE,Perú","1.1,IDENTIDAD_EXTRANJERA,Francia","1.1,OTRO,Otra / situación especial: Apátrida"})
    void adultoExtranjeroSinRunConservaDocumentoTextualDeclaracionYComTrasReinicio(String version,String document,String nationality)throws Exception{
        var c=d.delivery(version,"ADULTO_DECLARADO",List.of(),false,1).comunicacion().deepCopy();
        var person=(tools.jackson.databind.node.ObjectNode)c.path("personas").get(0);var attrs=(tools.jackson.databind.node.ObjectNode)person.path("atributos");
        for(var entry:Map.of("tipoIdentificacion","DOCUMENTO_EXTRANJERO","nacionalidad",nationality,"tipoDocumento",document,"numeroDocumento","AB/123-xyz.45").entrySet()){var a=attrs.putObject(entry.getKey());a.put("valor",entry.getValue());a.putNull("codigo");a.put("fuente","DECLARACION_CIUDADANA");a.put("nivel","DECLARADO_NO_CONTRASTADO");a.put("observadoEn",d.now.toString());a.put("evidencia",UUID.randomUUID().toString());}
        var e=d.sign(c);var receipt=d.receiver.guardarCompleta(e,d.files);e.validarRegistro(receipt,d.own,d.mapper);d.restart();assertEquals(receipt,d.receiver.guardarCompleta(e,d.files));
        var stored=d.receiver.completaRegistrada(receipt.registro().comunicacionId());assertEquals(c,stored.comunicacion());assertEquals("AB/123-xyz.45",stored.comunicacion().path("personas").get(0).path("atributos").path("numeroDocumento").path("valor").asString());assertTrue(person.path("runNumero").isNull());assertTrue(person.path("runDv").isNull());assertFalse(person.path("autenticada").asBoolean());assertFalse(person.path("modulo11").asBoolean());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));
    }
    @ParameterizedTest @CsvSource({"1.0,grupal,1","1.1,grupal,1","1.1,mixta,2"})
    void gruposSeConservanComoCamposSinInventarPersonas(String version,String mode,int persons){
        var c=d.delivery(version,"ADULTO_DECLARADO",List.of(),true,persons).comunicacion().deepCopy();var fields=(tools.jackson.databind.node.ObjectNode)c.path("datos").path("campos");fields.put("motivo","vulneracion");fields.put("afectados-modalidad",mode);fields.put("grupo-nna-1-nombre","Curso sintético");fields.put("grupo-nna-1-descripcion","Contexto colectivo de prueba");fields.put("grupo-nna-1-cantidad","25");fields.put("grupo-nna-1-edades","12 a 14 años");fields.put("grupo-nna-1-region","13");fields.put("grupo-nna-1-comuna","Santiago");
        if(persons==2)((tools.jackson.databind.node.ObjectNode)c.path("personas").get(1)).put("rol","AFECTADO_NNA");
        var e=d.sign(c);var ack=d.receiver.guardarCompleta(e,d.files);e.validarRegistro(ack,d.own,d.mapper);var stored=d.core.detalle(d.catalog.operador("demo.distribucion"),ack.registro().comunicacionId()).contenido().formularioCompleto();assertEquals(c,stored);assertEquals(persons,stored.path("personas").size());assertEquals("25",stored.path("datos").path("campos").path("grupo-nna-1-cantidad").asString());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Integer.class));
    }
}
