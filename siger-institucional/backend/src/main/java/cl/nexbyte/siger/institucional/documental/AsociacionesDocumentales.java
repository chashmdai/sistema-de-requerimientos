package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.formulario.EntregaCompleta;
import cl.nexbyte.siger.institucional.robustez.*;
import java.security.*;import java.time.*;import java.util.*;

/** Rectifica referencias del propietario, sin mover custodia, crear gestión o sustituir autorizaciones. */
public final class AsociacionesDocumentales {
 public record Rectificar(long revision,List<String> asociaciones,String fundamento){}
 public record Movimiento(long secuencia,long revision,String autor,Instant fecha,String fundamento,List<String> anteriores,List<String> vigentes){}
 public record Vista(long revision,String estado,String actor,List<String> asociaciones,List<Movimiento> historial){}
 private record Contexto(UUID comunicacion){}
 private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio catalog;
 private final DocumentosGestion casos;private final DocumentosRequerimiento reqs;private final Clock clock;
 public AsociacionesDocumentales(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio catalog,DocumentosGestion casos,DocumentosRequerimiento reqs,Clock clock){this.db=db;this.core=core;this.catalog=catalog;this.casos=casos;this.reqs=reqs;this.clock=clock;}
 private static String id(UUID u){if(u==null)throw new FalloTecnico(400);return u.toString();}
 private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
 private static String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(EntregaCompleta.lector().writeValueAsBytes(v)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
 private Contexto contexto(Operador a,String tipo,UUID recurso,UUID doc){
  if(!catalog.individual())throw new FalloTecnico(409,"IDENTIDAD_INDIVIDUAL_REQUERIDA");core.bloquearAcceso(a);
  if(!AsociacionesVigentes.esquema(db))throw new FalloTecnico(409,"ESQUEMA_ASOCIACION_NO_DISPONIBLE");
  if(tipo.equals("CASO")){
   core.autorizarConsultaCaso(a,recurso);var d=casos.documento(a,recurso,doc);
   if(!d.restringible())throw new FalloTecnico(404);
   if(db.sql.queryForObject("SELECT COUNT(*) FROM document_content_policy WHERE document_id=? AND owner_case=?",Long.class,id(doc),id(recurso))!=1)throw new FalloTecnico(409,"DOCUMENTO_REQUIERE_ACREDITACION");
   return new Contexto(UUID.fromString(db.sql.queryForObject("SELECT communication_id FROM core_case WHERE id=?",String.class,id(recurso))));
  }
  if(!tipo.equals("REQ"))throw new FalloTecnico(404);var c=core.u2().contextoDocumentoReq(a,recurso,false);var d=reqs.documento(a,recurso,doc);if(!d.restringible())throw new FalloTecnico(404);return new Contexto(c.comunicacion());
 }
 private Vista vista(Operador a,String tipo,UUID recurso,UUID doc){
  contexto(a,tipo,recurso,doc);var c=AsociacionesVigentes.control(db,tipo,recurso,doc);
  var history=db.sql.queryForList("SELECT * FROM document_association_history WHERE document_id=? AND owner_kind=? AND owner_resource=? ORDER BY seq DESC LIMIT 100",id(doc),tipo,id(recurso)).stream().map(r->new Movimiento(n(r,"seq"),n(r,"revision"),r.get("actor_id").toString(),Instant.ofEpochMilli(n(r,"at_ms")),r.get("reason_text").toString(),lista(r.get("previous_doc")),lista(r.get("current_doc")))).toList();
  return new Vista(c.isEmpty()?-1:n(c,"revision"),c.isEmpty()?"SIN_RECTIFICAR":c.get("state").toString(),a.id(),AsociacionesVigentes.contextos(db,tipo,recurso,doc),history);
 }
 private static List<String> lista(Object v){return List.of(EntregaCompleta.lector().readValue(v.toString(),String[].class));}
 public Vista ver(Operador a,String tipo,UUID recurso,UUID doc){return db.transaction(()->{var v=vista(a,tipo,recurso,doc);auditar(a,doc,"ASOCIACIONES_CONSULTADAS");return v;});}
 private void auditar(Operador a,UUID doc,String code){db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),id(doc),code,clock.millis());}
 private void validar(String tipo,UUID recurso,List<String> keys){for(String key:keys){
  if(tipo.equals("CASO")){if(key.equals("CASO"))continue;UUID act;try{act=UUID.fromString(key);if(!act.toString().equals(key))throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new FalloTecnico(400);}if(db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE id=? AND case_id=?",Long.class,id(act),id(recurso))!=1)throw new FalloTecnico(404);}
  else{if(key.equals("REQ"))continue;long seq;try{seq=Long.parseLong(key);if(seq<=0||!Long.toString(seq).equals(key))throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new FalloTecnico(400);}if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_action a JOIN institution_flow_history h ON h.seq=a.history_seq WHERE a.history_seq=? AND a.requirement_id=? AND h.resource_id=? AND h.resource_kind='REQ'",Long.class,seq,id(recurso),id(recurso))!=1)throw new FalloTecnico(404);}
 }}
 private void conservarVinculos(Operador a,String tipo,UUID recurso,UUID doc,List<String> keys){for(String key:keys){
  if(tipo.equals("CASO")){if(db.sql.queryForObject("SELECT COUNT(*) FROM management_document_link WHERE document_id=? AND case_id=? AND context_id=?",Long.class,id(doc),id(recurso),key)==0)db.sql.update("INSERT INTO management_document_link VALUES(?,?,?,?,?,?)",id(doc),id(recurso),key.equals("CASO")?null:key,key,a.id(),clock.millis());}
  else{Long seq=key.equals("REQ")?null:Long.valueOf(key);long context=seq==null?0:seq;if(db.sql.queryForObject("SELECT COUNT(*) FROM requirement_document_link WHERE document_id=? AND requirement_id=? AND context_key=?",Long.class,id(doc),id(recurso),context)==0)db.sql.update("INSERT INTO requirement_document_link VALUES(?,?,?,?,?,?)",id(doc),id(recurso),seq,context,a.id(),clock.millis());}
 }}
 public Vista rectificar(Operador a,UUID key,String tipo,UUID recurso,UUID doc,Rectificar r){
  if(r==null||r.revision()<-1||r.asociaciones()==null||r.asociaciones().size()>500||r.asociaciones().stream().anyMatch(Objects::isNull)||new HashSet<>(r.asociaciones()).size()!=r.asociaciones().size())throw new FalloTecnico(400);
  try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(r.fundamento(),2000,true);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}
  var keys=r.asociaciones().stream().sorted().toList();String digest=hash(List.of("ASOCIACION_DOCUMENTAL_1",doc,r));
  return db.transaction(()->{
   var c=contexto(a,tipo,recurso,doc);db.sql.queryForList("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",id(c.comunicacion()));contexto(a,tipo,recurso,doc);
   var prior=db.sql.queryForList("SELECT * FROM document_association_command WHERE id=?",id(key));
   if(!prior.isEmpty()){var p=prior.getFirst();if(!a.id().equals(p.get("actor_id"))||!id(doc).equals(p.get("document_id"))||!tipo.equals(p.get("owner_kind"))||!id(recurso).equals(p.get("owner_resource"))||!digest.equals(p.get("request_hash")))throw new FalloTecnico(409);return vista(a,tipo,recurso,doc);}
   var control=AsociacionesVigentes.control(db,tipo,recurso,doc);long rev=control.isEmpty()?-1:n(control,"revision");if(r.revision()!=rev)throw new FalloTecnico(409,"VERSION_OBSOLETA");validar(tipo,recurso,keys);var previous=AsociacionesVigentes.contextos(db,tipo,recurso,doc);if(new TreeSet<>(previous).equals(new TreeSet<>(keys)))throw new FalloTecnico(409,"ASOCIACION_SIN_CAMBIOS");
   String state=keys.isEmpty()?"RETIRADA":"ACTIVA";long next=Math.addExact(rev,1);
   if(control.isEmpty())db.sql.update("INSERT INTO document_association_control VALUES(?,?,?,?,?)",id(doc),tipo,id(recurso),next,state);else db.sql.update("UPDATE document_association_control SET revision=?,state=? WHERE document_id=? AND owner_kind=? AND owner_resource=?",next,state,id(doc),tipo,id(recurso));
   conservarVinculos(a,tipo,recurso,doc,keys);db.sql.update("DELETE FROM document_association_target WHERE document_id=? AND owner_kind=? AND owner_resource=?",id(doc),tipo,id(recurso));for(String ref:keys)db.sql.update("INSERT INTO document_association_target VALUES(?,?,?,?)",id(doc),tipo,id(recurso),ref);
   db.sql.update("INSERT INTO document_association_history(document_id,owner_kind,owner_resource,revision,actor_id,at_ms,reason_text,previous_doc,current_doc) VALUES(?,?,?,?,?,?,?,?,?)",id(doc),tipo,id(recurso),next,a.id(),clock.millis(),r.fundamento(),EntregaCompleta.lector().writeValueAsString(previous),EntregaCompleta.lector().writeValueAsString(keys));
   // Las publicaciones anteriores no reviven al restaurar una asociación ni cambian los bytes originales.
   if(tipo.equals("CASO")){
    db.sql.update("UPDATE document_content_policy SET revision=revision+1 WHERE document_id=? AND owner_case=?",id(doc),id(recurso));long p=db.sql.queryForObject("SELECT revision FROM document_content_policy WHERE document_id=?",Long.class,id(doc));
    db.sql.update("INSERT INTO document_content_history(document_id,case_id,revision,actor_id,at_ms,action_code,reason_text,snapshot_doc) VALUES(?,?,?,?,?,?,?,?)",id(doc),id(recurso),p,a.id(),clock.millis(),"ASOCIACION_RECTIFICADA",r.fundamento(),EntregaCompleta.lector().writeValueAsString(Map.of("version","1.0","revisionAsociacion",next,"asociaciones",keys)));
   }else{
    db.sql.update("UPDATE requirement_document_policy SET revision=revision+1 WHERE document_id=? AND requirement_id=?",id(doc),id(recurso));long p=db.sql.queryForObject("SELECT revision FROM requirement_document_policy WHERE document_id=?",Long.class,id(doc));
    db.sql.update("INSERT INTO institution_flow_history(communication_id,resource_id,resource_kind,revision,actor_id,at_ms,action_code,reason_text,snapshot_doc) VALUES(?,?,'REQ_DOCUMENTO',?,?,?,?,?,?)",id(c.comunicacion()),id(doc),p,a.id(),clock.millis(),"REQ_ASOCIACION_RECTIFICADA",r.fundamento(),EntregaCompleta.lector().writeValueAsString(Map.of("version","1.0","revisionAsociacion",next,"asociaciones",keys)));
   }
   db.sql.update("INSERT INTO document_association_command VALUES(?,?,?,?,?,?)",id(key),a.id(),id(doc),tipo,id(recurso),digest);auditar(a,doc,"ASOCIACION_RECTIFICADA");return vista(a,tipo,recurso,doc);
  });
 }
}
