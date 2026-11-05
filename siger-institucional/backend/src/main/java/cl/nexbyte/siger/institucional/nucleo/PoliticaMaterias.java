package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.*;
import cl.nexbyte.siger.institucional.identidad.CatalogoMaterias;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;

/** Consulta ordinaria explícita de Caso; no concede COM, documentos o antecedentes sin pertinencia acreditada. */
final class PoliticaMaterias {
 private final BaseJdbc db;private final Clock clock;
 PoliticaMaterias(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
 String materia(Object caso){var r=db.sql.queryForList("SELECT matter_id FROM case_matter WHERE case_id=?",caso.toString());return r.isEmpty()?null:r.getFirst().get("matter_id").toString();}
 String scope(Operador a){
  var terms=new ArrayList<String>();for(var x:a.accesos())if(x.rol().equals("DIRECTOR")&&x.alcance().equals("MATERIA")&&x.materia()!=null)terms.add("(m.id="+PoliticaAreas.literal(x.materia())+" AND m.functional_unit="+PoliticaAreas.literal(x.unidad())+" AND r.site_id="+PoliticaAreas.literal(x.sede())+")");
  return "EXISTS(SELECT 1 FROM case_matter cm JOIN institution_matter m ON m.id=cm.matter_id JOIN matter_regional_site r ON r.site_id=d.site_id WHERE cm.case_id=c.id AND cm.ordinary_consultation=TRUE AND m.status='ACTIVA' AND m.valid_from<="+clock.millis()+" AND m.valid_until>"+clock.millis()+" AND r.status='ACTIVA' AND r.valid_from<="+clock.millis()+" AND r.valid_until>"+clock.millis()+" AND ("+String.join(" OR ",terms.isEmpty()?List.of("1=0"):terms)+"))";
 }
 boolean puedeLeer(Operador a,Object caso){return db.sql.queryForObject("SELECT COUNT(*) FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE c.id=? AND "+scope(a),Long.class,caso.toString())>0;}
 boolean coordinacion(Operador a,String sede){return a.accesos().stream().anyMatch(x->x.rol().equals("COORDINADOR_REGIONAL")&&x.alcance().equals("SEDE")&&Objects.equals(x.sede(),sede));}
 void sedeRegional(String sede){new CatalogoMaterias(db,clock).exigirSede(sede);}
 void auditar(Operador a,String resource,String action){db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),resource,action,clock.millis());}
}
