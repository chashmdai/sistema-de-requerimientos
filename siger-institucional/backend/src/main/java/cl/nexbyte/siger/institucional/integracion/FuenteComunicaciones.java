package cl.nexbyte.siger.institucional.integracion;

import java.util.List;
import java.util.UUID;

/** Puerto saliente: SIGER inicia todas las conexiones. */
public interface FuenteComunicaciones {
    List<SobreComunicacion> recuperar();
    void confirmar(UUID id, SobreComunicacion.Confirmacion confirmacion);
}
