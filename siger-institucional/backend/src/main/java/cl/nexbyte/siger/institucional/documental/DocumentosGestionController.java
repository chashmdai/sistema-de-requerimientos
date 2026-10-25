package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.io.*;import java.util.*;import java.util.function.Function;
import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.security.core.Authentication;import org.springframework.http.*;import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/nucleo/casos/{caso}/documentos-gestion")
@ConditionalOnProperty(name="institucional.documental.habilitada",havingValue="true")
public class DocumentosGestionController {
    private final DocumentosGestion service;private final VisorDocumento visor;private final CatalogoLaboratorio catalog;
    public DocumentosGestionController(DocumentosGestion service,VisorDocumento visor,CatalogoLaboratorio catalog){this.service=service;this.visor=visor;this.catalog=catalog;}
    private <T>T seguro(Authentication auth,Function<Operador,T> action){var a=SesionFuncionario.actual(auth,catalog);try{return action.apply(a);}catch(FalloTecnico denied){if(denied.status==403||denied.status==404)service.evento(a,null,null,null,"ACCESO_DENEGADO");throw denied;}}
    @GetMapping public List<DocumentosGestion.Documento> listar(Authentication auth,@PathVariable UUID caso){return seguro(auth,a->service.listar(a,caso));}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentosGestion.Documento cargar(Authentication auth,@PathVariable UUID caso,@RequestHeader("Idempotency-Key")UUID key,@RequestParam("archivo")MultipartFile archivo,@RequestParam(required=false)UUID actuacion,jakarta.servlet.http.HttpServletRequest request){
        var multipart=org.springframework.web.util.WebUtils.getNativeRequest(request,org.springframework.web.multipart.MultipartHttpServletRequest.class);if(multipart==null||!multipart.getMultiFileMap().keySet().equals(Set.of("archivo"))||multipart.getFiles("archivo").size()!=1||!Set.of("actuacion","_csrf").containsAll(request.getParameterMap().keySet()))throw new FalloTecnico(400);
        if(archivo.isEmpty())throw new FalloTecnico(422,"DOCUMENTO_RECHAZADO");if(archivo.getSize()>FormatosDocumento.MAX)throw new FalloTecnico(413);
        return seguro(auth,a->{try(var input=archivo.getInputStream()){return service.cargar(a,key,caso,actuacion,archivo.getOriginalFilename(),input);}catch(IOException failure){throw new FalloTecnico(503);}});
    }
    public record Vinculo(UUID casoOrigen,UUID actuacion){}
    @PostMapping("/{documento}/vinculos") public DocumentosGestion.Documento vincular(Authentication auth,@PathVariable UUID caso,@PathVariable UUID documento,@RequestHeader("Idempotency-Key")UUID key,@RequestBody Vinculo body){return seguro(auth,a->service.vincular(a,key,documento,body.casoOrigen(),caso,body.actuacion()));}
    @GetMapping("/{documento}/historial") public List<DocumentosGestion.Movimiento> historial(Authentication auth,@PathVariable UUID caso,@PathVariable UUID documento){return seguro(auth,a->service.historial(a,caso,documento));}
    @GetMapping("/{documento}/vista") public VisorDocumento.Vista vista(Authentication auth,@PathVariable UUID caso,@PathVariable UUID documento){return seguro(auth,a->{var ref=service.referencia(a,caso,documento);var v=visor.vista(ref);service.evento(a,documento,caso,null,"VISTA_CONSULTADA");return v;});}
    @RequestMapping(value="/{documento}/paginas/{pagina}",method={RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> pagina(Authentication auth,@PathVariable UUID caso,@PathVariable UUID documento,@PathVariable int pagina,jakarta.servlet.http.HttpServletRequest request){return seguro(auth,a->{var ref=service.referencia(a,caso,documento);var p=visor.pagina(ref,pagina);return TransferenciaDocumento.responder(p.path(),p.tamanio(),p.sha256(),"pagina.png","image/png",true,request,mode->service.evento(a,documento,caso,null,"PAGINA_SOLICITADA"));});}
    @RequestMapping(value={"/{documento}/original","/{documento}/representacion"},method={RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> original(Authentication auth,@PathVariable UUID caso,@PathVariable UUID documento,jakarta.servlet.http.HttpServletRequest request){return seguro(auth,a->{var ref=service.referencia(a,caso,documento);var file=service.verificarArchivo(ref);boolean inline=request.getRequestURI().endsWith("/representacion");if(inline&&!Set.of("AUDIO","VIDEO").contains(service.documento(a,caso,documento).representacion()))throw new FalloTecnico(404);var d=ref.original();return TransferenciaDocumento.responder(file,d.tamanio(),d.sha256(),d.nombre(),d.tipo(),inline,request,mode->service.evento(a,documento,caso,null,mode.equals("RANGO_INVALIDO")?mode:inline?"REPRESENTACION_SOLICITADA":"ORIGINAL_SOLICITADO"));});}
}
