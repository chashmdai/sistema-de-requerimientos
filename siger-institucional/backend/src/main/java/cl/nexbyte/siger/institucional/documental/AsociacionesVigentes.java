package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;
import org.springframework.jdbc.core.ConnectionCallback;

/** La proyección solo existe tras una rectificación explícita; los vínculos anteriores no se reescriben. */
public final class AsociacionesVigentes {
 private AsociacionesVigentes(){}
 private static final Set<BaseJdbc> migradas=Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
 public static boolean esquema(BaseJdbc db){
  if(migradas.contains(db))return true;
  boolean ok=Boolean.TRUE.equals(db.sql.execute((ConnectionCallback<Boolean>)c->{String q=c.getMetaData().getIdentifierQuoteString().trim();try(var st=c.prepareStatement("SELECT COUNT(*) FROM "+q+"flyway_schema_history"+q+" WHERE "+q+"version"+q+"='15.21' AND "+q+"success"+q+"=TRUE");var r=st.executeQuery()){r.next();return r.getLong(1)==1;}}));
  if(ok)migradas.add(db);return ok;
 }
 public static Map<String,Object> control(BaseJdbc db,String tipo,UUID recurso,UUID doc){if(!esquema(db))return Map.of();var r=db.sql.queryForList("SELECT * FROM document_association_control WHERE document_id=? AND owner_kind=? AND owner_resource=?",doc.toString(),tipo,recurso.toString());return r.isEmpty()?Map.of():r.getFirst();}
 public static List<String> contextos(BaseJdbc db,String tipo,UUID recurso,UUID doc){
  if(!control(db,tipo,recurso,doc).isEmpty())return db.sql.queryForList("SELECT context_key FROM document_association_target WHERE document_id=? AND owner_kind=? AND owner_resource=? ORDER BY context_key",String.class,doc.toString(),tipo,recurso.toString());
  if(tipo.equals("CASO"))return db.sql.queryForList("SELECT context_id FROM management_document_link WHERE document_id=? AND case_id=? ORDER BY context_id",String.class,doc.toString(),recurso.toString());
  return db.sql.queryForList("SELECT context_key FROM requirement_document_link WHERE document_id=? AND requirement_id=? ORDER BY context_key",Long.class,doc.toString(),recurso.toString()).stream().map(k->k==0?"REQ":Long.toString(k)).toList();
 }
 public static boolean retirada(BaseJdbc db,String tipo,UUID recurso,UUID doc){return "RETIRADA".equals(control(db,tipo,recurso,doc).get("state"));}
 /** Retirar el documento de su propietario suspende también referencias y concesiones dependientes. */
 public static boolean retiradaCaso(BaseJdbc db,UUID caso,UUID doc){
  if(!esquema(db))return false;
  if(caso!=null&&retirada(db,"CASO",caso,doc))return true;
  return db.sql.queryForObject("SELECT COUNT(*) FROM document_association_control a JOIN document_content_policy p ON p.document_id=a.document_id AND p.owner_case=a.owner_resource WHERE a.document_id=? AND a.owner_kind='CASO' AND a.state='RETIRADA'",Long.class,doc.toString())>0;
 }
 public static void contenido(BaseJdbc db,String tipo,UUID recurso,UUID doc){if(tipo.equals("CASO")?retiradaCaso(db,recurso,doc):retirada(db,tipo,recurso,doc))throw new FalloTecnico(404);}
 /** Guardia de I/O para referencias privadas obtenidas antes de una retirada, incluidas vistas cacheadas. */
 public static void contenido(BaseJdbc db,UUID doc){if(esquema(db)&&db.sql.queryForObject("SELECT COUNT(*) FROM document_association_control a WHERE a.document_id=? AND a.state='RETIRADA' AND ((a.owner_kind='CASO' AND EXISTS(SELECT 1 FROM document_content_policy p WHERE p.document_id=a.document_id AND p.owner_case=a.owner_resource)) OR (a.owner_kind='REQ' AND EXISTS(SELECT 1 FROM requirement_document_policy p WHERE p.document_id=a.document_id AND p.requirement_id=a.owner_resource)))",Long.class,doc.toString())>0)throw new FalloTecnico(404);}
 public static void nuevoVinculo(BaseJdbc db,String tipo,UUID recurso,UUID doc,String key){if(!control(db,tipo,recurso,doc).isEmpty()&&!contextos(db,tipo,recurso,doc).contains(key))throw new FalloTecnico(409,"ASOCIACION_REQUIERE_RECTIFICACION");}
}
