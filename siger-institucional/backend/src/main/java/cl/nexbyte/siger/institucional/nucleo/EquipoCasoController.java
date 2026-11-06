package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import cl.nexbyte.siger.institucional.notificaciones.TareasPersonales;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;import static cl.nexbyte.siger.institucional.nucleo.EquipoCaso.*;
@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class EquipoCasoController {
 private final NucleoInstitucional core;private final CatalogoLaboratorio cat;
 public EquipoCasoController(NucleoInstitucional core,CatalogoLaboratorio cat){this.core=core;this.cat=cat;}
 private CatalogoLaboratorio.Operador a(Authentication a){return SesionFuncionario.actual(a,cat);}
 @GetMapping("/casos/{id}/equipo") Map<String,Object> equipo(Authentication a,@PathVariable UUID id){return core.equipo().ver(a(a),id);}
 @PostMapping("/casos/{id}/equipo") Resultado incorporar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Incorporar r){return core.equipo().incorporar(a(a),key,id,r);}
 @PostMapping("/casos/{id}/equipo/retirar") Resultado retirar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Retirar r){return core.equipo().retirar(a(a),key,id,r);}
 @GetMapping("/casos/{id}/agenda") List<TareasPersonales.Tarea> agenda(Authentication a,@PathVariable UUID id){return core.tareas().delCaso(a(a),id);}
 @PostMapping("/casos/{id}/agenda") Resultado crear(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody TareasPersonales.Edicion r){if(!"CASO".equals(r.tipoRecurso())||!id.equals(r.recurso()))throw new FalloTecnico(400);return core.tareas().crear(a(a),key,r);}
 private void tarea(Authentication a,UUID id){if(!"CASO".equals(core.tareas().leer(a(a),id).tipoRecurso()))throw new FalloTecnico(409);}
 @GetMapping("/agenda/tareas/{id}") TareasPersonales.Tarea leer(Authentication a,@PathVariable UUID id){tarea(a,id);return core.tareas().leer(a(a),id);}
 @PutMapping("/agenda/tareas/{id}") Resultado editar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody TareasPersonales.Edicion r){tarea(a,id);return core.tareas().editar(a(a),key,id,r);}
 @PostMapping("/agenda/tareas/{id}/editar") Resultado editarComando(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody TareasPersonales.Edicion r){return editar(a,key,id,r);}
 @PostMapping("/agenda/tareas/{id}/estado") Resultado estado(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody TareasPersonales.Cambio r){tarea(a,id);return core.tareas().estado(a(a),key,id,r);}
 @GetMapping("/agenda/tareas/{id}/historial") List<TareasPersonales.Historia> historia(Authentication a,@PathVariable UUID id){tarea(a,id);return core.tareas().historial(a(a),id);}
 @GetMapping("/agenda/tareas/{id}/intentos") List<Map<String,Object>> intentos(Authentication a,@PathVariable UUID id){tarea(a,id);return core.tareas().intentos(a(a),id);}
 @PostMapping("/agenda/tareas/{id}/intentos") Resultado intento(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody TareasPersonales.Intento r){tarea(a,id);return core.tareas().intento(a(a),key,id,r);}
 @PostMapping("/casos/{id}/espera") Resultado esperar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Espera r){return core.equipo().solicitarEspera(a(a),key,id,r);}
 @PostMapping("/casos/{id}/espera/{request}") Resultado decidir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@PathVariable UUID request,@RequestBody ResolverEspera r){return core.equipo().resolverEspera(a(a),key,id,request,r);}
}
