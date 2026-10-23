package cl.nexbyte.siger.institucional.robustez;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.Set;
import tools.jackson.databind.JsonNode;import tools.jackson.databind.json.JsonMapper;
/** Puerto de lectura Notificador. No envía mensajes ni descarga URLs de comprobantes. */
public final class NodoPiseeLectura implements AutoCloseable {
 private final URI origin;private final HttpClient http;private final JsonMapper json;private long next;
 public record Lectura(String operacion,JsonNode datos){@Override public String toString(){return "Lectura[operacion="+operacion+"]";}}
 public NodoPiseeLectura(URI origin,JsonMapper json){
  if(origin.getUserInfo()!=null||origin.getHost()==null||origin.getQuery()!=null||origin.getFragment()!=null||!(origin.getPath().isEmpty()||origin.getPath().equals("/"))
    ||!("https".equals(origin.getScheme())||("http".equals(origin.getScheme())&&Set.of("127.0.0.1","localhost","[::1]").contains(origin.getHost()))))throw new IllegalArgumentException("Nodo no seguro");
  this.origin=origin;this.json=json;http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 }
 public Lectura getProcedure(){return read("getProcedure","procedures");}
 public Lectura getSignature(){return read("getSignature","signatures");}
 public Lectura messageStatus(String id){return read("messageStatus/"+id(id),null);}
 public Lectura getReceipt(String id,String runSinDv){if(runSinDv==null||!runSinDv.matches("[0-9]{7,8}"))throw new IllegalArgumentException("RUN inválido");return read("getReceipt/"+id(id)+"/"+runSinDv,null);}
 private static String id(String value){if(value==null||!value.matches("[A-Za-z0-9_-]{1,120}"))throw new IllegalArgumentException("Identificador inválido");return value;}
 private synchronized void reserve(){long now=System.nanoTime();if(now<next)throw new FalloTecnico(429);next=now+250000000L;}
 private Lectura read(String operation,String array){
  reserve();try{
   var request=HttpRequest.newBuilder(origin.resolve("/notificador/"+operation)).timeout(Duration.ofSeconds(65)).header("Accept","application/json").GET().build();
   var response=http.send(request,HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(),1048576));
   if(response.statusCode()!=200)throw new FalloTecnico(response.statusCode());
   var data=json.readTree(response.body());if(data==null||!data.isObject()||(array!=null&&(!data.has(array)||!data.get(array).isArray())))throw new FalloTecnico(502);
   return new Lectura(operation.split("/")[0],data);
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new FalloTecnico(503);}catch(java.io.IOException|tools.jackson.core.JacksonException e){throw new FalloTecnico(502);}
 }
 public void close(){http.close();}
}
