package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;import java.util.*;
/** La notificación referencia una autorización existente; nunca es una credencial de acceso. */
public final class RecursosNotificacion {
 private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final NucleoInstitucional core;
 public RecursosNotificacion(BaseJdbc db,CatalogoLaboratorio catalogo,NucleoInstitucional core){this.db=db;this.catalogo=catalogo;this.core=core;}
 public void continuidad(Operador a,String tipo,UUID id,boolean solicitud){core.continuidad().autorizarAviso(a,tipo,id,solicitud);}
 public void equipo(Operador a,UUID id){core.equipo().autorizarAviso(a,id);}
 public NucleoInstitucional nucleo(){return core;}
 public void actor(Operador a){if(a==null)throw new FalloTecnico(401);if(!catalogo.operador(a.id()).equals(a))throw new FalloTecnico(403);}
 public void recurso(Operador a,String tipo,UUID id){
  actor(a);if(tipo==null&&id==null)return;if(tipo==null||id==null)throw new FalloTecnico(400);
  switch(tipo){
   case "COMUNICACION"->core.autorizarLectura(a,id);
   case "CASO"->core.autorizarCaso(a,id);
   case "CASO_CONSULTA"->core.autorizarConsultaCaso(a,id);
   case "REQUERIMIENTO"->{var rows=db.sql.queryForList("SELECT communication_id FROM core_requirement WHERE id=?",id.toString());if(rows.isEmpty())throw new FalloTecnico(404);core.autorizarLectura(a,UUID.fromString(rows.getFirst().get("communication_id").toString()));}
   case "SOLICITUD"->solicitud(a,id);
   default->throw new FalloTecnico(400);
  }
 }
 public Map<String,Object> fila(String tabla,String id){if(!Set.of("institution_task","core_case","core_destination","core_transfer","core_reopen").contains(tabla))throw new IllegalArgumentException();var rows=db.sql.queryForList("SELECT * FROM "+tabla+" WHERE id=?",id);if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
 public Map<String,Object> solicitud(Operador a,UUID id){actor(a);if(id==null)throw new FalloTecnico(400);var rows=db.sql.queryForList("SELECT * FROM institution_request WHERE id=? AND creator_id=? AND responsible_id=?",id.toString(),a.id(),a.id());if(rows.isEmpty())throw new FalloTecnico(404);var r=rows.getFirst();core.autorizarLectura(a,UUID.fromString(r.get("communication_id").toString()));return r;}
 public static String contextoTarea(Map<String,Object> t){return t.get("request_id")!=null?"SOLICITUD":(String)t.get("resource_type");}
 public static UUID recursoTarea(Map<String,Object> t){var id=t.get("request_id")!=null?t.get("request_id"):t.get("resource_id");return id==null?null:UUID.fromString(id.toString());}
 public void tarea(Operador a,Map<String,Object> t){actor(a);if("CASO".equals(contextoTarea(t))){core.autorizarCaso(a,recursoTarea(t));return;}if(!a.id().equals(t.get("creator_id"))||!a.id().equals(t.get("responsible_id")))throw new FalloTecnico(404);recurso(a,contextoTarea(t),recursoTarea(t));}
 public void destino(Operador a,Map<String,Object> d){core.autorizarJefaturaDestino(a,UUID.fromString(d.get("id").toString()));if(!a.id().equals(d.get("evaluator_id")))throw new FalloTecnico(404);core.autorizarLectura(a,UUID.fromString(d.get("communication_id").toString()));}
 public ContratosNucleo.Caso caso(Operador a,UUID id){return core.contextoCaso(a,id);}
 public void derivacion(Operador a,UUID id){core.autorizarDerivacion(a,id);var t=fila("core_transfer",id.toString());if(!a.jefeEn(t.get("target_unit").toString(),t.get("target_site").toString()))throw new FalloTecnico(404);}
 public void reapertura(Operador a,UUID id){core.autorizarReapertura(a,id);var r=fila("core_reopen",id.toString());var c=caso(a,UUID.fromString(r.get("case_id").toString()));if(!a.jefeEn(c.unidad(),c.sede(),c.area()))throw new FalloTecnico(404);}
 public void antecedente(Operador a,UUID id){core.autorizarAntecedente(a,id);}
 public void terminoColaboracion(Operador a,UUID id,boolean pendiente){actor(a);core.terminoColaboracion().autorizarAviso(a,id,pendiente);}
 public String href(String tipo,UUID id){return switch(tipo){case "TERMINO_COLAB","TRASLADO_REQ_U2","DERIVACION_U2","REAPERTURA_U2"->"u2";case "EQUIPO"->"casos";case "COMUNICACION"->"comunicacion/"+id;case "CASO","CASO_CONSULTA"->"caso/"+id;case "SOLICITUD"->"solicitudes/"+id;case "TAREA"->"tareas/"+id;case "ANTECEDENTE"->"antecedentes";case "DESTINO"->{var d=fila("core_destination",id.toString());yield "comunicacion/"+d.get("communication_id");}case "DERIVACION","REAPERTURA"->"decisiones";default->"bandeja";};}
}
