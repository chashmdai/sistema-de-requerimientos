import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.robustez.*;
import com.sun.net.httpserver.*;
import java.net.*;import java.nio.file.*;import java.security.*;import java.time.*;import java.util.*;import javax.net.ssl.*;
import tools.jackson.databind.*;import tools.jackson.databind.json.JsonMapper;

/** Productor exclusivamente sintético HTTPS. Nunca incluido en el JAR SMR. */
class LocalProducer {
 final JsonMapper json=EntregaCompleta.lector();final Path root;final String token,keyId;final PrivateKey signer;final Map<String,PublicKey> ackTrust;
 final List<EntregaCompleta> queue=new ArrayList<>();final Map<UUID,UUID> leases=new HashMap<>();final Map<UUID,Long> expiry=new HashMap<>();
 LocalProducer(JsonNode p)throws Exception{root=Path.of(p.path("root").asString());Files.createDirectories(root);token=p.path("token").asString();keyId=p.path("keyId").asString();signer=ComprobanteDigital.privatePem(p.path("privatePem").asString());ackTrust=Map.of(p.path("ackKeyId").asString(),ComprobanteDigital.publicPem(p.path("ackPublicPem").asString()));
  String[] types={"NNA","NNA","ADULTO_DECLARADO","ADULTO_CU","ADULTO_DECLARADO","NNA"};for(int i=0;i<types.length;i++){
   Path file=root.resolve("entrega-"+i+".json");if(Files.notExists(file))Files.writeString(file,json.writeValueAsString(create(i,types[i],i%2==0&&i!=4?"1.0":"1.1")));queue.add(json.readValue(Files.readString(file),EntregaCompleta.class));
  }
 }
 JsonNode attribute(String value,String source){var a=json.createObjectNode();if(value==null)a.putNull("valor");else a.put("valor",value);a.putNull("codigo");a.put("fuente",source);a.put("nivel",source.equals("CLAVE_UNICA_USERINFO")?"IDENTIDAD_AUTENTICADA":"DECLARADO_NO_CONTRASTADO");a.put("observadoEn",Instant.now().toString());a.put("evidencia",UUID.randomUUID().toString());return a;}
 EntregaCompleta create(int index,String type,String version)throws Exception{
  var c=json.createObjectNode();c.put("version",version);c.put("id",UUID.randomUUID().toString());c.put("recorridoId",UUID.randomUUID().toString());c.put("tipo",type);c.put("recibidaEn",Instant.now().toString());c.put("avisoVersion","SMR_SINTETICO_1");c.put("aceptaCanalDigital",!type.equals("NNA"));c.putObject("ampliacionCompatible").put("unicode","ñ · María José · 漢字 · 🎈").putNull("valorNulo");
  var data=c.putObject("datos");var fields=data.putObject("campos");fields.put("motivo","consulta");fields.put(type.equals("NNA")?"nna-story":"consulta-general-detalle","Relato exclusivamente sintético. "+"Contenido completo con tildes y Unicode ñ 🎈. ".repeat(120));fields.put("campo-futuro-opcional","Conservado íntegro");var relations=data.putArray("personas");relations.addObject().put("personaId","presentador").putObject("detalles").put("relacion","Presentación sintética");var institutions=data.putArray("instituciones");institutions.addObject().put("nombre","Entidad sintética").put("tipo","PRUEBA").put("rol","REFERENCIA").putNull("region").putNull("comuna");
  var people=c.putArray("personas");var person=people.addObject();person.put("id","presentador");person.put("rol","PRESENTADOR");boolean cu=type.equals("ADULTO_CU");person.put("autenticada",cu);person.put("modulo11",cu);if(cu){person.put("runNumero","12345678");person.put("runDv","5");}else{person.putNull("runNumero");person.putNull("runDv");}person.put("estadoConsulta","NO_HABILITADO");person.putNull("consultadaEn");person.putNull("consultaRef");var attrs=person.putObject("atributos");var discrepancy=person.putObject("discrepancias");String source=cu?"CLAVE_UNICA_USERINFO":"DECLARACION_CIUDADANA";
  for(var entry:Map.of(cu?"nombres":"primerNombre","María José Sintética",cu?"apellidos":"primerApellido","De Prueba").entrySet())attrs.set(entry.getKey(),attribute(entry.getValue(),source));
  if(cu){attrs.set("runNumero",attribute("12345678",source));attrs.set("runDv",attribute("5",source));}else{attrs.set("segundoNombre",attribute(null,source));attrs.set("tipoIdentificacion",attribute("DOCUMENTO_EXTRANJERO",source));attrs.set("nacionalidad",attribute("Perú",source));attrs.set("numeroDocumento",attribute("SINTETICO-000",source));discrepancy.set("primerNombre",attribute("Antecedente sintético discrepante","SRCEI"));}
  var ids=data.putArray("adjuntos");var descriptors=c.putArray("adjuntos");int count=index==4?12:1;
  for(int n=0;n<count;n++){UUID id=UUID.randomUUID();byte[] bytes=("Original sintético SMR #"+index+"/"+n+" · ñ 🎈\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(root.resolve(id+".bin"),bytes);var a=new EntregaCompleta.Adjunto(id,"original-sintetico-"+n+".txt",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"text/plain","VALIDADO");ids.add(id.toString());descriptors.add(json.valueToTree(a));}
  var sources=json.createObjectNode();for(var p:c.path("personas"))sources.set(p.path("id").asString(),json.createArrayNode().add(p.path("atributos")).add(p.path("discrepancias")));String sha=EntregaCompleta.huella("CONTENIDO",c,json),provenance=EntregaCompleta.huella("PROCEDENCIA",sources,json);
  var proof=new ComprobanteDigital.Contenido("1.0",UUID.fromString(c.path("id").asString()),Instant.parse(c.path("recibidaEn").asString()),"CUSTODIA_PUBLICA",sha,provenance,"FORMULARIO_PUBLICO_DDN","SMR_SINTETICO_1");return new EntregaCompleta(c,sha,provenance,ComprobanteDigital.firmar(proof,keyId,signer,json));
 }
 void send(HttpExchange x,int status,Object body)throws java.io.IOException{if(body==null){x.sendResponseHeaders(status,-1);x.close();return;}byte[] bytes=json.writeValueAsBytes(body);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}}
 synchronized void handle(HttpExchange x)throws java.io.IOException{
  try{
   if(!("Bearer "+token).equals(x.getRequestHeaders().getFirst("Authorization"))){send(x,401,null);return;}
   String suffix=x.getRequestURI().getPath().substring("/integracion/v3/formulario-completo/entregas".length());
   if(suffix.equals("/reclamar")&&x.getRequestMethod().equals("POST")){
    if(!"1.0,1.1".equals(x.getRequestHeaders().getFirst("X-Formulario-Versiones"))||json.readTree(x.getRequestBody()).path("limite").asInt()!=1){send(x,400,null);return;}
    for(var e:queue)if(Files.notExists(root.resolve(e.id()+".ack.json"))&&expiry.getOrDefault(e.id(),0L)<=System.currentTimeMillis()){
     UUID lease=UUID.randomUUID();long until=System.currentTimeMillis()+300000;leases.put(e.id(),lease);expiry.put(e.id(),until);send(x,200,Map.of("entregas",List.of(new FuenteFormulario.Reserva(lease,until,1,e))));return;
    }send(x,200,Map.of("entregas",List.of()));return;
   }
   String[] parts=suffix.split("/");if(parts.length<3){send(x,404,null);return;}
   var e=queue.stream().filter(q->q.id().toString().equals(parts[1])).findFirst().orElse(null);if(e==null){send(x,404,null);return;}
   if(parts[2].equals("adjuntos")&&parts.length==4){var a=e.adjuntos().stream().filter(f->f.id().toString().equals(parts[3])).findFirst().orElse(null);if(a==null){send(x,404,null);return;}if(!Objects.equals(x.getRequestHeaders().getFirst("X-Reserva"),String.valueOf(leases.get(e.id())))||expiry.getOrDefault(e.id(),0L)<=System.currentTimeMillis()){send(x,409,null);return;}
    x.getResponseHeaders().set("Content-Type","application/octet-stream");x.getResponseHeaders().set("Content-Length",Long.toString(a.tamanio()));x.getResponseHeaders().set("ETag","\""+a.sha256()+"\"");x.sendResponseHeaders(200,x.getRequestMethod().equals("HEAD")?-1:a.tamanio());if(!x.getRequestMethod().equals("HEAD"))try(var out=x.getResponseBody()){Files.copy(root.resolve(a.id()+".bin"),out);}x.close();return;
   }
   var body=json.readTree(x.getRequestBody());Path ack=root.resolve(e.id()+".ack.json");
   if(parts[2].equals("confirmacion")){
    var receipt=json.treeToValue(body.path("recepcion"),RegistroInstitucional.class);e.validarRegistro(receipt,ackTrust,json);
    if(Files.exists(ack)){send(x,Files.readString(ack).equals(json.writeValueAsString(receipt))?204:409,null);return;}
    if(!Objects.equals(body.path("reserva").asString(),String.valueOf(leases.get(e.id())))||expiry.getOrDefault(e.id(),0L)<=System.currentTimeMillis()){send(x,409,null);return;}
    Files.writeString(ack,json.writeValueAsString(receipt),StandardOpenOption.CREATE_NEW);
    // Una respuesta perdida real de red, solo para el primer escenario sintético.
    if(e.id().equals(queue.getFirst().id())){x.close();return;}send(x,204,null);return;
   }
   if(parts[2].equals("fallo")){expiry.put(e.id(),System.currentTimeMillis()+2000);send(x,204,null);return;}
   send(x,404,null);
  }catch(Exception ex){send(x,503,null);}
 }
 public static void main(String[] args)throws Exception{var json=EntregaCompleta.lector();var p=json.readTree(System.in);var producer=new LocalProducer(p);var ks=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(Path.of(p.path("tls").asString()))){ks.load(in,p.path("tlsPassword").asString().toCharArray());}var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(ks,p.path("tlsPassword").asString().toCharArray());var ssl=SSLContext.getInstance("TLS");ssl.init(km.getKeyManagers(),null,null);var server=HttpsServer.create(new InetSocketAddress("127.0.0.1",p.path("port").asInt()),8);server.setHttpsConfigurator(new HttpsConfigurator(ssl));server.createContext("/integracion/v3/formulario-completo/entregas",producer::handle);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("PRODUCTOR_SINTETICO_HTTPS_LISTO");}
}
