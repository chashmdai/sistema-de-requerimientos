package cl.nexbyte.siger.institucional.formulario;
import org.springframework.web.bind.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
/** Solo estado técnico; hereda autenticación operacional. No incluye entregas ni credenciales. */
@RestController @RequestMapping("/operacion/v1/formulario-completo") @ConditionalOnProperty(name="institucional.formulario-completo.automatico",havingValue="true")
public class EstadoConsumidorFormularioController {
 private final FormularioInstitucionalConfiguration.Planificador scheduler;
 public EstadoConsumidorFormularioController(FormularioInstitucionalConfiguration.Planificador scheduler){this.scheduler=scheduler;}
 @GetMapping("/estado") FormularioInstitucionalConfiguration.Planificador.Estado estado(){return scheduler.estado();}
}
