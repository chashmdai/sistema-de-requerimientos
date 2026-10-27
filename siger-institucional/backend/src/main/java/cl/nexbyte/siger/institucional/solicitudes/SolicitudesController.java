package cl.nexbyte.siger.institucional.solicitudes;
import cl.nexbyte.siger.institucional.nucleo.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/nucleo/solicitudes") @ConditionalOnProperty(name="institucional.solicitudes.habilitadas",havingValue="true")
public class SolicitudesController {
 private final SolicitudesInstitucionales solicitudes;private final CatalogoLaboratorio cat;
 public SolicitudesController(SolicitudesInstitucionales s,CatalogoLaboratorio c){solicitudes=s;cat=c;}
 private CatalogoLaboratorio.Operador actor(Authentication a){return SesionFuncionario.actual(a,cat);}
 @GetMapping List<SolicitudesInstitucionales.Solicitud> listar(Authentication a,@RequestParam(required=false) UUID comunicacion){return solicitudes.listar(actor(a),comunicacion);}
 @GetMapping("/{id}") SolicitudesInstitucionales.Solicitud leer(Authentication a,@PathVariable UUID id){return solicitudes.leer(actor(a),id);}
 @PostMapping ContratosNucleo.Resultado crear(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SolicitudesInstitucionales.Edicion e){return solicitudes.crear(actor(a),key,e);}
 @PutMapping("/{id}") ContratosNucleo.Resultado editar(Authentication a,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SolicitudesInstitucionales.Edicion e){return solicitudes.editar(actor(a),key,id,e);}
 @PostMapping("/{id}/preparacion") ContratosNucleo.Resultado preparar(Authentication a,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SolicitudesInstitucionales.Preparacion e){return solicitudes.preparar(actor(a),key,id,e);}
 @PostMapping("/{id}/seguimientos") ContratosNucleo.Resultado seguir(Authentication a,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SolicitudesInstitucionales.Seguimiento e){return solicitudes.seguimiento(actor(a),key,id,e);}
 @GetMapping("/{id}/seguimientos") List<SolicitudesInstitucionales.Nota> notas(Authentication a,@PathVariable UUID id){return solicitudes.seguimientos(actor(a),id);}
 @GetMapping("/{id}/historial") List<SolicitudesInstitucionales.Historia> historial(Authentication a,@PathVariable UUID id){return solicitudes.historial(actor(a),id);}
 @GetMapping("/{id}/relaciones") SolicitudesInstitucionales.Relaciones relaciones(Authentication a,@PathVariable UUID id){return solicitudes.relaciones(actor(a),id);}
 @PostMapping({"/{id}/responder","/{id}/canalizar","/{id}/escalar"}) void pendiente(Authentication a,@PathVariable UUID id){solicitudes.decisionPendiente(actor(a),id);}
}
