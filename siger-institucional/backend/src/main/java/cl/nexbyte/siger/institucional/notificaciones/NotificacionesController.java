package cl.nexbyte.siger.institucional.notificaciones;
import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.robustez.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.beans.factory.ObjectProvider;
import java.util.*;
@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.notificaciones.habilitadas",havingValue="true")
public class NotificacionesController {
 private final CatalogoLaboratorio cat;private final TareasPersonales tareas;private final NotificacionesInstitucionales notices;private final ObjectProvider<CorreosInstitucionales> correos;
 public NotificacionesController(CatalogoLaboratorio c,TareasPersonales t,NotificacionesInstitucionales n,ObjectProvider<CorreosInstitucionales> m){cat=c;tareas=t;notices=n;correos=m;}
 private CatalogoLaboratorio.Operador actor(Authentication a){return SesionFuncionario.actual(a,cat);}
 private CorreosInstitucionales correo(){var c=correos.getIfAvailable();if(c==null)throw new FalloTecnico(409,"CORREO_DESACTIVADO");return c;}
 @GetMapping("/notificaciones") NotificacionesInstitucionales.Bandeja listar(Authentication a,@RequestParam(defaultValue="0")long despues,@RequestParam(defaultValue="30")int limite){return notices.listar(actor(a),despues,limite);}
 public record DetalleAviso(NotificacionesInstitucionales.Notificacion aviso,boolean correoHabilitado){}
 @GetMapping("/notificaciones/{id}") DetalleAviso detalle(Authentication a,@PathVariable UUID id){var aviso=notices.detalle(actor(a),id);return new DetalleAviso(aviso,correos.getIfAvailable()!=null);}
 @GetMapping("/notificaciones/{id}/contexto-institucional") AvisosNucleo.Vista contextoInstitucional(Authentication a,@PathVariable UUID id){return notices.contextoInstitucional(actor(a),id);}
 @GetMapping("/notificaciones/contador") Map<String,Long> contador(Authentication a){return Map.of("pendientes",notices.pendientes(actor(a)));}
 @PostMapping("/notificaciones/{id}/leer") void leer(Authentication a,@PathVariable UUID id){notices.leer(actor(a),id);}
 @GetMapping("/notificaciones/{id}/correo") CorreosInstitucionales.Estado status(Authentication a,@PathVariable UUID id){return notices.estadoCorreo(actor(a),id);}
 @PostMapping("/notificaciones/{id}/correo") CorreosInstitucionales.Estado enviar(Authentication a,@PathVariable UUID id){return correo().enviar(actor(a),id);}
 @PostMapping("/notificaciones/{id}/confirmar-recepcion") void confirmar(Authentication a,@PathVariable UUID id){notices.confirmarCorreo(actor(a),id);}
 @GetMapping("/notificaciones/{id}/intentos") List<Map<String,Object>> intentos(Authentication a,@PathVariable UUID id){return notices.intentosCorreo(actor(a),id);}
 @GetMapping("/notificaciones/{id}/intentos/{intento}/mensaje") MensajesCorreo.Copia copia(Authentication a,@PathVariable UUID id,@PathVariable UUID intento){return notices.copiaCorreo(actor(a),id,intento);}
 @PostMapping("/notificaciones/operacion/procesar") Map<String,Integer> procesar(Authentication a){if(!actor(a).ti())throw new FalloTecnico(403);tareas.temporales();int n=notices.proyectar();notices.recuperarCorreos();return Map.of("notificacionesCreadas",n);}
 @GetMapping("/tareas") List<TareasPersonales.Tarea> tareas(Authentication a,@RequestParam(defaultValue="")String estado){return tareas.listar(actor(a),estado);}
 @GetMapping("/tareas/{id}") TareasPersonales.Tarea tarea(Authentication a,@PathVariable UUID id){return tareas.leer(actor(a),id);}
 @GetMapping("/tareas/{id}/historial") List<TareasPersonales.Historia> historia(Authentication a,@PathVariable UUID id){return tareas.historial(actor(a),id);}
 @PostMapping("/tareas") ContratosNucleo.Resultado crear(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@RequestBody TareasPersonales.Edicion e){return tareas.crear(actor(a),key,e);}
 @PutMapping("/tareas/{id}") ContratosNucleo.Resultado editar(Authentication a,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody TareasPersonales.Edicion e){return tareas.editar(actor(a),key,id,e);}
 @PostMapping("/tareas/{id}/estado") ContratosNucleo.Resultado estado(Authentication a,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody TareasPersonales.Cambio e){return tareas.estado(actor(a),key,id,e);}
}
