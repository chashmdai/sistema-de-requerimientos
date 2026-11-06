package cl.nexbyte.siger.institucional.robustez;
import java.sql.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.*;
/** Expansión compatible: identifica exclusivamente el CHECK personal histórico, cuyo nombre
 * fue generado por el motor. Nunca desactiva comprobaciones o claves foráneas globales. */
public abstract class MigracionEquipoAgenda extends BaseJavaMigration {
 private String schema(){try(var in=getClass().getResourceAsStream("/db/institucional/comun/equipo-agenda-v15-10.sql")){return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
 @Override public Integer getChecksum(){var crc=new CRC32();crc.update(("equipo-agenda-check-v1\n"+schema().replace("\r\n","\n")).getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
 @Override public void migrate(Context context)throws Exception{
  var c=context.getConnection();boolean h2=c.getMetaData().getDatabaseProductName().equals("H2");var names=new ArrayList<String>();
  String query=h2?"SELECT t.CONSTRAINT_NAME,k.CHECK_CLAUSE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS t JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS k ON k.CONSTRAINT_NAME=t.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA WHERE UPPER(t.TABLE_NAME)='INSTITUTION_TASK' AND t.TABLE_SCHEMA=? AND t.CONSTRAINT_TYPE='CHECK'":"SELECT t.CONSTRAINT_NAME,k.CHECK_CLAUSE FROM information_schema.TABLE_CONSTRAINTS t JOIN information_schema.CHECK_CONSTRAINTS k ON k.CONSTRAINT_NAME=t.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA WHERE t.TABLE_NAME='institution_task' AND t.CONSTRAINT_SCHEMA=DATABASE() AND t.CONSTRAINT_TYPE='CHECK'";
  try(var p=c.prepareStatement(query)){if(h2)p.setString(1,c.getSchema());try(var rows=p.executeQuery()){while(rows.next())if(rows.getString(2).replaceAll("[\\s`\"()]","").equalsIgnoreCase("creator_id=responsible_id"))names.add(rows.getString(1));}}
  if(names.size()!=1||!names.getFirst().matches("[A-Za-z0-9_]+"))throw new SQLException("CHECK personal histórico no identificado de forma única; no se alteran otros controles");
  try(var st=c.createStatement()){st.execute("ALTER TABLE institution_task DROP "+(h2?"CONSTRAINT \""+names.getFirst()+"\"":"CHECK `"+names.getFirst()+"`"));for(String sql:schema().split(";"))if(!sql.isBlank())st.execute(sql);}
 }
}
