package cl.nexbyte.siger.institucional.robustez;

import java.security.*;
import cl.nexbyte.siger.institucional.formulario.*;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import tools.jackson.databind.json.JsonMapper;

/** Registro formal mínimo: identidad propia, folio, canal, fecha y trazabilidad. Sin flujo institucional. */
public final class RecepcionInstitucionalJdbc {
    private static final String SOURCE="FORMULARIO_PUBLICO_DDN";
    private final BaseJdbc db;
    private final JsonMapper mapper;
    private final Map<String,PublicKey> publicTrust,institutionTrust;
    private final PrivateKey signer;
    private final String keyId,prefix;
    private final int width;
    private final Clock clock;

    public RecepcionInstitucionalJdbc(BaseJdbc db,JsonMapper mapper,Map<String,PublicKey> publicTrust,
            String keyId,PrivateKey signer,Map<String,PublicKey> institutionTrust,String prefix,int width,Clock clock){
        if(prefix==null||!prefix.matches("[A-Z][A-Z0-9]{1,15}")||width<1||width>18)throw new IllegalArgumentException("Formato de folio inválido");
        this.db=db;this.mapper=mapper;this.publicTrust=Map.copyOf(publicTrust);this.institutionTrust=Map.copyOf(institutionTrust);
        this.keyId=keyId;this.signer=signer;this.prefix=prefix;this.width=width;this.clock=clock;
        if(institutionTrust.values().stream().anyMatch(publicTrust.values()::contains))throw new IllegalArgumentException("Los firmantes público e institucional deben ser diferentes");
        var probe=new ComprobanteInstitucional.Contenido("1.0","SIGER_DDN","COMUNICACION_REGISTRADA",UUID.randomUUID(),UUID.randomUUID(),1,"COM-000001",clock.instant(),"FORMULARIO_WEB_PUBLICO","0".repeat(64),"0".repeat(64));
        ComprobanteInstitucional.firmar(probe,keyId,signer,mapper).verificar(institutionTrust,mapper);
    }

    public RegistroInstitucional guardar(EntregaVerificada entrega){
        entrega.validar(publicTrust,mapper);
        var id=entrega.comunicacion().id();
        var previous=existente(entrega);if(previous!=null)return previous;
        try{return db.transaction(()->{
            // También serializa la promoción explícita de una recepción técnica anterior a V3.
            var old=db.sql.queryForList("SELECT body FROM institutional_inbox WHERE source=? AND id=? FOR UPDATE",SOURCE,id.toString());
            var raced=existente(entrega);if(raced!=null)return raced;
            var control=new ControlesOperacion(db).estado();
            if(!control.aceptarNuevos()||!control.contingencia().equals("NORMAL"))throw new FalloTecnico(503);
            if(!old.isEmpty())validarEntregaGuardada(old.getFirst().get("body").toString(),entrega);
            var registered=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            var communicationId=UUID.randomUUID();var provenance=EntregaVerificada.huella(entrega.procedencia());
            if(old.isEmpty()){
                // La fila provisional solo existe dentro de esta transacción y nunca se devuelve al llamador.
                db.sql.update("INSERT INTO institutional_inbox(source,id,content_hash,body,receipt,ack_pending,created_ms) VALUES(?,?,?,?,?,TRUE,?)",SOURCE,id.toString(),fingerprint(entrega),mapper.writeValueAsString(entrega),"{}",registered.toEpochMilli());
            }
            var result=emitir(id,communicationId,registered,entrega.comunicacion().canal(),entrega.comunicacion().sha256(),provenance);
            result.validar(institutionTrust,mapper,entrega);
            db.sql.update("UPDATE institutional_inbox SET receipt=?,ack_pending=TRUE WHERE source=? AND id=?",mapper.writeValueAsString(result),SOURCE,id.toString());
            db.event("COMUNICACION",communicationId.toString(),"COMUNICACION_REGISTRADA");
            return result;
        });}catch(DuplicateKeyException error){var winner=existente(entrega);if(winner==null)throw new FalloTecnico(503);return winner;}
    }

