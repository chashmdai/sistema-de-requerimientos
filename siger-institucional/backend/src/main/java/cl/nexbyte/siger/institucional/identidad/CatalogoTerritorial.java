package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Una propuesta técnica de cobertura no autoriza competencia: la acredita el registro firmado. */
public final class CatalogoTerritorial {
    public record Acreditacion(UUID cobertura,String region,String sede,Instant desde,Instant hasta,String propuestaSha256,String estado,String referencia,String sha256){}
    public record Decision(String version,String modo,String condicionDomicilio,String regionDeclarada,String sedeInicial,Long autoridadRevision,UUID cobertura,String propuestaSha256,String respaldoSha256,String fundamento,String referencias){}
    public record Orientacion(String estado,String sedeAcreditada){}
    private final BaseJdbc db;private final AutoridadesInstitucionales autoridad;private final JsonMapper json;private final Clock clock;
    public CatalogoTerritorial(BaseJdbc db,AutoridadesInstitucionales autoridad,JsonMapper json,Clock clock){this.db=db;this.autoridad=autoridad;this.json=json;this.clock=clock;}
    static void validar(List<Acreditacion> values,List<CatalogoMaterias.SedeRegional> sedes,Instant limite){
        if(values==null||values.size()>64)throw new FalloTecnico(400);var seen=new HashSet<UUID>();
        for(var c:values){
            if(c==null||c.cobertura()==null||!seen.add(c.cobertura()))throw new FalloTecnico(400);
            AutoridadesInstitucionales.codigo(c.region());AutoridadesInstitucionales.codigo(c.sede());AutoridadesInstitucionales.texto(c.referencia(),180);
            AutoridadesInstitucionales.intervalo(c.desde(),c.hasta(),limite);
            if(!Set.of("ACTIVA","SUSPENDIDA").contains(c.estado())||!hash(c.sha256())||!hash(c.propuestaSha256())
                ||sedes.stream().noneMatch(s->s.id().equals(c.sede())&&!s.desde().isAfter(c.desde())&&!s.hasta().isBefore(c.hasta())))throw new FalloTecnico(400);
        }
        for(var c:values)for(var d:values)if(c!=d&&c.estado().equals("ACTIVA")&&d.estado().equals("ACTIVA")&&c.region().equals(d.region())&&c.desde().isBefore(d.hasta())&&d.desde().isBefore(c.hasta()))throw new FalloTecnico(409,"COBERTURAS_ACREDITADAS_SOLAPADAS");
    }
    private static boolean hash(String value){return value!=null&&value.matches("[a-f0-9]{64}");}
    private Map<String,Object> propuesta(UUID id){var rows=db.sql.queryForList("SELECT * FROM org_coverage WHERE id=?",id.toString());if(rows.size()!=1)throw new FalloTecnico(409,"COBERTURA_TERRITORIAL_NO_ACREDITADA");return rows.getFirst();}
    private static long num(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private String huella(Map<String,Object> p){return AutoridadesInstitucionales.sha(json.writeValueAsBytes(Arrays.asList("SIGER_COBERTURA_PROPUESTA_1",p.get("id"),p.get("region_id"),p.get("site_id"),num(p,"valid_from"),p.get("valid_until")==null?null:num(p,"valid_until"))));}
    /** Solo herramienta del canal de acreditación; no concede facultades ni expone un endpoint. */
    public String huellaPropuesta(UUID id){return huella(propuesta(id));}
    void verificarPropuestas(List<Acreditacion> values){for(var c:values)verificar(c);}
    private void verificar(Acreditacion c){
        var p=propuesta(c.cobertura());
        if(!c.region().equals(p.get("region_id"))||!c.sede().equals(p.get("site_id"))||!c.propuestaSha256().equals(huella(p))
            ||c.desde().toEpochMilli()<num(p,"valid_from")||p.get("valid_until")!=null&&c.hasta().toEpochMilli()>num(p,"valid_until"))throw new FalloTecnico(409,"COBERTURA_TERRITORIAL_NO_ACREDITADA");
    }
    private AutoridadesInstitucionales.ContextoTerritorial contexto(){if(autoridad==null)throw new FalloTecnico(409,"COBERTURA_TERRITORIAL_NO_ACREDITADA");return autoridad.contextoTerritorial();}
    private Acreditacion vigente(AutoridadesInstitucionales.ContextoTerritorial ctx,String region){
        var values=ctx.coberturas().stream().filter(c->c.region().equals(region)&&c.estado().equals("ACTIVA")&&!c.desde().isAfter(clock.instant())&&c.hasta().isAfter(clock.instant())).toList();
        if(values.size()!=1)throw new FalloTecnico(409,"COBERTURA_TERRITORIAL_NO_ACREDITADA");
        var c=values.getFirst();verificar(c);new CatalogoMaterias(db,clock).exigirSede(c.sede());return c;
    }
    public Orientacion orientar(boolean regional,String sede,String region){
        if(!regional)return new Orientacion("COMPETENCIA_CENTRAL",null);
        if(region==null)return new Orientacion("REQUIERE_DECISION_FUNDADA",null);
        try{var c=vigente(contexto(),region);return new Orientacion(c.sede().equals(sede)?"COBERTURA_ACREDITADA":"SEDE_NO_CORRESPONDE",c.sede());}
        catch(FalloTecnico e){if(Set.of(403,409).contains(e.status))return new Orientacion("COBERTURA_NO_ACREDITADA",null);throw e;}
    }
    private static void texto(String t){AutoridadesInstitucionales.texto(t==null?null:t.replace("\r"," ").replace("\n"," ").replace("\t"," "),2000);}
    public Decision resolver(boolean regional,String sede,String region,String condicion,String fundamento,String referencias,boolean contratoExplicito){
        if(condicion==null||!Set.of("UTILIZABLE","AUSENTE","CONTRADICTORIO","EXTRANJERO","INUTILIZABLE").contains(condicion)
            ||condicion.equals("UTILIZABLE")&&region==null||condicion.equals("AUSENTE")&&region!=null)throw new FalloTecnico(400,"CONDICION_DOMICILIO_INVALIDA");
        if(region!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM org_region WHERE id=?",Long.class,region)!=1)throw new FalloTecnico(400);
        if(!regional)return new Decision("1.0","COMPETENCIA_CENTRAL",condicion,region,sede,null,null,null,null,null,null);
        if(!condicion.equals("UTILIZABLE")){
            texto(fundamento);
            if(contratoExplicito)texto(referencias);
            return new Decision("1.0","DECISION_FUNDADA",condicion,region,sede,null,null,null,null,fundamento,referencias);
        }
        var ctx=contexto();var c=vigente(ctx,region);if(!c.sede().equals(sede))throw new FalloTecnico(409,"SEDE_NO_CORRESPONDE_A_DOMICILIO");
        return new Decision("1.0","COBERTURA_ACREDITADA",condicion,region,sede,ctx.revision(),c.cobertura(),c.propuestaSha256(),c.sha256(),null,c.referencia());
    }
}
