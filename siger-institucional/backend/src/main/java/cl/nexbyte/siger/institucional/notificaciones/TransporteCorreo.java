package cl.nexbyte.siger.institucional.notificaciones;
import java.time.*;
public interface TransporteCorreo {
 record Mensaje(String destinatario,String asunto,String texto,String correlacion){@Override public String toString(){return "Mensaje[reservado]";}}
 record Resultado(String estado,String fase,Integer http,long reintentarEnSegundos,String requestId){}
 Resultado enviar(Mensaje mensaje);
 boolean real();
}
