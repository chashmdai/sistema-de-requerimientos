package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import java.time.Clock;
import java.util.*;

/** Terminar una intervención aceptada requiere decisión de destino y después el cierre
 * ordinario de su Caso. Ninguna de estas solicitudes reasigna, revoca equipo o cierra. */
public final class TerminoColaboracion {
    public record Solicitar(long revisionRequerimiento,long revisionColaboracion,String fundamento) {
        @Override public String toString(){return "SolicitarTermino[protegido]";}
    }
    public record Decidir(long revision,String decision,String fundamento) {
        @Override public String toString(){return "DecidirTermino[protegido]";}
    }
    private final BaseJdbc db;
    private final NucleoInstitucional core;
    private final NucleoU2 flow;
    private final CatalogoLaboratorio catalogo;
    private final Clock clock;
    TerminoColaboracion(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio catalogo,Clock clock){
        this.db=db;this.core=core;this.flow=core.u2();this.catalogo=catalogo;this.clock=clock;
    }
    private static String id(UUID x){if(x==null)throw new FalloTecnico(400);return x.toString();}
    private static UUID uuid(Object x){return UUID.fromString(x.toString());}
    private static long revision(Map<String,Object> x){return ((Number)x.get("revision")).longValue();}
    private static void version(Map<String,Object> x,long v){if(v<0||revision(x)!=v)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private static void texto(String x){
        try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(x==null?null:x.replace('\n',' ').replace('\r',' '),2000,true);}
        catch(IllegalArgumentException e){throw new FalloTecnico(400);}
    }
    private Map<String,Object> uno(String sql,Object...args){
        var rows=db.sql.queryForList(sql,args);if(rows.size()!=1)throw new FalloTecnico(404);return rows.getFirst();
    }
    private Map<String,Object> colaboracion(UUID id){return uno("SELECT * FROM institution_collaboration WHERE id=?",id(id));}
    private Map<String,Object> solicitud(UUID id){return uno("SELECT * FROM institution_collaboration_end WHERE id=?",id(id));}
    private List<Map<String,Object>> normal(List<Map<String,Object>> rows){
        return rows.stream().map(r->{Map<String,Object> out=new LinkedHashMap<>();r.forEach((k,v)->out.put(k.toLowerCase(Locale.ROOT),v));return out;}).toList();
    }
    private void registrar(Operador a,Map<String,Object> q,UUID request,long rev,String code,String reason){
        db.sql.update("UPDATE core_requirement SET revision=revision+1 WHERE id=?",q.get("id"));
        flow.historia(uuid(q.get("communication_id")),request,"TERMINO_COLAB",rev,a,code,reason,Map.of("requerimiento",q.get("id")));
        flow.bumpCom(uuid(q.get("communication_id")));
    }
    public boolean pendienteCaso(UUID caso){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_collaboration_end WHERE case_id=? AND state='PENDIENTE'",Long.class,id(caso))>0;}
    public boolean pendienteReq(UUID req){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_collaboration_end e JOIN institution_collaboration c ON c.id=e.collaboration_id WHERE c.requirement_id=? AND e.state='PENDIENTE'",Long.class,id(req))>0;}

    public Resultado solicitar(Operador a,UUID key,UUID collaboration,Solicitar r){
        if(r==null)throw new FalloTecnico(400);
        return core.execute(key,a,"COLABORACION_SOLICITAR_TERMINO",List.of(collaboration,r),()->{
            flow.guard(a);var p=colaboracion(collaboration);var q=flow.req(uuid(p.get("requirement_id")));
            core.lock(uuid(q.get("communication_id")));p=colaboracion(collaboration);q=flow.req(uuid(p.get("requirement_id")));
            flow.jefatura(a,q);flow.abierto(q);version(q,r.revisionRequerimiento());version(p,r.revisionColaboracion());texto(r.fundamento());
            if(!"ACEPTADA".equals(p.get("state"))||p.get("case_id")==null)throw new FalloTecnico(409,"COLABORACION_NO_ACEPTADA");
            UUID caso=uuid(p.get("case_id"));var c=core.caseRow(caso);
            if("CERRADO".equals(c.get("state")))throw new FalloTecnico(409,"CASO_CERRADO");
            if(pendienteCaso(caso))throw new FalloTecnico(409,"TERMINO_COLABORACION_PENDIENTE");
            UUID request=UUID.randomUUID();
            db.sql.update("INSERT INTO institution_collaboration_end(id,collaboration_id,case_id,origin_destination,requirement_revision,requested_by,created_ms,reason_text,state,revision) VALUES(?,?,?,?,?,?,?,?,'PENDIENTE',0)",id(request),id(collaboration),id(caso),q.get("principal_destination"),revision(q),a.id(),clock.millis(),r.fundamento());
            registrar(a,q,request,0,"COLABORACION_TERMINO_SOLICITADO",r.fundamento());
            return new Resultado(request,0);
        });
    }
    public Resultado decidir(Operador a,UUID key,UUID request,Decidir r){
        if(r==null||!Set.of("ACEPTADA","RECHAZADA","RETIRADA").contains(Objects.toString(r.decision(),"")))throw new FalloTecnico(400);
        return core.execute(key,a,"COLABORACION_DECIDIR_TERMINO",List.of(request,r),()->{
            flow.guard(a);var e=solicitud(request);var p=colaboracion(uuid(e.get("collaboration_id")));var q=flow.req(uuid(p.get("requirement_id")));
            core.lock(uuid(q.get("communication_id")));e=solicitud(request);q=flow.req(uuid(p.get("requirement_id")));var c=core.caseRow(uuid(e.get("case_id")));
            if("RETIRADA".equals(r.decision()))flow.jefatura(a,q);else core.head(a,c);
            version(e,r.revision());texto(r.fundamento());if(!"PENDIENTE".equals(e.get("state")))throw new FalloTecnico(409,"SOLICITUD_YA_DECIDIDA");
            if(!"RETIRADA".equals(r.decision())){
                if("ACEPTADA".equals(r.decision())){
                    flow.abierto(q);if("CERRADO".equals(c.get("state")))throw new FalloTecnico(409,"CASO_CERRADO");
                    if(!Objects.equals(e.get("origin_destination"),q.get("principal_destination")))throw new FalloTecnico(409,"SOLICITUD_OBSOLETA");
                    flow.jefatura(catalogo.operador(e.get("requested_by").toString()),q);
                }
            }
            db.sql.update("UPDATE institution_collaboration_end SET state=?,revision=revision+1,decided_by=?,decided_ms=?,decision_text=? WHERE id=?",r.decision(),a.id(),clock.millis(),r.fundamento(),id(request));
            registrar(a,q,request,r.revision()+1,"COLABORACION_TERMINO_"+r.decision(),r.fundamento());
            return new Resultado(request,r.revision()+1);
        });
    }

    private Map<String,Object> vista(Operador a,Map<String,Object> e,Map<String,Object> q,Map<String,Object> c){
        var out=normal(List.of(e)).getFirst();
        out.put("puedeRetirar",a.jefeEn(q.get("unit_code").toString(),q.get("site_id").toString(),(String)q.get("area_id")));
        out.put("puedeDecidir",a.jefeEn(c.get("unit_code").toString(),c.get("site_id").toString(),new PoliticaAreas(db,clock).area(c)));
        out.put("estadoCaso",c.get("state"));
        out.put("historial",normal(db.sql.queryForList("SELECT revision,actor_id,at_ms,action_code,reason_text FROM institution_flow_history WHERE resource_id=? ORDER BY seq",e.get("id"))));
        return out;
    }
    public List<Map<String,Object>> delReq(Operador a,UUID req){return db.transaction(()->{
        flow.guard(a);var q=flow.req(req);flow.jefatura(a,q);
        return db.sql.queryForList("SELECT e.* FROM institution_collaboration_end e JOIN institution_collaboration p ON p.id=e.collaboration_id WHERE p.requirement_id=? ORDER BY e.created_ms,e.id",id(req)).stream().map(e->vista(a,e,q,core.caseRow(uuid(e.get("case_id"))))).toList();
    });}
    public List<Map<String,Object>> delCaso(Operador a,UUID caso){return db.transaction(()->{
        flow.guard(a);core.autorizarCaso(a,caso);var c=core.caseRow(caso);var q=flow.req(uuid(c.get("requirement_id")));
        return db.sql.queryForList("SELECT * FROM institution_collaboration_end WHERE case_id=? ORDER BY created_ms,id",id(caso)).stream().map(e->vista(a,e,q,c)).toList();
    });}
    public List<Map<String,Object>> bandeja(Operador a){return db.transaction(()->{
        flow.guard(a);
        String scope=flow.scope(a,"v",true).replace("v.unit_code","d.unit_code").replace("v.site_id","d.site_id").replace("v.area_id","ar.area_id");
        return db.sql.queryForList("SELECT e.* FROM institution_collaboration_end e JOIN core_case c ON c.id=e.case_id JOIN core_destination d ON d.id=c.destination_id LEFT JOIN case_area ar ON ar.case_id=c.id WHERE "+scope+" ORDER BY e.created_ms DESC,e.id LIMIT 100").stream().map(e->{var c=core.caseRow(uuid(e.get("case_id")));return vista(a,e,flow.req(uuid(c.get("requirement_id"))),c);}).toList();
    });}
    public void autorizarAviso(Operador a,UUID request,boolean pendiente){
        boolean allowed=db.transaction(()->{
            try{
                flow.guard(a);var e=solicitud(request);var p=colaboracion(uuid(e.get("collaboration_id")));var q=flow.req(uuid(p.get("requirement_id")));
                if(pendiente){if(!"PENDIENTE".equals(e.get("state")))return false;core.head(a,core.caseRow(uuid(e.get("case_id"))));}
                else try{flow.jefatura(a,q);}catch(FalloTecnico denied){if(denied.status!=404)throw denied;core.head(a,core.caseRow(uuid(e.get("case_id"))));}
                return true;
            }catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}
        });
        if(!allowed)throw new FalloTecnico(404);
    }
}
