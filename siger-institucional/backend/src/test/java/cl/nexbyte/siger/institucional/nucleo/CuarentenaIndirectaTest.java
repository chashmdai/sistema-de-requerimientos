package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.*;import java.time.*;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class CuarentenaIndirectaTest {
 @TempDir Path temp;
 String mysql(String suffix){String m=System.getProperty("siger.test.autoridad.mysql");return m==null?null:m+suffix+"?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000";}
 @Test void permisoSoloDocumentoNoEludeBarreraTecnicaNiConcedeCaso()throws Exception{
  try(var f=new DocumentoAisladoFixture(temp,Clock.systemUTC(),mysql("_cuarentenasolo"))){UUID doc=f.documento(),access=f.proponer(doc,true,true);f.f.aprobar(access);var actor=f.x.f.a(f.ti);assertTrue(f.f.flow.documentoAislado(actor,access,"CONTENIDO",false).contenido());var q=new CuarentenaDocumental(f.d.db,f.x.f.core,f.x.f.cat,f.x.f.f.autoridades,f.files,f.d.recepcion,p->true,f.inspector,f.clock);String sha=f.d.db.sql.queryForObject("SELECT sha256 FROM institution_document WHERE id=?",String.class,doc.toString());q.aislar(actor,UUID.randomUUID(),new CuarentenaDocumental.Aislar(doc,sha,"INC-DOCUMENTO-AISLADO","Barrera técnica sobre acceso excepcional vigente"));
   var meta=f.f.flow.documentoAislado(actor,access,"METADATOS",false);assertTrue(meta.cuarentena());assertFalse(meta.contenido());assertFalse(meta.descarga());assertEquals(423,assertThrows(FalloTecnico.class,()->f.f.flow.documentoAislado(actor,access,"CONTENIDO",false)).status);assertEquals(423,assertThrows(FalloTecnico.class,()->f.f.flow.documentoAislado(actor,access,"DESCARGA",false)).status);assertEquals(404,assertThrows(FalloTecnico.class,()->f.x.f.core.caso(actor,f.x.cas)).status);
  }
 }
 @Test void originalCiudadanoConservaEntregaFirmadaYBytesSinDescargaPermitida()throws Exception{
  try(var x=new ContenidoFixture(temp,Clock.systemUTC(),mysql("_cuarpub"))){var origin=x.original();var before=x.d.recepcion.completaRegistrada(origin.comunicacion());var descriptor=before.adjuntos().getFirst();String key=x.d.db.sql.queryForObject("SELECT storage_key FROM institution_document WHERE id=?",String.class,origin.documento().toString());var physical=x.files.verificar(key,descriptor);var q=new CuarentenaDocumental(x.d.db,x.f.core,x.f.cat,x.f.f.autoridades,x.files,x.d.recepcion,p->true,(p,e)->new ProcesamientoDocumental.Examen(descriptor.tipo(),"DESCARGA",null),x.clock);
   q.aislar(x.f.a(x.f.ti),UUID.randomUUID(),new CuarentenaDocumental.Aislar(origin.documento(),descriptor.sha256(),"INC-ORIGINAL-CIUDADANO","No alterar custodia pública preservada"));assertEquals(before,x.d.recepcion.completaRegistrada(origin.comunicacion()));assertArrayEquals(origin.bytes(),Files.readAllBytes(physical));var policy=new AutorizacionNucleo(x.f.core);assertFalse(policy.permisos(x.f.a(x.f.profA),origin.caso(),origin.documento()).contenido());assertEquals(423,assertThrows(FalloTecnico.class,()->policy.documento(x.f.a(x.f.profA),origin.caso(),origin.documento(),"DESCARGA")).status);assertEquals("INGRESO_CIUDADANO",x.d.db.sql.queryForObject("SELECT origin FROM institution_document WHERE id=?",String.class,origin.documento().toString()));
  }
 }
}
