package cl.nexbyte.siger.institucional.nucleo;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo/comunicaciones/{id}/rectificaciones")
@ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public final class RectificacionesIngresoController {
    private final NucleoInstitucional core;private final CatalogoLaboratorio cat;
    public RectificacionesIngresoController(NucleoInstitucional core,CatalogoLaboratorio cat){this.core=core;this.cat=cat;}
    private CatalogoLaboratorio.Operador actor(Authentication auth){return SesionFuncionario.actual(auth,cat);}
    @GetMapping public RectificacionesIngreso.Vista consultar(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="25")int tamanio){return core.rectificacionesIngreso().consultar(actor(auth),id,pagina,tamanio);}
    @GetMapping("/{secuencia}") public RectificacionesIngreso.Constancia constancia(Authentication auth,@PathVariable UUID id,@PathVariable long secuencia){return core.rectificacionesIngreso().constancia(actor(auth),id,secuencia);}
    @PostMapping public ContratosNucleo.Resultado registrar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RectificacionesIngreso.Solicitud r){return core.rectificacionesIngreso().registrar(actor(auth),key,id,r);}
}
