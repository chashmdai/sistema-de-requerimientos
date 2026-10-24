package cl.nexbyte.siger.institucional.formulario;
import org.springframework.web.bind.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import cl.nexbyte.siger.institucional.robustez.*;
/** Hereda autenticación técnica/TLS del filtro operacional, sin cookies funcionarias. */
@RestController @RequestMapping("/operacion/v1/formulario-completo") @ConditionalOnProperty(name="institucional.formulario-completo.habilitado",havingValue="true")
public class OperacionFormularioController {
    private final RecuperarFormulario receiver;private final ControlesOperacion control;
    public OperacionFormularioController(RecuperarFormulario r,ControlesOperacion c){receiver=r;control=c;}
    @PostMapping("/recuperar") RecuperarFormulario.Resultado recuperar(){var c=control.estado();if(!c.recuperar()||!c.contingencia().equals("NORMAL"))throw new FalloTecnico(503);receiver.conciliar();return receiver.ejecutar();}
}
