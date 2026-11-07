package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;
import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales.SupervisorBandeja;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;

/** Consulta nominal de metadatos de toma; nunca autoriza el contenido de una Comunicación. */
public final class SupervisionClasificacion {
 public record Item(UUID comunicacion,String folio,String titular,Instant tomadaEn,long revision,String sede){}
 public record Bandeja(List<Item> items,long total,long cursor){}
 private record Filtro(String sql,List<Object> argumentos){}
 private final BaseJdbc db;private final NucleoInstitucional core;private final AutoridadesInstitucionales autoridad;private final Clock clock;
 SupervisionClasificacion(BaseJdbc db,NucleoInstitucional core,AutoridadesInstitucionales autoridad,Clock clock){this.db=db;this.core=core;this.autoridad=autoridad;this.clock=clock;}
 private List<SupervisorBandeja> facultades(Operador a){
  if(autoridad==null)throw new FalloTecnico(403,"SUPERVISION_NO_ACREDITADA");
  return autoridad.supervisionesClasificacion(a.id()).stream().filter(f->f.sede()==null||db.sql.queryForObject("SELECT COUNT(*) FROM org_site_validity WHERE site_id=? AND valid_from<=? AND (valid_until IS NULL OR valid_until>?)",Long.class,f.sede(),clock.millis(),clock.millis())==1).toList();
 }
 private Filtro filtro(List<SupervisorBandeja> facultades){
  boolean web=facultades.stream().anyMatch(f->f.bandeja().equals("WEB_CENTRAL"));var sedes=facultades.stream().filter(f->f.bandeja().equals("MANUAL_SEDE")).map(SupervisorBandeja::sede).distinct().sorted().toList();
  var args=new ArrayList<Object>();args.add(web);args.addAll(sedes);
  String manual=sedes.isEmpty()?"1=0":"r.canal='REGISTRO_FUNCIONARIO' AND o.origin_code='REGISTRO_FUNCIONARIO' AND o.site_id IN ("+String.join(",",Collections.nCopies(sedes.size(),"?"))+")";
  return new Filtro("((? AND (r.source='FORMULARIO_PUBLICO_DDN' AND (o.origin_code='WEB' OR o.origin_code IS NULL))) OR ("+manual+"))",args);
 }
 private static final String FROM=" FROM communication_registry r LEFT JOIN communication_origin o ON o.communication_id=r.comunicacion_id ";
 public boolean permite(Operador a,UUID com){try{
  var f=filtro(facultades(a));var args=new ArrayList<>(f.argumentos());args.add(com.toString());return db.sql.queryForObject("SELECT COUNT(*)"+FROM+"WHERE "+f.sql()+" AND r.comunicacion_id=?",Long.class,args.toArray())==1;
 }catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
 public Bandeja listar(Operador a,long despues,int limite){return db.transaction(()->{
  core.u2().guard(a);if(despues<0||limite<1||limite>50)throw new FalloTecnico(400);var caps=facultades(a);if(caps.isEmpty())throw new FalloTecnico(403,"SUPERVISION_NO_ACREDITADA");var f=filtro(caps);
  String query="SELECT e.seq,e.communication_id,e.actor_id,e.at_ms,r.folio_prefix,r.folio_width,r.numero,o.site_id"+FROM+"JOIN core_communication c ON c.id=r.comunicacion_id JOIN core_event e ON e.communication_id=c.id AND e.resource_id=c.id WHERE "+f.sql()+" AND c.state='ABIERTA' AND e.resource_kind='CLASIFICACION' AND e.code IN ('CLASIFICACION_TOMADA','CLASIFICACION_RECUPERADA') AND NOT EXISTS(SELECT 1 FROM core_event later WHERE later.communication_id=c.id AND later.resource_id=c.id AND later.resource_kind='CLASIFICACION' AND later.seq>e.seq AND later.code IN ('CLASIFICACION_TOMADA','CLASIFICACION_RECUPERADA','CLASIFICACION_LIBERADA','CLASIFICACION_FINALIZADA')) AND NOT EXISTS(SELECT 1 FROM core_destination d WHERE d.communication_id=c.id) AND NOT EXISTS(SELECT 1 FROM core_requirement q WHERE q.communication_id=c.id) AND NOT EXISTS(SELECT 1 FROM core_link l WHERE l.child_id=c.id)";
  long total=db.sql.queryForObject("SELECT COUNT(*) FROM ("+query+") visible_claims",Long.class,f.argumentos().toArray());var args=new ArrayList<>(f.argumentos());args.add(despues);args.add(limite);
  var items=db.sql.queryForList(query+" AND e.seq>? ORDER BY e.seq LIMIT ?",args.toArray()).stream().map(r->new Item(UUID.fromString(r.get("communication_id").toString()),r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+r.get("folio_width")+"d",((Number)r.get("numero")).longValue()),r.get("actor_id").toString(),Instant.ofEpochMilli(((Number)r.get("at_ms")).longValue()),((Number)r.get("seq")).longValue(),Objects.toString(r.get("site_id"),null))).toList();
  new PoliticaMaterias(db,clock).auditar(a,"BANDEJA_CLASIFICACION","CLASIF_SUPERVISION_LISTADA");return new Bandeja(items,total,items.isEmpty()?despues:items.getLast().revision());
 });}
 public ClasificacionComunicaciones.Vista consultar(Operador a,UUID com){return db.transaction(()->{
  core.u2().guard(a);if(!permite(a,com))throw new FalloTecnico(404);var vista=core.clasificacion().metadatosSupervision(com);if(vista.revision()==0)throw new FalloTecnico(404);new PoliticaMaterias(db,clock).auditar(a,com.toString(),"CLASIF_SUPERVISION_CONSULTADA");return vista;
 });}
}
