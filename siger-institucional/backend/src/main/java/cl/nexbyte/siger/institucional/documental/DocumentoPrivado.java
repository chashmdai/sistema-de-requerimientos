package cl.nexbyte.siger.institucional.documental;
import cl.nexbyte.siger.institucional.formulario.EntregaCompleta;
import java.time.Instant;import java.util.UUID;
/** Referencia interna; nunca se serializa al navegador. */
public record DocumentoPrivado(UUID id,UUID comunicacion,String origen,String clave,EntregaCompleta.Adjunto original,String autor,Instant fecha){
    @Override public String toString(){return "DocumentoPrivado[protegido]";}
}