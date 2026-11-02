package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import static cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** ORG-02/11: autoridad sintética sobre H2; no concede designaciones institucionales reales. */
class DistribuidorUnicoTest {
    @TempDir Path temp;
    DatosNucleoTest data;
    Reloj clock=new Reloj();
    IdentidadInstitucional ids;
    CatalogoLaboratorio catalogo;
    NucleoInstitucional core;
    String ti;
    static final class Reloj extends Clock {
        Instant instante=Instant.parse("2026-11-02T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(instante,zone);}
        public Instant instant(){return instante;}
    }
    @BeforeEach void setup()throws Exception{
        data=new DatosNucleoTest(temp);limpiarIdentidad();ids=new IdentidadInstitucional(data.db,clock);catalogo=new CatalogoLaboratorio(ids);
        core=new NucleoInstitucional(data.db,catalogo,data.recepcion,data.mapper,clock);
        ti=ids.bootstrap(new Crear("ti.unico","Soporte sintético","ti.unico@example.invalid","TI",null,null,UUID.randomUUID()+"aZ")).id();
    }
    void limpiarIdentidad(){for(var table:List.of("staff_profile_history","identity_event","staff_assignment","staff_account"))data.db.sql.update("DELETE FROM "+table);}
    @AfterEach void close(){limpiarIdentidad();data.close();}
    Cuenta user(String name){return ids.crear(ti,new Crear(name,"Persona sintética "+name,name+"@example.invalid","Sin facultad por cargo",null,null,UUID.randomUUID()+"aZ"));}
    Otorgar distribucion(Instant from,Instant until){return new Otorgar("DISTRIBUCION","INSTITUCION",null,null,from,until);}
    String otorgar(Cuenta account,Instant from,Instant until){return ids.otorgar(ti,account.id(),distribucion(from,until));}
    Cuenta actual(Cuenta account){return ids.cuentas(ti).stream().filter(a->a.id().equals(account.id())).findFirst().orElseThrow();}
    Perfil perfil(Cuenta account,boolean enabled){var a=actual(account);return new Perfil(a.nombre(),a.correo(),a.cargo(),a.unidad(),a.sede(),enabled,a.revision());}
    void conflicto(org.junit.jupiter.api.function.Executable work){var error=assertThrows(FalloTecnico.class,work);assertEquals(409,error.status);assertEquals("DISTRIBUIDOR_CENTRAL_SOLAPADO",error.code);}

