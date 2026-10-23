package cl.nexbyte.siger.institucional.integracion;

/** Debe confirmar COMMIT definitivo antes de retornar. Implementación productiva pendiente. */
public interface RecepcionInstitucional {
    SobreComunicacion.Confirmacion guardarSiAusente(SobreComunicacion sobre);
}
