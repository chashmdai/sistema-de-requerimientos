package cl.nexbyte.siger.institucional.robustez;

import java.util.*;
import org.springframework.jdbc.core.ConnectionCallback;

/** Solo cachea versiones aplicadas, nunca ausencia de esquema o decisiones de autorización. */
public final class VersionEsquema {
 private VersionEsquema(){}
 private static final Map<BaseJdbc,Set<String>> aplicadas=new WeakHashMap<>();
 public static boolean aplicada(BaseJdbc db,String version){
  if(version==null||!version.matches("[0-9]+(?:\\.[0-9]+)*"))throw new IllegalArgumentException();
  synchronized(aplicadas){if(aplicadas.getOrDefault(db,Set.of()).contains(version))return true;}
  boolean ok=Boolean.TRUE.equals(db.sql.execute((ConnectionCallback<Boolean>)c->{String q=c.getMetaData().getIdentifierQuoteString().trim();try(var st=c.prepareStatement("SELECT COUNT(*) FROM "+q+"flyway_schema_history"+q+" WHERE "+q+"version"+q+"=? AND "+q+"success"+q+"=TRUE")){st.setString(1,version);try(var r=st.executeQuery()){r.next();return r.getLong(1)==1;}}}));
  if(ok)synchronized(aplicadas){aplicadas.computeIfAbsent(db,k->new HashSet<>()).add(version);}
  return ok;
 }
}
