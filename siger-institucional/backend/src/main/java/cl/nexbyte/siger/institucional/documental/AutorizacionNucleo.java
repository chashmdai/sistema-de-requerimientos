package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.nucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.Caso;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.util.UUID;
/** Adaptador del alcance provisional vigente. Equipos/subrogancias se resolverán en la política futura. */
public final class AutorizacionNucleo implements AutorizacionDocumental {
    private final NucleoInstitucional core;
    public AutorizacionNucleo(NucleoInstitucional core) { this.core=core; }
    public void ingreso(Operador a, UUID com, UUID caso) {
        if(caso==null || a.distribuidor()) { core.autorizarLectura(a,com); return; }
        var context=core.caso(a,caso);
        if(context.comunicacionId().equals(com)) { core.autorizarLectura(a,com); return; }
        var evidence=core.antecedente(a,com);
        if(!caso.equals(evidence.casoId()) || evidence.contenido()==null) throw new FalloTecnico(404);
    }
    public Caso leerCaso(Operador a, UUID caso) { return core.caso(a,caso); }
    public Caso gestionarCaso(Operador a, UUID caso) { return core.autorizarGestionDocumental(a,caso); }
}
