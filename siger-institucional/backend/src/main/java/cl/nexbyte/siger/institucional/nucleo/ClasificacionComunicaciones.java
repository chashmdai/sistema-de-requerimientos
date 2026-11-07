package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

/** Toma de bandeja, independiente del destino SOL. El cerrojo COM serializa su historia inmutable. */
public final class ClasificacionComunicaciones {
 public record Cambio(long revision,String fundamento){@Override public String toString(){return "CambioToma[reservado]";}}
 public record Historia(long revision,String accion,String actor,Instant fecha){}
 public record Vista(String estado,long revision,long revisionCom,String titular,Instant tomadaEn,boolean propia,boolean puedeTomar,boolean puedeLiberar,boolean puedeRecuperar,List<Historia> historial){}
 private record Toma(long revision,String titular,Instant fecha,String estado){}
 private record DatosAviso(String version,long toma,String titular){}
 private static final String ACTION="AVISO_CLASIFICACION_PROGRAMADO";
 private static final String EVENTOS="'CLASIFICACION_TOMADA','CLASIFICACION_RECUPERADA','CLASIFICACION_LIBERADA','CLASIFICACION_FINALIZADA'";
 private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio cat;private final Clock clock;
 ClasificacionComunicaciones(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio cat,Clock clock){this.db=db;this.core=core;this.cat=cat;this.clock=clock;}
 private static String str(Map<String,Object> r,String k){return Objects.toString(r.get(k),null);}
 private Toma actual(UUID com){
  var rows=db.sql.queryForList("SELECT seq,actor_id,at_ms,code FROM core_event WHERE communication_id=? AND resource_id=? AND resource_kind='CLASIFICACION' AND code IN ("+EVENTOS+") ORDER BY seq DESC LIMIT 1",com.toString(),com.toString());
  if(rows.isEmpty())return new Toma(0,null,null,"SIN_TOMA");var r=rows.getFirst();String code=str(r,"code");return new Toma(((Number)r.get("seq")).longValue(),str(r,"actor_id"),Instant.ofEpochMilli(((Number)r.get("at_ms")).longValue()),Set.of("CLASIFICACION_TOMADA","CLASIFICACION_RECUPERADA").contains(code)?"TOMADA":code.equals("CLASIFICACION_LIBERADA")?"LIBERADA":"FINALIZADA");
 }
 private void permitir(Operador a,UUID com){
  if(!a.distribuidor()&&a.accesos().stream().noneMatch(x->x.rol().equals("COORDINADOR_REGIONAL")))throw new FalloTecnico(404);
  var rows=db.sql.queryForList("SELECT canal FROM communication_registry WHERE comunicacion_id=?",com.toString());if(rows.size()!=1)throw new FalloTecnico(404);
  var source=db.sql.queryForList("SELECT origin_code,site_id FROM communication_origin WHERE communication_id=?",com.toString());String sede=null;
  if(!source.isEmpty()&&"REGISTRO_FUNCIONARIO".equals(source.getFirst().get("origin_code")))sede=str(source.getFirst(),"site_id");
  else if(source.isEmpty()&&"REGISTRO_FUNCIONARIO".equals(rows.getFirst().get("canal")))throw new FalloTecnico(404);
  if(core.u2().regional(sede)){
   final String local=sede;if(a.accesos().stream().noneMatch(x->x.rol().equals("COORDINADOR_REGIONAL")&&Objects.equals(x.sede(),local)))throw new FalloTecnico(404);
  }else if(!a.distribuidor())throw new FalloTecnico(404);
 }
 private boolean titularHabilitado(UUID com,Toma toma){try{var a=cat.operador(toma.titular());permitir(a,com);return true;}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
 private boolean pendiente(UUID com){return db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry r LEFT JOIN core_communication c ON c.id=r.comunicacion_id WHERE r.comunicacion_id=? AND (c.state IS NULL OR c.state='ABIERTA') AND NOT EXISTS(SELECT 1 FROM core_destination d WHERE d.communication_id=r.comunicacion_id) AND NOT EXISTS(SELECT 1 FROM core_requirement q WHERE q.communication_id=r.comunicacion_id) AND NOT EXISTS(SELECT 1 FROM core_link l WHERE l.child_id=r.comunicacion_id)",Long.class,com.toString())==1;}
 private void guard(Operador a,UUID com){core.u2().guard(a);permitir(a,com);}
 private void revision(Toma t,Cambio r){if(r==null||r.revision()<0)throw new FalloTecnico(400);if(t.revision()!=r.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");}
 private static void fundamento(String s){try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(s,1000,true);}catch(IllegalArgumentException invalid){throw new FalloTecnico(400);}}
 private Toma registrar(Operador a,UUID com,String accion,String motivo,Toma previa){core.u2().historia(com,com,"CLASIFICACION",previa.revision(),a,accion,motivo,Map.of("version","1.0","tomaAnterior",previa.revision(),"titularAnterior",Objects.toString(previa.titular(),"")));return actual(com);}
 public Vista consultar(Operador a,UUID com){return db.transaction(()->{guard(a,com);var v=metadatos(a,com,true);new PoliticaMaterias(db,clock).auditar(a,com.toString(),"CLASIFICACION_CONSULTADA");return v;});}
 Vista metadatosSupervision(UUID com){return metadatos(null,com,false);}
 private Vista metadatos(Operador a,UUID com,boolean operar){
  var t=actual(com);boolean p=pendiente(com),propia=operar&&"TOMADA".equals(t.estado())&&a.id().equals(t.titular());
  long rev=!operar||!p?0:db.sql.queryForList("SELECT revision FROM core_communication WHERE id=?",com.toString()).stream().mapToLong(r->((Number)r.get("revision")).longValue()).findFirst().orElse(0);
  var h=db.sql.queryForList("SELECT seq,actor_id,at_ms,code FROM core_event WHERE communication_id=? AND resource_kind='CLASIFICACION' AND code IN ("+EVENTOS+") ORDER BY seq DESC LIMIT 30",com.toString()).stream().map(r->new Historia(((Number)r.get("seq")).longValue(),str(r,"code"),str(r,"actor_id"),Instant.ofEpochMilli(((Number)r.get("at_ms")).longValue()))).toList();
  return new Vista(t.estado(),t.revision(),rev,t.titular(),t.fecha(),propia,operar&&p&&!"TOMADA".equals(t.estado()),operar&&p&&propia,operar&&p&&"TOMADA".equals(t.estado())&&!propia&&!titularHabilitado(com,t),h);
 }
 public Resultado cambiar(Operador a,UUID key,UUID com,String accion,Cambio r){if(!Set.of("TOMAR","LIBERAR","RECUPERAR").contains(accion)||r==null)throw new FalloTecnico(400);return core.execute(key,a,"CLASIFICACION_"+accion+"_1",List.of(com,r),()->{
  guard(a,com);core.lock(com);var t=actual(com);revision(t,r);if(!pendiente(com))throw new FalloTecnico(409,"COM_YA_ENCAMINADA");String evento;
  if(accion.equals("TOMAR")){if("TOMADA".equals(t.estado()))throw new FalloTecnico(409,"COM_TOMADA_POR_OTRA_CUENTA");evento="CLASIFICACION_TOMADA";}
  else{fundamento(r.fundamento());if(!"TOMADA".equals(t.estado()))throw new FalloTecnico(409);if(accion.equals("LIBERAR")){if(!a.id().equals(t.titular()))throw new FalloTecnico(404);evento="CLASIFICACION_LIBERADA";}else{if(a.id().equals(t.titular())||titularHabilitado(com,t))throw new FalloTecnico(409,"TITULAR_TOMA_VIGENTE");evento="CLASIFICACION_RECUPERADA";}}
  var next=registrar(a,com,evento,accion.equals("TOMAR")?"Toma explícita para clasificar":r.fundamento(),t);core.u2().bumpCom(com);
  if(!accion.equals("LIBERAR"))core.avisosNucleo().guardar(a,com,com,"CLASIFICACION",next.revision(),ACTION,"AVISO_CLASIFICACION","COM_TOMADA",new DatosAviso("1.0",next.revision(),a.id()));
  return new Resultado(com,next.revision());
 });}
 /** Compatibilidad: la distribución sin toma previa sigue siendo atómica; nunca pasa sobre otra toma. */
 void exigirDisponible(Operador a,UUID com){var t=actual(com);if("TOMADA".equals(t.estado())&&!a.id().equals(t.titular()))throw new FalloTecnico(409,"COM_TOMADA_POR_OTRA_CUENTA");}
 void finalizar(Operador a,UUID com,String motivo){var t=actual(com);exigirDisponible(a,com);if("TOMADA".equals(t.estado()))registrar(a,com,"CLASIFICACION_FINALIZADA",motivo,t);}
 private DatosAviso fuente(UUID com,String key){
  var ref=core.avisosNucleo().registro(com,key,ACTION,"AVISO_CLASIFICACION");try{var p=AutoridadesInstitucionales.lectorPrivado().readValue(str(ref.historia(),"snapshot_doc"),DatosAviso.class);if(!"1.0".equals(p.version())||p.toma()<1||p.titular()==null||!"COM_TOMADA".equals(ref.tipo())||!com.toString().equals(ref.historia().get("communication_id"))||!p.titular().equals(ref.historia().get("actor_id")))throw new IllegalArgumentException();var e=db.sql.queryForList("SELECT actor_id,code FROM core_event WHERE seq=? AND communication_id=? AND resource_id=? AND resource_kind='CLASIFICACION'",p.toma(),com.toString(),com.toString());if(e.size()!=1||!p.titular().equals(e.getFirst().get("actor_id"))||!Set.of("CLASIFICACION_TOMADA","CLASIFICACION_RECUPERADA").contains(e.getFirst().get("code")))throw new IllegalArgumentException();return p;}catch(RuntimeException invalid){if(invalid instanceof org.springframework.dao.DataAccessException)throw invalid;throw new FalloTecnico(404);}
 }
 public boolean vigenteAviso(UUID com,String key){try{var p=fuente(com,key);var t=actual(com);return "TOMADA".equals(t.estado())&&t.revision()==p.toma()&&p.titular().equals(t.titular())&&pendiente(com);}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
 private boolean actuante(Operador a,UUID com,DatosAviso p){try{permitir(a,com);return a.id().equals(p.titular());}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
 public boolean permiteAviso(Operador a,UUID com,String key){try{var p=fuente(com,key);return (actuante(a,com,p)||core.supervisionClasificacion().permite(a,com))&&vigenteAviso(com,key);}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
 public AvisosNucleo.Vista consultarAviso(Operador a,UUID com,String key){return db.transaction(()->{core.u2().guard(a);if(!permiteAviso(a,com,key))throw new FalloTecnico(404);var ref=core.avisosNucleo().registro(com,key,ACTION,"AVISO_CLASIFICACION");var r=db.sql.queryForMap("SELECT folio_prefix,folio_width,numero FROM communication_registry WHERE comunicacion_id=?",com.toString());String folio=r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+r.get("folio_width")+"d",((Number)r.get("numero")).longValue());new PoliticaMaterias(db,clock).auditar(a,com.toString(),"AVISO_CLASIFICACION_CONSULTADO");return new AvisosNucleo.Vista("COM_TOMADA",Instant.ofEpochMilli(((Number)ref.historia().get("at_ms")).longValue()),folio,null,null,actuante(a,com,fuente(com,key))?"comunicacion/"+com:"supervision-clasificacion/"+com,true);});}
}
