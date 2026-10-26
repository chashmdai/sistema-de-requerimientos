import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import tools.jackson.databind.*;

/** Importacion transaccional de snapshot DPAPI; nunca consume una cola ni ejecuta Spring. */
public class WindowsImportar {
 static String stage="REQUEST";
 static final Set<String> SEEDED=Set.of("core_link_guard","identity_guard","notification_guard","operation_control","org_macrozone","org_region");
 static final Path EVIDENCE=Path.of("C:/req/evidencias");
 static JsonNode fingerprint(Connection c)throws Exception{return WindowsBaseDatos.JSON.valueToTree(WindowsBaseDatos.fingerprint(c));}
 static void foreignKeys(Connection c)throws Exception{
  var groups=new TreeMap<String,List<String[]>>();
  try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name,constraint_name,column_name,referenced_table_name,referenced_column_name FROM information_schema.key_column_usage WHERE table_schema='"+WindowsBaseDatos.SCHEMA+"' AND referenced_table_name IS NOT NULL ORDER BY table_name,constraint_name,ordinal_position")){
   while(r.next()){var v=new String[]{r.getString(1),r.getString(3),r.getString(4),r.getString(5)};groups.computeIfAbsent(r.getString(1)+":"+r.getString(2),k->new ArrayList<>()).add(v);}
  }
  for(var cols:groups.values()){
   var first=cols.getFirst();String join=String.join(" AND ",cols.stream().map(x->"c.`"+x[1]+"`=p.`"+x[3]+"`").toList());String nonnull=String.join(" AND ",cols.stream().map(x->"c.`"+x[1]+"` IS NOT NULL").toList());
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM `"+first[0]+"` c LEFT JOIN `"+first[2]+"` p ON "+join+" WHERE "+nonnull+" AND p.`"+first[3]+"` IS NULL")){r.next();if(r.getLong(1)!=0)throw new IllegalStateException("FOREIGN_KEY_MISMATCH");}
  }
 }
 public static void main(String[] args){try{
  if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException();
  var json=WindowsBaseDatos.JSON;var request=json.readTree(System.in);
  if(!request.path("username").asString().equals("siger_windows_migrator"))throw new IllegalArgumentException();
  byte[] raw=Base64.getDecoder().decode(request.path("snapshot").asString());if(raw.length>4*1024*1024)throw new IllegalArgumentException();
  var source=json.readTree(Files.readString(EVIDENCE.resolve("sql-snapshot-origen.json")));String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));if(!sha.equals(source.path("snapshotSha256").asString()))throw new IllegalStateException("SNAPSHOT_INTEGRITY");
  var expected=(tools.jackson.databind.node.ObjectNode)source.path("tables").deepCopy();expected.remove("flyway_schema_history");
  stage="METADATA";var columns=new TreeMap<String,List<String>>();for(var col:source.path("columns"))columns.computeIfAbsent(col.path("table").asString(),k->new ArrayList<>()).add(col.path("column").asString());columns.remove("flyway_schema_history");
  for(var entry:columns.entrySet())if(!entry.getKey().matches("[a-z_][a-z0-9_]*")||entry.getValue().stream().anyMatch(x->!x.matches("[a-z_][a-z0-9_]*")))throw new IllegalArgumentException();
  String password=request.path("password").asString();int inserted=0;boolean reused=false;
  stage="CONNECT";try(var c=DriverManager.getConnection(WindowsBaseDatos.url(),"siger_windows_migrator",password)){
   stage="BASELINE";c.setAutoCommit(false);var current=fingerprint(c);
   if(current.equals(expected)){foreignKeys(c);reused=true;c.rollback();}
   else {
    var baseline=json.readTree(Files.readString(EVIDENCE.resolve("sql-post-flyway.json"))).path("tables");
    if(!current.equals(baseline)||current.size()!=columns.size())throw new IllegalStateException("DESTINATION_CHANGED");c.rollback();
    // Source sequence high-water marks are preserved before inserts. Never use the application for DDL.
    stage="SEQUENCES";var increments=json.readTree(Files.readString(EVIDENCE.resolve("auto-increment-origen.json")));
    for(var row:increments){String table=row.path("table").asString();long next=row.path("next").asLong();if(table.equals("flyway_schema_history"))continue;if(!columns.containsKey(table)||next<1)throw new IllegalStateException();try(var s=c.createStatement()){s.execute("ALTER TABLE `"+table+"` AUTO_INCREMENT="+next);}}
    try(var s=c.createStatement()){s.execute("SET SESSION FOREIGN_KEY_CHECKS=0");}
    try {
     stage="INSERT";for(String line:new String(raw,StandardCharsets.UTF_8).split("\n")){
      if(line.isBlank())continue;var row=json.readTree(line);String table=row.path("table").asString();stage="INSERT_"+table;if(table.equals("flyway_schema_history"))continue;var cols=columns.get(table);if(cols==null||row.path("values").size()!=cols.size())throw new IllegalStateException();
      String names=String.join(",",cols.stream().map(x->"`"+x+"`").toList());String placeholders=String.join(",",Collections.nCopies(cols.size(),"?"));String sql="INSERT INTO `"+table+"` ("+names+") VALUES ("+placeholders+")";
      if(SEEDED.contains(table))sql+=" ON DUPLICATE KEY UPDATE "+String.join(",",cols.stream().map(x->"`"+x+"`=VALUES(`"+x+"`)").toList());
      try(var statement=c.prepareStatement(sql)){for(int i=0;i<cols.size();i++){var v=row.path("values").get(i);if(v.isNull())statement.setNull(i+1,Types.NULL);else statement.setString(i+1,new String(HexFormat.of().parseHex(v.asString()),StandardCharsets.UTF_8));}statement.executeUpdate();inserted++;}
     }
     try(var s=c.createStatement()){s.execute("SET SESSION FOREIGN_KEY_CHECKS=1");}
     stage="VERIFY";foreignKeys(c);if(!fingerprint(c).equals(expected))throw new IllegalStateException("DATA_MISMATCH");c.commit();
    }catch(Exception e){c.rollback();throw e;}finally{try(var s=c.createStatement()){s.execute("SET SESSION FOREIGN_KEY_CHECKS=1");}}
   }
  }
  Arrays.fill(raw,(byte)0);System.out.println(json.writeValueAsString(Map.of("ok",true,"tables",columns.size(),"rowsImported",inserted,"alreadyImported",reused,"foreignKeysVerified",true,"allTableHashesVerified",true,"runtimeStarted",false,"mailSent",0)));
 }catch(Exception e){var safe=new TreeMap<String,Object>();safe.put("ok",false);safe.put("stage",stage);safe.put("errorClass",e.getClass().getSimpleName());if(e instanceof SQLException s){safe.put("sqlState",s.getSQLState());safe.put("vendorCode",s.getErrorCode());}System.out.println(WindowsBaseDatos.JSON.writeValueAsString(safe));System.exit(1);}}
}