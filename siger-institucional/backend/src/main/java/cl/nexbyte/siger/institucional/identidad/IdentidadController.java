package cl.nexbyte.siger.institucional.identidad;
import cl.nexbyte.siger.institucional.nucleo.*;import java.util.*;import java.time.*;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/nucleo/ti") @ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class IdentidadController {
 private final IdentidadInstitucional identidad;private final OrganizacionInstitucional org;private final CatalogoLaboratorio catalogo;
 public IdentidadController(IdentidadInstitucional identidad,OrganizacionInstitucional org,CatalogoLaboratorio catalogo){this.identidad=identidad;this.org=org;this.catalogo=catalogo;}
 private String actor(Authentication a){var p=SesionFuncionario.actual(a,catalogo);if(!p.ti())throw new cl.nexbyte.siger.institucional.robustez.FalloTecnico(403);return p.id();}
 @GetMapping("/funcionarios") List<IdentidadInstitucional.Cuenta> cuentas(Authentication a){return identidad.cuentas(actor(a));}
 @PostMapping("/funcionarios") IdentidadInstitucional.Cuenta crear(Authentication a,@RequestBody IdentidadInstitucional.Crear c){return identidad.crear(actor(a),c);}
 @PutMapping("/funcionarios/{id}") IdentidadInstitucional.Cuenta perfil(Authentication a,@PathVariable String id,@RequestBody IdentidadInstitucional.Perfil p){return identidad.actualizar(actor(a),id,p);}
 @PostMapping("/funcionarios/{id}/roles") Map<String,String> grant(Authentication a,@PathVariable String id,@RequestBody IdentidadInstitucional.Otorgar o){return Map.of("id",identidad.otorgar(actor(a),id,o));}
 @DeleteMapping("/funcionarios/{id}/roles/{grant}") void revoke(Authentication a,@PathVariable String id,@PathVariable String grant){identidad.revocar(actor(a),id,grant);}
 @PostMapping("/funcionarios/{id}/clave") void password(Authentication a,@PathVariable String id,@RequestBody IdentidadInstitucional.Clave c){identidad.clave(actor(a),id,c);}
 @GetMapping("/organizacion") Map<String,Object> org(Authentication a){actor(a);return org.catalogo();}
 @PostMapping("/organizacion/coberturas") Map<String,String> cobertura(Authentication a,@RequestBody OrganizacionInstitucional.Cobertura c){return Map.of("id",org.cubrir(actor(a),c));}
 public record Hasta(Instant hasta){}
 @PostMapping("/organizacion/coberturas/{id}/finalizar") void end(Authentication a,@PathVariable String id,@RequestBody Hasta c){org.terminarCobertura(actor(a),id,c.hasta());}
 @PostMapping("/organizacion/sedes") void office(Authentication a,@RequestBody OrganizacionInstitucional.Oficina o){org.oficina(actor(a),o);}
}
