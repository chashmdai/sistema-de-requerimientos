package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.nucleo.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/nucleo/subrogancias")
@ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class SubroganciasController {
    private final SubroganciasInstitucionales flow;private final CatalogoLaboratorio cat;
    public SubroganciasController(AutoridadesInstitucionales autoridad,CatalogoLaboratorio cat){flow=autoridad.subrogancias();this.cat=cat;}
    private String actor(Authentication a){return SesionFuncionario.actual(a,cat).id();}
    public record Ejecutar(long revision){}
    @GetMapping Map<String,Object> bandeja(Authentication a){return flow.bandeja(actor(a));}
    @PostMapping SubroganciasInstitucionales.Resultado solicitar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SubroganciasInstitucionales.Solicitud r){return flow.solicitar(actor(a),key,r);}
    @PostMapping("/{id}/aprobar") SubroganciasInstitucionales.Resultado aprobar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SubroganciasInstitucionales.Decision r){return flow.decidir(actor(a),key,id,r,true);}
    @PostMapping("/{id}/rechazar") SubroganciasInstitucionales.Resultado rechazar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SubroganciasInstitucionales.Decision r){return flow.decidir(actor(a),key,id,r,false);}
    @PostMapping("/{id}/ejecutar") SubroganciasInstitucionales.Resultado ejecutar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Ejecutar r){return flow.ejecutar(actor(a),key,id,r.revision());}
    @PostMapping("/{id}/revocar") SubroganciasInstitucionales.Resultado revocar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SubroganciasInstitucionales.Decision r){return flow.terminar(actor(a),key,id,r,"REVOCAR");}
    @PostMapping("/{id}/retornar") SubroganciasInstitucionales.Resultado retornar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SubroganciasInstitucionales.Decision r){return flow.terminar(actor(a),key,id,r,"RETORNAR");}
    @PostMapping("/{id}/retirar") SubroganciasInstitucionales.Resultado retirar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SubroganciasInstitucionales.Decision r){return flow.terminar(actor(a),key,id,r,"RETIRAR");}
}
