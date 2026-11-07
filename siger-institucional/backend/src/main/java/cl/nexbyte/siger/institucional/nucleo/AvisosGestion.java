package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.AvisosNucleo.Contexto;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;

/** Decisiones de asignación y término. El afectado saliente recibe una constancia, nunca acceso al Caso. */
public final class AvisosGestion {
    private static final String ACTION="AVISO_GESTION_PROGRAMADO";
    private static final Set<String> CASO=Set.of("CASO_ASIGNADO","CASO_TERMINO_DEVUELTO","CASO_CERRADO");
    private static final Set<String> REQ=Set.of("REQ_TERMINADO","REQ_REACTIVADO");
    public record Datos(String version,String tipo,UUID requerimiento,UUID caso,Contexto destino,String entrante,String saliente,UUID asignacion,UUID asignacionAnterior,Long revision,String estado,String registrador){}
    private record Fuente(Datos datos,UUID comunicacion,Instant fecha){}
    private final BaseJdbc db;private final NucleoInstitucional core;private final CatalogoLaboratorio cat;private final Clock clock;private final PoliticaAreas areas;
    AvisosGestion(BaseJdbc db,NucleoInstitucional core,CatalogoLaboratorio cat,Clock clock){this.db=db;this.core=core;this.cat=cat;this.clock=clock;areas=new PoliticaAreas(db,clock);}
    private static UUID uuid(Object o){return UUID.fromString(o.toString());}
    private static String s(Map<String,Object> r,String k){return r.get(k)==null?null:r.get(k).toString();}
    private Contexto contexto(Map<String,Object> c){return new Contexto(s(c,"unit_code"),s(c,"site_id"),areas.area(c));}
    private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private String registrador(UUID com){var rows=db.sql.queryForList("SELECT actor_id FROM core_manual WHERE communication_id=?",com.toString());return rows.size()==1?s(rows.getFirst(),"actor_id"):null;}
    public void asignacion(Operador a,UUID caso,UUID anterior,String saliente){
        var c=core.caseRow(caso);var ids=db.sql.queryForList("SELECT assignment_id FROM institution_assignment_current WHERE case_id=?",caso.toString());if(ids.size()!=1)throw new FalloTecnico(409,"ASIGNACION_NO_ACREDITADA");
        guardar(a,uuid(c.get("communication_id")),caso,"CASO",new Datos("1.0","CASO_ASIGNADO",uuid(c.get("requirement_id")),caso,contexto(c),s(c,"responsible_id"),saliente,uuid(ids.getFirst().get("assignment_id")),anterior,n(c,"revision"),s(c,"state"),null));
    }
    public void terminoCaso(Operador a,UUID caso,String tipo){
        if(!Set.of("CASO_TERMINO_DEVUELTO","CASO_CERRADO").contains(tipo))throw new IllegalArgumentException();var c=core.caseRow(caso);
        guardar(a,uuid(c.get("communication_id")),caso,"CASO",new Datos("1.0",tipo,uuid(c.get("requirement_id")),caso,contexto(c),s(c,"responsible_id"),null,null,null,n(c,"revision"),s(c,"state"),null));
    }
    public void terminoReq(Operador a,UUID req,String tipo){
        if(!REQ.contains(tipo))throw new IllegalArgumentException();var q=core.u2().req(req);UUID com=uuid(q.get("communication_id"));
        guardar(a,com,req,"REQ",new Datos("1.0",tipo,req,null,AvisosNucleo.contexto(q),s(q,"attention_actor"),null,null,null,n(q,"revision"),s(q,"state"),registrador(com)));
    }
    private void guardar(Operador a,UUID com,UUID recurso,String kind,Datos datos){core.avisosNucleo().guardar(a,com,recurso,kind,datos.revision(),ACTION,"AVISO_GESTION",datos.tipo(),datos);}
    private Fuente fuente(UUID recurso,String evento){
        var ref=core.avisosNucleo().registro(recurso,evento,ACTION,"AVISO_GESTION");var h=ref.historia();
        try{
            var p=AutoridadesInstitucionales.lectorPrivado().readValue(s(h,"snapshot_doc"),Datos.class);boolean cas=CASO.contains(p.tipo());
            if(!"1.0".equals(p.version())||(!cas&&!REQ.contains(p.tipo()))||!ref.tipo().equals(p.tipo())||p.requerimiento()==null||p.entrante()==null||!AvisosNucleo.contextoValido(p.destino())||p.revision()==null||p.revision()<0||n(h,"revision")!=p.revision()||p.estado()==null)throw new IllegalArgumentException();
            var q=core.u2().req(p.requerimiento());UUID com=uuid(q.get("communication_id"));if(!com.toString().equals(h.get("communication_id")))throw new IllegalArgumentException();
            if(cas){
                if(!"CASO".equals(h.get("resource_kind"))||!recurso.equals(p.caso()))throw new IllegalArgumentException();var c=core.caseRow(p.caso());
                if(!p.requerimiento().toString().equals(c.get("requirement_id")))throw new IllegalArgumentException();
                if(p.tipo().equals("CASO_ASIGNADO")){
                    if(p.asignacion()==null||(p.saliente()==null&&p.asignacionAnterior()!=null)||!Set.of("EN_GESTION","CERRADO").contains(p.estado())||db.sql.queryForObject("SELECT COUNT(*) FROM core_assignment WHERE id=? AND case_id=? AND responsible_id=?",Long.class,p.asignacion().toString(),recurso.toString(),p.entrante())!=1)throw new IllegalArgumentException();
                    if(p.saliente()!=null&&(p.asignacionAnterior()==null||p.saliente().equals(p.entrante())||db.sql.queryForObject("SELECT COUNT(*) FROM core_assignment WHERE id=? AND case_id=? AND responsible_id=?",Long.class,p.asignacionAnterior().toString(),recurso.toString(),p.saliente())!=1))throw new IllegalArgumentException();
                }else if(p.asignacion()!=null||p.asignacionAnterior()!=null||p.saliente()!=null)throw new IllegalArgumentException();
            }else if(!"REQ".equals(h.get("resource_kind"))||p.caso()!=null||!recurso.equals(p.requerimiento())||p.saliente()!=null||p.asignacion()!=null||p.asignacionAnterior()!=null)throw new IllegalArgumentException();
            if(p.tipo().equals("CASO_CERRADO")&&!"CERRADO".equals(p.estado())||p.tipo().equals("CASO_TERMINO_DEVUELTO")&&!"EN_GESTION".equals(p.estado())||p.tipo().equals("REQ_TERMINADO")&&!"TERMINADO".equals(p.estado())||p.tipo().equals("REQ_REACTIVADO")&&!"ACTIVO".equals(p.estado()))throw new IllegalArgumentException();
            return new Fuente(p,com,Instant.ofEpochMilli(n(h,"at_ms")));
        }catch(org.springframework.dao.DataAccessException unavailable){throw unavailable;}
        catch(RuntimeException invalid){if(invalid instanceof FalloTecnico f&&f.status==503)throw f;throw new FalloTecnico(404);}
    }
    private boolean autorizadoCaso(Operador a,UUID caso){try{core.autorizarCaso(a,caso);return true;}catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))return false;throw e;}}
    private boolean lectorReq(Operador a,UUID req){try{core.u2().lector(a,core.u2().req(req));return true;}catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))return false;throw e;}}
    private void autorizar(Operador a,Fuente f){
        if(a==null||!cat.operador(a.id()).equals(a))throw new FalloTecnico(403);core.identidadParaAviso(a);var p=f.datos();
        if(CASO.contains(p.tipo())){
            if(p.tipo().equals("CASO_ASIGNADO")&&a.id().equals(p.saliente()))return;
            if(!autorizadoCaso(a,p.caso()))throw new FalloTecnico(404);
            var c=core.caseRow(p.caso());if(core.avisosNucleo().jefatura(a,contexto(c)))return;
            if(p.tipo().equals("CASO_ASIGNADO")){if(a.id().equals(p.entrante())&&a.id().equals(c.get("responsible_id")))return;}
            else if(core.equipo().participa(a,p.caso()))return;
        }else{
            if(lectorReq(a,p.requerimiento()))return;
            if(a.id().equals(p.registrador())){try{core.autorizarLectura(a,f.comunicacion());return;}catch(FalloTecnico denied){if(!Set.of(401,403,404,409).contains(denied.status))throw denied;}}
            for(var c:db.sql.queryForList("SELECT id FROM core_case WHERE requirement_id=? AND responsible_id=?",p.requerimiento().toString(),a.id()))if(autorizadoCaso(a,uuid(c.get("id"))))return;
        }
        throw new FalloTecnico(404);
    }
    public boolean permite(Operador a,UUID recurso,String evento,String tipo){try{var f=fuente(recurso,evento);if(!f.datos().tipo().equals(tipo))return false;autorizar(a,f);return true;}catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))return false;throw e;}}
    /** Son constancias de decisiones, no recordatorios cuya vigencia imponga otra actuación. */
    public boolean vigente(UUID recurso,String evento){try{fuente(recurso,evento);return true;}catch(FalloTecnico e){if(Set.of(401,403,404,409).contains(e.status))return false;throw e;}}
    private boolean pendiente(Datos p,Operador actor){return p.tipo().equals("CASO_ASIGNADO")&&autorizadoCaso(actor,p.caso())&&db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_current a LEFT JOIN institution_assignment_receipt r ON r.assignment_id=a.assignment_id WHERE a.case_id=? AND a.assignment_id=? AND r.assignment_id IS NULL",Long.class,p.caso().toString(),p.asignacion().toString())==1;}
    public AvisosNucleo.Vista consultar(Operador a,UUID recurso,String evento){return db.transaction(()->{
        core.bloquearAcceso(a);var f=fuente(recurso,evento);autorizar(a,f);var p=f.datos();String ruta=CASO.contains(p.tipo())?(autorizadoCaso(a,p.caso())?"caso/"+p.caso():null):(lectorReq(a,p.requerimiento())?"requerimiento/"+p.requerimiento():null);
        var r=db.sql.queryForMap("SELECT folio_prefix,folio_width,numero FROM communication_registry WHERE comunicacion_id=?",f.comunicacion().toString());String folio=r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+r.get("folio_width")+"d",n(r,"numero"));
        new PoliticaMaterias(db,clock).auditar(a,recurso.toString(),"AVISO_GESTION_CONSULTADO");return new AvisosNucleo.Vista(p.tipo(),f.fecha(),folio,null,p.destino(),ruta,pendiente(p,a));
    });}
    public boolean sustituyeAsignacionHistorica(UUID caso){return db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE resource_id=? AND action_code=?",Long.class,caso.toString(),ACTION)>0;}
    List<AvisosNucleo.Enrutamiento> enrutamiento(UUID recurso){return db.sql.queryForList("SELECT h.at_ms,w.code,s.outcome FROM institution_flow_history h JOIN workflow_event w ON w.event_key=CONCAT('AVISO_U2:',h.seq) AND w.resource_type='AVISO_GESTION' LEFT JOIN notification_source s ON s.stream_code='TAREA' AND s.event_key=CONCAT('',w.seq) WHERE h.resource_id=? AND h.action_code=? ORDER BY h.seq",recurso.toString(),ACTION).stream().map(r->new AvisosNucleo.Enrutamiento(s(r,"code"),Instant.ofEpochMilli(n(r,"at_ms")),s(r,"outcome")==null?"PENDIENTE_PROYECCION":s(r,"outcome"))).toList();}
}
