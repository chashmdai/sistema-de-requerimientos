package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;
import cl.nexbyte.siger.institucional.robustez.*;
import java.sql.Statement;import java.time.*;import java.util.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import tools.jackson.databind.json.JsonMapper;

/** Intención de aviso y contexto histórico: informar una decisión no concede expediente. */
public final class AvisosNucleo {
    public record Contexto(String unidad,String sede,String area){}
    public record Datos(String version,String tipo,UUID solicitud,UUID requerimiento,UUID destinoActual,Contexto origen,Contexto destino,String asignador,String encargado,String sedeCaptura,Boolean capturaRegional){}
    public record Vista(String tipo,Instant fecha,String folio,Contexto origen,Contexto destino,String ruta,boolean pendienteActual){}
    public record Enrutamiento(String tipo,Instant fecha,String estado){}
    private record Fuente(Datos datos,UUID comunicacion,Instant fecha){}
    private static final String ACTION="AVISO_INSTITUCIONAL_PROGRAMADO";
    private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio cat;private final JsonMapper json;private final Clock clock;
    AvisosNucleo(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio cat,JsonMapper json,Clock clock){this.db=db;this.core=core;this.cat=cat;this.json=json;this.clock=clock;}
    static Contexto contexto(ContratosNucleo.NuevoDestino d){return new Contexto(d.unidad(),d.sede(),d.area());}
    static Contexto contexto(Map<String,Object> r){return new Contexto(str(r,"unit_code"),str(r,"site_id"),str(r,"area_id"));}
    private static String str(Map<String,Object> r,String k){return r.get(k)==null?null:r.get(k).toString();}
    long programar(Operador a,UUID com,UUID recurso,String kind,long revision,Datos datos){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transacción de negocio requerida");
        var key=new GeneratedKeyHolder();db.sql.update(connection->{var s=connection.prepareStatement("INSERT INTO institution_flow_history(communication_id,resource_id,resource_kind,revision,actor_id,at_ms,action_code,reason_text,snapshot_doc) VALUES(?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            s.setString(1,com.toString());s.setString(2,recurso.toString());s.setString(3,kind);s.setLong(4,revision);s.setString(5,a.id());s.setLong(6,clock.millis());s.setString(7,ACTION);s.setString(8,"Intención durable de aviso institucional");s.setString(9,json.writeValueAsString(datos));return s;},key);
        if(key.getKey()==null)throw new FalloTecnico(503);long seq=key.getKey().longValue();
        db.sql.update("INSERT INTO workflow_event(event_key,code,resource_type,resource_id,at_ms) VALUES(?,?,'AVISO_U2',?,?)","AVISO_U2:"+seq,datos.tipo(),recurso.toString(),clock.millis());return seq;
    }
    private static long seq(String value){try{long n=Long.parseLong(value);if(n<1)throw new IllegalArgumentException();return n;}catch(IllegalArgumentException e){throw new FalloTecnico(404);}}
    private static boolean contextoValido(Contexto c){return c!=null&&c.unidad()!=null&&c.unidad().matches("[A-Z0-9_]{1,8}")&&c.sede()!=null&&c.sede().matches("[A-Za-z0-9_.-]{1,40}")&&(c.area()==null||c.area().matches("[A-Za-z0-9_.-]{1,64}"));}
    private Fuente fuente(UUID recurso,String evento){
        if(evento==null||!evento.matches("[1-9][0-9]{0,18}"))throw new FalloTecnico(404);
        var events=db.sql.queryForList("SELECT * FROM workflow_event WHERE seq=? AND resource_type='AVISO_U2' AND resource_id=?",seq(evento),recurso.toString());if(events.size()!=1)throw new FalloTecnico(404);var e=events.getFirst();String ref=str(e,"event_key");if(ref==null||!ref.matches("AVISO_U2:[1-9][0-9]{0,18}"))throw new FalloTecnico(404);
        var rows=db.sql.queryForList("SELECT * FROM institution_flow_history WHERE seq=? AND resource_id=? AND action_code=?",seq(ref.substring(9)),recurso.toString(),ACTION);if(rows.size()!=1)throw new FalloTecnico(404);var row=rows.getFirst();
        try{
            var p=AutoridadesInstitucionales.lectorPrivado().readValue(str(row,"snapshot_doc"),Datos.class);
            if(!"1.0".equals(p.version())||!Set.of("SOL_DISTRIBUIDA","SOL_REDIRIGIDA","REQ_ASIGNADO").contains(p.tipo())||!p.tipo().equals(e.get("code"))||p.destinoActual()==null||(!contextoValido(p.destino()))||p.solicitud()==null||p.asignador()==null||p.capturaRegional()==null||p.capturaRegional()&&p.sedeCaptura()==null||p.tipo().equals("SOL_REDIRIGIDA")&&!contextoValido(p.origen()))throw new IllegalArgumentException();
            boolean req=p.tipo().equals("REQ_ASIGNADO");if(req?(p.requerimiento()==null||!recurso.equals(p.requerimiento())||!"REQ".equals(row.get("resource_kind"))||p.encargado()==null):(!recurso.equals(p.solicitud())||p.requerimiento()!=null||!"SOL".equals(row.get("resource_kind"))))throw new IllegalArgumentException();
            return new Fuente(p,UUID.fromString(str(row,"communication_id")),Instant.ofEpochMilli(((Number)row.get("at_ms")).longValue()));
        }catch(RuntimeException invalid){throw new FalloTecnico(404);}
    }
    private boolean jefatura(Operador a,Contexto c){
        if(c==null||!a.jefeEn(c.unidad(),c.sede(),c.area()))return false;
        if(c.area()!=null)return new PoliticaAreas(db,clock).vigente(c.area(),c.unidad(),c.sede());
        return db.sql.queryForObject("SELECT COUNT(*) FROM institution_area WHERE unit_code=? AND site_id=?",Long.class,c.unidad(),c.sede())==0;
    }
    private Map<String,Object> atencion(Map<String,Object> r){var rows=db.sql.queryForList("SELECT * FROM institution_req_attention WHERE requirement_id=?",r.get("id"));if(!rows.isEmpty())return rows.getFirst();var initial=new LinkedHashMap<String,Object>();r.forEach((k,v)->initial.put(k.toLowerCase(Locale.ROOT),v));initial.put("actor_id",r.get("attention_actor"));return initial;}
    private boolean encargado(Operador a,Datos p,Map<String,Object> req){
        if(!a.id().equals(p.encargado())||!a.id().equals(req.get("attention_actor")))return false;var c=atencion(req);if(!a.id().equals(c.get("actor_id"))||!p.destino().equals(contexto(c)))return false;
        return (p.destino().area()==null||new PoliticaAreas(db,clock).vigente(p.destino().area(),p.destino().unidad(),p.destino().sede()))&&(a.profesionalEn(p.destino().unidad(),p.destino().sede(),p.destino().area())||jefatura(a,p.destino()));
    }
    private void autorizar(Operador a,Datos p){
        if(a==null||!cat.operador(a.id()).equals(a))throw new FalloTecnico(403);
        boolean head=jefatura(a,p.origen())||jefatura(a,p.destino());
        if(p.tipo().equals("REQ_ASIGNADO")){if(head||encargado(a,p,core.u2().req(p.requerimiento())))return;}
        else if(head||a.id().equals(p.asignador())&&(p.capturaRegional()?a.accesos().stream().anyMatch(x->x.rol().equals("COORDINADOR_REGIONAL")&&Objects.equals(x.sede(),p.sedeCaptura())):a.distribuidor()))return;
        throw new FalloTecnico(404);
    }
    public boolean permite(Operador a,UUID recurso,String evento,String tipo){try{var datos=fuente(recurso,evento).datos();if(!datos.tipo().equals(tipo))return false;autorizar(a,datos);return true;}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
    public boolean vigente(UUID recurso,String evento){
        try{var p=fuente(recurso,evento).datos();
            if(p.tipo().equals("REQ_ASIGNADO")){var q=core.u2().req(p.requerimiento());return "ACTIVO".equals(q.get("state"))&&p.destinoActual().toString().equals(q.get("principal_destination"))&&p.encargado().equals(q.get("attention_actor"))&&p.encargado().equals(atencion(q).get("actor_id"))&&p.destino().equals(contexto(atencion(q)))&&db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,p.requerimiento().toString())==0;}
            var rows=db.sql.queryForList("SELECT state,destination_id FROM institution_sol WHERE id=?",p.solicitud().toString());if(rows.size()!=1)return false;var r=rows.getFirst();return p.destinoActual().toString().equals(r.get("destination_id"))&&(!p.tipo().equals("SOL_DISTRIBUIDA")||r.get("state").equals("PENDIENTE"));
        }catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}
    }
    public Vista consultar(Operador a,UUID recurso,String evento){return db.transaction(()->{
        core.bloquearAcceso(a);var f=fuente(recurso,evento);var p=f.datos();autorizar(a,p);String ruta=null;
        try{if(p.tipo().equals("REQ_ASIGNADO")){core.u2().lector(a,core.u2().req(p.requerimiento()));ruta="requerimiento/"+p.requerimiento();}else{core.autorizarLectura(a,f.comunicacion());ruta="sol/"+p.solicitud();}}
        catch(FalloTecnico denied){if(!Set.of(401,403,404,409).contains(denied.status))throw denied;}
        var r=db.sql.queryForMap("SELECT folio_prefix,folio_width,numero FROM communication_registry WHERE comunicacion_id=?",f.comunicacion().toString());String folio=r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+r.get("folio_width")+"d",((Number)r.get("numero")).longValue());
        new PoliticaMaterias(db,clock).auditar(a,recurso.toString(),"AVISO_U2_CONSULTADO");return new Vista(p.tipo(),f.fecha(),folio,p.origen(),p.destino(),ruta,vigente(recurso,evento));
    });}
    List<Enrutamiento> enrutamiento(UUID recurso){
        return db.sql.queryForList("SELECT h.at_ms,w.code,s.outcome FROM institution_flow_history h JOIN workflow_event w ON w.event_key=CONCAT('AVISO_U2:',h.seq) AND w.resource_type='AVISO_U2' LEFT JOIN notification_source s ON s.stream_code='TAREA' AND s.event_key=CONCAT('',w.seq) WHERE h.resource_id=? AND h.action_code=? ORDER BY h.seq",recurso.toString(),ACTION).stream().map(r->new Enrutamiento(str(r,"code"),Instant.ofEpochMilli(((Number)r.get("at_ms")).longValue()),str(r,"outcome")==null?"PENDIENTE_PROYECCION":str(r,"outcome"))).toList();
    }
}
