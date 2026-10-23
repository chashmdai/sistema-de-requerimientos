package cl.nexbyte.siger.institucional.robustez;
import org.springframework.context.annotation.Profile;import org.springframework.beans.factory.ObjectProvider;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @Profile("jdbc-reception")
public class RecuperacionMetricasController {
    private final ObjectProvider<RecuperarEntregasV2> recovery;
    public RecuperacionMetricasController(ObjectProvider<RecuperarEntregasV2> recovery){this.recovery=recovery;}
    @GetMapping("/operacion/v1/recuperacion/metricas") Map<String,Object> metrics(){var service=recovery.getIfAvailable();return service==null?Map.of("habilitada",false):service.metricas();}
}
