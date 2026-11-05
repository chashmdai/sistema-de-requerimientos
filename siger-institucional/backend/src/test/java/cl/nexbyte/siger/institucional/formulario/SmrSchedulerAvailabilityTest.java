package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.ControlesOperacion;import java.nio.file.*;import java.net.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class SmrSchedulerAvailabilityTest {
 @TempDir Path dir;
 @Test void estadoNoSeBloqueaCuandoElOrigenDemoraUnaReclamacion()throws Exception{
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/integracion/v3/formulario-completo/entregas/reclamar",exchange->{entered.countDown();try{release.await();byte[] json="{\"entregas\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,json.length);try(var out=exchange.getResponseBody()){out.write(json);}}catch(InterruptedException e){Thread.currentThread().interrupt();exchange.close();}});server.start();
  try(var d=new DatosFormularioTest(dir);var source=new FuenteFormulario(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"t".repeat(64),null,true);var executor=Executors.newVirtualThreadPerTaskExecutor()){
   var scheduler=new FormularioInstitucionalConfiguration.Planificador(new RecuperarFormulario(source,d.receiver,d.files),new ControlesOperacion(d.db));var cycle=executor.submit(scheduler::tick);
   try{assertTrue(entered.await(5,TimeUnit.SECONDS));var status=executor.submit(scheduler::estado);assertEquals("RECEPCION_EN_CURSO",status.get(2,TimeUnit.SECONDS).estado());}finally{release.countDown();}
   cycle.get(5,TimeUnit.SECONDS);
  }finally{release.countDown();server.stop(0);}
 }
}
