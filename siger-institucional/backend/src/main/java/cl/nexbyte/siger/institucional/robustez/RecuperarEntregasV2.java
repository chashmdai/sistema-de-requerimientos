package cl.nexbyte.siger.institucional.robustez;

import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;

/** Consumidor acotado. Red fuera de transacciones y confirmación solo después del registro durable. */
public final class RecuperarEntregasV2 implements AutoCloseable {
    private final FuenteEntregasV2 source;private final RecepcionInstitucionalJdbc store;
    private final AtomicBoolean active=new AtomicBoolean();private final ExecutorService workers;private final int parallelism;
    private final LongAdder processed=new LongAdder(),storedNanos=new LongAdder(),sentNanos=new LongAdder(),localAckNanos=new LongAdder();
    public record Resultado(int recuperadas,int confirmadas,int fallidas,boolean omitidaPorSolapamiento){}
    public RecuperarEntregasV2(FuenteEntregasV2 source,RecepcionInstitucionalJdbc store){this(source,store,1);}
    public RecuperarEntregasV2(FuenteEntregasV2 source,RecepcionInstitucionalJdbc store,int parallelism){
        if(parallelism<1||parallelism>8)throw new IllegalArgumentException("Paralelismo fuera de límites");
        this.source=source;this.store=store;this.parallelism=parallelism;
        workers=parallelism==1?null:Executors.newFixedThreadPool(parallelism,Thread.ofPlatform().name("registro-com-",0).daemon(true).factory());
    }
    public Resultado ejecutar(){
        if(!active.compareAndSet(false,true))return new Resultado(0,0,0,true);
        try{
            var batch=source.reclamar();if(batch==null||batch.size()>20)throw new FalloTecnico(502);int confirmed=0;
            if(workers==null){for(var item:batch)if(procesar(item))confirmed++;}
            else{
                var jobs=new ArrayList<Callable<Boolean>>();for(var item:batch)jobs.add(()->procesar(item));
                try{for(var job:workers.invokeAll(jobs))try{if(job.get())confirmed++;}catch(ExecutionException failure){/* Reserva queda recuperable. */}}
                catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            }
            return new Resultado(batch.size(),confirmed,batch.size()-confirmed,false);
        }finally{active.set(false);}
    }
    private boolean procesar(FuenteEntregasV2.Reserva item){
        String code;
        try{
            long start=System.nanoTime();
            var receipt=store.guardar(item.entrega());store.prepararConfirmacion(item.entrega().comunicacion().id(),item.reserva());
            storedNanos.add(System.nanoTime()-start);processed.increment();
            try{
                start=System.nanoTime();source.confirmar(item,receipt);sentNanos.add(System.nanoTime()-start);
                start=System.nanoTime();store.confirmacionEntregada(item.entrega().comunicacion().id());localAckNanos.add(System.nanoTime()-start);return true;
            }catch(RuntimeException error){code="CONFIRMACION_AMBIGUA";}
        }catch(IllegalArgumentException|tools.jackson.core.JacksonException invalid){code="CONTRATO_INVALIDO";}
        catch(FalloTecnico error){code=error.status==409?"CONTRATO_INVALIDO":"RECEPTOR_NO_DISPONIBLE";}
        catch(RuntimeException error){code="RECEPTOR_NO_DISPONIBLE";}
        try{store.incidente(item.entrega().comunicacion().id(),code);}catch(RuntimeException ignored){}
        try{source.fallo(item,code);}catch(RuntimeException ignored){}
        return false;
    }
    public int conciliarConfirmaciones(){
        if(!active.compareAndSet(false,true))return 0;
        try{
            int confirmed=0;
            for(var item:store.confirmacionesPendientes()){
                try{source.confirmar(item.reserva(),item.registro());store.confirmacionEntregada(item.registro().registro().operacionPublicaId());confirmed++;}
                catch(FalloTecnico unavailable){store.aplazarConfirmacion(item.registro().registro().operacionPublicaId(),item.reserva().reserva(),unavailable.status);}
                catch(RuntimeException unavailable){store.aplazarConfirmacion(item.registro().registro().operacionPublicaId(),item.reserva().reserva(),503);}
            }
            return confirmed;
        }finally{active.set(false);}
    }
    public Map<String,Object> metricas(){
        long n=processed.sum();return Map.of("paralelismo",parallelism,"registrosProcesados",n,"registroYPreparacionAckPromedioMs",n==0?0:storedNanos.sum()/1e6/n,
            "confirmacionPublicaPromedioMs",n==0?0:sentNanos.sum()/1e6/n,"cierreAckPromedioMs",n==0?0:localAckNanos.sum()/1e6/n);
    }
    public void close(){if(workers!=null)for(var queued:workers.shutdownNow())if(queued instanceof Future<?> task)task.cancel(true);}
}
