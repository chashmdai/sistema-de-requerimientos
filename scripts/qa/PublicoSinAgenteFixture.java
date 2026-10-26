package cl.nexbyte.siger.qa;

import cl.nexbyte.siger.formulario.*;import cl.nexbyte.siger.multimedia.*;import cl.nexbyte.siger.robustez.*;
import cl.nexbyte.siger.entrega.IntegracionSecurity;
import java.io.*;import java.nio.file.*;import java.security.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.springframework.boot.*;import org.springframework.boot.autoconfigure.*;import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.*;import org.springframework.context.annotation.*;import org.springframework.http.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;import tools.jackson.databind.json.JsonMapper;

/** Solo QA: servicios/controller/seguridad publicados, H2/almacén privados nuevos y datos sintéticos. Sin emisor ni retirada. */
@SpringBootConfiguration @EnableAutoConfiguration
@Import({IntegracionSecurity.class,IntegracionFormularioController.class,ErroresTecnicos.class,PublicoSinAgenteFixture.Control.class})
public class PublicoSinAgenteFixture {
 static Path root;static BaseJdbc db;static AlmacenPrivado store;static RecorridosJdbc journeys;static AdjuntosFormulario files;static CargasMultimedia media;static RecepcionFormularioJdbc receiver;static String token;
 static ConfigurableApplicationContext context;static final JsonMapper M=JsonMapper.builder().build();static final AtomicLong offset=new AtomicLong();
 static final Clock CLOCK=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.now().plusMillis(offset.get());}};
 static String mode="NORMAL";static String faultId;static final Map<String,Integer> downloads=new ConcurrentHashMap<>(),acks=new ConcurrentHashMap<>();
 static final Map<UUID,String> sessions=new ConcurrentHashMap<>();static final Map<UUID,UUID> attachments=new ConcurrentHashMap<>();
 @Bean RecepcionFormularioJdbc recepcion(){return receiver;}
 @Bean UserDetailsService noUsers(){return username->{throw new UsernameNotFoundException("Solo fixture técnico");};}
 @Bean @org.springframework.core.annotation.Order(3) SecurityFilterChain closed(HttpSecurity h)throws Exception{h.authorizeHttpRequests(a->a.anyRequest().denyAll());return h.build();}
 static String pem(PublicKey k){return "-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(k.getEncoded())+"\n-----END PUBLIC KEY-----\n";}
 static void write(String name,Object value)throws Exception{Files.write(root.resolve(name),M.writeValueAsBytes(value),StandardOpenOption.CREATE_NEW);Files.setPosixFilePermissions(root.resolve(name),java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}
 public static void main(String[] args)throws Exception{
  if(!System.getProperty("os.name").equals("Linux"))throw new IllegalArgumentException("Solo fixture POSIX");
  root=Path.of(args[0]).toAbsolutePath().normalize();if(!root.getParent().equals(Path.of("/home/nexbyteproveedor/.local/share/siger"))||!root.getFileName().toString().matches("sin-agente-[a-f0-9-]{36}")||Files.exists(root.resolve("ready.json")))throw new IllegalArgumentException("Solo raíz privada nueva");
  var institution=JdbcRecepcionConfiguration.publicKeys(M,root.resolve("institution-keys.json"));var signer=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
  token=HexFormat.of().formatHex(SecureRandom.getSeed(32));write("public-keys.json",Map.of("qa-publico-sin-agente",pem(signer.getPublic())));
  db=new BaseJdbc("jdbc:h2:file:"+root.resolve("publico")+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0","sa","",2);db.migrate("publico");new ControlesOperacion(db).cambiar(0,true,true,"NORMAL","PRUEBA_LOCAL");
  journeys=new RecorridosJdbc(db,M,r->{throw new AssertionError("Sin proveedor");},r->false,CLOCK,new byte[32],"ADULTO_QA_1","NNA_QA_1");
  store=new AlmacenPrivado(root.resolve("almacen"));
  files=new AdjuntosFormulario(db,journeys,b->true,store);
  var policy=new PoliticaMultimedia(12,262144,8192,131072,131072,131072,131072,268435456,0,60000);
  media=new CargasMultimedia(db,journeys,store,policy,p->true,(p,f)->new CargasMultimedia.Inspeccion(f.tipo(),"DESCARGA",null),CLOCK,M);
  receiver=new RecepcionFormularioJdbc(db,journeys,files,M,new ValidacionFormulario(new TerritorioFormulario(M)),"qa-publico-sin-agente",signer.getPrivate(),Map.of("qa-publico-sin-agente",signer.getPublic()),institution,CLOCK,true,true);
  String properties="server.address=127.0.0.1\nserver.port=0\nserver.ssl.enabled=true\nserver.ssl.key-store=file:"+root.resolve("https.p12")+"\nserver.ssl.key-store-type=PKCS12\nserver.ssl.key-store-password=synthetic-lab\nserver.ssl.key-alias=qa\npublico.integracion.token="+token+"\npublico.integracion.http-local=false\nciclo-vida.online-habilitado=false\nciclo-vida.eliminacion.habilitada=false\nspring.main.banner-mode=off\nlogging.level.root=ERROR\n";
  Files.writeString(root.resolve("fixture.properties"),properties,StandardOpenOption.CREATE_NEW);
  context=SpringApplication.run(PublicoSinAgenteFixture.class,"--spring.config.location=file:"+root.resolve("fixture.properties"));
  int port=Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));write("ready.json",Map.of("origin","https://localhost:"+port,"token",token,"agente",false,"retiroPublico",false));
  Runtime.getRuntime().addShutdownHook(new Thread(()->{try{media.close();store.close();db.close();}catch(Exception ignored){}}));
 }
 @RestController @RequestMapping("/integracion/qa-sin-agente")
 static class Control {
  @PostMapping("/crear") Map<String,Object> create(@RequestBody Map<String,String> input)throws Exception{
   String scenario=input.get("caso");if(!Set.of("normal-1.0","normal-1.1","ack-perdido-1.1","descarga-alterada-1.1").contains(scenario))throw new IllegalArgumentException();
   String session=HexFormat.of().formatHex(SecureRandom.getSeed(32));UUID journey=journeys.iniciar(IdentificacionCiudadana.Tipo.NNA,false,"NNA_QA_1",session,"127.0.0.1").id();
   journeys.identificar(journey,"presentador-nna",IdentificacionCiudadana.Rol.PRESENTADOR,null,null,session,"127.0.0.1",null);journeys.declarar(journey,"presentador-nna",Map.of("primerNombre","SINTETICO","primerApellido","SIN PERSONAS"),session);
   byte[] bytes=("EVIDENCIA TOTALMENTE SINTETICA "+scenario+"\n").repeat(64).getBytes(java.nio.charset.StandardCharsets.UTF_8);UUID attachment;
   if(scenario.endsWith("1.0"))attachment=files.reemplazar(journey,session,List.of(new AdjuntosFormulario.Archivo("sintetico.txt",bytes))).getFirst().id();
   else{
    String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));var load=media.iniciar(journey,session,new CargasMultimedia.Inicio("sintetico.txt",bytes.length,sha));
    media.bloque(journey,load.id(),session,0,sha,bytes.length,new ByteArrayInputStream(bytes));media.finalizar(journey,load.id(),session);
    long end=System.nanoTime()+5_000_000_000L;while(media.estado(journey,load.id(),session).estado().equals("ANALIZANDO")&&System.nanoTime()<end)Thread.sleep(20);
    if(!media.estado(journey,load.id(),session).estado().equals("VALIDADO"))throw new IllegalStateException("Fixture no preparado");media.confirmar(journey,session,new CargasMultimedia.Manifest(List.of(load.id()),media.lote(journey,session).revision()));attachment=load.id();
   }
   UUID id=UUID.randomUUID();receiver.recibir(id,journey,session,new DatosFormulario(Map.of("nna-region","13","nna-primary-method","unsafe","nna-story","RELATO SINTETICO SIN AGENTE"),List.of(),List.of(),List.of(attachment)));sessions.put(id,session);attachments.put(id,attachment);
   faultId=id.toString();mode=scenario.startsWith("ack-perdido")?"ACK_PERDIDO":scenario.startsWith("descarga-alterada")?"DESCARGA_ALTERADA":"NORMAL";
   var descriptor=db.sql.queryForMap("SELECT sha256 FROM citizen_attachment WHERE id=?",attachment.toString());var row=db.sql.queryForMap("SELECT body FROM citizen_submission WHERE id=?",id.toString());var e=M.readValue(row.get("body").toString(),EntregaFormulario.class);
   return Map.of("id",id,"version",e.comunicacion().version(),"contenido",e.sha256(),"adjuntoSha256",descriptor.get("sha256"));
  }
  @PostMapping("/modo") Map<String,Object> mode(@RequestBody Map<String,Object> input){String next=input.get("modo").toString();if(!Set.of("NORMAL","NO_DISPONIBLE").contains(next))throw new IllegalArgumentException();mode=next;if(input.containsKey("avanzarMs")){long n=((Number)input.get("avanzarMs")).longValue();if(n<0||n>10000)throw new IllegalArgumentException();offset.addAndGet(n);}return Map.of("ok",true);}
  @PostMapping("/estado") Map<String,Object> state(@RequestBody Map<String,String> input)throws Exception{
   UUID id=UUID.fromString(input.get("id"));if(!sessions.containsKey(id))throw new IllegalArgumentException();var row=db.sql.queryForMap("SELECT state,body,content_retired FROM citizen_submission WHERE id=?",id.toString());
   return Map.of("estado",row.get("state"),"bodyConservado",row.get("body")!=null,"retirada",row.get("content_retired"),"garantias",db.sql.queryForObject("SELECT COUNT(*) FROM custody_guarantee",Integer.class),"descargas",downloads.getOrDefault(id.toString(),0),"acks",acks.getOrDefault(id.toString(),0));
  }
  @PostMapping("/detener") Map<String,Object> stop(){Thread.ofVirtual().start(()->{try{Thread.sleep(300);context.close();}catch(Exception ignored){}});return Map.of("ok",true);}
 }
 static final class Buffered extends HttpServletResponseWrapper{
  final ByteArrayOutputStream bytes=new ByteArrayOutputStream();int code=200;
  Buffered(HttpServletResponse r){super(r);}
  public void setStatus(int n){code=n;}public int getStatus(){return code;}
  public ServletOutputStream getOutputStream(){return new ServletOutputStream(){public boolean isReady(){return true;}public void setWriteListener(WriteListener l){}public void write(int b)throws IOException{if(bytes.size()>=1048576)throw new IOException("Fixture acotado");bytes.write(b);}};}
  public void flushBuffer(){}public PrintWriter getWriter(){return new PrintWriter(bytes,true,java.nio.charset.StandardCharsets.UTF_8);}
 }
 @Bean FilterRegistrationBean<Filter> controlledFaults(){
  var registration=new FilterRegistrationBean<Filter>();registration.setOrder(100);registration.setFilter((request,response,chain)->{
   var q=(HttpServletRequest)request;var r=(HttpServletResponse)response;String path=q.getRequestURI(),base="/integracion/v3/formulario-completo/entregas";
   if(!path.startsWith(base)){chain.doFilter(request,response);return;}
   if(mode.equals("NO_DISPONIBLE")){r.setStatus(503);return;}
   String id=path.substring(base.length()).split("/").length>1?path.substring(base.length()).split("/")[1]:"";
   boolean download=path.contains("/adjuntos/"),ack=path.endsWith("/confirmacion");if(download)downloads.merge(id,1,Integer::sum);if(ack)acks.merge(id,1,Integer::sum);
   var buffered=new Buffered(r);chain.doFilter(q,buffered);byte[] bytes=buffered.bytes.toByteArray();int status=buffered.code;
   if(download&&mode.equals("DESCARGA_ALTERADA")&&id.equals(faultId)&&bytes.length>0&&status==200)bytes[0]^=1;
   if(ack&&mode.equals("ACK_PERDIDO")&&id.equals(faultId)&&acks.get(id)==1&&status==204)status=503;
   r.setStatus(status);if(status!=204){r.setContentLength(bytes.length);r.getOutputStream().write(bytes);}
  });return registration;
 }
}
