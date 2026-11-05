package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.CatalogoMaterias;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Decisiones por recurso; compartir agrupador, referencia o materia nunca sustituye una autorización. */
public final class ContenidoInstitucional {
    public record Pertinencia(long revision,List<UUID> casos,boolean contenido,String fundamento) {}
    public record Valoracion(long revision,long anterior,String decision,String fundamento) {}
    public record Proteccion(long revision,String clasificacion,String fundamento) {}
    public record Publicacion(long revision,Instant desde,Instant hasta,boolean contenido,boolean descarga,String estado,String fundamento) {}
    public record Permisos(boolean contenido,boolean descarga,boolean administrar,String clasificacion,boolean restringir) {
        Permisos(boolean c,boolean d,boolean a,String k){this(c,d,a,k,a);}
    }
    private final BaseJdbc db;
    private final NucleoInstitucional core;
    private final Clock clock;
    private final JsonMapper json;
    private final PoliticaAreas areas;
    ContenidoInstitucional(BaseJdbc db,NucleoInstitucional core,Clock clock,JsonMapper json) {
        this.db=db;this.core=core;this.clock=clock;this.json=json;areas=new PoliticaAreas(db,clock);
    }
    private static String id(UUID v){if(v==null)throw new FalloTecnico(400);return v.toString();}
    private static long n(Map<String,Object> r,String k){return r.get(k)==null?0:((Number)r.get(k)).longValue();}
    private static UUID uuid(Object v){return v==null?null:UUID.fromString(v.toString());}
    private void motivo(String v,int max){cl.nexbyte.siger.contrato.DeclaracionNna.texto(v,max,true);}
    private void guard(Operador a){db.sql.queryForObject("SELECT id FROM identity_guard WHERE id=1 FOR UPDATE",Integer.class);core.bloquearAcceso(a);}
    public void bloquear(Operador a){guard(a);}
    private void locks(UUID... ids){Arrays.stream(ids).filter(Objects::nonNull).distinct().sorted().forEach(core::lock);}
    private boolean gestor(Operador a,UUID caso){try{core.autorizarCaso(a,caso);return true;}catch(FalloTecnico e){if(e.status==404)return false;throw e;}}
    private long secuencia(String tabla,UUID caso){return db.sql.queryForObject("SELECT COALESCE(MAX(seq),0) FROM "+tabla+" WHERE case_id=?",Long.class,id(caso));}
    private boolean contextoVigente(Map<String,Object> target,UUID caso){
        return n(target,"area_seq")==secuencia("case_area_history",caso)&&n(target,"matter_seq")==secuencia("case_matter_history",caso);
    }
    private Map<String,Object> requerimiento(UUID req){
        var rows=db.sql.queryForList("SELECT * FROM core_requirement WHERE id=?",id(req));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private Map<String,Object> principal(UUID req){
        var rows=db.sql.queryForList("SELECT * FROM authority_requirement_principal WHERE requirement_id=? ORDER BY batch_revision DESC LIMIT 1",id(req));
        if(rows.isEmpty())throw new FalloTecnico(409,"PRINCIPAL_REQ_NO_ACREDITADO");
        var p=rows.getFirst();if(!"ACTIVA".equals(p.get("status"))||n(p,"valid_from")>clock.millis()||n(p,"valid_until")<=clock.millis())throw new FalloTecnico(409,"PRINCIPAL_REQ_NO_ACREDITADO");return p;
    }
    private Map<String,Object> jefaturaPrincipal(Operador a,UUID req){
        core.bloquearAcceso(a);var p=principal(req);String ar=(String)p.get("area_id");
        if(ar!=null&&!areas.vigente(ar,p.get("unit_code").toString(),p.get("site_id").toString())||!a.jefeEn(p.get("unit_code").toString(),p.get("site_id").toString(),ar))throw new FalloTecnico(404);
        return p;
    }
    private Map<String,Object> vinculo(UUID req,UUID child){
        var rows=db.sql.queryForList("SELECT * FROM core_link WHERE child_id=? AND requirement_reference=?",id(child),id(req));
        if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    public Map<String,Object> contextoReq(Operador a,UUID req){
        return db.transaction(()->{guard(a);var p=jefaturaPrincipal(a,req);var r=requerimiento(req);
            var casos=db.sql.queryForList("SELECT c.id,c.state,d.unit_code,d.site_id,m.matter_id FROM core_case c JOIN core_destination d ON d.id=c.destination_id LEFT JOIN case_matter m ON m.case_id=c.id WHERE c.requirement_id=? ORDER BY c.id",id(req));
            var hijas=db.sql.queryForList("SELECT l.child_id,COALESCE(p.revision,0) AS revision FROM core_link l LEFT JOIN communication_pertinence p ON p.child_id=l.child_id WHERE l.requirement_reference=? ORDER BY l.child_id",id(req));
            return Map.of("requerimiento",req,"comunicacion",r.get("communication_id"),"unidad",p.get("unit_code"),"sede",p.get("site_id"),"casos",normalizar(casos),"hijas",normalizar(hijas));});
    }
    private List<Map<String,Object>> normalizar(List<Map<String,Object>> rows){return rows.stream().map(r->{Map<String,Object> m=new LinkedHashMap<>();r.forEach((k,v)->m.put(k.toLowerCase(Locale.ROOT),v));return m;}).toList();}
    public Map<String,Object> propuesta(Operador a,UUID req,UUID child){
        return db.transaction(()->{guard(a);jefaturaPrincipal(a,req);vinculo(req,child);
            return Map.of("contenido",originalAutorizado(a,child,null),"historial",normalizar(db.sql.queryForList("SELECT revision,actor_id,at_ms,reason_text,snapshot_doc FROM communication_pertinence_history WHERE child_id=? ORDER BY seq DESC",id(child))));});
    }
    public Resultado determinar(Operador a,UUID key,UUID req,UUID child,Pertinencia request){
        if(request==null||request.casos()==null||request.casos().size()>32||new HashSet<>(request.casos()).size()!=request.casos().size())throw new FalloTecnico(400);
        motivo(request.fundamento(),1000);jefaturaPrevia(a,req);
        return core.execute(key,a,"PERTINENCIA_COM",List.of(req,child,request),()->{guard(a);
            var r=requerimiento(req);locks(uuid(r.get("communication_id")),child);var p=jefaturaPrincipal(a,req);vinculo(req,child);
            long revision=db.sql.queryForObject("SELECT COALESCE(MAX(revision),0) FROM communication_pertinence WHERE child_id=?",Long.class,id(child));
            if(revision!=request.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");
            var targets=new ArrayList<Map<String,Object>>();
            for(var cid:request.casos()){
                var c=core.caseRow(cid);if(!id(req).equals(c.get("requirement_id")))throw new FalloTecnico(400);
                String materia=new PoliticaMaterias(db,clock).materia(cid);if(materia!=null)new CatalogoMaterias(db,clock).exigir(materia);
                var t=new LinkedHashMap<String,Object>();t.put("caso",cid);t.put("materia",materia);t.put("areaSeq",secuencia("case_area_history",cid));t.put("materiaSeq",secuencia("case_matter_history",cid));targets.add(t);
            }
            long next=revision+1;
            if(revision==0)db.sql.update("INSERT INTO communication_pertinence VALUES(?,?,?,?,?,?)",id(child),next,request.contenido(),a.id(),clock.millis(),request.fundamento());
            else db.sql.update("UPDATE communication_pertinence SET revision=?,content_allowed=?,actor_id=?,at_ms=?,reason_text=? WHERE child_id=?",next,request.contenido(),a.id(),clock.millis(),request.fundamento(),id(child));
            db.sql.update("DELETE FROM communication_pertinence_target WHERE child_id=?",id(child));
            for(var t:targets)db.sql.update("INSERT INTO communication_pertinence_target VALUES(?,?,?,?,?)",id(child),t.get("caso").toString(),t.get("materia"),t.get("areaSeq"),t.get("materiaSeq"));
            db.sql.update("INSERT INTO communication_pertinence_history(child_id,revision,actor_id,at_ms,reason_text,snapshot_doc,principal_batch) VALUES(?,?,?,?,?,?,?)",id(child),next,a.id(),clock.millis(),request.fundamento(),json.writeValueAsString(Map.of("decision",request,"alcances",targets)),p.get("batch_revision"));
            core.event(uuid(r.get("communication_id")),"PERTINENCIA",child,a,"PERTINENCIA_COM_DETERMINADA");
            for(UUID cid:request.casos())core.event(uuid(r.get("communication_id")),"CASO",cid,a,"ANTECEDENTE_PERTINENTE");
            return new Resultado(child,next);
        });
    }
    private void jefaturaPrevia(Operador a,UUID req){db.transaction(()->{guard(a);jefaturaPrincipal(a,req);return null;});}
    boolean gestionada(UUID child){return db.sql.queryForObject("SELECT COUNT(*) FROM communication_pertinence WHERE child_id=?",Long.class,id(child))>0;}
    private Map<String,Object> objetivo(Operador a,UUID caso,UUID child){
        core.autorizarConsultaCaso(a,caso);
        var rows=db.sql.queryForList("SELECT t.*,p.revision,p.content_allowed FROM communication_pertinence_target t JOIN communication_pertinence p ON p.child_id=t.child_id WHERE t.case_id=? AND t.child_id=?",id(caso),id(child));
        if(rows.isEmpty()||!contextoVigente(rows.getFirst(),caso))throw new FalloTecnico(404);return rows.getFirst();
    }
    boolean comEnCaso(Operador a,UUID caso,UUID child,boolean contenido){
        try{var t=objetivo(a,caso,child);return !contenido||Boolean.TRUE.equals(t.get("content_allowed"));}catch(FalloTecnico e){if(e.status==404)return false;throw e;}
    }
    public List<Map<String,Object>> antecedentes(Operador a,UUID caso){
        return db.transaction(()->{guard(a);core.autorizarConsultaCaso(a,caso);var result=new ArrayList<Map<String,Object>>();
            for(var r:db.sql.queryForList("SELECT child_id FROM communication_pertinence_target WHERE case_id=? ORDER BY child_id",id(caso))){
                UUID child=uuid(r.get("child_id"));if(!comEnCaso(a,caso,child,false))continue;var t=objetivo(a,caso,child);
                var item=new LinkedHashMap<String,Object>();item.put("comunicacion",child);item.put("revision",n(t,"revision"));item.put("contenido",Boolean.TRUE.equals(t.get("content_allowed"))?originalAutorizado(a,child,caso):null);
                item.put("valoraciones",gestor(a,caso)?normalizar(db.sql.queryForList("SELECT seq,pertinence_revision,actor_id,at_ms,decision,reason_text,supersedes_seq FROM case_evidence_assessment WHERE child_id=? AND case_id=? ORDER BY seq DESC",id(child),id(caso))):List.of());result.add(item);
            }
            new PoliticaMaterias(db,clock).auditar(a,id(caso),"CONSULTAR_PERTINENCIA");return result;});
    }
    public Resultado valorar(Operador a,UUID key,UUID caso,UUID child,Valoracion r){
        if(r==null||r.decision()==null||!Set.of("ADMITIDA","NO_ADMITIDA").contains(r.decision()))throw new FalloTecnico(400);motivo(r.fundamento(),2000);
        core.autorizarCaso(a,caso);
        return core.execute(key,a,"VALORAR_COM_CASO",List.of(caso,child,r),()->{guard(a);
            var c=core.caseRow(caso);locks(uuid(c.get("communication_id")),child);core.autorizarCaso(a,caso);
            if(!a.id().equals(c.get("responsible_id"))||!areas.profesional(a,c))throw new FalloTecnico(404);
            var t=objetivo(a,caso,child);if(n(t,"revision")!=r.revision()||!Boolean.TRUE.equals(t.get("content_allowed")))throw new FalloTecnico(409);
            long old=db.sql.queryForObject("SELECT COALESCE(MAX(seq),0) FROM case_evidence_assessment WHERE child_id=? AND case_id=?",Long.class,id(child),id(caso));
            if(old!=r.anterior())throw new FalloTecnico(409,"VERSION_OBSOLETA");
            db.sql.update("INSERT INTO case_evidence_assessment(child_id,case_id,pertinence_revision,actor_id,at_ms,decision,reason_text,supersedes_seq) VALUES(?,?,?,?,?,?,?,?)",id(child),id(caso),r.revision(),a.id(),clock.millis(),r.decision(),r.fundamento(),old==0?null:old);
            long seq=db.sql.queryForObject("SELECT MAX(seq) FROM case_evidence_assessment WHERE child_id=? AND case_id=?",Long.class,id(child),id(caso));
            core.event(uuid(c.get("communication_id")),"CASO",caso,a,"COM_VALORADA_EN_CONTEXTO");return new Resultado(child,seq);
        });
    }
    public void registrarDocumento(Operador a,UUID caso,UUID doc){
        core.autorizarGestionDocumental(a,caso);
        db.sql.update("INSERT INTO document_content_policy VALUES(?,?,'SIN_CLASIFICAR',0)",id(doc),id(caso));
    }
    private Map<String,Object> documento(UUID doc){
        var rows=db.sql.queryForList("SELECT * FROM institution_document WHERE id=?",id(doc));if(rows.isEmpty())throw new FalloTecnico(404);return rows.getFirst();
    }
    private Map<String,Object> politica(UUID doc){var rows=db.sql.queryForList("SELECT * FROM document_content_policy WHERE document_id=?",id(doc));return rows.isEmpty()?Map.of():rows.getFirst();}
    private boolean propietario(Operador a,UUID owner){var c=core.caseRow(owner);return areas.jefe(a,c)||a.id().equals(c.get("responsible_id"))&&areas.profesional(a,c);}
    private void asociado(Operador a,UUID caso,Map<String,Object> d){
        if(caso==null){core.autorizarLectura(a,uuid(d.get("communication_id")));if(!"INGRESO_CIUDADANO".equals(d.get("origin")))throw new FalloTecnico(404);return;}
        core.autorizarConsultaCaso(a,caso);boolean relation;
        if("GESTION_INSTITUCIONAL".equals(d.get("origin")))relation=db.sql.queryForObject("SELECT COUNT(*) FROM management_document_link WHERE document_id=? AND case_id=?",Long.class,d.get("id"),id(caso))>0;
        else {var c=core.caseRow(caso);relation=c.get("communication_id").equals(d.get("communication_id"))||comEnCaso(a,caso,uuid(d.get("communication_id")),false);
            if(!relation&&gestor(a,caso)&&!gestionada(uuid(d.get("communication_id"))))relation=db.sql.queryForObject("SELECT COUNT(*) FROM core_link WHERE child_id=? AND case_reference=?",Long.class,d.get("communication_id"),id(caso))==1;
        }
        if(!relation)throw new FalloTecnico(404);
    }
    public Permisos permisos(Operador a,UUID caso,UUID doc){
        var result=db.transaction(()->{guard(a);try{return evaluarPermisos(a,caso,doc);}catch(FalloTecnico e){if(e.status==404)return null;throw e;}});
        if(result==null)throw new FalloTecnico(404);return result;
    }
    private Permisos evaluarPermisos(Operador a,UUID caso,UUID doc){
        var d=documento(doc);asociado(a,caso,d);var p=politica(doc);boolean managed=caso==null||gestor(a,caso);
        if(managed&&caso!=null&&"INGRESO_CIUDADANO".equals(d.get("origin"))&&!core.caseRow(caso).get("communication_id").equals(d.get("communication_id"))&&gestionada(uuid(d.get("communication_id")))){
            try{core.autorizarLectura(a,uuid(d.get("communication_id")));}catch(FalloTecnico e){if(e.status==404)managed=false;else throw e;}
        }
            if(p.isEmpty()){if(!managed)throw new FalloTecnico(404);boolean admin=administrableHistorico(a,caso,doc);return new Permisos(true,true,admin,"HISTORICO_SIN_ACREDITAR",admin||propietarioHistorico(a,caso,doc));}
            UUID owner=uuid(p.get("owner_case"));boolean propio=propietario(a,owner);boolean head=areas.jefe(a,core.caseRow(owner));String kind=p.get("classification").toString();
            boolean restringir=propio&&Objects.equals(caso,owner);
            if(kind.equals("BLOQUEADO")){if(!propio)throw new FalloTecnico(404);return new Permisos(false,false,head&&Objects.equals(caso,owner),kind,restringir);}
            if(kind.equals("FIR")){if(!propio)throw new FalloTecnico(404);return new Permisos(true,true,head&&Objects.equals(caso,owner),kind,restringir);}
            if(managed)return new Permisos(true,true,head&&Objects.equals(caso,owner),kind,restringir);
            var rows=db.sql.queryForList("SELECT * FROM document_disclosure WHERE document_id=? AND case_id=?",id(doc),id(caso));
            if(!kind.equals("ORDINARIO")||rows.isEmpty())throw new FalloTecnico(404);
            var v=rows.getFirst();
            if(!"ACTIVA".equals(v.get("status"))||n(v,"policy_revision")!=n(p,"revision")||n(v,"valid_from")>clock.millis()||n(v,"valid_until")<=clock.millis()||!contextoVigente(v,caso)||!Objects.equals(v.get("matter_id"),new PoliticaMaterias(db,clock).materia(caso)))throw new FalloTecnico(404);
            return new Permisos(Boolean.TRUE.equals(v.get("content_allowed")),Boolean.TRUE.equals(v.get("download_allowed")),false,kind);
    }
    private boolean administrableHistorico(Operador a,UUID caso,UUID doc){
        if(caso==null||!areas.jefe(a,core.caseRow(caso)))return false;
        var d=documento(doc);
        if("GESTION_INSTITUCIONAL".equals(d.get("origin"))){var owners=db.sql.queryForList("SELECT DISTINCT case_id FROM document_event WHERE document_id=? AND event_code='DOCUMENTO_CREADO'",id(doc));return owners.size()==1&&id(caso).equals(owners.getFirst().get("case_id"));}
        try{var p=principal(uuid(core.caseRow(caso).get("requirement_id")));return a.jefeEn(p.get("unit_code").toString(),p.get("site_id").toString(),(String)p.get("area_id"));}catch(FalloTecnico e){if(e.status==409)return false;throw e;}
    }
    private boolean propietarioHistorico(Operador a,UUID caso,UUID doc){
        if(caso==null||!propietario(a,caso)||!"GESTION_INSTITUCIONAL".equals(documento(doc).get("origin")))return false;
        var owners=db.sql.queryForList("SELECT DISTINCT case_id FROM document_event WHERE document_id=? AND event_code='DOCUMENTO_CREADO'",id(doc));return owners.size()==1&&id(caso).equals(owners.getFirst().get("case_id"));
    }
    /** Proyección de consulta: nunca cambia los bytes firmados ni expone descriptores de adjuntos excluidos. */
    Contenido originalAutorizado(Operador a,UUID com,UUID caso){
        var c=core.contenidoOriginal(com);if(c.formularioCompleto()==null)return c;
        var copy=(tools.jackson.databind.node.ObjectNode)c.formularioCompleto().deepCopy();var visible=copy.arrayNode();var referencias=copy.arrayNode();
        for(var attachment:c.formularioCompleto().path("adjuntos")){
            var rows=db.sql.queryForList("SELECT id FROM institution_document WHERE communication_id=? AND attachment_id=? AND origin='INGRESO_CIUDADANO'",id(com),attachment.path("id").asString());
            if(rows.size()!=1)throw new FalloTecnico(409);
            try{evaluarPermisos(a,caso,uuid(rows.getFirst().get("id")));visible.add(attachment.deepCopy());referencias.add(attachment.path("id").asString());}catch(FalloTecnico e){if(e.status!=404)throw e;}
        }
        copy.set("adjuntos",visible);((tools.jackson.databind.node.ObjectNode)copy.path("datos")).set("adjuntos",referencias);return new Contenido(c.texto(),c.tipoIdentidad(),c.identidad(),c.declaracionNna(),c.recibidaEnDeclarada(),c.canalDeclarado(),c.registradoPor(),copy);
    }
    public void contextoIngreso(Operador a,UUID com,UUID caso){db.transaction(()->{guard(a);asociado(a,caso,Map.of("origin","INGRESO_CIUDADANO","communication_id",id(com)));return null;});}
    public void autorizarDocumento(Operador a,UUID caso,UUID doc,String accion){
        try{comprobarAccion(a,caso,doc,accion);}catch(FalloTecnico e){denegacion(a,doc,"DOCUMENTO_DENEGADO",e);throw e;}
        new PoliticaMaterias(db,clock).auditar(a,id(doc),"DOCUMENTO_"+accion);
    }
    /** Reevaluación interna antes de cada bloque; no simula una nueva apertura o descarga. */
    public void revalidarDocumento(Operador a,UUID caso,UUID doc,String accion){try{comprobarAccion(a,caso,doc,accion);}catch(FalloTecnico e){denegacion(a,doc,"DOCUMENTO_REVOCADO",e);throw e;}}
    private void denegacion(Operador a,UUID doc,String codigo,FalloTecnico e){if(Set.of(403,404).contains(e.status))new PoliticaMaterias(db,clock).auditar(a,id(doc),codigo);}
    private void comprobarAccion(Operador a,UUID caso,UUID doc,String accion){
        var p=permisos(a,caso,doc);
        if(!Set.of("METADATOS","CONTENIDO","DESCARGA","HISTORIAL").contains(accion))throw new FalloTecnico(400);
        if(accion.equals("CONTENIDO")&&!p.contenido()||accion.equals("DESCARGA")&&!p.descarga()||accion.equals("HISTORIAL")&&(caso==null||!gestor(a,caso)))throw new FalloTecnico(404);
    }
    private UUID ownerAcreditable(Operador a,UUID caso,UUID doc){
        var p=politica(doc);if(!p.isEmpty()){var owner=uuid(p.get("owner_case"));core.head(a,core.caseRow(owner));if(!owner.equals(caso))throw new FalloTecnico(404);return owner;}
        core.head(a,core.caseRow(caso));var d=documento(doc);asociado(a,caso,d);
        if("GESTION_INSTITUCIONAL".equals(d.get("origin"))){
            var owners=db.sql.queryForList("SELECT DISTINCT case_id FROM document_event WHERE document_id=? AND event_code='DOCUMENTO_CREADO'",id(doc));
            if(owners.size()!=1||!id(caso).equals(owners.getFirst().get("case_id")))throw new FalloTecnico(409,"PROPIETARIO_DOCUMENTAL_NO_ACREDITADO");
        }else {jefaturaPrincipal(a,uuid(core.caseRow(caso).get("requirement_id")));core.autorizarLectura(a,uuid(d.get("communication_id")));}
        return caso;
    }
    private void restringirPropio(Operador a,UUID caso,UUID doc){
        var p=politica(doc);if(p.isEmpty()){if(!propietarioHistorico(a,caso,doc))ownerAcreditable(a,caso,doc);}
        else if(!id(caso).equals(p.get("owner_case"))||!propietario(a,caso))throw new FalloTecnico(404);
        asociado(a,caso,documento(doc));
    }
    private void autorizarProteccion(Operador a,UUID caso,UUID doc,String clasificacion){if(clasificacion.equals("BLOQUEADO"))restringirPropio(a,caso,doc);else ownerAcreditable(a,caso,doc);}
    public Map<String,Object> control(Operador a,UUID caso,UUID doc){
        return db.transaction(()->{guard(a);var p=politica(doc);if(!areas.jefe(a,core.caseRow(caso))){restringirPropio(a,caso,doc);return Map.of("revision",n(p,"revision"),"clasificacion",p.isEmpty()?"HISTORICO_SIN_ACREDITAR":p.get("classification"),"contextos",List.of(),"publicaciones",List.of(),"historial",List.of());}ownerAcreditable(a,caso,doc);
            var d=documento(doc);var targets=db.sql.queryForList("SELECT c.id,d.unit_code,d.site_id,m.matter_id FROM core_case c JOIN core_destination d ON d.id=c.destination_id LEFT JOIN case_matter m ON m.case_id=c.id WHERE EXISTS(SELECT 1 FROM management_document_link l WHERE l.case_id=c.id AND l.document_id=?) OR (?='INGRESO_CIUDADANO' AND (c.communication_id=? OR EXISTS(SELECT 1 FROM communication_pertinence_target t WHERE t.case_id=c.id AND t.child_id=?))) ORDER BY c.id",id(doc),d.get("origin"),d.get("communication_id"),d.get("communication_id")).stream().filter(t->areas.jefe(a,core.caseRow(uuid(t.get("id"))))).toList();
            return Map.of("revision",n(p,"revision"),"clasificacion",p.isEmpty()?"HISTORICO_SIN_ACREDITAR":p.get("classification"),"contextos",normalizar(targets),"publicaciones",normalizar(db.sql.queryForList("SELECT case_id,revision,content_allowed,download_allowed,status,valid_from,valid_until FROM document_disclosure WHERE document_id=? ORDER BY case_id",id(doc))),"historial",normalizar(db.sql.queryForList("SELECT revision,actor_id,at_ms,action_code,reason_text FROM document_content_history WHERE document_id=? ORDER BY seq DESC LIMIT 100",id(doc))));});
    }
    private void historia(Operador a,UUID caso,UUID doc,long revision,String op,String motivo,Object payload){
        db.sql.update("INSERT INTO document_content_history(document_id,case_id,revision,actor_id,at_ms,action_code,reason_text,snapshot_doc) VALUES(?,?,?,?,?,?,?,?)",id(doc),id(caso),revision,a.id(),clock.millis(),op,motivo,json.writeValueAsString(payload));
    }
    public Resultado proteger(Operador a,UUID key,UUID caso,UUID doc,Proteccion r){
        if(r==null||r.clasificacion()==null||!Set.of("ORDINARIO","FIR","BLOQUEADO").contains(r.clasificacion()))throw new FalloTecnico(400);motivo(r.fundamento(),1000);
        db.transaction(()->{guard(a);autorizarProteccion(a,caso,doc,r.clasificacion());return null;});
        return core.execute(key,a,"PROTEGER_DOCUMENTO",List.of(caso,doc,r),()->{guard(a);
            locks(uuid(core.caseRow(caso).get("communication_id")),uuid(documento(doc).get("communication_id")));autorizarProteccion(a,caso,doc,r.clasificacion());
            var p=politica(doc);if(n(p,"revision")!=r.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");
            if(r.clasificacion().equals("FIR")&&!"GESTION_INSTITUCIONAL".equals(documento(doc).get("origin")))throw new FalloTecnico(409,"FIR_REQUIERE_FICHA_INTERNA");
            long next=r.revision()+1;if(p.isEmpty())db.sql.update("INSERT INTO document_content_policy VALUES(?,?,?,?)",id(doc),id(caso),r.clasificacion(),next);
            else db.sql.update("UPDATE document_content_policy SET classification=?,revision=? WHERE document_id=?",r.clasificacion(),next,id(doc));
            historia(a,caso,doc,next,"PROTECCION",r.fundamento(),r);return new Resultado(doc,next);
        });
    }
    public Resultado publicar(Operador a,UUID key,UUID owner,UUID doc,UUID caso,Publicacion r){
        if(r==null||r.desde()==null||r.hasta()==null||!r.hasta().isAfter(r.desde())||r.descarga()&&!r.contenido()||r.estado()==null||!Set.of("ACTIVA","SUSPENDIDA").contains(r.estado()))throw new FalloTecnico(400);motivo(r.fundamento(),1000);
        db.transaction(()->{guard(a);ownerAcreditable(a,owner,doc);if(r.estado().equals("ACTIVA"))core.head(a,core.caseRow(caso));return null;});
        return core.execute(key,a,"PUBLICAR_DOCUMENTO",List.of(owner,doc,caso,r),()->{guard(a);
            locks(uuid(core.caseRow(owner).get("communication_id")),uuid(core.caseRow(caso).get("communication_id")));ownerAcreditable(a,owner,doc);
            var old=db.sql.queryForList("SELECT revision FROM document_disclosure WHERE document_id=? AND case_id=?",id(doc),id(caso));long rev=old.isEmpty()?0:n(old.getFirst(),"revision");if(rev!=r.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");
            if(r.estado().equals("SUSPENDIDA")){
                if(old.isEmpty())throw new FalloTecnico(409);
                db.sql.update("UPDATE document_disclosure SET status='SUSPENDIDA',revision=revision+1 WHERE document_id=? AND case_id=?",id(doc),id(caso));
                historia(a,owner,doc,rev+1,"SUSPENDER_CONSULTA",r.fundamento(),Map.of("destino",caso,"decision",r));return new Resultado(doc,rev+1);
            }
            core.head(a,core.caseRow(caso));asociado(a,caso,documento(doc));var p=politica(doc);if(!"ORDINARIO".equals(p.get("classification")))throw new FalloTecnico(409,"DOCUMENTO_NO_ORDINARIO");
            String materia=new PoliticaMaterias(db,clock).materia(caso);var m=new CatalogoMaterias(db,clock).exigir(materia);
            if(r.desde().isBefore(m.desde())||r.hasta().isAfter(m.hasta()))throw new FalloTecnico(400);
            long next=rev+1;if(old.isEmpty())db.sql.update("INSERT INTO document_disclosure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id(doc),id(caso),next,n(p,"revision"),materia,secuencia("case_area_history",caso),secuencia("case_matter_history",caso),r.desde().toEpochMilli(),r.hasta().toEpochMilli(),r.contenido(),r.descarga(),r.estado());
            else db.sql.update("UPDATE document_disclosure SET revision=?,policy_revision=?,matter_id=?,area_seq=?,matter_seq=?,valid_from=?,valid_until=?,content_allowed=?,download_allowed=?,status=? WHERE document_id=? AND case_id=?",next,n(p,"revision"),materia,secuencia("case_area_history",caso),secuencia("case_matter_history",caso),r.desde().toEpochMilli(),r.hasta().toEpochMilli(),r.contenido(),r.descarga(),r.estado(),id(doc),id(caso));
            historia(a,owner,doc,next,"CONSULTA_MATERIA",r.fundamento(),Map.of("destino",caso,"decision",r,"materia",materia,"areaSeq",secuencia("case_area_history",caso),"materiaSeq",secuencia("case_matter_history",caso),"proteccionRevision",n(p,"revision")));return new Resultado(doc,next);
        });
    }
}

