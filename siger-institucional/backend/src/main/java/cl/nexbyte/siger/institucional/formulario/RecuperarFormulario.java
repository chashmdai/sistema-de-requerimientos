package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.robustez.*;import java.util.concurrent.atomic.AtomicBoolean;

/** Un lote pequeño; originales fuera de SQL y ACK solamente al volver del COMMIT institucional. */
public final class RecuperarFormulario {
    private final FuenteFormulario source;private final RecepcionInstitucionalJdbc recepcion;private final ArchivoInstitucional files;private final AtomicBoolean active=new AtomicBoolean();
    public RecuperarFormulario(FuenteFormulario source,RecepcionInstitucionalJdbc recepcion,ArchivoInstitucional files){this.source=source;this.recepcion=recepcion;this.files=files;}
    public record Resultado(int recuperadas,int confirmadas,int fallidas,boolean solapada){}
    public Resultado ejecutar(){if(!active.compareAndSet(false,true))return new Resultado(0,0,0,true);try{int done=0;var batch=source.reclamar();for(var item:batch){boolean committed=false;try{
        recepcion.validarCompleta(item.entrega());for(var a:item.entrega().adjuntos())source.descargar(item,a,files);
        var receipt=recepcion.guardarCompleta(item.entrega(),files);committed=true;recepcion.prepararConfirmacion(item.entrega().id(),item.reserva());
        receipt=recepcion.registroCompletoConservado(item.entrega(),files);source.confirmar(item,receipt);recepcion.confirmacionEntregada(item.entrega().id());done++;
    }catch(RuntimeException|java.io.IOException error){
        if(committed){recepcion.aplazarConfirmacion(item.entrega().id(),item.reserva(),error instanceof FalloTecnico f?f.status:503);}
        else{String code=error instanceof IllegalArgumentException?"CONTRATO_INVALIDO":"RECEPTOR_NO_DISPONIBLE";try{source.fallo(item,code);}catch(RuntimeException ignored){}try{recepcion.incidente(item.entrega().id(),code);}catch(RuntimeException ignored){}}
    }}return new Resultado(batch.size(),done,batch.size()-done,false);}finally{active.set(false);}}
    public int conciliar(){if(!active.compareAndSet(false,true))return 0;try{int done=0;for(var pending:recepcion.confirmacionesCompletas()){var r=pending.reserva();try{
        for(var a:r.entrega().adjuntos())if(!files.disponible(r.entrega().id(),a))throw new java.io.IOException();
        source.confirmar(r,pending.registro());recepcion.confirmacionEntregada(r.entrega().id());done++;
    }catch(FalloTecnico expired){if(expired.status==409)recepcion.reservaCompletaVencida(r.entrega().id(),r.reserva());else recepcion.aplazarConfirmacion(r.entrega().id(),r.reserva(),expired.status);}
    catch(RuntimeException|java.io.IOException unavailable){recepcion.aplazarConfirmacion(r.entrega().id(),r.reserva(),503);}}return done;}finally{active.set(false);}}
}
