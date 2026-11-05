package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.identidad.CatalogoAreas;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.*;
import java.time.*;import java.util.*;

/** Misma separación explícita para consultas SQL y decisiones por recurso. Sin herencia unidad → área. */
final class PoliticaAreas {
 private final BaseJdbc db;private final Clock clock;
 PoliticaAreas(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
 boolean tieneAreas(String unidad,String sede){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_area WHERE unit_code=? AND site_id=?",Long.class,unidad,sede)>0;}
 void exigirAreaParaAlta(String area,String unidad,String sede){if(area==null&&tieneAreas(unidad,sede))throw new FalloTecnico(409,"AREA_REQUERIDA");}
 static String literal(String v){if(v==null)return "NULL";if(!v.matches("[A-Za-z0-9_.-]{1,64}"))throw new FalloTecnico(403);return "'"+v+"'";}
 String area(Map<String,Object> row){if(row.containsKey("area_id"))return (String)row.get("area_id");String kind=row.containsKey("responsible_id")?"case":"destination";var list=db.sql.queryForList("SELECT area_id FROM "+kind+"_area WHERE "+kind+"_id=?",row.get("id"));return list.isEmpty()?null:list.getFirst().get("area_id").toString();}
 boolean vigente(String ar,String unit,String site){try{new CatalogoAreas(db,clock).exigir(ar,unit,site);return true;}catch(FalloTecnico e){if(e.status==409)return false;throw e;}}
 boolean jefe(Operador a,Map<String,Object> row){String ar=area(row),u=row.get("unit_code").toString(),s=row.get("site_id").toString();return (ar==null||vigente(ar,u,s))&&a.jefeEn(u,s,ar);}
 boolean profesional(Operador a,Map<String,Object> row){String ar=area(row),u=row.get("unit_code").toString(),s=row.get("site_id").toString();return (ar==null||vigente(ar,u,s))&&a.profesionalEn(u,s,ar);}
 String persona(String actor,String area){var people=db.sql.queryForList("SELECT DISTINCT i.beneficiary_person FROM access_instruction i JOIN instruction_area x ON x.instruction_id=i.id JOIN approved_assignment p ON p.instruction_id=i.id WHERE i.beneficiary_id=? AND x.area_id=? AND i.state IN ('EJECUTADA','REVOCADA') AND i.beneficiary_person IS NOT NULL",actor,area);if(people.size()!=1)throw new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA");return people.getFirst().get("beneficiary_person").toString();}
 String scope(Operador a,String kind,String resource,String unit,String site,boolean head,String legacy){
  var terms=new ArrayList<String>();for(var x:a.accesos())if("AREA".equals(x.alcance())&&x.area()!=null&&(head?x.jefe():x.rol().equals("PROFESIONAL")))terms.add("(ar.id="+literal(x.area())+" AND "+unit+"="+literal(x.unidad())+" AND "+site+"="+literal(x.sede())+")");
  String areaTerms=String.join(" OR ",terms.isEmpty()?List.of("1=0"):terms);
  return "((NOT EXISTS(SELECT 1 FROM "+kind+"_area xa WHERE xa."+kind+"_id="+resource+") AND "+legacy+") OR EXISTS(SELECT 1 FROM "+kind+"_area xa JOIN institution_area ar ON ar.id=xa.area_id WHERE xa."+kind+"_id="+resource+" AND ar.valid_from<="+clock.millis()+" AND ar.valid_until>"+clock.millis()+" AND ("+areaTerms+")))";
 }
}
