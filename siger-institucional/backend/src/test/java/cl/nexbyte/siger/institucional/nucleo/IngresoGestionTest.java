package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class IngresoGestionTest {
    @TempDir Path temp;DatosAvisosTest f;NucleoU2 flow;
    @BeforeEach void setup()throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");f=new DatosAvisosTest(temp,mysql==null?null:mysql+"_u2?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");flow=f.core.u2();}
    @AfterEach void close(){if(f!=null)f.close();}
    UUID key(){return UUID.randomUUID();}
    NucleoU2.PrimeraGestion body(String tipo){return new NucleoU2.PrimeraGestion("1.0",new Manual("Consulta telefónica sintética","TELÉFONO",f.tiempo.instant().minusSeconds(3600),"DEMO_SEDE_A",null),new NuevoDestino("UPR","DEMO_SEDE_A",f.head),"Respuesta sustantiva efectivamente entregada",tipo,f.tiempo.instant().minusSeconds(1800));}
    long count(String table){return f.d.db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
    void denied(int status,Runnable op){assertEquals(status,assertThrows(FalloTecnico.class,op::run).status);}

    @ParameterizedTest @ValueSource(strings={"ORIENTACION","RESPUESTA_DIRECTA","REMISION_CONCLUIDA"})
    void primeraAtencionConResultadoYFechaConservaComReqYNoCreaCaso(String tipo){
        var input=body(tipo);var result=flow.primeraGestion(f.a(f.prof),key(),input);var q=flow.requerimiento(f.a(f.prof),result.id());
        assertEquals("TERMINADO",q.get("state"));assertEquals(1,count("communication_registry"));assertEquals(1,count("core_requirement"));assertEquals(0,count("core_case"));assertEquals(0,count("institution_sol"));
        var first=(NucleoU2.ResultadoPrimeraGestion)q.get("primeraGestion");assertEquals(tipo,first.tipoResultado());assertEquals(input.realizadaEn(),first.realizadaEn());assertEquals(input.comunicacion().recibidaEnDeclarada(),first.recibidaEn());
        var bit=f.core.bitacora().requerimiento(f.a(f.prof),result.id(),0,25);assertFalse(bit.puedeRegistrar());assertEquals(1,bit.total());var act=bit.entradas().getFirst();
        assertEquals(input.realizadaEn(),act.realizadaEn());assertEquals(f.tiempo.instant(),act.registradaEn());assertEquals(f.prof,act.ejecutor());assertEquals(input.respuesta(),act.descripcion());
        UUID com=UUID.fromString(q.get("communication_id").toString());assertEquals(input.comunicacion().recibidaEnDeclarada(),f.core.detalle(f.a(f.prof),com).contenido().recibidaEnDeclarada());
    }
    @Test void fechasInvalidasOTipoDeRecepcionNoFabricanRespuestaNiRegistro(){
        var b=body("ORIENTACION");
        denied(400,()->flow.primeraGestion(f.a(f.prof),key(),new NucleoU2.PrimeraGestion("1.0",b.comunicacion(),b.ambito(),b.respuesta(),"ACUSE_RECIBO",b.realizadaEn())));
        denied(400,()->flow.primeraGestion(f.a(f.prof),key(),new NucleoU2.PrimeraGestion("1.0",b.comunicacion(),b.ambito(),b.respuesta(),b.tipoResultado(),f.tiempo.instant().plusSeconds(1))));
        denied(400,()->flow.primeraGestion(f.a(f.prof),key(),new NucleoU2.PrimeraGestion("1.0",b.comunicacion(),b.ambito(),b.respuesta(),b.tipoResultado(),b.comunicacion().recibidaEnDeclarada().minusSeconds(1))));
        denied(400,()->flow.primeraGestion(f.a(f.prof),key(),new NucleoU2.PrimeraGestion("2.0",b.comunicacion(),b.ambito(),b.respuesta(),b.tipoResultado(),b.realizadaEn())));
        denied(400,()->flow.primeraGestion(f.a(f.prof),key(),new NucleoU2.PrimeraGestion("1.0",b.comunicacion(),b.ambito(),"",b.tipoResultado(),b.realizadaEn())));
        assertEquals(0,count("communication_registry"));assertEquals(0,count("core_requirement"));
    }
    @Test void tiYOtraUnidadNoObtienenUnaFacultadDeResolverPorRegistrar(){
        denied(403,()->flow.primeraGestion(f.a(f.ti),key(),body("RESPUESTA_DIRECTA")));
        denied(403,()->flow.primeraGestion(f.a(f.otro),key(),body("RESPUESTA_DIRECTA")));
        assertEquals(0,count("communication_registry"));assertEquals(0,count("core_requirement"));
    }
    @Test void reintentoConcurrenteConservaOriginalFolioRespuestaYUnicaGestion()throws Exception{
        UUID key=key();var input=body("ORIENTACION");var go=new CountDownLatch(1);Resultado result;
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Resultado> op=()->{go.await();return flow.primeraGestion(f.a(f.prof),key,input);};var a=pool.submit(op);var b=pool.submit(op);go.countDown();result=a.get(8,TimeUnit.SECONDS);assertEquals(result,b.get(8,TimeUnit.SECONDS));
        }
        var before=f.d.db.sql.queryForMap("SELECT * FROM communication_registry");assertEquals(result,flow.primeraGestion(f.a(f.prof),key,input));assertEquals(before,f.d.db.sql.queryForMap("SELECT * FROM communication_registry"));
        assertEquals(1,count("core_requirement"));assertEquals(1,count("institution_req_action"));assertEquals(0,count("core_case"));
        denied(409,()->flow.primeraGestion(f.a(f.prof),key,body("REMISION_CONCLUIDA")));
    }
    @Test void falloDePersistenciaRevierteComFolioRegistradoReqRespuestaYComandos(){
        f.d.ddl(s->s.execute("ALTER TABLE institution_req_action ADD CONSTRAINT ingreso_rollback CHECK(effective_ms=0)"));
        try{var failure=assertThrows(org.springframework.dao.DataAccessException.class,()->flow.primeraGestion(f.a(f.prof),key(),body("ORIENTACION")));var sql=assertInstanceOf(java.sql.SQLException.class,org.springframework.core.NestedExceptionUtils.getMostSpecificCause(failure));assertTrue(sql.getMessage().toLowerCase(Locale.ROOT).contains("ingreso_rollback"));}
        finally{f.d.ddl(s->s.execute("ALTER TABLE institution_req_action DROP "+(System.getProperty("siger.test.autoridad.mysql")==null?"CONSTRAINT":"CHECK")+" ingreso_rollback"));}
        for(String t:List.of("communication_registry","core_manual","core_requirement","institution_req","institution_flow_history","institution_req_action","core_command"))assertEquals(0,count(t),t);
    }
    @Test void registroTardioConservaFechaRealSinDisfrazarElTiempoDeGestion(){
        var b=body("RESPUESTA_DIRECTA");var input=new NucleoU2.PrimeraGestion("1.0",new Manual(b.comunicacion().texto(),"TELÉFONO",f.tiempo.instant().minusSeconds(80*3600),"DEMO_SEDE_A",null),b.ambito(),b.respuesta(),b.tipoResultado(),f.tiempo.instant().minusSeconds(72*3600));
        var q=flow.primeraGestion(f.a(f.prof),key(),input);var act=f.core.bitacora().requerimiento(f.a(f.prof),q.id(),0,25).entradas().getFirst();assertEquals(input.realizadaEn(),act.realizadaEn());assertEquals(f.tiempo.instant(),act.registradaEn());
    }
}
