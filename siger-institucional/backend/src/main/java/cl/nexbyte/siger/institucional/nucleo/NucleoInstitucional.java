package cl.nexbyte.siger.institucional.nucleo;

import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.*;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.contrato.DeclaracionNna;
import java.security.*;import java.nio.charset.StandardCharsets;import java.sql.Statement;import java.time.*;import java.util.*;import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;import org.springframework.jdbc.support.GeneratedKeyHolder;
import tools.jackson.databind.json.JsonMapper;

/** Gestión separada del registro firmado. Cada mutación serializa la Comunicación raíz y confirma su auditoría en la misma transacción. */
public final class NucleoInstitucional {
    private final BaseJdbc db;private final CatalogoLaboratorio catalogo;private final RecepcionInstitucionalJdbc recepcion;
    private final org.springframework.transaction.support.TransactionTemplate lectura;private final JsonMapper mapper;private final Clock clock;private final String prefix;private final int width;
    public NucleoInstitucional(BaseJdbc db,CatalogoLaboratorio catalogo,RecepcionInstitucionalJdbc recepcion,JsonMapper mapper,Clock clock){this(db,catalogo,recepcion,mapper,clock,"COM",6);}
    public NucleoInstitucional(BaseJdbc db,CatalogoLaboratorio catalogo,RecepcionInstitucionalJdbc recepcion,JsonMapper mapper,Clock clock,String prefix,int width){
        if(prefix==null||!prefix.matches("[A-Z][A-Z0-9]{1,15}")||width<1||width>18)throw new IllegalArgumentException("Folio inválido");
        this.lectura=new org.springframework.transaction.support.TransactionTemplate(db.tx.getTransactionManager());this.lectura.setIsolationLevelName("ISOLATION_REPEATABLE_READ");this.lectura.setReadOnly(true);this.lectura.setTimeout(5);this.db=db;this.catalogo=catalogo;this.recepcion=recepcion;this.mapper=mapper;this.clock=clock;this.prefix=prefix;this.width=width;
    }
    private long now(){return clock.millis();}
    private static String id(UUID value){if(value==null)throw new FalloTecnico(400);return value.toString();}
    private static UUID uuid(Object v){return v==null?null:UUID.fromString(v.toString());}
    private static long number(Map<String,Object> r,String k){return r.get(k)==null?0:((Number)r.get(k)).longValue();}
    private static String str(Map<String,Object> r,String k){return r.get(k)==null?null:r.get(k).toString();}
    private static Instant date(Map<String,Object> r,String k){return r.get(k)==null?null:Instant.ofEpochMilli(number(r,k));}
    private static void text(String value,int max){DeclaracionNna.texto(value,max,true);}
    private Operador actor(Operador a){if(a==null)throw new FalloTecnico(401);var original=catalogo.operador(a.id());if(!original.equals(a))throw new FalloTecnico(403);return original;}
    private static void distributor(Operador a){if(!a.distribuidor())throw new FalloTecnico(403);}
    private static void revision(Map<String,Object> row,long expected){if(expected<0||number(row,"revision")!=expected)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(value)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private Resultado previous(UUID key,Operador a,String action,String digest){
        var rows=db.sql.queryForList("SELECT actor_id,action_code,request_hash,result_id,result_revision FROM core_command WHERE id=?",id(key));
        if(rows.isEmpty())return null;var row=rows.getFirst();
        if(!a.id().equals(row.get("actor_id"))||!action.equals(row.get("action_code"))||!digest.equals(row.get("request_hash")))throw new FalloTecnico(409);
        return new Resultado(uuid(row.get("result_id")),number(row,"result_revision"));
    }
    private Resultado execute(UUID key,Operador operator,String action,Object payload,Supplier<Resultado> work){
        var a=actor(operator);var digest=hash(List.of("NUCLEO_CMD_1",action,a.id(),payload));
        var old=previous(key,a,action,digest);if(old!=null)return old;
        try{return db.transaction(()->{
            if(catalogo.individual())db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);actor(a);var same=previous(key,a,action,digest);if(same!=null)return same;
            var result=work.get();db.sql.update("INSERT INTO core_command VALUES(?,?,?,?,?,?,?)",id(key),a.id(),action,digest,id(result.id()),result.revision(),now());return result;
        });}catch(DuplicateKeyException error){var winner=previous(key,a,action,digest);if(winner==null)throw new FalloTecnico(409);return winner;}catch(FalloTecnico error){if(error.status==409){var winner=previous(key,a,action,digest);if(winner!=null)return winner;}throw error;}
    }
    private Map<String,Object> registry(UUID communication,boolean lock){
        var rows=db.sql.queryForList("SELECT * FROM communication_registry WHERE comunicacion_id=?"+(lock?" FOR UPDATE":""),id(communication));
        if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private Map<String,Object> lock(UUID communication){
        registry(communication,true);
        if(db.sql.queryForList("SELECT id FROM core_communication WHERE id=?",id(communication)).isEmpty())db.sql.update("INSERT INTO core_communication(id,state,revision) VALUES(?,'ABIERTA',0)",id(communication));
        return db.sql.queryForMap("SELECT * FROM core_communication WHERE id=?",id(communication));
    }
    private void event(UUID communication,String kind,UUID resource,Operador a,String code){db.sql.update("INSERT INTO core_event(communication_id,resource_kind,resource_id,actor_id,at_ms,code) VALUES(?,?,?,?,?,?)",id(communication),kind,id(resource),a.id(),now(),code);}
    private void bump(UUID communication){db.sql.update("UPDATE core_communication SET revision=revision+1 WHERE id=?",id(communication));}
    private Resultado result(UUID resource,UUID communication){return new Resultado(resource,db.sql.queryForObject("SELECT revision FROM core_communication WHERE id=?",Long.class,id(communication)));}
    private Map<String,Object> destination(UUID destination){
        var rows=db.sql.queryForList("SELECT * FROM core_destination WHERE id=?",id(destination));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private Map<String,Object> caseRow(UUID caseId){
        var rows=db.sql.queryForList("SELECT c.*,d.unit_code,d.site_id FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE c.id=?",id(caseId));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private boolean canCase(Operador a,Map<String,Object> row){return a.jefeEn(str(row,"unit_code"),str(row,"site_id"))||a.profesionalEn(str(row,"unit_code"),str(row,"site_id"))&&a.id().equals(str(row,"responsible_id"));}
    private void head(Operador a,Map<String,Object> row){if(!a.jefeEn(str(row,"unit_code"),str(row,"site_id")))throw new FalloTecnico(404);}
    /** Misma facultad/estado que registrar actuación; el llamador mantiene la transacción y su auditoría documental. */
    public Caso autorizarGestionDocumental(Operador a,UUID caseId){
        if(catalogo.individual())db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);actor(a);var first=caseRow(caseId);lock(uuid(first.get("communication_id")));var current=caseRow(caseId);
        if(!canCase(a,current))throw new FalloTecnico(404);
        if(!"EN_GESTION".equals(current.get("state")))throw new FalloTecnico(409);
        return caso(a,caseId);
    }
    public void bloquearAcceso(Operador a){if(catalogo.individual())db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);actor(a);}
    /** Autorización sin abrir otra TransactionTemplate: utilizable dentro de un outbox o de otro recurso. */
    public void autorizarCaso(Operador a,UUID id){actor(a);if(!canCase(a,caseRow(id)))throw new FalloTecnico(404);}
    public Caso contextoCaso(Operador a,UUID id){actor(a);var c=caseRow(id);if(!canCase(a,c))throw new FalloTecnico(404);return caseView(c,false);}
    public void autorizarAntecedente(Operador a,UUID id){actor(a);linkReader(a,linkRow(id));}
    public void autorizarDerivacion(Operador a,UUID id){actor(a);transferReader(a,transferRow(id));}
    public void autorizarReapertura(Operador a,UUID id){actor(a);var r=reopenRow(id);if(!canCase(a,caseRow(uuid(r.get("case_id")))))throw new FalloTecnico(404);}
    public void autorizarLectura(Operador a,UUID communication){actor(a);readable(a,communication);}
    private void readable(Operador a,UUID communication){
        registry(communication,false);if(a.distribuidor())return;
        if(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry r WHERE r.comunicacion_id=? AND "+communicationScope(a),Long.class,id(communication))==0)throw new FalloTecnico(404);
    }
    private static String literal(String v){if(v==null)return "NULL";if(!v.matches("[A-Za-z0-9_.-]{1,64}"))throw new FalloTecnico(403);return "'"+v+"'";}
    private String grantSql(Operador a,String unit,String site,boolean head){
        var terms=new ArrayList<String>();
        for(var x:a.accesos())if(head?x.jefe():x.rol().equals("PROFESIONAL")){
            terms.add(x.alcance().equals("INSTITUCION")?"1=1":"("+site+"="+literal(x.sede())+(x.unidad()==null?"":" AND "+unit+"="+literal(x.unidad()))+")");
        }
        return "("+String.join(" OR ",terms.isEmpty()?List.of("1=0"):terms)+")";
    }
    private String caseScopeGestion(Operador a){return "("+grantSql(a,"d.unit_code","d.site_id",true)+" OR (c.responsible_id="+literal(a.id())+" AND "+grantSql(a,"d.unit_code","d.site_id",false)+"))";}
    private String caseScope(Operador a){return a.distribuidor()?"1=1":caseScopeGestion(a);}
    private ArrayList<Object> caseArgs(Operador a){return new ArrayList<>();}
    private String communicationScope(Operador a){
        if(a.distribuidor())return "1=1";
        String scope="EXISTS(SELECT 1 FROM core_destination d WHERE d.communication_id=r.comunicacion_id AND "+grantSql(a,"d.unit_code","d.site_id",true)+") OR EXISTS(SELECT 1 FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE c.communication_id=r.comunicacion_id AND c.responsible_id="+literal(a.id())+" AND "+grantSql(a,"d.unit_code","d.site_id",false)+")";
        if(catalogo.individual()){
            scope+=" OR EXISTS(SELECT 1 FROM core_manual m WHERE m.communication_id=r.comunicacion_id AND m.actor_id="+literal(a.id())+")";
            for(var x:a.accesos())if(x.rol().equals("COORDINADOR_REGIONAL"))scope+=" OR EXISTS(SELECT 1 FROM core_communication o WHERE o.id=r.comunicacion_id AND o.site_id="+literal(x.sede())+")";
        }
        return "("+scope+")";
    }
    private void distributionAllowed(Operador a,UUID com){
        if(a.distribuidor())return;var rows=db.sql.queryForList("SELECT site_id FROM core_communication WHERE id=?",id(com));
        if(rows.isEmpty()||!a.distribuyeSede(str(rows.getFirst(),"site_id")))throw new FalloTecnico(403);
    }
    private void openCommunication(UUID communication,Map<String,Object> row){
        if(!"ABIERTA".equals(row.get("state")))throw new FalloTecnico(409);
        var req=db.sql.queryForList("SELECT state FROM core_requirement WHERE communication_id=?",id(communication));
        if(!req.isEmpty()&&"TERMINADO".equals(req.getFirst().get("state")))throw new FalloTecnico(409);
        long destinations=db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Long.class,id(communication));
        if(destinations>0&&db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND state<>'RESUELTO'",Long.class,id(communication))==0)throw new FalloTecnico(409);
    }
    private void reevaluate(UUID communication,Operador a){
        var reqs=db.sql.queryForList("SELECT id,state FROM core_requirement WHERE communication_id=?",id(communication));if(reqs.isEmpty())return;
        long openCases=db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE communication_id=? AND state<>'CERRADO'",Long.class,id(communication));
        long pending=db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND state<>'RESUELTO'",Long.class,id(communication));
        var row=reqs.getFirst();String state=openCases==0&&pending==0?"TERMINADO":"ACTIVO";
        if(!state.equals(row.get("state"))){db.sql.update("UPDATE core_requirement SET state=?,revision=revision+1,ended_ms=? WHERE id=?",state,state.equals("TERMINADO")?now():null,row.get("id"));event(communication,"REQUERIMIENTO",uuid(row.get("id")),a,state.equals("TERMINADO")?"REQUERIMIENTO_TERMINADO":"REQUERIMIENTO_ACTIVO");}
    }
    private String contentHash(UUID id,long registered,Manual m){if(m.regionAsunto()!=null)return hash(Arrays.asList("MANUAL_2",id.toString(),registered,m.texto(),m.canalDeclarado(),m.recibidaEnDeclarada().toEpochMilli(),m.sede(),m.regionAsunto()));return hash(Arrays.asList(m.sede()==null?"MANUAL_SIN_SEDE_1":"MANUAL_1",id.toString(),registered,m.texto(),m.canalDeclarado(),m.recibidaEnDeclarada().toEpochMilli(),m.sede()));}
    private record ManualHistorico(String texto,String canalDeclarado,Instant recibidaEnDeclarada,String sede,String respuestaYaEntregada){}
    public Resultado registrarManual(Operador a,UUID key,Manual request){
        actor(a);if(!catalogo.individual())distributor(a);if(request==null)throw new FalloTecnico(400);DeclaracionNna.relato(request.texto());text(request.canalDeclarado(),100);
        if(request.recibidaEnDeclarada()==null||request.recibidaEnDeclarada().isAfter(clock.instant())||(request.sede()!=null&&catalogo.sedes().stream().noneMatch(s->s.id().equals(request.sede()))))throw new FalloTecnico(400);
        if(catalogo.individual()&&!Objects.equals(a.sede(),request.sede()))throw new FalloTecnico(403);
        if(request.regionAsunto()!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM org_region WHERE id=?",Integer.class,request.regionAsunto())!=1)throw new FalloTecnico(400);
        if(request.respuestaYaEntregada()!=null)DeclaracionNna.relato(request.respuestaYaEntregada());
        return execute(key,a,"REGISTRO_MANUAL",request.regionAsunto()==null?new ManualHistorico(request.texto(),request.canalDeclarado(),request.recibidaEnDeclarada(),request.sede(),request.respuestaYaEntregada()):request,()->{
            var cid=UUID.randomUUID();long registered=now();var generated=new GeneratedKeyHolder();
            db.sql.update(connection->{var s=connection.prepareStatement("INSERT INTO communication_registry(comunicacion_id,source,operacion_id,registrada_ms,canal,content_hash,provenance_hash,folio_prefix,folio_width) VALUES(?,'REGISTRO_FUNCIONARIO',NULL,?,'REGISTRO_FUNCIONARIO',?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
                s.setString(1,id(cid));s.setLong(2,registered);s.setString(3,contentHash(cid,registered,request));s.setString(4,hash(List.of("REGISTRO_FUNCIONARIO_1",a.id(),registered)));s.setString(5,prefix);s.setInt(6,width);return s;},generated);
            if(generated.getKey()==null)throw new FalloTecnico(503);
            db.sql.update("INSERT INTO core_communication(id,state,revision,site_id) VALUES(?,'ABIERTA',0,?)",id(cid),request.sede());
            db.sql.update("INSERT INTO core_manual VALUES(?,?,?,?,?)",id(cid),request.recibidaEnDeclarada().toEpochMilli(),request.canalDeclarado(),request.texto(),a.id());
            db.sql.update("INSERT INTO communication_origin VALUES(?,'REGISTRO_FUNCIONARIO',?,?)",id(cid),request.sede(),request.regionAsunto());
            event(cid,"COMUNICACION",cid,a,"COMUNICACION_MANUAL_REGISTRADA");
            if(request.respuestaYaEntregada()!=null){db.sql.update("UPDATE core_communication SET state='RESPONDIDA',decision_text=?,decided_by=?,decided_ms=?,revision=1 WHERE id=?",request.respuestaYaEntregada(),a.id(),registered,id(cid));event(cid,"COMUNICACION",cid,a,"RESPUESTA_YA_ENTREGADA_REGISTRADA");}
            return result(cid,cid);
        });
    }
    public Resultado decidirComunicacion(Operador a,UUID key,UUID communication,Decision request,boolean discard){
        actor(a);distributor(a);text(request.texto(),2000);
        return execute(key,a,discard?"DESCARTAR_COMUNICACION":"RESPONDER_COMUNICACION",List.of(communication,request),()->{
            var row=lock(communication);revision(row,request.revision());openCommunication(communication,row);
            if(db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Long.class,id(communication))>0)throw new FalloTecnico(409);
            db.sql.update("UPDATE core_communication SET state=?,decision_text=?,decided_by=?,decided_ms=?,revision=revision+1 WHERE id=?",discard?"DESCARTADA":"RESPONDIDA",request.texto(),a.id(),now(),id(communication));
            event(communication,"COMUNICACION",communication,a,discard?"COMUNICACION_DESCARTADA":"RESPUESTA_DIRECTA_REGISTRADA");return result(communication,communication);
        });
    }
    public Resultado distribuir(Operador a,UUID key,UUID communication,Distribucion request){
        actor(a);distributionAllowed(a,communication);if(request.destinos()==null||request.destinos().isEmpty()||request.destinos().size()>10)throw new FalloTecnico(400);
        for(var d:request.destinos()){
            if(d==null||catalogo.presencias().stream().noneMatch(p->p.unidad().equals(d.unidad())&&p.sede().equals(d.sede())))throw new FalloTecnico(400);
            var evaluator=catalogo.operador(d.evaluador());if(!evaluator.jefeEn(d.unidad(),d.sede()))throw new FalloTecnico(400);
        }
        return execute(key,a,"DISTRIBUIR",List.of(communication,request),()->{
            var row=lock(communication);distributionAllowed(a,communication);revision(row,request.revision());openCommunication(communication,row);
            var pairs=new HashSet<String>();for(var d:request.destinos())if(catalogo.individual()&&(!pairs.add(d.unidad()+"|"+d.sede())||db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND unit_code=? AND site_id=?",Integer.class,id(communication),d.unidad(),d.sede())>0))throw new FalloTecnico(409);
            for(var d:request.destinos()){var did=UUID.randomUUID();db.sql.update("INSERT INTO core_destination(id,communication_id,unit_code,site_id,evaluator_id,state,revision,created_ms,created_by) VALUES(?,?,?,?,?,'EVALUANDO',0,?,?)",id(did),id(communication),d.unidad(),d.sede(),d.evaluador(),now(),a.id());db.sql.update("INSERT INTO distribution_event VALUES(?,?,?,?,?,?,?)",id(did),id(communication),a.id(),row.get("site_id"),d.unidad(),d.sede(),now());event(communication,"DESTINO",did,a,"DESTINO_DISTRIBUIDO");}
            bump(communication);return result(communication,communication);
        });
    }
    public Resultado abrirCaso(Operador a,UUID key,UUID destination,Asignacion request){
        actor(a);var initial=destination(destination);head(a,initial);var communication=uuid(initial.get("communication_id"));validarResponsable(initial,request.responsable());DeclaracionNna.texto(request.complejidadProvisional(),300,false);
        return execute(key,a,"ABRIR_CASO",List.of(destination,request),()->{
            var root=lock(communication);openCommunication(communication,root);var d=destination(destination);head(a,d);revision(d,request.revision());
            if(!"EVALUANDO".equals(d.get("state")))throw new FalloTecnico(409);
            var reqs=db.sql.queryForList("SELECT id FROM core_requirement WHERE communication_id=?",id(communication));UUID requirement;
            if(reqs.isEmpty()){requirement=UUID.randomUUID();db.sql.update("INSERT INTO core_requirement(id,communication_id,state,revision,created_ms,created_by) VALUES(?,?,'ACTIVO',0,?,?)",id(requirement),id(communication),now(),a.id());event(communication,"REQUERIMIENTO",requirement,a,"PRIMER_REQUERIMIENTO_CREADO");}else requirement=uuid(reqs.getFirst().get("id"));
            var cid=UUID.randomUUID();db.sql.update("INSERT INTO core_case(id,communication_id,requirement_id,destination_id,responsible_id,complexity_text,state,revision,created_ms,created_by) VALUES(?,?,?,?,?,?,'EN_GESTION',0,?,?)",id(cid),id(communication),id(requirement),id(destination),request.responsable(),request.complejidadProvisional(),now(),a.id());
            assignment(cid,a,request);db.sql.update("UPDATE core_destination SET revision=revision+1 WHERE id=?",id(destination));bump(communication);event(communication,"CASO",cid,a,"CASO_ABIERTO");reevaluate(communication,a);return new Resultado(cid,0);
        });
    }
    private void validarResponsable(Map<String,Object> row,String id){var responsible=catalogo.operador(id);if(!responsible.alcance(str(row,"unit_code"),str(row,"site_id"))||!(responsible.jefeEn(str(row,"unit_code"),str(row,"site_id"))||responsible.profesionalEn(str(row,"unit_code"),str(row,"site_id"))))throw new FalloTecnico(400);}
    private void assignment(UUID caseId,Operador a,Asignacion request){db.sql.update("INSERT INTO core_assignment VALUES(?,?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),request.responsable(),request.complejidadProvisional(),a.id(),now());}
    public Resultado asignar(Operador a,UUID key,UUID caseId,Asignacion request){
        actor(a);var initial=caseRow(caseId);head(a,initial);validarResponsable(initial,request.responsable());DeclaracionNna.texto(request.complejidadProvisional(),300,false);var communication=uuid(initial.get("communication_id"));
        return execute(key,a,"ASIGNAR_CASO",List.of(caseId,request),()->{
            lock(communication);var c=caseRow(caseId);head(a,c);revision(c,request.revision());if(!Set.of("EN_GESTION","CERRADO").contains(c.get("state")))throw new FalloTecnico(409);
            // Reasignar un cerrado permite encaminar antecedentes; nunca lo reabre.
            if("CERRADO".equals(c.get("state"))&&!Objects.equals(request.complejidadProvisional(),c.get("complexity_text")))throw new FalloTecnico(409);
            db.sql.update("UPDATE core_case SET responsible_id=?,complexity_text=?,revision=revision+1 WHERE id=?",request.responsable(),request.complejidadProvisional(),id(caseId));assignment(caseId,a,request);bump(communication);event(communication,"CASO",caseId,a,"RESPONSABLE_Y_COMPLEJIDAD_REGISTRADOS");return new Resultado(caseId,number(c,"revision")+1);
        });
    }
    public Resultado resolverDestino(Operador a,UUID key,UUID destination,Decision request,boolean sinCaso){
        actor(a);var initial=destination(destination);head(a,initial);var communication=uuid(initial.get("communication_id"));text(request.texto(),2000);
        return execute(key,a,sinCaso?"RESPONDER_DESTINO":"FINALIZAR_DESTINO",List.of(destination,request),()->{
            lock(communication);var d=destination(destination);head(a,d);revision(d,request.revision());if(!"EVALUANDO".equals(d.get("state")))throw new FalloTecnico(409);
            long cases=db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE destination_id=?",Long.class,id(destination));
            long open=db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE destination_id=? AND state<>'CERRADO'",Long.class,id(destination));
            if(sinCaso?cases!=0:cases==0||open!=0)throw new FalloTecnico(409);
            db.sql.update("UPDATE core_destination SET state='RESUELTO',revision=revision+1,decision=?,decision_text=?,decided_by=?,decided_ms=? WHERE id=?",sinCaso?"RESPUESTA_SIN_CASO":"EVALUACION_FINALIZADA",request.texto(),a.id(),now(),id(destination));
            bump(communication);reevaluate(communication,a);event(communication,"DESTINO",destination,a,sinCaso?"DESTINO_RESPONDIDO":"EVALUACION_DESTINO_FINALIZADA");return new Resultado(destination,number(d,"revision")+1);
        });
    }
    public Resultado actuar(Operador a,UUID key,UUID caseId,Decision request){
        actor(a);var initial=caseRow(caseId);if(!canCase(a,initial))throw new FalloTecnico(404);DeclaracionNna.relato(request.texto());var communication=uuid(initial.get("communication_id"));
        return execute(key,a,"REGISTRAR_ACTUACION",List.of(caseId,request),()->{
            lock(communication);var c=caseRow(caseId);if(!canCase(a,c))throw new FalloTecnico(404);revision(c,request.revision());if(!"EN_GESTION".equals(c.get("state")))throw new FalloTecnico(409);
            db.sql.update("INSERT INTO core_action VALUES(?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),a.id(),now(),request.texto());db.sql.update("UPDATE core_case SET revision=revision+1 WHERE id=?",id(caseId));bump(communication);event(communication,"CASO",caseId,a,"ACTUACION_PROVISIONAL_REGISTRADA");return new Resultado(caseId,number(c,"revision")+1);
        });
    }
    public Resultado revisarCaso(Operador a,UUID key,UUID caseId,Decision request,String decision){
        actor(a);if(!Set.of("SOLICITUD_TERMINO","DEVOLUCION","CIERRE").contains(decision))throw new FalloTecnico(400);text(request.texto(),2000);
        var initial=caseRow(caseId);if(!canCase(a,initial))throw new FalloTecnico(404);var communication=uuid(initial.get("communication_id"));
        return execute(key,a,decision,List.of(caseId,request),()->{
            lock(communication);var c=caseRow(caseId);if(!canCase(a,c))throw new FalloTecnico(404);revision(c,request.revision());String state=str(c,"state"),next;String modality=null;
            if(!decision.equals("DEVOLUCION")&&pendiente(caseId))throw new FalloTecnico(409);
            if(decision.equals("SOLICITUD_TERMINO")){
                if(!a.id().equals(c.get("responsible_id"))||!state.equals("EN_GESTION"))throw new FalloTecnico(409);next="REVISION_TERMINO";
            }else if(decision.equals("CIERRE")&&!a.jefeEn(str(c,"unit_code"),str(c,"site_id"))&&a.profesionalEn(str(c,"unit_code"),str(c,"site_id"))){
                if(!a.id().equals(c.get("responsible_id"))||!state.equals("EN_GESTION")||!"BAJA".equals(c.get("complexity_code")))throw new FalloTecnico(409);
                next="CERRADO";modality="PROFESIONAL_BAJA";
            }else{
                head(a,c);
                if(decision.equals("DEVOLUCION")){if(!state.equals("REVISION_TERMINO"))throw new FalloTecnico(409);next="EN_GESTION";}
                else{
                    boolean personal=a.id().equals(c.get("responsible_id"))&&state.equals("EN_GESTION")&&db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE case_id=? AND actor_id=?",Long.class,id(caseId),a.id())>0;
                    if(!state.equals("REVISION_TERMINO")&&!personal)throw new FalloTecnico(409);next="CERRADO";modality="JEFATURA";
                }
            }
            db.sql.update("UPDATE core_case SET state=?,revision=revision+1,closed_ms=?,closed_by=? WHERE id=?",next,next.equals("CERRADO")?now():null,next.equals("CERRADO")?a.id():null,id(caseId));
            if(next.equals("CERRADO"))db.sql.update("UPDATE core_case SET closure_mode=? WHERE id=?",modality,id(caseId));
            db.sql.update("INSERT INTO core_review VALUES(?,?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),a.id(),now(),decision,request.texto());
            bump(communication);reevaluate(communication,a);event(communication,"CASO",caseId,a,decision.equals("CIERRE")?("PROFESIONAL_BAJA".equals(modality)?"CASO_CERRADO_PROFESIONAL_BAJA":"CASO_CERRADO"):decision.equals("DEVOLUCION")?"TERMINO_DEVUELTO_CON_OBSERVACIONES":"TERMINO_SOLICITADO");return new Resultado(caseId,number(c,"revision")+1);
        });
    }
    public Resultado vincular(Operador a,UUID key,UUID child,Vinculacion request){
        actor(a);distributor(a);if(child.equals(request.padre()))throw new FalloTecnico(400);id(request.padre());text(request.motivo(),1000);
        return execute(key,a,"VINCULAR_COMUNICACION",List.of(child,request),()->{
            db.sql.queryForObject("SELECT id FROM core_link_guard WHERE id=1 FOR UPDATE",Integer.class);
            var ids=new ArrayList<>(List.of(child,request.padre()));ids.sort(Comparator.comparing(UUID::toString));for(var cid:ids)lock(cid);
            if(!db.sql.queryForList("SELECT child_id FROM core_link WHERE child_id=?",id(child)).isEmpty())throw new FalloTecnico(409);
            UUID cursor=request.padre();for(int step=0;cursor!=null;step++){
                if(step>=100||cursor.equals(child))throw new FalloTecnico(409);
                var parents=db.sql.queryForList("SELECT parent_id FROM core_link WHERE child_id=?",id(cursor));cursor=parents.isEmpty()?null:uuid(parents.getFirst().get("parent_id"));
            }
            String recipient=null;
            if(request.casoReferencia()!=null){var c=caseRow(request.casoReferencia());if(!request.padre().equals(uuid(c.get("communication_id"))))throw new FalloTecnico(400);recipient=str(c,"responsible_id");}
            var requirements=db.sql.queryForList("SELECT id FROM core_requirement WHERE communication_id=?",id(request.padre()));
            if(requirements.isEmpty())throw new FalloTecnico(409);
            db.sql.update("INSERT INTO core_link(child_id,parent_id,case_reference,actor_id,at_ms,reason_text,requirement_reference,revision) VALUES(?,?,?,?,?,?,?,0)",id(child),id(request.padre()),request.casoReferencia()==null?null:id(request.casoReferencia()),a.id(),now(),request.motivo(),requirements.getFirst().get("id"));
            if(request.casoReferencia()!=null)db.sql.update("UPDATE core_case SET revision=revision+1 WHERE id=?",id(request.casoReferencia()));
            db.sql.update("INSERT INTO core_notice VALUES(?,?,?,?,?,NULL)",id(UUID.randomUUID()),id(child),id(request.padre()),recipient,now());
            bump(child);bump(request.padre());event(child,"VINCULO",request.padre(),a,"VINCULO_DOCUMENTAL_CREADO");event(request.padre(),"VINCULO",child,a,recipient==null?"AVISO_PENDIENTE_DE_RESPONSABLE":"AVISO_INTERNO_PENDIENTE");return result(child,child);
        });
    }
    public List<Aviso> avisos(Operador a){actor(a);return db.sql.queryForList("SELECT n.* FROM core_notice n JOIN core_link l ON l.child_id=n.child_id JOIN core_case c ON c.id=l.case_reference JOIN core_destination d ON d.id=c.destination_id WHERE n.recipient_id=? AND c.responsible_id=? AND "+caseScope(a)+" ORDER BY n.at_ms DESC,n.id LIMIT 100",a.id(),a.id()).stream().map(r->new Aviso(uuid(r.get("id")),uuid(r.get("child_id")),uuid(r.get("parent_id")),date(r,"at_ms"),r.get("read_ms")!=null)).toList();}
    public Resultado leerAviso(Operador a,UUID key,UUID notice){
        actor(a);return execute(key,a,"LEER_AVISO",notice,()->{
            var rows=db.sql.queryForList("SELECT parent_id,child_id FROM core_notice WHERE id=? AND recipient_id=?",id(notice),a.id());if(rows.isEmpty())throw new FalloTecnico(404);
            linkReader(a,linkRow(uuid(rows.getFirst().get("child_id"))));
            var parent=uuid(rows.getFirst().get("parent_id"));lock(parent);
            if(db.sql.update("UPDATE core_notice SET read_ms=? WHERE id=? AND recipient_id=? AND read_ms IS NULL",now(),id(notice),a.id())==1)event(parent,"VINCULO",notice,a,"AVISO_INTERNO_LEIDO");return new Resultado(notice,0);
        });
    }
    private boolean pendiente(UUID caseId){return db.sql.queryForObject("SELECT COUNT(*) FROM core_transfer WHERE origin_case_id=? AND state='PENDIENTE'",Long.class,id(caseId))>0;}
    private List<ClasificacionHistorica> classificationHistory(UUID caseId){return db.sql.queryForList("SELECT * FROM core_complexity WHERE case_id=? ORDER BY at_ms,id",id(caseId)).stream().map(r->new ClasificacionHistorica(str(r,"classification"),str(r,"reason_text"),str(r,"actor_id"),date(r,"at_ms"))).toList();}
    public Resultado clasificar(Operador a,UUID key,UUID caseId,Clasificacion request){
        actor(a);var initial=caseRow(caseId);head(a,initial);if(request.clasificacion()!=null&&!request.clasificacion().equals("BAJA"))throw new FalloTecnico(400);text(request.fundamento(),2000);
        var communication=uuid(initial.get("communication_id"));
        return execute(key,a,"CLASIFICAR_COMPLEJIDAD",List.of(caseId,request),()->{
            lock(communication);var c=caseRow(caseId);head(a,c);revision(c,request.revision());if("CERRADO".equals(c.get("state")))throw new FalloTecnico(409);
            db.sql.update("UPDATE core_case SET complexity_code=?,revision=revision+1 WHERE id=?",request.clasificacion(),id(caseId));
            db.sql.update("INSERT INTO core_complexity VALUES(?,?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),request.clasificacion(),request.fundamento(),a.id(),now());
            bump(communication);event(communication,"CASO",caseId,a,"COMPLEJIDAD_CLASIFICADA");return new Resultado(caseId,number(c,"revision")+1);
        });
    }
    private Map<String,Object> transferRow(UUID requestId){
        var rows=db.sql.queryForList("SELECT t.*,c.communication_id,c.requirement_id,c.revision AS origin_revision,c.state AS origin_state,d.unit_code AS origin_unit,d.site_id AS origin_site FROM core_transfer t JOIN core_case c ON c.id=t.origin_case_id JOIN core_destination d ON d.id=c.destination_id WHERE t.id=?",id(requestId));
        if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private void transferReader(Operador a,Map<String,Object> t){if(!(a.jefeEn(str(t,"target_unit"),str(t,"target_site"))||a.jefeEn(str(t,"origin_unit"),str(t,"origin_site"))))throw new FalloTecnico(404);}
    private Derivacion transferView(Map<String,Object> t){
        var compatible=db.sql.queryForList("SELECT id,revision,state FROM core_destination WHERE communication_id=? AND unit_code=? AND site_id=? AND state='EVALUANDO' ORDER BY created_ms,id",t.get("communication_id"),t.get("target_unit"),t.get("target_site")).stream().map(r->new DestinoCompatible(uuid(r.get("id")),number(r,"revision"),str(r,"state"))).toList();
        return new Derivacion(uuid(t.get("id")),uuid(t.get("origin_case_id")),uuid(t.get("requirement_id")),str(t,"origin_unit"),str(t,"origin_site"),str(t,"target_unit"),str(t,"target_site"),str(t,"requested_by"),date(t,"requested_ms"),number(t,"origin_revision_at_request"),str(t,"reason_text"),str(t,"evaluation_text"),str(t,"state"),number(t,"revision"),number(t,"origin_revision"),str(t,"origin_state"),str(t,"decided_by"),date(t,"decided_ms"),str(t,"decision_text"),uuid(t.get("target_case_id")),compatible);
    }
    public Derivacion derivacion(Operador a,UUID requestId){actor(a);return lectura.execute(status->{var t=transferRow(requestId);transferReader(a,t);return transferView(t);});}
    public List<Derivacion> entrantes(Operador a){actor(a);if(!a.jefatura())throw new FalloTecnico(403);return lectura.execute(status->db.sql.queryForList("SELECT id FROM core_transfer WHERE "+grantSql(a,"target_unit","target_site",true)+" ORDER BY requested_ms DESC,id LIMIT 100").stream().map(r->transferView(transferRow(uuid(r.get("id"))))).toList());}
    public Pagina<Derivacion> entrantesPagina(Operador a,int page,int size){actor(a);if(!a.jefatura())throw new FalloTecnico(403);page(page,size);return lectura.execute(s->{long total=db.sql.queryForObject("SELECT COUNT(*) FROM core_transfer WHERE "+grantSql(a,"target_unit","target_site",true)+" AND state='PENDIENTE'",Long.class);var items=db.sql.queryForList("SELECT id FROM core_transfer WHERE "+grantSql(a,"target_unit","target_site",true)+" AND state='PENDIENTE' ORDER BY requested_ms,id LIMIT ? OFFSET ?",size,page*size).stream().map(r->transferView(transferRow(uuid(r.get("id"))))).toList();return new Pagina<>(items,total,page,size);});}
    public List<Derivacion> salientes(Operador a,UUID caseId){actor(a);var c=caseRow(caseId);head(a,c);return lectura.execute(status->db.sql.queryForList("SELECT id FROM core_transfer WHERE origin_case_id=?",id(caseId)).stream().map(r->transferView(transferRow(uuid(r.get("id"))))).toList());}
    public Resultado solicitarDerivacion(Operador a,UUID key,UUID caseId,SolicitarDerivacion request){
        actor(a);var initial=caseRow(caseId);head(a,initial);text(request.motivo(),1000);DeclaracionNna.relato(request.antecedentesEvaluacion());
        if(catalogo.presencias().stream().noneMatch(p->p.unidad().equals(request.unidad())&&p.sede().equals(request.sede()))||(Objects.equals(initial.get("unit_code"),request.unidad())&&Objects.equals(initial.get("site_id"),request.sede())))throw new FalloTecnico(400);
        var communication=uuid(initial.get("communication_id"));return execute(key,a,"SOLICITAR_DERIVACION",List.of(caseId,request),()->{
            lock(communication);var c=caseRow(caseId);head(a,c);revision(c,request.revision());if(!"EN_GESTION".equals(c.get("state"))||pendiente(caseId))throw new FalloTecnico(409);
            var rid=UUID.randomUUID();db.sql.update("INSERT INTO core_transfer(id,origin_case_id,target_unit,target_site,requested_by,requested_ms,origin_revision_at_request,reason_text,evaluation_text,state,revision) VALUES(?,?,?,?,?,?,?,?,?,'PENDIENTE',0)",id(rid),id(caseId),request.unidad(),request.sede(),a.id(),now(),request.revision(),request.motivo(),request.antecedentesEvaluacion());
            db.sql.update("UPDATE core_case SET revision=revision+1 WHERE id=?",id(caseId));bump(communication);event(communication,"CASO",caseId,a,"DERIVACION_SOLICITADA_ORIGEN_ABIERTO");return new Resultado(rid,0);
        });
    }
    public Resultado aceptarDerivacion(Operador a,UUID key,UUID requestId,AceptarDerivacion request){
        actor(a);var initial=transferRow(requestId);if(!a.jefeEn(str(initial,"target_unit"),str(initial,"target_site")))throw new FalloTecnico(404);text(request.fundamento(),2000);
        var communication=uuid(initial.get("communication_id"));return execute(key,a,"ACEPTAR_DERIVACION",List.of(requestId,request),()->{
            lock(communication);var t=transferRow(requestId);
            if(!a.jefeEn(str(t,"target_unit"),str(t,"target_site")))throw new FalloTecnico(404);
            revision(t,request.revision());if(!"PENDIENTE".equals(t.get("state")))throw new FalloTecnico(409);
            var origin=uuid(t.get("origin_case_id"));var c=caseRow(origin);revision(c,request.revisionOrigen());if(!"EN_GESTION".equals(c.get("state")))throw new FalloTecnico(409);
            var target=request.destinoId();
            if(target==null){
                if(request.revisionDestino()!=null)throw new FalloTecnico(400);target=UUID.randomUUID();
                db.sql.update("INSERT INTO core_destination(id,communication_id,unit_code,site_id,evaluator_id,state,revision,created_ms,created_by) VALUES(?,?,?,?,?,'EVALUANDO',0,?,?)",id(target),id(communication),t.get("target_unit"),t.get("target_site"),a.id(),now(),a.id());
                event(communication,"DESTINO",target,a,"DESTINO_CREADO_POR_DERIVACION_ACEPTADA");
            }else{
                var d=destination(target);if(!communication.equals(uuid(d.get("communication_id")))||!a.jefeEn(str(d,"unit_code"),str(d,"site_id")))throw new FalloTecnico(409);
                if(request.revisionDestino()==null)throw new FalloTecnico(400);revision(d,request.revisionDestino());if(!"EVALUANDO".equals(d.get("state")))throw new FalloTecnico(409);
            }
            var newCase=UUID.randomUUID();db.sql.update("INSERT INTO core_case(id,communication_id,requirement_id,destination_id,responsible_id,complexity_text,state,revision,created_ms,created_by,origin_case_id) VALUES(?,?,?,?,NULL,NULL,'EN_GESTION',0,?,?,?)",id(newCase),id(communication),c.get("requirement_id"),id(target),now(),a.id(),id(origin));
            db.sql.update("UPDATE core_destination SET revision=revision+1 WHERE id=?",id(target));
            db.sql.update("UPDATE core_transfer SET state='ACEPTADA',revision=revision+1,decided_by=?,decided_ms=?,decision_text=?,target_case_id=? WHERE id=?",a.id(),now(),request.fundamento(),id(newCase),id(requestId));
            db.sql.update("UPDATE core_case SET state='CERRADO',revision=revision+1,closed_ms=?,closed_by=?,closure_mode='DERIVACION' WHERE id=?",now(),a.id(),id(origin));
            db.sql.update("INSERT INTO core_review VALUES(?,?,?,?,?,?)",id(UUID.randomUUID()),id(origin),a.id(),now(),"CIERRE",request.fundamento());
            event(communication,"CASO",origin,a,"ORIGEN_CERRADO_POR_DERIVACION_ACEPTADA");event(communication,"CASO",newCase,a,"CASO_DERIVADO_CREADO_SIN_ASIGNACION");bump(communication);reevaluate(communication,a);return new Resultado(newCase,0);
        });
    }
    public void decisionDerivacionPendiente(Operador a,UUID requestId){derivacion(a,requestId);throw new FalloTecnico(409);}
    public void transicionPendiente(Operador a,UUID caseId){actor(a);var c=caseRow(caseId);head(a,c);throw new FalloTecnico(409);}

    // Antecedentes: autorización por contexto. Un vínculo nunca amplía el alcance al resto del agrupador.
    private Map<String,Object> linkRow(UUID child){var rows=db.sql.queryForList("SELECT * FROM core_link WHERE child_id=?",id(child));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
    private void linkReader(Operador a,Map<String,Object> l){
        if(a.distribuidor())return;
        if(l.get("case_reference")!=null){if(canCase(a,caseRow(uuid(l.get("case_reference")))))return;}
        else if(db.sql.queryForObject("SELECT COUNT(*) FROM core_destination d WHERE d.communication_id=? AND "+grantSql(a,"d.unit_code","d.site_id",true),Long.class,l.get("parent_id"))>0)return;
        throw new FalloTecnico(404);
    }
    private void evaluator(Operador a,Map<String,Object> c){if(!a.profesionalEn(str(c,"unit_code"),str(c,"site_id"))||!canCase(a,c)||!a.id().equals(c.get("responsible_id")))throw new FalloTecnico(404);}
    private Antecedente linkView(Operador a,Map<String,Object> l,boolean content){
        var child=uuid(l.get("child_id"));var c=l.get("case_reference")==null?null:caseRow(uuid(l.get("case_reference")));
        var evals=db.sql.queryForList("SELECT * FROM core_link_evaluation WHERE child_id=?",id(child));var e=evals.isEmpty()?Map.<String,Object>of():evals.getFirst();
        String state=e.isEmpty()?(l.get("requirement_reference")==null?"SIN_EVALUACION_HISTORICA":c==null?"PENDIENTE_ASIGNACION":"PENDIENTE"):str(e,"decision");
        String responsible=null;
        if(c!=null&&c.get("responsible_id")!=null){try{var current=catalogo.operador(str(c,"responsible_id"));if(current.profesionalEn(str(c,"unit_code"),str(c,"site_id"))&&canCase(current,c))responsible=current.id();}catch(FalloTecnico inactive){if(inactive.status!=401&&inactive.status!=404&&inactive.status!=403)throw inactive;}}
        if(state.equals("PENDIENTE")&&responsible==null)state="PENDIENTE_ASIGNACION";
        return new Antecedente(child,resumen(registry(child,false)).folio(),uuid(l.get("parent_id")),uuid(l.get("requirement_reference")),uuid(l.get("case_reference")),state,number(l,"revision"),str(l,"reason_text"),str(l,"actor_id"),date(l,"at_ms"),responsible,str(e,"reason_text"),str(e,"actor_id"),date(e,"at_ms"),content?contenidoOriginal(child):null);
    }
    public Antecedente antecedente(Operador a,UUID child){actor(a);return lectura.execute(s->{var l=linkRow(child);linkReader(a,l);return linkView(a,l,a.distribuidor()||l.get("case_reference")!=null);});}
    private String linkScope(Operador a){return a.distribuidor()?"1=1":"(EXISTS(SELECT 1 FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE c.id=l.case_reference AND "+caseScope(a)+") OR (l.case_reference IS NULL AND EXISTS(SELECT 1 FROM core_destination d WHERE d.communication_id=l.parent_id AND "+grantSql(a,"d.unit_code","d.site_id",true)+")))";}
    private ArrayList<Object> linkArgs(Operador a){return new ArrayList<>();}
    private static void page(int page,int size){if(page<0||page>100000||size<1||size>50)throw new FalloTecnico(400);}
    public Pagina<Antecedente> antecedentes(Operador a,UUID caseId,boolean pending,int page,int size){
        actor(a);page(page,size);return lectura.execute(s->{var args=linkArgs(a);String where=linkScope(a);
            if(caseId!=null){var c=caseRow(caseId);if(!a.distribuidor()&&!canCase(a,c))throw new FalloTecnico(404);where+=" AND l.case_reference=?";args.add(id(caseId));}
            if(pending)where+=" AND NOT EXISTS(SELECT 1 FROM core_link_evaluation e WHERE e.child_id=l.child_id)";
            long total=db.sql.queryForObject("SELECT COUNT(*) FROM core_link l WHERE "+where,Long.class,args.toArray());args.add(size);args.add(page*size);
            var items=db.sql.queryForList("SELECT l.* FROM core_link l WHERE "+where+" ORDER BY l.at_ms DESC,l.child_id LIMIT ? OFFSET ?",args.toArray()).stream().map(l->linkView(a,l,false)).toList();return new Pagina<>(items,total,page,size);
        });
    }
    public Resultado evaluarAntecedente(Operador a,UUID key,UUID child,EvaluarAntecedente request){
        actor(a);var initial=linkRow(child);linkReader(a,initial);if(initial.get("case_reference")==null)throw new FalloTecnico(409);evaluator(a,caseRow(uuid(initial.get("case_reference"))));
        if(request.decision()==null||!Set.of("ADMITIDA","NO_ADMITIDA").contains(request.decision()))throw new FalloTecnico(400);text(request.fundamento(),2000);
        return execute(key,a,"EVALUAR_ANTECEDENTE",List.of(child,request),()->{
            var parent=uuid(initial.get("parent_id"));lock(parent);var l=linkRow(child);revision(l,request.revision());var caseId=uuid(l.get("case_reference"));var c=caseRow(caseId);evaluator(a,c);revision(c,request.revisionCaso());
            if(!db.sql.queryForList("SELECT child_id FROM core_link_evaluation WHERE child_id=?",id(child)).isEmpty())throw new FalloTecnico(409);
            db.sql.update("INSERT INTO core_link_evaluation VALUES(?,?,?,?,?,?)",id(child),request.decision(),request.fundamento(),a.id(),now(),number(c,"revision"));
            db.sql.update("UPDATE core_link SET revision=revision+1 WHERE child_id=?",id(child));db.sql.update("UPDATE core_case SET revision=revision+1 WHERE id=?",id(caseId));bump(parent);
            event(parent,"CASO",caseId,a,request.decision().equals("ADMITIDA")?"ANTECEDENTE_ADMITIDO_REFERENCIADO":"ANTECEDENTE_NO_INCORPORADO");return new Resultado(child,number(l,"revision")+1);
        });
    }
    private Map<String,Object> reopenRow(UUID requestId){var rows=db.sql.queryForList("SELECT * FROM core_reopen WHERE id=?",id(requestId));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();}
    private List<UUID> reopenEvidence(UUID requestId){return db.sql.queryForList("SELECT child_id FROM core_reopen_evidence WHERE request_id=? ORDER BY child_id",id(requestId)).stream().map(r->uuid(r.get("child_id"))).toList();}
    private Reapertura reopenView(Map<String,Object> r){var c=caseRow(uuid(r.get("case_id")));return new Reapertura(uuid(r.get("id")),uuid(c.get("id")),uuid(c.get("requirement_id")),str(c,"unit_code"),str(c,"site_id"),str(r,"requested_by"),date(r,"requested_ms"),number(r,"case_revision"),number(c,"revision"),str(c,"responsible_id"),date(r,"closed_ms"),str(r,"closure_mode"),str(r,"reason_text"),reopenEvidence(uuid(r.get("id"))),str(r,"state"),number(r,"revision"),str(r,"decided_by"),date(r,"decided_ms"),str(r,"decision_text"),uuid(r.get("supersedes_id")));}
    public Reapertura reapertura(Operador a,UUID requestId){actor(a);return lectura.execute(s->{var r=reopenRow(requestId);if(!canCase(a,caseRow(uuid(r.get("case_id")))))throw new FalloTecnico(404);return reopenView(r);});}
    public Pagina<Reapertura> reaperturas(Operador a,UUID caseId,boolean pending,int page,int size){actor(a);page(page,size);if(a.distribuidor()&&!a.jefatura()&&a.accesos().stream().noneMatch(x->x.rol().equals("PROFESIONAL")))throw new FalloTecnico(403);return lectura.execute(s->{
        String from=" FROM core_reopen r JOIN core_case c ON c.id=r.case_id JOIN core_destination d ON d.id=c.destination_id WHERE "+caseScopeGestion(a);var args=caseArgs(a);
        if(caseId!=null){from+=" AND c.id=?";args.add(id(caseId));}if(pending)from+=" AND r.state='PENDIENTE'";
        long total=db.sql.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());args.add(size);args.add(page*size);return new Pagina<>(db.sql.queryForList("SELECT r.*"+from+" ORDER BY r.requested_ms DESC,r.id LIMIT ? OFFSET ?",args.toArray()).stream().map(this::reopenView).toList(),total,page,size);
    });}
    private void admitted(UUID child,UUID caseId){var l=linkRow(child);if(!caseId.equals(uuid(l.get("case_reference")))||db.sql.queryForObject("SELECT COUNT(*) FROM core_link_evaluation WHERE child_id=? AND decision='ADMITIDA'",Long.class,id(child))!=1)throw new FalloTecnico(409);}
    public Resultado solicitarReapertura(Operador a,UUID key,UUID caseId,SolicitarReapertura request){
        actor(a);var initial=caseRow(caseId);evaluator(a,initial);text(request.fundamento(),2000);
        if(request.antecedentes()==null||request.antecedentes().isEmpty()||request.antecedentes().size()>20||new HashSet<>(request.antecedentes()).size()!=request.antecedentes().size()||request.antecedentes().stream().anyMatch(Objects::isNull))throw new FalloTecnico(400);
        return execute(key,a,"SOLICITAR_REAPERTURA",List.of(caseId,request),()->{
            var parent=uuid(initial.get("communication_id"));lock(parent);var c=caseRow(caseId);evaluator(a,c);revision(c,request.revision());
            if("DERIVACION".equals(c.get("closure_mode")))throw new FalloTecnico(409,"REAPERTURA_DERIVACION_PENDIENTE");
            if(!"CERRADO".equals(c.get("state")))throw new FalloTecnico(409,"CASO_NO_CERRADO");
            request.antecedentes().forEach(child->admitted(child,caseId));
            var pending=db.sql.queryForList("SELECT * FROM core_reopen WHERE case_id=? AND state='PENDIENTE'",id(caseId));
            if(!pending.isEmpty()){
                var old=pending.getFirst();if(request.sustituye()==null||!request.sustituye().equals(uuid(old.get("id")))||request.revisionSustituida()==null)throw new FalloTecnico(409);revision(old,request.revisionSustituida());
                // Solo se reemplaza explícitamente una solicitud obsoleta. Sus hechos y evidencias permanecen.
                if(number(old,"case_revision")==number(c,"revision"))throw new FalloTecnico(409);
                db.sql.update("UPDATE core_reopen SET state='SUPERADA',revision=revision+1 WHERE id=?",old.get("id"));event(parent,"CASO",caseId,a,"SOLICITUD_OBSOLETA_SUSTITUIDA");
            }else if(request.sustituye()!=null||request.revisionSustituida()!=null)throw new FalloTecnico(409);
            // Una denegación no genera un ciclo idéntico: debe existir al menos un antecedente no invocado antes para este cierre.
            var prior=db.sql.queryForList("SELECT id FROM core_reopen WHERE case_id=? AND closed_ms=? AND state='RECHAZADA'",id(caseId),c.get("closed_ms"));
            if(!prior.isEmpty()&&request.antecedentes().stream().allMatch(child->prior.stream().anyMatch(r->reopenEvidence(uuid(r.get("id"))).contains(child))))throw new FalloTecnico(409);
            UUID rid=UUID.randomUUID();db.sql.update("INSERT INTO core_reopen(id,case_id,requested_by,requested_ms,case_revision,closed_ms,closed_by,closure_mode,reason_text,state,revision,supersedes_id) VALUES(?,?,?,?,?,?,?,?,?,'PENDIENTE',0,?)",id(rid),id(caseId),a.id(),now(),number(c,"revision"),c.get("closed_ms"),c.get("closed_by"),c.get("closure_mode"),request.fundamento(),request.sustituye()==null?null:id(request.sustituye()));
            for(var child:request.antecedentes())db.sql.update("INSERT INTO core_reopen_evidence VALUES(?,?,?)",id(rid),id(child),number(linkRow(child),"revision"));
            event(parent,"CASO",caseId,a,"REAPERTURA_SOLICITADA_CASO_CERRADO");bump(parent);return new Resultado(rid,0);
        });
    }
    public Resultado decidirReapertura(Operador a,UUID key,UUID requestId,DecidirReapertura request,boolean approve){
        actor(a);var initial=reopenRow(requestId);var first=caseRow(uuid(initial.get("case_id")));head(a,first);text(request.fundamento(),2000);
        return execute(key,a,approve?"APROBAR_REAPERTURA":"RECHAZAR_REAPERTURA",List.of(requestId,request),()->{
            var parent=uuid(first.get("communication_id"));lock(parent);var r=reopenRow(requestId);var caseId=uuid(r.get("case_id"));var c=caseRow(caseId);head(a,c);revision(r,request.revision());revision(c,request.revisionCaso());
            if(!"PENDIENTE".equals(r.get("state")))throw new FalloTecnico(409,"SOLICITUD_YA_DECIDIDA");
            if(!"CERRADO".equals(c.get("state"))||"DERIVACION".equals(c.get("closure_mode"))||number(c,"revision")!=number(r,"case_revision")||!Objects.equals(c.get("closed_ms"),r.get("closed_ms"))||!Objects.equals(c.get("responsible_id"),r.get("requested_by")))throw new FalloTecnico(409,"SOLICITUD_OBSOLETA");
            var professional=catalogo.operador(str(c,"responsible_id"));evaluator(professional,c);
            for(var child:reopenEvidence(requestId)){admitted(child,caseId);var saved=db.sql.queryForMap("SELECT link_revision FROM core_reopen_evidence WHERE request_id=? AND child_id=?",id(requestId),id(child));if(number(saved,"link_revision")!=number(linkRow(child),"revision"))throw new FalloTecnico(409);}
            db.sql.update("UPDATE core_reopen SET state=?,revision=revision+1,decided_by=?,decided_ms=?,decision_text=? WHERE id=?",approve?"APROBADA":"RECHAZADA",a.id(),now(),request.fundamento(),id(requestId));
            if(approve){
                var d=destination(uuid(c.get("destination_id")));
                if("RESUELTO".equals(d.get("state"))){db.sql.update("UPDATE core_destination SET state='EVALUANDO',revision=revision+1 WHERE id=?",d.get("id"));event(parent,"DESTINO",uuid(d.get("id")),a,"DESTINO_REANUDADO_POR_REAPERTURA");event(parent,"REAPERTURA",requestId,a,"AUTORIZACION_REANUDA_DESTINO");}
                // Se retienen cierre y revisión anteriores; core_reopen conserva su instantánea y la autorización de este período.
                db.sql.update("UPDATE core_case SET state='EN_GESTION',revision=revision+1 WHERE id=?",id(caseId));
                db.sql.update("INSERT INTO core_management_period VALUES(?,?,?,?,?)",id(UUID.randomUUID()),id(caseId),id(requestId),now(),a.id());
            }
            var evidence=reopenEvidence(requestId).getFirst();db.sql.update("INSERT INTO core_notice VALUES(?,?,?,?,?,NULL)",id(UUID.randomUUID()),id(evidence),id(parent),r.get("requested_by"),now());
            event(parent,"CASO",caseId,a,approve?"REAPERTURA_AUTORIZADA_NUEVO_PERIODO":"REAPERTURA_RECHAZADA_CASO_CERRADO");bump(parent);reevaluate(parent,a);return new Resultado(requestId,number(r,"revision")+1);
        });
    }
    public List<Periodo> periodos(Operador a,UUID caseId){actor(a);return lectura.execute(s->{var c=caseRow(caseId);if(!canCase(a,c))throw new FalloTecnico(404);return db.sql.queryForList("SELECT * FROM core_management_period WHERE case_id=? ORDER BY opened_ms,id",id(caseId)).stream().map(r->new Periodo(uuid(r.get("id")),uuid(r.get("request_id")),date(r,"opened_ms"),str(r,"authorized_by"))).toList();});}
    public Pagina<CasoResumen> casos(Operador a,String state,int page,int size){actor(a);page(page,size);return lectura.execute(s->{
        String from=" FROM core_case c JOIN core_destination d ON d.id=c.destination_id JOIN communication_registry r ON r.comunicacion_id=c.communication_id WHERE "+caseScope(a);var args=caseArgs(a);
        if(state!=null&&!state.isBlank()){if(!Set.of("EN_GESTION","REVISION_TERMINO","CERRADO").contains(state))throw new FalloTecnico(400);from+=" AND c.state=?";args.add(state);}
        long total=db.sql.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());args.add(size);args.add(page*size);
        var items=db.sql.queryForList("SELECT c.*,d.unit_code,d.site_id,r.numero,r.folio_prefix,r.folio_width"+from+" ORDER BY c.created_ms DESC,c.id LIMIT ? OFFSET ?",args.toArray()).stream().map(c->new CasoResumen(uuid(c.get("id")),uuid(c.get("communication_id")),str(c,"folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+number(c,"folio_width")+"d",number(c,"numero")),uuid(c.get("requirement_id")),str(c,"unit_code"),str(c,"site_id"),str(c,"responsible_id"),str(c,"complexity_code"),str(c,"state"),number(c,"revision"),pendiente(uuid(c.get("id"))),db.sql.queryForObject("SELECT COUNT(*) FROM core_reopen WHERE case_id=? AND state='PENDIENTE'",Long.class,c.get("id"))>0)).toList();return new Pagina<>(items,total,page,size);
    });}
    public Pagina<Resumen> buscar(Operador a,String query,boolean requirements,int page,int size){actor(a);page(page,size);if(query==null||query.length()>60||!query.matches("[a-zA-Z0-9-]*"))throw new FalloTecnico(400);return lectura.execute(s->{
        String scope=communicationScope(a);
        var args=caseArgs(a);String where=scope+" AND LOWER(CONCAT(r.folio_prefix,'-',LPAD(CONCAT('',r.numero),r.folio_width,'0'))) LIKE ?";args.add("%"+query.toLowerCase(Locale.ROOT)+"%");
        if(requirements)where+=" AND EXISTS(SELECT 1 FROM core_requirement q WHERE q.communication_id=r.comunicacion_id)";
        long total=db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry r WHERE "+where,Long.class,args.toArray());args.add(size);args.add(page*size);
        return new Pagina<>(db.sql.queryForList("SELECT r.* FROM communication_registry r WHERE "+where+" ORDER BY r.numero DESC LIMIT ? OFFSET ?",args.toArray()).stream().map(this::resumen).toList(),total,page,size);
    });}
    public Bandeja bandeja(Operador a){actor(a);return lectura.execute(s->{
        long communications=buscar(a,"",false,0,1).total();long cases=casos(a,"EN_GESTION",0,1).total();long reviews=casos(a,"REVISION_TERMINO",0,1).total();long links=antecedentes(a,null,true,0,1).total();
        long unassigned=a.distribuidor()?db.sql.queryForObject("SELECT COUNT(*) FROM core_link WHERE case_reference IS NULL",Long.class):0;
        long reopen=a.distribuidor()&&!a.jefatura()&&a.accesos().stream().noneMatch(x->x.rol().equals("PROFESIONAL"))?0:reaperturas(a,null,true,0,1).total();long transfers=a.jefatura()?db.sql.queryForObject("SELECT COUNT(*) FROM core_transfer WHERE state='PENDIENTE' AND "+grantSql(a,"target_unit","target_site",true),Long.class):0;
        return new Bandeja(communications,cases,links,unassigned,reopen,transfers,reviews);
    });}
    public List<Resumen> buzon(Operador a,long despues,int limite){return lectura.execute(status->buzonSnapshot(a,despues,limite));}
    private List<Resumen> buzonSnapshot(Operador a,long despues,int limite){
        actor(a);if(despues<0||limite<1||limite>100)throw new FalloTecnico(400);
        String scope=communicationScope(a);
        var args=new ArrayList<Object>();args.add(despues);args.addAll(caseArgs(a));args.add(limite);
        var rows=db.sql.queryForList("SELECT r.* FROM communication_registry r WHERE r.numero>? AND "+scope+" ORDER BY r.numero LIMIT ?",args.toArray());
        return rows.stream().map(r->resumen(r)).toList();
    }
    private Resumen resumen(Map<String,Object> r){
        var cid=uuid(r.get("comunicacion_id"));var core=db.sql.queryForList("SELECT state,revision FROM core_communication WHERE id=?",id(cid));var req=db.sql.queryForList("SELECT id,state FROM core_requirement WHERE communication_id=?",id(cid));
        int open=db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE communication_id=? AND state<>'CERRADO'",Integer.class,id(cid));int pending=db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=? AND state<>'RESUELTO'",Integer.class,id(cid));
        String state=core.isEmpty()?"ABIERTA":str(core.getFirst(),"state");if(state.equals("ABIERTA")&&!req.isEmpty())state="REQUERIMIENTO_"+str(req.getFirst(),"state");
        else if(state.equals("ABIERTA")&&pending==0&&db.sql.queryForObject("SELECT COUNT(*) FROM core_destination WHERE communication_id=?",Integer.class,id(cid))>0)state="ATENDIDA_SIN_CASO";
        String folio=str(r,"folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+number(r,"folio_width")+"d",number(r,"numero"));
        var origins=db.sql.queryForList("SELECT * FROM communication_origin WHERE communication_id=?",id(cid));var origin=origins.isEmpty()?Map.<String,Object>of():origins.getFirst();var manual=db.sql.queryForList("SELECT channel_declared FROM core_manual WHERE communication_id=?",id(cid));
        return new Resumen(cid,number(r,"numero"),folio,date(r,"registrada_ms"),str(r,"canal"),state,core.isEmpty()?0:number(core.getFirst(),"revision"),req.isEmpty()?null:uuid(req.getFirst().get("id")),req.isEmpty()?null:str(req.getFirst(),"state"),open,pending,"REGISTRO_FUNCIONARIO".equals(r.get("source"))?"REGISTRO_FUNCIONARIO":"WEB",str(origin,"site_id"),str(origin,"subject_region"),manual.isEmpty()?null:str(manual.getFirst(),"channel_declared"));
    }
    private Contenido contenidoOriginal(UUID communication){
        var r=registry(communication,false);var root=db.sql.queryForList("SELECT * FROM core_communication WHERE id=?",id(communication));
        Contenido content;
        if("REGISTRO_FUNCIONARIO".equals(r.get("source"))){
            var m=db.sql.queryForMap("SELECT * FROM core_manual WHERE communication_id=?",id(communication));
            var meta=db.sql.queryForList("SELECT subject_region FROM communication_origin WHERE communication_id=?",id(communication));
            var manual=new Manual(str(m,"body_text"),str(m,"channel_declared"),date(m,"received_declared_ms"),str(root.getFirst(),"site_id"),null,meta.isEmpty()?null:str(meta.getFirst(),"subject_region"));
            if(!contentHash(communication,number(r,"registrada_ms"),manual).equals(r.get("content_hash"))||!hash(List.of("REGISTRO_FUNCIONARIO_1",str(m,"actor_id"),number(r,"registrada_ms"))).equals(r.get("provenance_hash")))throw new FalloTecnico(409);
            content=new Contenido(manual.texto(),"REGISTRO_POR_OPERADOR",null,null,manual.recibidaEnDeclarada(),manual.canalDeclarado(),str(m,"actor_id"));
        }else if(recepcion.esCompleta(communication)){
            var e=recepcion.completaRegistrada(communication);var c=e.comunicacion();var fields=c.path("datos").path("campos");String story=fields.path("nna-story").asString("");
            for(var key:List.of("relato","consulta-general-detalle","entrevista-motivo","stats-descripcion"))if(!fields.path(key).asString("").isBlank())story=fields.path(key).asString();
            content=new Contenido(story,e.tipo().equals("ADULTO_CU")?"AUTENTICADA_CLAVE_UNICA":e.tipo().equals("NNA")?"DECLARADA_NNA":"DECLARADA_ADULTO",null,null,Instant.parse(c.path("recibidaEn").asString()),e.canal(),null,c);
        }else{var e=recepcion.entregaRegistrada(communication);content=new Contenido(e.comunicacion().mensaje(),e.comunicacion().declaracionNna()!=null?"DECLARADA_NNA":"AUTENTICADA_CLAVE_UNICA",e.comunicacion().identidad(),e.comunicacion().declaracionNna(),e.comunicacion().recibidaEn(),null,null);}
        return content;
    }
    public Detalle detalle(Operador a,UUID communication){return lectura.execute(status->detalleSnapshot(a,communication));}
    private Detalle detalleSnapshot(Operador a,UUID communication){
        actor(a);readable(a,communication);var r=registry(communication,false);var root=db.sql.queryForList("SELECT * FROM core_communication WHERE id=?",id(communication));
        var content=contenidoOriginal(communication);
        var destinations=db.sql.queryForList("SELECT * FROM core_destination WHERE communication_id=? ORDER BY created_ms,id",id(communication)).stream()
            .filter(d->a.distribuidor()||a.jefeEn(str(d,"unit_code"),str(d,"site_id"))||a.profesionalEn(str(d,"unit_code"),str(d,"site_id"))&&db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE destination_id=? AND responsible_id=?",Long.class,d.get("id"),a.id())>0)
            .map(d->new Destino(uuid(d.get("id")),str(d,"unit_code"),str(d,"site_id"),str(d,"evaluator_id"),str(d,"state"),number(d,"revision"),str(d,"decision"),a.distribuidor()||a.jefeEn(str(d,"unit_code"),str(d,"site_id"))?str(d,"decision_text"):null,str(d,"decided_by"),date(d,"decided_ms"),db.sql.queryForObject("SELECT COUNT(*) FROM core_case WHERE destination_id=?",Integer.class,d.get("id")))).toList();
        var rows=db.sql.queryForList("SELECT c.*,d.unit_code,d.site_id FROM core_case c JOIN core_destination d ON d.id=c.destination_id WHERE c.communication_id=? ORDER BY c.created_ms,c.id",id(communication));
        var cases=rows.stream().filter(c->a.distribuidor()||canCase(a,c)).map(c->caseView(c,!a.distribuidor())).toList();
        var visible=new HashSet<UUID>();cases.forEach(c->visible.add(c.id()));destinations.forEach(d->visible.add(d.id()));
        var history=db.sql.queryForList("SELECT * FROM core_event WHERE communication_id=? ORDER BY seq DESC LIMIT 200",id(communication)).stream()
            .filter(e->a.distribuidor()||str(e,"resource_kind").equals("COMUNICACION")||visible.contains(uuid(e.get("resource_id"))))
            .map(e->new Evento(number(e,"seq"),str(e,"resource_kind"),uuid(e.get("resource_id")),str(e,"actor_id"),date(e,"at_ms"),str(e,"code"))).toList();
        var links=a.distribuidor()?db.sql.queryForList("SELECT * FROM core_link WHERE child_id=? OR parent_id=? ORDER BY at_ms",id(communication),id(communication)).stream().map(l->new Vinculo(uuid(l.get("child_id")),uuid(l.get("parent_id")),uuid(l.get("case_reference")),str(l,"actor_id"),date(l,"at_ms"),str(l,"reason_text"))).toList():List.<Vinculo>of();
        var core=root.isEmpty()?Map.<String,Object>of():root.getFirst();
        return new Detalle(resumen(r),content,destinations,cases,history,links,str(core,"decision_text"),str(core,"decided_by"),date(core,"decided_ms"),true,true,List.of("Matriz DDN y catálogos pendientes de aprobación","Finalizar evaluación del destino sigue siendo una decisión provisional; su reanudación por reapertura autorizada es explícita","Derivación aceptada confirmada por F04; promoción, rechazo y cancelación de derivación pendientes. Encaminamiento de vínculos solo a Requerimiento pendiente"));
    }
    public Caso caso(Operador a,UUID caseId){return lectura.execute(status->{actor(a);var row=caseRow(caseId);if(!canCase(a,row))throw new FalloTecnico(404);return caseView(row,true);});}
    private Caso caseView(Map<String,Object> c,boolean details){
        var caseId=uuid(c.get("id"));
        var actions=details?db.sql.queryForList("SELECT * FROM core_action WHERE case_id=? ORDER BY at_ms,id",id(caseId)).stream().map(r->new Actuacion(uuid(r.get("id")),str(r,"actor_id"),date(r,"at_ms"),str(r,"body_text"))).toList():List.<Actuacion>of();
        var reviews=details?db.sql.queryForList("SELECT * FROM core_review WHERE case_id=? ORDER BY at_ms,id",id(caseId)).stream().map(r->new Revision(uuid(r.get("id")),str(r,"actor_id"),date(r,"at_ms"),str(r,"decision"),str(r,"body_text"))).toList():List.<Revision>of();
        var assignments=details?db.sql.queryForList("SELECT * FROM core_assignment WHERE case_id=? ORDER BY at_ms,id",id(caseId)).stream().map(r->new AsignacionHistorica(str(r,"responsible_id"),str(r,"complexity_text"),str(r,"actor_id"),date(r,"at_ms"))).toList():List.<AsignacionHistorica>of();
        return new Caso(caseId,uuid(c.get("communication_id")),uuid(c.get("requirement_id")),uuid(c.get("destination_id")),str(c,"unit_code"),str(c,"site_id"),str(c,"responsible_id"),str(c,"complexity_text"),str(c,"state"),number(c,"revision"),actions,reviews,assignments,str(c,"complexity_code"),details?classificationHistory(caseId):List.of(),str(c,"closure_mode"),uuid(c.get("origin_case_id")),pendiente(caseId));
    }
}





