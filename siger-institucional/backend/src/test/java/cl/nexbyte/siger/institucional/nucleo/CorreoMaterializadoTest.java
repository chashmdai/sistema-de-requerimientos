package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.notificaciones.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.net.URI;import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CorreoMaterializadoTest {
 @TempDir Path temp;DatosAvisosTest f;final List<TransporteCorreo.Mensaje> enviados=new ArrayList<>();
 TransporteCorreo.Resultado resultado=new TransporteCorreo.Resultado("SIMULADO","SIMULADO",null,0,null);
 @BeforeEach void setup()throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");f=new DatosAvisosTest(temp,mysql==null?null:mysql+"_correo?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");}
 @AfterEach void close(){if(f!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)f.d.limpiar();f.close();}}
 TransporteCorreo transporte(){return new TransporteCorreo(){public Resultado enviar(Mensaje m){enviados.add(m);return resultado;}public boolean real(){return false;}};}
 CorreosInstitucionales mail(){return new CorreosInstitucionales(f.d.db,f.cat,f.notices,transporte(),Set.of("prof.avisos@example.invalid"),f.tiempo,"copia_sintetica",r->{},URI.create("https://siger.example.invalid/"));}
 UUID aviso(){return f.recordatorio(f.caso());}
 UUID intento(UUID n){return UUID.fromString(f.d.db.sql.queryForObject("SELECT id FROM mail_attempt WHERE notification_id=? ORDER BY started_ms,id LIMIT 1",String.class,n.toString()));}
 long count(String table){return f.d.db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
 void denied(int code,Runnable action){assertEquals(code,assertThrows(FalloTecnico.class,action::run).status);}

 @Test void conservaExactamenteMensajePreparadoAntesDelTransporteYNoLoRecrea(){
  var n=aviso();var m=mail();m.enviar(f.a(f.prof),n);var c=m.copia(f.a(f.prof),n,intento(n));var enviado=enviados.getFirst();assertTrue(c.acreditada());assertEquals("AVISO_INTERNO_LAB_1.0",c.plantilla());assertEquals(enviado.destinatario(),c.destinatario());assertEquals(enviado.asunto(),c.asunto());assertEquals(enviado.texto(),c.texto());assertEquals(enviado.correlacion(),c.correlacion());assertTrue(c.texto().contains("https://siger.example.invalid/#notificacion/"+n));assertTrue(c.texto().contains("\n\n"));assertFalse(c.texto().contains("Descripción privada"));assertEquals("MANUAL",c.origen());assertEquals(f.prof,c.solicitadaPor());assertTrue(c.sha256().matches("[a-f0-9]{64}"));m.enviar(f.a(f.prof),n);assertEquals(c,m.copia(f.a(f.prof),n,c.intento()));assertEquals(1,enviados.size());assertEquals(1,count("mail_message_snapshot"));
 }
 @Test void procesamientoAutomaticoNoInventaUnaSolicitudHumana(){
  var n=aviso();var m=mail();for(int i=0;i<3;i++)m.automaticos(4);var c=m.copia(f.a(f.prof),n,intento(n));assertTrue(c.acreditada());assertEquals("AUTOMATICO",c.origen());assertNull(c.solicitadaPor());assertTrue(c.texto().contains("#notificacion/"+n));
 }
 @Test void reintentoConservaCopiaAnteriorAunqueCambieConfiguracionDelEnlace(){
  var n=aviso();resultado=new TransporteCorreo.Resultado("REINTENTABLE","GRAPH",429,120,null);var m=mail();m.enviar(f.a(f.prof),n);var old=m.copia(f.a(f.prof),n,intento(n));f.tiempo.avanzar(121);resultado=new TransporteCorreo.Resultado("SIMULADO","SIMULADO",null,0,null);var cambiado=new CorreosInstitucionales(f.d.db,f.cat,f.notices,transporte(),Set.of("prof.avisos@example.invalid"),f.tiempo,"copia_sintetica",r->{},URI.create("https://otro.example.invalid/siger/"));cambiado.enviar(f.a(f.prof),n);assertEquals(old,cambiado.copia(f.a(f.prof),n,old.intento()));assertEquals(2,count("mail_message_snapshot"));assertEquals(2,enviados.size());assertTrue(enviados.getLast().texto().contains("https://otro.example.invalid/siger/#notificacion/"+n));assertFalse(old.texto().contains("otro.example.invalid"));
 }
 @Test void falloTrasRedConservaCopiaYAmbiguedadSinReenvioCiego(){
  var n=aviso();var roto=new CorreosInstitucionales(f.d.db,f.cat,f.notices,transporte(),Set.of("prof.avisos@example.invalid"),f.tiempo,"copia_sintetica",r->{throw new IllegalStateException("Fallo de persistencia simulado");},URI.create("https://siger.example.invalid/"));assertThrows(IllegalStateException.class,()->roto.enviar(f.a(f.prof),n));var id=intento(n);String texto=mail().copia(f.a(f.prof),n,id).texto();f.tiempo.avanzar(91);f.notices.recuperarCorreos();assertEquals("AMBIGUO",mail().consultar(f.a(f.prof),n).estado());mail().enviar(f.a(f.prof),n);assertEquals(1,enviados.size());assertEquals(texto,mail().copia(f.a(f.prof),n,id).texto());
 }
 @Test void falloAlGuardarCopiaRevierteIntentoReservaYContadorSinLlamarRed(){
  var n=aviso();f.d.ddl(sql->sql.execute("ALTER TABLE mail_message_snapshot ADD CONSTRAINT snapshot_rollback CHECK(template_version<>'AVISO_INTERNO_LAB_1.0')"));try{assertThrows(org.springframework.dao.DataAccessException.class,()->mail().enviar(f.a(f.prof),n));}finally{f.d.ddl(sql->sql.execute("ALTER TABLE mail_message_snapshot DROP CONSTRAINT snapshot_rollback"));}assertEquals(0,count("mail_attempt"));assertEquals(0,count("mail_message_snapshot"));assertTrue(enviados.isEmpty());assertEquals("PENDIENTE",mail().consultar(f.a(f.prof),n).estado());assertEquals(0,mail().consultar(f.a(f.prof),n).intentos());mail().enviar(f.a(f.prof),n);assertEquals(1,enviados.size());
 }
 @Test void otroFuncionarioTiYUuidDeOtroAvisoNoLeenCopia(){
  var n=aviso();mail().enviar(f.a(f.prof),n);var id=intento(n);for(String actor:List.of(f.ti,f.head,f.otro))denied(404,()->mail().copia(f.a(actor),n,id));var otro=aviso();denied(404,()->mail().copia(f.a(f.prof),otro,id));denied(404,()->f.notices.detalle(f.a(f.ti),n));
 }
 @Test void revocarAccesoRetiraDetalleDeAvisoYMensajeConservado(){
  var n=aviso();mail().enviar(f.a(f.prof),n);var id=intento(n);var cuenta=f.ids.cuentas(f.ti).stream().filter(a->a.id().equals(f.prof)).findFirst().orElseThrow();ConcesionFixture.revocar(f.d.db,f.ids,f.ti,f.prof,cuenta.asignaciones().getFirst().get("id").toString(),f.tiempo);denied(404,()->mail().copia(f.a(f.prof),n,id));denied(404,()->f.notices.detalle(f.a(f.prof),n));assertEquals(1,count("mail_message_snapshot"));
 }
 @Test void textoAlteradoFallaIntegridadSinRecomponerUnaCopiaActual(){
  var n=aviso();mail().enviar(f.a(f.prof),n);var id=intento(n);f.d.db.sql.update("UPDATE mail_message_snapshot SET body_text=? WHERE attempt_id=?","Texto alterado sintético",id.toString());denied(409,()->mail().copia(f.a(f.prof),n,id));assertEquals(1,enviados.size());assertEquals(1,count("mail_message_snapshot"));
 }
 @Test void cualquierTransporteQueDeclaraRealNecesitaBaseHttpsAntesDeReservar(){
  var n=aviso();var real=new TransporteCorreo(){public Resultado enviar(Mensaje m){fail("No debe llamar proveedor");return null;}public boolean real(){return true;}};var m=new CorreosInstitucionales(f.d.db,f.cat,f.notices,real,Set.of("prof.avisos@example.invalid"),f.tiempo,"guardia_sintetica");denied(409,()->m.enviar(f.a(f.prof),n));assertEquals(0,count("mail_attempt"));assertEquals(0,count("mail_message_snapshot"));assertEquals("PENDIENTE",m.consultar(f.a(f.prof),n).estado());
 }
 @Test void enviosConcurrentesTienenUnSoloIntentoYCopia()throws Exception{
  var n=aviso();var m=mail();try(var pool=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return m.enviar(f.a(f.prof),n);});var b=pool.submit(()->{gate.await();return m.enviar(f.a(f.prof),n);});gate.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}assertEquals(1,enviados.size());assertEquals(1,count("mail_attempt"));assertEquals(1,count("mail_message_snapshot"));
 }
 @Test void consultarCopiaYDetalleSoloAuditaSinLeerAcusarCumplirONotificar(){
  var n=aviso();mail().enviar(f.a(f.prof),n);long eventos=count("workflow_event"),notificaciones=count("institution_notification");assertFalse(f.notices.detalle(f.a(f.prof),n).leida());mail().copia(f.a(f.prof),n,intento(n));assertFalse(f.notices.detalle(f.a(f.prof),n).leida());assertEquals(eventos,count("workflow_event"));assertEquals(notificaciones,count("institution_notification"));assertEquals(0,count("institution_assignment_receipt"));assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM matter_access_event WHERE resource_id=? AND action_code='CORREO_COPIA_CONSULTADA'",Long.class,n.toString()));
 }
 @Test void destinatarioDeIntentoAlteradoNoSeConfundeConDuenoDelAviso(){
  var n=aviso();var m=mail();for(int i=0;i<3;i++)m.automaticos(4);var id=intento(n);f.d.db.sql.update("UPDATE mail_attempt SET recipient_id=? WHERE id=?",f.otro,id.toString());denied(409,()->m.copia(f.a(f.prof),n,id));
 }
 @Test void alteracionPoscommitAntesDeRedBloqueaSinEnviarNiReintentar(){
  var n=aviso();var registrado=new java.util.concurrent.atomic.AtomicBoolean();var reloj=new java.time.Clock(){public java.time.ZoneId getZone(){return f.tiempo.getZone();}public java.time.Clock withZone(java.time.ZoneId z){return this;}public java.time.Instant instant(){
   if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()&&registrado.compareAndSet(false,true))org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){
    // Conexión independiente del MISMO destino descartable, después de confirmar la reserva.
    try(var c=f.d.db.sql.getDataSource().getConnection();var s=c.prepareStatement("UPDATE mail_message_snapshot SET body_text=? WHERE attempt_id IN(SELECT id FROM mail_attempt WHERE notification_id=?)")){c.setAutoCommit(true);s.setString(1,"Alteración sintética posterior al COMMIT");s.setString(2,n.toString());assertEquals(1,s.executeUpdate());}catch(java.sql.SQLException failure){throw new AssertionError("No se pudo inyectar el fallo local",failure);}
   }});return f.tiempo.instant();
  }};
  var m=new CorreosInstitucionales(f.d.db,f.cat,f.notices,transporte(),Set.of("prof.avisos@example.invalid"),reloj,"integridad_sintetica",r->{},URI.create("https://siger.example.invalid/"));assertEquals("BLOQUEADO_INTEGRIDAD",m.enviar(f.a(f.prof),n).estado());assertTrue(enviados.isEmpty());assertEquals(1,count("mail_attempt"));assertEquals(1,count("mail_message_snapshot"));m.enviar(f.a(f.prof),n);assertTrue(enviados.isEmpty());assertEquals(1,count("mail_attempt"));
 }
}
