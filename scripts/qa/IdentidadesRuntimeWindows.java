import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
import tools.jackson.databind.json.JsonMapper;
/** Provision QA idempotente: sin DDL, sin reemplazar contrasenas ni tocar el historico. */
public class IdentidadesRuntimeWindows {
 public static void main(String[] args){var json=JsonMapper.builder().build();try{
  if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException();
  var input=json.readTree(System.in);if(!"siger_windows_qa_app".equals(input.path("user").asString()))throw new IllegalArgumentException();
  String url="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";
  try(var db=new BaseJdbc(url,input.path("user").asString(),input.path("password").asString(),2)){
   if(!"siger_windows_qa_app@localhost".equals(db.sql.queryForObject("SELECT CURRENT_USER()",String.class))||!"siger_institucional_windows_qa".equals(db.sql.queryForObject("SELECT DATABASE()",String.class))||db.sql.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version='12' AND success=TRUE",Integer.class)!=1)throw new IllegalStateException();
   new CatalogoLaboratorio().instalar(db);var ids=new IdentidadInstitucional(db,Clock.systemUTC());
   String admin;
   var ti=db.sql.queryForList("SELECT s.actor_id FROM staff_account s JOIN staff_assignment a ON a.actor_id=s.actor_id WHERE s.enabled=TRUE AND a.role_code='TI' AND a.revoked_ms IS NULL AND a.valid_from<=? AND (a.valid_until IS NULL OR a.valid_until>?)",System.currentTimeMillis(),System.currentTimeMillis());
   if(ti.isEmpty())admin=ids.bootstrap(new IdentidadInstitucional.Crear("nexbyte","Nexbyte Proveedor","nexbyte.proveedor@defensorianinez.cl","Laboratorio Windows QA","UPR","DEMO_SEDE_A",input.path("nexbytePassword").asString())).id();else admin=ti.getFirst().get("actor_id").toString();
   var nex=ids.cuentas(admin).stream().filter(c->c.usuario().equals("nexbyte")).findFirst().orElse(null);
   if(nex==null)nex=ids.crear(admin,new IdentidadInstitucional.Crear("nexbyte","Nexbyte Proveedor","nexbyte.proveedor@defensorianinez.cl","Laboratorio Windows QA","UPR","DEMO_SEDE_A",input.path("nexbytePassword").asString()));
   for(String role:List.of("TI","DISTRIBUCION","DIRECTOR","COORDINADOR","COORDINADOR_REGIONAL","PROFESIONAL","ADMINISTRATIVO"))if(ids.accesos(nex.id()).stream().noneMatch(a->a.rol().equals(role))){boolean global=Set.of("TI","DISTRIBUCION").contains(role),regional=role.equals("COORDINADOR_REGIONAL");ids.otorgar(admin,nex.id(),new IdentidadInstitucional.Otorgar(role,global?"INSTITUCION":regional?"SEDE":"UNIDAD_SEDE",global||regional?null:"UPR",global?null:"DEMO_SEDE_A",Instant.now().minusSeconds(1),null));}
   for(String name:List.of("qa.windows.ti","qa.windows.otra"))if(ids.cuentas(admin).stream().noneMatch(c->c.usuario().equals(name))){boolean onlyTi=name.endsWith("ti");var c=ids.crear(admin,new IdentidadInstitucional.Crear(name,"Funcionario sintetico Windows QA",name+"@example.invalid","Pruebas negativas",onlyTi?null:"UPD",onlyTi?null:"DEMO_SEDE_A",input.path(name).asString()));ids.otorgar(admin,c.id(),new IdentidadInstitucional.Otorgar(onlyTi?"TI":"PROFESIONAL",onlyTi?"INSTITUCION":"UNIDAD_SEDE",onlyTi?null:"UPD",onlyTi?null:"DEMO_SEDE_A",Instant.now().minusSeconds(1),null));}
   System.out.println(json.writeValueAsString(Map.of("qa",true,"nexbyteId",nex.id(),"nexbyteRoles",ids.accesos(nex.id()).stream().map(a->a.rol()).distinct().sorted().toList(),"restricted",List.of("qa.windows.ti","qa.windows.otra"),"passwordsReplaced",0)));
  }
 }catch(Exception failure){System.out.println(json.writeValueAsString(Map.of("ok",false,"errorClass",failure.getClass().getSimpleName())));System.exit(1);}}
}
