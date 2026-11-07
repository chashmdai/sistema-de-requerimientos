package cl.nexbyte.siger.institucional.nucleo;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;

@RestController @RequestMapping("/nucleo")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public final class BitacoraGestionController {
    private final NucleoInstitucional core;
    private final CatalogoLaboratorio cat;
    public BitacoraGestionController(NucleoInstitucional core,CatalogoLaboratorio cat){this.core=core;this.cat=cat;}
    private Operador actor(Authentication auth){return SesionFuncionario.actual(auth,cat);}
    @GetMapping("/casos/{id}/bitacora")
    public BitacoraGestion.Vista caso(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="25")int tamanio){return core.bitacora().caso(actor(auth),id,pagina,tamanio);}
    @GetMapping("/casos/{id}/bitacora/constancias/{accion}")
    public BitacoraGestion.Entrada constanciaCaso(Authentication auth,@PathVariable UUID id,@PathVariable String accion){return core.bitacora().constanciaCaso(actor(auth),id,accion);}
    @PostMapping("/casos/{id}/bitacora")
    public ContratosNucleo.Resultado registrarCaso(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody BitacoraGestion.Registrar body){return core.bitacora().registrarCaso(actor(auth),key,id,body);}
    @GetMapping("/institucional/requerimientos/{id}/bitacora")
    public BitacoraGestion.Vista req(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="25")int tamanio){return core.bitacora().requerimiento(actor(auth),id,pagina,tamanio);}
    @GetMapping("/institucional/requerimientos/{id}/bitacora/constancias/{accion}")
    public BitacoraGestion.Entrada constanciaReq(Authentication auth,@PathVariable UUID id,@PathVariable String accion){return core.bitacora().constanciaReq(actor(auth),id,accion);}
    @PostMapping("/institucional/requerimientos/{id}/bitacora")
    public ContratosNucleo.Resultado registrarReq(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody BitacoraGestion.Registrar body){return core.bitacora().registrarRequerimiento(actor(auth),key,id,body);}
}
