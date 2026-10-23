package cl.nexbyte.siger.institucional.robustez;
import com.sun.net.httpserver.HttpServer;import java.net.*;import org.junit.jupiter.api.Test;import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class NodoPiseeLecturaTest {
 @Test void contratoDeLecturaSinEnviosConLimiteDeFrecuenciaYPrevencionDeInyeccion()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/notificador/getProcedure",e->{assertThat(e.getRequestMethod()).isEqualTo("GET");var body="{\"procedures\":[]}".getBytes();e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});server.start();
  try(var adapter=new NodoPiseeLectura(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),JsonMapper.builder().build())){
   assertThat(adapter.getProcedure().datos().get("procedures").isArray()).isTrue();
   assertThatThrownBy(adapter::getSignature).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(429);
   assertThatThrownBy(()->adapter.messageStatus("../sendMessage")).isInstanceOf(IllegalArgumentException.class);
   assertThatThrownBy(()->adapter.getReceipt("id","11111111-1")).isInstanceOf(IllegalArgumentException.class);
  }finally{server.stop(0);}
 }
}
