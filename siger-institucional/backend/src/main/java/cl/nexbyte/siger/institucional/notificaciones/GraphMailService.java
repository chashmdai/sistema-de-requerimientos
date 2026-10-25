package cl.nexbyte.siger.institucional.notificaciones;
import java.net.*;import java.net.http.*;import java.nio.file.*;import java.nio.file.attribute.PosixFilePermissions;import java.nio.charset.StandardCharsets;import java.time.*;import java.io.*;import java.util.*;import tools.jackson.databind.json.JsonMapper;
/** Adaptado del GraphMailService institucional mínimo: client_credentials + sendMail. Sin trazas HTTP ni reintentos implícitos. */
public final class GraphMailService implements TransporteCorreo {
 public record Ajustes(String tenant,String client,String remitente,Path secreto){@Override public String toString(){return "Ajustes[reservados]";}}
 public record Respuesta(int estado,byte[] cuerpo,String retryAfter,String requestId){@Override public String toString(){return "Respuesta[HTTP="+estado+"]";}}
 @FunctionalInterface public interface Conexion {Respuesta post(URI uri,Map<String,String> headers,byte[] body)throws IOException,InterruptedException;}
 private final Ajustes settings;private final Conexion conexion;private final JsonMapper json;private final Clock clock;private String token;private long tokenUntil;
 public GraphMailService(Ajustes settings,JsonMapper json,Clock clock){this(settings,json,clock,jdk());}
 public GraphMailService(Ajustes settings,JsonMapper json,Clock clock,Conexion conexion){if(!settings.tenant().matches("[a-fA-F0-9-]{36}")||!settings.client().matches("[a-fA-F0-9-]{36}")||!settings.remitente().matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"))throw new IllegalArgumentException("Configuración Graph inválida");this.settings=settings;this.conexion=conexion;this.json=json;this.clock=clock;}
 private static Conexion jdk(){
  var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
  return (uri,headers,body)->{var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).POST(HttpRequest.BodyPublishers.ofByteArray(body));headers.forEach(builder::header);var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());try(var input=response.body()){byte[] bytes=input.readNBytes(32769);if(bytes.length>32768)throw new IOException("Respuesta fuera de límites");return new Respuesta(response.statusCode(),bytes,response.headers().firstValue("Retry-After").orElse(null),response.headers().firstValue("request-id").orElse(null));}};
 }
 private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 private String secreto()throws IOException{
  Path p=settings.secreto().toAbsolutePath().normalize();if(!p.equals(p.toRealPath())||Files.isSymbolicLink(p)||!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||Files.size(p)>1024||!Files.getPosixFilePermissions(p).equals(PosixFilePermissions.fromString("rw-------")))throw new IOException("Custodia no válida");
  String value=Files.readString(p).strip();if(value.length()<20||value.length()>512||value.chars().anyMatch(Character::isISOControl))throw new IOException("Credencial no válida");return value;
 }
 private synchronized Respuesta obtener()throws IOException,InterruptedException{
  if(token!=null&&tokenUntil>clock.millis())return null;
  String form="client_id="+encode(settings.client())+"&client_secret="+encode(secreto())+"&grant_type=client_credentials&scope="+encode("https://graph.microsoft.com/.default");
  var response=conexion.post(URI.create("https://login.microsoftonline.com/"+settings.tenant()+"/oauth2/v2.0/token"),Map.of("Content-Type","application/x-www-form-urlencoded"),form.getBytes(StandardCharsets.UTF_8));
  if(response.estado()!=200)return response;
  var data=json.readTree(response.cuerpo());String access=data.path("access_token").asString("");long seconds=data.path("expires_in").asLong(0);
  if(access.isBlank()||access.length()>16000||access.chars().anyMatch(Character::isISOControl)||seconds<60)throw new IOException("Token no válido");
  token=access;tokenUntil=clock.millis()+Math.min(3600,seconds-30)*1000;return null;
 }
 private static String safeId(String id){return id!=null&&id.matches("[a-fA-F0-9-]{36}")?id:null;}
 private static long espera(String value){try{return Math.max(1,Math.min(86400,Long.parseLong(value)));}catch(Exception ignored){return 60;}}
 public Resultado enviar(Mensaje m){
  if(m.destinatario()==null||!m.destinatario().matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+")||!m.correlacion().matches("[a-fA-F0-9-]{36}")||!m.asunto().startsWith("[SIGER LAB]")||m.asunto().length()>180)return new Resultado("RECHAZADO","VALIDACION",null,0,null);
  try{var failure=obtener();if(failure!=null)return new Resultado(failure.estado()==429||failure.estado()>=500?"REINTENTABLE":"RECHAZADO","TOKEN",failure.estado(),espera(failure.retryAfter()),safeId(failure.requestId()));}
  catch(Exception failure){if(failure instanceof InterruptedException)Thread.currentThread().interrupt();return new Resultado("REINTENTABLE","TOKEN",null,60,null);}
  try{
   var payload=Map.of("message",Map.of("subject",m.asunto(),"body",Map.of("contentType","Text","content",m.texto()),"toRecipients",List.of(Map.of("emailAddress",Map.of("address",m.destinatario()))),"internetMessageHeaders",List.of(Map.of("name","X-SIGER-Correlation","value",m.correlacion()))),"saveToSentItems",true);
   var response=conexion.post(URI.create("https://graph.microsoft.com/v1.0/users/"+encode(settings.remitente())+"/sendMail"),Map.of("Authorization","Bearer "+token,"Content-Type","application/json","client-request-id",m.correlacion(),"return-client-request-id","true"),json.writeValueAsBytes(payload));
   int status=response.estado();if(status==401){synchronized(this){token=null;tokenUntil=0;}}
   String state=status==202?"ACCEPTED_BY_GRAPH":status==429?"REINTENTABLE":status>=400&&status<500?"RECHAZADO":"AMBIGUO";
   return new Resultado(state,"GRAPH",status,espera(response.retryAfter()),safeId(response.requestId()));
  }catch(Exception failure){if(failure instanceof InterruptedException)Thread.currentThread().interrupt();return new Resultado("AMBIGUO","GRAPH",null,0,null);}
 }
 public boolean real(){return true;}
 @Override public String toString(){return "GraphMailService[credenciales protegidas]";}
}
