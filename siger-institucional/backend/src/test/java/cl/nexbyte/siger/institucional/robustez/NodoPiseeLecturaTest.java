package cl.nexbyte.siger.institucional.robustez;
import com.sun.net.httpserver.HttpServer;import java.net.*;import java.util.concurrent.atomic.*;import org.junit.jupiter.api.Test;import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class NodoPiseeLecturaTest {
 @Test void contratoDeLecturaSinEnviosConLimiteDeFrecuenciaYPrevencionDeInyeccion()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/notificador/getProcedure",e->{assertThat(e.getRequestMethod()).isEqualTo("GET");var body="{\"procedures\":[]}".getBytes();e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});var signatures=new AtomicInteger();server.createContext("/notificador/getSignature",e->{assertThat(e.getRequestMethod()).isEqualTo("GET");signatures.incrementAndGet();var body="{\"signatures\":[]}".getBytes();e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});server.start();
  var ticks=new AtomicLong(1000000000L);
  try(var adapter=new NodoPiseeLectura(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),JsonMapper.builder().build(),ticks::get)){
   assertThat(adapter.getProcedure().datos().get("procedures").isArray()).isTrue();
   assertThatThrownBy(adapter::getSignature).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(429);
   ticks.addAndGet(249999999L);
   assertThatThrownBy(adapter::getSignature).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(429);
   assertThat(signatures.get()).isZero();ticks.incrementAndGet();assertThat(adapter.getSignature().datos().get("signatures").isArray()).isTrue();assertThat(signatures.get()).isEqualTo(1);
   assertThatThrownBy(()->adapter.messageStatus("../sendMessage")).isInstanceOf(IllegalArgumentException.class);
   assertThatThrownBy(()->adapter.getReceipt("id","11111111-1")).isInstanceOf(IllegalArgumentException.class);assertThat(signatures.get()).isEqualTo(1);
  }finally{server.stop(0);}
 }
 @Test void relojMonotonicoPuedeComenzarNegativoYCruzarElLimiteDelContador()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var reads=new AtomicInteger();server.createContext("/notificador/getProcedure",e->{reads.incrementAndGet();var body="{\"procedures\":[]}".getBytes();e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});server.start();
  try{
   for(long start:new long[]{-1000000000L,Long.MAX_VALUE-100000000L}){
    var ticks=new AtomicLong(start);try(var adapter=new NodoPiseeLectura(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),JsonMapper.builder().build(),ticks::get)){
     assertThat(adapter.getProcedure().datos().get("procedures").isArray()).isTrue();ticks.addAndGet(249999999L);assertThatThrownBy(adapter::getProcedure).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(429);
     ticks.incrementAndGet();assertThat(adapter.getProcedure().datos().get("procedures").isArray()).isTrue();
    }
   }
   assertThat(reads.get()).isEqualTo(4);
  }finally{server.stop(0);}
 }

}
