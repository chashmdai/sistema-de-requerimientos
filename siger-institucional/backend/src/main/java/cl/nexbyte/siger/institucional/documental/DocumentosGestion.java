package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;
import org.springframework.dao.DuplicateKeyException;

/** Originales de gestión inmutables; los vínculos y su autorización son independientes de la custodia ciudadana. */
public final class DocumentosGestion {
    public record Contexto(UUID caso,UUID actuacion){}
    public record Documento(UUID id,String origen,String nombre,long tamanio,String sha256,String tipo,String autor,Instant fecha,List<Contexto> contextos,String representacion,boolean consultable,boolean descargable,boolean administrable,String proteccion,boolean restringible,boolean referenciable,boolean cuarentena,boolean asociacionRetirada){}
    private final BaseJdbc db;private final ArchivoInstitucional files;private final AutorizacionDocumental policy;
    private final OriginalesGestion originales;private final Clock clock;
    public DocumentosGestion(BaseJdbc db,ArchivoInstitucional files,AutorizacionDocumental policy,ProcesamientoDocumental.Antivirus antivirus,ProcesamientoDocumental.Inspector inspector,Clock clock){this.db=db;this.files=files;this.policy=policy;this.originales=new OriginalesGestion(files,antivirus,inspector);this.clock=clock;}
    private static String sid(UUID id){if(id==null)throw new FalloTecnico(400);return id.toString();}
    private static String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(EntregaCompleta.lector().writeValueAsBytes(value)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    public void evento(Operador actor,UUID documento,UUID caso,UUID action,String code){db.sql.update("INSERT INTO document_event(document_id,case_id,action_id,actor_id,at_ms,event_code) VALUES(?,?,?,?,?,?)",documento==null?null:sid(documento),caso==null?null:sid(caso),action==null?null:sid(action),actor.id(),clock.millis(),code);}
    private UUID anterior(UUID key,Operador a,UUID caso,String operation,String digest){
        var rows=db.sql.queryForList("SELECT * FROM document_command WHERE id=?",sid(key));if(rows.isEmpty())return null;var row=rows.getFirst();
        if(!a.id().equals(row.get("actor_id"))||!sid(caso).equals(row.get("case_id"))||!operation.equals(row.get("operation_code"))||!digest.equals(row.get("request_hash")))throw new FalloTecnico(409);
        return UUID.fromString(row.get("document_id").toString());
    }
    private void comando(UUID key,Operador a,UUID caso,String op,String digest,UUID document){db.sql.update("INSERT INTO document_command VALUES(?,?,?,?,?,?,?)",sid(key),a.id(),sid(caso),op,digest,sid(document),clock.millis());}
    private void actuacion(UUID caso,UUID action){if(action!=null&&db.sql.queryForObject("SELECT COUNT(*) FROM core_action WHERE id=? AND case_id=?",Integer.class,sid(action),sid(caso))!=1)throw new FalloTecnico(404);}
    public Documento cargar(Operador actor,UUID key,UUID caso,UUID action,String nombre,InputStream input){
        policy.leerCaso(actor,caso);
        try(var temp=originales.preparar(nombre,input)){
            String tipo=temp.tipo();var digest=hash(List.of("GESTION_CARGA_1",actor.id(),caso,Objects.toString(action,""),nombre,temp.tamanio(),temp.sha256(),tipo));
            var old=anterior(key,actor,caso,"CARGAR",digest);if(old!=null){verificarArchivo(referencia(actor,caso,old,"METADATOS"));return documento(actor,caso,old);}
            var initial=db.transaction(()->{var c=policy.gestionarCaso(actor,caso);DisponibilidadDocumental.huella(db,temp.sha256());actuacion(caso,action);return c;});
            try{temp.validar();}catch(OriginalesGestion.Rechazo rejected){evento(actor,null,caso,null,rejected.codigo);throw new FalloTecnico(422,"DOCUMENTO_RECHAZADO");}
            var id=UUID.nameUUIDFromBytes(("GESTION_DOC_1:"+actor.id()+":"+key).getBytes(StandardCharsets.UTF_8));String storage=temp.conservar(id);
            try{return db.transaction(()->{
                var same=anterior(key,actor,caso,"CARGAR",digest);if(same!=null)return documento(actor,caso,same);
                var current=policy.gestionarCaso(actor,caso);DisponibilidadDocumental.huella(db,temp.sha256());actuacion(caso,action);if(!current.comunicacionId().equals(initial.comunicacionId()))throw new FalloTecnico(409);
                db.sql.update("INSERT INTO institution_document(id,communication_id,origin,name,size_bytes,sha256,media_type,storage_key,created_ms,created_by) VALUES(?,?,'GESTION_INSTITUCIONAL',?,?,?,?,?,?,?)",sid(id),sid(current.comunicacionId()),nombre,temp.tamanio(),temp.sha256(),tipo,storage,clock.millis(),actor.id());
                vinculo(id,caso,action,actor);policy.registrarDocumento(actor,caso,id);comando(key,actor,caso,"CARGAR",digest,id);evento(actor,id,caso,action,"DOCUMENTO_CREADO");return documento(actor,caso,id);
            });}catch(DuplicateKeyException race){var winner=anterior(key,actor,caso,"CARGAR",digest);if(winner==null)throw new FalloTecnico(409);return documento(actor,caso,winner);}
        }catch(IOException failure){evento(actor,null,caso,null,"CARGA_NO_COMPLETADA");throw new FalloTecnico(503,"DOCUMENTO_NO_DISPONIBLE");}
    }
    private void vinculo(UUID document,UUID caso,UUID action,Operador actor){
        AsociacionesVigentes.nuevoVinculo(db,"CASO",caso,document,action==null?"CASO":sid(action));
        if(db.sql.queryForObject("SELECT COUNT(*) FROM management_document_link WHERE document_id=? AND case_id=? AND context_id=?",Integer.class,sid(document),sid(caso),action==null?"CASO":sid(action))==0)
            db.sql.update("INSERT INTO management_document_link VALUES(?,?,?,?,?,?)",sid(document),sid(caso),action==null?null:sid(action),action==null?"CASO":sid(action),actor.id(),clock.millis());
    }
    public Documento vincular(Operador actor,UUID key,UUID documento,UUID origen,UUID destino,UUID action){
        policy.leerCaso(actor,origen);referencia(actor,origen,documento,"DESCARGA");policy.leerCaso(actor,destino);String digest=hash(List.of("GESTION_VINCULO_1",documento,origen,destino,Objects.toString(action,"")));
        return db.transaction(()->{
            policy.bloquearMutacion(actor);
            var roots=db.sql.queryForList("SELECT communication_id FROM core_case WHERE id=? OR id=? ORDER BY communication_id",String.class,sid(origen),sid(destino));
            for(var root:new LinkedHashSet<>(roots))db.sql.queryForList("SELECT comunicacion_id FROM communication_registry WHERE comunicacion_id=? FOR UPDATE",root);
            policy.leerCaso(actor,origen);referencia(actor,origen,documento,"DESCARGA");var previous=anterior(key,actor,destino,"VINCULAR",digest);if(previous!=null)return resultadoVinculo(actor,origen,destino,action,previous);
            if(policy.permisos(actor,origen,documento).clasificacion().equals("HISTORICO_SIN_ACREDITAR"))throw new FalloTecnico(409,"DOCUMENTO_REQUIERE_ACREDITACION");
            policy.gestionarCaso(actor,destino);actuacion(destino,action);vinculo(documento,destino,action,actor);comando(key,actor,destino,"VINCULAR",digest,documento);evento(actor,documento,destino,action,"VINCULO_CREADO");return resultadoVinculo(actor,origen,destino,action,documento);
        });
    }
    private Documento resultadoVinculo(Operador actor,UUID origen,UUID destino,UUID action,UUID id){
        try{return documento(actor,destino,id);}catch(FalloTecnico e){if(e.status!=404)throw e;}
        var d=documento(actor,origen,id);return new Documento(d.id(),d.origen(),d.nombre(),d.tamanio(),d.sha256(),d.tipo(),d.autor(),d.fecha(),List.of(new Contexto(destino,action)),d.representacion(),false,false,false,d.proteccion(),false,false,d.cuarentena(),d.asociacionRetirada());
    }
    private boolean referenciable(Operador actor,UUID caso,boolean descarga){if(!descarga)return false;try{policy.leerCaso(actor,caso);return true;}catch(FalloTecnico e){if(e.status==404)return false;throw e;}}
    public List<Documento> listar(Operador actor,UUID caso){return db.transaction(()->{policy.contextoDocumento(actor,caso);var result=new ArrayList<Documento>();for(var r:db.sql.queryForList("SELECT DISTINCT d.id,d.created_ms FROM institution_document d JOIN management_document_link l ON l.document_id=d.id WHERE l.case_id=? AND d.origin='GESTION_INSTITUCIONAL' ORDER BY d.created_ms,d.id",sid(caso))){try{result.add(documento(actor,caso,UUID.fromString(r.get("id").toString())));}catch(FalloTecnico e){if(e.status!=404)throw e;}}return result;});}
    public Documento documento(Operador actor,UUID caso,UUID id){var d=referencia(actor,caso,id,"METADATOS");var permisos=policy.permisos(actor,caso,id);var links=AsociacionesVigentes.contextos(db,"CASO",caso,id).stream().map(k->new Contexto(caso,k.equals("CASO")?null:UUID.fromString(k))).toList();String view=FormatosDocumento.paginable(d.original().tipo())?"PAGINAS":d.original().tipo().startsWith("audio/")?"AUDIO":d.original().tipo().startsWith("video/")?"VIDEO":"DESCARGA";var a=d.original();boolean held=DisponibilidadDocumental.retenido(db,id);return new Documento(id,d.origen(),a.nombre(),a.tamanio(),a.sha256(),a.tipo(),d.autor(),d.fecha(),links,view,permisos.contenido()&&!held,permisos.descarga()&&!held,permisos.administrar(),permisos.clasificacion(),permisos.restringir(),referenciable(actor,caso,permisos.descarga()&&!held),held,AsociacionesVigentes.retiradaCaso(db,caso,id));}
    public DocumentoPrivado referencia(Operador actor,UUID caso,UUID id){
        return referencia(actor,caso,id,"CONTENIDO");
    }
    public void revalidar(Operador actor,UUID caso,UUID id,String accion){policy.revalidarDocumento(actor,caso,id,accion);if(!accion.equals("METADATOS")){AsociacionesVigentes.contenido(db,"CASO",caso,id);DisponibilidadDocumental.contenido(db,id);}}
    public DocumentoPrivado referencia(Operador actor,UUID caso,UUID id,String accion){
        policy.documento(actor,caso,id,accion);if(!accion.equals("METADATOS")){AsociacionesVigentes.contenido(db,"CASO",caso,id);DisponibilidadDocumental.contenido(db,id);}var rows=db.sql.queryForList("SELECT d.* FROM institution_document d WHERE d.id=? AND d.origin='GESTION_INSTITUCIONAL' AND EXISTS(SELECT 1 FROM management_document_link l WHERE l.document_id=d.id AND l.case_id=?)",sid(id),sid(caso));if(rows.isEmpty())throw new FalloTecnico(404);var r=rows.getFirst();
        if(!("gestion/"+id).equals(r.get("storage_key"))||r.get("created_by")==null)throw new FalloTecnico(409);
        var a=new EntregaCompleta.Adjunto(id,r.get("name").toString(),((Number)r.get("size_bytes")).longValue(),r.get("sha256").toString(),r.get("media_type").toString(),"VALIDADO");
        return new DocumentoPrivado(id,UUID.fromString(r.get("communication_id").toString()),"GESTION_INSTITUCIONAL",r.get("storage_key").toString(),a,r.get("created_by").toString(),Instant.ofEpochMilli(((Number)r.get("created_ms")).longValue()));
    }
    public record Movimiento(long secuencia,String autor,Instant fecha,String tipo,UUID actuacion){}
    public List<Movimiento> historial(Operador actor,UUID caso,UUID documento){
        referencia(actor,caso,documento,"HISTORIAL");
        return db.sql.queryForList("SELECT seq,actor_id,at_ms,event_code,action_id FROM document_event WHERE document_id=? AND case_id=? ORDER BY seq DESC LIMIT 100",sid(documento),sid(caso)).stream().map(row->new Movimiento(((Number)row.get("seq")).longValue(),row.get("actor_id").toString(),Instant.ofEpochMilli(((Number)row.get("at_ms")).longValue()),row.get("event_code").toString(),row.get("action_id")==null?null:UUID.fromString(row.get("action_id").toString()))).toList();
    }
    public java.nio.file.Path verificarArchivo(DocumentoPrivado document){try{return files.verificar(document.clave(),document.original());}catch(IOException bad){throw new FalloTecnico(409,"INTEGRIDAD_DOCUMENTAL_INVALIDA");}}
}
