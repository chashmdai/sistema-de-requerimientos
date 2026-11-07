package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import java.nio.file.Path;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class CuarentenaMigracionTest {
 @TempDir Path temp;
 @Test void v1520PreservaOriginalReservaVersionYReintentoSinRetencionesInferidas()throws Exception{
  try(var x=new DocumentosReqFixture(temp,"15.19","_cuarentenaprev")){
   var caso=new DocumentosGestion(x.d.db,x.files,new AutorizacionNucleo(x.f.core),p->true,x.inspector,x.clock);var versions=new VersionesDocumentales(x.d.db,x.f.core,x.f.cat,caso,x.docs,x.clock);UUID key=x.key();var doc=x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"preservado.txt",x.bytes());versions.registrar(x.f.a(x.f.profA),x.key(),"REQ",x.req,doc.id(),new VersionesDocumentales.Metadatos(-1,"Declarado previo","Tipo declarado","Evidencia de versión anterior"));
   var before=x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",doc.id().toString());var policies=x.d.db.sql.queryForList("SELECT * FROM requirement_document_policy");var metadata=x.d.db.sql.queryForList("SELECT * FROM management_document_metadata");var history=x.d.db.sql.queryForList("SELECT * FROM management_document_metadata_history");var roots=x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");assertFalse(DisponibilidadDocumental.esquema(x.d.db));
   x.d.db.migrate("institucional");assertTrue(DisponibilidadDocumental.esquema(x.d.db));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM document_technical_hold",Long.class));assertEquals(before,x.d.db.sql.queryForMap("SELECT * FROM institution_document WHERE id=?",doc.id().toString()));assertEquals(policies,x.d.db.sql.queryForList("SELECT * FROM requirement_document_policy"));assertEquals(metadata,x.d.db.sql.queryForList("SELECT * FROM management_document_metadata"));assertEquals(history,x.d.db.sql.queryForList("SELECT * FROM management_document_metadata_history"));assertEquals(roots,x.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(doc,x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"preservado.txt",x.bytes()));
  }
 }
}
