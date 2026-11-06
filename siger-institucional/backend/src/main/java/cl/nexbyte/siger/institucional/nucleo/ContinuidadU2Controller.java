package cl.nexbyte.siger.institucional.nucleo;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static cl.nexbyte.siger.institucional.nucleo.ContinuidadU2.*;

@RestController @RequestMapping("/nucleo/institucional/continuidad")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class ContinuidadU2Controller {
 private final ContinuidadU2 flow;private final CatalogoLaboratorio cat;
 public ContinuidadU2Controller(NucleoInstitucional core,CatalogoLaboratorio cat){flow=core.continuidad();this.cat=cat;}
 private CatalogoLaboratorio.Operador a(Authentication a){return SesionFuncionario.actual(a,cat);}
 @GetMapping Map<String,Object> bandeja(Authentication a){return flow.bandeja(a(a));}
 @GetMapping("/requerimientos/{id}") Map<String,Object> req(Authentication a,@PathVariable UUID id){return flow.requerimiento(a(a),id);}
 @PostMapping("/requerimientos/{id}/transferir") Resultado transferir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Trasladar r){return flow.transferir(a(a),key,id,r);}
 @PostMapping("/transferencias/{id}/decidir") Resultado decidirTransferencia(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Resolver r){return flow.decidirTransferencia(a(a),key,id,r);}
 @GetMapping("/casos/{id}") Map<String,Object> caso(Authentication a,@PathVariable UUID id){return flow.caso(a(a),id,true);}
 @PostMapping("/casos/{id}/derivar") Resultado derivar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Derivar r){return flow.derivar(a(a),key,id,r);}
 @PostMapping("/derivaciones/{id}/decidir") Resultado decidirDerivacion(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Resolver r){return flow.decidirDerivacion(a(a),key,id,r);}
 @PostMapping("/casos/{id}/termino-por-derivacion") Resultado termino(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision r){return flow.solicitarCierreDerivacion(a(a),key,id,r);}
 @PostMapping("/casos/{id}/reaperturas") Resultado reabrir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Reabrir r){return flow.solicitarReapertura(a(a),key,id,r);}
 @PostMapping("/reaperturas/{id}/decidir") Resultado decidirReapertura(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody ContinuidadU2.DecidirReapertura r){return flow.decidirReapertura(a(a),key,id,r);}
 @PostMapping("/reaperturas/{id}/reactivacion-req") Resultado reactivar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Reactivar r){return flow.autorizarReactivacion(a(a),key,id,r);}
 @PostMapping("/casos/{id}/acuse") Resultado acusar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Acusar r){return flow.acusar(a(a),key,id,r);}
}
