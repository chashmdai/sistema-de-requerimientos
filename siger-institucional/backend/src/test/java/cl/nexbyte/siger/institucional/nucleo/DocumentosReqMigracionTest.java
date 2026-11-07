package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class DocumentosReqMigracionTest {
 @TempDir Path temp;
 @Test void v1518PreservaOriginalCasoFirmaPoliticaComandoYReintentoSinAsignarReq()throws Exception{
  try(var x=new DocumentosReqFixture(temp,"15.17")){
   UUID cas=x.f.caso("AREA_A",x.f.headA,x.f.profA),key=x.key();var old=new DocumentosGestion(x.d.db,x.files,new AutorizacionNucleo(x.f.core),p->true,x.inspector,x.clock);
   var doc=old.cargar(x.f.a(x.f.profA),key,cas,null,"previo.txt",x.bytes());String id=doc.id().toString();var original=x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",id);
   var link=x.d.db.sql.queryForList("SELECT * FROM management_document_link WHERE document_id=?",id);var policy=x.d.db.sql.queryForMap("SELECT * FROM document_content_policy WHERE document_id=?",id);var command=x.d.db.sql.queryForMap("SELECT * FROM document_command WHERE id=?",key.toString());var roots=x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");
   x.d.db.migrate("institucional");var current=x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",id);assertTrue(current.containsKey("owner_context"));assertNull(current.remove("owner_context"));assertEquals(original,current);
   assertEquals(link,x.d.db.sql.queryForList("SELECT * FROM management_document_link WHERE document_id=?",id));assertEquals(policy,x.d.db.sql.queryForMap("SELECT * FROM document_content_policy WHERE document_id=?",id));assertEquals(command,x.d.db.sql.queryForMap("SELECT * FROM document_command WHERE id=?",key.toString()));assertEquals(roots,x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(doc,old.cargar(x.f.a(x.f.profA),key,cas,null,"previo.txt",x.bytes()));
   assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM requirement_document_policy",Long.class));assertEquals(404,assertThrows(FalloTecnico.class,()->x.docs.referencia(x.f.a(x.f.profA),x.req,doc.id(),"CONTENIDO")).status);
   var nuevo=x.cargar();assertNotNull(x.docs.referencia(x.f.a(x.f.profA),x.req,nuevo.id(),"CONTENIDO"));assertEquals(404,assertThrows(FalloTecnico.class,()->old.referencia(x.f.a(x.f.profA),cas,nuevo.id())).status);
  }
 }
}
