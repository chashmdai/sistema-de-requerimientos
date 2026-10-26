import java.sql.*;import java.util.*;import tools.jackson.databind.json.JsonMapper;
/** Preflight de QA, solo SELECT/SHOW, credenciales efímeras por stdin desde DPAPI. */
public class WindowsQaConexion {
 public static void main(String[] args){var json=JsonMapper.builder().build();try{
  if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException();var data=json.readTree(System.in);String user=data.path("username").asString();if(!Set.of("siger_windows_qa_app","siger_windows_qa_migrator").contains(user))throw new IllegalStateException();
  String url="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";
  try(var c=DriverManager.getConnection(url,user,data.path("password").asString());var s=c.createStatement()){
   var out=new TreeMap<String,Object>();try(var r=s.executeQuery("SELECT CURRENT_USER(),DATABASE(),VERSION()")){r.next();if(!"siger_institucional_windows_qa".equals(r.getString(2)))throw new IllegalStateException();out.put("user",r.getString(1));out.put("schema",r.getString(2));out.put("version",r.getString(3));}
   try(var r=s.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_version'")){r.next();out.put("tls",r.getString(2));}
   var grants=new ArrayList<String>();try(var r=s.executeQuery("SHOW GRANTS")){while(r.next())grants.add(r.getString(1));}out.put("grants",grants);
   if(data.path("audit").asBoolean()){
    if(!user.equals("siger_windows_qa_app"))throw new IllegalStateException();
    var counts=new TreeMap<String,Long>();for(var table:List.of("staff_account","staff_assignment","core_communication","communication_registry","core_requirement","core_case","institution_task","institution_notification","notification_outbox","mail_attempt"))try(var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();counts.put(table,r.getLong(1));}
    try(var r=s.executeQuery("SELECT COUNT(*) FROM mail_attempt WHERE state='ACCEPTED_BY_GRAPH'")){r.next();out.put("graphAcceptedQa",r.getLong(1));}
    try(var r=s.executeQuery("SELECT COUNT(*) FROM mail_attempt WHERE state='SIMULADO'")){r.next();out.put("simulatedAttempts",r.getLong(1));}
    try(var r=s.executeQuery("SELECT COUNT(*)-COUNT(DISTINCT CONCAT(folio_prefix,':',numero)) FROM communication_registry")){r.next();out.put("duplicatedCom",r.getLong(1));}
    out.put("counts",counts);
   }
   System.out.println(json.writeValueAsString(out));
  }
 }catch(Exception e){System.out.println(json.writeValueAsString(Map.of("ok",false,"errorClass",e.getClass().getSimpleName())));System.exit(1);}}
}
