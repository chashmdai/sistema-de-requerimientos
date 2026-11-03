package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.nucleo.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/nucleo/concesiones")
@ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class ConcesionesController {
    private final ConcesionesInstitucionales concesiones;
    private final AutoridadesInstitucionales autoridades;
    private final CatalogoLaboratorio catalogo;
    public ConcesionesController(ConcesionesInstitucionales concesiones,AutoridadesInstitucionales autoridades,CatalogoLaboratorio catalogo){this.concesiones=concesiones;this.autoridades=autoridades;this.catalogo=catalogo;}
    private String actor(Authentication a){return SesionFuncionario.actual(a,catalogo).id();}
    @GetMapping("/contexto") Map<String,Object> contexto(Authentication a){return autoridades.contexto(actor(a));}
    @GetMapping List<Map<String,Object>> listar(Authentication a){return concesiones.listar(actor(a));}
    @GetMapping("/{id}") Map<String,Object> leer(Authentication a,@PathVariable UUID id){return concesiones.leer(actor(a),id);}
    @GetMapping("/{id}/historial") List<Map<String,Object>> historial(Authentication a,@PathVariable UUID id){return concesiones.historial(actor(a),id);}
    @PostMapping ConcesionesInstitucionales.Resultado solicitar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@RequestBody ConcesionesInstitucionales.Solicitud s){return concesiones.solicitar(actor(a),key,s);}
    @PostMapping("/{id}/aprobar") ConcesionesInstitucionales.Resultado aprobar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody ConcesionesInstitucionales.Decision d){return concesiones.decidir(actor(a),key,id,d,true);}
    @PostMapping("/{id}/rechazar") ConcesionesInstitucionales.Resultado rechazar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody ConcesionesInstitucionales.Decision d){return concesiones.decidir(actor(a),key,id,d,false);}
    @PostMapping("/{id}/ejecutar") ConcesionesInstitucionales.Resultado ejecutar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody ConcesionesInstitucionales.Ejecucion e){return concesiones.ejecutar(actor(a),key,id,e);}
    @PostMapping("/{id}/revocar") ConcesionesInstitucionales.Resultado revocar(Authentication a,@RequestHeader("Idempotency-Key") UUID key,@PathVariable UUID id,@RequestBody ConcesionesInstitucionales.Decision d){return concesiones.revocar(actor(a),key,id,d);}
}
