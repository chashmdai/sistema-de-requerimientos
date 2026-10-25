package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
/** Proyección transaccional sin cursor de máximos: los commits tardíos no se pierden. */
public final class NotificacionesInstitucionales {
 private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final RecursosNotificacion permisos;private final Clock clock;
 public static final Set<String> CORREO=Set.of("CASO_ASIGNADO","ANTECEDENTE_NUEVO","TAREA_RECORDATORIO","TAREA_VENCIDA");
 public NotificacionesInstitucionales(BaseJdbc db,CatalogoLaboratorio catalogo,RecursosNotificacion permisos,Clock clock){this.db=db;this.catalogo=catalogo;this.permisos=permisos;this.clock=clock;if(!catalogo.individual())throw new IllegalStateException("Se requieren cuentas individuales");}
 public record Notificacion(long secuencia,UUID id,String tipo,String titulo,Instant fecha,boolean leida,String ruta,String estadoCorreo,String confirmacionCorreo,UUID correlacion){}
 public record Bandeja(List<Notificacion> items,long pendientes,long cursor){}
 private record Aviso(String categoria,String tipo,UUID recurso){}
 private static UUID uuid(Object o){return UUID.fromString(o.toString());}
 private static long num(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
 public static String titulo(String category){return switch(category){case "COMUNICACION_NUEVA"->"Nueva Comunicación pendiente de distribución";case "DESTINO_PENDIENTE"->"Comunicación recibida para evaluación";case "CASO_ASIGNADO"->"Caso asignado o reasignado";case "ANTECEDENTE_NUEVO"->"Nuevos antecedentes por evaluar";case "VINCULO_PENDIENTE"->"Comunicación vinculada pendiente de encaminamiento";case "REVISION_TERMINO"->"Revisión de término pendiente";case "DERIVACION_PENDIENTE"->"Derivación pendiente de decisión";case "REAPERTURA_PENDIENTE"->"Reapertura pendiente de decisión";case "TAREA_RECORDATORIO"->"Recordatorio de tarea personal";case "TAREA_VENCIDA"->"Plazo de tarea personal vencido";case "TAREA_CREADA"->"Tarea personal creada";default->"Tarea personal actualizada";};}
 private List<Aviso> avisos(String stream,Map<String,Object> e){
  String code=e.get("code").toString();UUID resource=uuid(e.get(stream.equals("RECEPCION")?"aggregate_id":"resource_id"));
  if(stream.equals("RECEPCION"))return code.equals("COMUNICACION_REGISTRADA")?List.of(new Aviso("COMUNICACION_NUEVA","COMUNICACION",resource)):List.of();
  if(stream.equals("TAREA"))return List.of(new Aviso(code,"TAREA",resource));
  return switch(code){
   case "COMUNICACION_MANUAL_REGISTRADA"->List.of(new Aviso("COMUNICACION_NUEVA","COMUNICACION",resource));
   case "DESTINO_DISTRIBUIDO"->List.of(new Aviso("DESTINO_PENDIENTE","DESTINO",resource));
   case "CASO_ABIERTO","RESPONSABLE_Y_COMPLEJIDAD_REGISTRADOS"->List.of(new Aviso("CASO_ASIGNADO","CASO",resource));
   case "VINCULO_DOCUMENTAL_CREADO"->{var child=uuid(e.get("communication_id"));var links=db.sql.queryForList("SELECT case_reference FROM core_link WHERE child_id=?",child.toString());yield links.isEmpty()?List.of():List.of(new Aviso(links.getFirst().get("case_reference")==null?"VINCULO_PENDIENTE":"ANTECEDENTE_NUEVO","ANTECEDENTE",child));}
   case "TERMINO_SOLICITADO"->List.of(new Aviso("REVISION_TERMINO","CASO",resource));
   case "DERIVACION_SOLICITADA_ORIGEN_ABIERTO"->db.sql.queryForList("SELECT id FROM core_transfer WHERE origin_case_id=? AND state='PENDIENTE'",resource.toString()).stream().map(r->new Aviso("DERIVACION_PENDIENTE","DERIVACION",uuid(r.get("id")))).toList();
   case "REAPERTURA_SOLICITADA_CASO_CERRADO"->db.sql.queryForList("SELECT id FROM core_reopen WHERE case_id=? AND state='PENDIENTE'",resource.toString()).stream().map(r->new Aviso("REAPERTURA_PENDIENTE","REAPERTURA",uuid(r.get("id")))).toList();
   default->List.of();
  };
 }
 private boolean autorizado(Operador a,String categoria,String tipo,UUID id,String eventKey,String stream){
  try{
   permisos.actor(a);
   switch(tipo){
    case "COMUNICACION"->{var rows=db.sql.queryForList("SELECT site_id,state FROM core_communication WHERE id=?",id.toString());String site=rows.isEmpty()?null:(String)rows.getFirst().get("site_id");if(!a.distribuyeSede(site))return false;permisos.recurso(a,tipo,id);}
    case "DESTINO"->{var d=permisos.fila("core_destination",id.toString());permisos.destino(a,d);}
    case "CASO"->{var c=permisos.caso(a,id);if(categoria.equals("CASO_ASIGNADO")){if(!a.id().equals(c.responsable()))return false;}else {if(!a.jefeEn(c.unidad(),c.sede()))return false;var d=permisos.fila("core_destination",c.destinoId().toString());if(!a.id().equals(d.get("evaluator_id")))return false;}}
    case "ANTECEDENTE"->{permisos.antecedente(a,id);var l=db.sql.queryForMap("SELECT * FROM core_link WHERE child_id=?",id.toString());if(categoria.equals("ANTECEDENTE_NUEVO")){if(l.get("case_reference")==null)return false;var c=permisos.caso(a,uuid(l.get("case_reference")));if(!a.id().equals(c.responsable())||!a.profesionalEn(c.unidad(),c.sede()))return false;}else if(!a.distribuidor()&&db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND evaluator_id=?",Integer.class,l.get("parent_id"),a.id())==0)return false;}
    case "DERIVACION"->{permisos.derivacion(a,id);}
    case "REAPERTURA"->{permisos.reapertura(a,id);var r=permisos.fila("core_reopen",id.toString());var c=permisos.caso(a,uuid(r.get("case_id")));if(!a.id().equals(permisos.fila("core_destination",c.destinoId().toString()).get("evaluator_id")))return false;}
    case "TAREA"->{var t=permisos.fila("institution_task",id.toString());permisos.tarea(a,t);}

    default->{return false;}
   }
   return true;
  }catch(FalloTecnico unavailable){if(Set.of(401,403,404,409).contains(unavailable.status))return false;throw unavailable;}
 }
 private boolean temporalVigente(String categoria,UUID id,String eventKey){if(!Set.of("TAREA_RECORDATORIO","TAREA_VENCIDA").contains(categoria))return true;var t=permisos.fila("institution_task",id.toString());if(!Set.of("PENDIENTE","EN_CURSO").contains(t.get("state")))return false;Object planned=t.get(categoria.equals("TAREA_RECORDATORIO")?"reminder_ms":"deadline_ms");if(planned==null||((Number)planned).longValue()>clock.millis())return false;var rows=db.sql.queryForList("SELECT event_key FROM workflow_event WHERE seq=?",Long.parseLong(eventKey));return !rows.isEmpty()&&rows.getFirst().get("event_key").equals(id+":"+categoria+":"+planned);}
 private boolean accionVigente(String c,String tipo,UUID id,String key){
  if(!temporalVigente(c,id,key))return false;
  return switch(c){
   case "COMUNICACION_NUEVA"->db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Integer.class,id.toString())==0&&db.sql.queryForObject("SELECT COUNT(*) FROM core_communication WHERE id=? AND state<>'ABIERTA'",Integer.class,id.toString())==0;
   case "DESTINO_PENDIENTE"->permisos.fila("core_destination",id.toString()).get("state").equals("EVALUANDO");
   case "REVISION_TERMINO"->permisos.fila("core_case",id.toString()).get("state").equals("REVISION_TERMINO");
   case "DERIVACION_PENDIENTE"->permisos.fila("core_transfer",id.toString()).get("state").equals("PENDIENTE");
   case "REAPERTURA_PENDIENTE"->permisos.fila("core_reopen",id.toString()).get("state").equals("PENDIENTE");
   case "ANTECEDENTE_NUEVO"->db.sql.queryForObject("SELECT COUNT(*) FROM core_link_evaluation WHERE child_id=?",Integer.class,id.toString())==0;
   default->true;
  };
 }
 public boolean enviable(Operador a,Map<String,Object> n){return visible(a,n)&&accionVigente(n.get("category").toString(),n.get("resource_type").toString(),uuid(n.get("resource_id")),n.get("event_key").toString());}
 public boolean visible(Operador a,Map<String,Object> n){return a.id().equals(n.get("recipient_id"))&&autorizado(a,n.get("category").toString(),n.get("resource_type").toString(),uuid(n.get("resource_id")),n.get("event_key").toString(),n.get("stream_code").toString());}
 public int proyectar(){return db.transaction(()->{
  db.sql.queryForObject("SELECT id FROM notification_guard WHERE id=1 FOR UPDATE",Integer.class);int inserted=0;var actors=catalogo.operadores();
  for(var stream:List.of("CORE","RECEPCION","TAREA")){
   String table=switch(stream){case "CORE"->"core_event";case "RECEPCION"->"operational_event";default->"workflow_event";};
   var rows=db.sql.queryForList("SELECT e.* FROM "+table+" e WHERE NOT EXISTS(SELECT 1 FROM notification_source s WHERE s.stream_code=? AND s.event_key=CONCAT('',e.seq)) ORDER BY e.seq LIMIT 20",stream);
   for(var e:rows){String event=e.get("seq").toString();int recipients=0;
    boolean relevant=!stream.equals("RECEPCION")||"COMUNICACION_REGISTRADA".equals(e.get("code"))&&"COMUNICACION".equals(e.get("category"));
    if(relevant)for(var signal:avisos(stream,e))for(var a:actors)if(autorizado(a,signal.categoria(),signal.tipo(),signal.recurso(),event,stream)&&accionVigente(signal.categoria(),signal.tipo(),signal.recurso(),event)){
     if(signal.categoria().equals("COMUNICACION_NUEVA")&&(db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Integer.class,signal.recurso().toString())>0||db.sql.queryForObject("SELECT COUNT(*) FROM core_communication WHERE id=? AND state<>'ABIERTA'",Integer.class,signal.recurso().toString())>0))continue;
     String id=UUID.randomUUID().toString();db.sql.update("INSERT INTO institution_notification(id,stream_code,event_key,recipient_id,category,resource_type,resource_id,created_ms) VALUES(?,?,?,?,?,?,?,?)",id,stream,event,a.id(),signal.categoria(),signal.tipo(),signal.recurso().toString(),((Number)e.get("at_ms")).longValue());
     db.sql.update("INSERT INTO notification_outbox(notification_id,correlation_id,state,human_state,attempt_count,next_attempt_ms) VALUES(?,?,?,'NO_ENVIADO',0,?)",id,UUID.randomUUID().toString(),CORREO.contains(signal.categoria())?"PENDIENTE":"NO_APLICA",clock.millis());recipients++;inserted++;
    }
    db.sql.update("INSERT INTO notification_source VALUES(?,?,?,?)",stream,event,clock.millis(),recipients==0?"SIN_DESTINATARIO_VIGENTE":"PROYECTADO");
   }
  }
  return inserted;
 });}
 public Map<String,Object> propia(Operador a,UUID id){permisos.actor(a);var rows=db.sql.queryForList("SELECT n.*,o.state AS mail_state,o.human_state,o.correlation_id FROM institution_notification n JOIN notification_outbox o ON o.notification_id=n.id WHERE n.id=? AND n.recipient_id=?",id.toString(),a.id());if(rows.isEmpty()||!visible(a,rows.getFirst()))throw new FalloTecnico(404);return rows.getFirst();}
 private Notificacion vista(Map<String,Object> r){return new Notificacion(num(r,"seq"),uuid(r.get("id")),r.get("category").toString(),titulo(r.get("category").toString()),Instant.ofEpochMilli(num(r,"created_ms")),r.get("read_ms")!=null,permisos.href(r.get("resource_type").toString(),uuid(r.get("resource_id"))),r.get("mail_state").toString(),r.get("human_state").toString(),uuid(r.get("correlation_id")));}
 public long pendientes(Operador a){permisos.actor(a);long count=0;for(var row:db.sql.queryForList("SELECT * FROM institution_notification WHERE recipient_id=? AND read_ms IS NULL ORDER BY seq",a.id()))if(visible(a,row))count++;return count;}
 public Bandeja listar(Operador a,long despues,int limite){permisos.actor(a);if(despues<0||limite<1||limite>50)throw new FalloTecnico(400);var result=new ArrayList<Notificacion>();long cursor=despues;
  while(result.size()<limite){var rows=db.sql.queryForList("SELECT n.*,o.state AS mail_state,o.human_state,o.correlation_id FROM institution_notification n JOIN notification_outbox o ON o.notification_id=n.id WHERE n.recipient_id=? AND n.seq>? ORDER BY n.seq LIMIT 50",a.id(),cursor);if(rows.isEmpty())break;for(var r:rows){cursor=num(r,"seq");if(visible(a,r))result.add(vista(r));if(result.size()==limite)break;}if(rows.size()<50)break;}
  return new Bandeja(result,pendientes(a),cursor);
 }
 public void leer(Operador a,UUID id){db.transaction(()->{db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);propia(a,id);db.sql.update("UPDATE institution_notification SET read_ms=? WHERE id=? AND recipient_id=? AND read_ms IS NULL",clock.millis(),id.toString(),a.id());return null;});}
}
