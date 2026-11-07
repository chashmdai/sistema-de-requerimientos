package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.NucleoU2.ContextoDocumentoReq;
import cl.nexbyte.siger.institucional.robustez.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;

/** Documentos de la atención sin Caso. El REQ y el ámbito de origen son condiciones conjuntas de acceso. */
public final class DocumentosRequerimiento {
    public record Contexto(UUID requerimiento,Long gestionSeq){}
    public record Documento(UUID id,String nombre,long tamanio,String sha256,String tipo,String autor,Instant fecha,
                            List<Contexto> contextos,String representacion,boolean consultable,boolean descargable,
                            boolean administrable,boolean restringible,String proteccion,long revision,boolean cuarentena){}
    public record Lista(boolean editable,List<Documento> documentos){}
    public record Proteccion(long revision,String clasificacion,String fundamento){}
    public record Vinculo(Long gestionSeq){}
    public record Movimiento(long secuencia,String autor,Instant fecha,String tipo,String fundamento){}
    private final BaseJdbc db;private final NucleoU2 flow;private final ArchivoInstitucional files;
    private final OriginalesGestion originales;private final Clock clock;
    public DocumentosRequerimiento(BaseJdbc db,NucleoInstitucional core,ArchivoInstitucional files,ProcesamientoDocumental.Antivirus av,ProcesamientoDocumental.Inspector inspector,Clock clock){this.db=db;this.flow=core.u2();this.files=files;this.originales=new OriginalesGestion(files,av,inspector);this.clock=clock;}
    private static String id(UUID id){if(id==null)throw new FalloTecnico(400);return id.toString();}
    private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private static String s(Map<String,Object> r,String k){return Objects.toString(r.get(k),null);}
    private static String hash(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(EntregaCompleta.lector().writeValueAsBytes(v)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    private void lock(ContextoDocumentoReq c){db.sql.queryForList("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",id(c.comunicacion()));}
    private ContextoDocumentoReq contexto(Operador a,UUID req,boolean escritura){return flow.contextoDocumentoReq(a,req,escritura);}
    private Map<String,Object> politica(ContextoDocumentoReq c,UUID doc){
        var rows=db.sql.queryForList("SELECT p.* FROM requirement_document_policy p JOIN institution_document d ON d.id=p.document_id AND d.owner_context=p.owner_context WHERE p.document_id=? AND p.requirement_id=? AND d.communication_id=? AND d.origin='GESTION_INSTITUCIONAL' AND NOT EXISTS(SELECT 1 FROM document_content_policy x WHERE x.document_id=p.document_id)",id(doc),id(c.requerimiento()),id(c.comunicacion()));
        if(rows.size()!=1)throw new FalloTecnico(404);var p=rows.getFirst();
        if(!Objects.equals(c.unidad(),p.get("unit_code"))||!Objects.equals(c.sede(),p.get("site_id"))||!Objects.equals(c.area(),p.get("area_id")))throw new FalloTecnico(404);
        return p;
    }
    private Map<String,Object> permitir(Operador a,UUID req,UUID doc,String accion){
        var c=contexto(a,req,false);var p=politica(c,doc);boolean bloqueado="BLOQUEADO".equals(p.get("classification"));
        if(bloqueado&&!(accion.equals("METADATOS")&&c.jefatura()))throw new FalloTecnico(404);
        // FIR exige responsabilidad profesional o jefatura efectiva; ningún rol administrativo, técnico o de consulta la hereda.
        if("FIR".equals(p.get("classification"))&&!c.jefatura()&&!c.profesional())throw new FalloTecnico(404);
        if(!accion.equals("METADATOS"))DisponibilidadDocumental.contenido(db,doc);
        return p;
    }
    private void gestion(UUID req,Long seq){if(seq!=null&&(seq<=0||db.sql.queryForObject("SELECT COUNT(*) FROM institution_req_action d JOIN institution_flow_history h ON h.seq=d.history_seq WHERE d.history_seq=? AND d.requirement_id=? AND h.resource_id=? AND h.resource_kind='REQ'",Long.class,seq,id(req),id(req))!=1))throw new FalloTecnico(404);}
    private UUID anterior(Operador a,UUID key,UUID req,String op,String digest){var rows=db.sql.queryForList("SELECT * FROM requirement_document_command WHERE id=?",id(key));if(rows.isEmpty())return null;var r=rows.getFirst();if(!a.id().equals(r.get("actor_id"))||!id(req).equals(r.get("requirement_id"))||!op.equals(r.get("operation_code"))||!digest.equals(r.get("request_hash")))throw new FalloTecnico(409);return UUID.fromString(s(r,"document_id"));}
    private void comando(Operador a,UUID key,UUID req,String op,String digest,UUID doc){db.sql.update("INSERT INTO requirement_document_command VALUES(?,?,?,?,?,?,?)",id(key),a.id(),id(req),op,digest,id(doc),clock.millis());}
    private void historia(Operador a,ContextoDocumentoReq c,UUID doc,long revision,String tipo,String fundamento,Map<String,Object> data){db.sql.update("INSERT INTO institution_flow_history(communication_id,resource_id,resource_kind,revision,actor_id,at_ms,action_code,reason_text,snapshot_doc) VALUES(?,?,'REQ_DOCUMENTO',?,?,?,?,?,?)",id(c.comunicacion()),id(doc),revision,a.id(),clock.millis(),tipo,fundamento,EntregaCompleta.lector().writeValueAsString(data));}
    private void vinculo(Operador a,UUID req,UUID doc,Long seq){long key=seq==null?0:seq;if(db.sql.queryForObject("SELECT COUNT(*) FROM requirement_document_link WHERE document_id=? AND requirement_id=? AND context_key=?",Long.class,id(doc),id(req),key)==0)db.sql.update("INSERT INTO requirement_document_link VALUES(?,?,?,?,?,?)",id(doc),id(req),seq,key,a.id(),clock.millis());}
    public Documento cargar(Operador a,UUID key,UUID req,Long seq,String nombre,InputStream input){
        db.transaction(()->contexto(a,req,false));
        try(var temp=originales.preparar(nombre,input)){
            String digest=hash(List.of("GESTION_REQ_CARGA_1",a.id(),req,Objects.toString(seq,""),nombre,temp.tamanio(),temp.sha256(),temp.tipo()));
            UUID old=db.transaction(()->{contexto(a,req,false);return anterior(a,key,req,"CARGAR",digest);});
            if(old!=null){verificarArchivo(referencia(a,req,old,"METADATOS"));return documento(a,req,old);}
            var initial=db.transaction(()->{var c=contexto(a,req,true);gestion(req,seq);DisponibilidadDocumental.huella(db,temp.sha256());return c;});
            try{temp.validar();}catch(OriginalesGestion.Rechazo e){auditar(a,req,null,e.codigo);throw new FalloTecnico(422,"DOCUMENTO_RECHAZADO");}
            UUID doc=UUID.nameUUIDFromBytes(("GESTION_REQ_DOC_1:"+a.id()+":"+key).getBytes(StandardCharsets.UTF_8));String storage=temp.conservar(doc);
            try{return db.transaction(()->{
                var c=contexto(a,req,false);lock(c);UUID previous=anterior(a,key,req,"CARGAR",digest);if(previous!=null)return documento(a,req,previous);
                c=contexto(a,req,true);gestion(req,seq);DisponibilidadDocumental.huella(db,temp.sha256());if(!c.comunicacion().equals(initial.comunicacion())||!c.unidad().equals(initial.unidad())||!c.sede().equals(initial.sede())||!Objects.equals(c.area(),initial.area()))throw new FalloTecnico(409,"AMBITO_DOCUMENTAL_CAMBIO");
                db.sql.update("INSERT INTO institution_document(id,communication_id,origin,name,size_bytes,sha256,media_type,storage_key,created_ms,created_by,owner_context) VALUES(?,?,'GESTION_INSTITUCIONAL',?,?,?,?,?,?,?,'REQ')",id(doc),id(c.comunicacion()),nombre,temp.tamanio(),temp.sha256(),temp.tipo(),storage,clock.millis(),a.id());
                db.sql.update("INSERT INTO requirement_document_policy VALUES(?,'REQ',?,?,?,?,'SIN_CLASIFICAR',0)",id(doc),id(req),c.unidad(),c.sede(),c.area());vinculo(a,req,doc,seq);comando(a,key,req,"CARGAR",digest,doc);historia(a,c,doc,0,"REQ_DOCUMENTO_CREADO","Original de gestión conservado",Map.of("requerimiento",req,"version","1.0","gestionSeq",seq==null?0:seq));return documento(a,req,doc);
            });}catch(DuplicateKeyException race){UUID winner=db.transaction(()->{contexto(a,req,false);return anterior(a,key,req,"CARGAR",digest);});if(winner==null)throw new FalloTecnico(409);return documento(a,req,winner);}
        }catch(IOException e){auditar(a,req,null,"CARGA_NO_COMPLETADA");throw new FalloTecnico(503,"DOCUMENTO_NO_DISPONIBLE");}
    }
    public Lista listar(Operador a,UUID req){return db.transaction(()->{
        var c=contexto(a,req,false);var args=new ArrayList<Object>(List.of(id(req),id(c.comunicacion()),c.unidad(),c.sede()));
        String area=c.area()==null?"p.area_id IS NULL":"p.area_id=?";if(c.area()!=null)args.add(c.area());
        String proteccion=c.jefatura()?"":" AND p.classification<>'BLOQUEADO'"+(c.profesional()?"":" AND p.classification<>'FIR'");
        var rows=db.sql.queryForList("SELECT d.id FROM institution_document d JOIN requirement_document_policy p ON p.document_id=d.id AND p.owner_context=d.owner_context WHERE p.requirement_id=? AND d.communication_id=? AND p.unit_code=? AND p.site_id=? AND "+area+proteccion+" AND d.owner_context='REQ' AND NOT EXISTS(SELECT 1 FROM document_content_policy x WHERE x.document_id=d.id) ORDER BY d.created_ms,d.id",args.toArray());
        var result=rows.stream().map(r->documentoActual(a,req,UUID.fromString(s(r,"id")))).toList();auditar(a,req,null,"DOCUMENTOS_REQ_LISTADOS");return new Lista(c.editable(),result);
    });}
    public Documento documento(Operador a,UUID req,UUID doc){return db.transaction(()->documentoActual(a,req,doc));}
    private Documento documentoActual(Operador a,UUID req,UUID doc){
        var p=permitir(a,req,doc,"METADATOS");var c=contexto(a,req,false);var d=original(doc);boolean held=DisponibilidadDocumental.retenido(db,doc),contenido=!held&&!"BLOQUEADO".equals(p.get("classification"));var x=d.original();
        var links=db.sql.queryForList("SELECT activity_seq FROM requirement_document_link WHERE document_id=? AND requirement_id=? ORDER BY context_key",id(doc),id(req)).stream().map(r->new Contexto(req,r.get("activity_seq")==null?null:n(r,"activity_seq"))).toList();
        String view=FormatosDocumento.paginable(x.tipo())?"PAGINAS":x.tipo().startsWith("audio/")?"AUDIO":x.tipo().startsWith("video/")?"VIDEO":"DESCARGA";
        return new Documento(doc,x.nombre(),x.tamanio(),x.sha256(),x.tipo(),d.autor(),d.fecha(),links,view,contenido,contenido,c.jefatura(),c.jefatura()||c.profesional(),s(p,"classification"),n(p,"revision"),held);
    }
    private DocumentoPrivado original(UUID doc){var rows=db.sql.queryForList("SELECT * FROM institution_document WHERE id=? AND owner_context='REQ' AND origin='GESTION_INSTITUCIONAL'",id(doc));if(rows.size()!=1)throw new FalloTecnico(404);var r=rows.getFirst();if(!("gestion/"+doc).equals(r.get("storage_key"))||r.get("created_by")==null)throw new FalloTecnico(409);return new DocumentoPrivado(doc,UUID.fromString(s(r,"communication_id")),"GESTION_INSTITUCIONAL",s(r,"storage_key"),new EntregaCompleta.Adjunto(doc,s(r,"name"),n(r,"size_bytes"),s(r,"sha256"),s(r,"media_type"),"VALIDADO"),s(r,"created_by"),Instant.ofEpochMilli(n(r,"created_ms")));}
    public DocumentoPrivado referencia(Operador a,UUID req,UUID doc,String accion){return db.transaction(()->{permitir(a,req,doc,accion);return original(doc);});}
    public void revalidar(Operador a,UUID req,UUID doc,String accion){db.transaction(()->{permitir(a,req,doc,accion);return null;});}
    public java.nio.file.Path verificarArchivo(DocumentoPrivado doc){try{return files.verificar(doc.clave(),doc.original());}catch(IOException e){throw new FalloTecnico(409,"INTEGRIDAD_DOCUMENTAL_INVALIDA");}}
    public Documento proteger(Operador a,UUID key,UUID req,UUID doc,Proteccion r){
        if(r==null||r.revision()<0||!Set.of("ORDINARIO","FIR","BLOQUEADO").contains(Objects.toString(r.clasificacion(),"")))throw new FalloTecnico(400);texto(r.fundamento());String digest=hash(List.of(doc,r));
        return db.transaction(()->{var c=contexto(a,req,false);lock(c);c=contexto(a,req,false);var p=politica(c,doc);
            if(!c.jefatura()&&!(c.profesional()&&r.clasificacion().equals("BLOQUEADO")))throw new FalloTecnico(404);
            if(anterior(a,key,req,"PROTEGER",digest)!=null)return "BLOQUEADO".equals(p.get("classification"))&&!c.jefatura()?null:documento(a,req,doc);
            if(n(p,"revision")!=r.revision())throw new FalloTecnico(409,"VERSION_OBSOLETA");db.sql.update("UPDATE requirement_document_policy SET classification=?,revision=revision+1 WHERE document_id=?",r.clasificacion(),id(doc));comando(a,key,req,"PROTEGER",digest,doc);historia(a,c,doc,r.revision()+1,"REQ_DOCUMENTO_PROTEGIDO",r.fundamento(),Map.of("requerimiento",req,"clasificacion",r.clasificacion()));
            if(r.clasificacion().equals("BLOQUEADO")&&!c.jefatura())return null;return documento(a,req,doc);
        });
    }
    private static void texto(String v){try{cl.nexbyte.siger.contrato.DeclaracionNna.texto(v,2000,true);}catch(IllegalArgumentException e){throw new FalloTecnico(400);}}
    public Documento vincular(Operador a,UUID key,UUID req,UUID doc,Long seq){if(seq==null)throw new FalloTecnico(400);String digest=hash(List.of(doc,seq));return db.transaction(()->{
        var c=contexto(a,req,false);lock(c);permitir(a,req,doc,"DESCARGA");if(anterior(a,key,req,"VINCULAR",digest)!=null)return documento(a,req,doc);c=contexto(a,req,true);gestion(req,seq);vinculo(a,req,doc,seq);comando(a,key,req,"VINCULAR",digest,doc);historia(a,c,doc,n(politica(c,doc),"revision"),"REQ_DOCUMENTO_VINCULADO","Referencia a gestión existente",Map.of("requerimiento",req,"gestionSeq",seq));return documento(a,req,doc);
    });}
    public List<Movimiento> historial(Operador a,UUID req,UUID doc){return db.transaction(()->{permitir(a,req,doc,"HISTORIAL");auditar(a,req,doc,"HISTORIAL_REQ_CONSULTADO");return db.sql.queryForList("SELECT seq,actor_id,at_ms,action_code,reason_text FROM institution_flow_history WHERE resource_id=? AND resource_kind='REQ_DOCUMENTO' ORDER BY seq DESC LIMIT 100",id(doc)).stream().map(r->new Movimiento(n(r,"seq"),s(r,"actor_id"),Instant.ofEpochMilli(n(r,"at_ms")),s(r,"action_code"),s(r,"reason_text"))).toList();});}
    public void auditar(Operador a,UUID req,UUID doc,String accion){if(doc==null)db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),id(req),accion,clock.millis());else db.sql.update("INSERT INTO institution_document_access(document_id,communication_id,actor_id,at_ms,representation) SELECT id,communication_id,?,?,? FROM institution_document WHERE id=? AND owner_context='REQ'",a.id(),clock.millis(),accion,id(doc));}
}
