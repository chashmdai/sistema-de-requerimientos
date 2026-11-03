package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;
import java.util.*;

/** Catálogo acreditado por el canal firmado; no hay edición organizacional libre para TI. */
public final class CatalogoAreas {
    public record Area(String id,String unidad,String sede,String nombre,String jefatura,Instant desde,Instant hasta){}
    private final BaseJdbc db;private final Clock clock;
    public CatalogoAreas(BaseJdbc db,Clock clock){this.db=db;this.clock=clock;}
    public List<Area> listar(){return db.sql.queryForList("SELECT * FROM institution_area ar WHERE ar.valid_from<=? AND ar.valid_until>? AND EXISTS(SELECT 1 FROM org_unit_validity u WHERE u.unit_code=ar.unit_code AND u.valid_from<="+clock.millis()+" AND (u.valid_until IS NULL OR u.valid_until>"+clock.millis()+")) AND EXISTS(SELECT 1 FROM org_site_validity s WHERE s.site_id=ar.site_id AND s.valid_from<="+clock.millis()+" AND (s.valid_until IS NULL OR s.valid_until>"+clock.millis()+")) ORDER BY unit_code,site_id,id",clock.millis(),clock.millis()).stream().map(r->new Area(r.get("id").toString(),r.get("unit_code").toString(),r.get("site_id").toString(),r.get("label").toString(),r.get("head_mode").toString(),Instant.ofEpochMilli(((Number)r.get("valid_from")).longValue()),Instant.ofEpochMilli(((Number)r.get("valid_until")).longValue()))).toList();}
    public Area exigir(String id,String unidad,String sede){return listar().stream().filter(a->a.id().equals(id)&&a.unidad().equals(unidad)&&a.sede().equals(sede)).findFirst().orElseThrow(()->new FalloTecnico(409,"AREA_NO_ACREDITADA"));}
}
