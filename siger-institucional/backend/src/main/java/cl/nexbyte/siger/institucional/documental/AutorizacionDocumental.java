package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.Caso;
import java.util.UUID;
/** Política sustituible por recurso; no combina roles ni otorga permisos adicionales. */
public interface AutorizacionDocumental {
    void contextoDocumento(Operador actor,UUID caso);
    void contextoIngreso(Operador actor,UUID comunicacion,UUID caso);
    void documento(Operador actor,UUID caso,UUID documento,String accion);
    void revalidarDocumento(Operador actor,UUID caso,UUID documento,String accion);
    void registrarDocumento(Operador actor,UUID caso,UUID documento);
    cl.nexbyte.siger.institucional.nucleo.ContenidoInstitucional.Permisos permisos(Operador actor,UUID caso,UUID documento);
    default void bloquearMutacion(Operador actor){}
    void ingreso(Operador actor, UUID comunicacion, UUID contextoCaso);
    Caso leerCaso(Operador actor, UUID caso);
    Caso gestionarCaso(Operador actor, UUID caso);
}
