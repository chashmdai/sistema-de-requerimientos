package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class RevisionExcepcionalController {
    private final RevisionExcepcional flow;private final CatalogoLaboratorio cat;
    public RevisionExcepcionalController(NucleoInstitucional core,CatalogoLaboratorio cat){flow=core.revisionesExcepcionales();this.cat=cat;}
    private CatalogoLaboratorio.Operador actor(Authentication a){return SesionFuncionario.actual(a,cat);}
    @GetMapping("/revisiones-excepcionales") List<Map<String,Object>> bandeja(Authentication a){return flow.bandeja(actor(a));}
    @GetMapping("/casos/{id}/revision-excepcional") Map<String,Object> contexto(Authentication a,@PathVariable UUID id){return flow.contexto(actor(a),id);}
    @GetMapping("/revisiones-excepcionales/{id}") Map<String,Object> paquete(Authentication a,@PathVariable UUID id){return flow.paquete(actor(a),id);}
    @PostMapping("/casos/{id}/revision-excepcional") ContratosNucleo.Resultado solicitar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Solicitar r){return flow.solicitar(actor(a),key,id,r);}
    @PostMapping("/revisiones-excepcionales/{id}/aprobar") ContratosNucleo.Resultado aprobar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Decidir r){return flow.decidir(actor(a),key,id,r,"APROBAR");}
    @PostMapping("/revisiones-excepcionales/{id}/rechazar") ContratosNucleo.Resultado rechazar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Decidir r){return flow.decidir(actor(a),key,id,r,"RECHAZAR");}
    @PostMapping("/revisiones-excepcionales/{id}/revocar") ContratosNucleo.Resultado revocar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Decidir r){return flow.decidir(actor(a),key,id,r,"REVOCAR");}
    @PostMapping("/revisiones-excepcionales/{id}/retirar") ContratosNucleo.Resultado retirar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Decidir r){return flow.decidir(actor(a),key,id,r,"RETIRAR");}
    @PostMapping("/revisiones-excepcionales/{id}/confirmar") ContratosNucleo.Resultado confirmar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RevisionExcepcional.Confirmar r){return flow.confirmar(actor(a),key,id,r);}
}
