package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static cl.nexbyte.siger.institucional.nucleo.NucleoU2.*;

@RestController @RequestMapping("/nucleo/institucional")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class NucleoU2Controller {
 private final NucleoInstitucional core;private final NucleoU2 flow;private final CatalogoLaboratorio cat;
 public NucleoU2Controller(NucleoInstitucional core,CatalogoLaboratorio cat){this.core=core;this.flow=core.u2();this.cat=cat;}
 private CatalogoLaboratorio.Operador a(Authentication a){return SesionFuncionario.actual(a,cat);}
 @PostMapping("/primera-atencion") Resultado primera(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@RequestBody PrimeraAtencion r){return flow.primeraAtencion(a(a),key,r);}
 @PostMapping("/comunicaciones/{id}/asumir-propia") Resultado propia(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody AsumirPropia r){return flow.asumirPropia(a(a),key,id,r);}
 @PostMapping("/comunicaciones/{id}/distribuir") Resultado distribuir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Encaminar r){return flow.distribuir(a(a),key,id,r);}
 @GetMapping("/solicitudes") List<Map<String,Object>> solicitudes(Authentication a){return flow.solicitudes(a(a));}
 @GetMapping("/solicitudes/{id}") Map<String,Object> solicitud(Authentication a,@PathVariable UUID id){return flow.solicitud(a(a),id);}
 @PostMapping("/solicitudes/{id}/redirigir") Resultado redirigir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Encaminar r){return flow.redirigir(a(a),key,id,r);}
 @PostMapping("/solicitudes/{id}/asumir") Resultado asumir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Asumir r){return flow.asumir(a(a),key,id,r);}
 @GetMapping("/requerimientos") List<Map<String,Object>> requerimientos(Authentication a){return flow.requerimientos(a(a));}
 @GetMapping("/requerimientos/referencia") List<Map<String,Object>> referencias(Authentication a,@RequestParam String folio){return flow.referencias(a(a),folio);}
 @GetMapping("/requerimientos/{id}") Map<String,Object> requerimiento(Authentication a,@PathVariable UUID id){return flow.requerimiento(a(a),id);}
 @PostMapping("/requerimientos/{id}/actuaciones") Resultado actuar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision r){var actor=a(a);return core.soloReintentoGestion(actor,key,()->flow.actuar(actor,key,id,r,false));}
 @PostMapping("/requerimientos/{id}/cerrar") Resultado cerrar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision r){return flow.actuar(a(a),key,id,r,true);}
 @PostMapping("/requerimientos/{id}/complejidad") Resultado clasificar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Clasificacion r){return flow.clasificar(a(a),key,id,r);}
 @PostMapping("/requerimientos/{id}/casos") Resultado abrir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Apertura r){return flow.abrirCaso(a(a),key,id,r);}
 @PostMapping("/requerimientos/{id}/colaboraciones") Resultado colaborar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Colaborar r){return flow.colaborar(a(a),key,id,r);}
 @GetMapping("/colaboraciones") List<Map<String,Object>> colaboraciones(Authentication a){return flow.colaboraciones(a(a));}
 @PostMapping("/colaboraciones/{id}/decidir") Resultado decidir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody ResolverColaboracion r){return flow.resolver(a(a),key,id,r);}
}
