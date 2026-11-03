package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;
import static cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales.*;

/** Solicitud inmutable → aprobación institucional independiente → ejecución TI. Ninguna etapa crea autoridad raíz. */
public final class ConcesionesInstitucionales {
    public record Solicitud(String beneficiario,String rol,String alcance,String unidad,String sede,Instant desde,Instant hasta,String fundamento){}
    public record Decision(long revision,String fundamento){}
    public record Ejecucion(long revision){}
    public record Resultado(UUID id,long revision){}
    private final BaseJdbc db;
    private final IdentidadInstitucional ids;
    private final AutoridadesInstitucionales authority;
    private final JsonMapper json;
    private final Clock clock;
    public ConcesionesInstitucionales(BaseJdbc db,IdentidadInstitucional ids,AutoridadesInstitucionales authority,JsonMapper json,Clock clock){this.db=db;this.ids=ids;this.authority=authority;this.json=json;this.clock=clock;}
    private Map<String,Object> fila(UUID id){var rows=db.sql.queryForList("SELECT * FROM access_instruction WHERE id=?",id.toString());if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
    private IdentidadInstitucional.Otorgar grant(Map<String,Object> row){return new IdentidadInstitucional.Otorgar(str(row,"role_code"),str(row,"scope_code"),str(row,"unit_code"),str(row,"site_id"),Instant.ofEpochMilli(num(row,"valid_from")),Instant.ofEpochMilli(num(row,"valid_until")));}
    private void ti(String actor){if(!ids.operador(actor).ti())throw new FalloTecnico(403);}
    private void revision(Map<String,Object> row,long expected){if(expected<0||num(row,"revision")!=expected)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private void estado(Map<String,Object> row,String required){if(!required.equals(row.get("state")))throw new FalloTecnico(409,"ESTADO_INSTRUCCION_INCOMPATIBLE");}
    private void distinto(String a,String b){if(authority.persona(a).equals(authority.persona(b)))throw new FalloTecnico(403,"SEPARACION_DE_FUNCIONES_REQUERIDA");}
    private void beneficiarioHabilitado(String actor){try{ids.operador(actor);}catch(FalloTecnico e){if(Set.of(401,403,404).contains(e.status))throw new FalloTecnico(409,"BENEFICIARIO_NO_HABILITADO");throw e;}}
    private void solicitanteHabilitado(String actor){try{authority.actor(actor);}catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))throw new FalloTecnico(409,"INSTRUCCION_NO_VIGENTE");throw e;}}
    private void validar(Solicitud s){
        if(s==null)throw new FalloTecnico(400);codigo(s.beneficiario());texto(s.fundamento(),1000);
        if(s.desde()==null||s.hasta()==null||!s.hasta().isAfter(clock.instant())||!s.hasta().isAfter(s.desde()))throw new FalloTecnico(400);
        intervalo(s.desde(),s.hasta(),s.hasta());
        ids.validarConcesion(new IdentidadInstitucional.Otorgar(s.rol(),s.alcance(),s.unidad(),s.sede(),s.desde(),s.hasta()));
    }
    private String hashSolicitud(Solicitud s){return sha(json.writeValueAsBytes(Arrays.asList(s.beneficiario(),s.rol(),s.alcance(),s.unidad(),s.sede(),s.desde().toEpochMilli(),s.hasta().toEpochMilli(),s.fundamento())));}
    private void integro(Map<String,Object> row){var g=grant(row);String actual=hashSolicitud(new Solicitud(str(row,"beneficiary_id"),g.rol(),g.alcance(),g.unidad(),g.sede(),g.desde(),g.hasta(),str(row,"reason_text")));if(!actual.equals(row.get("request_hash")))throw new FalloTecnico(409,"INSTRUCCION_ALTERADA");}
    private Resultado comando(String actor,UUID key,String op,Object body,UUID resource,Supplier<Resultado> work){
        if(key==null)throw new FalloTecnico(400);String hash=sha(json.writeValueAsBytes(Arrays.asList("CONCESION_1",actor,op,resource,body)));
        try{return db.transaction(()->{
            authority.guard();authority.actor(actor);
            var old=db.sql.queryForList("SELECT * FROM authority_command WHERE id=?",key.toString());
            if(!old.isEmpty()){
                var previous=old.getFirst();if(!actor.equals(previous.get("actor_id"))||!op.equals(previous.get("action_code"))||!hash.equals(previous.get("request_hash")))throw new FalloTecnico(409,"IDEMPOTENCIA_CONFLICTIVA");
                UUID result=UUID.fromString(str(previous,"result_id"));autorizarComando(actor,op,fila(result));return new Resultado(result,num(previous,"result_revision"));
            }
            var result=work.get();db.sql.update("INSERT INTO authority_command VALUES(?,?,?,?,?,?)",key.toString(),actor,op,hash,result.id().toString(),result.revision());
            authority.evento(actor,result.id().toString(),op,"CONFIRMADO",fila(result.id()));return result;
        });}catch(FalloTecnico e){
            if(Set.of(403,404,409).contains(e.status))db.transaction(()->{authority.guard();authority.evento(actor,resource==null?null:resource.toString(),op,"DENEGADO_"+e.status,null);return null;});
            throw e;
        }
    }
    private void autorizarComando(String actor,String op,Map<String,Object> row){
        if(op.equals("SOLICITAR")){ti(actor);beneficiarioHabilitado(str(row,"beneficiary_id"));distinto(actor,str(row,"beneficiary_id"));return;}
        if(op.equals("EJECUTAR")){
            ti(actor);beneficiarioHabilitado(str(row,"beneficiary_id"));solicitanteHabilitado(str(row,"requester_id"));distinto(actor,str(row,"beneficiary_id"));distinto(actor,str(row,"approver_id"));integro(row);
            if(!Set.of("APROBADA","EJECUTADA").contains(str(row,"state")))throw new FalloTecnico(409,"ESTADO_INSTRUCCION_INCOMPATIBLE");
            if(!grant(row).hasta().isAfter(clock.instant()))throw new FalloTecnico(409,"INSTRUCCION_VENCIDA");authority.comprobarDecision(str(row,"approver_id"),num(row,"authority_revision"),str(row,"mandate_id"),grant(row));return;
        }
        authority.mandato(actor,grant(row),op.equals("REVOCAR"));distinto(actor,str(row,"beneficiary_id"));distinto(actor,str(row,"requester_id"));
        if(op.equals("APROBAR"))beneficiarioHabilitado(str(row,"beneficiary_id"));
    }
    public Resultado solicitar(String actor,UUID key,Solicitud s){
        validar(s);
        return comando(actor,key,"SOLICITAR",s,null,()->{
            ti(actor);beneficiarioHabilitado(s.beneficiario());distinto(actor,s.beneficiario());
            UUID id=UUID.randomUUID();String hash=hashSolicitud(s);
            db.sql.update("INSERT INTO access_instruction(id,requester_id,beneficiary_id,role_code,scope_code,unit_code,site_id,valid_from,valid_until,reason_text,request_hash,state,revision,created_ms) VALUES(?,?,?,?,?,?,?,?,?,?,?,'PENDIENTE',0,?)",id.toString(),actor,s.beneficiario(),s.rol(),s.alcance(),s.unidad(),s.sede(),s.desde().toEpochMilli(),s.hasta().toEpochMilli(),s.fundamento(),hash,clock.millis());return new Resultado(id,0);
        });
    }
    public Resultado decidir(String actor,UUID key,UUID id,Decision d,boolean aprobar){
        if(d==null)throw new FalloTecnico(400);texto(d.fundamento(),1000);
        return comando(actor,key,aprobar?"APROBAR":"RECHAZAR",d,id,()->{
            var row=fila(id);autorizarComando(actor,aprobar?"APROBAR":"RECHAZAR",row);revision(row,d.revision());estado(row,"PENDIENTE");integro(row);
            var g=grant(row);var mandate=authority.mandato(actor,g,false);String approverPerson=authority.persona(actor),targetPerson=authority.persona(str(row,"beneficiary_id"));
            if(aprobar){if(!g.hasta().isAfter(clock.instant()))throw new FalloTecnico(409,"INSTRUCCION_VENCIDA");sinCiclo(approverPerson,targetPerson,g);}
            db.sql.update("UPDATE access_instruction SET state=?,revision=revision+1,approver_id=?,approver_person=?,beneficiary_person=?,authority_revision=?,mandate_id=?,approved_ms=?,decision_reason=? WHERE id=?",aprobar?"APROBADA":"RECHAZADA",actor,approverPerson,targetPerson,authority.revision(),mandate.get("id"),clock.millis(),d.fundamento(),id.toString());return new Resultado(id,d.revision()+1);
        });
    }
    private void sinCiclo(String approver,String beneficiary,IdentidadInstitucional.Otorgar grant){
        var edges=db.sql.queryForList("SELECT approver_person,beneficiary_person FROM access_instruction WHERE state IN ('APROBADA','EJECUTADA') AND valid_until>? AND valid_from<? AND valid_until>?",clock.millis(),grant.hasta().toEpochMilli(),grant.desde().toEpochMilli());
        Map<String,List<String>> graph=new HashMap<>();for(var e:edges)graph.computeIfAbsent(str(e,"approver_person"),k->new ArrayList<>()).add(str(e,"beneficiary_person"));
        var queue=new ArrayDeque<String>();var seen=new HashSet<String>();queue.add(beneficiary);
        while(!queue.isEmpty()){String next=queue.remove();if(next.equals(approver))throw new FalloTecnico(403,"APROBACION_CRUZADA_NO_PERMITIDA");if(seen.add(next))queue.addAll(graph.getOrDefault(next,List.of()));}
    }
    public Resultado ejecutar(String actor,UUID key,UUID id,Ejecucion execution){
        if(execution==null)throw new FalloTecnico(400);
        return comando(actor,key,"EJECUTAR",execution,id,()->{
            var row=fila(id);autorizarComando(actor,"EJECUTAR",row);revision(row,execution.revision());estado(row,"APROBADA");
            var g=grant(row);if(!g.hasta().isAfter(clock.instant()))throw new FalloTecnico(409,"INSTRUCCION_VENCIDA");
            authority.persona(str(row,"beneficiary_id"));
            authority.comprobarDecision(str(row,"approver_id"),num(row,"authority_revision"),str(row,"mandate_id"),g);
            if(!str(row,"beneficiary_person").equals(authority.persona(str(row,"beneficiary_id"))))throw new FalloTecnico(409,"IDENTIDAD_NO_REASIGNABLE");
            String assignment=ids.otorgarRespaldado(actor,str(row,"beneficiary_id"),g);
            db.sql.update("INSERT INTO approved_assignment VALUES(?,?)",assignment,id.toString());
            db.sql.update("UPDATE access_instruction SET state='EJECUTADA',revision=revision+1,assignment_id=?,executed_by=?,executed_ms=? WHERE id=?",assignment,actor,clock.millis(),id.toString());return new Resultado(id,execution.revision()+1);
        });
    }
    public Resultado revocar(String actor,UUID key,UUID id,Decision d){
        if(d==null)throw new FalloTecnico(400);texto(d.fundamento(),1000);
        return comando(actor,key,"REVOCAR",d,id,()->{
            var row=fila(id);autorizarComando(actor,"REVOCAR",row);revision(row,d.revision());
            if(!Set.of("PENDIENTE","APROBADA","EJECUTADA").contains(str(row,"state")))throw new FalloTecnico(409,"ESTADO_INSTRUCCION_INCOMPATIBLE");
            if(row.get("assignment_id")!=null)ids.revocarRespaldado(actor,str(row,"beneficiary_id"),str(row,"assignment_id"));
            db.sql.update("UPDATE access_instruction SET state='REVOCADA',revision=revision+1,revoked_by=?,revoked_ms=?,revocation_reason=? WHERE id=?",actor,clock.millis(),d.fundamento(),id.toString());return new Resultado(id,d.revision()+1);
        });
    }
    private boolean visible(String actor,Map<String,Object> row){
        if(ids.operador(actor).ti()||Set.of(str(row,"requester_id"),str(row,"beneficiary_id")).contains(actor)||actor.equals(row.get("approver_id")))return true;
        try{authority.mandato(actor,grant(row),false);return true;}catch(FalloTecnico e){if(e.status!=403&&e.status!=409)throw e;return false;}
    }
    public Map<String,Object> leer(String actor,UUID id){
        authority.actor(actor);var row=fila(id);if(!visible(actor,row))throw new FalloTecnico(404);return salida(row);
    }
    public List<Map<String,Object>> listar(String actor){
        authority.actor(actor);return db.sql.queryForList("SELECT * FROM access_instruction ORDER BY created_ms DESC,id LIMIT 500").stream().filter(r->visible(actor,r)).map(AutoridadesInstitucionales::salida).toList();
    }
    public List<Map<String,Object>> historial(String actor,UUID id){leer(actor,id);return db.sql.queryForList("SELECT actor_id,action_code,result_code,at_ms,snapshot_doc FROM authority_event WHERE resource_id=? ORDER BY seq DESC LIMIT 100",id.toString()).stream().map(AutoridadesInstitucionales::salida).toList();}
}
