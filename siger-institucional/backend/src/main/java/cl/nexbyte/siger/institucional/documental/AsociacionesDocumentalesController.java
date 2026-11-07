package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;import java.util.function.Function;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/nucleo/{contexto}/{recurso}/documentos-gestion/{documento}/asociaciones") @ConditionalOnProperty(name="institucional.documental.habilitada",havingValue="true")
public class AsociacionesDocumentalesController {
 private final AsociacionesDocumentales service;private final CatalogoLaboratorio catalog;private final BaseJdbc db;
 public AsociacionesDocumentalesController(AsociacionesDocumentales service,CatalogoLaboratorio catalog,BaseJdbc db){this.service=service;this.catalog=catalog;this.db=db;}
 @ModelAttribute public void privado(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
 private static String tipo(String c){return switch(c){case "casos"->"CASO";case "requerimientos"->"REQ";default->throw new FalloTecnico(404);};}
 private <T>T seguro(Authentication auth,UUID doc,Function<Operador,T> op){var a=SesionFuncionario.actual(auth,catalog);try{return op.apply(a);}catch(FalloTecnico e){if(e.status==403||e.status==404)db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),doc.toString(),"ASOCIACION_DOCUMENTAL_DENEGADA",System.currentTimeMillis());throw e;}}
 @GetMapping public AsociacionesDocumentales.Vista ver(Authentication auth,@PathVariable String contexto,@PathVariable UUID recurso,@PathVariable UUID documento){return seguro(auth,documento,a->service.ver(a,tipo(contexto),recurso,documento));}
 @PostMapping public AsociacionesDocumentales.Vista rectificar(Authentication auth,@PathVariable String contexto,@PathVariable UUID recurso,@PathVariable UUID documento,@RequestHeader("Idempotency-Key")UUID key,@RequestHeader("X-Siger-Actor")String actor,@RequestBody AsociacionesDocumentales.Rectificar r){return seguro(auth,documento,a->{if(!a.id().equals(actor))throw new FalloTecnico(409,"CUENTA_DE_OPERACION_CAMBIADA");return service.rectificar(a,key,tipo(contexto),recurso,documento,r);});}
}
