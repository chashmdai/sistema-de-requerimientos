package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.notificaciones.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.net.URI;import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;

class AvisosNucleoTest {
    @TempDir Path temp;DatosNucleoTest d;AreasFixture f;NucleoU2 u;NotificacionesInstitucionales n;
    final AreasInstitucionalesTest.Reloj clock=new AreasInstitucionalesTest.Reloj();
    @BeforeEach void setup()throws Exception{String mysql=System.getProperty("siger.test.autoridad.mysql");d=new DatosNucleoTest(temp,mysql==null?null:mysql+"_materia?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000");if(mysql!=null)limpiar();f=new AreasFixture(d,clock);u=f.core.u2();n=new NotificacionesInstitucionales(d.db,f.cat,new RecursosNotificacion(d.db,f.cat,f.core),clock);}
    void limpiar(){d.limpiar();for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);}
    @AfterEach void close(){if(d!=null){if(System.getProperty("siger.test.autoridad.mysql")!=null)limpiar();d.close();}}
    UUID sol(){return u.distribuir(f.a(f.dist),UUID.randomUUID(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación con antecedente sensible reservado al recurso",null)).id();}
    UUID req(UUID sol){return u.asumir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Asumir(0,f.profA,"Asigna la atención profesional",null)).id();}
    List<Map<String,Object>> rows(String tipo,UUID recurso){return d.db.sql.queryForList("SELECT * FROM institution_notification WHERE resource_type='AVISO_U2' AND category=? AND resource_id=? ORDER BY recipient_id",tipo,recurso.toString());}
    Set<String> recipients(String tipo,UUID recurso){var result=new HashSet<String>();rows(tipo,recurso).forEach(r->result.add(r.get("recipient_id").toString()));return result;}
    UUID notice(String tipo,UUID recurso,String actor){return UUID.fromString(rows(tipo,recurso).stream().filter(r->r.get("recipient_id").equals(actor)).findFirst().orElseThrow().get("id").toString());}
    void denied(int code,org.junit.jupiter.api.function.Executable work){assertEquals(code,assertThrows(FalloTecnico.class,work).status);}
    void revocar(String actor,String role){var rows=d.db.sql.queryForList("SELECT id,revision FROM access_instruction WHERE beneficiary_id=? AND role_code=? AND state='EJECUTADA'",actor,role);for(var r:rows)f.f.concesiones.revocar(f.f.revisor,UUID.randomUUID(),UUID.fromString(r.get("id").toString()),new ConcesionesInstitucionales.Decision(((Number)r.get("revision")).longValue(),"Revocación sintética independiente"));}

    @Test void solDistribuidaAvisaSoloJefaturaYAsignadorConDosCanalesDurables(){
        var sol=sol();n.proyectar();assertEquals(Set.of(f.headA,f.dist),recipients("SOL_DISTRIBUIDA",sol));
        for(var r:rows("SOL_DISTRIBUIDA",sol))assertEquals("PENDIENTE",d.db.sql.queryForObject("SELECT state FROM notification_outbox WHERE notification_id=?",String.class,r.get("id")));
        UUID id=notice("SOL_DISTRIBUIDA",sol,f.headA);var view=n.contextoInstitucional(f.a(f.headA),id);assertEquals("sol/"+sol,view.ruta());assertEquals("AREA_A",view.destino().area());assertTrue(view.pendienteActual());
        for(String actor:List.of(f.ti,f.profA,f.headB,f.legacy))denied(404,()->n.contextoInstitucional(f.a(actor),id));
        assertTrue(n.listar(f.a(f.headA),0,50).items().stream().anyMatch(x->x.id().equals(id)&&x.ruta().equals("aviso-institucional/"+id)));
        n.leer(f.a(f.headA),id);assertEquals("PENDIENTE",u.solicitud(f.a(f.headA),sol).get("state"));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_req",Long.class));
        n.proyectar();assertEquals(2,rows("SOL_DISTRIBUIDA",sol).size());
    }

    @Test void redireccionDaPaqueteMinimoAlOrigenSinRecuperarSolActual(){
        var sol=sol();n.proyectar();UUID old=notice("SOL_DISTRIBUIDA",sol,f.headA);
        u.redirigir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Redirección fundada por competencia",null));n.proyectar();
        assertEquals(Set.of(f.headA,f.headB,f.dist),recipients("SOL_REDIRIGIDA",sol));denied(404,()->u.solicitud(f.a(f.headA),sol));
        var origin=n.contextoInstitucional(f.a(f.headA),notice("SOL_REDIRIGIDA",sol,f.headA));assertNull(origin.ruta());assertEquals("AREA_A",origin.origen().area());assertEquals("AREA_B",origin.destino().area());
        String visible=d.mapper.writeValueAsString(origin);assertFalse(visible.contains("Clasificación con antecedente sensible"));assertFalse(visible.contains("Relato sintético"));assertFalse(visible.contains("casos"));
        assertEquals("sol/"+sol,n.contextoInstitucional(f.a(f.headB),notice("SOL_REDIRIGIDA",sol,f.headB)).ruta());assertFalse(n.enviable(f.a(f.headA),n.propia(f.a(f.headA),old)));
    }

    @Test void proyeccionTardiaNoAtribuyeAvisoAnteriorAlNuevoDestino(){
        var sol=sol();u.redirigir(f.a(f.headA),UUID.randomUUID(),sol,new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Corrige el destino sin asumir",null));n.proyectar();
        assertTrue(rows("SOL_DISTRIBUIDA",sol).isEmpty());assertEquals(Set.of(f.headA,f.headB,f.dist),recipients("SOL_REDIRIGIDA",sol));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_req",Long.class));
    }

    @Test void reqAsumidoSinCasoAvisaEncargadoYJefaturaSinAcuseAdicional(){
        var sol=sol();var q=req(sol);n.proyectar();assertEquals(Set.of(f.profA,f.headA),recipients("REQ_ASIGNADO",q));assertTrue(rows("SOL_DISTRIBUIDA",sol).isEmpty());var id=notice("REQ_ASIGNADO",q,f.profA);
        assertEquals("requerimiento/"+q,n.contextoInstitucional(f.a(f.profA),id).ruta());n.leer(f.a(f.profA),id);assertEquals(0,((Number)u.requerimiento(f.a(f.profA),q).get("revision")).longValue());assertEquals(true,u.requerimiento(f.a(f.profA),q).get("sinCasos"));
        assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_assignment_receipt",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_task",Long.class));
    }

    @Test void casoAbiertoAntesDeProyectarSuprimeAsignacionSinCasoObsoleta(){
        var q=req(sol());u.abrirCaso(f.a(f.headA),UUID.randomUUID(),q,new NucleoU2.Apertura(0,f.profA,"Objetivo propio"));n.proyectar();assertTrue(rows("REQ_ASIGNADO",q).isEmpty());assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_case",Long.class));
    }

    @Test void sinDestinatarioNoPierdeFuenteNiBloqueaOtrasYReanudaSinDuplicar(){
        var first=sol();var second=u.distribuir(f.a(f.dist),UUID.randomUUID(),d.nuevaWeb(),new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headB,"AREA_B"),"Otro destino competente",null)).id();
        revocar(f.headA,"COORDINADOR");revocar(f.dist,"DISTRIBUCION");n.proyectar();assertTrue(rows("SOL_DISTRIBUIDA",first).isEmpty());assertEquals(Set.of(f.headB),recipients("SOL_DISTRIBUIDA",second));
        assertEquals("SIN_DESTINATARIO_VIGENTE",f.core.avisosNucleo().enrutamiento(first).getFirst().estado());f.otorgar(f.headA,"COORDINADOR","AREA_A");n.proyectar();assertEquals(Set.of(f.headA),recipients("SOL_DISTRIBUIDA",first));assertEquals("PROYECTADO",f.core.avisosNucleo().enrutamiento(first).getFirst().estado());n.proyectar();assertEquals(1,rows("SOL_DISTRIBUIDA",first).size());
    }

    @Test void revocacionActualRetiraDetalleConteosYEnvio(){
        var q=req(sol());n.proyectar();UUID id=notice("REQ_ASIGNADO",q,f.profA);revocar(f.profA,"PROFESIONAL");denied(404,()->n.contextoInstitucional(f.a(f.profA),id));assertTrue(n.listar(f.a(f.profA),0,50).items().stream().noneMatch(x->x.id().equals(id)));assertEquals(0,n.pendientes(f.a(f.profA)));
    }

    @Test void proyeccionesConcurrentesSonIdempotentes()throws Exception{
        var sol=sol();var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->{gate.await();return n.proyectar();});var two=pool.submit(()->{gate.await();return n.proyectar();});gate.countDown();one.get(10,TimeUnit.SECONDS);two.get(10,TimeUnit.SECONDS);}
        assertEquals(Set.of(f.headA,f.dist),recipients("SOL_DISTRIBUIDA",sol));assertEquals(2,rows("SOL_DISTRIBUIDA",sol).size());
    }

    @Test void falloDeIntencionRevierteDistribucionPeroConservaComYPermiteReintento(){
        var com=d.nuevaWeb();var key=UUID.randomUUID();var body=new NucleoU2.Encaminar(0,new NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"),"Clasificación atómica",null);long destinations=d.db.sql.queryForObject("SELECT COUNT(*) FROM core_destination",Long.class);
        d.ddl(sql->sql.execute("ALTER TABLE workflow_event ADD CONSTRAINT aviso_u2_rollback CHECK(resource_type<>'AVISO_U2')"));try{assertThrows(org.springframework.dao.DataAccessException.class,()->u.distribuir(f.a(f.dist),key,com,body));}finally{d.ddl(sql->sql.execute("ALTER TABLE workflow_event DROP CONSTRAINT aviso_u2_rollback"));}
        assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_sol",Long.class));assertEquals(destinations,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_destination",Long.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_command WHERE id=?",Long.class,key.toString()));
        var result=u.distribuir(f.a(f.dist),key,com,body);assertEquals(result,u.distribuir(f.a(f.dist),key,com,body));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM workflow_event WHERE resource_id=? AND resource_type='AVISO_U2'",Long.class,result.id().toString()));
    }

    @Test void correoSimuladoTieneEnlaceAutenticadoMinimoSinRelatoYSinDuplicar(){
        var sol=sol();n.proyectar();UUID id=notice("SOL_DISTRIBUIDA",sol,f.headA);var messages=new ArrayList<TransporteCorreo.Mensaje>();
        var transport=new TransporteCorreo(){public Resultado enviar(Mensaje m){messages.add(m);return new Resultado("SIMULADO","SIMULADO",null,0,null);}public boolean real(){return false;}};
        var mail=new CorreosInstitucionales(d.db,f.cat,n,transport,Set.of("jefe.a@example.invalid"),clock,"u2_sintetico",r->{},URI.create("https://siger.example.invalid/"));
        assertEquals("SIMULADO",mail.enviar(f.a(f.headA),id).estado());mail.enviar(f.a(f.headA),id);assertEquals(1,messages.size());assertTrue(messages.getFirst().texto().contains("https://siger.example.invalid/#aviso-institucional/"+id));assertFalse(messages.getFirst().texto().contains("Clasificación con antecedente"));assertFalse(messages.getFirst().texto().contains("Relato sintético"));assertEquals("PENDIENTE",u.solicitud(f.a(f.headA),sol).get("state"));
    }

    @Test void transporteQueDeclaraRealSinBaseHttpsNoLlamaProveedor(){
        var sol=sol();n.proyectar();UUID id=notice("SOL_DISTRIBUIDA",sol,f.headA);var transport=new TransporteCorreo(){public Resultado enviar(Mensaje m){fail("Nunca debe llamar proveedor");return null;}public boolean real(){return true;}};
        var mail=new CorreosInstitucionales(d.db,f.cat,n,transport,Set.of("jefe.a@example.invalid"),clock,"u2_guardia");denied(409,()->mail.enviar(f.a(f.headA),id));assertEquals("PENDIENTE",mail.consultar(f.a(f.headA),id).estado());assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM mail_attempt",Long.class));
        assertThrows(IllegalArgumentException.class,()->new CorreosInstitucionales(d.db,f.cat,n,transport,Set.of("jefe.a@example.invalid"),clock,"u2_guardia",r->{},URI.create("http://siger.example.invalid/")));
    }
    @Test void cuentaAliasConMismaPersonaYRolNoHeredaNotificacionNominativa()throws Exception{
        var sol=sol();n.proyectar();UUID original=notice("SOL_DISTRIBUIDA",sol,f.headA);String alias=f.user("alias.jefatura.aviso");var r=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);var bindings=new ArrayList<>(r.vinculos());bindings.add(new AutoridadesInstitucionales.Vinculo(alias,"p-"+f.headA,f.start,f.end,ConcesionFixture.huella(d.db,alias)));f.importar(new AutoridadesInstitucionales.Registro11("1.1",r.revision()+1,clock.instant(),f.end,bindings,r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea()));f.otorgar(alias,"COORDINADOR","AREA_A");
        assertEquals(f.f.autoridades.persona(f.headA),f.f.autoridades.persona(alias));assertTrue(f.a(alias).jefeEn("UPR","DEMO_SEDE_A","AREA_A"));denied(404,()->n.contextoInstitucional(f.a(alias),original));assertTrue(n.listar(f.a(alias),0,50).items().isEmpty());
    }

}
