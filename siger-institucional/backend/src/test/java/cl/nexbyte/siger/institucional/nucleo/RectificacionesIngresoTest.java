package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class RectificacionesIngresoTest {
    @TempDir Path temp;DatosNucleoTest d;AreasFixture f;NucleoU2 u;RectificacionesIngreso changes;
    final AreasInstitucionalesTest.Reloj clock=new AreasInstitucionalesTest.Reloj();
    @BeforeEach void setup()throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_materia?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");if(mysql!=null)limpiar();f=new AreasFixture(d,clock);u=f.core.u2();changes=f.core.rectificacionesIngreso();}
    void limpiar(){d.limpiar();for(var table:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+table);}
    @AfterEach void close(){if(d!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)limpiar();d.close();}}
    UUID manual(){return f.core.registrarManual(f.a(f.profA),UUID.randomUUID(),new Manual("Antecedente originalmente capturado","TELÉFONO",clock.instant().minusSeconds(60),f.a(f.profA).sede(),null,"REG_LAB_1")).id();}
    UUID distribuir(UUID com){return u.distribuir(f.a(f.dist),UUID.randomUUID(),com,new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasifica sin alterar declaración",null)).id();}
    UUID asumir(UUID sol){return u.asumir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,f.profA,"Asunción legítima",null)).id();}
    long revision(UUID com){return d.db.sql.queryForObject("SELECT revision FROM core_communication WHERE id=?",Long.class,com.toString());}
    RectificacionesIngreso.Solicitud body(UUID com,String texto){return new RectificacionesIngreso.Solicitud("1.0",revision(com),new RectificacionesIngreso.Datos(texto,"CORREO",clock.instant().minusSeconds(90),"REG_LAB_2"),"Corrección fundada contrastada con el antecedente");}
    void denied(int code,org.junit.jupiter.api.function.Executable work){assertEquals(code,assertThrows(FalloTecnico.class,work).status);}

    @Test void rectificarConservaOriginalHashesFolioProcedenciaYComandoDeCaptura(){
        UUID com=manual(),key=UUID.randomUUID();var original=d.db.sql.queryForMap("SELECT * FROM core_manual WHERE communication_id=?",com.toString());var registry=d.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",com.toString());var origin=d.db.sql.queryForMap("SELECT * FROM communication_origin WHERE communication_id=?",com.toString());var input=body(com,"Contenido de captura corregido");
        var result=changes.registrar(f.a(f.profA),key,com,input);assertEquals(result,changes.registrar(f.a(f.profA),key,com,input));
        var v=changes.consultar(f.a(f.profA),com,0,25);assertEquals(1,v.total());assertEquals(input.datos(),v.vigente());assertEquals("Antecedente originalmente capturado",v.original().texto());assertEquals(f.profA,v.entradas().getFirst().autor());assertEquals(input.motivo(),v.entradas().getFirst().motivo());
        assertEquals(original,d.db.sql.queryForMap("SELECT * FROM core_manual WHERE communication_id=?",com.toString()));assertEquals(registry,d.db.sql.queryForMap("SELECT * FROM communication_registry WHERE comunicacion_id=?",com.toString()));assertEquals(origin,d.db.sql.queryForMap("SELECT * FROM communication_origin WHERE communication_id=?",com.toString()));assertEquals(v.original().texto(),f.core.detalle(f.a(f.profA),com).contenido().texto());
        denied(409,()->changes.registrar(f.a(f.profA),key,com,body(com,"Otro payload no reutiliza clave")));
    }

    @Test void distribucionRetiraFacultadDelRegistradorYLaResponsabilidadReqLaRestituye(){
        var com=manual();var sol=distribuir(com);var originalSession=f.a(f.profA);
        denied(404,()->changes.consultar(originalSession,com,0,25));denied(404,()->changes.registrar(originalSession,UUID.randomUUID(),com,body(com,"El registrador no decide después de distribuir")));
        for(String actor:List.of(f.ti,f.dist,f.headB))denied(404,()->changes.consultar(f.a(actor),com,0,25));
        changes.registrar(f.a(f.headA),UUID.randomUUID(),com,body(com,"Jefatura receptora corrige"));var req=asumir(sol);assertEquals(1,changes.consultar(f.a(f.profA),com,0,25).total());changes.registrar(f.a(f.profA),UUID.randomUUID(),com,body(com,"Responsable asignado corrige"));
        assertEquals(req.toString(),u.requerimiento(f.a(f.profA),req).get("id").toString());assertNull(u.solicitud(f.a(f.headA),sol).get("domicile_region"));assertEquals("DEMO_SEDE_A",u.requerimiento(f.a(f.headA),req).get("site_id"));
    }

    @Test void casoColaboradorDelReqNoConcedeRectificarIngresoCompartido(){
        var com=manual();var q=asumir(distribuir(com));var p=u.colaborar(f.a(f.headA),UUID.randomUUID(),q,new NucleoU2.Colaborar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Objetivo independiente de otra área")).id();u.resolver(f.a(f.headB),UUID.randomUUID(),p,new NucleoU2.ResolverColaboracion(0,"ACEPTADA",f.profB,"Acepta competencia propia"));
        UUID caso=UUID.fromString(d.db.sql.queryForObject("SELECT case_id FROM institution_collaboration WHERE id=?",String.class,p.toString()));assertNotNull(f.core.caso(f.a(f.profB),caso));
        for(String actor:List.of(f.profB,f.headB,f.ti))denied(404,()->changes.consultar(f.a(actor),com,0,25));denied(404,()->changes.constancia(f.a(f.profB),com,1));
    }

    @Test void aliasDeLaMismaPersonaNoHeredaResponsabilidad()throws Exception{
        String alias=f.user("alias.prof.captura");var r=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);var bindings=new ArrayList<>(r.vinculos());bindings.add(new AutoridadesInstitucionales.Vinculo(alias,"p-"+f.profA,f.start,f.end,ConcesionFixture.huella(d.db,alias)));f.importar(new AutoridadesInstitucionales.Registro11("1.1",r.revision()+1,clock.instant(),f.end,bindings,r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea()));
        var com=manual();asumir(distribuir(com));assertEquals(f.f.autoridades.persona(f.profA),f.f.autoridades.persona(alias));denied(404,()->changes.consultar(f.a(alias),com,0,25));assertNotNull(changes.consultar(f.a(f.profA),com,0,25));
    }

    @Test void revisionesConcurrentesNoPierdenCambiosYReintentoNoDuplica()throws Exception{
        var com=manual();var one=body(com,"Primer cambio concurrente");var two=body(com,"Segundo cambio concurrente");var a=f.a(f.profA);var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){var first=pool.submit(()->{gate.await();try{changes.registrar(a,UUID.randomUUID(),com,one);return 200;}catch(FalloTecnico e){return e.status;}});var second=pool.submit(()->{gate.await();try{changes.registrar(a,UUID.randomUUID(),com,two);return 200;}catch(FalloTecnico e){return e.status;}});gate.countDown();var results=new ArrayList<>(List.of(first.get(8,TimeUnit.SECONDS),second.get(8,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);}
        assertEquals(1,changes.consultar(a,com,0,25).total());assertEquals(1,revision(com));
    }

    @Test void falloAuditoriaRevierteCambioRevisionEventoYComando(){
        var com=manual();var key=UUID.randomUUID();var input=body(com,"Corrección transaccional");long events=d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event",Long.class);
        d.ddl(sql->sql.execute("ALTER TABLE institution_flow_history ADD CONSTRAINT rectificacion_rollback CHECK(action_code<>'COM_DATOS_RECTIFICADOS')"));try{assertThrows(org.springframework.dao.DataAccessException.class,()->changes.registrar(f.a(f.profA),key,com,input));}finally{d.ddl(sql->sql.execute("ALTER TABLE institution_flow_history DROP CONSTRAINT rectificacion_rollback"));}
        assertEquals(0,revision(com));assertEquals(0,changes.consultar(f.a(f.profA),com,0,25).total());assertEquals(events,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_event",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,key.toString()));changes.registrar(f.a(f.profA),key,com,input);assertEquals(1,revision(com));
    }

    @Test void datosInvalidosVersionMotivoYNoopNoCreanHistoria(){
        var com=manual();var v=changes.consultar(f.a(f.profA),com,0,25);denied(400,()->changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("1.0",0,v.original(),"No se cambia ningún valor")));
        var input=body(com,"Contenido válido");denied(400,()->changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("2.0",0,input.datos(),"Versión no admitida")));
        denied(400,()->changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("1.0",0,new RectificacionesIngreso.Datos("Dato","CORREO",clock.instant().plusSeconds(1),null),"Fecha imposible")));
        denied(400,()->changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("1.0",0,input.datos()," ")));assertEquals(0,revision(com));assertEquals(0,changes.consultar(f.a(f.profA),com,0,25).total());
    }

    @Test void cadenaPaginadaIndividualYContextosDistintosSeProtegen(){
        var com=manual();changes.registrar(f.a(f.profA),UUID.randomUUID(),com,body(com,"Primera corrección"));var first=changes.consultar(f.a(f.profA),com,0,1).entradas().getFirst();changes.registrar(f.a(f.profA),UUID.randomUUID(),com,body(com,"Segunda corrección"));var page=changes.consultar(f.a(f.profA),com,1,1);assertEquals(2,page.total());assertEquals(first,page.entradas().getFirst());assertEquals(first,changes.constancia(f.a(f.profA),com,first.secuencia()));
        var other=manual();denied(404,()->changes.constancia(f.a(f.profA),other,first.secuencia()));denied(400,()->changes.consultar(f.a(f.profA),com,0,101));
        d.db.sql.update("UPDATE institution_flow_history SET snapshot_doc=? WHERE seq=?",d.mapper.writeValueAsString(new RectificacionesIngreso.Cambio("1.0",new RectificacionesIngreso.Datos("Otro original","TELÉFONO",clock.instant().minusSeconds(60),null),body(com,"Alteración").datos())),first.secuencia());denied(409,()->changes.consultar(f.a(f.profA),com,0,25));
    }

    @Test void respuestaSimpleFinalizadaConservaCierreFechaDeGestionYTareas(){
        var manual=new Manual("Consulta atendida","TELÉFONO",clock.instant().minusSeconds(120),f.a(f.profA).sede(),null);var first=new NucleoU2.PrimeraGestion("1.0",manual,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Orientación entregada","ORIENTACION",clock.instant().minusSeconds(60));var q=u.primeraGestion(f.a(f.profA),UUID.randomUUID(),first).id();var row=u.requerimiento(f.a(f.profA),q);UUID com=UUID.fromString(row.get("communication_id").toString());long tasks=d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_task",Long.class);var result=row.get("primeraGestion");
        changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("1.0",revision(com),new RectificacionesIngreso.Datos("Captura corregida","TELÉFONO",clock.instant().minusSeconds(130),null),"Errata de transcripción"));assertEquals("TERMINADO",u.requerimiento(f.a(f.profA),q).get("state"));assertEquals(result,u.requerimiento(f.a(f.profA),q).get("primeraGestion"));assertEquals(tasks,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_task",Long.class));
        denied(409,()->changes.registrar(f.a(f.profA),UUID.randomUUID(),com,new RectificacionesIngreso.Solicitud("1.0",revision(com),new RectificacionesIngreso.Datos("Fecha contradictoria","TELÉFONO",clock.instant().minusSeconds(30),null),"No admite recepción posterior a respuesta acreditada")));
    }
    @Test void revocacionDeConcesionRetiraConsultaYEscrituraSinHeredarAccesoPorRegistrador(){
        var com=manual();asumir(distribuir(com));changes.registrar(f.a(f.profA),UUID.randomUUID(),com,body(com,"Corrección anterior al retiro"));var old=f.a(f.profA);
        String instruction=d.db.sql.queryForObject("SELECT id FROM access_instruction WHERE beneficiary_id=? AND role_code='PROFESIONAL' AND state='EJECUTADA'",String.class,f.profA);
        f.f.concesiones.revocar(f.f.revisor,UUID.randomUUID(),UUID.fromString(instruction),new ConcesionesInstitucionales.Decision(2,"Revocación de ámbito autorizada"));
        denied(404,()->changes.consultar(f.a(f.profA),com,0,25));denied(403,()->changes.registrar(old,UUID.randomUUID(),com,body(com,"Sesión de alcance anterior")));assertEquals(1,changes.consultar(f.a(f.headA),com,0,25).total());
    }

}
