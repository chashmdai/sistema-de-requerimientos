package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.notificaciones.*;import java.nio.file.Path;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class CorreoMigracionTest {
 @TempDir Path temp;
 @Test void actualizar1516PreservaIntentosSinInventarTextoVersionOEntrega()throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");try(var f=new DatosAvisosTest(temp,mysql==null?null:mysql+"_correoprev?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000","15.16")){
   var aviso=f.recordatorio(f.caso());var id=UUID.randomUUID();f.d.db.sql.update("INSERT INTO mail_attempt(id,notification_id,recipient_id,resolved_email,started_ms,finished_ms,state,stage_code,http_status) VALUES(?,?,?,?,?,?,'ACCEPTED_BY_GRAPH','GRAPH',202)",id.toString(),aviso.toString(),f.prof,"prof.avisos@example.invalid",f.tiempo.millis(),f.tiempo.millis());f.d.db.sql.update("UPDATE notification_outbox SET state='ACCEPTED_BY_GRAPH',human_state='PENDIENTE_CONFIRMACION_USUARIO',attempt_count=1 WHERE notification_id=?",aviso.toString());
   var intentos=f.d.db.sql.queryForList("SELECT * FROM mail_attempt ORDER BY id");var outbox=f.d.db.sql.queryForList("SELECT * FROM notification_outbox ORDER BY notification_id");var autoridad=f.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision");
   f.d.db.migrate("institucional");assertEquals(intentos,f.d.db.sql.queryForList("SELECT * FROM mail_attempt ORDER BY id"));assertEquals(outbox,f.d.db.sql.queryForList("SELECT * FROM notification_outbox ORDER BY notification_id"));assertEquals(autoridad,f.d.db.sql.queryForList("SELECT * FROM authority_batch ORDER BY revision"));assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM mail_message_snapshot",Long.class));
   var transporte=new TransporteCorreo(){public Resultado enviar(Mensaje m){fail("El intento aceptado anterior no se reenvía");return null;}public boolean real(){return false;}};var m=new CorreosInstitucionales(f.d.db,f.cat,f.notices,transporte,Set.of("prof.avisos@example.invalid"),f.tiempo,"upgrade_sintetico");var copia=m.copia(f.a(f.prof),aviso,id);assertFalse(copia.acreditada());assertNull(copia.texto());assertNull(copia.plantilla());assertNull(copia.preparadaEn());assertEquals("ACCEPTED_BY_GRAPH",m.enviar(f.a(f.prof),aviso).estado());assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM mail_message_snapshot",Long.class));
   if(mysql!=null)f.d.limpiar();
  }
 }
}
