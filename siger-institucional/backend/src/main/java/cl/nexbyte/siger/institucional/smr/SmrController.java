package cl.nexbyte.siger.institucional.smr;

import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.smr.habilitado",havingValue="true")
public class SmrController {
    private final BaseJdbc db;private final CatalogoLaboratorio catalog;private final AccesoSmr access;private final RecepcionInstitucionalJdbc receiver;
    private final ArchivoInstitucional files;private final ControlesOperacion control;private final Environment env;
    private final ObjectProvider<FormularioInstitucionalConfiguration.Planificador> scheduler;private final ObjectProvider<RecuperarFormulario> pull;
    public SmrController(BaseJdbc db,CatalogoLaboratorio catalog,AccesoSmr access,RecepcionInstitucionalJdbc receiver,ArchivoInstitucional files,
        ControlesOperacion control,Environment env,ObjectProvider<FormularioInstitucionalConfiguration.Planificador> scheduler,ObjectProvider<RecuperarFormulario> pull){
        this.db=db;this.catalog=catalog;this.access=access;this.receiver=receiver;this.files=files;this.control=control;this.env=env;this.scheduler=scheduler;this.pull=pull;
    }
    private CatalogoLaboratorio.Operador actor(Authentication auth){return SesionFuncionario.actual(auth,catalog);}
    public record Sesion(boolean autenticado,CatalogoLaboratorio.Operador operador,String modo,boolean lectura,boolean operacion,boolean sintetico){}
    @GetMapping("/session") public Sesion session(Authentication auth){boolean valid=auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails;var a=valid?actor(auth):null;
        return new Sesion(valid,a,"SMR",valid&&access.tiene(a.id(),"LECTURA"),valid&&access.tiene(a.id(),"OPERACION"),env.getProperty("institucional.smr.sintetico",Boolean.class,false));}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName(),"parameterName",token.getParameterName());}
    private static final String JOIN=" FROM communication_registry c JOIN institutional_inbox i ON c.source=i.source AND c.operacion_id=i.id WHERE i.document_kind='FORMULARIO_COMPLETO'";
    public record Resumen(UUID id,String folio,Instant registradaEn,String canal,String tipo,String estado,int adjuntos){}
    public record Pagina(List<Resumen> items,long total,int pagina,int limite){}
    private String estado(Map<String,Object> r){return Boolean.TRUE.equals(r.get("ack_pending"))?(Boolean.TRUE.equals(r.get("ack_blocked"))?"ACK_REQUIERE_REVISION":"ACK_PENDIENTE"):"CONFIRMADA";}
    private Resumen resumen(Map<String,Object> r){var e=EntregaCompleta.lector().readValue(r.get("body").toString(),EntregaCompleta.class);
        return new Resumen(UUID.fromString(r.get("comunicacion_id").toString()),r.get("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+((Number)r.get("folio_width")).intValue()+"d",((Number)r.get("numero")).longValue()),Instant.ofEpochMilli(((Number)r.get("registrada_ms")).longValue()),r.get("canal").toString(),e.comunicacion().path("tipo").asString(),estado(r),e.adjuntos().size());}
    @GetMapping("/smr/comunicaciones") public Pagina listar(Authentication auth,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String tipo,
        @RequestParam(defaultValue="")String canal,@RequestParam(defaultValue="")String estado,@RequestParam(defaultValue="")String desde,@RequestParam(defaultValue="")String hasta,
        @RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){
        access.exigir(actor(auth),"LECTURA");if(pagina<0||pagina>100000||limite<1||limite>100||!q.matches("[A-Za-z0-9-]{0,40}"))throw new FalloTecnico(400);
        var sql=new StringBuilder(JOIN);var args=new ArrayList<Object>();
        if(!q.isEmpty()){sql.append(" AND CONCAT(c.folio_prefix,'-',LPAD(c.numero,c.folio_width,'0')) LIKE ?");args.add("%"+q.toUpperCase(Locale.ROOT)+"%");}
        if(!tipo.isEmpty()){if(!Set.of("ADULTO_CU","ADULTO_DECLARADO","NNA").contains(tipo))throw new FalloTecnico(400);sql.append(" AND i.body LIKE ?");args.add("%\"tipo\":\""+tipo+"\"%");}
        if(!canal.isEmpty()){if(!Set.of("FORMULARIO_WEB_PUBLICO","FORMULARIO_WEB_NNA").contains(canal))throw new FalloTecnico(400);sql.append(" AND c.canal=?");args.add(canal);}
        if(!estado.isEmpty())switch(estado){case "CONFIRMADA"->sql.append(" AND i.ack_pending=FALSE");case "ACK_PENDIENTE"->sql.append(" AND i.ack_pending=TRUE AND i.ack_blocked=FALSE");case "ACK_REQUIERE_REVISION"->sql.append(" AND i.ack_pending=TRUE AND i.ack_blocked=TRUE");default->throw new FalloTecnico(400);}
        try{if(!desde.isEmpty()){sql.append(" AND c.registrada_ms>=?");args.add(LocalDate.parse(desde).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli());}if(!hasta.isEmpty()){sql.append(" AND c.registrada_ms<?");args.add(LocalDate.parse(hasta).plusDays(1).atStartOfDay(ZoneId.of("America/Santiago")).toInstant().toEpochMilli());}}catch(java.time.DateTimeException ex){throw new FalloTecnico(400);}
        long count=db.sql.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());args.add(limite);args.add(pagina*limite);
        var rows=db.sql.queryForList("SELECT c.*,i.body,i.ack_pending,i.ack_blocked"+sql+" ORDER BY c.numero DESC LIMIT ? OFFSET ?",args.toArray());
        return new Pagina(rows.stream().map(this::resumen).toList(),count,pagina,limite);
    }
    private EntregaCompleta entrega(Authentication auth,UUID id){access.exigir(actor(auth),"LECTURA");var e=receiver.completaRegistrada(id);receiver.registroCompletoConservado(e,files);return e;}
    public record Detalle(Resumen resumen,EntregaCompleta entrega,RegistroInstitucional recepcion,Map<String,Object> confirmacion){}
    @GetMapping("/smr/comunicaciones/{id}") public Detalle detalle(Authentication auth,@PathVariable UUID id){var e=entrega(auth,id);
        var row=db.sql.queryForList("SELECT c.*,i.body,i.ack_pending,i.ack_blocked,i.ack_attempts,i.ack_last_error,i.ack_next_ms"+JOIN+" AND c.comunicacion_id=?",id.toString()).getFirst();
        var ack=new LinkedHashMap<String,Object>();for(String k:List.of("ack_pending","ack_blocked","ack_attempts","ack_last_error","ack_next_ms"))ack.put(k,row.get(k));
        return new Detalle(resumen(row),e,receiver.registroCompletoConservado(e,files),ack);
    }
    @GetMapping("/smr/comunicaciones/{id}/comprobante") public ResponseEntity<RegistroInstitucional> comprobante(Authentication auth,@PathVariable UUID id){var e=entrega(auth,id);return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=\"comprobante-"+id+".json\"").body(receiver.registroCompletoConservado(e,files));}
    @GetMapping("/smr/estado") public Map<String,Object> estado(Authentication auth){var a=actor(auth);if(!access.tiene(a.id(),"LECTURA")&&!access.tiene(a.id(),"OPERACION"))throw new FalloTecnico(403);
        var result=new LinkedHashMap<String,Object>();var s=scheduler.getIfAvailable();result.put("transporteConfigurado",s!=null);result.put("consumidor",s==null?Map.of("automatico",false,"estado","DESACTIVADO_POR_CONFIGURACION"):s.estado());result.put("control",control.estado());
        result.put("enCurso",pull.getIfAvailable()!=null&&pull.getIfAvailable().enCurso());result.put("registrosDurables",db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class));result.put("ackPendientes",db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Long.class));result.put("ackBloqueados",db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE AND ack_blocked=TRUE",Long.class));result.put("origen",env.getRequiredProperty("institucional.formulario-completo.origen-publico"));return result;
    }
    public record Cambiar(boolean recuperar,String motivo){}
    @PostMapping("/smr/control") public ControlesOperacion.Control cambiar(Authentication auth,@RequestHeader("If-Match")long revision,@RequestBody Cambiar body){var a=actor(auth);access.exigir(a,"OPERACION");if(body.recuperar()&&pull.getIfAvailable()==null)throw new FalloTecnico(409,"TRANSPORTE_NO_CONFIGURADO");return control.cambiar(revision,body.recuperar(),body.recuperar(),body.recuperar()?"NORMAL":"SOLO_CONSULTA",body.motivo(),a.id());}
    @PostMapping("/smr/conciliar") public Map<String,Integer> conciliar(Authentication auth){var a=actor(auth);access.exigir(a,"OPERACION");if(!control.estado().recuperar())throw new FalloTecnico(409);var r=pull.getIfAvailable();if(r==null)throw new FalloTecnico(409);db.event("CONTROL",a.id(),"SMR_CONCILIACION_MANUAL");return Map.of("confirmadas",r.conciliar());}
    public record Reintento(int intentos){}
    @PostMapping("/smr/comunicaciones/{id}/reintentar-ack") public void reintentar(Authentication auth,@PathVariable UUID id,@RequestBody Reintento body){var a=actor(auth);access.exigir(a,"OPERACION");var e=receiver.completaRegistrada(id);receiver.reintentarConfirmacion(e.id(),body.intentos());db.event("CONTROL",a.id(),"SMR_ACK_REINTENTO_MANUAL");}
    public record Documento(UUID id,String origen,EntregaCompleta.Adjunto original,String representacion,boolean descargable){}
    @GetMapping("/comunicaciones/{id}/documentos") public List<Documento> documentos(Authentication auth,@PathVariable UUID id){var e=entrega(auth,id);return db.sql.queryForList("SELECT id,attachment_id FROM institution_document WHERE communication_id=? AND origin='INGRESO_CIUDADANO' ORDER BY id",id.toString()).stream().map(r->new Documento(UUID.fromString(r.get("id").toString()),"INGRESO_CIUDADANO",e.adjuntos().stream().filter(f->f.id().toString().equals(r.get("attachment_id"))).findFirst().orElseThrow(()->new FalloTecnico(409)),"DESCARGA",true)).toList();}
    @RequestMapping(value="/comunicaciones/{id}/documentos/{documento}/original",method={RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> original(Authentication auth,@PathVariable UUID id,@PathVariable UUID documento,jakarta.servlet.http.HttpServletRequest request)throws java.io.IOException{
        var e=entrega(auth,id);var a=actor(auth);var rows=db.sql.queryForList("SELECT attachment_id,storage_key FROM institution_document WHERE id=? AND communication_id=? AND origin='INGRESO_CIUDADANO'",documento.toString(),id.toString());if(rows.isEmpty())throw new FalloTecnico(404);var row=rows.getFirst();var f=e.adjuntos().stream().filter(d->d.id().toString().equals(row.get("attachment_id"))).findFirst().orElseThrow(()->new FalloTecnico(409));
        return cl.nexbyte.siger.institucional.documental.TransferenciaDocumento.responder(files.verificar(row.get("storage_key").toString(),f),f.tamanio(),f.sha256(),f.nombre(),f.tipo(),false,request,code->db.sql.update("INSERT INTO institution_document_access(document_id,communication_id,actor_id,at_ms,representation) VALUES(?,?,?,?,?)",documento.toString(),id.toString(),a.id(),System.currentTimeMillis(),code));
    }
}
