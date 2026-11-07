package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.formulario.EntregaCompleta;
import java.security.*;import java.time.*;import java.util.*;

/** Metadatos y sucesión de originales, nunca edición de sus bytes ni una firma/visación externa. */
public final class VersionesDocumentales {
 public record Metadatos(long revision,String titulo,String tipoDeclarado,String fundamento){}
 public record Finalizar(long revision,String referenciaExterna,String fundamento){}
 public record Sucesor(long revision,long revisionAnterior,UUID anterior,String fundamento){}
 public record Movimiento(long secuencia,long revision,String autor,Instant fecha,String tipo,String fundamento,Map<String,Object> datos){}
 public record Vista(boolean acreditada,long revision,Long numero,String titulo,String tipoDeclarado,String fase,
                     String referenciaExterna,Instant finalDeclaradoEn,String declaradoPor,boolean editable,boolean finalizable,boolean permiteSucesion,
                     UUID anterior,UUID siguiente,List<Movimiento> historial){}
 private record Contexto(UUID com,boolean propietario,boolean abierto,String proteccion){}
 private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio catalog;
 private final DocumentosGestion casos;private final DocumentosRequerimiento reqs;private final Clock clock;
 public VersionesDocumentales(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio catalog,DocumentosGestion casos,DocumentosRequerimiento reqs,Clock clock){this.db=db;this.core=core;this.catalog=catalog;this.casos=casos;this.reqs=reqs;this.clock=clock;}
 private static String id(UUID v){if(v==null)throw new FalloTecnico(400);return v.toString();}
 private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
 private static String s(Map<String,Object> r,String k){return Objects.toString(r.get(k),null);}
 private static String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(EntregaCompleta.lector().writeValueAsBytes(v)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
 private static void texto(String v,int max){try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(v,max,true);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}}
 private Contexto contexto(Operador a,String tipo,UUID recurso,UUID doc){
  if(!catalog.individual())throw new FalloTecnico(409,"IDENTIDAD_INDIVIDUAL_REQUERIDA");core.bloquearAcceso(a);
  if(tipo.equals("CASO")){core.autorizarConsultaCaso(a,recurso);var r=casos.referencia(a,recurso,doc,"CONTENIDO");var d=casos.documento(a,recurso,doc);return new Contexto(r.comunicacion(),d.restringible(),"EN_GESTION".equals(db.sql.queryForObject("SELECT state FROM core_case WHERE id=?",String.class,id(recurso))),d.proteccion());}
  if(!tipo.equals("REQ"))throw new FalloTecnico(404);var r=reqs.referencia(a,recurso,doc,"CONTENIDO");var c=core.u2().contextoDocumentoReq(a,recurso,false);var d=reqs.documento(a,recurso,doc);return new Contexto(r.comunicacion(),d.restringible(),c.editable(),d.proteccion());
 }
 private Map<String,Object> meta(UUID doc){var rows=db.sql.queryForList("SELECT * FROM management_document_metadata WHERE document_id=?",id(doc));return rows.isEmpty()?Map.of():rows.getFirst();}
 private static void revision(Map<String,Object> row,long expected){if((row.isEmpty()?-1:n(row,"revision"))!=expected)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
 private void propio(Contexto c,Map<String,Object> row,String tipo,UUID recurso){if(!c.propietario()||!row.isEmpty()&&(!tipo.equals(row.get("owner_kind"))||!id(recurso).equals(row.get("owner_resource"))))throw new FalloTecnico(404);if("HISTORICO_SIN_ACREDITAR".equals(c.proteccion()))throw new FalloTecnico(409,"DOCUMENTO_REQUIERE_ACREDITACION");}
 private void lock(Contexto c){db.sql.queryForList("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",id(c.com));}
 private boolean anterior(Operador a,UUID key,String tipo,UUID recurso,UUID doc,String digest){var rows=db.sql.queryForList("SELECT * FROM management_document_metadata_command WHERE id=?",id(key));if(rows.isEmpty())return false;var r=rows.getFirst();if(!a.id().equals(r.get("actor_id"))||!tipo.equals(r.get("owner_kind"))||!id(recurso).equals(r.get("owner_resource"))||!id(doc).equals(r.get("document_id"))||!digest.equals(r.get("request_hash")))throw new FalloTecnico(409);return true;}
 private void comando(Operador a,UUID key,String tipo,UUID recurso,UUID doc,String digest){db.sql.update("INSERT INTO management_document_metadata_command VALUES(?,?,?,?,?,?)",id(key),a.id(),tipo,id(recurso),id(doc),digest);}
 private void historia(Operador a,UUID doc,String code,String reason){var m=meta(doc);db.sql.update("INSERT INTO management_document_metadata_history(document_id,revision,actor_id,at_ms,event_code,reason_text,snapshot_doc) VALUES(?,?,?,?,?,?,?)",id(doc),n(m,"revision"),a.id(),clock.millis(),code,reason,EntregaCompleta.lector().writeValueAsString(normal(m)));}
 private Set<UUID> visibles(Operador a,String tipo,UUID recurso){return tipo.equals("CASO")?casos.listar(a,recurso).stream().filter(DocumentosGestion.Documento::consultable).map(DocumentosGestion.Documento::id).collect(java.util.stream.Collectors.toSet()):reqs.listar(a,recurso).documentos().stream().filter(DocumentosRequerimiento.Documento::consultable).map(DocumentosRequerimiento.Documento::id).collect(java.util.stream.Collectors.toSet());}
 public Vista ver(Operador a,String tipo,UUID recurso,UUID doc){return db.transaction(()->vista(a,tipo,recurso,doc));}
 private Vista vista(Operador a,String tipo,UUID recurso,UUID doc){
  var c=contexto(a,tipo,recurso,doc);var row=meta(doc);boolean owner=c.propietario()&&!"HISTORICO_SIN_ACREDITAR".equals(c.proteccion())&&(row.isEmpty()||tipo.equals(row.get("owner_kind"))&&id(recurso).equals(row.get("owner_resource")));
  db.sql.update("INSERT INTO institution_document_access(document_id,communication_id,actor_id,at_ms,representation) VALUES(?,?,?,?,?)",id(doc),id(c.com),a.id(),clock.millis(),"VERSIONES_CONSULTADAS");
  if(row.isEmpty())return new Vista(false,-1,null,null,null,"SIN_ACREDITAR",null,null,null,owner,false,false,null,null,List.of());
  var visible=visibles(a,tipo,recurso);var prev=db.sql.queryForList("SELECT predecessor_id FROM management_document_successor WHERE document_id=?",String.class,id(doc));var next=db.sql.queryForList("SELECT document_id FROM management_document_successor WHERE predecessor_id=?",String.class,id(doc));
  UUID p=prev.isEmpty()?null:UUID.fromString(prev.getFirst()),q=next.isEmpty()?null:UUID.fromString(next.getFirst());
  List<Movimiento> history=owner?db.sql.queryForList("SELECT * FROM management_document_metadata_history WHERE document_id=? ORDER BY seq DESC LIMIT 100",id(doc)).stream().map(r->new Movimiento(n(r,"seq"),n(r,"revision"),s(r,"actor_id"),Instant.ofEpochMilli(n(r,"at_ms")),s(r,"event_code"),s(r,"reason_text"),normal(EntregaCompleta.lector().readValue(s(r,"snapshot_doc"),Map.class)))).toList():List.of();
  return new Vista(true,n(row,"revision"),n(row,"version_number"),s(row,"title_text"),s(row,"type_text"),s(row,"phase_code"),s(row,"external_reference"),row.get("finalized_ms")==null?null:Instant.ofEpochMilli(n(row,"finalized_ms")),s(row,"finalized_by"),owner,owner&&c.abierto()&&"BORRADOR".equals(row.get("phase_code")),owner&&c.abierto()&&"BORRADOR".equals(row.get("phase_code"))&&prev.isEmpty()&&next.isEmpty(),p!=null&&visible.contains(p)?p:null,q!=null&&visible.contains(q)?q:null,history);
 }
 private static Map<String,Object> normal(Map<?,?> row){var out=new LinkedHashMap<String,Object>();row.forEach((k,v)->out.put(k.toString().toLowerCase(Locale.ROOT),v));return out;}
 public Vista registrar(Operador a,UUID key,String tipo,UUID recurso,UUID doc,Metadatos r){
  if(r==null)throw new FalloTecnico(400);texto(r.titulo(),180);texto(r.tipoDeclarado(),120);texto(r.fundamento(),2000);String digest=hash(List.of("DOC_METADATOS_1",doc,r));
  return db.transaction(()->{var c=contexto(a,tipo,recurso,doc);lock(c);c=contexto(a,tipo,recurso,doc);var m=meta(doc);propio(c,m,tipo,recurso);if(anterior(a,key,tipo,recurso,doc,digest))return vista(a,tipo,recurso,doc);revision(m,r.revision());
   if(m.isEmpty())db.sql.update("INSERT INTO management_document_metadata(document_id,owner_kind,owner_resource,revision,version_number,title_text,type_text,phase_code) VALUES(?,?,?,0,1,?,?,'BORRADOR')",id(doc),tipo,id(recurso),r.titulo(),r.tipoDeclarado());
   else db.sql.update("UPDATE management_document_metadata SET revision=revision+1,title_text=?,type_text=? WHERE document_id=?",r.titulo(),r.tipoDeclarado(),id(doc));
   historia(a,doc,m.isEmpty()?"METADATOS_DECLARADOS":"METADATOS_RECTIFICADOS",r.fundamento());comando(a,key,tipo,recurso,doc,digest);return vista(a,tipo,recurso,doc);
  });
 }
 public Vista finalizar(Operador a,UUID key,String tipo,UUID recurso,UUID doc,Finalizar r){
  if(r==null)throw new FalloTecnico(400);texto(r.fundamento(),2000);if(r.referenciaExterna()!=null)texto(r.referenciaExterna(),500);String digest=hash(List.of("DOC_FINAL_1",doc,r));
  return db.transaction(()->{var c=contexto(a,tipo,recurso,doc);lock(c);c=contexto(a,tipo,recurso,doc);var m=meta(doc);propio(c,m,tipo,recurso);if(anterior(a,key,tipo,recurso,doc,digest))return vista(a,tipo,recurso,doc);revision(m,r.revision());if(!c.abierto()||!"BORRADOR".equals(m.get("phase_code")))throw new FalloTecnico(409,"DOCUMENTO_NO_FINALIZABLE");
   db.sql.update("UPDATE management_document_metadata SET revision=revision+1,phase_code='FINAL',external_reference=?,finalized_ms=?,finalized_by=? WHERE document_id=?",r.referenciaExterna(),clock.millis(),a.id(),id(doc));historia(a,doc,"FINAL_DECLARADO",r.fundamento());comando(a,key,tipo,recurso,doc,digest);return vista(a,tipo,recurso,doc);
  });
 }
 public Vista suceder(Operador a,UUID key,String tipo,UUID recurso,UUID doc,Sucesor r){
  if(r==null||r.anterior()==null||r.anterior().equals(doc))throw new FalloTecnico(400);texto(r.fundamento(),2000);String digest=hash(List.of("DOC_SUCESOR_1",doc,r));
  return db.transaction(()->{var c=contexto(a,tipo,recurso,doc);lock(c);c=contexto(a,tipo,recurso,doc);var p=contexto(a,tipo,recurso,r.anterior());var m=meta(doc);var old=meta(r.anterior());propio(c,m,tipo,recurso);propio(p,old,tipo,recurso);if(anterior(a,key,tipo,recurso,doc,digest))return vista(a,tipo,recurso,doc);
   revision(m,r.revision());revision(old,r.revisionAnterior());if(!c.abierto()||!p.abierto()||m.isEmpty()||old.isEmpty()||!"BORRADOR".equals(m.get("phase_code")))throw new FalloTecnico(409,"SUCESION_NO_DISPONIBLE");
   if(!c.proteccion().equals(p.proteccion()))throw new FalloTecnico(409,"PROTECCION_DOCUMENTAL_DISTINTA");
   if(db.sql.queryForObject("SELECT COUNT(*) FROM management_document_successor WHERE document_id=? OR predecessor_id=? OR predecessor_id=?",Long.class,id(doc),id(doc),id(r.anterior()))!=0)throw new FalloTecnico(409,"CADENA_DOCUMENTAL_NO_LINEAL");
   long number;try{number=Math.addExact(n(old,"version_number"),1);}catch(ArithmeticException e){throw new FalloTecnico(409);}
   db.sql.update("INSERT INTO management_document_successor VALUES(?,?,?,?,?)",id(doc),id(r.anterior()),a.id(),clock.millis(),r.fundamento());db.sql.update("UPDATE management_document_metadata SET revision=revision+1,version_number=? WHERE document_id=?",number,id(doc));db.sql.update("UPDATE management_document_metadata SET revision=revision+1 WHERE document_id=?",id(r.anterior()));
   historia(a,doc,"SUCESORA_DECLARADA",r.fundamento());historia(a,r.anterior(),"SUCESOR_REGISTRADO",r.fundamento());comando(a,key,tipo,recurso,doc,digest);return vista(a,tipo,recurso,doc);
  });
 }
}
