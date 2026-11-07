package cl.nexbyte.siger.institucional.documental;

import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.io.*;import java.util.*;import java.util.function.Function;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo/requerimientos/{requerimiento}/documentos-gestion")
@ConditionalOnProperty(name="institucional.documental.habilitada",havingValue="true")
public class DocumentosRequerimientoController {
    private final DocumentosRequerimiento service;private final VisorDocumento visor;private final CatalogoLaboratorio catalog;
    public DocumentosRequerimientoController(DocumentosRequerimiento service,VisorDocumento visor,CatalogoLaboratorio catalog){this.service=service;this.visor=visor;this.catalog=catalog;}
    @ModelAttribute public void privado(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private <T>T seguro(Authentication auth,UUID req,Function<Operador,T> action){var a=SesionFuncionario.actual(auth,catalog);try{return action.apply(a);}catch(FalloTecnico e){if(e.status==403||e.status==404)service.auditar(a,req,null,"DOCUMENTO_REQ_DENEGADO");throw e;}}
    @GetMapping public DocumentosRequerimiento.Lista listar(Authentication auth,@PathVariable UUID requerimiento){return seguro(auth,requerimiento,a->service.listar(a,requerimiento));}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentosRequerimiento.Documento cargar(Authentication auth,@PathVariable UUID requerimiento,@RequestHeader("Idempotency-Key")UUID key,@RequestParam("archivo")MultipartFile archivo,@RequestParam(required=false)Long gestionSeq,jakarta.servlet.http.HttpServletRequest request){
        var multipart=org.springframework.web.util.WebUtils.getNativeRequest(request,org.springframework.web.multipart.MultipartHttpServletRequest.class);
        if(multipart==null||!multipart.getMultiFileMap().keySet().equals(Set.of("archivo"))||multipart.getFiles("archivo").size()!=1||!Set.of("gestionSeq","_csrf").containsAll(request.getParameterMap().keySet()))throw new FalloTecnico(400);
        if(archivo.isEmpty())throw new FalloTecnico(422,"DOCUMENTO_RECHAZADO");if(archivo.getSize()>FormatosDocumento.MAX)throw new FalloTecnico(413);
        return seguro(auth,requerimiento,a->{try(var input=archivo.getInputStream()){return service.cargar(a,key,requerimiento,gestionSeq,archivo.getOriginalFilename(),input);}catch(IOException e){throw new FalloTecnico(503);}});
    }
    @PostMapping("/{documento}/proteccion") public ResponseEntity<DocumentosRequerimiento.Documento> proteger(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentosRequerimiento.Proteccion body){return seguro(auth,requerimiento,a->{var d=service.proteger(a,key,requerimiento,documento,body);return d==null?ResponseEntity.noContent().build():ResponseEntity.ok(d);});}
    @PostMapping("/{documento}/vinculos") public DocumentosRequerimiento.Documento vincular(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentosRequerimiento.Vinculo body){if(body==null)throw new FalloTecnico(400);return seguro(auth,requerimiento,a->service.vincular(a,key,requerimiento,documento,body.gestionSeq()));}
    @GetMapping("/{documento}/historial") public List<DocumentosRequerimiento.Movimiento> historial(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento){return seguro(auth,requerimiento,a->service.historial(a,requerimiento,documento));}
    @GetMapping("/{documento}/vista") public VisorDocumento.Vista vista(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento){return seguro(auth,requerimiento,a->{var ref=service.referencia(a,requerimiento,documento,"CONTENIDO");var v=visor.vista(ref);service.revalidar(a,requerimiento,documento,"CONTENIDO");service.auditar(a,requerimiento,documento,"VISTA_CONSULTADA");return new VisorDocumento.Vista(v.representacion(),v.paginas(),v.perfil(),v.originalInline(),service.documento(a,requerimiento,documento).descargable());});}
    @RequestMapping(value="/{documento}/paginas/{pagina}",method={RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> pagina(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento,@PathVariable int pagina,jakarta.servlet.http.HttpServletRequest request){return seguro(auth,requerimiento,a->{var ref=service.referencia(a,requerimiento,documento,"CONTENIDO");var p=visor.pagina(ref,pagina);service.revalidar(a,requerimiento,documento,"CONTENIDO");return TransferenciaDocumento.responder(p.path(),p.tamanio(),p.sha256(),"pagina.png","image/png",true,request,mode->service.auditar(a,requerimiento,documento,"PAGINA_SOLICITADA"),()->service.revalidar(a,requerimiento,documento,"CONTENIDO"));});}
    @RequestMapping(value={"/{documento}/original","/{documento}/representacion"},method={RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> original(Authentication auth,@PathVariable UUID requerimiento,@PathVariable UUID documento,jakarta.servlet.http.HttpServletRequest request){return seguro(auth,requerimiento,a->{boolean inline=request.getRequestURI().endsWith("/representacion");var ref=service.referencia(a,requerimiento,documento,"DESCARGA");var file=service.verificarArchivo(ref);if(inline&&!Set.of("AUDIO","VIDEO").contains(service.documento(a,requerimiento,documento).representacion()))throw new FalloTecnico(404);var d=ref.original();return TransferenciaDocumento.responder(file,d.tamanio(),d.sha256(),d.nombre(),d.tipo(),inline,request,mode->service.auditar(a,requerimiento,documento,mode.equals("RANGO_INVALIDO")?mode:inline?"REPRESENTACION_SOLICITADA":"ORIGINAL_SOLICITADO"),()->service.revalidar(a,requerimiento,documento,"DESCARGA"));});}
}
