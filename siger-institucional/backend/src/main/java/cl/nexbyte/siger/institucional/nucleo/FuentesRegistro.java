package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos;
import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;

/** Hechos temporales anexados a la transacción de origen, sin backfill ni movimiento de fechas. */
public final class FuentesRegistro {
 private FuentesRegistro(){}
 public static void registrar(BaseJdbc db,String id,UUID comunicacion,String tipo,UUID recurso,String objetivo,String origen,String unidad,String sede,String area,long efectiva,long registrada,long capturada,String autor,String anterior){
  if(!VersionEsquema.aplicada(db,"15.22"))return;
  if(anterior!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM temporal_registration_source WHERE id=?",Long.class,anterior)==0)anterior=null;
  var values=new ArrayList<Object>(Arrays.asList(id,comunicacion.toString(),tipo,recurso.toString(),objetivo,origen,unidad,sede,area,null,efectiva,registrada,capturada,autor,anterior));String sha=huella(values);
  var old=db.sql.queryForList("SELECT source_sha256 FROM temporal_registration_source WHERE id=?",id);if(!old.isEmpty()){if(!sha.equals(old.getFirst().get("source_sha256")))throw new FalloTecnico(409,"FUENTE_TEMPORAL_CONFLICTIVA");return;}
  values.add(sha);db.sql.update("INSERT INTO temporal_registration_source VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",values.toArray());
 }
 private static String huella(List<Object> values){return CatalogoPlazos.huella(Arrays.asList("SIGER_FUENTE_REGISTRO_1",values));}
 public static void verificar(Map<String,Object> r){
  var values=new ArrayList<Object>();for(String key:List.of("id","communication_id","resource_kind","resource_id","topic_code","source_type","unit_code","site_id","area_id","priority_code","effective_ms","registered_ms","captured_ms","registered_by","previous_id"))values.add(r.get(key));
  if(!huella(values).equals(r.get("source_sha256")))throw new FalloTecnico(409,"FUENTE_TEMPORAL_ALTERADA");
 }
 public static void correccionManual(BaseJdbc db,UUID com,long revision,long efectiva,long capturada,String autor){
  if(!VersionEsquema.aplicada(db,"15.22"))return;
  var r=db.sql.queryForMap("SELECT registrada_ms FROM communication_registry WHERE comunicacion_id=?",com.toString());var origins=db.sql.queryForList("SELECT site_id FROM communication_origin WHERE communication_id=?",com.toString());
  var previous=db.sql.queryForList("SELECT id FROM temporal_registration_source WHERE communication_id=? AND resource_kind='COM' AND NOT EXISTS(SELECT 1 FROM temporal_registration_source child WHERE child.previous_id=temporal_registration_source.id)",String.class,com.toString());
  if(previous.size()>1)throw new FalloTecnico(409,"FUENTE_TEMPORAL_AMBIGUA");
  registrar(db,"COMR:"+com+":"+revision,com,"COM",com,"REGISTRO_ATENCION","RECTIFICACION_MANUAL",null,origins.isEmpty()?(String)null:(String)origins.getFirst().get("site_id"),null,efectiva,((Number)r.get("registrada_ms")).longValue(),capturada,autor,previous.isEmpty()?null:previous.getFirst());
 }
}
