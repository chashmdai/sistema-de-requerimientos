package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.nucleo.ContenidoInstitucional.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public final class ContenidoController {
 private final NucleoInstitucional core;private final CatalogoLaboratorio cat;
 public ContenidoController(NucleoInstitucional core,CatalogoLaboratorio cat){this.core=core;this.cat=cat;}
 private Operador actor(Authentication auth){return SesionFuncionario.actual(auth,cat);}
 @GetMapping("/requerimientos/{req}/pertinencia") public Map<String,Object> req(Authentication a,@PathVariable UUID req){return core.contenidos().contextoReq(actor(a),req);}
 @GetMapping("/requerimientos/{req}/pertinencia/{com}") public Map<String,Object> propuesta(Authentication a,@PathVariable UUID req,@PathVariable UUID com){return core.contenidos().propuesta(actor(a),req,com);}
 @PostMapping("/requerimientos/{req}/pertinencia/{com}") public ContratosNucleo.Resultado decidir(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID req,@PathVariable UUID com,@RequestBody Pertinencia body){return core.contenidos().determinar(actor(a),key,req,com,body);}
 @GetMapping("/casos/{caso}/antecedentes-pertinentes") public List<Map<String,Object>> antecedentes(Authentication a,@PathVariable UUID caso){return core.contenidos().antecedentes(actor(a),caso);}
 @PostMapping("/casos/{caso}/antecedentes-pertinentes/{com}/valoraciones") public ContratosNucleo.Resultado valorar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID caso,@PathVariable UUID com,@RequestBody Valoracion body){return core.contenidos().valorar(actor(a),key,caso,com,body);}
 @GetMapping("/casos/{caso}/proteccion-documental/{doc}") public Map<String,Object> control(Authentication a,@PathVariable UUID caso,@PathVariable UUID doc){return core.contenidos().control(actor(a),caso,doc);}
 @PostMapping("/casos/{caso}/proteccion-documental/{doc}") public ContratosNucleo.Resultado proteger(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID caso,@PathVariable UUID doc,@RequestBody Proteccion body){return core.contenidos().proteger(actor(a),key,caso,doc,body);}
 @PostMapping("/casos/{caso}/proteccion-documental/{doc}/consulta/{destino}") public ContratosNucleo.Resultado publicar(Authentication a,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID caso,@PathVariable UUID doc,@PathVariable UUID destino,@RequestBody Publicacion body){return core.contenidos().publicar(actor(a),key,caso,doc,destino,body);}
}

