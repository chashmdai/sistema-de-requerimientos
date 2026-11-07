package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;import org.springframework.jdbc.core.ConnectionCallback;

/** Barrera técnica independiente de cualquier permiso institucional. Solo se cachea la migración acreditada. */
public final class DisponibilidadDocumental {
 private DisponibilidadDocumental(){}
 private static final Set<BaseJdbc> migradas=Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
 public static boolean esquema(BaseJdbc db){
  if(migradas.contains(db))return true;
  boolean vigente=Boolean.TRUE.equals(db.sql.execute((ConnectionCallback<Boolean>)c->{String q=c.getMetaData().getIdentifierQuoteString().trim();String table=q+"flyway_schema_history"+q,version=q+"version"+q,success=q+"success"+q;try(var st=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE "+version+"='15.20' AND "+success+"=TRUE");var r=st.executeQuery()){r.next();return r.getLong(1)==1;}}));
  if(vigente)migradas.add(db);return vigente;
 }
 public static boolean retenido(BaseJdbc db,UUID doc){return esquema(db)&&db.sql.queryForObject("SELECT COUNT(*) FROM document_technical_hold h JOIN institution_document d ON d.sha256=h.source_sha256 WHERE d.id=? AND h.state='RETENIDO'",Long.class,doc.toString())>0;}
 public static void huella(BaseJdbc db,String sha){if(esquema(db)&&db.sql.queryForObject("SELECT COUNT(*) FROM document_technical_hold WHERE source_sha256=? AND state='RETENIDO'",Long.class,sha)>0)throw new FalloTecnico(423,"DOCUMENTO_AISLADO_TECNICAMENTE");}
 public static void contenido(BaseJdbc db,UUID doc){if(retenido(db,doc))throw new FalloTecnico(423,"DOCUMENTO_AISLADO_TECNICAMENTE");}
}
