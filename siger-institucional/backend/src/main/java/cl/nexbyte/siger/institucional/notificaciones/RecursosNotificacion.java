package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;import java.util.*;
/** La notificación referencia una autorización existente; nunca es una credencial de acceso. */
public final class RecursosNotificacion {
 private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final NucleoInstitucional core;
 public RecursosNotificacion(BaseJdbc db,CatalogoLaboratorio catalogo,NucleoInstitucional core){this.db=db;this.catalogo=catalogo;this.core=core;}
 public void actor(Operador a){if(a==null)throw new FalloTecnico(401);if(!catalogo.operador(a.id()).equals(a))throw new FalloTecnico(403);}
 public void recurso(Operador a,String tipo,UUID id){
  actor(a);if(tipo==null&&id==null)return;if(tipo==null||id==null)throw new FalloTecnico(400);
  switch(tipo){
   case "COMUNICACION"->core.autorizarLectura(a,id);
   case "CASO"->core.autorizarCaso(a,id);
   case "REQUERIMIENTO"->{var rows=db.sql.queryForList("SELECT communication_id FROM core_requirement WHERE id=?",id.toString());if(rows.isEmpty())throw new FalloTecnico(404);core.autorizarLectura(a,UUID.fromString(rows.getFirst().get("communication_id").toString()));}
   case "SOLICITUD"->throw new FalloTecnico(409,"OBJETO_NO_DISPONIBLE");
   default->throw new FalloTecnico(400);
  }
 }
 public Map<String,Object> fila(String tabla,String id){if(!Set.of("institution_task","core_case","core_destination","core_transfer","core_reopen").contains(tabla))throw new IllegalArgumentException();var rows=db.sql.queryForList("SELECT * FROM "+tabla+" WHERE id=?",id);if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
 public void tarea(Operador a,Map<String,Object> t){actor(a);if(!a.id().equals(t.get("creator_id"))||!a.id().equals(t.get("responsible_id")))throw new FalloTecnico(404);recurso(a,(String)t.get("resource_type"),t.get("resource_id")==null?null:UUID.fromString(t.get("resource_id").toString()));}
 public void destino(Operador a,Map<String,Object> d){if(!a.id().equals(d.get("evaluator_id"))||!a.jefeEn(d.get("unit_code").toString(),d.get("site_id").toString()))throw new FalloTecnico(404);core.autorizarLectura(a,UUID.fromString(d.get("communication_id").toString()));}
 public ContratosNucleo.Caso caso(Operador a,UUID id){return core.contextoCaso(a,id);}
 public void derivacion(Operador a,UUID id){core.autorizarDerivacion(a,id);var t=fila("core_transfer",id.toString());if(!a.jefeEn(t.get("target_unit").toString(),t.get("target_site").toString()))throw new FalloTecnico(404);}
 public void reapertura(Operador a,UUID id){core.autorizarReapertura(a,id);var r=fila("core_reopen",id.toString());var c=caso(a,UUID.fromString(r.get("case_id").toString()));if(!a.jefeEn(c.unidad(),c.sede()))throw new FalloTecnico(404);}
 public void antecedente(Operador a,UUID id){core.autorizarAntecedente(a,id);}
 public String href(String tipo,UUID id){return switch(tipo){case "COMUNICACION"->"comunicacion/"+id;case "CASO"->"caso/"+id;case "TAREA"->"tareas/"+id;case "ANTECEDENTE"->"antecedentes";case "DESTINO"->{var d=fila("core_destination",id.toString());yield "comunicacion/"+d.get("communication_id");}case "DERIVACION","REAPERTURA"->"decisiones";default->"bandeja";};}
}
