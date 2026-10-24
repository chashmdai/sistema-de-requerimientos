package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;import com.sun.net.httpserver.*;
import java.nio.file.*;import java.net.*;import java.io.*;import java.util.*;import java.security.*;import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;

/** Red loopback y productor controlado: no es la evidencia E2E de las dos aplicaciones reales. */
class RecuperarFormularioHttpTest {
    @TempDir Path dir;DatosFormularioTest d;HttpServer server;FuenteFormulario source;RecuperarFormulario job;EntregaCompleta delivery;UUID lease;
    final AtomicInteger acks=new AtomicInteger(),downloads=new AtomicInteger(),badHeaders=new AtomicInteger();final AtomicReference<RegistroInstitucional> firstAck=new AtomicReference<>();
    boolean lost,expired,broken,reject,large,stall;byte[] content="Archivo sintético institucional".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @BeforeEach void open()throws Exception{
        d=new DatosFormularioTest(dir);var file=FormularioCompletoInstitucionalTest.attachment(content);delivery=d.delivery("1.1","ADULTO_DECLARADO",List.of(file),true,1);lease=UUID.randomUUID();
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/integracion/v3/formulario-completo/entregas",this::handle);server.start();
        source=new FuenteFormulario(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"t".repeat(64),null,true);job=new RecuperarFormulario(source,d.receiver,d.files);
    }
    void handle(HttpExchange exchange)throws IOException{
        try{
            if(!("Bearer "+"t".repeat(64)).equals(exchange.getRequestHeaders().getFirst("Authorization"))){badHeaders.incrementAndGet();reply(exchange,401,new byte[0]);return;}
            String path=exchange.getRequestURI().getPath();
            if(path.endsWith("/reclamar")){if(!"1.0,1.1".equals(exchange.getRequestHeaders().getFirst("X-Formulario-Versiones")))badHeaders.incrementAndGet();var body=d.mapper.readTree(exchange.getRequestBody().readAllBytes());if(body.path("limite").asInt()!=1)badHeaders.incrementAndGet();reply(exchange,200,d.mapper.writeValueAsBytes(Map.of("entregas",List.of(new FuenteFormulario.Reserva(lease,System.currentTimeMillis()+(stall?2000:300000),1,delivery)))));}
            else if(path.contains("/adjuntos/")){downloads.incrementAndGet();if(!lease.toString().equals(exchange.getRequestHeaders().getFirst("X-Reserva"))){reply(exchange,409,new byte[0]);return;}if(stall){exchange.sendResponseHeaders(200,content.length);exchange.getResponseBody().write(content,0,1);exchange.getResponseBody().flush();try{Thread.sleep(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}else if(large){exchange.sendResponseHeaders(200,12L*1024*1024);byte[] buffer=new byte[65536];Arrays.fill(buffer,(byte)90);for(int i=0;i<192;i++)exchange.getResponseBody().write(buffer);}else{exchange.sendResponseHeaders(200,content.length);exchange.getResponseBody().write(broken?Arrays.copyOf(content,3):content);}}
            else if(path.endsWith("/confirmacion")){
                var body=d.mapper.readTree(exchange.getRequestBody().readAllBytes());var ack=d.mapper.treeToValue(body.path("recepcion"),RegistroInstitucional.class);
                assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));assertEquals(delivery.adjuntos().size(),d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document",Integer.class));
                delivery.validarRegistro(ack,d.own,d.mapper);var old=firstAck.getAndSet(ack);if(old!=null)assertEquals(old,ack);acks.incrementAndGet();
                if(reject)reply(exchange,403,new byte[0]);else if(expired)reply(exchange,409,new byte[0]);else if(lost&&acks.get()==1)reply(exchange,503,new byte[0]);else reply(exchange,204,new byte[0]);
            }else if(path.endsWith("/fallo"))reply(exchange,204,new byte[0]);else reply(exchange,404,new byte[0]);
        }finally{exchange.close();}
    }
    static void reply(HttpExchange x,int status,byte[] bytes)throws IOException{x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,status==204?-1:bytes.length);if(status!=204)x.getResponseBody().write(bytes);}
    @AfterEach void close()throws Exception{if(source!=null)source.close();if(server!=null)server.stop(0);if(d!=null)d.close();}
    @Test void negociaVersionesTransfiereOriginalYConfirmaSoloTrasCommit(){var r=job.ejecutar();assertEquals(1,r.confirmadas());assertEquals(1,acks.get());assertEquals(1,downloads.get());assertEquals(0,badHeaders.get());assertEquals(Boolean.FALSE,d.db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class));assertTrue(d.receiver.confirmacionesPendientes().isEmpty());}
    @Test void ackPerdidoYReinicioSeConcilianConElMismoRegistroSinDescargarOtraVez()throws Exception{
        lost=true;assertEquals(1,job.ejecutar().fallidas());var expected=firstAck.get();d.restart();d.now=d.now.plusSeconds(10);job=new RecuperarFormulario(source,d.receiver,d.files);assertEquals(1,job.conciliar());assertEquals(expected,firstAck.get());assertEquals(1,downloads.get());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));
        assertEquals(0,job.conciliar());assertEquals(2,acks.get());
    }
    @Test void reservaVencidaRequiereOtraReclamacionPeroNoOtroCom(){lost=true;job.ejecutar();var expected=firstAck.get();d.now=d.now.plusSeconds(10);expired=true;assertEquals(0,job.conciliar());assertTrue(d.receiver.confirmacionesCompletas().isEmpty());expired=false;lease=UUID.randomUUID();assertEquals(1,job.ejecutar().confirmadas());assertEquals(expected,firstAck.get());assertEquals(1,downloads.get());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));}
    @Test void respuestaDuplicadaReutilizaFolioYOriginal(){assertEquals(1,job.ejecutar().confirmadas());var first=firstAck.get();assertEquals(1,job.ejecutar().confirmadas());assertEquals(first,firstAck.get());assertEquals(1,downloads.get());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Integer.class));}
    @Test void transferenciaInterrumpidaNoRegistraNiConfirmaYSeRecupera(){broken=true;assertEquals(1,job.ejecutar().fallidas());assertEquals(0,acks.get());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));broken=false;assertEquals(1,job.ejecutar().confirmadas());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));}
    @Test void firmaInvalidaSeRechazaAntesDeDescargar(){delivery=new EntregaCompleta(delivery.comunicacion(),"0".repeat(64),delivery.huellaProcedencia(),delivery.custodia());assertEquals(1,job.ejecutar().fallidas());assertEquals(0,downloads.get());assertEquals(0,acks.get());}
    @Test void rechazoDeAckNuncaSeConvierteEnConfirmacion(){reject=true;assertEquals(1,job.ejecutar().fallidas());d.now=d.now.plusSeconds(10);assertEquals(0,job.conciliar());assertEquals(Boolean.TRUE,d.db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class));}
    @Test void originalAlteradoAntesDeConciliarNoEmiteAck(){lost=true;job.ejecutar();d.now=d.now.plusSeconds(10);var a=delivery.adjuntos().getFirst();try{Files.write(dir.resolve("archivo/objects/"+d.files.key(delivery.id(),a.id())+".bin"),new byte[content.length]);}catch(IOException e){throw new AssertionError(e);}assertEquals(0,job.conciliar());assertEquals(1,acks.get());}
    @Test void timeoutDeTransferenciaNoPublicaOriginalNiAck(){stall=true;assertEquals(1,job.ejecutar().fallidas());assertEquals(0,acks.get());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Integer.class));}
    @Test void transferenciaMultimediaMayorQueLimiteHistoricoSinAcumularOriginalEnMemoria()throws Exception{
        large=true;byte[] buffer=new byte[65536];Arrays.fill(buffer,(byte)90);var digest=MessageDigest.getInstance("SHA-256");for(int i=0;i<192;i++)digest.update(buffer);
        var a=new EntregaCompleta.Adjunto(UUID.randomUUID(),"sintetico12MiB.wav",12L*1024*1024,HexFormat.of().formatHex(digest.digest()),"audio/wav","VALIDADO");delivery=d.delivery("1.1","NNA",List.of(a),false,1);
        assertEquals(1,job.ejecutar().confirmadas());assertEquals(12L*1024*1024,Files.size(d.files.verificar(d.files.key(delivery.id(),a.id()),a)));assertEquals(1,downloads.get());
    }
    @Test void exigeHttpsParaOrigenNoExpresamenteLocal(){assertThrows(IllegalArgumentException.class,()->new FuenteFormulario(URI.create("http://127.0.0.1:9000"),"t".repeat(64),null,false));assertThrows(IllegalArgumentException.class,()->new FuenteFormulario(URI.create("https://usuario:clave@example.invalid"),"t".repeat(64),null,false));}
}
