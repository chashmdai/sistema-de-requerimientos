package cl.nexbyte.siger.institucional.integracion;

/** Al menos una vez: persistir -> confirmar. Una confirmación perdida causa reentrega, nunca otro registro. */
public final class RecuperarComunicaciones {
    private final FuenteComunicaciones fuente;
    private final RecepcionInstitucional recepcion;
    public record Resultado(int recuperadas, int confirmadas, int fallidas) {}
    public RecuperarComunicaciones(FuenteComunicaciones fuente, RecepcionInstitucional recepcion) { this.fuente = fuente; this.recepcion = recepcion; }
    public Resultado ejecutar() {
        var batch = fuente.recuperar(); // Si falla, el planificador reintenta con espera.
        int confirmed = 0;
        for (var sobre : batch) {
            try {
                var receipt = recepcion.guardarSiAusente(sobre);
                fuente.confirmar(sobre.id(), receipt);
                confirmed++;
            } catch (RuntimeException e) {
                // No ACK de registros no guardados. Sin PII, tokens ni cuerpos en logs.
                // Se sigue con el lote: un registro en conflicto no bloquea los otros del mismo lote.
            }
        }
        return new Resultado(batch.size(), confirmed, batch.size() - confirmed);
    }
}
