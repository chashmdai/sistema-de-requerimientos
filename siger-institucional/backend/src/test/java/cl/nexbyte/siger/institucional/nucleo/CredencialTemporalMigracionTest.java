package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CredencialTemporalMigracionTest {
 @TempDir Path temp;
 @Test void v15_1PreservaCuentasAnterioresYNoLesImponeResetRetrospectivo(){
  String url="jdbc:h2:file:"+temp.resolve("anterior").toAbsolutePath().toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE";
  try(var db=new BaseJdbc(url,"sa","",2)){
   Flyway.configure().dataSource(db.sql.getDataSource()).locations("classpath:db/institucional/h2").target("14").cleanDisabled(true).load().migrate();new CatalogoLaboratorio().instalar(db);
   db.sql.update("INSERT INTO staff_account VALUES(?,?,?,?,?,?,TRUE,7,?,?,?,?)","demo.distribucion","historico.lab","historico@example.invalid","Cargo",null,null,"{bcrypt}hash-sintetico-no-autenticable",1L,2L,"demo.distribucion");
   var before=db.sql.queryForMap("SELECT * FROM staff_account");db.migrate("institucional");assertEquals(before,db.sql.queryForMap("SELECT * FROM staff_account"));assertEquals(0,db.sql.queryForObject("SELECT COUNT(*) FROM staff_credential_state",Integer.class));
   // El INSERT posicional anterior continúa siendo válido después de expandir el esquema.
   db.sql.update("INSERT INTO staff_account VALUES(?,?,?,?,?,?,TRUE,7,?,?,?,?)","demo.profesional.upr.a","anterior.jar","anterior.jar@example.invalid","Cargo",null,null,"{bcrypt}hash-sintetico-no-autenticable",1L,2L,"demo.distribucion");
   assertEquals(2,db.sql.queryForObject("SELECT COUNT(*) FROM staff_account",Integer.class));
  }
 }
 @Test void scriptsAditivosH2YMySqlCoinciden()throws Exception{
  try(var h2=getClass().getResourceAsStream("/db/institucional/h2/V15_1__credencial_temporal.sql");var mysql=getClass().getResourceAsStream("/db/institucional/mysql/V15_1__credencial_temporal.sql")){assertNotNull(h2);assertNotNull(mysql);assertArrayEquals(h2.readAllBytes(),mysql.readAllBytes());}
 }
}
