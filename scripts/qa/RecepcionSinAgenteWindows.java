package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import java.io.*;import java.net.*;import java.net.http.*;import java.nio.file.*;import java.security.*;import java.time.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.json.JsonMapper;

/** QA opt-in: receptor REAL, MySQL QA nativo y NTFS nuevo. Sin agente, migraciones, workers ni servicios. */
public final class RecepcionSinAgenteWindows {
 static final String URL="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";
 static final JsonMapper M=EntregaCompleta.lector();static int checks;static final AtomicLong offset=new AtomicLong();
 static final Clock CLOCK=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.now().plusMillis(offset.get());}};
 static void check(boolean ok){if(!ok)throw new AssertionError("Comprobación E2E no satisfecha");checks++;}
 static Path root(String value)throws Exception{Path p=Path.of(value).toAbsolutePath().normalize();if(!System.getProperty("os.name").startsWith("Windows")||!p.getParent().equals(Path.of("C:\\req\\testdata"))||!p.getFileName().toString().matches("sin-agente-[a-f0-9-]{36}"))throw new IOException("Solo raíz QA nueva");ArchivoPrivado.validarAcceso(p);return p;}
 static String pem(Key key,String kind){return "-----BEGIN "+kind+" KEY-----\n"+Base64.getEncoder().encodeToString(key.getEncoded())+"\n-----END "+kind+" KEY-----\n";}
 static void write(Path p,byte[] value)throws Exception{Files.createFile(p);ArchivoPrivado.proteger(p);Files.write(p,value);}
 static Map<String,Object> snapshot(BaseJdbc db){var result=new TreeMap<String,Object>();for(String table:List.of("institutional_inbox","communication_registry","institution_document")){var rows=db.sql.queryForList("SELECT * FROM "+table+" ORDER BY "+(table.equals("communication_registry")?"numero":"id"));if(rows.size()>1000)throw new IllegalStateException("QA excesiva");result.put(table,Map.of("filas",rows.size(),"sha256",sha(M.writeValueAsBytes(rows))));}return result;}
 static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
 static Map<String,Object> baseline;static List<String> ours=new ArrayList<>();
 static void unchanged(BaseJdbc db){var actual=new TreeMap<String,Object>();for(String table:List.of("institutional_inbox","communication_registry","institution_document")){String column=table.equals("communication_registry")?"operacion_id":table.equals("institution_document")?"operation_id":"id";var rows=db.sql.queryForList("SELECT * FROM "+table+" ORDER BY "+(table.equals("communication_registry")?"numero":"id")).stream().filter(r->!ours.contains(r.get(column).toString())).toList();actual.put(table,Map.of("filas",rows.size(),"sha256",sha(M.writeValueAsBytes(rows))));}check(actual.equals(baseline));}
 static Map<String,String> secret()throws Exception{byte[] bytes=System.in.readNBytes(16385);if(bytes.length>16384)throw new IOException();try{var n=M.readTree(bytes);if(!"siger_windows_qa_app".equals(n.path("username").asString()))throw new IOException("Solo identidad QA app");return Map.of("username",n.path("username").asString(),"password",n.path("password").asString());}finally{Arrays.fill(bytes,(byte)0);}}
 static HttpClient http;static URI origin;static String token;
 static tools.jackson.databind.JsonNode qa(String action,Map<String,?> body)throws Exception{var request=HttpRequest.newBuilder(origin.resolve("/integracion/qa-sin-agente/"+action)).header("Authorization","Bearer "+token).header("Content-Type","application/json").timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofByteArray(M.writeValueAsBytes(body))).build();var r=http.send(request,HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()!=200)throw new IOException("Fixture no disponible");return M.readTree(r.body());}


 static PrivateKey key(Path root)throws Exception{Path p=ArchivoPrivado.validar(root.resolve("institution-private.pem"),16384);return ComprobanteDigital.privatePem(Files.readString(p));}
 static Map<String,PublicKey> publicMap(Path file)throws Exception{var n=M.readTree(Files.readString(file));var r=new HashMap<String,PublicKey>();n.properties().forEach(e->r.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));return Map.copyOf(r);}
 static RecepcionInstitucionalJdbc receiver(BaseJdbc db,Path p)throws Exception{return new RecepcionInstitucionalJdbc(db,M,publicMap(p.resolve("public-keys.json")),"qa-institucion-sin-agente",key(p),publicMap(p.resolve("institution-keys.json")),"COM",6,CLOCK);}
 static void verify(BaseJdbc db,ArchivoInstitucional files,RecepcionInstitucionalJdbc service,UUID id,tools.jackson.databind.JsonNode state)throws Exception{
  var rows=db.sql.queryForList("SELECT c.comunicacion_id,i.receipt FROM communication_registry c JOIN institutional_inbox i ON c.source=i.source AND c.operacion_id=i.id WHERE c.source='FORMULARIO_PUBLICO_DDN' AND c.operacion_id=?",id.toString());check(rows.size()==1);
  UUID com=UUID.fromString(rows.getFirst().get("comunicacion_id").toString());var e=service.completaRegistrada(com);check(e.id().equals(id));String version=state.has("caso")?(state.path("caso").asString().endsWith("1.0")?"1.0":"1.1"):state.path("version").asString();check(version.equals(e.comunicacion().path("version").asString()));var ack=service.registroCompletoConservado(e,files);e.validarRegistro(ack,publicMap(activeRoot.resolve("institution-keys.json")),M);
  check(ack.registro().folio().startsWith("COM-"));check(e.sha256().equals(state.path("contenido").asString()));check(e.adjuntos().size()==1);
  for(var a:e.adjuntos()){check(files.disponible(id,a));check(a.sha256().equals(state.path("adjuntoSha256").asString()));check(Files.size(files.verificar(files.key(id,a.id()),a))==a.tamanio());}
 }
 static Path activeRoot;
 public static void main(String[] args){
  try{run(args);}catch(Exception|AssertionError failure){System.out.println("{\"ok\":false,\"error\":\"PRUEBA_NO_COMPLETADA\",\"tipo\":\""+failure.getClass().getSimpleName()+"\"}");System.exit(1);}
 }
 static void run(String[] args)throws Exception{
  Path p=root(args[1]);activeRoot=p;String mode=args[0];if(mode.equals("e2e")&&Files.exists(p.resolve("resultado-privado.json")))throw new IOException("Run ya completado; usar offline");var credentials=secret();
  try(var db=new BaseJdbc(URL,credentials.get("username"),credentials.get("password"),2)){
   check("siger_institucional_windows_qa".equals(db.sql.queryForObject("SELECT DATABASE()",String.class)));
   check(!db.sql.queryForMap("SHOW SESSION STATUS LIKE 'Ssl_cipher'").get("Value").toString().isBlank());
   if(mode.equals("preflight")){
    check(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Integer.class)==0);
    baseline=snapshot(db);write(p.resolve("baseline.json"),M.writeValueAsBytes(baseline));
    var keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();write(p.resolve("institution-private.pem"),pem(keys.getPrivate(),"PRIVATE").getBytes(java.nio.charset.StandardCharsets.US_ASCII));write(p.resolve("institution-keys.json"),M.writeValueAsBytes(Map.of("qa-institucion-sin-agente",pem(keys.getPublic(),"PUBLIC"))));
    System.out.println("{\"ok\":true,\"fase\":\"preflight\",\"mysql\":\"8.0\",\"tlsVerificado\":true,\"qaPreexistenteSinModificar\":true}");return;
   }
   if(mode.equals("offline")){
    var state=M.readTree(Files.readString(p.resolve("resultado-privado.json")));ours=new ArrayList<>();for(var item:state.path("casos"))ours.add(item.path("id").asString());baseline=M.readValue(Files.readString(p.resolve("baseline.json")),new tools.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});
    try(var files=new ArchivoInstitucional(p.resolve("archivo"))){var service=receiver(db,p);for(var item:state.path("casos"))verify(db,files,service,UUID.fromString(item.path("id").asString()),item);unchanged(db);}
    System.out.println("{\"ok\":true,\"fase\":\"reinicio-sin-formulario\",\"originalesYComprobantesRecuperables\":true,\"agente\":false,\"comunicaciones\":4}");return;
   }
   if(!mode.equals("e2e"))throw new IllegalArgumentException();
   var config=M.readTree(Files.readString(p.resolve("conexion-publico.json")));origin=URI.create(config.path("origin").asString());if(!"https".equals(origin.getScheme())||!"localhost".equals(origin.getHost())||origin.getPort()<1024||Set.of(8080,8081,18080).contains(origin.getPort()))throw new IOException("Solo HTTPS QA");
   token=config.path("token").asString();Path ca=p.resolve("ca-publico.pem");var cert=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(Files.newInputStream(ca));var ks=KeyStore.getInstance(KeyStore.getDefaultType());ks.load(null,null);ks.setCertificateEntry("qa",cert);var tm=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());tm.init(ks);var tls=javax.net.ssl.SSLContext.getInstance("TLS");tls.init(null,tm.getTrustManagers(),null);http=HttpClient.newBuilder().sslContext(tls).followRedirects(HttpClient.Redirect.NEVER).build();
   try{Class.forName("cl.nexbyte.siger.custodia.AgenteCustodiaNativo");throw new AssertionError("Agente presente");}catch(ClassNotFoundException expected){checks++;}
   baseline=M.readValue(Files.readString(p.resolve("baseline.json")),new tools.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});
   var control=new ControlesOperacion(db);var before=control.estado();boolean changed=false;
   if(!before.aceptarNuevos()||!before.recuperar()||!before.contingencia().equals("NORMAL")){control.cambiar(before.revision(),true,true,"NORMAL","PRUEBA_LOCAL");changed=true;}
   var results=new ArrayList<Map<String,Object>>();
   try(var files=new ArchivoInstitucional(p.resolve("archivo"));var source=new FuenteFormulario(origin,token,ca,false)){
    var service=receiver(db,p);var job=new RecuperarFormulario(source,service,files);
    for(String scenario:List.of("normal-1.0","normal-1.1","ack-perdido-1.1","descarga-alterada-1.1")){
     var state=qa("crear",Map.of("caso",scenario));UUID id=UUID.fromString(state.path("id").asString());ours.add(id.toString());
     var first=job.ejecutar();
     if(scenario.startsWith("descarga-alterada")){
      check(first.fallidas()==1);check(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry WHERE operacion_id=?",Integer.class,id.toString())==0);check(qa("estado",Map.of("id",id)).path("acks").asInt()==0);
      qa("modo",Map.of("modo","NORMAL","avanzarMs",5000));check(job.ejecutar().confirmadas()==1);
     }else if(scenario.startsWith("ack-perdido")){
      check(first.fallidas()==1);check(Boolean.TRUE.equals(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox WHERE id=?",Boolean.class,id.toString())));
      String ack=db.sql.queryForObject("SELECT receipt FROM institutional_inbox WHERE id=?",String.class,id.toString());
      // Otras conexiones independientes confirman que no se está viendo una transacción provisional.
      try(var independent=new BaseJdbc(URL,credentials.get("username"),credentials.get("password"),1)){check(independent.sql.queryForObject("SELECT COUNT(*) FROM communication_registry WHERE operacion_id=?",Integer.class,id.toString())==1);}
      offset.addAndGet(5000);check(job.conciliar()==1);check(ack.equals(db.sql.queryForObject("SELECT receipt FROM institutional_inbox WHERE id=?",String.class,id.toString())));check(job.conciliar()==0);
     }else check(first.confirmadas()==1);
     verify(db,files,service,id,state);check(Boolean.FALSE.equals(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox WHERE id=?",Boolean.class,id.toString())));
     var publicState=qa("estado",Map.of("id",id));check(publicState.path("estado").asString().equals("CONFIRMADA"));check(publicState.path("bodyConservado").asBoolean());check(!publicState.path("retirada").asBoolean());check(publicState.path("garantias").asInt()==0);
     check(publicState.path("descargas").asInt()==(scenario.startsWith("descarga-alterada")?2:1));
     var e=service.completaRegistrada(UUID.fromString(db.sql.queryForObject("SELECT comunicacion_id FROM communication_registry WHERE operacion_id=?",String.class,id.toString())));var old=service.guardarCompleta(e,files);check(old.equals(service.guardarCompleta(e,files)));
     results.add(Map.of("id",id.toString(),"caso",scenario,"contenido",state.path("contenido").asString(),"adjuntoSha256",state.path("adjuntoSha256").asString()));
    }
    qa("modo",Map.of("modo","NO_DISPONIBLE"));boolean unavailable=false;try{job.ejecutar();}catch(FalloTecnico f){unavailable=f.status==503;}check(unavailable);
    for(var item:results)verify(db,files,service,UUID.fromString(item.get("id").toString()),M.valueToTree(item));
    check(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Integer.class)==0);unchanged(db);
    write(p.resolve("resultado-privado.json"),M.writeValueAsBytes(Map.of("casos",results)));
    var safe=Map.of("ok",true,"fase","e2e","comunicaciones",4,"documentos",4,"checks",checks,"agente",false,"retiroPublico",false,"mysql",true,"https",true,"runtimeCompleto",false);
    write(p.resolve("resultado.json"),M.writeValueAsBytes(safe));System.out.println(M.writeValueAsString(safe));
   }finally{if(changed){var current=control.estado();control.cambiar(current.revision(),before.aceptarNuevos(),before.recuperar(),before.contingencia(),"PRUEBA_LOCAL");}}
  }
 }
}
