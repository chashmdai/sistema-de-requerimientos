package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos;import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;import cl.nexbyte.siger.institucional.robustez.*;
import java.util.*;import java.util.function.Function;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class PlazosRegistroController {
 private final PlazosRegistro service;private final CatalogoLaboratorio catalog;private final BaseJdbc db;
 public PlazosRegistroController(PlazosRegistro service,CatalogoLaboratorio catalog,BaseJdbc db){this.service=service;this.catalog=catalog;this.db=db;}
 @ModelAttribute public void privado(jakarta.servlet.http.HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
 private String tipo(String t){return switch(t){case "comunicaciones"->"COM";case "requerimientos"->"REQ";case "casos"->"CASO";default->throw new FalloTecnico(404);};}
 private <T>T seguro(Authentication auth,UUID resource,Function<Operador,T> op){var a=SesionFuncionario.actual(auth,catalog);try{return op.apply(a);}catch(FalloTecnico e){if(e.status==403||e.status==404)db.sql.update("INSERT INTO matter_access_event(actor_id,resource_id,action_code,at_ms) VALUES(?,?,?,?)",a.id(),resource==null?null:resource.toString(),"PLAZO_REGISTRO_DENEGADO",System.currentTimeMillis());throw e;}}
 @GetMapping("/nucleo/{contexto}/{recurso}/plazos-registro") public PlazosRegistro.Pagina listar(Authentication auth,@PathVariable String contexto,@PathVariable UUID recurso,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="25")int tamanio){return seguro(auth,recurso,a->service.listar(a,tipo(contexto),recurso,pagina,tamanio));}
 @PostMapping("/nucleo/{contexto}/{recurso}/plazos-registro/mediciones") public PlazosRegistro.Medicion medir(Authentication auth,@PathVariable String contexto,@PathVariable UUID recurso,@RequestHeader("Idempotency-Key")UUID key,@RequestHeader("X-Siger-Actor")String actor,@RequestBody PlazosRegistro.Solicitud r){return seguro(auth,recurso,a->{if(!a.id().equals(actor))throw new FalloTecnico(409,"CUENTA_DE_OPERACION_CAMBIADA");return service.medir(a,key,tipo(contexto),recurso,r);});}
 @GetMapping("/nucleo/identidad/catalogo-plazos") public CatalogoPlazos.Contexto catalogo(Authentication auth){return seguro(auth,null,service::catalogo);}
}
