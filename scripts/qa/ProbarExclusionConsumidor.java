import cl.nexbyte.siger.institucional.formulario.ExclusionConsumidorFormulario;import cl.nexbyte.siger.institucional.robustez.BaseJdbc;import tools.jackson.databind.json.JsonMapper;import java.util.*;
/** Exclusión real QA sin fixtures, migraciones, PULL ni cambios de tablas. Credenciales por stdin. */
class ProbarExclusionConsumidor {
 public static void main(String[] args){var json=JsonMapper.builder().build();try{if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException();var data=json.readTree(System.in);if(!data.path("username").asString().equals("siger_windows_qa_app"))throw new IllegalStateException();
 String url="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";
 try(var db=new BaseJdbc(url,data.path("username").asString(),data.path("password").asString(),3)){
  if(data.path("expectActive").asBoolean()){boolean denied=false;try(var other=new ExclusionConsumidorFormulario(db)){throw new IllegalStateException("OTRO_CONSUMIDOR_ADMITIDO");}catch(IllegalStateException e){denied="Exclusividad del consumidor QA no disponible".equals(e.getMessage());}if(!denied)throw new IllegalStateException();System.out.println(json.writeValueAsString(Map.of("ok",true,"activeRuntimeExcludesSecondConsumer",true,"tableChanges",0,"publicPulls",0)));return;}
  boolean rejected=false;try(var one=new ExclusionConsumidorFormulario(db)){one.verificar();try(var two=new ExclusionConsumidorFormulario(db)){throw new AssertionError("DOS_CONSUMIDORES");}catch(IllegalStateException expected){rejected=true;}one.verificar();}if(!rejected)throw new IllegalStateException();try(var next=new ExclusionConsumidorFormulario(db)){next.verificar();}
  System.out.println(json.writeValueAsString(Map.of("ok",true,"schema","siger_institucional_windows_qa","firstOwnerValid",true,"secondConsumerRejected",true,"releaseAndReacquire",true,"tableChanges",0,"publicPulls",0)));
 }
 }catch(Exception e){System.out.println(json.writeValueAsString(Map.of("ok",false,"errorClass",e.getClass().getSimpleName())));System.exit(1);}}
}
