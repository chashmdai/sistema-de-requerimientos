package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import java.nio.file.Path;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class VersionesDocMigracionTest {
 @TempDir Path temp;
 @Test void v1519PreservaOriginalReqPoliticaComandoYRaizSinInventarUnaVersion()throws Exception{
  try(var x=new DocumentosReqFixture(temp,"15.18","_docmetaprev")){
   String h=System.getProperty("siger.test.autoridad.mysql")!=null?"SELECT version FROM flyway_schema_history WHERE success=TRUE ORDER BY installed_rank DESC LIMIT 1":"SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\"=TRUE ORDER BY \"installed_rank\" DESC LIMIT 1";
   assertEquals("15.18",x.d.db.sql.queryForObject(h,String.class));UUID key=x.key();var d=x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"anterior.txt",x.bytes());var original=x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",d.id().toString());var policy=x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",d.id().toString());var command=x.d.db.sql.queryForMap("SELECT * FROM requirement_document_command WHERE id=?",key.toString());var roots=x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");
   x.d.db.migrate("institucional");assertEquals("15.20",x.d.db.sql.queryForObject(h,String.class));assertEquals(original,x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",d.id().toString()));assertEquals(policy,x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",d.id().toString()));assertEquals(command,x.d.db.sql.queryForMap("SELECT * FROM requirement_document_command WHERE id=?",key.toString()));assertEquals(roots,x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(d,x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"anterior.txt",x.bytes()));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM management_document_metadata",Long.class));
  }
 }
}
