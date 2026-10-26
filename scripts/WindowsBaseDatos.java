import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.io.*;
import org.flywaydb.core.Flyway;
import tools.jackson.databind.json.JsonMapper;

/** Operaciones acotadas al esquema autorizado. Credenciales solo por stdin anónimo desde DPAPI. */
public class WindowsBaseDatos {
 static final String SCHEMA="siger_institucional_windows_lab";
 static final JsonMapper JSON=JsonMapper.builder().build();
 static String url(){return "jdbc:mysql://127.0.0.1:3306/"+SCHEMA+"?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";}
 static List<String> tables(Connection c)throws Exception {var list=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='"+SCHEMA+"' AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history' ORDER BY table_name")){while(r.next())list.add(r.getString(1));}return list;}
 static Map<String,Object> fingerprint(Connection c)throws Exception{
  var all=new TreeMap<String,Object>();for(String table:tables(c)){
   var cols=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_schema='"+SCHEMA+"' AND table_name='"+table+"' ORDER BY ordinal_position")){while(r.next())cols.add(r.getString(1));}
   String expression=String.join(",",cols.stream().map(col->"IF(`"+col+"` IS NULL,'N',CONCAT('V',HEX(CAST(`"+col+"` AS BINARY))))").toList());
   var rows=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT SHA2(CONCAT_WS('|',"+expression+"),256) FROM `"+table+"`")){while(r.next())rows.add(r.getString(1));}Collections.sort(rows);
   var d=MessageDigest.getInstance("SHA-256");for(String row:rows)d.update((row+"\n").getBytes(StandardCharsets.US_ASCII));all.put(table,Map.of("count",rows.size(),"sha256",HexFormat.of().formatHex(d.digest())));
  }return all;
 }
 public static void main(String[] args){try{
  if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException();
  var request=JSON.readTree(System.in);String mode=args[0],user=request.path("username").asString(),password=request.path("password").asString();
  if(!Set.of("siger_windows_app","siger_windows_migrator").contains(user))throw new IllegalArgumentException();
  try(var c=DriverManager.getConnection(url(),user,password)){
   c.setAutoCommit(false);var result=new TreeMap<String,Object>();result.put("mode",mode);
   try(var s=c.createStatement();var r=s.executeQuery("SELECT CURRENT_USER(),DATABASE(),VERSION()")){r.next();if(!SCHEMA.equals(r.getString(2)))throw new IllegalStateException();result.put("user",r.getString(1));result.put("schema",r.getString(2));result.put("version",r.getString(3));}
   try(var s=c.createStatement();var r=s.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_version'")){r.next();result.put("tls",r.getString(2));}
   if(mode.equals("inspect")){
    var grants=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SHOW GRANTS")){while(r.next())grants.add(r.getString(1));}result.put("grants",grants);
    try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+SCHEMA+"'")){r.next();result.put("objects",r.getLong(1));}
   }else if(mode.equals("probe-ddl")){
    if(!user.equals("siger_windows_app"))throw new IllegalStateException();
    try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+SCHEMA+"' AND table_name='u45_app_ddl_probe'")){r.next();if(r.getInt(1)!=0)throw new IllegalStateException();}
    try(var s=c.createStatement()){s.execute("CREATE TABLE u45_app_ddl_probe(id INT PRIMARY KEY)");s.execute("DROP TABLE u45_app_ddl_probe");result.put("appDdlDenied",false);}catch(SQLException e){if(e.getErrorCode()!=1142)throw e;result.put("appDdlDenied",true);result.put("vendorCode",e.getErrorCode());}
   }else if(mode.equals("probe-view")){
    if(!user.equals("siger_windows_migrator")||!tables(c).isEmpty())throw new IllegalStateException();
    try(var s=c.createStatement()){s.execute("CREATE VIEW u45_privilege_probe AS SELECT 1 AS v");s.execute("DROP VIEW u45_privilege_probe");result.put("createView",true);}catch(SQLException e){result.put("createView",false);result.put("sqlState",e.getSQLState());result.put("vendorCode",e.getErrorCode());}
   }else if(mode.equals("migrate")){
    if(!user.equals("siger_windows_migrator"))throw new IllegalStateException();
    var grants=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SHOW GRANTS")){while(r.next())grants.add(r.getString(1));}var pending=Flyway.configure().dataSource(url(),user,password).locations("filesystem:"+args[1].replace('\\','/')).cleanDisabled(true).load().info().all();if(Arrays.stream(pending).anyMatch(i->i.getState().name().equals("PENDING")&&i.getVersion().getVersion().equals("9"))&&grants.stream().noneMatch(g->g.contains("CREATE VIEW")))throw new IllegalStateException("CREATE_VIEW_REQUIRED");
    var f=Flyway.configure().dataSource(url(),user,password).locations("filesystem:"+args[1].replace('\\','/')).cleanDisabled(true).validateMigrationNaming(true).load();var migrated=f.migrate();f.validate();result.put("migrationsExecuted",migrated.migrationsExecuted);
   }else if(mode.equals("plan")){
    var f=Flyway.configure().dataSource(url(),user,password).locations("filesystem:"+args[1].replace('\\','/' )).cleanDisabled(true).load();result.put("pendingMigrations",Arrays.stream(f.info().all()).filter(i->i.getState().name().equals("PENDING")).count());
   }else if(mode.equals("validate")){
    Flyway.configure().dataSource(url(),user,password).locations("filesystem:"+args[1].replace('\\','/')).cleanDisabled(true).load().validate();result.put("valid",true);
   }else if(mode.equals("audit")){
    var original=JSON.readTree(Files.readString(Path.of("C:/req/evidencias/flyway-origen.json")));var migrations=new ArrayList<Map<String,Object>>();
    try(var s=c.createStatement();var r=s.executeQuery("SELECT version,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank")){int i=0;while(r.next()){var expected=original.get(i++);if(!expected.path("version").asString().equals(r.getString(1))||!expected.path("script").asString().equals(r.getString(2))||expected.path("checksum").asInt()!=r.getInt(3)||!r.getBoolean(4))throw new IllegalStateException("FLYWAY_MISMATCH");migrations.add(Map.of("version",r.getString(1),"script",r.getString(2),"checksum",r.getInt(3),"success",true));}if(i!=12)throw new IllegalStateException();}result.put("migrations",migrations);
    try(var s=c.createStatement()){s.execute("SET SESSION information_schema_stats_expiry=0");}
    var increments=new TreeMap<String,Long>();try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name,auto_increment FROM information_schema.tables WHERE table_schema='"+SCHEMA+"' AND auto_increment IS NOT NULL ORDER BY table_name")){while(r.next())increments.put(r.getString(1),r.getLong(2));}result.put("autoIncrement",increments);
    String[] queries={"SELECT COUNT(*) FROM communication_registry","SELECT COUNT(DISTINCT numero) FROM communication_registry","SELECT COUNT(*) FROM core_case","SELECT COUNT(*) FROM core_requirement","SELECT COUNT(*) FROM staff_account WHERE username='nexbyte'","SELECT COUNT(DISTINCT a.role_code) FROM staff_assignment a JOIN staff_account s ON s.actor_id=a.actor_id WHERE s.username='nexbyte' AND a.revoked_ms IS NULL","SELECT COUNT(*) FROM mail_attempt WHERE state='ACCEPTED_BY_GRAPH' AND http_status=202","SELECT COUNT(*) FROM notification_outbox WHERE human_state='CONFIRMADA_POR_USUARIO'","SELECT sends_started FROM mail_campaign WHERE id='u4_20261025_v1'","SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=true","SELECT COUNT(*) FROM institution_task WHERE creator_id<>responsible_id OR resource_type<>'CASO'"};String[] keys={"communications","uniqueCom","cases","requirements","nexbyteAccounts","nexbyteRoles","acceptedGraphHistorical","humanConfirmedHistorical","campaignSendsStarted","ackPending","invalidTasks"};for(int i=0;i<queries.length;i++)try(var s=c.createStatement();var r=s.executeQuery(queries[i])){r.next();result.put(keys[i],r.getLong(1));}
   }else if(mode.equals("fingerprint")){
    c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);result.put("tables",fingerprint(c));
   }else throw new IllegalArgumentException();
   c.rollback();System.out.println(JSON.writeValueAsString(result));
  }
 }catch(Exception e){var safe=new TreeMap<String,Object>();safe.put("ok",false);safe.put("errorClass",e.getClass().getSimpleName());if(e instanceof IllegalStateException && "CREATE_VIEW_REQUIRED".equals(e.getMessage()))safe.put("blocker","CREATE_VIEW_REQUIRED");if(e instanceof SQLException s){safe.put("sqlState",s.getSQLState());safe.put("vendorCode",s.getErrorCode());}System.out.println(JSON.writeValueAsString(safe));System.exit(1);}}
}