package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;

/** Catálogo firmado de competencia, separado de área, complejidad y perfil del funcionario. */
public final class CatalogoMaterias {
 public record Materia(String id,String nombre,String competencia,Instant desde,Instant hasta,String estado){}
 public record SedeRegional(String id,Instant desde,Instant hasta,String estado){}
 private final BaseJdbc db;private final Clock clock;
 public CatalogoMaterias(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
 public List<Materia> listar(){return db.sql.queryForList("SELECT m.* FROM institution_matter m JOIN org_unit_validity u ON u.unit_code=m.functional_unit WHERE m.status='ACTIVA' AND m.valid_from<=? AND m.valid_until>? AND u.valid_from<=? AND (u.valid_until IS NULL OR u.valid_until>?) ORDER BY m.id",clock.millis(),clock.millis(),clock.millis(),clock.millis()).stream().map(r->new Materia(r.get("id").toString(),r.get("label").toString(),r.get("functional_unit").toString(),Instant.ofEpochMilli(((Number)r.get("valid_from")).longValue()),Instant.ofEpochMilli(((Number)r.get("valid_until")).longValue()),r.get("status").toString())).toList();}
 public Materia exigir(String id){return listar().stream().filter(m->m.id().equals(id)).findFirst().orElseThrow(()->new FalloTecnico(409,"MATERIA_NO_ACREDITADA"));}
 public List<SedeRegional> sedes(){return db.sql.queryForList("SELECT r.* FROM matter_regional_site r JOIN org_site_validity s ON s.site_id=r.site_id WHERE r.status='ACTIVA' AND r.valid_from<=? AND r.valid_until>? AND s.valid_from<=? AND (s.valid_until IS NULL OR s.valid_until>?) ORDER BY r.site_id",clock.millis(),clock.millis(),clock.millis(),clock.millis()).stream().map(r->new SedeRegional(r.get("site_id").toString(),Instant.ofEpochMilli(((Number)r.get("valid_from")).longValue()),Instant.ofEpochMilli(((Number)r.get("valid_until")).longValue()),r.get("status").toString())).toList();}
 public SedeRegional exigirSede(String id){return sedes().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new FalloTecnico(409,"SEDE_REGIONAL_NO_ACREDITADA"));}
}
