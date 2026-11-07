package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;
import java.util.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

/** BIT-01/02: una proyección común sobre las actuaciones originales, nunca una copia del relato. */
public final class BitacoraGestion {
    public record Registrar(String version,long revision,Instant realizadaEn,String descripcion,String rectifica,String motivo) {
        @Override public String toString(){return "RegistroGestion[reservado]";}
    }
    public record Entrada(String id,String version,String descripcion,Instant realizadaEn,Instant registradaEn,
                          String registrador,String ejecutor,String rectifica,String rectificadaPor,String motivo) {}
    public record Vista(String contexto,UUID recurso,long revision,boolean puedeRegistrar,
                        List<Entrada> entradas,int pagina,int tamanio,long total) {}
    private static final String ACCIONES_REQ="('REQ_ATENCION_SIN_CASO','PRIMERA_ATENCION_RESUELTA')";
    private final BaseJdbc db;
    private final NucleoInstitucional core;
    private final Clock clock;

    BitacoraGestion(BaseJdbc db,NucleoInstitucional core,Clock clock){this.db=db;this.core=core;this.clock=clock;}
    private static String s(Map<String,Object> row,String key){return row.get(key)==null?null:row.get(key).toString();}
    private static long n(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static Instant fecha(Map<String,Object> row,String key){return row.get(key)==null?null:Instant.ofEpochMilli(n(row,key));}
    private static UUID uuid(Object v){return UUID.fromString(v.toString());}
    private static void texto(String value,int max){
        try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(value==null?null:value.replace('\r',' ').replace('\n',' ').replace('\t',' '),max,true);}
        catch(IllegalArgumentException e){throw new FalloTecnico(400);}
    }
    private void validar(Registrar r){
        if(r==null||!"1.0".equals(r.version())||r.revision()<0||r.realizadaEn()==null||r.realizadaEn().isAfter(clock.instant()))throw new FalloTecnico(400,"REGISTRO_GESTION_INVALIDO");
        try{r.realizadaEn().toEpochMilli();}catch(ArithmeticException e){throw new FalloTecnico(400);}
        texto(r.descripcion(),12000);
        if(r.rectifica()==null){if(r.motivo()!=null)throw new FalloTecnico(400);}
        else texto(r.motivo(),2000);
    }
    private static void version(Map<String,Object> row,long revision){if(n(row,"revision")!=revision)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private static void pagina(int pagina,int tamanio){if(pagina<0||pagina>1000000||tamanio<1||tamanio>100)throw new FalloTecnico(400);}
    private static String reqId(long seq){return "REQ:"+seq;}
    private static long reqSeq(String id){
        try{if(id==null||!id.matches("REQ:[1-9][0-9]{0,18}"))throw new IllegalArgumentException();return Long.parseLong(id.substring(4));}
        catch(IllegalArgumentException e){throw new FalloTecnico(400);}
    }
    private static UUID caseId(String id){try{return UUID.fromString(id);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}}
    private void auditar(Operador a,UUID recurso,String codigo){new PoliticaMaterias(db,clock).auditar(a,recurso.toString(),codigo);}
    private boolean gestiona(Operador a,UUID caso){try{core.autorizarCaso(a,caso);return true;}catch(FalloTecnico e){if(e.status==404)return false;throw e;}}
    /** Rectificar acredita a quien rectifica; nunca cambia quién realizó la gestión original. */
    private String ejecutor(Map<String,Object> row,boolean req,Map<String,String> cache){
        var path=new LinkedHashSet<String>();Map<String,Object> current=row;String resolved;
        while(true){
            String key=(req?"REQ:":"CAS:")+s(current,req?"seq":"id");
            if(cache.containsKey(key)){resolved=cache.get(key);break;}
            if(!path.add(key))throw new FalloTecnico(409,"CADENA_RECTIFICACION_INVALIDA");
            if(current.get("schema_version")==null){resolved=null;break;}
            Object original=current.get(req?"rectifies_seq":"rectifies_id");
            if(original==null){resolved=s(current,"actor_id");break;}
            var parents=req
                ?db.sql.queryForList("SELECT h.*,d.schema_version,d.rectifies_seq FROM institution_flow_history h LEFT JOIN institution_req_action d ON d.history_seq=h.seq WHERE h.seq=? AND h.resource_id=? AND h.resource_kind='REQ' AND h.action_code IN "+ACCIONES_REQ,original,current.get("resource_id"))
                :db.sql.queryForList("SELECT a.*,d.schema_version,d.rectifies_id FROM core_action a LEFT JOIN institution_case_action d ON d.action_id=a.id WHERE a.id=? AND a.case_id=?",original,current.get("case_id"));
            if(parents.size()!=1)throw new FalloTecnico(409,"CADENA_RECTIFICACION_INVALIDA");
            current=parents.getFirst();
        }
        for(String key:path)cache.put(key,resolved);
        return resolved;
    }
    private Entrada entradaCaso(Map<String,Object> r,Map<String,String> cache){return new Entrada(s(r,"id"),s(r,"schema_version"),s(r,"body_text"),fecha(r,"effective_ms"),fecha(r,"at_ms"),s(r,"actor_id"),ejecutor(r,false,cache),s(r,"rectifies_id"),s(r,"corrected_by"),s(r,"correction_reason"));}
    private Entrada entradaReq(Map<String,Object> r,Map<String,String> cache){return new Entrada(reqId(n(r,"seq")),s(r,"schema_version"),s(r,"reason_text"),fecha(r,"effective_ms"),fecha(r,"at_ms"),s(r,"actor_id"),ejecutor(r,true,cache),r.get("rectifies_seq")==null?null:reqId(n(r,"rectifies_seq")),r.get("corrected_by")==null?null:reqId(n(r,"corrected_by")),s(r,"correction_reason"));}
    public Entrada constanciaCaso(Operador a,UUID caso,String accion){return db.transaction(()->{
        core.bloquearAcceso(a);core.autorizarConsultaCaso(a,caso);UUID id=caseId(accion);
        var rows=db.sql.queryForList("SELECT a.*,d.schema_version,d.effective_ms,d.rectifies_id,d.correction_reason,(SELECT x.action_id FROM institution_case_action x WHERE x.rectifies_id=a.id) AS corrected_by FROM core_action a LEFT JOIN institution_case_action d ON d.action_id=a.id WHERE a.case_id=? AND a.id=?",caso.toString(),id.toString());
        if(rows.isEmpty())throw new FalloTecnico(404);auditar(a,caso,"CONSULTAR_CONSTANCIA");return entradaCaso(rows.getFirst(),new HashMap<>());
    });}
    public Entrada constanciaReq(Operador a,UUID req,String accion){return db.transaction(()->{
        var flow=core.u2();flow.guard(a);flow.lector(a,flow.req(req));long seq=reqSeq(accion);
        var rows=db.sql.queryForList("SELECT h.*,d.schema_version,d.effective_ms,d.rectifies_seq,d.correction_reason,(SELECT x.history_seq FROM institution_req_action x WHERE x.rectifies_seq=h.seq) AS corrected_by FROM institution_flow_history h LEFT JOIN institution_req_action d ON d.history_seq=h.seq WHERE h.resource_id=? AND h.seq=? AND h.resource_kind='REQ' AND h.action_code IN "+ACCIONES_REQ,req.toString(),seq);
        if(rows.isEmpty())throw new FalloTecnico(404);auditar(a,req,"CONSULTAR_CONSTANCIA");return entradaReq(rows.getFirst(),new HashMap<>());
    });}

    public Vista caso(Operador a,UUID caso,int pagina,int tamanio){
        pagina(pagina,tamanio);
        return db.transaction(()->{
            core.bloquearAcceso(a);core.autorizarConsultaCaso(a,caso);var c=core.caseRow(caso);
            long total=db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE case_id=?",Long.class,caso.toString());
            var rows=db.sql.queryForList("SELECT a.*,d.schema_version,d.effective_ms,d.rectifies_id,d.correction_reason,(SELECT x.action_id FROM institution_case_action x WHERE x.rectifies_id=a.id) AS corrected_by FROM core_action a LEFT JOIN institution_case_action d ON d.action_id=a.id WHERE a.case_id=? ORDER BY COALESCE(d.effective_ms,a.at_ms),a.at_ms,a.id LIMIT ? OFFSET ?",caso.toString(),tamanio,(long)pagina*tamanio);
            var cache=new HashMap<String,String>();var entries=rows.stream().map(r->entradaCaso(r,cache)).toList();
            auditar(a,caso,"CONSULTAR_BITACORA_CASO");
            return new Vista("CASO",caso,n(c,"revision"),gestiona(a,caso)&&"EN_GESTION".equals(c.get("state")),entries,pagina,tamanio,total);
        });
    }
    public Vista requerimiento(Operador a,UUID req,int pagina,int tamanio){
        pagina(pagina,tamanio);
        return db.transaction(()->{
            var flow=core.u2();flow.guard(a);var row=flow.req(req);flow.lector(a,row);
            long total=db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE resource_id=? AND resource_kind='REQ' AND action_code IN "+ACCIONES_REQ,Long.class,req.toString());
            var rows=db.sql.queryForList("SELECT h.*,d.schema_version,d.effective_ms,d.rectifies_seq,d.correction_reason,(SELECT x.history_seq FROM institution_req_action x WHERE x.rectifies_seq=h.seq) AS corrected_by FROM institution_flow_history h LEFT JOIN institution_req_action d ON d.history_seq=h.seq WHERE h.resource_id=? AND h.resource_kind='REQ' AND h.action_code IN "+ACCIONES_REQ+" ORDER BY COALESCE(d.effective_ms,h.at_ms),h.at_ms,h.seq LIMIT ? OFFSET ?",req.toString(),tamanio,(long)pagina*tamanio);
            var cache=new HashMap<String,String>();var entries=rows.stream().map(r->entradaReq(r,cache)).toList();
            boolean sinCasos=db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,req.toString())==0;
            auditar(a,req,"CONSULTAR_BITACORA_REQ");
            return new Vista("REQ",req,n(row,"revision"),sinCasos&&"ACTIVO".equals(row.get("state")),entries,pagina,tamanio,total);
        });
    }
    public Resultado registrarCaso(Operador a,UUID key,UUID caso,Registrar r){
        validar(r);
        return core.execute(key,a,"BITACORA_CASO_1",List.of(caso,r),()->{
            core.u2().guard(a);var c=core.caseRow(caso);core.lock(uuid(c.get("communication_id")));core.autorizarCaso(a,caso);c=core.caseRow(caso);version(c,r.revision());
            if(!"EN_GESTION".equals(c.get("state")))throw new FalloTecnico(409,"GESTION_NO_ABIERTA");
            UUID original=null;
            if(r.rectifica()!=null){
                original=caseId(r.rectifica());
                if(db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE id=? AND case_id=?",Long.class,original.toString(),caso.toString())!=1)throw new FalloTecnico(404);
                if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_case_action WHERE rectifies_id=?",Long.class,original.toString())!=0)throw new FalloTecnico(409,"RECTIFICACION_OBSOLETA");
            }
            UUID accion=UUID.randomUUID();long recorded=clock.millis();
            var result=core.insertarActuacion(a,caso,new Decision(r.revision(),r.descripcion()),accion,recorded,"ACTUACION_REGISTRADA");
            db.sql.update("INSERT INTO institution_case_action(action_id,case_id,schema_version,effective_ms,rectifies_id,correction_reason) VALUES(?,?,'1.0',?,?,?)",accion.toString(),caso.toString(),r.realizadaEn().toEpochMilli(),original==null?null:original.toString(),r.motivo());
            FuentesRegistro.registrar(db,"CASO:"+accion,uuid(c.get("communication_id")),"CASO",caso,"REGISTRO_GESTION","GESTION_CASO",c.get("unit_code").toString(),c.get("site_id").toString(),new PoliticaAreas(db,clock).area(c),r.realizadaEn().toEpochMilli(),recorded,recorded,a.id(),original==null?null:"CASO:"+original);
            if(original!=null)core.event(uuid(c.get("communication_id")),"CASO",caso,a,"ACTUACION_RECTIFICADA");
            return result;
        });
    }
    public Resultado registrarRequerimiento(Operador a,UUID key,UUID req,Registrar r){
        validar(r);
        return core.execute(key,a,"BITACORA_REQ_1",List.of(req,r),()->{
            var flow=core.u2();flow.guard(a);var row=flow.req(req);UUID com=uuid(row.get("communication_id"));core.lock(com);row=flow.req(req);flow.lector(a,row);version(row,r.revision());flow.abierto(row);
            if(db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE requirement_id=?",Long.class,req.toString())!=0)throw new FalloTecnico(409,"ACTUACION_REQUIERE_CASO");
            Long original=null;
            if(r.rectifica()!=null){
                original=reqSeq(r.rectifica());
                if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE seq=? AND resource_id=? AND resource_kind='REQ' AND action_code IN "+ACCIONES_REQ,Long.class,original,req.toString())!=1)throw new FalloTecnico(404);
                if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_action WHERE rectifies_seq=?",Long.class,original)!=0)throw new FalloTecnico(409,"RECTIFICACION_OBSOLETA");
            }
            db.sql.update("UPDATE core_requirement SET revision=revision+1 WHERE id=?",req.toString());
            flow.historia(com,req,"REQ",r.revision()+1,a,"REQ_ATENCION_SIN_CASO",r.descripcion(),Map.of("casos",0,"versionGestion","1.0"));
            long seq=db.sql.queryForObject("SELECT seq FROM institution_flow_history WHERE resource_id=? AND revision=? AND action_code='REQ_ATENCION_SIN_CASO'",Long.class,req.toString(),r.revision()+1);
            db.sql.update("INSERT INTO institution_req_action(history_seq,requirement_id,schema_version,effective_ms,rectifies_seq,correction_reason) VALUES(?,?,'1.0',?,?,?)",seq,req.toString(),r.realizadaEn().toEpochMilli(),original,r.motivo());
            long registrada=db.sql.queryForObject("SELECT at_ms FROM institution_flow_history WHERE seq=?",Long.class,seq);
            FuentesRegistro.registrar(db,"REQ:"+seq,com,"REQ",req,"REGISTRO_GESTION","GESTION_REQ",row.get("unit_code").toString(),row.get("site_id").toString(),(String)row.get("area_id"),r.realizadaEn().toEpochMilli(),registrada,registrada,a.id(),original==null?null:"REQ:"+original);
            flow.bumpCom(com);
            if(original!=null)core.event(com,"REQ",req,a,"ACTUACION_RECTIFICADA");
            return new Resultado(req,r.revision()+1);
        });
    }
}
