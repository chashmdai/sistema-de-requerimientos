package cl.nexbyte.siger.institucional.notificaciones;

import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.ByteBuffer;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;

/** Materialización inmutable por intento; nunca reconstruye como enviado un mensaje histórico desconocido. */
public final class MensajesCorreo {
 static final String PLANTILLA="AVISO_INTERNO_LAB_1.0";
 static String asunto(String category,String correlation){return "[SIGER LAB] "+category+" · "+correlation;}
 private final BaseJdbc db;private final Clock clock;
 MensajesCorreo(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
 public record Copia(UUID intento,boolean acreditada,String plantilla,Instant preparadaEn,String origen,String solicitadaPor,String destinatario,String asunto,String texto,String correlacion,String sha256,String estadoIntento){@Override public String toString(){return "CopiaCorreo[reservada]";}}
 static String huella(UUID aviso,UUID intento,String destinatario,String plantilla,long ms,String origen,String por,String asunto,String texto,String correlacion){
  try{var hash=MessageDigest.getInstance("SHA-256");for(var campo:List.of("SIGER_AVISO_MATERIAL_1",aviso.toString(),intento.toString(),destinatario,plantilla,Long.toString(ms),origen,Objects.toString(por,""),asunto,texto,correlacion)){byte[] bytes=campo.getBytes(StandardCharsets.UTF_8);hash.update(ByteBuffer.allocate(4).putInt(bytes.length).array());hash.update(bytes);}return HexFormat.of().formatHex(hash.digest());}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
 void guardar(UUID aviso,UUID intento,String por,TransporteCorreo.Mensaje mensaje){
  if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transacción del intento requerida");
  long ms=clock.millis();String origen=por==null?"AUTOMATICO":"MANUAL";
  db.sql.update("INSERT INTO mail_message_snapshot(attempt_id,template_version,materialized_ms,trigger_code,requested_by,subject_text,body_text,correlation_id,content_sha256) VALUES(?,?,?,?,?,?,?,?,?)",intento.toString(),PLANTILLA,ms,origen,por,mensaje.asunto(),mensaje.texto(),mensaje.correlacion(),huella(aviso,intento,mensaje.destinatario(),PLANTILLA,ms,origen,por,mensaje.asunto(),mensaje.texto(),mensaje.correlacion()));
 }
 Copia leer(UUID aviso,UUID intento){
  var rows=db.sql.queryForList("SELECT a.*,m.attempt_id AS prepared_attempt,m.template_version,m.materialized_ms,m.trigger_code,m.requested_by,m.subject_text,m.body_text,m.correlation_id,m.content_sha256,o.correlation_id AS expected_correlation,n.recipient_id AS expected_recipient FROM mail_attempt a JOIN notification_outbox o ON o.notification_id=a.notification_id JOIN institution_notification n ON n.id=a.notification_id LEFT JOIN mail_message_snapshot m ON m.attempt_id=a.id WHERE a.id=? AND a.notification_id=?",intento.toString(),aviso.toString());
  if(rows.size()!=1)throw new FalloTecnico(404);var r=rows.getFirst();String estado=r.get("state").toString();
  if(r.get("prepared_attempt")==null)return new Copia(intento,false,null,null,null,null,null,null,null,null,null,estado);
  try{
   String plantilla=r.get("template_version").toString(),origen=r.get("trigger_code").toString(),por=Objects.toString(r.get("requested_by"),null),address=r.get("resolved_email").toString(),asunto=r.get("subject_text").toString(),texto=r.get("body_text").toString(),correlacion=r.get("correlation_id").toString(),hash=r.get("content_sha256").toString();long ms=((Number)r.get("materialized_ms")).longValue();
   if(!PLANTILLA.equals(plantilla)||ms<0||!Objects.equals(r.get("recipient_id"),r.get("expected_recipient"))||!correlacion.equals(r.get("expected_correlation"))||(!origen.equals("MANUAL")&&!origen.equals("AUTOMATICO"))||origen.equals("MANUAL")&&!Objects.equals(por,r.get("recipient_id"))||origen.equals("AUTOMATICO")&&por!=null||!hash.matches("[a-f0-9]{64}")||!MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),huella(aviso,intento,address,plantilla,ms,origen,por,asunto,texto,correlacion).getBytes(StandardCharsets.US_ASCII)))throw new IllegalArgumentException();
   return new Copia(intento,true,plantilla,Instant.ofEpochMilli(ms),origen,por,address,asunto,texto,correlacion,hash,estado);
  }catch(RuntimeException corrupted){throw new FalloTecnico(409,"CONSTANCIA_CORREO_INTEGRA_REQUERIDA");}
 }
 TransporteCorreo.Mensaje paraEnvio(UUID aviso,UUID intento){var c=leer(aviso,intento);if(!c.acreditada())throw new FalloTecnico(409,"CONSTANCIA_CORREO_INTEGRA_REQUERIDA");return new TransporteCorreo.Mensaje(c.destinatario(),c.asunto(),c.texto(),c.correlacion());}
}
