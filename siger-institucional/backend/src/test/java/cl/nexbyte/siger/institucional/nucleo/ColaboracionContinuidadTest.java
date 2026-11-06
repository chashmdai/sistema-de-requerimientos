package cl.nexbyte.siger.institucional.nucleo;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static org.junit.jupiter.api.Assertions.*;

class ColaboracionContinuidadTest {
    @TempDir Path temp;
    DatosAvisosTest f;TerminoColaboracion flow;UUID req,col,cas;
    UUID key(){return UUID.randomUUID();}
    long rev(String table,UUID id){return f.d.db.sql.queryForObject("SELECT revision FROM "+table+" WHERE id=?",Long.class,id.toString());}
    void denied(int status,Runnable work){assertEquals(status,assertThrows(FalloTecnico.class,work::run).status);}
    @BeforeEach void setup()throws Exception{
        String mysql=System.getProperty("siger.test.autoridad.mysql");
        f=new DatosAvisosTest(temp,mysql==null?null:mysql+"_u2?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");
        var u=f.core.u2();
        var sol=u.distribuir(f.a(f.dist),key(),f.d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.head),"Ingreso sintético",null));
        req=u.asumir(f.a(f.head),key(),sol.id(),new NucleoU2.Asumir(0,f.prof,"Asigna atención",null)).id();
        col=u.colaborar(f.a(f.head),key(),req,new NucleoU2.Colaborar(0,new NuevoDestino("UPD","DEMO_SEDE_A",f.headOtra),"Objetivo independiente de colaboración")).id();
        u.resolver(f.a(f.headOtra),key(),col,new NucleoU2.ResolverColaboracion(0,"ACEPTADA",f.otro,"Acepta intervención"));
        cas=UUID.fromString(f.d.db.sql.queryForObject("SELECT case_id FROM institution_collaboration WHERE id=?",String.class,col.toString()));
        flow=f.core.terminoColaboracion();
    }
    @AfterEach void close(){if(f!=null)f.close();}
    UUID solicitar(){return flow.solicitar(f.a(f.head),key(),col,new TerminoColaboracion.Solicitar(rev("core_requirement",req),1,"Se propone finalizar intervención; destino decide")).id();}
    TerminoColaboracion.Decidir decidir(String d){return new TerminoColaboracion.Decidir(0,d,"Decisión fundada de la función competente");}

    @Test void origenSolicitaDestinoDecideSinCerrarCasoNiRevocarEquipoOTareas(){
        var task=f.tareas.crear(f.a(f.otro),key(),f.edit(cas,null,null));
        var before=f.d.db.sql.queryForMap("SELECT * FROM core_case WHERE id=?",cas.toString());
        var agenda=f.d.db.sql.queryForMap("SELECT * FROM institution_task WHERE id=?",task.id().toString());
        var p=solicitar();
        denied(404,()->flow.decidir(f.a(f.head),key(),p,decidir("ACEPTADA")));
        denied(404,()->f.core.caso(f.a(f.head),cas));
        assertEquals(1,flow.bandeja(f.a(f.headOtra)).size());
        assertEquals(0,flow.bandeja(f.a(f.ti)).size());
        var k=key();assertEquals(flow.decidir(f.a(f.headOtra),k,p,decidir("ACEPTADA")),flow.decidir(f.a(f.headOtra),k,p,decidir("ACEPTADA")));
        assertEquals(before,f.d.db.sql.queryForMap("SELECT * FROM core_case WHERE id=?",cas.toString()));
        assertEquals(agenda,f.d.db.sql.queryForMap("SELECT * FROM institution_task WHERE id=?",task.id().toString()));
        assertEquals("ACEPTADA",flow.delReq(f.a(f.head),req).getFirst().get("state"));
        denied(404,()->flow.decidir(f.a(f.ti),key(),p,new TerminoColaboracion.Decidir(999,"ACEPTADA","No revelar estado terminal")));
        assertEquals("ACEPTADA",f.d.db.sql.queryForObject("SELECT state FROM institution_collaboration WHERE id=?",String.class,col.toString()));
        assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_action",Long.class));
    }
    @Test void solicitudNoEsCierreYConfirmaSoloDespuesDeDecisionExpresa(){
        var p=solicitar();
        f.core.revisarCaso(f.a(f.otro),key(),cas,new Decision(0,"Solicitud del profesional responsable"),"SOLICITUD_TERMINO");
        denied(409,()->f.core.revisarCaso(f.a(f.headOtra),key(),cas,new Decision(1,"Falta decidir intervención"),"CIERRE"));
        denied(409,()->f.core.u2().actuar(f.a(f.head),key(),req,new Decision(rev("core_requirement",req),"No inferir cierre"),true));
        flow.decidir(f.a(f.headOtra),key(),p,decidir("ACEPTADA"));
        f.core.revisarCaso(f.a(f.headOtra),key(),cas,new Decision(1,"Revisión ordinaria independiente"),"CIERRE");
        assertEquals("ACTIVO",f.core.u2().requerimiento(f.a(f.head),req).get("state"));
        f.core.u2().actuar(f.a(f.head),key(),req,new Decision(rev("core_requirement",req),"Cierre global explícito"),true);
        assertEquals("TERMINADO",f.core.u2().requerimiento(f.a(f.head),req).get("state"));
    }
    @Test void rechazoYRetiroConservanHistoriaYPermitenNuevaPeticion(){
        var p=solicitar();flow.decidir(f.a(f.headOtra),key(),p,decidir("RECHAZADA"));
        var next=solicitar();flow.decidir(f.a(f.head),key(),next,decidir("RETIRADA"));
        assertEquals(2,flow.delReq(f.a(f.head),req).size());
        assertEquals("EN_GESTION",f.core.caso(f.a(f.otro),cas).estado());
        denied(409,()->flow.decidir(f.a(f.headOtra),key(),next,decidir("ACEPTADA")));
        assertFalse(flow.pendienteCaso(cas));
    }
    @Test void profesionalDeCasoYTiNoSustituyenJefaturasNiLeenExpedienteMixto(){
        denied(404,()->flow.solicitar(f.a(f.prof),key(),col,new TerminoColaboracion.Solicitar(rev("core_requirement",req),1,"No es principal funcional")));
        var p=solicitar();
        denied(404,()->flow.decidir(f.a(f.otro),key(),p,decidir("ACEPTADA")));
        denied(404,()->flow.decidir(f.a(f.ti),key(),p,new TerminoColaboracion.Decidir(999,"ACEPTADA","No revelar revisión a TI")));
        denied(404,()->flow.delReq(f.a(f.otro),req));
        denied(404,()->flow.delCaso(f.a(f.ti),cas));
        assertEquals(1,flow.delCaso(f.a(f.otro),cas).size());
        assertTrue(flow.bandeja(f.a(f.prof)).isEmpty());
        denied(404,()->f.core.detalle(f.a(f.headOtra),f.core.caso(f.a(f.otro),cas).comunicacionId()));
    }
    @Test void idempotenciaNoDuplicaSolicitudNiHistoriaYBloqueaSegundoPendiente(){
        var k=key();var body=new TerminoColaboracion.Solicitar(rev("core_requirement",req),1,"Fundamento inmutable");
        assertEquals(flow.solicitar(f.a(f.head),k,col,body),flow.solicitar(f.a(f.head),k,col,body));
        denied(409,()->flow.solicitar(f.a(f.head),k,col,new TerminoColaboracion.Solicitar(body.revisionRequerimiento(),1,"Otro cuerpo")));
        denied(409,this::solicitar);
        assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_collaboration_end",Long.class));
        assertEquals(1,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event WHERE code='COLABORACION_TERMINO_SOLICITADO'",Long.class));
    }
    @Test void carreraAceptacionYRetiroProduceUnaSolaDecision()throws Exception{
        var p=solicitar();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var start=new java.util.concurrent.CountDownLatch(1);
            var accept=pool.submit(()->{start.await();try{flow.decidir(f.a(f.headOtra),key(),p,decidir("ACEPTADA"));return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});
            var withdraw=pool.submit(()->{start.await();try{flow.decidir(f.a(f.head),key(),p,decidir("RETIRADA"));return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}});
            start.countDown();assertNotEquals(accept.get(),withdraw.get());
        }
        assertEquals(2,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE resource_id=?",Long.class,p.toString()));
        assertEquals("EN_GESTION",f.core.caso(f.a(f.otro),cas).estado());
    }
    @Test void falloDeAuditoriaRevierteDecisionYRevisionDelReq(){
        var p=solicitar();long initial=rev("core_requirement",req);var k=key();
        f.d.ddl(st->st.execute("ALTER TABLE institution_flow_history ADD CONSTRAINT test_end_block CHECK(action_code<>'COLABORACION_TERMINO_ACEPTADA')"));
        try{assertThrows(org.springframework.dao.DataAccessException.class,()->flow.decidir(f.a(f.headOtra),k,p,decidir("ACEPTADA")));}
        finally{f.d.ddl(st->st.execute("ALTER TABLE institution_flow_history DROP CONSTRAINT test_end_block"));}
        assertEquals(initial,rev("core_requirement",req));assertTrue(flow.pendienteCaso(cas));
        flow.decidir(f.a(f.headOtra),k,p,decidir("ACEPTADA"));assertFalse(flow.pendienteCaso(cas));
    }
    @Test void avisosSonDeJefaturaSinExpedienteNiAcuseYSeDeduplican(){
        var p=solicitar();for(int i=0;i<3;i++)f.notices.proyectar();
        assertEquals(1,f.notices.listar(f.a(f.headOtra),0,50).items().stream().filter(n->n.tipo().equals("CONTINUIDAD_SOLICITADA")).count());
        assertTrue(f.notices.listar(f.a(f.ti),0,50).items().isEmpty());
        assertEquals(0,f.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt",Long.class));
        flow.decidir(f.a(f.headOtra),key(),p,decidir("ACEPTADA"));for(int i=0;i<3;i++)f.notices.proyectar();
        assertTrue(f.notices.listar(f.a(f.head),0,50).items().stream().anyMatch(n->n.tipo().equals("CONTINUIDAD_DECIDIDA")));
        denied(404,()->f.core.caso(f.a(f.head),cas));
    }
    @Test void revocacionDelOrigenNoSeSustituyePorElCuerpoGuardado(){
        var p=solicitar();
        var account=f.ids.cuentas(f.ti).stream().filter(x->x.id().equals(f.head)).findFirst().orElseThrow();
        ConcesionFixture.revocar(f.d.db,f.ids,f.ti,f.head,account.asignaciones().getFirst().get("id").toString(),f.tiempo);
        denied(404,()->flow.decidir(f.a(f.headOtra),key(),p,decidir("ACEPTADA")));
        denied(404,()->flow.delReq(f.a(f.head),req));
        assertTrue(flow.pendienteCaso(cas));
        flow.decidir(f.a(f.headOtra),key(),p,decidir("RECHAZADA"));
        assertFalse(flow.pendienteCaso(cas));
    }
    @Test void transferenciaDePrincipalNoReinterpretaUnaPropuestaAnterior(){
        var p=solicitar();
        var transfer=f.core.continuidad().transferir(f.a(f.head),key(),req,new ContinuidadU2.Trasladar(rev("core_requirement",req),new NuevoDestino("UPD","DEMO_SEDE_A",f.headOtra),"Cambio de principal independiente","Paquete limitado"));
        f.core.continuidad().decidirTransferencia(f.a(f.headOtra),key(),transfer.id(),new ContinuidadU2.Resolver(0,"ACEPTADA",null,"Asume coordinación principal"));
        denied(409,()->flow.decidir(f.a(f.headOtra),key(),p,decidir("ACEPTADA")));
        denied(404,()->flow.delReq(f.a(f.head),req));
        flow.decidir(f.a(f.headOtra),key(),p,decidir("RETIRADA"));
        assertEquals("EN_GESTION",f.core.caso(f.a(f.otro),cas).estado());
        assertFalse(flow.pendienteCaso(cas));
    }
}
