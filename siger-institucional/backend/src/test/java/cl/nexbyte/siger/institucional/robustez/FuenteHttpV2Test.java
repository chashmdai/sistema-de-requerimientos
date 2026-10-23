package cl.nexbyte.siger.institucional.robustez;
import com.sun.net.httpserver.HttpServer;import java.net.*;import java.time.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class FuenteHttpV2Test {
 HttpServer server;ExecutorService threads;
 @AfterEach void close(){if(server!=null)server.stop(0);if(threads!=null)threads.close();}
 FuenteHttpV2 source(com.sun.net.httpserver.HttpHandler handler)throws Exception{
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);threads=Executors.newVirtualThreadPerTaskExecutor();server.setExecutor(threads);server.createContext("/",handler);server.start();
  return new FuenteHttpV2(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"t".repeat(40),JsonMapper.builder().build(),true,Duration.ofMillis(350));
 }
 @Test void protocoloYCredencialSeTransmitenSoloEnBackend()throws Exception{
  try(var client=source(e->{assertThat(e.getRequestMethod()).isEqualTo("POST");assertThat(e.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer "+"t".repeat(40));assertThat(new String(e.getRequestBody().readAllBytes())).contains("limite");byte[] bytes="{\"entregas\":[]}".getBytes();e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();})){assertThat(client.reclamar()).isEmpty();}
 }
 @Test void respuestaGiganteSeCancela()throws Exception{
  try(var client=source(e->{byte[] bytes=new byte[600000];e.sendResponseHeaders(200,bytes.length);try{e.getResponseBody().write(bytes);}catch(java.io.IOException ignored){}e.close();})){assertThatThrownBy(client::reclamar).isInstanceOf(FalloTecnico.class);}
 }
 @Test void timeoutNoBloqueaElPlanificadorIndefinidamente()throws Exception{
  try(var client=source(e->{try{Thread.sleep(900);}catch(InterruptedException x){Thread.currentThread().interrupt();}e.close();})){var start=System.nanoTime();assertThatThrownBy(client::reclamar).isInstanceOf(FalloTecnico.class);assertThat(Duration.ofNanos(System.nanoTime()-start)).isLessThan(Duration.ofSeconds(2));}
 }
 @Test void noSigueRedirectConCredenciales()throws Exception{
  try(var client=source(e->{e.getResponseHeaders().set("Location","http://127.0.0.1:1/robo");e.sendResponseHeaders(302,-1);e.close();})){assertThatThrownBy(client::reclamar).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(302);}
 }
}
