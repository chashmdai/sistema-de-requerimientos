package cl.nexbyte.siger.institucional.solicitudes;

import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.Resultado;
import cl.nexbyte.siger.institucional.notificaciones.RecursosNotificacion;
import cl.nexbyte.siger.institucional.robustez.*;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.util.*;
import java.security.*;

/** Preparación personal sobre un ingreso autorizado. No concede facultades de decisión institucional. */
public final class SolicitudesInstitucionales {
    private final BaseJdbc db;
    private final RecursosNotificacion permisos;
    private final CatalogoLaboratorio catalogo;
    private final JsonMapper json;
    private final Clock clock;
    public SolicitudesInstitucionales(BaseJdbc db,RecursosNotificacion permisos,CatalogoLaboratorio catalogo,JsonMapper json,Clock clock){
        this.db=db;this.permisos=permisos;this.catalogo=catalogo;this.json=json;this.clock=clock;
    }
    public record Edicion(long revision,UUID comunicacion,String titulo,String descripcion){@Override public String toString(){return "Edicion[privada]";}}
    public record Preparacion(long revision,String tipo,String texto,String unidadDestino,String sedeDestino){@Override public String toString(){return "Preparacion[privada]";}}
    public record Seguimiento(long revision,String texto){@Override public String toString(){return "Seguimiento[privado]";}}
    public record Solicitud(UUID id,UUID comunicacion,String folio,String creador,String responsable,String unidad,String sede,String titulo,String descripcion,String estado,long revision,Instant creada,Instant actualizada,String tipoPreparado,String textoPreparado,String unidadDestino,String sedeDestino){@Override public String toString(){return "Solicitud[privada]";}}
    public record Historia(long secuencia,String autor,Instant fecha,long revision,String evento,Solicitud datos){}
    public record Nota(UUID id,String autor,Instant fecha,String texto){@Override public String toString(){return "Nota[privada]";}}
    public record Relaciones(UUID comunicacion,List<UUID> requerimientos,List<UUID> casos){}
    private static UUID id(Object v){return UUID.fromString(v.toString());}
    private static long num(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static void texto(String v,int max,boolean required){if(v==null||v.length()>max||required&&v.isBlank()||v.chars().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw new FalloTecnico(400);}
    private void validar(Edicion e){if(e==null||e.comunicacion()==null)throw new FalloTecnico(400);texto(e.titulo(),180,true);texto(e.descripcion(),2000,false);}
    private void lock(Operador a){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);permisos.actor(a);}
    private void origen(Operador a,UUID com){permisos.recurso(a,"COMUNICACION",com);db.sql.queryForObject("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",String.class,com.toString());}
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException();}}
    private Resultado anterior(Operador a,UUID key,String op,String hash){
        if(key==null)throw new FalloTecnico(400);
        var rows=db.sql.queryForList("SELECT * FROM request_command WHERE id=?",key.toString());if(rows.isEmpty())return null;
        var r=rows.getFirst();if(!a.id().equals(r.get("actor_id"))||!op.equals(r.get("action_code"))||!hash.equals(r.get("request_hash")))throw new FalloTecnico(409);
        UUID id=id(r.get("request_id"));leer(a,id);return new Resultado(id,num(r,"result_revision"));
    }
    private Solicitud vista(Map<String,Object> r){
        var registro=db.sql.queryForMap("SELECT numero,folio_prefix,folio_width FROM communication_registry WHERE comunicacion_id=?",r.get("communication_id"));
        String folio=registro.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+num(registro,"folio_width")+"d",num(registro,"numero"));
        return new Solicitud(id(r.get("id")),id(r.get("communication_id")),folio,r.get("creator_id").toString(),r.get("responsible_id").toString(),(String)r.get("unit_code"),(String)r.get("site_id"),r.get("title").toString(),r.get("description_text").toString(),r.get("state").toString(),num(r,"revision"),Instant.ofEpochMilli(num(r,"created_ms")),Instant.ofEpochMilli(num(r,"updated_ms")),(String)r.get("prepared_kind"),(String)r.get("prepared_text"),(String)r.get("target_unit"),(String)r.get("target_site"));
    }
    public Solicitud leer(Operador a,UUID id){return vista(permisos.solicitud(a,id));}
    public List<Solicitud> listar(Operador a,UUID comunicacion){
        permisos.actor(a);if(comunicacion!=null)permisos.recurso(a,"COMUNICACION",comunicacion);
        var params=new ArrayList<Object>(List.of(a.id(),a.id()));if(comunicacion!=null)params.add(comunicacion.toString());
        var rows=db.sql.queryForList("SELECT * FROM institution_request WHERE creator_id=? AND responsible_id=?"+(comunicacion==null?"":" AND communication_id=?")+" ORDER BY updated_ms DESC,id LIMIT 500",params.toArray());
        var result=new ArrayList<Solicitud>();for(var row:rows)try{permisos.solicitud(a,id(row.get("id")));result.add(vista(row));}catch(FalloTecnico denied){if(!Set.of(401,403,404).contains(denied.status))throw denied;}
        return result;
    }
    private Resultado registrar(Operador a,UUID key,String op,String hash,UUID id,String evento){
        var view=leer(a,id);
        db.sql.update("INSERT INTO request_history(request_id,actor_id,at_ms,revision,event_code,snapshot_doc) VALUES(?,?,?,?,?,?)",id.toString(),a.id(),clock.millis(),view.revision(),evento,json.writeValueAsString(view));
        db.sql.update("INSERT INTO workflow_event(event_key,code,resource_type,resource_id,at_ms) VALUES(?,?,'SOLICITUD',?,?)",id+":"+view.revision()+":"+evento,evento,id.toString(),clock.millis());
        db.sql.update("INSERT INTO request_command VALUES(?,?,?,?,?,?)",key.toString(),a.id(),op,hash,id.toString(),view.revision());
        return new Resultado(id,view.revision());
    }
    public Resultado crear(Operador a,UUID key,Edicion e){
        permisos.actor(a);validar(e);if(e.revision()!=0)throw new FalloTecnico(400);String hash=hash(e);
        return db.transaction(()->{lock(a);var prev=anterior(a,key,"CREAR",hash);if(prev!=null)return prev;origen(a,e.comunicacion());UUID id=UUID.randomUUID();
            String unidad=a.unidad(),sede=a.sede();if(unidad==null||sede==null){unidad=null;sede=null;}
            db.sql.update("INSERT INTO institution_request(id,communication_id,creator_id,responsible_id,unit_code,site_id,title,description_text,state,revision,created_ms,updated_ms) VALUES(?,?,?,?,?,?,?,?,'EN_PREPARACION',0,?,?)",id.toString(),e.comunicacion().toString(),a.id(),a.id(),unidad,sede,e.titulo(),e.descripcion(),clock.millis(),clock.millis());
            return registrar(a,key,"CREAR",hash,id,"SOLICITUD_CREADA");});
    }
    private Solicitud vigente(Operador a,UUID id,long revision){var t=leer(a,id);origen(a,t.comunicacion());db.sql.queryForObject("SELECT id FROM institution_request WHERE id=? FOR UPDATE",String.class,id.toString());if(t.revision()!=revision)throw new FalloTecnico(409,"VERSION_OBSOLETA");return t;}
    public Resultado editar(Operador a,UUID key,UUID id,Edicion e){
        permisos.actor(a);validar(e);String hash=hash(List.of(id,e));return db.transaction(()->{lock(a);var prev=anterior(a,key,"EDITAR",hash);if(prev!=null)return prev;var t=vigente(a,id,e.revision());if(!t.comunicacion().equals(e.comunicacion()))throw new FalloTecnico(409,"ORIGEN_INMUTABLE");
            db.sql.update("UPDATE institution_request SET title=?,description_text=?,revision=revision+1,updated_ms=? WHERE id=?",e.titulo(),e.descripcion(),clock.millis(),id.toString());return registrar(a,key,"EDITAR",hash,id,"SOLICITUD_MODIFICADA");});
    }
    public Resultado preparar(Operador a,UUID key,UUID id,Preparacion e){
        permisos.actor(a);if(e==null||!Set.of("RESPUESTA","CANALIZACION","ESCALAMIENTO").contains(Objects.toString(e.tipo(),"")))throw new FalloTecnico(400);texto(e.texto(),2000,true);
        if(e.tipo().equals("RESPUESTA")){if(e.unidadDestino()!=null||e.sedeDestino()!=null)throw new FalloTecnico(400);}else if(catalogo.presencias().stream().noneMatch(p->Objects.equals(p.unidad(),e.unidadDestino())&&Objects.equals(p.sede(),e.sedeDestino())))throw new FalloTecnico(400);
        String hash=hash(List.of(id,e));return db.transaction(()->{lock(a);var prev=anterior(a,key,"PREPARAR",hash);if(prev!=null)return prev;vigente(a,id,e.revision());
            if(!e.tipo().equals("RESPUESTA")&&catalogo.presencias().stream().noneMatch(p->Objects.equals(p.unidad(),e.unidadDestino())&&Objects.equals(p.sede(),e.sedeDestino())))throw new FalloTecnico(400);
            db.sql.update("UPDATE institution_request SET prepared_kind=?,prepared_text=?,target_unit=?,target_site=?,state='PREPARADA',revision=revision+1,updated_ms=? WHERE id=?",e.tipo(),e.texto(),e.unidadDestino(),e.sedeDestino(),clock.millis(),id.toString());return registrar(a,key,"PREPARAR",hash,id,"SOLICITUD_PREPARADA");});
    }
    public Resultado seguimiento(Operador a,UUID key,UUID id,Seguimiento e){
        permisos.actor(a);if(e==null)throw new FalloTecnico(400);texto(e.texto(),2000,true);String hash=hash(List.of(id,e));return db.transaction(()->{lock(a);var prev=anterior(a,key,"SEGUIMIENTO",hash);if(prev!=null)return prev;vigente(a,id,e.revision());
            db.sql.update("INSERT INTO request_followup VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),id.toString(),a.id(),clock.millis(),e.texto());db.sql.update("UPDATE institution_request SET revision=revision+1,updated_ms=? WHERE id=?",clock.millis(),id.toString());return registrar(a,key,"SEGUIMIENTO",hash,id,"SOLICITUD_SEGUIMIENTO");});
    }
    public List<Nota> seguimientos(Operador a,UUID id){leer(a,id);return db.sql.queryForList("SELECT * FROM request_followup WHERE request_id=? ORDER BY at_ms DESC,id LIMIT 100",id.toString()).stream().map(r->new Nota(id(r.get("id")),r.get("actor_id").toString(),Instant.ofEpochMilli(num(r,"at_ms")),r.get("body_text").toString())).toList();}
    public List<Historia> historial(Operador a,UUID id){leer(a,id);return db.sql.queryForList("SELECT * FROM request_history WHERE request_id=? ORDER BY seq DESC LIMIT 100",id.toString()).stream().map(r->new Historia(num(r,"seq"),r.get("actor_id").toString(),Instant.ofEpochMilli(num(r,"at_ms")),num(r,"revision"),r.get("event_code").toString(),json.readValue(r.get("snapshot_doc").toString(),Solicitud.class))).toList();}
    public Relaciones relaciones(Operador a,UUID id){var t=leer(a,id);var req=db.sql.queryForList("SELECT id FROM core_requirement WHERE communication_id=?",t.comunicacion().toString()).stream().map(r->id(r.get("id"))).toList();var cases=new ArrayList<UUID>();for(var r:db.sql.queryForList("SELECT c.id FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE d.communication_id=?",t.comunicacion().toString()))try{UUID c=id(r.get("id"));permisos.recurso(a,"CASO",c);cases.add(c);}catch(FalloTecnico denied){if(!Set.of(403,404).contains(denied.status))throw denied;}return new Relaciones(t.comunicacion(),req,cases);}
    public void decisionPendiente(Operador a,UUID id){leer(a,id);throw new FalloTecnico(409,"DEFINICION_INSTITUCIONAL_PENDIENTE");}
}
