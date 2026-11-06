package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
/** Proyección transaccional sin cursor de máximos: los commits tardíos no se pierden. */
public final class NotificacionesInstitucionales {
 private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final RecursosNotificacion permisos;private final Clock clock;
 // §16: cada Aviso emitido representa un evento funcional habilitado en ambos canales.
 // Las consultas/auditorías sin señal no generan aviso; NO_APLICA histórico se conserva.
 public NotificacionesInstitucionales(BaseJdbc db,CatalogoLaboratorio catalogo,RecursosNotificacion permisos,Clock clock){this.db=db;this.catalogo=catalogo;this.permisos=permisos;this.clock=clock;if(!catalogo.individual())throw new IllegalStateException("Se requieren cuentas individuales");}
 public record Notificacion(long secuencia,UUID id,String tipo,String titulo,Instant fecha,boolean leida,String ruta,String estadoCorreo,String confirmacionCorreo,UUID correlacion){}
 public record Bandeja(List<Notificacion> items,long pendientes,long cursor){}
 private record Aviso(String categoria,String tipo,UUID recurso){}
 private static UUID uuid(Object o){return UUID.fromString(o.toString());}
 private static long num(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
 public static String titulo(String category){return switch(category){case "SUBROGANCIA_ACTUALIZADA"->"Subrogancia institucional actualizada";case "EQUIPO_ACTUALIZADO"->"Participación en equipo actualizada";case "ESPERA_PENDIENTE"->"Espera de antecedentes pendiente de decisión";case "CONTINUIDAD_SOLICITADA"->"Decisión institucional de continuidad pendiente";case "CONTINUIDAD_DECIDIDA"->"Decisión institucional de continuidad registrada";case "PERTINENCIA_ACTUALIZADA"->"Antecedentes del Caso actualizados";case "MATERIA_ACTUALIZADA"->"Materia de Caso actualizada";case "COMUNICACION_NUEVA"->"Nueva Comunicación pendiente de distribución";case "DESTINO_PENDIENTE"->"Comunicación recibida para evaluación";case "CASO_ASIGNADO"->"Caso asignado o reasignado";case "ANTECEDENTE_NUEVO"->"Nuevos antecedentes por evaluar";case "VINCULO_PENDIENTE"->"Comunicación vinculada pendiente de encaminamiento";case "REVISION_TERMINO"->"Revisión de término pendiente";case "DERIVACION_PENDIENTE"->"Derivación pendiente de decisión";case "REAPERTURA_PENDIENTE"->"Reapertura pendiente de decisión";case "TAREA_RECORDATORIO"->"Recordatorio de tarea personal";case "TAREA_VENCIDA"->"Plazo de tarea personal vencido";case "SOLICITUD_CREADA"->"Preparación de Solicitud registrada";case "SOLICITUD_MODIFICADA"->"Preparación de Solicitud actualizada";case "SOLICITUD_PREPARADA"->"Borrador de gestión preparado";case "SOLICITUD_SEGUIMIENTO"->"Seguimiento de Solicitud registrado";case "TAREA_CREADA"->"Tarea personal creada";default->"Tarea personal actualizada";};}
 private List<Aviso> avisos(String stream,Map<String,Object> e){
  String code=e.get("code").toString();UUID resource=uuid(e.get(stream.equals("RECEPCION")?"aggregate_id":"resource_id"));
  if(stream.equals("RECEPCION"))return code.equals("COMUNICACION_REGISTRADA")?List.of(new Aviso("COMUNICACION_NUEVA","COMUNICACION",resource)):List.of();
  if(stream.equals("TAREA")&&Set.of("REV_CIERRE","RESERVA_DOC").contains(e.get("resource_type")))return List.of(new Aviso("CONTINUIDAD_DECIDIDA",e.get("resource_type").toString(),resource));
  if(stream.equals("TAREA"))return List.of(new Aviso(code,"SUBROGANCIA".equals(e.get("resource_type"))?"SUBROGANCIA":"SOLICITUD".equals(e.get("resource_type"))?"SOLICITUD":"TAREA",resource));
  return switch(code){case "RESERVA_PROPUESTA"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","RESERVA_DOC",resource));case "RESERVA_APROBAR","RESERVA_RECHAZAR","RESERVA_RETIRAR","RESERVA_REVOCAR"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","RESERVA_DOC",resource));case "CIERRE_EXCEPCION_SOLICITADA"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","REV_CIERRE",resource));case "CIERRE_EXCEPCION_APROBAR","CIERRE_EXCEPCION_RECHAZAR","CIERRE_EXCEPCION_REVOCAR","CIERRE_EXCEPCION_RETIRAR","CIERRE_EXCEPCION_UTILIZADA"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","REV_CIERRE",resource));case "EQUIPO_PARTICIPACION_ACTUALIZADA"->List.of(new Aviso("EQUIPO_ACTUALIZADO","EQUIPO",resource));case "ESPERA_ANTECEDENTES_SOLICITADA"->List.of(new Aviso("ESPERA_PENDIENTE","CASO",resource));
   case "COLABORACION_TERMINO_SOLICITADO"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","TERMINO_COLAB",resource));
   case "COLABORACION_TERMINO_ACEPTADA","COLABORACION_TERMINO_RECHAZADA","COLABORACION_TERMINO_RETIRADA"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","TERMINO_COLAB",resource));
   case "PRINCIPAL_TRANSFERENCIA_SOLICITADA"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","TRASLADO_REQ_U2",resource));
   case "DERIVACION_INSTITUCIONAL_SOLICITADA"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","DERIVACION_U2",resource));
   case "REAPERTURA_INSTITUCIONAL_SOLICITADA"->List.of(new Aviso("CONTINUIDAD_SOLICITADA","REAPERTURA_U2",resource));
   case "PRINCIPAL_ACEPTADA","PRINCIPAL_RECHAZADA","PRINCIPAL_RETIRADA"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","TRASLADO_REQ_U2",resource));
   case "DERIVACION_ACEPTADA","DERIVACION_RECHAZADA","DERIVACION_RETIRADA"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","DERIVACION_U2",resource));
   case "REAPERTURA_APROBADA","REAPERTURA_RECHAZADA"->List.of(new Aviso("CONTINUIDAD_DECIDIDA","REAPERTURA_U2",resource));
   case "ANTECEDENTE_PERTINENTE","COM_VALORADA_EN_CONTEXTO"->List.of(new Aviso("PERTINENCIA_ACTUALIZADA","CASO_CONSULTA",resource));
   case "MATERIA_RECTIFICADA"->List.of(new Aviso("MATERIA_ACTUALIZADA","CASO_CONSULTA",resource));
   case "COMUNICACION_MANUAL_REGISTRADA"->List.of(new Aviso("COMUNICACION_NUEVA","COMUNICACION",resource));
   case "DESTINO_DISTRIBUIDO"->List.of(new Aviso("DESTINO_PENDIENTE","DESTINO",resource));
   case "CASO_ABIERTO","RESPONSABLE_Y_COMPLEJIDAD_REGISTRADOS"->List.of(new Aviso("CASO_ASIGNADO","CASO",resource));
   case "VINCULO_DOCUMENTAL_CREADO"->{var child=uuid(e.get("communication_id"));if(db.sql.queryForObject("SELECT COUNT(*) FROM communication_pertinence WHERE child_id=?",Long.class,child.toString())>0)yield List.of();var links=db.sql.queryForList("SELECT case_reference FROM core_link WHERE child_id=?",child.toString());yield links.isEmpty()?List.of():List.of(new Aviso(links.getFirst().get("case_reference")==null?"VINCULO_PENDIENTE":"ANTECEDENTE_NUEVO","ANTECEDENTE",child));}
   case "TERMINO_SOLICITADO"->List.of(new Aviso("REVISION_TERMINO","CASO",resource));
   case "DERIVACION_SOLICITADA_ORIGEN_ABIERTO"->db.sql.queryForList("SELECT id FROM core_transfer WHERE origin_case_id=? AND state='PENDIENTE'",resource.toString()).stream().map(r->new Aviso("DERIVACION_PENDIENTE","DERIVACION",uuid(r.get("id")))).toList();
   case "REAPERTURA_SOLICITADA_CASO_CERRADO"->db.sql.queryForList("SELECT id FROM core_reopen WHERE case_id=? AND state='PENDIENTE'",resource.toString()).stream().map(r->new Aviso("REAPERTURA_PENDIENTE","REAPERTURA",uuid(r.get("id")))).toList();
   default->List.of();
  };
 }
 private boolean autorizado(Operador a,String categoria,String tipo,UUID id,String eventKey,String stream){
  try{
   permisos.actor(a);
   switch(tipo){case "RESERVA_DOC"->{if(!permisos.nucleo().accesosReservados().autorizarAviso(a,id))return false;}case "REV_CIERRE"->{if(!permisos.nucleo().revisionesExcepcionales().autorizarAviso(a,id))return false;}case "TERMINO_COLAB"->permisos.terminoColaboracion(a,id,categoria.equals("CONTINUIDAD_SOLICITADA"));
    case "EQUIPO"->permisos.equipo(a,id);
    case "TRASLADO_REQ_U2","DERIVACION_U2","REAPERTURA_U2"->permisos.continuidad(a,tipo,id,categoria.equals("CONTINUIDAD_SOLICITADA"));
    case "SUBROGANCIA"->{permisos.recurso(a,tipo,id);}
    case "COMUNICACION"->{var rows=db.sql.queryForList("SELECT site_id,state FROM core_communication WHERE id=?",id.toString());String site=rows.isEmpty()?null:(String)rows.getFirst().get("site_id");if(!a.distribuyeSede(site))return false;permisos.recurso(a,tipo,id);}
    case "DESTINO"->{var d=permisos.fila("core_destination",id.toString());permisos.destino(a,d);}
    case "CASO_CONSULTA"->{permisos.recurso(a,tipo,id);}
    case "CASO"->{var c=permisos.caso(a,id);if(categoria.equals("CASO_ASIGNADO")){if(!a.id().equals(c.responsable()))return false;}else {if(!a.jefeEn(c.unidad(),c.sede(),c.area()))return false;var d=permisos.fila("core_destination",c.destinoId().toString());if(!categoria.equals("ESPERA_PENDIENTE")&&!a.id().equals(d.get("evaluator_id")))return false;}}
    case "ANTECEDENTE"->{permisos.antecedente(a,id);var l=db.sql.queryForMap("SELECT * FROM core_link WHERE child_id=?",id.toString());if(categoria.equals("ANTECEDENTE_NUEVO")){if(l.get("case_reference")==null)return false;var c=permisos.caso(a,uuid(l.get("case_reference")));if(!a.id().equals(c.responsable())||!a.profesionalEn(c.unidad(),c.sede(),c.area()))return false;}else if(!a.distribuidor()&&db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND evaluator_id=?",Integer.class,l.get("parent_id"),a.id())==0)return false;}
    case "DERIVACION"->{permisos.derivacion(a,id);}
    case "REAPERTURA"->{permisos.reapertura(a,id);var r=permisos.fila("core_reopen",id.toString());var c=permisos.caso(a,uuid(r.get("case_id")));if(!a.id().equals(permisos.fila("core_destination",c.destinoId().toString()).get("evaluator_id")))return false;}
    case "SOLICITUD"->permisos.solicitud(a,id);
    case "TAREA"->{var t=permisos.fila("institution_task",id.toString());permisos.tarea(a,t);if(!a.id().equals(t.get("responsible_id"))&&!a.id().equals(t.get("creator_id")))return false;if(Set.of("TAREA_RECORDATORIO","TAREA_VENCIDA").contains(categoria)&&!a.id().equals(t.get("responsible_id")))return false;}

    default->{return false;}
   }
   return true;
  }catch(FalloTecnico unavailable){if(Set.of(401,403,404,409).contains(unavailable.status))return false;throw unavailable;}
 }
 private boolean temporalVigente(String categoria,UUID id,String eventKey){if(!Set.of("TAREA_RECORDATORIO","TAREA_VENCIDA").contains(categoria))return true;var t=permisos.fila("institution_task",id.toString());if(!Set.of("PENDIENTE","EN_CURSO").contains(t.get("state")))return false;Object planned=t.get(categoria.equals("TAREA_RECORDATORIO")?"reminder_ms":"deadline_ms");if(planned==null||((Number)planned).longValue()>clock.millis())return false;var rows=db.sql.queryForList("SELECT event_key FROM workflow_event WHERE seq=?",Long.parseLong(eventKey));return !rows.isEmpty()&&rows.getFirst().get("event_key").equals(id+":"+categoria+":"+planned+(((Number)t.get("reminder_generation")).longValue()==0?"":":G"+t.get("reminder_generation")));}
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
 /** Mantiene el límite de trabajo por llamada, con una transacción atómica por fuente. */
 public int proyectar(){
  int inserted=0;var candidatos=new Candidatos();
  for(var stream:List.of("CORE","RECEPCION","TAREA"))for(int i=0;i<20;i++){
   Integer count=proyectarSiguiente(stream,candidatos);if(count==null)break;inserted+=count;
  }
  return inserted;
 }
 // Solo la enumeración del lote: autorizado() revalida cada identidad y recurso, sin cachear permisos.
 private final class Candidatos {private List<Operador> lista;List<Operador> obtener(){if(lista==null)lista=catalogo.operadores();return lista;}}
 private Integer proyectarSiguiente(String stream,Candidatos candidatos){return db.transaction(()->{
  db.sql.queryForObject("SELECT id FROM notification_guard WHERE id=1 FOR UPDATE",Integer.class);int inserted=0;
   String table=switch(stream){case "CORE"->"core_event";case "RECEPCION"->"operational_event";default->"workflow_event";};
   var rows=db.sql.queryForList("SELECT e.* FROM "+table+" e WHERE NOT EXISTS(SELECT 1 FROM notification_source s WHERE s.stream_code=? AND s.event_key=CONCAT('',e.seq)) ORDER BY e.seq LIMIT 1",stream);
   if(rows.isEmpty())return null;
   var e=rows.getFirst();String event=e.get("seq").toString();int recipients=0;
    boolean relevant=!stream.equals("RECEPCION")||"COMUNICACION_REGISTRADA".equals(e.get("code"))&&"COMUNICACION".equals(e.get("category"));
    List<Aviso> signals=relevant?avisos(stream,e):List.of();
    // No cargar identidades para eventos sin aviso ni para colas vacías; no se omiten autorizaciones.
    if(!signals.isEmpty())for(var a:candidatos.obtener())for(var signal:signals)if(autorizado(a,signal.categoria(),signal.tipo(),signal.recurso(),event,stream)&&accionVigente(signal.categoria(),signal.tipo(),signal.recurso(),event)){
     if(signal.categoria().equals("COMUNICACION_NUEVA")&&(db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Integer.class,signal.recurso().toString())>0||db.sql.queryForObject("SELECT COUNT(*) FROM core_communication WHERE id=? AND state<>'ABIERTA'",Integer.class,signal.recurso().toString())>0))continue;
     String id=UUID.randomUUID().toString();db.sql.update("INSERT INTO institution_notification(id,stream_code,event_key,recipient_id,category,resource_type,resource_id,created_ms) VALUES(?,?,?,?,?,?,?,?)",id,stream,event,a.id(),signal.categoria(),signal.tipo(),signal.recurso().toString(),((Number)e.get("at_ms")).longValue());
     db.sql.update("INSERT INTO notification_outbox(notification_id,correlation_id,state,human_state,attempt_count,next_attempt_ms) VALUES(?,?,?,'NO_ENVIADO',0,?)",id,UUID.randomUUID().toString(),"PENDIENTE",clock.millis());recipients++;inserted++;
    }
    db.sql.update("INSERT INTO notification_source VALUES(?,?,?,?)",stream,event,clock.millis(),recipients==0?"SIN_DESTINATARIO_VIGENTE":"PROYECTADO");
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
