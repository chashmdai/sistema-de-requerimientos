import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.formulario.*;
import java.nio.file.*;import java.util.*;import java.time.*;import java.security.*;import org.flywaydb.core.Flyway;
class VerifyRestore {
 public static void main(String[] args)throws Exception{var json=EntregaCompleta.lector();var p=json.readTree(System.in);var pub=new HashMap<String,PublicKey>();for(var entry:json.readTree(Files.readString(Path.of(p.path("producerTrust").asString()))).properties())pub.put(entry.getKey(),ComprobanteDigital.publicPem(entry.getValue().asString()));var own=new HashMap<String,PublicKey>();for(var entry:json.readTree(Files.readString(Path.of(p.path("ownTrust").asString()))).properties())own.put(entry.getKey(),ComprobanteDigital.publicPem(entry.getValue().asString()));
  try(var db=new BaseJdbc(p.path("url").asString(),p.path("user").asString(),p.path("password").asString(),2);var files=new ArchivoInstitucional(Path.of(p.path("store").asString()))){
   Flyway.configure().dataSource(db.sql.getDataSource()).locations("classpath:db/institucional/mysql").load().validate();
   var receiver=new RecepcionInstitucionalJdbc(db,json,pub,p.path("keyId").asString(),ComprobanteDigital.privatePem(p.path("privatePem").asString()),own,"COM",6,Clock.systemUTC());int records=0,originals=0;
   for(var row:db.sql.queryForList("SELECT comunicacion_id FROM communication_registry ORDER BY numero")){var e=receiver.completaRegistrada(UUID.fromString(row.get("comunicacion_id").toString()));receiver.registroCompletoConservado(e,files);records++;originals+=e.adjuntos().size();}
   String schema=db.sql.queryForObject("SELECT DATABASE()",String.class),user=p.path("user").asString();boolean excluded=false;
   try(var first=new ExclusionConsumidorFormulario(db,schema,user,p.path("origin").asString())){first.verificar();ExclusionConsumidorFormulario second=null;try{second=new ExclusionConsumidorFormulario(db,schema,user,p.path("origin").asString());}catch(IllegalStateException expected){excluded=true;}finally{if(second!=null)second.close();}}
   if(!excluded)throw new IllegalStateException();
   System.out.println(json.writeValueAsString(Map.of("records",records,"originals",originals,"signaturesAndHashesVerified",true,"exclusiveConsumerVerified",true,"pendingAck",db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Long.class),"flywayValidated",true,"publicPullPerformed",false)));
  }
 }
}
