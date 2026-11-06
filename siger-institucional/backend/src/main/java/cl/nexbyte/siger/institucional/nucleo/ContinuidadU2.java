package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import java.time.Clock;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Decisiones de continuidad. La titularidad principal, la gestión de cada Caso y
 * la asignación profesional tienen transiciones diferentes y el mismo cerrojo COM. */
public final class ContinuidadU2 {
    public record Trasladar(long revision,NuevoDestino destino,String fundamento,String resumen){@Override public String toString(){return "Trasladar[protegido]";}}
    public record Derivar(long revision,NuevoDestino destino,String fundamento,String resumen,String objetivo){@Override public String toString(){return "Derivar[protegido]";}}
    public record Resolver(long revision,String decision,String responsable,String fundamento){@Override public String toString(){return "Resolver[protegido]";}}
    public record Reabrir(long revision,String fundamento,String antecedentes){@Override public String toString(){return "Reabrir[protegido]";}}
    public record DecidirReapertura(long revision,long revisionCaso,String decision,String fundamento){@Override public String toString(){return "DecidirReapertura[protegido]";}}
    public record Reactivar(long revision,long revisionRequerimiento,String fundamento){@Override public String toString(){return "Reactivar[protegido]";}}
    public record Acusar(UUID asignacion){ }
    private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio cat;private final Clock clock;private final NucleoU2 flow;private final PoliticaAreas areas;
    ContinuidadU2(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio cat,Clock clock,JsonMapper json){this.db=db;this.core=core;this.cat=cat;this.clock=clock;this.flow=core.u2();this.areas=new PoliticaAreas(db,clock);}
    private static String id(UUID v){if(v==null)throw new FalloTecnico(400);return v.toString();}
    private static UUID uuid(Object v){return UUID.fromString(v.toString());}
    private static String s(Map<String,Object> r,String k){return r.get(k)==null?null:r.get(k).toString();}
    private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private static void texto(String v){try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(v==null?null:v.replace('\n',' ').replace('\r',' ').replace('\t',' '),2000,true);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}}
    private static void version(Map<String,Object> r,long v){if(v<0||n(r,"revision")!=v)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private Map<String,Object> uno(String sql,Object...args){var rows=db.sql.queryForList(sql,args);if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
    private List<Map<String,Object>> rows(String sql,Object...args){return db.sql.queryForList(sql,args).stream().map(r->{var out=new LinkedHashMap<String,Object>();r.forEach((k,v)->out.put(k.toLowerCase(Locale.ROOT),v));return (Map<String,Object>)out;}).toList();}
    private List<Map<String,Object>> paquetes(List<Map<String,Object>> paquetes){for(var p:paquetes)p.put("historial",rows("SELECT revision,action_code,reason_text,actor_id,at_ms FROM institution_flow_history WHERE resource_id=? ORDER BY seq",p.get("id")));return paquetes;}
    private Map<String,Object> caso(Operador a,UUID c){core.autorizarCaso(a,c);if(!core.casoU2(c))throw new FalloTecnico(409,"HISTORICO_REQUIERE_CONCILIACION");return core.caseRow(c);}
    private void independiente(Operador a,String solicitante){if(core.personaU2(a.id()).equals(core.personaU2(solicitante)))throw new FalloTecnico(409,"DECISION_REQUIERE_PERSONA_INDEPENDIENTE");}
    private Operador vigente(String actor){try{return cat.operador(actor);}catch(FalloTecnico e){throw new FalloTecnico(409,"AUTORIZACION_ORIGEN_NO_VIGENTE");}}
    private void distinto(Map<String,Object> origin,NuevoDestino d){if(Objects.equals(origin.get("unit_code"),d.unidad())&&Objects.equals(origin.get("site_id"),d.sede())&&Objects.equals(origin.get("area_id"),d.area()))throw new FalloTecnico(409,"MISMO_AMBITO");}
    private String materia(UUID caso){var m=db.sql.queryForList("SELECT matter_id FROM case_matter WHERE case_id=?",id(caso));return m.isEmpty()?null:s(m.getFirst(),"matter_id");}
    private void reqRevision(UUID req){db.sql.update("UPDATE core_requirement SET revision=revision+1 WHERE id=?",id(req));}
    private void historia(UUID com,UUID resource,String kind,long revision,Operador a,String code,String reason,Object snapshot){flow.historia(com,resource,kind,revision,a,code,reason,snapshot);flow.bumpCom(com);}
    boolean pendientesReq(UUID req){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_transfer WHERE requirement_id=? AND state='PENDIENTE'",Long.class,id(req))>0||db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_derivation WHERE requirement_id=? AND state='PENDIENTE'",Long.class,id(req))>0||db.sql.queryForObject("SELECT COUNT(*) FROM core_reopen r JOIN institution_reopen i ON i.request_id=r.id JOIN core_case c ON c.id=r.case_id WHERE c.requirement_id=? AND r.state='PENDIENTE'",Long.class,id(req))>0;}
    boolean derivacionPendiente(UUID c){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_derivation WHERE origin_case_id=? AND state='PENDIENTE'",Long.class,id(c))>0;}

    public Resultado transferir(Operador a,UUID key,UUID req,Trasladar r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_TRANSFERIR_PRINCIPAL",List.of(req,r),()->{
        flow.guard(a);var q=flow.req(req);UUID com=uuid(q.get("communication_id"));core.lock(com);q=flow.req(req);flow.jefatura(a,q);flow.abierto(q);version(q,r.revision());flow.ambito(r.destino());distinto(q,r.destino());texto(r.fundamento());texto(r.resumen());
        if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_transfer WHERE requirement_id=? AND state='PENDIENTE'",Long.class,id(req))>0)throw new FalloTecnico(409,"TRANSFERENCIA_PENDIENTE");
        UUID p=UUID.randomUUID();db.sql.update("INSERT INTO institution_req_transfer(id,requirement_id,origin_destination,unit_code,site_id,area_id,reason_text,summary_text,state,revision,requested_by,created_ms) VALUES(?,?,?,?,?,?,?,?,'PENDIENTE',0,?,?)",id(p),id(req),q.get("principal_destination"),r.destino().unidad(),r.destino().sede(),r.destino().area(),r.fundamento(),r.resumen(),a.id(),clock.millis());reqRevision(req);
        historia(com,p,"TRASLADO_REQ",0,a,"PRINCIPAL_TRANSFERENCIA_SOLICITADA",r.fundamento(),r.destino());return new Resultado(p,0);
    });}
    public Resultado decidirTransferencia(Operador a,UUID key,UUID proposal,Resolver r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_DECIDIR_PRINCIPAL",List.of(proposal,r),()->{
        flow.guard(a);var p=uno("SELECT * FROM institution_req_transfer WHERE id=?",id(proposal));UUID req=uuid(p.get("requirement_id"));var q=flow.req(req);UUID com=uuid(q.get("communication_id"));core.lock(com);p=uno("SELECT * FROM institution_req_transfer WHERE id=?",id(proposal));q=flow.req(req);
        boolean retiro="RETIRADA".equals(r.decision());if(retiro)flow.jefatura(a,q);else flow.jefatura(a,p);version(p,r.revision());if(!"PENDIENTE".equals(p.get("state")))throw new FalloTecnico(409);texto(r.fundamento());
        if(!Set.of("ACEPTADA","RECHAZADA","RETIRADA").contains(r.decision()))throw new FalloTecnico(400);
        UUID acceptedDestination=null;Long acceptedAuthority=null;
        if("ACEPTADA".equals(r.decision())){
            flow.abierto(q);if(!Objects.equals(p.get("origin_destination"),q.get("principal_destination")))throw new FalloTecnico(409,"SOLICITUD_OBSOLETA");flow.jefatura(vigente(s(p,"requested_by")),q);independiente(a,s(p,"requested_by"));var target=flow.destino(p,a.id());flow.ambito(target);UUID dest=flow.crearDestino(com,target,a);acceptedDestination=dest;acceptedAuthority=db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);
            if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_attention WHERE requirement_id=?",Long.class,id(req))==0)db.sql.update("INSERT INTO institution_req_attention VALUES(?,?,?,?,?,?)",id(req),q.get("attention_actor"),q.get("unit_code"),q.get("site_id"),q.get("area_id"),id(proposal));
            db.sql.update("UPDATE institution_req SET principal_destination=?,unit_code=?,site_id=?,area_id=? WHERE requirement_id=?",id(dest),target.unidad(),target.sede(),target.area(),id(req));
            flow.historia(com,req,"REQ",n(q,"revision")+1,a,"REQ_PRINCIPAL_TRANSFERIDO",r.fundamento(),Map.of("solicitud",id(proposal),"principalAnterior",q.get("principal_destination"),"principalNuevo",id(dest)));
        }
        db.sql.update("UPDATE institution_req_transfer SET state=?,revision=revision+1,accepted_authority=?,accepted_destination=? WHERE id=?",r.decision(),acceptedAuthority,acceptedDestination==null?null:id(acceptedDestination),id(proposal));reqRevision(req);historia(com,proposal,"TRASLADO_REQ",r.revision()+1,a,"PRINCIPAL_"+r.decision(),r.fundamento(),Map.of("requerimiento",id(req)));return new Resultado(proposal,r.revision()+1);
    });}

    public Resultado derivar(Operador a,UUID key,UUID origin,Derivar r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_DERIVAR_CASO",List.of(origin,r),()->{
        flow.guard(a);var c=caso(a,origin);UUID com=uuid(c.get("communication_id"));core.lock(com);c=caso(a,origin);core.head(a,c);version(c,r.revision());flow.abierto(flow.req(uuid(c.get("requirement_id"))));if(!"EN_GESTION".equals(c.get("state"))||derivacionPendiente(origin))throw new FalloTecnico(409);
        flow.ambito(r.destino());var context=new HashMap<String,Object>();context.put("unit_code",c.get("unit_code"));context.put("site_id",c.get("site_id"));context.put("area_id",areas.area(c));distinto(context,r.destino());texto(r.fundamento());texto(r.resumen());texto(r.objetivo());UUID p=UUID.randomUUID();
        db.sql.update("INSERT INTO institution_case_derivation(id,origin_case_id,requirement_id,origin_destination,origin_area,origin_responsible,unit_code,site_id,area_id,reason_text,summary_text,objective_text,state,revision,requested_by,created_ms,successor_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'PENDIENTE',0,?,?,NULL)",id(p),id(origin),c.get("requirement_id"),c.get("destination_id"),areas.area(c),c.get("responsible_id"),r.destino().unidad(),r.destino().sede(),r.destino().area(),r.fundamento(),r.resumen(),r.objetivo(),a.id(),clock.millis());
        db.sql.update("UPDATE institution_case_derivation SET matter_id=? WHERE id=?",materia(origin),id(p));
        historia(com,p,"DERIVACION",0,a,"DERIVACION_INSTITUCIONAL_SOLICITADA",r.fundamento(),r.destino());return new Resultado(p,0);
    });}
    public Resultado decidirDerivacion(Operador a,UUID key,UUID proposal,Resolver r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_DECIDIR_DERIVACION",List.of(proposal,r),()->{
        flow.guard(a);var p=uno("SELECT * FROM institution_case_derivation WHERE id=?",id(proposal));var c=core.caseRow(uuid(p.get("origin_case_id")));UUID com=uuid(c.get("communication_id"));core.lock(com);p=uno("SELECT * FROM institution_case_derivation WHERE id=?",id(proposal));c=core.caseRow(uuid(p.get("origin_case_id")));
        if("RETIRADA".equals(r.decision()))core.head(a,c);else flow.jefatura(a,p);version(p,r.revision());if(!"PENDIENTE".equals(p.get("state")))throw new FalloTecnico(409);texto(r.fundamento());UUID successor=null;
        if("ACEPTADA".equals(r.decision())){
            var q=flow.req(uuid(p.get("requirement_id")));flow.abierto(q);if(!"EN_GESTION".equals(c.get("state"))||!Objects.equals(p.get("origin_destination"),c.get("destination_id"))||!Objects.equals(p.get("origin_area"),areas.area(c))||!Objects.equals(p.get("origin_responsible"),c.get("responsible_id"))||!Objects.equals(p.get("matter_id"),materia(uuid(c.get("id")))))throw new FalloTecnico(409,"SOLICITUD_OBSOLETA");core.head(vigente(s(p,"requested_by")),c);independiente(a,s(p,"requested_by"));
            var d=flow.destino(p,a.id());flow.ambito(d);flow.responsable(r.responsable(),d);UUID dest=flow.crearDestino(com,d,a);successor=flow.abrir(a,key,q,dest,r.responsable(),s(p,"objective_text")).id();db.sql.update("UPDATE core_case SET origin_case_id=? WHERE id=?",p.get("origin_case_id"),id(successor));reqRevision(uuid(p.get("requirement_id")));
        }else if(!Set.of("RECHAZADA","RETIRADA").contains(r.decision()))throw new FalloTecnico(400);
        db.sql.update("UPDATE institution_case_derivation SET state=?,revision=revision+1,successor_id=? WHERE id=?",r.decision(),successor==null?null:id(successor),id(proposal));historia(com,proposal,"DERIVACION",r.revision()+1,a,"DERIVACION_"+r.decision(),r.fundamento(),Map.of("sucesor",successor==null?"":id(successor)));return new Resultado(proposal,r.revision()+1);
    });}
    /** Circuito ordinario de solicitud/revisión; la modalidad DERIVACION se elige expresamente. */
    public Resultado solicitarCierreDerivacion(Operador a,UUID key,UUID caso,Decision r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_TERMINO_POR_DERIVACION",List.of(caso,r),()->{
        flow.guard(a);var c=caso(a,caso);core.lock(uuid(c.get("communication_id")));if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_derivation WHERE origin_case_id=? AND state='ACEPTADA'",Long.class,id(caso))<1)throw new FalloTecnico(409);
        var result=core.revisarCaso(a,UUID.nameUUIDFromBytes((key+":CIERRE").getBytes(java.nio.charset.StandardCharsets.UTF_8)),caso,r,"SOLICITUD_TERMINO");
        db.sql.update("INSERT INTO institution_case_closure_request VALUES(?,?,'DERIVACION')",id(caso),result.revision());return result;
    });}

    public Resultado solicitarReapertura(Operador a,UUID key,UUID caseId,Reabrir r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_SOLICITAR_REAPERTURA",List.of(caseId,r),()->{
        flow.guard(a);var c=caso(a,caseId);UUID com=uuid(c.get("communication_id"));core.lock(com);c=caso(a,caseId);version(c,r.revision());if(!a.id().equals(c.get("responsible_id")))throw new FalloTecnico(404);if(!"CERRADO".equals(c.get("state"))||"DERIVACION".equals(c.get("closure_mode")))throw new FalloTecnico(409,"REAPERTURA_NO_ORDINARIA");texto(r.fundamento());texto(r.antecedentes());
        if(db.sql.queryForObject("SELECT COUNT(*) FROM core_reopen WHERE case_id=? AND state='PENDIENTE'",Long.class,id(caseId))>0)throw new FalloTecnico(409,"REAPERTURA_PENDIENTE");UUID p=UUID.randomUUID();
        db.sql.update("INSERT INTO core_reopen(id,case_id,requested_by,requested_ms,case_revision,closed_ms,closed_by,closure_mode,reason_text,state,revision) VALUES(?,?,?,?,?,?,?,?,?,'PENDIENTE',0)",id(p),id(caseId),a.id(),clock.millis(),c.get("revision"),c.get("closed_ms"),c.get("closed_by"),c.get("closure_mode"),r.fundamento());db.sql.update("INSERT INTO institution_reopen(request_id,evidence_text) VALUES(?,?)",id(p),r.antecedentes());
        historia(com,p,"REAPERTURA",0,a,"REAPERTURA_INSTITUCIONAL_SOLICITADA",r.fundamento(),Map.of("caso",id(caseId)));return new Resultado(p,0);
    });}
    private Map<String,Object> reapertura(UUID id){return uno("SELECT r.*,i.evidence_text,i.principal_destination,i.requirement_revision,i.reactivation_by,i.reactivation_ms FROM core_reopen r JOIN institution_reopen i ON i.request_id=r.id WHERE r.id=?",id(id));}
    /** Solo referencias de avisos; nunca una credencial de lectura de COM/Caso/documento. */
    public void autorizarAviso(Operador a,String tipo,UUID id,boolean solicitud){
        flow.guard(a);
        if(tipo.equals("REAPERTURA_U2")){var p=reapertura(id);var c=caso(a,uuid(p.get("case_id")));if(solicitud)core.head(a,c);return;}
        String table=switch(tipo){case "TRASLADO_REQ_U2"->"institution_req_transfer";case "DERIVACION_U2"->"institution_case_derivation";default->throw new FalloTecnico(400);};
        var p=uno("SELECT * FROM "+table+" WHERE id=?",id(id));try{flow.jefatura(a,p);}catch(FalloTecnico denied){if(solicitud)throw denied;if(tipo.equals("DERIVACION_U2"))core.head(a,core.caseRow(uuid(p.get("origin_case_id"))));else{var origin=uno("SELECT d.unit_code,d.site_id,a.area_id FROM core_destination d LEFT JOIN destination_area a ON a.destination_id=d.id WHERE d.id=?",p.get("origin_destination"));flow.jefatura(a,origin);}}
    }
    public Resultado autorizarReactivacion(Operador a,UUID key,UUID proposal,Reactivar r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_AUTORIZAR_REACTIVACION_REQ",List.of(proposal,r),()->{
        flow.guard(a);var p=reapertura(proposal);var c=core.caseRow(uuid(p.get("case_id")));var q=flow.req(uuid(c.get("requirement_id")));UUID com=uuid(c.get("communication_id"));core.lock(com);p=reapertura(proposal);q=flow.req(uuid(c.get("requirement_id")));flow.jefatura(a,q);version(p,r.revision());version(q,r.revisionRequerimiento());if(!"PENDIENTE".equals(p.get("state"))||!"TERMINADO".equals(q.get("state")))throw new FalloTecnico(409);independiente(a,s(p,"requested_by"));texto(r.fundamento());
        db.sql.update("UPDATE institution_reopen SET principal_destination=?,requirement_revision=?,reactivation_by=?,reactivation_ms=? WHERE request_id=?",q.get("principal_destination"),q.get("revision"),a.id(),clock.millis(),id(proposal));db.sql.update("UPDATE core_reopen SET revision=revision+1 WHERE id=?",id(proposal));historia(com,proposal,"REAPERTURA",r.revision()+1,a,"REQ_REACTIVACION_AUTORIZADA",r.fundamento(),Map.of("requerimiento",q.get("id")));return new Resultado(proposal,r.revision()+1);
    });}
    public Resultado decidirReapertura(Operador a,UUID key,UUID proposal,DecidirReapertura r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_DECIDIR_REAPERTURA",List.of(proposal,r),()->{
        flow.guard(a);var p=reapertura(proposal);UUID caseId=uuid(p.get("case_id"));var c=caso(a,caseId);UUID com=uuid(c.get("communication_id"));core.lock(com);p=reapertura(proposal);c=caso(a,caseId);core.head(a,c);version(p,r.revision());version(c,r.revisionCaso());if(!"PENDIENTE".equals(p.get("state")))throw new FalloTecnico(409);texto(r.fundamento());
        if("APROBADA".equals(r.decision())){
            independiente(a,s(p,"requested_by"));if(!"CERRADO".equals(c.get("state"))||"DERIVACION".equals(c.get("closure_mode"))||n(p,"case_revision")!=n(c,"revision")||!Objects.equals(p.get("closed_ms"),c.get("closed_ms"))||!Objects.equals(p.get("requested_by"),c.get("responsible_id")))throw new FalloTecnico(409,"SOLICITUD_OBSOLETA");caso(vigente(s(p,"requested_by")),caseId);
            UUID req=uuid(c.get("requirement_id"));var q=flow.req(req);if("TERMINADO".equals(q.get("state"))){if(p.get("reactivation_by")==null||!Objects.equals(q.get("principal_destination"),p.get("principal_destination"))||n(q,"revision")!=n(p,"requirement_revision"))throw new FalloTecnico(409,"REQ_REQUIERE_REACTIVACION_EXPRESA");flow.jefatura(vigente(s(p,"reactivation_by")),q);independiente(vigente(s(p,"reactivation_by")),s(p,"requested_by"));db.sql.update("UPDATE core_requirement SET state='ACTIVO',ended_ms=NULL,revision=revision+1 WHERE id=?",id(req));flow.historia(com,req,"REQ",n(q,"revision")+1,a,"REQ_REACTIVADO_POR_AUTORIZACION",r.fundamento(),Map.of("autorizacion",id(proposal),"autorizadoPor",p.get("reactivation_by")));}
            // Los datos del cierre siguen en el Caso, la solicitud y el registro inmutable de cierres.
            db.sql.update("UPDATE core_case SET state='EN_GESTION',revision=revision+1 WHERE id=?",id(caseId));db.sql.update("INSERT INTO core_management_period VALUES(?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),id(proposal),clock.millis(),a.id());
        }else if(!"RECHAZADA".equals(r.decision()))throw new FalloTecnico(400);
        db.sql.update("UPDATE core_reopen SET state=?,revision=revision+1,decided_by=?,decided_ms=?,decision_text=? WHERE id=?",r.decision(),a.id(),clock.millis(),r.fundamento(),id(proposal));historia(com,proposal,"REAPERTURA",r.revision()+1,a,"REAPERTURA_"+r.decision(),r.fundamento(),Map.of("caso",id(caseId)));return new Resultado(proposal,r.revision()+1);
    });}

    void asignacionInicial(UUID caso){var assignments=db.sql.queryForList("SELECT id FROM core_assignment WHERE case_id=?",id(caso));if(assignments.size()!=1)throw new FalloTecnico(409,"ASIGNACION_NO_ACREDITADA");asignacionActual(caso,uuid(assignments.getFirst().get("id")));}
    void asignacionActual(UUID caso,UUID asignacion){db.sql.update("DELETE FROM institution_assignment_current WHERE case_id=?",id(caso));db.sql.update("INSERT INTO institution_assignment_current VALUES(?,?)",id(caso),id(asignacion));}
    public Resultado acusar(Operador a,UUID key,UUID caseId,Acusar r){if(r==null)throw new FalloTecnico(400);return core.execute(key,a,"U2_ACUSAR_ASIGNACION",List.of(caseId,r),()->{
        flow.guard(a);var c=caso(a,caseId);UUID com=uuid(c.get("communication_id"));core.lock(com);c=caso(a,caseId);if(!a.id().equals(c.get("responsible_id")))throw new FalloTecnico(404);var assignment=uno("SELECT x.* FROM core_assignment x JOIN institution_assignment_current i ON i.assignment_id=x.id WHERE i.case_id=?",id(caseId));if(!id(r.asignacion()).equals(assignment.get("id"))||!a.id().equals(assignment.get("responsible_id")))throw new FalloTecnico(409,"ASIGNACION_OBSOLETA");
        if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt WHERE assignment_id=?",Long.class,id(r.asignacion()))==0){db.sql.update("INSERT INTO institution_assignment_receipt VALUES(?,?,?)",id(r.asignacion()),a.id(),clock.millis());historia(com,r.asignacion(),"ASIGNACION",0,a,"ASIGNACION_ACUSADA","Acuse manual de la asignación",Map.of("caso",id(caseId)));}return new Resultado(r.asignacion(),0);
    });}

    /** Paquetes de evaluación sin documentos, actuaciones ni acceso al expediente por destino. */
    public Map<String,Object> bandeja(Operador a){return db.transaction(()->{flow.guard(a);return Map.of("transferencias",bandejaPaquetes(a,false),"derivaciones",bandejaPaquetes(a,true));});}
    private List<Map<String,Object>> bandejaPaquetes(Operador a,boolean derivacion){
        String scopeOrigen=flow.scope(a,"o",true).replace("o.unit_code","od.unit_code").replace("o.site_id","od.site_id").replace("o.area_id","oa.area_id");
        String join=derivacion?" JOIN core_case oc ON oc.id=p.origin_case_id JOIN core_destination od ON od.id=oc.destination_id LEFT JOIN case_area oa ON oa.case_id=oc.id ":" JOIN core_destination od ON od.id=p.origin_destination LEFT JOIN destination_area oa ON oa.destination_id=od.id ";
        var result=rows("SELECT p.id,p.requirement_id,p.unit_code,p.site_id,p.area_id,p.reason_text,p.summary_text,p.state,p.revision"+(derivacion?",p.origin_case_id,p.objective_text,p.matter_id,p.successor_id":"")+" FROM "+(derivacion?"institution_case_derivation":"institution_req_transfer")+" p "+join+" WHERE ("+flow.scope(a,"p",true)+" OR "+scopeOrigen+") ORDER BY p.created_ms DESC,p.id LIMIT 100");
        for(var p:result){boolean puede=a.jefeEn(s(p,"unit_code"),s(p,"site_id"),s(p,"area_id"));if(p.get("area_id")!=null)puede&=areas.vigente(s(p,"area_id"),s(p,"unit_code"),s(p,"site_id"));p.put("puedeDecidirDestino",puede);}return paquetes(result);
    }
    public Map<String,Object> requerimiento(Operador a,UUID req){return db.transaction(()->{flow.guard(a);var q=flow.req(req);flow.jefatura(a,q);var result=new LinkedHashMap<String,Object>();result.put("transferencias",paquetes(rows("SELECT id,unit_code,site_id,area_id,state,revision,reason_text,summary_text FROM institution_req_transfer WHERE requirement_id=? ORDER BY created_ms,id",id(req))));
        result.put("reactivaciones",rows("SELECT r.id,r.revision,c.id AS case_id,r.reason_text,i.reactivation_by FROM core_reopen r JOIN institution_reopen i ON i.request_id=r.id JOIN core_case c ON c.id=r.case_id WHERE c.requirement_id=? AND r.state='PENDIENTE' AND EXISTS(SELECT 1 FROM core_requirement q WHERE q.id=c.requirement_id AND q.state='TERMINADO')",id(req)));return result;});}
    public Map<String,Object> caso(Operador a,UUID caseId,boolean detalle){return db.transaction(()->{flow.guard(a);caso(a,caseId);return Map.of("derivaciones",paquetes(rows("SELECT id,unit_code,site_id,area_id,state,revision,reason_text,summary_text,objective_text,successor_id FROM institution_case_derivation WHERE origin_case_id=? ORDER BY created_ms,id",id(caseId))),"reaperturas",paquetes(rows("SELECT r.id,r.revision,r.state,r.case_revision,r.reason_text,r.decision_text,r.requested_by,i.evidence_text,i.reactivation_by FROM core_reopen r JOIN institution_reopen i ON i.request_id=r.id WHERE r.case_id=? ORDER BY r.requested_ms,r.id",id(caseId))),"asignacion",rows("SELECT x.id,x.responsible_id,x.at_ms,r.acknowledged_by,r.acknowledged_ms FROM institution_assignment_current i JOIN core_assignment x ON x.id=i.assignment_id LEFT JOIN institution_assignment_receipt r ON r.assignment_id=x.id WHERE i.case_id=?",id(caseId)),"cierres",rows("SELECT * FROM institution_case_closure WHERE case_id=? ORDER BY case_revision",id(caseId)));});}
}
