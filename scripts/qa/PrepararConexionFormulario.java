import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.net.*;import java.security.*;import java.time.*;import java.util.*;
/** Diagnóstico Windows por stdin. No construye un recuperador ni abre una base de datos. */
class PrepararConexionFormulario {
 public static void main(String[] args){var json=EntregaCompleta.lector();try{
  if(!System.getProperty("os.name").startsWith("Windows")||args.length!=0)throw new IllegalArgumentException();
  var data=json.readTree(System.in);var origin=URI.create(data.path("origin").asString());
  if(!"https://formulario.defensorianinez.gob.cl".equals(origin.toString())||Arrays.stream(InetAddress.getAllByName(origin.getHost())).anyMatch(a->!a.isLoopbackAddress()))throw new IllegalArgumentException();
  var publicTrust=json.readTree(Files.readString(Path.of(data.path("publicTrust").asString())));if(!publicTrust.isObject()||publicTrust.isEmpty()||publicTrust.size()>20||!publicTrust.has(data.path("publicKeyId").asString()))throw new IllegalArgumentException();
  publicTrust.properties().forEach(e->{if(!e.getKey().matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException();ComprobanteDigital.publicPem(e.getValue().asString());});
  var trust=new HashMap<String,PublicKey>();data.path("institutionalTrust").properties().forEach(e->trust.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));
  var proof=new ComprobanteInstitucional.Contenido("1.0","SIGER_DDN","COMUNICACION_REGISTRADA",UUID.randomUUID(),UUID.randomUUID(),1,"COM-000001",Instant.now(),"FORMULARIO_WEB_PUBLICO","0".repeat(64),"0".repeat(64));
  String keyId=data.path("institutionalKeyId").asString();var key=ComprobanteDigital.privatePem(data.path("institutionalPrivateKey").asString());
  boolean trusted=proof.equals(ComprobanteInstitucional.firmar(proof,keyId,key,json).verificar(trust,json));
  try(var source=new FuenteFormulario(origin,data.path("token").asString(),Path.of(data.path("ca").asString()),false)){
   var connection=source.comprobarConexion();System.out.println(json.writeValueAsString(Map.of("scope","PREPARACION_SOLO_LECTURA_WINDOWS_WSL","connection",connection,"technicalAuthenticationReady",connection.preparada(),"institutionalSigningTrustedOffline",trusted,"publicSigningKeyConfigured",true,"publicRecoveryEnabled",false,"claims",0,"acksSent",0)));
   if(!connection.preparada()||!trusted)System.exit(2);
  }
 }catch(Exception e){var code=e instanceof FalloTecnico f?Objects.toString(f.code,"SONDA_NO_ACREDITADA"):"PREPARACION_NO_ACREDITADA";System.out.println(json.writeValueAsString(Map.of("ok",false,"code",code,"errorClass",e.getClass().getSimpleName(),"claims",0,"acksSent",0)));System.exit(1);}}
}
