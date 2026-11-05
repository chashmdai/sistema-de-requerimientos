package cl.nexbyte.siger.institucional.nucleo;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController @RequestMapping("/nucleo") @ConditionalOnProperty(name="institucional.nucleo.habilitado",havingValue="true")
public class NucleoController {
    private final NucleoInstitucional core;private final CatalogoLaboratorio catalogo;private final org.springframework.core.env.Environment env;
    public NucleoController(NucleoInstitucional core,CatalogoLaboratorio catalogo,org.springframework.core.env.Environment env){this.core=core;this.catalogo=catalogo;this.env=env;}
    private CatalogoLaboratorio.Operador actor(Authentication auth){return SesionFuncionario.actual(auth,catalogo);}
    public record Sesion(boolean autenticado,CatalogoLaboratorio.Operador operador,String modo,boolean notificacionesHabilitadas,boolean solicitudesHabilitadas,boolean cambioClaveObligatorio){}
    public record Csrf(String token,String headerName,String parameterName){}
    public record Catalogo(List<CatalogoLaboratorio.Unidad> unidades,List<CatalogoLaboratorio.Sede> sedes,List<CatalogoLaboratorio.Presencia> presencias,List<CatalogoLaboratorio.Operador> operadores,List<CatalogoLaboratorio.Region> regiones,List<cl.nexbyte.siger.institucional.identidad.CatalogoAreas.Area> areas,List<CatalogoLaboratorio.Presencia> ambitosConAreas,List<cl.nexbyte.siger.institucional.identidad.CatalogoMaterias.Materia> materias,List<cl.nexbyte.siger.institucional.identidad.CatalogoMaterias.SedeRegional> sedesRegionales){}
    @GetMapping("/session") Sesion session(Authentication auth){
        boolean valid=auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails;
        boolean cambio=valid&&auth.getPrincipal() instanceof cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.Principal p&&p.cambioClavePendiente();
        return new Sesion(valid,valid&&!cambio?actor(auth):null,catalogo.individual()?"LABORATORIO_INDIVIDUAL":"PRUEBAS_HISTORICAS",!cambio&&env.getProperty("institucional.notificaciones.habilitadas",Boolean.class,false),!cambio&&env.getProperty("institucional.solicitudes.habilitadas",Boolean.class,false),cambio);
    }
    @GetMapping("/csrf") Csrf csrf(CsrfToken token){return new Csrf(token.getToken(),token.getHeaderName(),token.getParameterName());}
    @GetMapping("/catalogo") Catalogo catalogo(Authentication auth){
        var a=actor(auth);return new Catalogo(catalogo.unidades(),catalogo.sedes(),catalogo.presencias(),
            catalogo.operadores(),catalogo.regiones(),core.areas(a),core.ambitosConAreas(a),core.materias(a),core.sedesRegionales(a));
    }
    @GetMapping("/comunicaciones") List<Resumen> buzon(Authentication auth,@RequestParam(defaultValue="0")long despues,@RequestParam(defaultValue="100")int limite){return core.buzon(actor(auth),despues,limite);}
    @GetMapping("/comunicaciones/{id}") Detalle detalle(Authentication auth,@PathVariable UUID id){return core.detalle(actor(auth),id);}
    @PostMapping("/comunicaciones/manual") Resultado manual(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@RequestBody Manual body){if(body.respuestaYaEntregada()!=null)return core.soloReintento(actor(auth),key,()->core.registrarManual(actor(auth),key,body));return core.registrarManual(actor(auth),key,body);}
    @PostMapping("/comunicaciones/{id}/distribuir") Resultado distribuir(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Distribucion body){return core.soloReintento(actor(auth),key,()->core.distribuir(actor(auth),key,id,body));}
    @PostMapping("/comunicaciones/{id}/responder") Resultado responder(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.soloReintento(actor(auth),key,()->core.decidirComunicacion(actor(auth),key,id,body,false));}
    @PostMapping("/comunicaciones/{id}/descartar") Resultado descartar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.soloReintento(actor(auth),key,()->core.decidirComunicacion(actor(auth),key,id,body,true));}
    @PostMapping("/comunicaciones/{id}/vincular") Resultado vincular(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Vinculacion body){return core.vincular(actor(auth),key,id,body);}
    @PostMapping("/destinos/{id}/abrir-caso") Resultado abrir(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Asignacion body){return core.soloReintento(actor(auth),key,()->core.abrirCaso(actor(auth),key,id,body));}
    @PostMapping("/destinos/{id}/responder") Resultado resolver(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.soloReintento(actor(auth),key,()->core.resolverDestino(actor(auth),key,id,body,true));}
    @PostMapping("/destinos/{id}/finalizar-evaluacion") Resultado finalizar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.soloReintento(actor(auth),key,()->core.resolverDestino(actor(auth),key,id,body,false));}
    @GetMapping("/casos/{id}") Caso caso(Authentication auth,@PathVariable UUID id){return core.caso(actor(auth),id);}
    @GetMapping("/casos/{id}/materia") Map<String,Object> materia(Authentication auth,@PathVariable UUID id){return core.contextoMateria(actor(auth),id);}
    @PostMapping("/casos/{id}/materia") Resultado materia(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody RectificarMateria body){return core.rectificarMateria(actor(auth),key,id,body);}
    @GetMapping("/areas") List<cl.nexbyte.siger.institucional.identidad.CatalogoAreas.Area> areas(Authentication auth){return core.areas(actor(auth));}
    @GetMapping("/casos/{id}/areas/historial") List<Map<String,Object>> historialArea(Authentication auth,@PathVariable UUID id){return core.historialArea(actor(auth),id);}
    @GetMapping("/casos/{id}/areas/disponibles") List<String> cambiosArea(Authentication auth,@PathVariable UUID id){core.autorizarCaso(actor(auth),id);return core.cambiosAreaDisponibles(actor(auth),id);}
    @PostMapping("/casos/{id}/area") Resultado area(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody CambiarArea body){return core.cambiarArea(actor(auth),key,id,body);}
    @PostMapping("/casos/{id}/asignar") Resultado asignar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Asignacion body){return core.asignar(actor(auth),key,id,body);}
    @PostMapping("/casos/{id}/actuaciones") Resultado actuar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.actuar(actor(auth),key,id,body);}
    @PostMapping("/casos/{id}/solicitar-termino") Resultado solicitar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.revisarCaso(actor(auth),key,id,body,"SOLICITUD_TERMINO");}
    @PostMapping("/casos/{id}/observar") Resultado observar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.revisarCaso(actor(auth),key,id,body,"DEVOLUCION");}
    @PostMapping("/casos/{id}/cerrar") Resultado cerrar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Decision body){return core.revisarCaso(actor(auth),key,id,body,"CIERRE");}
    @PostMapping("/casos/{id}/reabrir") void pendiente(Authentication auth,@PathVariable UUID id){core.transicionPendiente(actor(auth),id);}
    @GetMapping("/avisos") List<Aviso> avisos(Authentication auth){return core.avisos(actor(auth));}
    @PostMapping("/casos/{id}/clasificar") Resultado clasificar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody Clasificacion body){return core.clasificar(actor(auth),key,id,body);}
    @PostMapping("/casos/{id}/derivar") Resultado solicitarDerivacion(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody SolicitarDerivacion body){return core.solicitarDerivacion(actor(auth),key,id,body);}
    @GetMapping("/derivaciones/entrantes") List<Derivacion> entrantes(Authentication auth){return core.entrantes(actor(auth));}
    @GetMapping("/derivaciones/entrantes/pagina") Pagina<Derivacion> entrantesPagina(Authentication auth,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){return core.entrantesPagina(actor(auth),pagina,limite);}
    @GetMapping("/derivaciones/{id}") Derivacion derivacion(Authentication auth,@PathVariable UUID id){return core.derivacion(actor(auth),id);}
    @GetMapping("/casos/{id}/derivaciones") List<Derivacion> salientes(Authentication auth,@PathVariable UUID id){return core.salientes(actor(auth),id);}
    @PostMapping("/derivaciones/{id}/aceptar") Resultado aceptar(Authentication auth,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@RequestBody AceptarDerivacion body){return core.aceptarDerivacion(actor(auth),key,id,body);}
    @PostMapping({"/derivaciones/{id}/rechazar","/derivaciones/{id}/cancelar","/derivaciones/{id}/escalar"}) void rechazoPendiente(Authentication auth,@PathVariable UUID id){core.decisionDerivacionPendiente(actor(auth),id);}
    @PostMapping("/avisos/{id}/leer") Resultado leer(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key){return core.leerAviso(actor(auth),key,id);}
    @GetMapping("/trabajo") Bandeja trabajo(Authentication auth){return core.bandeja(actor(auth));}
    @GetMapping("/comunicaciones/buscar") Pagina<Resumen> buscar(Authentication auth,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="false")boolean requerimientos,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){return core.buscar(actor(auth),q,requerimientos,pagina,limite);}
    @GetMapping("/casos") Pagina<CasoResumen> casos(Authentication auth,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String estado,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){return core.casos(actor(auth),estado,q,pagina,limite);}
    @GetMapping("/antecedentes") Pagina<Antecedente> antecedentes(Authentication auth,@RequestParam(required=false)UUID caso,@RequestParam(defaultValue="false")boolean pendientes,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){return core.antecedentes(actor(auth),caso,pendientes,pagina,limite);}
    @GetMapping("/antecedentes/{id}") Antecedente antecedente(Authentication auth,@PathVariable UUID id){return core.antecedente(actor(auth),id);}
    @PostMapping("/antecedentes/{id}/evaluar") Resultado evaluar(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody EvaluarAntecedente body){return core.evaluarAntecedente(actor(auth),key,id,body);}
    @GetMapping("/reaperturas") Pagina<Reapertura> reaperturas(Authentication auth,@RequestParam(required=false)UUID caso,@RequestParam(defaultValue="false")boolean pendientes,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int limite){return core.reaperturas(actor(auth),caso,pendientes,pagina,limite);}
    @GetMapping("/reaperturas/{id}") Reapertura reapertura(Authentication auth,@PathVariable UUID id){return core.reapertura(actor(auth),id);}
    @PostMapping("/casos/{id}/solicitar-reapertura") Resultado solicitarReapertura(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody SolicitarReapertura body){return core.solicitarReapertura(actor(auth),key,id,body);}
    @PostMapping("/reaperturas/{id}/aprobar") Resultado aprobarReapertura(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DecidirReapertura body){return core.decidirReapertura(actor(auth),key,id,body,true);}
    @PostMapping("/reaperturas/{id}/rechazar") Resultado rechazarReapertura(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DecidirReapertura body){return core.decidirReapertura(actor(auth),key,id,body,false);}
    @GetMapping("/casos/{id}/periodos") List<Periodo> periodos(Authentication auth,@PathVariable UUID id){return core.periodos(actor(auth),id);}
}

