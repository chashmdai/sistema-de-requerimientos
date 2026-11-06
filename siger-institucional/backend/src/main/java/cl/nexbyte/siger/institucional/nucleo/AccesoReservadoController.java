package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public final class AccesoReservadoController {
    private final AccesoReservado flow;private final CatalogoLaboratorio cat;
    public AccesoReservadoController(NucleoInstitucional core,CatalogoLaboratorio cat){flow=core.accesosReservados();this.cat=cat;}
    private CatalogoLaboratorio.Operador actor(Authentication a){return SesionFuncionario.actual(a,cat);}
    @GetMapping("/accesos-reservados") public List<Map<String,Object>> bandeja(Authentication a){return flow.bandeja(actor(a));}
    @GetMapping("/casos/{cas}/proteccion-documental/{doc}/acceso-reservado") public Map<String,Object> contexto(Authentication a,@PathVariable UUID cas,@PathVariable UUID doc){return flow.contexto(actor(a),cas,doc);}
    @PostMapping("/casos/{cas}/proteccion-documental/{doc}/acceso-reservado") public ContratosNucleo.Resultado proponer(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID cas,@PathVariable UUID doc,@RequestBody AccesoReservado.Proponer r){return flow.proponer(actor(a),key,cas,doc,r);}
    @PostMapping("/accesos-reservados/{id}/{action:aprobar|rechazar|retirar|revocar}") public ContratosNucleo.Resultado decidir(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@PathVariable String action,@RequestBody AccesoReservado.Decidir r){return flow.decidir(actor(a),key,id,r,action.toUpperCase(Locale.ROOT));}
}
