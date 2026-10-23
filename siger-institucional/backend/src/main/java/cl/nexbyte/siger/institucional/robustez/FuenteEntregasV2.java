package cl.nexbyte.siger.institucional.robustez;
import cl.nexbyte.siger.institucional.integracion.SobreComunicacion;
import java.util.*;
public interface FuenteEntregasV2 {
    record Reserva(UUID reserva,long venceEnEpochMs,int intento,EntregaVerificada entrega){}
    List<Reserva> reclamar();
    void confirmar(Reserva reserva,RegistroInstitucional recepcion);
    void fallo(Reserva reserva,String codigo);
}
