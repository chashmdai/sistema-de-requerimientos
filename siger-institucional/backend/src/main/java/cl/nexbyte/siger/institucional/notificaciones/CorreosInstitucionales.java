package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.net.URI;import java.time.*;import java.util.*;import java.util.function.Consumer;
/** Outbox con intento durable previo a red. Un estado ambiguo jamás se reenvía automáticamente. */
public final class CorreosInstitucionales {
 private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final NotificacionesInstitucionales notices;private final TransporteCorreo transporte;private final Set<String> permitidos;private final Clock clock;private final String campaign;
 private final Consumer<TransporteCorreo.Resultado> despuesDeRed;private final URI enlaceBase;private final MensajesCorreo mensajes;
 public CorreosInstitucionales(BaseJdbc db,CatalogoLaboratorio catalogo,NotificacionesInstitucionales notices,TransporteCorreo transporte,Set<String> permitidos,Clock clock,String campaign){this(db,catalogo,notices,transporte,permitidos,clock,campaign,r->{});}
 public CorreosInstitucionales(BaseJdbc db,CatalogoLaboratorio catalogo,NotificacionesInstitucionales notices,TransporteCorreo transporte,Set<String> permitidos,Clock clock,String campaign,Consumer<TransporteCorreo.Resultado> despuesDeRed){this(db,catalogo,notices,transporte,permitidos,clock,campaign,despuesDeRed,null);}
 public CorreosInstitucionales(BaseJdbc db,CatalogoLaboratorio catalogo,NotificacionesInstitucionales notices,TransporteCorreo transporte,Set<String> permitidos,Clock clock,String campaign,Consumer<TransporteCorreo.Resultado> despuesDeRed,URI enlaceBase){this.db=db;this.catalogo=catalogo;this.notices=notices;this.transporte=transporte;this.permitidos=Set.copyOf(permitidos);this.clock=clock;this.campaign=campaign;this.despuesDeRed=despuesDeRed;this.enlaceBase=enlaceBase;this.mensajes=new MensajesCorreo(db,clock);if(enlaceBase!=null&&(enlaceBase.toString().length()>2048||!"https".equals(enlaceBase.getScheme())||enlaceBase.getHost()==null||enlaceBase.getUserInfo()!=null||enlaceBase.getQuery()!=null||enlaceBase.getFragment()!=null))throw new IllegalArgumentException("Base HTTPS institucional requerida");if(!campaign.matches("[a-zA-Z0-9_-]{1,64}")||permitidos.size()!=1)throw new IllegalArgumentException("Campaña acotada y allowlist única requeridas");}
 public record Estado(UUID notificacion,String estado,String confirmacion,UUID correlacion,String asunto,int intentos){}
 private record Intento(String claim,String attempt,String recipient,String address){@Override public String toString(){return "IntentoCorreo[reservado]";}}
 private void lock(){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);db.sql.queryForObject("SELECT id FROM notification_guard WHERE id=1 FOR UPDATE",Integer.class);}
 private String subject(String category,String correlation){return MensajesCorreo.asunto(category,correlation);}
 private Estado estado(UUID id){return notices.estado(id);}
 private void state(UUID id,String state){notices.suprimirCorreo(id,state);}
 public Estado consultar(Operador a,UUID id){return notices.estadoCorreo(a,id);}
 public Estado enviar(Operador a,UUID id){return enviar(a,id,false);}
 private Estado enviar(Operador a,UUID id,boolean automatico){
  var claim=db.transaction(()->{
   lock();var n=notices.propia(a,id);var out=db.sql.queryForMap("SELECT * FROM notification_outbox WHERE notification_id=? FOR UPDATE",id.toString());String old=out.get("state").toString();
   if(!Set.of("PENDIENTE","REINTENTABLE").contains(old))return null;
   if(((Number)out.get("next_attempt_ms")).longValue()>clock.millis())throw new FalloTecnico(409,"ESPERAR_REINTENTO");
   if(!notices.enviable(a,n)){state(id,"SUPRIMIDO_CONTEXTO");return null;}
   if(transporte.real()&&enlaceBase==null)throw new FalloTecnico(409,"ENLACE_INSTITUCIONAL_NO_CONFIGURADO");
   // Solo filas PENDIENTE/REINTENTABLE emitidas por la proyección funcional.
   // NO_APLICA previo no se promueve ni se envía retrospectivamente.
   var account=db.sql.queryForMap("SELECT email,enabled FROM staff_account WHERE actor_id=?",a.id());if(!Boolean.TRUE.equals(account.get("enabled")))throw new FalloTecnico(401);
   String address=account.get("email").toString().toLowerCase(Locale.ROOT);if(!permitidos.contains(address)){state(id,"BLOQUEADO_ALLOWLIST");return null;}
   if(transporte.real()){
    if(db.sql.queryForObject("SELECT COUNT(*) FROM mail_campaign WHERE id=?",Integer.class,campaign)==0)db.sql.update("INSERT INTO mail_campaign VALUES(?,0,4)",campaign);
    var c=db.sql.queryForMap("SELECT sends_started,maximum_sends FROM mail_campaign WHERE id=? FOR UPDATE",campaign);if(((Number)c.get("sends_started")).intValue()>=((Number)c.get("maximum_sends")).intValue())throw new FalloTecnico(409,"CAMPANA_AGOTADA");
    db.sql.update("UPDATE mail_campaign SET sends_started=sends_started+1 WHERE id=?",campaign);
   }
   String c=UUID.randomUUID().toString(),attempt=UUID.randomUUID().toString();db.sql.update("UPDATE notification_outbox SET state='RESERVADO',claim_id=?,lease_until=?,attempt_count=attempt_count+1 WHERE notification_id=?",c,clock.millis()+90000,id.toString());
   db.sql.update("INSERT INTO mail_attempt(id,notification_id,recipient_id,resolved_email,started_ms,state,stage_code) VALUES(?,?,?,?,?,'RESERVADO','NINGUNA')",attempt,id.toString(),a.id(),address,clock.millis());String correlacion=out.get("correlation_id").toString(),enlace=(enlaceBase==null?"":enlaceBase.toString())+"#notificacion/"+id;
   String texto="Hay un aviso institucional en SIGER. Accede con tu sesión funcionaria: "+enlace+"\n\nEste mensaje no confirma una asignación, una gestión ni la entrega de este correo. Correlación: "+correlacion;
   mensajes.guardar(id,UUID.fromString(attempt),automatico?null:a.id(),new TransporteCorreo.Mensaje(address,subject(n.get("category").toString(),correlacion),texto,correlacion));return new Intento(c,attempt,a.id(),address);
  });
  if(claim==null)return estado(id);
  TransporteCorreo.Mensaje message=db.transaction(()->{
   lock();var current=catalogo.operador(claim.recipient());var rows=db.sql.queryForList("SELECT * FROM institution_notification WHERE id=?",id.toString());
   if(rows.isEmpty()||!notices.enviable(current,rows.getFirst())){state(id,"SUPRIMIDO_PERMISOS");db.sql.update("UPDATE mail_attempt SET state='SUPRIMIDO_PERMISOS',finished_ms=? WHERE id=?",clock.millis(),claim.attempt());return null;}
   if(!db.sql.queryForObject("SELECT email FROM staff_account WHERE actor_id=?",String.class,current.id()).equalsIgnoreCase(claim.address())){state(id,"SUPRIMIDO_PERMISOS");db.sql.update("UPDATE mail_attempt SET state='SUPRIMIDO_PERMISOS',finished_ms=? WHERE id=?",clock.millis(),claim.attempt());return null;}
   TransporteCorreo.Mensaje materializado;
   try{materializado=mensajes.paraEnvio(id,UUID.fromString(claim.attempt()));}catch(FalloTecnico invalid){if(!Set.of(404,409).contains(invalid.status))throw invalid;state(id,"BLOQUEADO_INTEGRIDAD");db.sql.update("UPDATE mail_attempt SET state='BLOQUEADO_INTEGRIDAD',stage_code='CONSTANCIA',finished_ms=? WHERE id=?",clock.millis(),claim.attempt());return null;}
   if(db.sql.update("UPDATE notification_outbox SET state='EN_INTENTO' WHERE notification_id=? AND claim_id=? AND state='RESERVADO'",id.toString(),claim.claim())!=1)throw new FalloTecnico(409);
   db.sql.update("UPDATE mail_attempt SET state='EN_INTENTO',stage_code='PREVIO_RED' WHERE id=?",claim.attempt());return materializado;
  });
  if(message==null)return estado(id);
  TransporteCorreo.Resultado result;
  try{result=transporte.enviar(message);}catch(Exception failure){result=new TransporteCorreo.Resultado("AMBIGUO","GRAPH",null,0,null);}
  despuesDeRed.accept(result); // punto de fallo controlado de persistencia, nunca registra el objeto HTTP.
  var outcome=result;
  db.transaction(()->{lock();String human=outcome.estado().equals("ACCEPTED_BY_GRAPH")||outcome.estado().equals("AMBIGUO")?"PENDIENTE_CONFIRMACION_USUARIO":"NO_ENVIADO";
   if(db.sql.update("UPDATE notification_outbox SET state=?,human_state=?,next_attempt_ms=?,claim_id=NULL,lease_until=NULL WHERE notification_id=? AND claim_id=? AND state='EN_INTENTO'",outcome.estado(),human,clock.millis()+Math.max(1,outcome.reintentarEnSegundos())*1000,id.toString(),claim.claim())!=1)throw new FalloTecnico(409);
   db.sql.update("UPDATE mail_attempt SET state=?,stage_code=?,http_status=?,remote_request_id=?,finished_ms=? WHERE id=?",outcome.estado(),outcome.fase(),outcome.http(),outcome.requestId(),clock.millis(),claim.attempt());return null;});
  return estado(id);
 }
 public void recuperar(){notices.recuperarCorreos();}
 public void confirmar(Operador a,UUID id){notices.confirmarCorreo(a,id);}
 public List<Map<String,Object>> intentos(Operador a,UUID id){return notices.intentosCorreo(a,id);}
 public MensajesCorreo.Copia copia(Operador a,UUID aviso,UUID intento){return notices.copiaCorreo(a,aviso,intento);}
 public int automaticos(int max){recuperar();int count=0;for(var r:db.sql.queryForList("SELECT o.notification_id,n.recipient_id FROM notification_outbox o JOIN institution_notification n ON n.id=o.notification_id WHERE o.state IN ('PENDIENTE','REINTENTABLE') AND o.next_attempt_ms<=? ORDER BY n.seq LIMIT ?",clock.millis(),Math.min(max,4)))try{enviar(catalogo.operador(r.get("recipient_id").toString()),UUID.fromString(r.get("notification_id").toString()),true);count++;}catch(FalloTecnico denied){if(!Set.of(401,403,404,409).contains(denied.status))throw denied;}return count;}
}
