package cl.nexbyte.siger.institucional.nucleo;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import cl.nexbyte.siger.institucional.nucleo.CatalogoLaboratorio.Operador;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class NucleoInstitucionalTest {
    @TempDir Path dir;DatosNucleoTest data;NucleoInstitucional core;Operador dist,jef,prof,jefUpd,prof2;
    @BeforeEach void open()throws Exception{data=new DatosNucleoTest(dir);core=data.core;dist=data.actor("demo.distribucion");jef=data.actor("demo.jefatura.upr.a");prof=data.actor("demo.profesional.upr.a");prof2=data.actor("demo.profesional2.upr.a");jefUpd=data.actor("demo.jefatura.upd.a");}
    @AfterEach void close(){if(data!=null)data.close();}
    long count(String table){return data.db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
    @Test void detalleConservaUnaInstantaneaSiOtraSesionDecideDuranteLaLectura()throws Exception{
        var cid=data.web();var checked=org.mockito.Mockito.spy(data.recepcion);
        try(var executor=Executors.newSingleThreadExecutor()){
            org.mockito.Mockito.doAnswer(invocation->{executor.submit(()->core.decidirComunicacion(dist,UUID.randomUUID(),cid,new Decision(0,"Respuesta sintética concurrente"),false)).get(3,TimeUnit.SECONDS);return invocation.callRealMethod();}).when(checked).entregaRegistrada(cid);
            var reader=new NucleoInstitucional(data.db,data.catalogo,checked,data.mapper,Clock.systemUTC());
            var previous=reader.detalle(dist,cid);assertThat(previous.comunicacion().estado()).isEqualTo("ABIERTA");assertThat(previous.respuestaDirecta()).isNull();assertThat(previous.historial()).isEmpty();
            var current=core.detalle(dist,cid);assertThat(current.comunicacion().estado()).isEqualTo("RESPONDIDA");assertThat(current.respuestaDirecta()).isEqualTo("Respuesta sintética concurrente");
        }
    }
    @Test void buzonPaginadoNoOmiteComunicacionesNiAmpliaAlcance(){
        var first=manual(null);var second=manual(null);var page=core.buzon(dist,0,1);assertThat(page).hasSize(1);assertThat(page.getFirst().id()).isEqualTo(first);
        var next=core.buzon(dist,page.getFirst().secuencia(),1);assertThat(next).hasSize(1);assertThat(next.getFirst().id()).isEqualTo(second);assertThat(core.buzon(prof,0,100)).isEmpty();
    }
    UUID manual(String response){return core.registrarManual(dist,UUID.randomUUID(),new Manual("Atención exclusivamente sintética","Canal declarado de demostración",Instant.parse("2025-01-02T10:00:00Z"),"DEMO_SEDE_A",response)).id();}
    List<Destino> distribuir(UUID cid,boolean different){core.distribuir(dist,UUID.randomUUID(),cid,new Distribucion(core.detalle(dist,cid).comunicacion().revision(),List.of(new NuevoDestino("UPR","DEMO_SEDE_A",jef.id()),new NuevoDestino(different?"UPD":"UPR","DEMO_SEDE_A",different?jefUpd.id():jef.id()))));return core.detalle(dist,cid).destinos();}
    UUID abrir(Operador head,Destino d,String responsible){return core.abrirCaso(head,UUID.randomUUID(),d.id(),new Asignacion(d.revision(),responsible,null)).id();}
    void cerrar(Operador head,Operador professional,UUID cid){
        var c=core.caso(professional,cid);core.actuar(professional,UUID.randomUUID(),cid,new Decision(c.revision(),"Actuación general sintética"));
        c=core.caso(professional,cid);core.revisarCaso(professional,UUID.randomUUID(),cid,new Decision(c.revision(),"Solicito término"),"SOLICITUD_TERMINO");
        c=core.caso(head,cid);core.revisarCaso(head,UUID.randomUUID(),cid,new Decision(c.revision(),"Revisión conforme"),"CIERRE");
    }
    @Test void buzonWebConservaCuerpoFirmaEIdentidadMientrasGestionSeGuardaAparte(){
        var cid=data.web();String before=data.db.sql.queryForObject("SELECT CONCAT(body,receipt) FROM institutional_inbox",String.class);
        assertThat(core.buzon(dist,0,100)).extracting(Resumen::id).contains(cid);
        var detail=core.detalle(dist,cid);assertThat(detail.contenido().identidad()).isEqualTo(data.historica.comunicacion().identidad());
        assertThat(detail.contenido().tipoIdentidad()).isEqualTo("AUTENTICADA_CLAVE_UNICA");
        var ds=distribuir(cid,true);abrir(jef,ds.stream().filter(d->d.unidad().equals("UPR")).findFirst().orElseThrow(),prof.id());
        assertThat(data.db.sql.queryForObject("SELECT CONCAT(body,receipt) FROM institutional_inbox",String.class)).isEqualTo(before);
        assertThat(data.recepcion.guardar(data.historica).registro().comunicacionId()).isEqualTo(cid);
    }
    @Test void manualYaRespondidaSinCasoSinRequerimientoNiCiudadanoFalso(){
        UUID key=UUID.randomUUID();var request=new Manual("Texto sintético","Atención declarada",Instant.parse("2025-01-01T00:00:00Z"),"DEMO_SEDE_A","Respuesta ya entregada sintética");
        var first=core.registrarManual(dist,key,request);assertThat(core.registrarManual(dist,key,request)).isEqualTo(first);
        var detail=core.detalle(dist,first.id());assertThat(detail.comunicacion().estado()).isEqualTo("RESPONDIDA");assertThat(detail.comunicacion().requerimientoId()).isNull();assertThat(count("core_case")).isZero();assertThat(count("core_requirement")).isZero();
        assertThat(detail.contenido().identidad()).isNull();assertThat(detail.contenido().declaracionNna()).isNull();assertThat(detail.contenido().registradoPor()).isEqualTo(dist.id());
        assertThat(detail.contenido().recibidaEnDeclarada()).isBefore(detail.comunicacion().registradaEn());assertThat(detail.decisionAutor()).isEqualTo(dist.id());assertThat(detail.decisionFecha()).isNotNull();assertThat(count("institutional_inbox")).isZero();
        assertThat(data.db.sql.queryForObject("SELECT operacion_id FROM communication_registry",String.class)).isNull();
    }
    @Test void respuestaSinCasoYDescarteExigenTextoResponsableYFechaSinBorrarRegistro(){
        var cid=manual(null);core.decidirComunicacion(dist,UUID.randomUUID(),cid,new Decision(0,"Motivo de descarte sintético"),true);
        var d=core.detalle(dist,cid);assertThat(d.comunicacion().estado()).isEqualTo("DESCARTADA");assertThat(d.decisionAutor()).isEqualTo(dist.id());assertThat(d.decisionFecha()).isNotNull();assertThat(count("communication_registry")).isEqualTo(1);assertThat(count("core_requirement")).isZero();
        assertThatThrownBy(()->core.decidirComunicacion(dist,UUID.randomUUID(),manual(null),new Decision(0," "),true)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void dosDestinosIndependientesUnaRespuestaNoResuelveElOtro(){
        var cid=manual(null);var ds=distribuir(cid,true);var a=ds.stream().filter(d->d.unidad().equals("UPR")).findFirst().orElseThrow();var b=ds.stream().filter(d->d.unidad().equals("UPD")).findFirst().orElseThrow();
        core.resolverDestino(jef,UUID.randomUUID(),a.id(),new Decision(a.revision(),"Orientación sintética"),true);
        var detail=core.detalle(dist,cid);assertThat(detail.comunicacion().destinosPendientes()).isEqualTo(1);assertThat(detail.comunicacion().estado()).isEqualTo("ABIERTA");assertThat(count("core_requirement")).isZero();
        assertThat(core.detalle(jefUpd,cid).destinos().getFirst().id()).isEqualTo(b.id());abrir(jefUpd,b,data.actor("demo.profesional.upd.a").id());assertThat(count("core_requirement")).isEqualTo(1);
    }
    @Test void dosPrimerasAperturasConcurrentesCreanUnAgrupadorYVariosCasosEnMismaUnidad()throws Exception{
        var cid=manual(null);var ds=distribuir(cid,false);var gate=new CountDownLatch(1);var results=new ArrayList<UUID>();
        try(var executor=Executors.newFixedThreadPool(2)){
            var tasks=new ArrayList<Future<UUID>>();for(var d:ds)tasks.add(executor.submit(()->{gate.await();return abrir(jef,d,prof.id());}));gate.countDown();for(var t:tasks)results.add(t.get(30,TimeUnit.SECONDS));
        }
        assertThat(results).doesNotHaveDuplicates();assertThat(count("core_requirement")).isEqualTo(1);assertThat(count("core_case")).isEqualTo(2);
        assertThat(core.caso(jef,results.getFirst()).requerimientoId()).isEqualTo(core.caso(jef,results.getLast()).requerimientoId());
        var sameDestination=core.detalle(jef,cid).destinos().getFirst();abrir(jef,sameDestination,prof2.id());assertThat(count("core_case")).isEqualTo(3);
    }
    @Test void cierreIndependienteYUltimoDestinoSinCasoReevaluaTerminoGlobal(){
        var cid=manual(null);var ds=distribuir(cid,true);var a=ds.stream().filter(d->d.unidad().equals("UPR")).findFirst().orElseThrow();var b=ds.stream().filter(d->d.unidad().equals("UPD")).findFirst().orElseThrow();
        var c1=abrir(jef,a,prof.id());var updated=core.detalle(jef,cid).destinos().getFirst();var c2=abrir(jef,updated,prof2.id());
        cerrar(jef,prof,c1);assertThat(core.caso(jef,c2).estado()).isEqualTo("EN_GESTION");assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");
        cerrar(jef,prof2,c2);assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");
        var dest=core.detalle(jef,cid).destinos().getFirst();core.resolverDestino(jef,UUID.randomUUID(),dest.id(),new Decision(dest.revision(),"Evaluación agotada, explícitamente finalizada"),false);
        assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");
        core.resolverDestino(jefUpd,UUID.randomUUID(),b.id(),new Decision(b.revision(),"Último destino responde sin Caso"),true);
        assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("TERMINADO");
        assertThatThrownBy(()->abrir(jef,core.detalle(jef,cid).destinos().getFirst(),prof.id())).isInstanceOf(FalloTecnico.class);
    }
    @Test void observacionesDevuelvenGestionConActuacionesAutorYFechaConservados(){
        var cid=manual(null);var c=abrir(jef,distribuir(cid,false).getFirst(),prof.id());core.actuar(prof,UUID.randomUUID(),c,new Decision(0,"Primera actuación"));
        core.revisarCaso(prof,UUID.randomUUID(),c,new Decision(1,"Solicitud"),"SOLICITUD_TERMINO");core.revisarCaso(jef,UUID.randomUUID(),c,new Decision(2,"Falta antecedente sintético"),"DEVOLUCION");
        var detail=core.caso(prof,c);assertThat(detail.estado()).isEqualTo("EN_GESTION");assertThat(detail.actuaciones()).hasSize(1);assertThat(detail.actuaciones().getFirst().autor()).isEqualTo(prof.id());assertThat(detail.actuaciones().getFirst().fecha()).isNotNull();assertThat(detail.revisiones()).hasSize(2);
        assertThat(detail.revisiones()).anyMatch(r->r.decision().equals("DEVOLUCION")&&r.autor().equals(jef.id()));assertThat(core.detalle(dist,cid).historial()).anyMatch(e->e.codigo().equals("TERMINO_DEVUELTO_CON_OBSERVACIONES"));
    }
    @Test void gestionPersonalJefaturaCierraSoloSuCasoYTransicionesPendientesNoEjecutan(){
        var cid=manual(null);var c=abrir(jef,distribuir(cid,false).getFirst(),jef.id());
        assertThatThrownBy(()->core.revisarCaso(jef,UUID.randomUUID(),c,new Decision(0,"Sin actuación"),"CIERRE")).isInstanceOf(FalloTecnico.class);
        core.actuar(jef,UUID.randomUUID(),c,new Decision(0,"Gestión personal sintética"));core.revisarCaso(jef,UUID.randomUUID(),c,new Decision(1,"Término decidido personalmente"),"CIERRE");
        assertThat(core.caso(jef,c).estado()).isEqualTo("CERRADO");assertThatThrownBy(()->core.transicionPendiente(jef,c)).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(409);
        assertThat(core.detalle(jef,cid).derivacionHabilitada()).isTrue();assertThat(core.detalle(jef,cid).reaperturaHabilitada()).isTrue();
    }
    @Test void vinculacionConservaOriginalesNoReabreYAvisaSoloAlResponsableSeleccionado(){
        var parent=data.web();var child=data.nuevaWeb();var ds=distribuir(parent,true);var c=abrir(jef,ds.stream().filter(d->d.unidad().equals("UPR")).findFirst().orElseThrow(),prof.id());cerrar(jef,prof,c);
        var before=data.db.sql.queryForList("SELECT id,body,receipt FROM institutional_inbox ORDER BY id");
        var caseBefore=core.caso(jef,c);core.vincular(dist,UUID.randomUUID(),child,new Vinculacion(parent,c,"Antecedente documental sintético"));
        assertThat(core.caso(jef,c).estado()).isEqualTo(caseBefore.estado());assertThat(core.caso(jef,c).actuaciones()).isEqualTo(caseBefore.actuaciones());assertThat(core.caso(jef,c).revision()).isEqualTo(caseBefore.revision()+1);assertThat(data.db.sql.queryForList("SELECT id,body,receipt FROM institutional_inbox ORDER BY id")).isEqualTo(before);
        assertThat(core.detalle(dist,child).comunicacion().folio()).isNotEqualTo(core.detalle(dist,parent).comunicacion().folio());
        assertThat(core.avisos(prof)).hasSize(1);assertThat(core.avisos(prof2)).isEmpty();assertThat(core.avisos(prof).getFirst().leido()).isFalse();
        assertThatThrownBy(()->core.vincular(dist,UUID.randomUUID(),parent,new Vinculacion(child,null,"Ciclo no permitido"))).isInstanceOf(FalloTecnico.class);
    }
    @Test void profesionalNoTieneVisibilidadGlobalNiPuedeCambiarUUIDAjeno(){
        var cid=manual(null);var ds=distribuir(cid,true);var d=ds.stream().filter(x->x.unidad().equals("UPR")).findFirst().orElseThrow();var c=abrir(jef,d,prof.id());
        assertThat(core.buzon(prof,0,100)).hasSize(1);assertThat(core.buzon(prof2,0,100)).isEmpty();
        assertThatThrownBy(()->core.detalle(prof2,cid)).isInstanceOf(FalloTecnico.class).extracting(e->((FalloTecnico)e).status).isEqualTo(404);
        assertThatThrownBy(()->core.caso(jefUpd,c)).isInstanceOf(FalloTecnico.class);assertThatThrownBy(()->core.actuar(prof2,UUID.randomUUID(),c,new Decision(0,"Ataque"))).isInstanceOf(FalloTecnico.class);
        assertThat(core.detalle(prof,cid).destinos()).hasSize(1);
        assertThatThrownBy(()->core.actuar(new Operador(prof.id(),prof.nombre(),CatalogoLaboratorio.Rol.JEFATURA,"UGI","DEMO_SEDE_B"),UUID.randomUUID(),c,new Decision(0,"Suplantar alcance"))).isInstanceOf(FalloTecnico.class);
    }
    @Test void rollbackAperturaNoDejaAgrupadorVacioYReintentoEsIdempotente(){
        var cid=manual(null);var d=distribuir(cid,false).getFirst();UUID key=UUID.randomUUID();var request=new Asignacion(d.revision(),prof.id(),null);
        data.ddl(sql->sql.execute("ALTER TABLE core_event ADD CONSTRAINT reject_open CHECK(code<>'CASO_ABIERTO')"));
        try{assertThatThrownBy(()->core.abrirCaso(jef,key,d.id(),request)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(count("core_requirement")).isZero();assertThat(count("core_case")).isZero();}
        finally{data.ddl(sql->sql.execute("ALTER TABLE core_event DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" reject_open"));}
        var first=core.abrirCaso(jef,key,d.id(),request);assertThat(core.abrirCaso(jef,key,d.id(),request)).isEqualTo(first);assertThat(count("core_requirement")).isEqualTo(1);assertThat(count("core_case")).isEqualTo(1);
    }
    @Test void rollbackUltimaResolucionNoDejaRequerimientoTerminadoConDestinoAbierto(){
        var cid=manual(null);var ds=distribuir(cid,true);var a=ds.stream().filter(d->d.unidad().equals("UPR")).findFirst().orElseThrow();var b=ds.stream().filter(d->d.unidad().equals("UPD")).findFirst().orElseThrow();
        var c=abrir(jef,a,prof.id());cerrar(jef,prof,c);a=core.detalle(jef,cid).destinos().getFirst();core.resolverDestino(jef,UUID.randomUUID(),a.id(),new Decision(a.revision(),"Finalizar"),false);
        UUID key=UUID.randomUUID();var request=new Decision(b.revision(),"Respuesta final sintética");
        data.ddl(sql->sql.execute("ALTER TABLE core_event ADD CONSTRAINT reject_resolve CHECK(code<>'DESTINO_RESPONDIDO')"));
        try{assertThatThrownBy(()->core.resolverDestino(jefUpd,key,b.id(),request,true)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("ACTIVO");assertThat(core.detalle(jefUpd,cid).destinos().getFirst().estado()).isEqualTo("EVALUANDO");}
        finally{data.ddl(sql->sql.execute("ALTER TABLE core_event DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" reject_resolve"));}
        core.resolverDestino(jefUpd,key,b.id(),request,true);assertThat(core.detalle(dist,cid).comunicacion().estadoRequerimiento()).isEqualTo("TERMINADO");
    }
}