    /** Lectura funcionaria: verifica cuerpo, sello y registro existentes, sin editarlos. */
    public EntregaVerificada entregaRegistrada(UUID comunicacionId){
        var rows=db.sql.queryForList("SELECT i.body FROM institutional_inbox i JOIN communication_registry c ON c.source=i.source AND c.operacion_id=i.id WHERE c.comunicacion_id=?",comunicacionId.toString());
        if(rows.isEmpty())throw new FalloTecnico(404);
        var entrega=mapper.readValue(rows.getFirst().get("body").toString(),EntregaVerificada.class);
        if(existente(entrega)==null)throw new FalloTecnico(409);return entrega;
    }
    private RegistroInstitucional existente(EntregaVerificada entrega){
        var id=entrega.comunicacion().id();
        var rows=db.sql.queryForList("SELECT i.body,i.content_hash,i.receipt,c.numero,c.comunicacion_id,c.registrada_ms,c.canal,c.content_hash AS registered_hash,c.provenance_hash,c.folio_prefix,c.folio_width FROM institutional_inbox i JOIN communication_registry c ON c.source=i.source AND c.operacion_id=i.id WHERE i.source=? AND i.id=?",SOURCE,id.toString());
        if(rows.isEmpty())return null;
        var row=rows.getFirst();if(!fingerprint(entrega).equals(row.get("content_hash")))throw new FalloTecnico(409);
        validarEntregaGuardada(row.get("body").toString(),entrega);
        var result=mapper.readValue(row.get("receipt").toString(),RegistroInstitucional.class);result.validar(institutionTrust,mapper,entrega);
        var r=result.registro();long n=((Number)row.get("numero")).longValue();
        var folio=row.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+((Number)row.get("folio_width")).intValue()+"d",n);
        if(n!=r.numeroFolio()||!row.get("comunicacion_id").equals(r.comunicacionId().toString())||!folio.equals(r.folio())
                ||((Number)row.get("registrada_ms")).longValue()!=r.registradaEn().toEpochMilli()||!row.get("canal").equals(r.canalOrigen())
                ||!row.get("registered_hash").equals(r.sha256())||!row.get("provenance_hash").equals(r.huellaProcedencia()))throw new FalloTecnico(409);
        return result;
    }
    private void validarEntregaGuardada(String json,EntregaVerificada incoming){
        var stored=mapper.readValue(json,EntregaVerificada.class);stored.validar(publicTrust,mapper);
        if(!stored.comunicacion().id().equals(incoming.comunicacion().id())||!fingerprint(stored).equals(fingerprint(incoming)))throw new FalloTecnico(409);
    }
    public void confirmacionEntregada(UUID id){db.transaction(()->{
        if(db.sql.update("UPDATE institutional_inbox SET ack_pending=FALSE WHERE source=? AND id=? AND ack_pending=TRUE",SOURCE,id.toString())==1)db.event("ENTREGA",id.toString(),"ACK_PUBLICO_CONFIRMADO");return null;
    });}
    public void prepararConfirmacion(UUID id,UUID lease){
        if(lease==null)throw new FalloTecnico(400);
        db.sql.update("UPDATE institutional_inbox SET ack_lease=? WHERE source=? AND id=? AND ack_pending=TRUE",lease.toString(),SOURCE,id.toString());
    }
    public record ConfirmacionPendiente(FuenteEntregasV2.Reserva reserva,RegistroInstitucional registro){}
    public List<ConfirmacionPendiente> confirmacionesPendientes(){
        var rows=db.sql.queryForList("SELECT id,body,ack_lease FROM institutional_inbox WHERE document_kind='HISTORICO_V3' AND ack_pending=TRUE AND ack_lease IS NOT NULL AND ack_blocked=FALSE AND ack_next_ms<=? ORDER BY ack_next_ms,created_ms,id LIMIT 20",clock.millis());
        var result=new ArrayList<ConfirmacionPendiente>();
        for(var row:rows){
            try{
                var entrega=mapper.readValue(row.get("body").toString(),EntregaVerificada.class);
                if(!entrega.comunicacion().id().toString().equals(row.get("id")))throw new FalloTecnico(409);
                var registro=existente(entrega);
                if(registro==null)throw new FalloTecnico(409);
                result.add(new ConfirmacionPendiente(new FuenteEntregasV2.Reserva(UUID.fromString(row.get("ack_lease").toString()),0,1,entrega),registro));
            }catch(IllegalArgumentException|tools.jackson.core.JacksonException|FalloTecnico invalid){
                // Aislar esta confirmación técnica sin borrar Comunicación, cuerpo ni comprobante.
                db.transaction(()->{
                    if(db.sql.update("UPDATE institutional_inbox SET ack_lease=NULL WHERE source=? AND id=? AND ack_lease=? AND ack_pending=TRUE",SOURCE,row.get("id"),row.get("ack_lease"))==1)
                        db.event("ENTREGA",row.get("id").toString(),"ACK_INTEGRIDAD_INVALIDA");return null;
                });
            }
        }
        return List.copyOf(result);
    }
    /** Un lote por ciclo. Los rechazos nunca se convierten en confirmaciones. */
    public void aplazarConfirmacion(UUID id,UUID lease,int status){
        boolean persistent=status>=400&&status<500&&status!=408&&status!=429;
        db.transaction(()->{
            var rows=db.sql.queryForList("SELECT ack_attempts FROM institutional_inbox WHERE source=? AND id=? AND ack_lease=? AND ack_pending=TRUE AND ack_blocked=FALSE FOR UPDATE",SOURCE,id.toString(),lease.toString());
            if(rows.isEmpty())return null;
            int attempts=((Number)rows.getFirst().get("ack_attempts")).intValue()+1;
            boolean blocked=attempts>=(persistent?3:8);
            int safeStatus=status>=100&&status<=599?status:503;
            String code=(persistent?"ACK_PERSISTENTE_HTTP_":"ACK_TRANSITORIO_HTTP_")+safeStatus;
            long next=clock.millis()+Math.min(300000L,1000L<<Math.min(attempts,8));
            db.sql.update("UPDATE institutional_inbox SET ack_attempts=?,ack_next_ms=?,ack_blocked=?,ack_last_error=? WHERE source=? AND id=?",attempts,next,blocked,code,SOURCE,id.toString());
            db.event("ENTREGA",id.toString(),code);
            if(blocked)db.event("ENTREGA",id.toString(),"ACK_REVISION_REQUERIDA");return null;
        });
    }
    public void reintentarConfirmacion(UUID id,int intentos){
        db.transaction(()->{
            if(db.sql.update("UPDATE institutional_inbox SET ack_attempts=0,ack_next_ms=0,ack_blocked=FALSE,ack_last_error=NULL WHERE source=? AND id=? AND ack_attempts=? AND ack_blocked=TRUE AND ack_pending=TRUE AND ack_lease IS NOT NULL",SOURCE,id.toString(),intentos)!=1)throw new FalloTecnico(409);
            db.event("ENTREGA",id.toString(),"ACK_REINTENTO_OPERACIONAL");return null;
        });
    }
    public void incidente(UUID id,String code){
        if(code==null||!Set.of("CONTRATO_INVALIDO","RECEPTOR_NO_DISPONIBLE","CONFIRMACION_AMBIGUA").contains(code))throw new FalloTecnico(400);
        db.transaction(()->{db.event("ENTREGA",id.toString(),code);return null;});
    }
    private static String fingerprint(EntregaVerificada e){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((e.comunicacion().sha256()+":"+EntregaVerificada.huella(e.procedencia())).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}}

    /** Asignador y firma COM compartidos por el contrato histórico y el formulario completo. */
    private RegistroInstitucional emitir(UUID id,UUID communicationId,Instant registered,String canal,String digest,String provenance){
            var generated=new GeneratedKeyHolder();
            db.sql.update(connection->{
                var statement=connection.prepareStatement("INSERT INTO communication_registry(comunicacion_id,source,operacion_id,registrada_ms,canal,content_hash,provenance_hash,folio_prefix,folio_width) VALUES(?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
                statement.setString(1,communicationId.toString());statement.setString(2,SOURCE);statement.setString(3,id.toString());
                statement.setLong(4,registered.toEpochMilli());statement.setString(5,canal);
                statement.setString(6,digest);statement.setString(7,provenance);statement.setString(8,prefix);statement.setInt(9,width);return statement;
            },generated);
            var number=Objects.requireNonNull(generated.getKey()).longValue();
            var folio=prefix+"-"+String.format(Locale.ROOT,"%0"+width+"d",number);
            var content=new ComprobanteInstitucional.Contenido("FORMULARIO_WEB_NNA".equals(canal)?"1.1":"1.0","SIGER_DDN","COMUNICACION_REGISTRADA",id,communicationId,number,folio,registered,canal,digest,provenance);
            var result=new RegistroInstitucional(content,ComprobanteInstitucional.firmar(content,keyId,signer,mapper));
        return result;
    }
    public RegistroInstitucional guardarCompleta(EntregaCompleta entrega,ArchivoInstitucional files){
        entrega.validar(publicTrust,mapper);
        try{for(var a:entrega.adjuntos())if(!files.disponible(entrega.id(),a))throw new java.io.IOException();}catch(java.io.IOException e){throw new FalloTecnico(503);}
        var previous=existenteCompleta(entrega);if(previous!=null)return previous;
        try{return db.transaction(()->{
            var rows=db.sql.queryForList("SELECT document_kind FROM institutional_inbox WHERE source=? AND id=? FOR UPDATE",SOURCE,entrega.id().toString());
            if(!rows.isEmpty()){var same=existenteCompleta(entrega);if(same==null)throw new FalloTecnico(409);return same;}
            var control=new ControlesOperacion(db).estado();if(!control.aceptarNuevos()||!control.contingencia().equals("NORMAL"))throw new FalloTecnico(503);
            var at=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);var com=UUID.randomUUID();
            db.sql.update("INSERT INTO institutional_inbox(source,id,content_hash,body,receipt,ack_pending,created_ms,document_kind) VALUES(?,?,?,?,?,TRUE,?,'FORMULARIO_COMPLETO')",SOURCE,entrega.id().toString(),fingerprint(entrega),mapper.writeValueAsString(entrega),"{}",at.toEpochMilli());
            var receipt=emitir(entrega.id(),com,at,entrega.canal(),entrega.sha256(),entrega.huellaProcedencia());entrega.validarRegistro(receipt,institutionTrust,mapper);
            for(var a:entrega.adjuntos())db.sql.update("INSERT INTO institution_document(id,communication_id,operation_id,attachment_id,origin,name,size_bytes,sha256,media_type,storage_key,created_ms) VALUES(?,?,?,?,'INGRESO_CIUDADANO',?,?,?,?,?,?)",UUID.randomUUID().toString(),com.toString(),entrega.id().toString(),a.id().toString(),a.nombre(),a.tamanio(),a.sha256(),a.tipo(),files.key(entrega.id(),a.id()),at.toEpochMilli());
            db.sql.update("UPDATE institutional_inbox SET receipt=? WHERE source=? AND id=?",mapper.writeValueAsString(receipt),SOURCE,entrega.id().toString());
            db.event("COMUNICACION",com.toString(),"COMUNICACION_REGISTRADA");return receipt;
        });}catch(DuplicateKeyException race){var winner=existenteCompleta(entrega);if(winner==null)throw new FalloTecnico(503);return winner;}
    }
    /** Recupera el comprobante ya confirmado y verifica custodia antes de cada ACK. */
    public RegistroInstitucional registroCompletoConservado(EntregaCompleta entrega,ArchivoInstitucional files){
        var receipt=existenteCompleta(entrega);if(receipt==null)throw new FalloTecnico(409);
        try{for(var a:entrega.adjuntos())if(!files.disponible(entrega.id(),a))throw new java.io.IOException();}catch(java.io.IOException e){throw new FalloTecnico(503);}return receipt;
    }
    public boolean esCompleta(UUID communication){return db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox i JOIN communication_registry c ON c.source=i.source AND c.operacion_id=i.id WHERE c.comunicacion_id=? AND i.document_kind='FORMULARIO_COMPLETO'",Integer.class,communication.toString())==1;}
    public EntregaCompleta completaRegistrada(UUID communication){
        var rows=db.sql.queryForList("SELECT i.body FROM institutional_inbox i JOIN communication_registry c ON c.source=i.source AND c.operacion_id=i.id WHERE c.comunicacion_id=? AND i.document_kind='FORMULARIO_COMPLETO'",communication.toString());if(rows.isEmpty())throw new FalloTecnico(404);
        var e=EntregaCompleta.lector().readValue(rows.getFirst().get("body").toString(),EntregaCompleta.class);if(existenteCompleta(e)==null)throw new FalloTecnico(409);return e;
    }
    private RegistroInstitucional existenteCompleta(EntregaCompleta incoming){
        var rows=db.sql.queryForList("SELECT i.body,i.content_hash,i.receipt,i.document_kind,c.numero,c.comunicacion_id,c.registrada_ms,c.canal,c.content_hash AS registered_hash,c.provenance_hash,c.folio_prefix,c.folio_width FROM institutional_inbox i JOIN communication_registry c ON c.source=i.source AND c.operacion_id=i.id WHERE i.source=? AND i.id=?",SOURCE,incoming.id().toString());if(rows.isEmpty())return null;
        var r=rows.getFirst();if(!"FORMULARIO_COMPLETO".equals(r.get("document_kind"))||!fingerprint(incoming).equals(r.get("content_hash")))throw new FalloTecnico(409);
        var stored=EntregaCompleta.lector().readValue(r.get("body").toString(),EntregaCompleta.class);stored.validar(publicTrust,mapper);if(!fingerprint(stored).equals(fingerprint(incoming)))throw new FalloTecnico(409);
        var ack=mapper.readValue(r.get("receipt").toString(),RegistroInstitucional.class);stored.validarRegistro(ack,institutionTrust,mapper);var a=ack.registro();long n=((Number)r.get("numero")).longValue();
        if(!r.get("comunicacion_id").equals(a.comunicacionId().toString())||n!=a.numeroFolio()||!a.folio().equals(r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+((Number)r.get("folio_width")).intValue()+"d",n))||((Number)r.get("registrada_ms")).longValue()!=a.registradaEn().toEpochMilli()||!r.get("canal").equals(a.canalOrigen())||!r.get("registered_hash").equals(a.sha256())||!r.get("provenance_hash").equals(a.huellaProcedencia()))throw new FalloTecnico(409);
        var documents=db.sql.queryForList("SELECT attachment_id,name,size_bytes,sha256,media_type,origin,storage_key,operation_id FROM institution_document WHERE communication_id=? AND origin='INGRESO_CIUDADANO'",a.comunicacionId().toString());if(documents.size()!=stored.adjuntos().size())throw new FalloTecnico(409);
        for(var d:stored.adjuntos())if(documents.stream().noneMatch(row->incoming.id().toString().equals(row.get("operation_id"))&&(incoming.id()+"/"+d.id()).equals(row.get("storage_key"))&&d.id().toString().equals(row.get("attachment_id"))&&d.nombre().equals(row.get("name"))&&d.tamanio()==((Number)row.get("size_bytes")).longValue()&&d.sha256().equals(row.get("sha256"))&&d.tipo().equals(row.get("media_type"))&&"INGRESO_CIUDADANO".equals(row.get("origin"))))throw new FalloTecnico(409);
        return ack;
    }
    public record ConfirmacionCompleta(FuenteFormulario.Reserva reserva,RegistroInstitucional registro){}
    public List<ConfirmacionCompleta> confirmacionesCompletas(){
        var result=new ArrayList<ConfirmacionCompleta>();for(var row:db.sql.queryForList("SELECT id,body,ack_lease FROM institutional_inbox WHERE document_kind='FORMULARIO_COMPLETO' AND ack_pending=TRUE AND ack_lease IS NOT NULL AND ack_blocked=FALSE AND ack_next_ms<=? ORDER BY created_ms LIMIT 10",clock.millis())){
            try{var e=EntregaCompleta.lector().readValue(row.get("body").toString(),EntregaCompleta.class);var ack=existenteCompleta(e);if(ack==null)throw new FalloTecnico(409);result.add(new ConfirmacionCompleta(new FuenteFormulario.Reserva(UUID.fromString(row.get("ack_lease").toString()),0,1,e),ack));}
            catch(RuntimeException invalid){db.sql.update("UPDATE institutional_inbox SET ack_blocked=TRUE,ack_last_error='INTEGRIDAD_INVALIDA' WHERE source=? AND id=?",SOURCE,row.get("id"));}
        }return List.copyOf(result);
    }
    public void reservaCompletaVencida(UUID id,UUID lease){db.sql.update("UPDATE institutional_inbox SET ack_lease=NULL WHERE source=? AND id=? AND ack_lease=? AND document_kind='FORMULARIO_COMPLETO' AND ack_pending=TRUE",SOURCE,id.toString(),lease.toString());}
    public void validarCompleta(EntregaCompleta e){e.validar(publicTrust,mapper);}
    private static String fingerprint(EntregaCompleta e){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("FORMULARIO_COMPLETO_1:"+e.sha256()+":"+e.huellaProcedencia()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException x){throw new IllegalStateException(x);}}
}
