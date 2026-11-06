package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static cl.nexbyte.siger.institucional.nucleo.TerminoColaboracion.*;

@RestController @RequestMapping("/nucleo/institucional")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class TerminoColaboracionController {
    private final TerminoColaboracion flow;private final CatalogoLaboratorio catalogo;
    public TerminoColaboracionController(NucleoInstitucional core,CatalogoLaboratorio catalogo){flow=core.terminoColaboracion();this.catalogo=catalogo;}
    private CatalogoLaboratorio.Operador a(Authentication a){return SesionFuncionario.actual(a,catalogo);}
    @GetMapping("/colaboraciones/terminos") List<Map<String,Object>> bandeja(Authentication a){return flow.bandeja(a(a));}
    @GetMapping("/requerimientos/{id}/terminos-colaboracion") List<Map<String,Object>> req(Authentication a,@PathVariable UUID id){return flow.delReq(a(a),id);}
    @GetMapping("/casos/{id}/terminos-colaboracion") List<Map<String,Object>> caso(Authentication a,@PathVariable UUID id){return flow.delCaso(a(a),id);}
    @PostMapping("/colaboraciones/{id}/solicitar-termino") Resultado solicitar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody Solicitar r){return flow.solicitar(a(a),key,id,r);}
    @PostMapping("/colaboraciones/terminos/{id}/decidir") Resultado decidir(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody Decidir r){return flow.decidir(a(a),key,id,r);}
}
