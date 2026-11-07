package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;

/** Medición explícita y durable de registro. Consultar o medir no crea una actuación ni cambia responsables. */
public final class PlazosRegistro {
 public record Solicitud(String fuente,String huellaFuente,String politica,String versionPolitica,String huellaPolitica,String huellaCalendario){}
 public record Regla(String id,String version,String modo,String referencia,String calendario,String versionCalendario,String huella,String huellaCalendario){}
 public record Medicion(UUID id,String politica,String versionPolitica,Instant vencimiento,long transcurridoMs,String resultado,String autor,Instant fecha,long autoridad,boolean actual){}
 public record Entrada(String id,String objetivo,String origen,Instant efectiva,Instant registrada,Instant capturada,String registradoPor,String anterior,boolean vigente,Long duracionCorridaMs,String huella,String estado,Regla regla,List<Medicion> mediciones){}
 public record Pagina(String actor,List<Entrada> items,long total,int pagina,int tamanio){}
 private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio catalog;private final AutoridadesInstitucionales autoridad;private final Clock clock;
 public PlazosRegistro(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio catalog,AutoridadesInstitucionales autoridad,Clock clock){this.db=db;this.core=core;this.catalog=catalog;this.autoridad=autoridad;this.clock=clock;}
 private static String id(UUID u){if(u==null)throw new FalloTecnico(400);return u.toString();}
 private static String s(Map<String,Object> r,String k){return Objects.toString(r.get(k),null);}
 private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
 private void guard(Operador a,String tipo,UUID recurso){
  if(!catalog.individual())throw new FalloTecnico(409,"IDENTIDAD_INDIVIDUAL_REQUERIDA");core.bloquearAcceso(a);
  if(!VersionEsquema.aplicada(db,"15.22"))throw new FalloTecnico(409,"ESQUEMA_TEMPORAL_REQUERIDO");
  switch(tipo){case "COM"->core.autorizarLectura(a,recurso);case "CASO"->core.autorizarConsultaCaso(a,recurso);case "REQ"->core.u2().contextoDocumentoReq(a,recurso,false);default->throw new FalloTecnico(404);}
 }
 private String condicion(String tipo){return tipo.equals("REQ")?"((f.resource_kind='REQ' AND f.resource_id=?) OR (f.resource_kind='COM' AND f.communication_id=(SELECT communication_id FROM core_requirement WHERE id=?)))":"(f.resource_kind=? AND f.resource_id=?)";}
 private ArrayList<Object> argumentos(String tipo,UUID recurso){return new ArrayList<>(tipo.equals("REQ")?List.of(id(recurso),id(recurso)):List.of(tipo,id(recurso)));}
 private Map<String,Object> fuente(String tipo,UUID recurso,String ref){var args=argumentos(tipo,recurso);args.add(ref);var rows=db.sql.queryForList("SELECT f.* FROM temporal_registration_source f WHERE "+condicion(tipo)+" AND f.id=?",args.toArray());if(rows.size()!=1)throw new FalloTecnico(404);return rows.getFirst();}
 private Contexto configuracion(){if(autoridad==null)throw new FalloTecnico(409,"CONFIGURACION_TEMPORAL_NO_ACREDITADA");return autoridad.contextoPlazos();}
 private Politica aplicable(Map<String,Object> r,Contexto c){
  long at=n(r,"effective_ms");var found=c.politicas().stream().filter(p->p.estado().equals("ACTIVA")&&p.objetivo().equals(r.get("topic_code"))&&Objects.equals(p.prioridad(),r.get("priority_code"))&&p.desde().toEpochMilli()<=at&&p.hasta().toEpochMilli()>at&&(p.alcance().equals("INSTITUCION")||Objects.equals(p.unidad(),r.get("unit_code"))&&Objects.equals(p.sede(),r.get("site_id"))&&(p.area()==null||Objects.equals(p.area(),r.get("area_id"))))).toList();
  if(found.size()>1)throw new FalloTecnico(409,"POLITICAS_TEMPORALES_SOLAPADAS");return found.isEmpty()?null:found.getFirst();
 }
 private Calendario calendario(Politica p,Contexto c){return p.calendario()==null?null:c.calendarios().stream().filter(v->v.id().equals(p.calendario())&&v.version().equals(p.versionCalendario())).findFirst().orElseThrow(()->new FalloTecnico(409,"CALENDARIO_NO_ACREDITADO"));}
 private void verificarVersion(String tabla,String id,String version,String sha){var r=db.sql.queryForList("SELECT snapshot_sha256 FROM "+tabla+" WHERE id=? AND version_code=?",id,version);if(r.size()!=1||!sha.equals(r.getFirst().get("snapshot_sha256")))throw new FalloTecnico(409,"VERSION_TEMPORAL_ALTERADA");}
 private Regla regla(Politica p,Contexto c){if(p==null)return null;String sha=CatalogoPlazos.huellaPolitica(p);verificarVersion("temporal_policy_version",p.id(),p.version(),sha);var calendar=calendario(p,c);String cs=calendar==null?null:CatalogoPlazos.huellaCalendario(calendar);if(calendar!=null)verificarVersion("temporal_calendar_version",calendar.id(),calendar.version(),cs);return new Regla(p.id(),p.version(),p.modo(),p.referencia(),p.calendario(),p.versionCalendario(),sha,cs);}
 private boolean vigente(Map<String,Object> f){return db.sql.queryForObject("SELECT COUNT(*) FROM temporal_registration_source WHERE previous_id=?",Long.class,s(f,"id"))==0;}
 private String sello(Map<String,Object> r){return CatalogoPlazos.huella(Arrays.asList("SIGER_MEDICION_REGISTRO_1",r.get("id"),r.get("source_id"),r.get("source_sha256"),r.get("policy_id"),r.get("policy_version"),r.get("policy_sha256"),r.get("calendar_sha256"),r.get("authority_revision"),r.get("deadline_ms"),r.get("elapsed_ms"),r.get("result_code"),r.get("measured_by"),r.get("at_ms")));}
 private Medicion medicion(Map<String,Object> r,Map<String,Object> f,Regla regla,boolean vigente){
  if(!sello(r).equals(r.get("measurement_sha256"))||!Objects.equals(r.get("source_sha256"),f.get("source_sha256")))throw new FalloTecnico(409,"MEDICION_TEMPORAL_ALTERADA");
  boolean actual=vigente&&regla!=null&&regla.id().equals(r.get("policy_id"))&&regla.version().equals(r.get("policy_version"))&&regla.huella().equals(r.get("policy_sha256"))&&Objects.equals(regla.huellaCalendario(),r.get("calendar_sha256"));
  return new Medicion(UUID.fromString(s(r,"id")),s(r,"policy_id"),s(r,"policy_version"),Instant.ofEpochMilli(n(r,"deadline_ms")),n(r,"elapsed_ms"),s(r,"result_code"),s(r,"measured_by"),Instant.ofEpochMilli(n(r,"at_ms")),n(r,"authority_revision"),actual);
 }
 private Entrada entrada(Map<String,Object> f,Contexto c){
  FuentesRegistro.verificar(f);var p=aplicable(f,c);var regla=regla(p,c);boolean current=vigente(f);Long corrida;try{corrida=Math.subtractExact(n(f,"registered_ms"),n(f,"effective_ms"));}catch(ArithmeticException e){corrida=null;}
  String estado=corrida==null?"FECHAS_FUERA_COMPUTO":corrida<0?"FECHAS_NO_COMPARABLES":regla==null?"SIN_POLITICA_APLICABLE":"MEDICION_DISPONIBLE";
  var history=db.sql.queryForList("SELECT * FROM temporal_measurement WHERE source_id=? ORDER BY at_ms DESC,id DESC LIMIT 20",s(f,"id")).stream().map(r->medicion(r,f,regla,current)).toList();
  return new Entrada(s(f,"id"),s(f,"topic_code"),s(f,"source_type"),Instant.ofEpochMilli(n(f,"effective_ms")),Instant.ofEpochMilli(n(f,"registered_ms")),Instant.ofEpochMilli(n(f,"captured_ms")),s(f,"registered_by"),s(f,"previous_id"),current,corrida,s(f,"source_sha256"),estado,regla,history);
 }
 private void auditar(Operador a,UUID recurso,String code){db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),id(recurso),code,clock.millis());}
 public Pagina listar(Operador a,String tipo,UUID recurso,int pagina,int tamanio){
  if(pagina<0||tamanio<1||tamanio>100)throw new FalloTecnico(400);return db.transaction(()->{guard(a,tipo,recurso);var args=argumentos(tipo,recurso);long total=db.sql.queryForObject("SELECT COUNT(*) FROM temporal_registration_source f WHERE "+condicion(tipo),Long.class,args.toArray());args.add(tamanio);args.add((long)pagina*tamanio);var rows=db.sql.queryForList("SELECT f.* FROM temporal_registration_source f WHERE "+condicion(tipo)+" ORDER BY f.registered_ms,f.captured_ms,f.id LIMIT ? OFFSET ?",args.toArray());var config=configuracion();var items=rows.stream().map(f->entrada(f,config)).toList();auditar(a,recurso,"PLAZOS_REGISTRO_CONSULTADOS");return new Pagina(a.id(),items,total,pagina,tamanio);});
 }
 public Medicion medir(Operador a,UUID key,String tipo,UUID recurso,Solicitud r){
  if(r==null||r.fuente()==null||r.huellaFuente()==null||r.politica()==null||r.versionPolitica()==null||r.huellaPolitica()==null)throw new FalloTecnico(400);String digest=CatalogoPlazos.huella(Arrays.asList("MEDIR_REGISTRO_1",tipo,recurso,r));
  return db.transaction(()->{
   guard(a,tipo,recurso);var f=fuente(tipo,recurso,r.fuente());FuentesRegistro.verificar(f);if(!r.huellaFuente().equals(f.get("source_sha256")))throw new FalloTecnico(409,"FUENTE_TEMPORAL_CAMBIADA");var c=configuracion();var p=aplicable(f,c);var regla=regla(p,c);
   var previous=db.sql.queryForList("SELECT * FROM temporal_measurement_command WHERE id=?",id(key));if(!previous.isEmpty()){var v=previous.getFirst();if(!a.id().equals(v.get("actor_id"))||!tipo.equals(v.get("resource_kind"))||!id(recurso).equals(v.get("resource_id"))||!r.fuente().equals(v.get("source_id"))||!digest.equals(v.get("request_hash")))throw new FalloTecnico(409);return medicion(db.sql.queryForMap("SELECT * FROM temporal_measurement WHERE id=?",v.get("measurement_id")),f,regla,vigente(f));}
   if(p==null)throw new FalloTecnico(409,"CONFIGURACION_TEMPORAL_NO_ACREDITADA");if(!regla.id().equals(r.politica())||!regla.version().equals(r.versionPolitica())||!regla.huella().equals(r.huellaPolitica())||!Objects.equals(regla.huellaCalendario(),r.huellaCalendario()))throw new FalloTecnico(409,"POLITICA_TEMPORAL_CAMBIADA");
   var existing=db.sql.queryForList("SELECT * FROM temporal_measurement WHERE source_id=? AND source_sha256=? AND policy_id=? AND policy_version=?",r.fuente(),r.huellaFuente(),p.id(),p.version());
   Map<String,Object> measurement;if(!existing.isEmpty())measurement=existing.getFirst();else{
    var computed=MotorPlazos.medir(Instant.ofEpochMilli(n(f,"effective_ms")),Instant.ofEpochMilli(n(f,"registered_ms")),p,calendario(p,c));measurement=new LinkedHashMap<>();measurement.put("id",UUID.randomUUID().toString());measurement.put("source_id",r.fuente());measurement.put("source_sha256",r.huellaFuente());measurement.put("policy_id",p.id());measurement.put("policy_version",p.version());measurement.put("policy_sha256",regla.huella());measurement.put("calendar_sha256",regla.huellaCalendario());measurement.put("authority_revision",c.autoridad());measurement.put("deadline_ms",computed.vencimiento().toEpochMilli());measurement.put("elapsed_ms",computed.transcurridoMs());measurement.put("result_code",computed.resultado());measurement.put("measured_by",a.id());measurement.put("at_ms",clock.millis());measurement.put("measurement_sha256",sello(measurement));
    db.sql.update("INSERT INTO temporal_measurement(id,source_id,source_sha256,policy_id,policy_version,policy_sha256,calendar_sha256,authority_revision,deadline_ms,elapsed_ms,result_code,measurement_sha256,measured_by,at_ms) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",measurement.get("id"),r.fuente(),r.huellaFuente(),p.id(),p.version(),regla.huella(),regla.huellaCalendario(),c.autoridad(),computed.vencimiento().toEpochMilli(),computed.transcurridoMs(),computed.resultado(),measurement.get("measurement_sha256"),a.id(),measurement.get("at_ms"));
   }
   guard(a,tipo,recurso);var finalConfig=configuracion();var finalPolicy=aplicable(f,finalConfig);if(finalPolicy==null||!regla.equals(regla(finalPolicy,finalConfig)))throw new FalloTecnico(409,"POLITICA_TEMPORAL_CAMBIADA");
   var result=medicion(measurement,f,regla,vigente(f));db.sql.update("INSERT INTO temporal_measurement_command VALUES(?,?,?,?,?,?,?)",id(key),a.id(),tipo,id(recurso),r.fuente(),digest,measurement.get("id"));auditar(a,recurso,"PLAZO_MEDICION_CONFIRMADA");return result;
  });
 }
 public Contexto catalogo(Operador a){return db.transaction(()->{core.bloquearAcceso(a);if(!catalog.individual()||!a.accesos().stream().anyMatch(x->x.rol().equals("TI")&&x.alcance().equals("INSTITUCION")))throw new FalloTecnico(403);var c=configuracion();db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,NULL,?,?)",a.id(),"CATALOGO_PLAZOS_CONSULTADO",clock.millis());return c;});}
}
