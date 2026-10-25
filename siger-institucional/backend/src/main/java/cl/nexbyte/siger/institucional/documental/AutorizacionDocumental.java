package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.Caso;
import java.util.UUID;
/** Política sustituible por recurso; no combina roles ni otorga permisos adicionales. */
public interface AutorizacionDocumental {
    void ingreso(Operador actor, UUID comunicacion, UUID contextoCaso);
    Caso leerCaso(Operador actor, UUID caso);
    Caso gestionarCaso(Operador actor, UUID caso);
}
