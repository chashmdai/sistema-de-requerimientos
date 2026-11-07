package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import java.nio.file.Path;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class AsociacionesMigracionTest {
 @TempDir Path temp;
 @Test void upgrade1520ConservaOriginalPoliticaVinculosRaizYComandoSinInventarRectificaciones()throws Exception{
  try(var x=new DocumentosReqFixture(temp,"15.20","_asocprev")){
   UUID key=x.key();var doc=x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"previo.txt",x.bytes());var original=x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",doc.id().toString());var policy=x.d.db.sql.queryForList("SELECT * FROM requirement_document_policy");var links=x.d.db.sql.queryForList("SELECT * FROM requirement_document_link");var roots=x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");var commands=x.d.db.sql.queryForList("SELECT * FROM requirement_document_command");assertFalse(AsociacionesVigentes.esquema(x.d.db));
   x.d.db.migrate("institucional");assertTrue(AsociacionesVigentes.esquema(x.d.db));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM document_association_control",Long.class));assertEquals(original,x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",doc.id().toString()));assertEquals(policy,x.d.db.sql.queryForList("SELECT * FROM requirement_document_policy"));assertEquals(links,x.d.db.sql.queryForList("SELECT * FROM requirement_document_link"));assertEquals(roots,x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(commands,x.d.db.sql.queryForList("SELECT * FROM requirement_document_command"));assertEquals(doc,x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"previo.txt",x.bytes()));
  }
 }
}
