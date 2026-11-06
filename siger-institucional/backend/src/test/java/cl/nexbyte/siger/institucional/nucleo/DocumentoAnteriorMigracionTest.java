package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.robustez.*;import java.nio.file.Path;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class DocumentoAnteriorMigracionTest {
 @TempDir Path temp;
 @Test void v1515PreservaDecisionHashFirmaYReintentoDeContrato16()throws Exception{
  var clock=new AreasInstitucionalesTest.Reloj();String mysql=System.getProperty("siger.test.autoridad.mysql");try(var d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_docprev?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000","15.14")){
   if(mysql!=null){d.limpiar();for(String t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);}
   var f=new AccesoReservadoFixture(d,clock);var x=f.x;UUID doc=x.documento("FIR"),key=UUID.randomUUID();var r=new AccesoReservado.Proponer(1,x.reviewer,clock.instant(),clock.instant().plusSeconds(120),true,false,"Decisión anterior sin ampliar expediente");var result=f.flow.proponer(x.f.a(x.owner),key,x.cas,doc,r);f.aprobar(result.id());var before=d.db.sql.queryForMap("SELECT * FROM institution_sensitive_access WHERE id=?",result.id().toString());var cmd=d.db.sql.queryForMap("SELECT * FROM core_command WHERE id=?",key.toString());var proof=d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");
   d.db.migrate("institucional");var after=d.db.sql.queryForMap("SELECT * FROM institution_sensitive_access WHERE id=?",result.id().toString());assertEquals("CASO_DOCUMENTO",after.remove("scope_mode"));assertEquals(before,after);assertEquals(cmd,d.db.sql.queryForMap("SELECT * FROM core_command WHERE id=?",key.toString()));assertEquals(proof,d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(result,f.flow.proponer(x.f.a(x.owner),key,x.cas,doc,r));assertTrue(x.f.core.contenidos().permisos(x.f.a(x.reviewer),x.cas,doc).contenido());assertEquals(404,assertThrows(FalloTecnico.class,()->f.flow.documentoAislado(x.f.a(x.reviewer),result.id(),"METADATOS",true)).status);
   if(mysql!=null){d.limpiar();for(String t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);}
  }
 }
}
