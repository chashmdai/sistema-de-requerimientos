package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class TerritorioInstitucionalTest {
    @TempDir Path temp;DatosNucleoTest d;TerritorioFixture t;AreasFixture f;NucleoU2 u;
    final AreasInstitucionalesTest.Reloj clock=new AreasInstitucionalesTest.Reloj();
    @BeforeEach void setup()throws Exception{
        String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_materia?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");
        if(mysql!=null)limpiar();t=new TerritorioFixture(d,clock);f=t.f;u=f.core.u2();
    }
    void limpiar(){d.limpiar();for(var table:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+table);}
    @AfterEach void close(){if(d!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)limpiar();d.close();}}
    void denied(int code,org.junit.jupiter.api.function.Executable work){assertEquals(code,assertThrows(FalloTecnico.class,work).status);}
    long reqs(){return d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_req",Long.class);}

    @Test void propuestaTiNoEsCompetenciaYContratoAnteriorNoLaAcredita(){var sol=t.solicitud("REG_LAB_1");denied(409,()->t.asumir(sol));assertEquals(0,reqs());assertEquals("PENDIENTE",u.solicitud(f.a(f.headA),sol).get("state"));assertEquals("COBERTURA_NO_ACREDITADA",((CatalogoTerritorial.Orientacion)u.solicitud(f.a(f.headA),sol).get("territorio")).estado());}

    @Test void coberturaFirmadaAsumeReqSinCasoYConservaPrincipalTrasSuspenderla()throws Exception{
        t.publicar();UUID sol=t.solicitud("REG_LAB_1"),q=t.asumir(sol).id();var r=u.requerimiento(f.a(f.headA),q);var basis=(CatalogoTerritorial.Decision)r.get("territorioInicial");
        assertEquals("COBERTURA_ACREDITADA",basis.modo());assertEquals(t.propuesta,basis.cobertura());assertEquals("REG_LAB_1",basis.regionDeclarada());assertEquals("DEMO_SEDE_A",basis.sedeInicial());assertEquals(true,r.get("sinCasos"));assertEquals(1,reqs());
        UUID first=u.abrirCaso(f.a(f.headA),UUID.randomUUID(),q,new NucleoU2.Apertura(0,f.profA,"Primer objetivo independiente")).id();
        UUID second=u.abrirCaso(f.a(f.headA),UUID.randomUUID(),q,new NucleoU2.Apertura(1,f.profA,"Segundo objetivo independiente")).id();
        t.publicar(List.of(t.certificado("SUSPENDIDA",clock.instant().plusSeconds(600))));
        assertEquals(basis,u.requerimiento(f.a(f.headA),q).get("territorioInicial"));assertEquals(q,f.core.caso(f.a(f.profA),first).requerimientoId());assertEquals(q,f.core.caso(f.a(f.profA),second).requerimientoId());
        assertEquals("DEMO_SEDE_A",u.requerimiento(f.a(f.headA),q).get("site_id"));assertEquals(1,reqs());denied(409,()->t.asumir(t.solicitud("REG_LAB_1")));
    }

    @Test void coberturaVencidaRetiradaOAlteradaFallaCerrada()throws Exception{
        t.publicar(List.of(t.certificado("ACTIVA",clock.instant().plusSeconds(10))));var sol=t.solicitud("REG_LAB_1");clock.now=clock.now.plusSeconds(11);denied(409,()->t.asumir(sol));
        t.publicar();t.org.terminarCobertura(f.ti,t.propuesta.toString(),clock.instant().plusSeconds(900));denied(409,()->t.asumir(sol));assertEquals(0,reqs());
        t.publicar();assertNotNull(t.asumir(sol));
        t.publicar(List.of());denied(409,()->t.asumir(t.solicitud("REG_LAB_1")));
    }

    @Test void sedeDistintaNoPuedeSuplantarLaCobertura()throws Exception{t.publicar();var e=assertThrows(FalloTecnico.class,()->f.core.territorio().resolver(true,"DEMO_SEDE_B","REG_LAB_1","UTILIZABLE",null,null,true));assertEquals(409,e.status);assertEquals("SEDE_NO_CORRESPONDE_A_DOMICILIO",e.code);assertEquals(0,reqs());}

    @Test void domicilioContradictorioConservaDeclaracionYExigeFundamentoYReferencias(){
        var sol=t.solicitud("REG_LAB_1");
        var bad=new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Evaluación de jefatura","CONTRADICTORIO","Antecedentes discordantes",null);
        denied(400,()->u.asumirTerritorial(f.a(f.headA),UUID.randomUUID(),sol,bad));assertEquals(0,reqs());
        var body=new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Evaluación de jefatura","CONTRADICTORIO","Decisión territorial fundada","Referencia sintética de contacto verificado");var q=u.asumirTerritorial(f.a(f.headA),UUID.randomUUID(),sol,body).id();
        assertEquals("REG_LAB_1",u.solicitud(f.a(f.headA),sol).get("domicile_region"));var basis=(CatalogoTerritorial.Decision)u.requerimiento(f.a(f.headA),q).get("territorioInicial");assertEquals("DECISION_FUNDADA",basis.modo());assertEquals("CONTRADICTORIO",basis.condicionDomicilio());assertNull(basis.cobertura());assertEquals(body.referenciasTerritoriales(),basis.referencias());
    }

    @Test void condicionExplicitaNoConvierteFaltaDeCatalogoEnDomicilioAusente(){var sol=t.solicitud("REG_LAB_1");denied(400,()->u.asumirTerritorial(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Evaluación","AUSENTE","No se permite borrar domicilio","Referencia")));denied(409,()->t.asumir(sol));assertEquals(0,reqs());}

    @Test void tiMateriaYOtraAreaNoDecidenTerritorio()throws Exception{
        t.publicar();var sol=t.solicitud("REG_LAB_1");var body=new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Asunción legítima","UTILIZABLE",null,null);
        for(String a:List.of(f.ti,f.profA,f.headB,t.m.upr))denied(404,()->u.asumirTerritorial(f.a(a),UUID.randomUUID(),sol,body));assertEquals(0,reqs());
        assertNotNull(u.asumirTerritorial(f.a(f.headA),UUID.randomUUID(),sol,body));
    }

    @Test void asuncionConcurrenteEIdempotenciaNoDuplicaReqNiConstancia()throws Exception{
        t.publicar();var sol=t.solicitud("REG_LAB_1");var key=UUID.randomUUID();var body=new NucleoU2.AsuncionTerritorial("1.0",0,f.profA,"Asunción concurrente","UTILIZABLE",null,null);var actor=f.a(f.headA);var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->{gate.await();return u.asumirTerritorial(actor,key,sol,body);});var two=pool.submit(()->{gate.await();return u.asumirTerritorial(actor,key,sol,body);});gate.countDown();assertEquals(one.get(8,TimeUnit.SECONDS),two.get(8,TimeUnit.SECONDS));}
        assertEquals(1,reqs());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE action_code='REQ_TERRITORIO_ACREDITADO'",Long.class));
    }

    @Test void falloDeConstanciaRevierteAsuncionCompletaYPermiteMismoReintento()throws Exception{
        t.publicar();var sol=t.solicitud("REG_LAB_1");var key=UUID.randomUUID();var body=new NucleoU2.Asumir(0,f.profA,"Evaluación",null);
        d.ddl(sql->sql.execute("ALTER TABLE institution_flow_history ADD CONSTRAINT territorio_rollback CHECK(action_code<>'REQ_TERRITORIO_ACREDITADO')"));
        try{assertThrows(org.springframework.dao.DataAccessException.class,()->u.asumir(f.a(f.headA),key,sol,body));}finally{d.ddl(sql->sql.execute("ALTER TABLE institution_flow_history DROP CONSTRAINT territorio_rollback"));}
        assertEquals(0,reqs());assertEquals("PENDIENTE",u.solicitud(f.a(f.headA),sol).get("state"));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,key.toString()));assertNotNull(u.asumir(f.a(f.headA),key,sol,body));
    }

    @Test void registro18RechazaHuellaInventadaYSinReinterpretar17()throws Exception{
        var c=t.certificado("ACTIVA",clock.instant().plusSeconds(600));long before=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class);
        denied(409,()->t.publicar(List.of(new CatalogoTerritorial.Acreditacion(c.cobertura(),c.region(),c.sede(),c.desde(),c.hasta(),"0".repeat(64),c.estado(),c.referencia(),c.sha256()))));assertEquals(before,d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class));
        var r=t.common;var legacy=d.mapper.valueToTree(new AutoridadesInstitucionales.Registro17("1.7",before+1,clock.instant(),f.end,r.vinculos(),r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea(),r.materias(),r.sedesRegionales(),r.ambitosMateria(),List.of(),List.of(),List.of(),List.of(),List.of()));((tools.jackson.databind.node.ObjectNode)legacy).set("coberturas",d.mapper.valueToTree(List.of(c)));denied(400,()->f.importar(legacy));
        t.publicar();assertEquals(1,f.f.autoridades.contextoTerritorial().coberturas().size());
    }

    @Test void propiaJefaturaConDomicilioSeAcreditaSinTransitoCentral()throws Exception{
        t.publicar();var com=f.core.registrarManual(f.a(f.headA),UUID.randomUUID(),new Manual("Ingreso directo de jefatura","PRESENCIAL",clock.instant().minusSeconds(10),f.a(f.headA).sede(),null)).id();
        var body=new NucleoU2.AsuncionPropiaTerritorial("1.0",0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),f.profA,"Evaluación propia","REG_LAB_1","UTILIZABLE",null,null);var key=UUID.randomUUID();var result=u.asumirPropiaTerritorial(f.a(f.headA),key,com,body);assertEquals(result,u.asumirPropiaTerritorial(f.a(f.headA),key,com,body));assertEquals(1,reqs());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case",Long.class));assertEquals("REG_LAB_1",((CatalogoTerritorial.Decision)u.requerimiento(f.a(f.headA),result.id()).get("territorioInicial")).regionDeclarada());
    }
    @Test void competenciaCentralNoSeRegionalizaPorDomicilioNiExigeCobertura(){
        var decision=f.core.territorio().resolver(false,"DEMO_SEDE_A","REG_LAB_2","UTILIZABLE",null,null,true);
        assertEquals("COMPETENCIA_CENTRAL",decision.modo());assertNull(decision.cobertura());assertEquals("DEMO_SEDE_A",decision.sedeInicial());assertEquals("REG_LAB_2",decision.regionDeclarada());
    }

}
