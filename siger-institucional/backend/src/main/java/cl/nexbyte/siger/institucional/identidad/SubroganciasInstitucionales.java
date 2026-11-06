package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Acceso;
import cl.nexbyte.siger.institucional.robustez.*;
import static cl.nexbyte.siger.institucional.identidad.AutoridadesInstitucionales.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;

/** Función temporal acreditada. No cambia cartera, tareas, autores o concesiones base. */
public final class SubroganciasInstitucionales {
    public record Solicitud(String designacion,Instant desde,Instant hasta,Instant revisionEn,String fundamento,UUID anterior){@Override public String toString(){return "SolicitudSubrogancia[reservada]";}}
    public record Decision(long revision,String fundamento){@Override public String toString(){return "DecisionSubrogancia[reservada]";}}
    public record Resultado(UUID id,long revision){}
    private final BaseJdbc db;
    private final IdentidadInstitucional ids;
    private final AutoridadesInstitucionales autoridad;
    private final JsonMapper json;
    private final Clock clock;
    SubroganciasInstitucionales(BaseJdbc db,IdentidadInstitucional ids,AutoridadesInstitucionales autoridad,JsonMapper json,Clock clock){this.db=db;this.ids=ids;this.autoridad=autoridad;this.json=json;this.clock=clock;}
    private String hash(Funcion f){return sha(json.writeValueAsBytes(f));}
    private String mandatoHash(Funcion f){return hash(new Funcion(f.id(),f.codigoFuncion(),f.ruta(),f.autoriza(),f.sede(),f.titular(),f.subrogantes(),f.autoridades(),f.ambitos(),f.desde(),f.hasta(),f.referencia(),f.sha256(),"ACTIVA"));}
    private static Acceso acceso(AmbitoFuncion a){return new Acceso(a.rol(),a.alcance(),a.unidad(),a.sede(),a.area());}
    private static boolean igual(Acceso a,AmbitoFuncion b){return a.rol().equals(b.rol())&&a.alcance().equals(b.alcance())&&Objects.equals(a.unidad(),b.unidad())&&Objects.equals(a.sede(),b.sede())&&Objects.equals(a.area(),b.area())&&a.materia()==null;}
    private static boolean contexto(Acceso a,Map<String,Object> s){
        if(!a.jefe()||a.alcance().equals("MATERIA"))return false;
        return a.cubre(str(s,"unit_code"),str(s,"site_id"),str(s,"area_id"));
    }
    private static boolean vigente(Funcion f,Instant t){return f.estado().equals("ACTIVA")&&!f.desde().isAfter(t)&&f.hasta().isAfter(t);}
    private boolean ambitoVigente(AmbitoFuncion a){try{if(ids.sedes().stream().noneMatch(s->s.id().equals(a.sede()))||a.unidad()!=null&&ids.presencias().stream().noneMatch(p->p.unidad().equals(a.unidad())&&p.sede().equals(a.sede())))return false;ids.validarConcesion(new IdentidadInstitucional.Otorgar(a.rol(),a.alcance(),a.unidad(),a.sede(),clock.instant(),clock.instant().plusMillis(1),a.area()));return true;}catch(FalloTecnico e){if(Set.of(400,403,409).contains(e.status))return false;throw e;}}
    private String huellaSolicitud(Map<String,Object> e){return sha(json.writeValueAsBytes(List.of("SUB_SOL_1",Arrays.asList(e.get("designation_id"),e.get("function_code"),e.get("designation_hash"),e.get("authority_revision"),e.get("titular_person"),e.get("substitute_person"),e.get("requested_by"),e.get("requested_person"),e.get("requested_ms"),e.get("reason_text"),e.get("from_ms"),e.get("until_ms"),e.get("review_ms"),e.get("previous_id")))));}
    private void integro(Map<String,Object> e){if(!huellaSolicitud(e).equals(e.get("request_hash")))throw new FalloTecnico(409,"SUBROGANCIA_ALTERADA");}
    private Map<String,Object> fila(UUID id){if(id==null)throw new FalloTecnico(400);var rows=db.sql.queryForList("SELECT * FROM institution_substitution WHERE id=?",id.toString());if(rows.size()!=1)throw new FalloTecnico(404);var e=rows.getFirst();integro(e);return e;}
    private Funcion documento(Map<String,Object> e){var f=lectorPrivado().readValue(str(e,"designation_doc"),Funcion.class);if(!hash(f).equals(e.get("designation_hash")))throw new FalloTecnico(409,"SUBROGANCIA_ALTERADA");return f;}
    private Funcion actual(ContextoFunciones c,Map<String,Object> e,boolean exigirHash){var f=c.funciones().stream().filter(x->x.id().equals(e.get("designation_id"))).findFirst().orElseThrow(()->new FalloTecnico(409,"DESIGNACION_NO_VIGENTE"));if(exigirHash&&!hash(f).equals(e.get("designation_hash")))throw new FalloTecnico(409,"DESIGNACION_NO_VIGENTE");return f;}
    private static void revision(Map<String,Object> e,long expected){if(expected<0||num(e,"revision")!=expected)throw new FalloTecnico(409,"VERSION_OBSOLETA");}
    private static void textoMotivo(String s){try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(s==null?null:s.replace('\n',' ').replace('\r',' '),2000,true);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}}
    private void epoch(String...personas){for(var p:new HashSet<>(Arrays.asList(personas)))if(p!=null)db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1 WHERE actor_id IN (SELECT actor_id FROM authority_binding WHERE batch_revision=(SELECT revision FROM authority_registry WHERE id=1) AND person_id=?)",p);}
    private List<Map<String,Object>> activas(){return db.sql.queryForList("SELECT * FROM institution_substitution WHERE state='EJECUTADA' AND from_ms<=? AND until_ms>? ORDER BY function_code,from_ms,id",clock.millis(),clock.millis());}
    private boolean aprobacionVigente(Funcion f,Map<String,Object> e){try{integro(e);var c=autoridad.contextoFunciones(str(e,"approved_by"));return c.persona().equals(e.get("approved_person"))&&f.autoridades().contains(c.persona());}catch(FalloTecnico ex){if(Set.of(401,403,404,409).contains(ex.status))return false;throw ex;}}
    private void aviso(UUID id,long rev,String code){db.sql.update("INSERT INTO workflow_event(event_key,code,resource_type,resource_id,at_ms) VALUES(?,'SUBROGANCIA_ACTUALIZADA','SUBROGANCIA',?,?)",id+":"+rev+":"+code,id.toString(),clock.millis());}

    /** Ámbitos conocidos sobreviven a omisiones del registro. No se resucita un rol
     * histórico como titular cuando falta la designación vigente. */
    public List<Acceso> aplicar(String actor,List<Acceso> base){
        var scopes=db.sql.queryForList("SELECT * FROM institution_function_scope");if(scopes.isEmpty())return base;
        var out=new ArrayList<>(base.stream().filter(a->scopes.stream().noneMatch(s->contexto(a,s))).toList());
        ContextoFunciones ctx;
        try{ctx=autoridad.contextoFunciones(actor);}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return out;throw denied;}
        var active=activas();
        for(var f:ctx.funciones())if(vigente(f,clock.instant())){
            var delegates=active.stream().filter(e->e.get("function_code").equals(f.codigoFuncion())).toList();
            if(delegates.isEmpty()){
                if(f.titular().equals(ctx.persona()))for(var a:f.ambitos())if(ambitoVigente(a)&&base.stream().anyMatch(b->igual(b,a)))out.add(acceso(a));
            }else if(delegates.size()==1){
                var e=delegates.getFirst();
                if(actor.equals(e.get("requested_by"))&&ctx.persona().equals(e.get("substitute_person"))&&hash(f).equals(e.get("designation_hash"))&&f.subrogantes().contains(ctx.persona())&&aprobacionVigente(f,e)&&e.get("executed_ms")!=null&&num(e,"executed_ms")<=clock.millis())for(var a:f.ambitos())if(ambitoVigente(a))out.add(acceso(a));
            }
        }
        return out.stream().distinct().toList();
    }
    void registrarAmbitos(List<Funcion> funciones,long revision){
        var personas=new HashSet<String>();for(var f:funciones){personas.add(f.titular());personas.addAll(f.subrogantes());personas.addAll(f.autoridades());}for(var e:db.sql.queryForList("SELECT titular_person,substitute_person,approved_person FROM institution_substitution"))for(String k:List.of("titular_person","substitute_person","approved_person"))if(e.get(k)!=null)personas.add(str(e,k));
        for(var f:funciones)for(var a:f.ambitos()){
            ids.validarConcesion(new IdentidadInstitucional.Otorgar(a.rol(),a.alcance(),a.unidad(),a.sede(),f.desde(),f.hasta(),a.area()));
            String digest=sha(json.writeValueAsBytes(a));var old=db.sql.queryForList("SELECT function_code FROM institution_function_scope WHERE scope_hash=?",digest);
            for(var scope:db.sql.queryForList("SELECT * FROM institution_function_scope WHERE site_id=?",a.sede()))if(Objects.equals(a.unidad(),scope.get("unit_code"))&&Objects.equals(a.area(),scope.get("area_id"))&&!f.codigoFuncion().equals(scope.get("function_code")))throw new FalloTecnico(409,"FUNCION_AMBITO_NO_REASIGNABLE");
            if(!old.isEmpty()&&!old.getFirst().get("function_code").equals(f.codigoFuncion()))throw new FalloTecnico(409,"FUNCION_AMBITO_NO_REASIGNABLE");
            if(old.isEmpty())db.sql.update("INSERT INTO institution_function_scope VALUES(?,?,?,?,?,?,?,?)",digest,f.codigoFuncion(),a.rol(),a.alcance(),a.unidad(),a.sede(),a.area(),revision);
        }
        for(var e:db.sql.queryForList("SELECT * FROM institution_substitution WHERE state IN ('APROBADA','EJECUTADA') AND until_ms>?",clock.millis())){
            var next=funciones.stream().filter(f->f.id().equals(e.get("designation_id"))).findFirst();
            if(next.isPresent()&&!mandatoHash(next.get()).equals(mandatoHash(documento(e))))throw new FalloTecnico(409,"FUNCION_REQUIERE_RESOLVER_SUBROGANCIA");
        }
        for(var p:personas)db.sql.update("UPDATE staff_account SET security_epoch=security_epoch+1 WHERE actor_id IN (SELECT actor_id FROM authority_binding WHERE batch_revision=? AND person_id=?)",revision,p);
    }
    private boolean visible(String actor,ContextoFunciones c,Map<String,Object> e){
        if(c.persona().equals(e.get("requested_person"))||c.persona().equals(e.get("titular_person"))||c.persona().equals(e.get("approved_person"))||c.persona().equals(e.get("executed_person")))return true;
        if(c.funciones().stream().anyMatch(f->f.id().equals(e.get("designation_id"))&&f.autoridades().contains(c.persona())))return true;
        return e.get("state").equals("APROBADA")&&ids.accesosBase(actor).stream().anyMatch(a->a.rol().equals("TI")&&a.alcance().equals("INSTITUCION"));
    }
    private void autorizarComando(String actor,ContextoFunciones ctx,String op,Map<String,Object> e){
        if(op.equals("SOLICITAR")||op.equals("RETIRAR")){if(!actor.equals(e.get("requested_by"))||!ctx.persona().equals(e.get("requested_person")))throw new FalloTecnico(404);return;}
        if(op.equals("RETORNAR")){var f=actual(ctx,e,false);if(!ctx.persona().equals(f.titular())||!ctx.persona().equals(e.get("titular_person")))throw new FalloTecnico(404);return;}
        if(op.equals("EJECUTAR")){
            if(ids.accesosBase(actor).stream().noneMatch(a->a.rol().equals("TI")&&a.alcance().equals("INSTITUCION")))throw new FalloTecnico(403);
            if(ctx.persona().equals(e.get("requested_person"))||ctx.persona().equals(e.get("approved_person")))throw new FalloTecnico(403,"SEPARACION_DE_FUNCIONES_REQUERIDA");return;
        }
        var f=actual(ctx,e,!op.equals("REVOCAR"));
        if(!f.autoridades().contains(ctx.persona()))throw new FalloTecnico(404);
        if(ctx.persona().equals(e.get("requested_person"))||ctx.persona().equals(e.get("titular_person")))throw new FalloTecnico(403,"SEPARACION_DE_FUNCIONES_REQUERIDA");
    }
    private Resultado comando(String actor,UUID key,String op,Object body,UUID resource,Supplier<Resultado> work){
        if(key==null)throw new FalloTecnico(400);String digest=sha(json.writeValueAsBytes(Arrays.asList("FUNCION_1",actor,op,resource,body)));
        try{return db.transaction(()->{
            autoridad.guard();var ctx=autoridad.contextoFunciones(actor);
            var old=db.sql.queryForList("SELECT * FROM function_command WHERE id=?",key.toString());
            if(!old.isEmpty()){
                var e=old.getFirst();if(!actor.equals(e.get("actor_id"))||!op.equals(e.get("action_code"))||!digest.equals(e.get("request_hash")))throw new FalloTecnico(409,"IDEMPOTENCIA_CONFLICTIVA");
                var id=UUID.fromString(str(e,"result_id"));autorizarComando(actor,ctx,op,fila(id));return new Resultado(id,num(e,"result_revision"));
            }
            var result=work.get();db.sql.update("INSERT INTO function_command VALUES(?,?,?,?,?,?)",key.toString(),actor,op,digest,result.id().toString(),result.revision());
            autoridad.evento(actor,result.id().toString(),"SUBROGANCIA_"+op,"CONFIRMADO",fila(result.id()));aviso(result.id(),result.revision(),op);return result;
        });}catch(FalloTecnico e){if(Set.of(403,404,409).contains(e.status))db.transaction(()->{autoridad.guard();autoridad.evento(actor,resource==null?null:resource.toString(),"SUBROGANCIA_"+op,"DENEGADO_"+e.status,null);return null;});throw e;}
    }
    public Resultado solicitar(String actor,UUID key,Solicitud r){
        if(r==null)throw new FalloTecnico(400);textoMotivo(r.fundamento());
        return comando(actor,key,"SOLICITAR",r,null,()->{
            var ctx=autoridad.contextoFunciones(actor);var f=ctx.funciones().stream().filter(x->x.id().equals(r.designacion())&&x.estado().equals("ACTIVA")).findFirst().orElseThrow(()->new FalloTecnico(409,"DESIGNACION_NO_VIGENTE"));
            if(!f.subrogantes().contains(ctx.persona()))throw new FalloTecnico(403,"SUBROGANTE_NO_DESIGNADO");
            Instant end=r.hasta();if(r.revisionEn()!=null&&(end==null||r.revisionEn().isBefore(end)))end=r.revisionEn();
            intervalo(r.desde(),end,f.hasta());if(r.desde().isBefore(f.desde())||!end.isAfter(clock.instant()))throw new FalloTecnico(400);
            if(r.anterior()!=null){var old=fila(r.anterior());if(!old.get("function_code").equals(f.codigoFuncion())||!old.get("substitute_person").equals(ctx.persona())||!Set.of("EJECUTADA","EXPIRADA","REVISION_REQUERIDA").contains(old.get("state"))||r.desde().toEpochMilli()<num(old,"until_ms"))throw new FalloTecnico(409,"PRORROGA_REQUIERE_NUEVO_PERIODO");}
            long at=clock.millis();Long review=r.revisionEn()==null?null:r.revisionEn().toEpochMilli();String previous=r.anterior()==null?null:r.anterior().toString();
            var sealed=new LinkedHashMap<String,Object>();sealed.put("designation_id",f.id());sealed.put("function_code",f.codigoFuncion());sealed.put("designation_hash",hash(f));sealed.put("authority_revision",ctx.revision());sealed.put("titular_person",f.titular());sealed.put("substitute_person",ctx.persona());sealed.put("requested_by",actor);sealed.put("requested_person",ctx.persona());sealed.put("requested_ms",at);sealed.put("reason_text",r.fundamento());sealed.put("from_ms",r.desde().toEpochMilli());sealed.put("until_ms",end.toEpochMilli());sealed.put("review_ms",review);sealed.put("previous_id",previous);
            UUID id=UUID.randomUUID();db.sql.update("INSERT INTO institution_substitution(id,designation_id,function_code,designation_hash,designation_doc,authority_revision,titular_person,substitute_person,requested_by,requested_person,requested_ms,reason_text,request_hash,from_ms,until_ms,review_ms,state,revision,previous_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDIENTE',0,?)",id.toString(),f.id(),f.codigoFuncion(),hash(f),json.writeValueAsString(f),ctx.revision(),f.titular(),ctx.persona(),actor,ctx.persona(),at,r.fundamento(),huellaSolicitud(sealed),r.desde().toEpochMilli(),end.toEpochMilli(),review,previous);return new Resultado(id,0);
        });
    }
    public Resultado decidir(String actor,UUID key,UUID id,Decision r,boolean aprobar){
        if(r==null)throw new FalloTecnico(400);textoMotivo(r.fundamento());String op=aprobar?"APROBAR":"RECHAZAR";
        return comando(actor,key,op,r,id,()->{
            var ctx=autoridad.contextoFunciones(actor);var e=fila(id);autorizarComando(actor,ctx,op,e);revision(e,r.revision());
            if(!e.get("state").equals("PENDIENTE"))throw new FalloTecnico(409,"ESTADO_INSTRUCCION_INCOMPATIBLE");
            if(aprobar){
                var f=actual(ctx,e,true);if(!f.estado().equals("ACTIVA")||num(e,"until_ms")<=clock.millis())throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");
                long conflicts=db.sql.queryForObject("SELECT COUNT(*) FROM institution_substitution WHERE function_code=? AND state IN ('APROBADA','EJECUTADA') AND from_ms<? AND until_ms>?",Long.class,e.get("function_code"),e.get("until_ms"),Math.max(clock.millis(),num(e,"from_ms")));
                if(conflicts>0)throw new FalloTecnico(409,"SUBROGANCIA_SOLAPADA");
                if(db.sql.queryForObject("SELECT COUNT(*) FROM institution_substitution WHERE requested_person=? AND approved_person=? AND state<>'RECHAZADA' AND approved_ms IS NOT NULL AND from_ms<? AND until_ms>?",Long.class,ctx.persona(),e.get("requested_person"),e.get("until_ms"),Math.max(clock.millis(),num(e,"from_ms")))>0)throw new FalloTecnico(403,"APROBACION_CRUZADA_NO_PERMITIDA");
            }
            db.sql.update("UPDATE institution_substitution SET state=?,revision=revision+1,approved_by=?,approved_person=?,approved_ms=?,decision_text=? WHERE id=?",aprobar?"APROBADA":"RECHAZADA",actor,ctx.persona(),clock.millis(),r.fundamento(),id.toString());return new Resultado(id,r.revision()+1);
        });
    }
    public Resultado ejecutar(String actor,UUID key,UUID id,long rev){return comando(actor,key,"EJECUTAR",rev,id,()->{
        var ctx=autoridad.contextoFunciones(actor);var e=fila(id);autorizarComando(actor,ctx,"EJECUTAR",e);revision(e,rev);
        var f=actual(ctx,e,true);if(!e.get("state").equals("APROBADA")||!f.estado().equals("ACTIVA")||num(e,"until_ms")<=clock.millis()||!aprobacionVigente(f,e))throw new FalloTecnico(409,"APROBACION_NO_VIGENTE");
        // El beneficiario debe seguir ligado a esa persona y credencial; el TI no elige otro ámbito.
        var beneficiary=autoridad.contextoFunciones(str(e,"requested_by"));if(!beneficiary.persona().equals(e.get("substitute_person")))throw new FalloTecnico(409,"IDENTIDAD_INSTITUCIONAL_NO_ACREDITADA");
        boolean inmediata=num(e,"from_ms")<=clock.millis();db.sql.update("UPDATE institution_substitution SET state='EJECUTADA',revision=revision+1,executed_by=?,executed_person=?,executed_ms=?,activated_ms=? WHERE id=?",actor,ctx.persona(),clock.millis(),inmediata?clock.millis():null,id.toString());epoch(str(e,"titular_person"),str(e,"substitute_person"));
        if(inmediata){autoridad.evento(actor,id.toString(),"SUBROGANCIA_ACTIVADA","EFECTIVA",Map.of("efectivaDesde",clock.millis(),"registradaEn",clock.millis()));aviso(id,rev+1,"ACTIVADA");}return new Resultado(id,rev+1);
    });}
    public Resultado terminar(String actor,UUID key,UUID id,Decision r,String op){
        if(r==null||!Set.of("REVOCAR","RETORNAR","RETIRAR").contains(op))throw new FalloTecnico(400);textoMotivo(r.fundamento());
        return comando(actor,key,op,r,id,()->{
            var ctx=autoridad.contextoFunciones(actor);var e=fila(id);autorizarComando(actor,ctx,op,e);revision(e,r.revision());
            String required=op.equals("RETIRAR")?"PENDIENTE":null;
            if(required!=null?!required.equals(e.get("state")):!Set.of("APROBADA","EJECUTADA").contains(e.get("state")))throw new FalloTecnico(409,"ESTADO_INSTRUCCION_INCOMPATIBLE");
            String state=op.equals("RETORNAR")?"FINALIZADA":op.equals("RETIRAR")?"RETIRADA":"REVOCADA";
            db.sql.update("UPDATE institution_substitution SET state=?,revision=revision+1,ended_by=?,ended_ms=?,end_reason=? WHERE id=?",state,actor,clock.millis(),r.fundamento(),id.toString());epoch(str(e,"titular_person"),str(e,"substitute_person"));return new Resultado(id,r.revision()+1);
        });
    }
    public void temporales(){db.transaction(()->{
        autoridad.guard();
        for(var e:db.sql.queryForList("SELECT * FROM institution_substitution WHERE state='EJECUTADA' ORDER BY from_ms,id")){
            if(num(e,"until_ms")<=clock.millis()){
                String next=e.get("review_ms")!=null&&num(e,"review_ms")==num(e,"until_ms")?"REVISION_REQUERIDA":"EXPIRADA";
                db.sql.update("UPDATE institution_substitution SET state=?,revision=revision+1,ended_ms=? WHERE id=?",next,num(e,"until_ms"),e.get("id"));epoch(str(e,"titular_person"),str(e,"substitute_person"));autoridad.evento(null,str(e,"id"),"SUBROGANCIA_"+next,"EFECTIVA",Map.of("efectivaHasta",num(e,"until_ms"),"registradaEn",clock.millis()));aviso(UUID.fromString(str(e,"id")),num(e,"revision")+1,next);
            }else if(num(e,"from_ms")<=clock.millis()&&e.get("activated_ms")==null){db.sql.update("UPDATE institution_substitution SET activated_ms=? WHERE id=?",Math.max(num(e,"from_ms"),num(e,"executed_ms")),e.get("id"));epoch(str(e,"titular_person"),str(e,"substitute_person"));autoridad.evento(null,str(e,"id"),"SUBROGANCIA_ACTIVADA","EFECTIVA",Map.of("efectivaDesde",Math.max(num(e,"from_ms"),num(e,"executed_ms")),"registradaEn",clock.millis()));aviso(UUID.fromString(str(e,"id")),num(e,"revision"),"ACTIVADA");}
        }
        return null;
    });}
    public Map<String,Object> bandeja(String actor){
        temporales();return db.transaction(()->{
            autoridad.guard();var ctx=autoridad.contextoFunciones(actor);var functions=ctx.funciones().stream().filter(f->f.titular().equals(ctx.persona())||f.subrogantes().contains(ctx.persona())||f.autoridades().contains(ctx.persona())).toList();
            var records=new ArrayList<Map<String,Object>>();
            for(var e:db.sql.queryForList("SELECT * FROM institution_substitution ORDER BY requested_ms DESC,id LIMIT 500"))if(visible(actor,ctx,e)){
                integro(e);var out=salida(e);out.remove("designation_doc");
                var designation=documento(e);out.put("ambitos",designation.ambitos());out.put("referencia",designation.referencia());out.put("ruta",designation.ruta());
                // La administración técnica recibe la instrucción, no el relato de ausencia ni el registro firmado completo.
                boolean institucional=ctx.persona().equals(e.get("requested_person"))||ctx.persona().equals(e.get("titular_person"))||ctx.funciones().stream().anyMatch(f->f.id().equals(e.get("designation_id"))&&f.autoridades().contains(ctx.persona()));
                if(!institucional){out.remove("reason_text");out.remove("decision_text");out.remove("end_reason");}
                out.put("historial",db.sql.queryForList("SELECT actor_id,at_ms,action_code,result_code FROM authority_event WHERE resource_id=? ORDER BY seq",e.get("id")).stream().map(AutoridadesInstitucionales::salida).toList());
                out.put("puedeAprobar",puede(actor,ctx,"APROBAR",e));out.put("puedeEjecutar",puede(actor,ctx,"EJECUTAR",e));out.put("puedeRevocar",puede(actor,ctx,"REVOCAR",e));out.put("puedeRetornar",ctx.persona().equals(e.get("titular_person")));records.add(out);
            }
            return Map.of("persona",ctx.persona(),"funciones",functions,"solicitudes",records);
        });
    }
    private boolean puede(String actor,ContextoFunciones ctx,String op,Map<String,Object> e){try{autorizarComando(actor,ctx,op,e);return true;}catch(FalloTecnico denied){if(Set.of(401,403,404,409).contains(denied.status))return false;throw denied;}}
    public Funcion titularRegional(String actor,String unidad,String sede,String area){
        var ctx=autoridad.contextoFunciones(actor);
        return ctx.funciones().stream().filter(f->f.ruta().equals("REGIONAL")&&f.titular().equals(ctx.persona())&&vigente(f,clock.instant())&&f.ambitos().stream().map(SubroganciasInstitucionales::acceso).anyMatch(a->a.cubre(unidad,sede,area))&&activas().stream().noneMatch(e->e.get("function_code").equals(f.codigoFuncion()))).findFirst().orElseThrow(()->new FalloTecnico(403,"TITULAR_REGIONAL_NO_ACREDITADO"));
    }
    /** Valida el acto de origen ya emitido, no concede permiso para emitir actos nuevos.
     * Conserva la decisión funcional durante ausencia/retorno; una baja ordinaria de rol no se suple. */
    public boolean actoConservado(String actor,String unidad,String sede,String area,long at){
        var ctx=autoridad.contextoFunciones(actor);
        for(var f:ctx.funciones())if(vigente(f,clock.instant())&&f.ambitos().stream().anyMatch(a->acceso(a).cubre(unidad,sede,area)&&ambitoVigente(a))){
            if(f.titular().equals(ctx.persona())&&ids.accesosBase(actor).stream().anyMatch(a->a.jefe()&&a.cubre(unidad,sede,area))&&activas().stream().anyMatch(e->e.get("function_code").equals(f.codigoFuncion())))return true;
            for(var e:db.sql.queryForList("SELECT * FROM institution_substitution WHERE function_code=? AND requested_by=? AND executed_ms IS NOT NULL AND from_ms<=? AND until_ms>?",f.codigoFuncion(),actor,at,at))if(ctx.persona().equals(e.get("substitute_person"))&&hash(f).equals(e.get("designation_hash"))&&num(e,"executed_ms")<=at&&(e.get("ended_ms")==null||num(e,"ended_ms")>at))return true;
        }
        return false;
    }
    public boolean autorizarAviso(String actor,UUID id){try{var c=autoridad.contextoFunciones(actor);var e=fila(id);return c.persona().equals(e.get("requested_person"))||c.persona().equals(e.get("titular_person"))||c.funciones().stream().anyMatch(f->f.id().equals(e.get("designation_id"))&&f.autoridades().contains(c.persona()));}catch(FalloTecnico ex){if(Set.of(401,403,404,409).contains(ex.status))return false;throw ex;}}
}