    @Test void org02RechazaSegundaDesignacionInclusoMismaPersonaSinCambiosParciales(){
        var a=user("primero");var b=user("segundo");otorgar(a,clock.instant(),null);
        long epoch=actual(b).revision();int eventos=data.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event",Integer.class);
        conflicto(()->otorgar(b,clock.instant(),null));conflicto(()->otorgar(a,clock.instant(),null));
        assertEquals(epoch,actual(b).revision());assertTrue(actual(b).asignaciones().isEmpty());
        assertEquals(eventos,data.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event",Integer.class));assertTrue(catalogo.operador(a.id()).distribuidor());assertFalse(catalogo.operador(b.id()).distribuidor());
    }
    @Test void org11IntervalosSonSemiabiertosYRelevoFuturoNoDuplicaFacultad(){
        var a=user("saliente");var b=user("entrante");var corte=clock.instant().plusSeconds(3600);
        otorgar(a,clock.instant(),corte);otorgar(b,corte,null);
        assertTrue(catalogo.operador(a.id()).distribuidor());assertFalse(catalogo.operador(b.id()).distribuidor());
        conflicto(()->otorgar(b,corte.minusMillis(1),null));
        clock.instante=corte;
        assertFalse(catalogo.operador(a.id()).distribuidor());assertTrue(catalogo.operador(b.id()).distribuidor());
        // Los intervalos terminados son historia, no reservas operativas futuras.
        otorgar(a,corte.minusSeconds(7200),corte.minusSeconds(3600));assertFalse(catalogo.operador(a.id()).distribuidor());
    }
    @Test void org11ConcesionesConcurrentesConfirmanSoloUna()throws Exception{
        var a=user("candidato.a");var b=user("candidato.b");var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            List<Future<Integer>> attempts=new ArrayList<>();
            for(var target:List.of(a,b))attempts.add(executor.submit(()->{ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));try{otorgar(target,clock.instant(),null);return 200;}catch(FalloTecnico e){return e.status;}}));
            assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();
            var results=new ArrayList<Integer>();for(var attempt:attempts)results.add(attempt.get(5,TimeUnit.SECONDS));Collections.sort(results);assertEquals(List.of(200,409),results);
        }
        assertEquals(1,data.db.sql.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE role_code='DISTRIBUCION'",Integer.class));
        assertEquals(1,List.of(a,b).stream().filter(u->catalogo.operador(u.id()).distribuidor()).count());
    }
    @Test void activarCuentaConDesignacionSolapadaReviertePerfilYPermisos(){
        var a=user("vigente");var b=user("inactivo");otorgar(a,clock.instant(),null);
        ids.actualizar(ti,b.id(),perfil(b,false));otorgar(b,clock.instant(),null);
        var previous=actual(b);int eventos=data.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event",Integer.class);
        conflicto(()->ids.actualizar(ti,b.id(),perfil(b,true)));
        assertFalse(actual(b).activo());assertEquals(previous.revision(),actual(b).revision());assertEquals(eventos,data.db.sql.queryForObject("SELECT COUNT(*) FROM identity_event",Integer.class));
        ids.revocar(ti,a.id(),actual(a).asignaciones().getFirst().get("id").toString());ids.actualizar(ti,b.id(),perfil(b,true));assertTrue(catalogo.operador(b.id()).distribuidor());
    }
    @Test void solapamientoHistoricoFallaCerradoHastaRevocacionExplicita(){
        var a=user("historico.a");var b=user("historico.b");otorgar(a,clock.instant(),null);String other=UUID.randomUUID().toString();
        // Representa datos previos a esta política, exclusivamente en la base descartable del test.
        data.db.sql.update("INSERT INTO staff_assignment(id,actor_id,role_code,scope_code,valid_from,granted_by) VALUES(?,?,'DISTRIBUCION','INSTITUCION',?,?)",other,b.id(),clock.millis(),ti);
        assertFalse(catalogo.operador(a.id()).distribuidor());assertFalse(catalogo.operador(b.id()).distribuidor());
        var com=data.nuevaWeb();var error=assertThrows(FalloTecnico.class,()->core.distribuir(catalogo.operador(a.id()),UUID.randomUUID(),com,new Distribucion(0,List.of(new NuevoDestino("UPR","DEMO_SEDE_A",a.id())))));assertEquals(403,error.status);
        assertEquals(0,data.db.sql.queryForObject("SELECT COUNT(*) FROM core_destination",Integer.class));
        ids.revocar(ti,b.id(),other);assertTrue(catalogo.operador(a.id()).distribuidor());assertFalse(catalogo.operador(b.id()).distribuidor());
    }
    @Test void relevoRevocaSesionYActorAnteriorConservandoPendienteOriginalYFolio(){
        var a=user("saliente");var b=user("entrante");String grant=otorgar(a,clock.instant(),null);var principal=(Principal)ids.autenticar(a.usuario());var old=catalogo.operador(a.id());
        var com=data.nuevaWeb();var registro=data.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",com.toString());var inbox=data.db.sql.queryForList("SELECT * FROM institutional_inbox");
        ids.revocar(ti,a.id(),grant);otorgar(b,clock.instant(),null);
        assertFalse(ids.vigente(principal));var error=assertThrows(FalloTecnico.class,()->core.buzon(old,0,100));assertEquals(403,error.status);
        assertEquals(1,core.buzon(catalogo.operador(b.id()),0,100).size());assertEquals(registro,data.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",com.toString()));assertEquals(inbox,data.db.sql.queryForList("SELECT * FROM institutional_inbox"));
        assertEquals(0,data.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Integer.class));assertEquals(0,data.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Integer.class));
    }
}
