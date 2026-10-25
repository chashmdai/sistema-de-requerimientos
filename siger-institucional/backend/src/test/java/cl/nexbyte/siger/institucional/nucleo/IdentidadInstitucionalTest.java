package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.documental.AutorizacionNucleo;
import java.time.*;import java.util.*;import java.nio.file.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import static cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.*;
class IdentidadInstitucionalTest {
 @TempDir Path temp;DatosNucleoTest d;IdentidadInstitucional ids;CatalogoLaboratorio cat;NucleoInstitucional core;OrganizacionInstitucional org;String ti;
 static String password(){return UUID.randomUUID()+"aZ";}
 @BeforeEach void setup()throws Exception{d=new DatosNucleoTest(temp);clean();ids=new IdentidadInstitucional(d.db,Clock.systemUTC());cat=new CatalogoLaboratorio(ids);core=new NucleoInstitucional(d.db,cat,d.recepcion,d.mapper,Clock.systemUTC());org=new OrganizacionInstitucional(d.db,ids,Clock.systemUTC());ti=ids.bootstrap(new Crear("ti.lab","TI sintético","ti@example.invalid","Soporte",null,null,password())).id();}
 void clean(){for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);d.db.sql.update("DELETE FROM core_unit_site WHERE site_id='LAB_NUEVA'");d.db.sql.update("DELETE FROM org_site_validity WHERE site_id='LAB_NUEVA'");d.db.sql.update("DELETE FROM core_site WHERE id='LAB_NUEVA'");}
 @AfterEach void finish(){clean();d.close();}
 Cuenta user(String name,String unit,String site){return ids.crear(ti,new Crear(name,"Persona "+name,name+"@example.invalid","Cargo declarado",unit,site,password()));}
 String grant(String id,String role,String scope,String unit,String site){return ids.otorgar(ti,id,new Otorgar(role,scope,unit,site,Instant.now().minusSeconds(2),null));}
 CatalogoLaboratorio.Operador a(String id){return cat.operador(id);}
 UUID manual(String actor,String site){return core.registrarManual(a(actor),UUID.randomUUID(),new Manual("Relato sintético individual","TELÉFONO",Instant.now().minusSeconds(10),site,null,"REG_LAB_1")).id();}
 Resultado distribute(String actor,UUID c,String head,String unit,String site){return core.distribuir(a(actor),UUID.randomUUID(),c,new Distribucion(core.detalle(a(actor),c).comunicacion().revision(),List.of(new NuevoDestino(unit,site,head))));}
 void denied(int status,org.junit.jupiter.api.function.Executable call){var e=assertThrows(FalloTecnico.class,call);assertEquals(status,e.status);}
 @Test void cuentasSonIndividualesCargoNoConcedeRolesYBootstrapNoSeRepite(){
  var password=password();var u=ids.crear(ti,new Crear("admin.lab","Persona sintética","admin@example.invalid","Director", "UPR","DEMO_SEDE_A",password));
  var p=(Principal)ids.autenticar("admin.lab");assertTrue(ids.vigente(p));assertTrue(new org.springframework.security.crypto.password.DelegatingPasswordEncoder("bcrypt",Map.of("bcrypt",new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder())).matches(password,p.getPassword()));
  assertFalse(a(u.id()).jefatura());assertFalse(a(u.id()).distribuidor());assertFalse(a(u.id()).ti());assertTrue(a(u.id()).accesos().isEmpty());assertNotEquals(password,d.db.sql.queryForObject("SELECT password_hash FROM staff_account WHERE actor_id=?",String.class,u.id()));
  denied(409,()->ids.bootstrap(new Crear("otro.ti","Otro","otro@example.invalid","TI",null,null,password())));
 }
 @Test void todosActivosRegistranSinInventarSedeYNoAbrenAgrupadores(){
  var u=user("administrativo","UPR","DEMO_SEDE_A");grant(u.id(),"ADMINISTRATIVO","UNIDAD_SEDE","UPR","DEMO_SEDE_A");var c=manual(u.id(),"DEMO_SEDE_A");
  assertEquals("DEMO_SEDE_A",core.detalle(a(u.id()),c).comunicacion().sedeOrigen());assertEquals("TELÉFONO",core.detalle(a(u.id()),c).comunicacion().canalDeclarado());
  denied(403,()->manual(u.id(),"DEMO_SEDE_B"));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_requirement",Integer.class));
  var central=manual(ti,null);assertNull(core.detalle(a(ti),central).comunicacion().sedeOrigen());
 }
 @Test void coordinadorSoloDistribuyeOrigenPropioYPrincipalGlobalSinAutoCaso(){
  var regional=user("regional","UPR","DEMO_SEDE_A");grant(regional.id(),"COORDINADOR_REGIONAL","SEDE",null,"DEMO_SEDE_A");
  var otro=user("otro","UEP","DEMO_SEDE_B");var head=user("jefe","UEP","DEMO_SEDE_B");grant(head.id(),"DIRECTOR","UNIDAD_SEDE","UEP","DEMO_SEDE_B");
  var dist=user("principal",null,null);grant(dist.id(),"DISTRIBUCION","INSTITUCION",null,null);
  var propio=manual(regional.id(),"DEMO_SEDE_A");distribute(regional.id(),propio,head.id(),"UEP","DEMO_SEDE_B");
  var ajeno=manual(otro.id(),"DEMO_SEDE_B");denied(403,()->core.distribuir(a(regional.id()),UUID.randomUUID(),ajeno,new Distribucion(0,List.of(new NuevoDestino("UEP","DEMO_SEDE_B",head.id())))));
  var web=d.nuevaWeb();denied(403,()->core.distribuir(a(regional.id()),UUID.randomUUID(),web,new Distribucion(0,List.of(new NuevoDestino("UEP","DEMO_SEDE_B",head.id())))));
  distribute(dist.id(),ajeno,head.id(),"UEP","DEMO_SEDE_B");distribute(dist.id(),web,head.id(),"UEP","DEMO_SEDE_B");
  assertEquals(3,d.db.sql.queryForObject("SELECT COUNT(*) FROM distribution_event",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_case",Integer.class));assertNull(core.detalle(a(dist.id()),web).comunicacion().sedeOrigen());
 }
 @Test void reintentoDistribucionConservaComDestinoYRevisionYRechazaOtraOperacionDuplicada(){
  var u=user("regional","UPR","DEMO_SEDE_A");grant(u.id(),"COORDINADOR_REGIONAL","SEDE",null,"DEMO_SEDE_A");UUID c=manual(u.id(),"DEMO_SEDE_A"),key=UUID.randomUUID();var body=new Distribucion(0,List.of(new NuevoDestino("UPR","DEMO_SEDE_A",u.id())));
  var r=core.distribuir(a(u.id()),key,c,body);assertEquals(r,core.distribuir(a(u.id()),key,c,body));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_destination",Integer.class));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM distribution_event",Integer.class));
  denied(409,()->core.distribuir(a(u.id()),UUID.randomUUID(),c,new Distribucion(1,body.destinos())));
 }
 @Test void rolesMultiplesNoMultiplicanAlcancesNiDocumentosParaTi(){
  var head=user("jefatura","UPR","DEMO_SEDE_A");grant(head.id(),"COORDINADOR","UNIDAD_SEDE","UPR","DEMO_SEDE_A");grant(head.id(),"PROFESIONAL","UNIDAD_SEDE","UPD","DEMO_SEDE_A");
  assertTrue(a(head.id()).jefeEn("UPR","DEMO_SEDE_A"));assertFalse(a(head.id()).jefeEn("UPD","DEMO_SEDE_A"));assertFalse(a(head.id()).distribuidor());
  var regional=user("regional","UPR","DEMO_SEDE_A");grant(regional.id(),"COORDINADOR_REGIONAL","SEDE",null,"DEMO_SEDE_A");
  var c=manual(regional.id(),"DEMO_SEDE_A");distribute(regional.id(),c,head.id(),"UPR","DEMO_SEDE_A");var dest=core.detalle(a(head.id()),c).destinos().getFirst();var caso=core.abrirCaso(a(head.id()),UUID.randomUUID(),dest.id(),new Asignacion(0,head.id(),null)).id();
  denied(404,()->core.caso(a(ti),caso));denied(404,()->new AutorizacionNucleo(core).ingreso(a(ti),c,null));denied(404,()->new AutorizacionNucleo(core).gestionarCaso(a(ti),caso));
  var other=user("otra.unidad","UPD","DEMO_SEDE_A");grant(other.id(),"COORDINADOR","UNIDAD_SEDE","UPD","DEMO_SEDE_A");denied(404,()->core.caso(a(other.id()),caso));
 }
 @Test void revocacionYDesactivacionInvalidanPrincipalYActorPrevio(){
  var u=user("persona","UPR","DEMO_SEDE_A");var grant=grant(u.id(),"PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A");var p=(Principal)ids.autenticar(u.usuario());var old=a(u.id());ids.revocar(ti,u.id(),grant);assertFalse(ids.vigente(p));denied(403,()->core.buscar(old,"",false,0,20));assertFalse(a(u.id()).profesionalEn("UPR","DEMO_SEDE_A"));
  var current=ids.cuentas(ti).stream().filter(x->x.id().equals(u.id())).findFirst().orElseThrow();p=(Principal)ids.autenticar(u.usuario());ids.actualizar(ti,u.id(),new Perfil(u.nombre(),u.correo(),u.cargo(),u.unidad(),u.sede(),false,current.revision()));assertFalse(ids.vigente(p));denied(401,()->a(u.id()));assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,()->ids.autenticar(u.usuario()));
 }
 @Test void vigenciaNoRequiereNuevoLoginYCargoGenericoNoDistribuye(){
  var u=user("persona","UPR","DEMO_SEDE_A");ids.otorgar(ti,u.id(),new Otorgar("DIRECTOR","UNIDAD_SEDE","UPR","DEMO_SEDE_A",Instant.now().plusSeconds(600),null));assertFalse(a(u.id()).jefatura());assertFalse(a(u.id()).distribuidor());
  denied(400,()->grant(u.id(),"TI","SEDE",null,"DEMO_SEDE_A"));denied(400,()->grant(u.id(),"PROFESIONAL","INSTITUCION",null,null));
 }
 @Test void dosRegionesCoberturaHistoricaYNuevaOficinaSonDatos(){
  Instant inicio=Instant.now().minusSeconds(3600),cambio=Instant.now().minusSeconds(30);
  var r1=org.cubrir(ti,new OrganizacionInstitucional.Cobertura("REG_LAB_1","DEMO_SEDE_A",inicio,null));org.cubrir(ti,new OrganizacionInstitucional.Cobertura("REG_LAB_2","DEMO_SEDE_A",inicio,null));
  var u=user("persona","UPR","DEMO_SEDE_A");var c=manual(u.id(),"DEMO_SEDE_A");org.terminarCobertura(ti,r1,cambio);org.cubrir(ti,new OrganizacionInstitucional.Cobertura("REG_LAB_1","DEMO_SEDE_B",cambio,null));
  assertEquals("DEMO_SEDE_A",core.detalle(a(u.id()),c).comunicacion().sedeOrigen());assertEquals(3,d.db.sql.queryForObject("SELECT COUNT(*) FROM org_coverage",Integer.class));
  org.oficina(ti,new OrganizacionInstitucional.Oficina("LAB_NUEVA","Nueva sede sintética",List.of("UPR","UPD"),inicio,null));assertTrue(cat.sedes().stream().anyMatch(x->x.id().equals("LAB_NUEVA")));
  denied(409,()->org.cubrir(ti,new OrganizacionInstitucional.Cobertura("REG_LAB_1","DEMO_SEDE_A",inicio,null)));
 }
 @Test void tiNoPuedeRetirarUltimoAccesoAdministrativoNiFuncionarioAutoadministrar(){
  var account=ids.cuentas(ti).getFirst();var g=account.asignaciones().getFirst().get("id").toString();denied(409,()->ids.revocar(ti,ti,g));assertTrue(a(ti).ti());
  denied(409,()->ids.actualizar(ti,ti,new Perfil(account.nombre(),account.correo(),account.cargo(),null,null,false,account.revision())));
  var u=user("persona",null,null);denied(403,()->ids.otorgar(u.id(),u.id(),new Otorgar("TI","INSTITUCION",null,null,Instant.now(),null)));
 }
 @Test void falloPersistenciaNoDejaCuentaNiPerfilParcial(){
  d.ddl(sql->sql.execute("ALTER TABLE staff_account ADD CONSTRAINT test_block_account CHECK (username<>'bloqueado')"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->user("bloqueado",null,null));assertEquals(1,d.db.sql.queryForObject("SELECT COUNT(*) FROM staff_account",Integer.class));assertEquals(0,d.db.sql.queryForObject("SELECT COUNT(*) FROM core_actor WHERE label='Persona bloqueado'",Integer.class));}
  finally{d.ddl(sql->sql.execute("ALTER TABLE staff_account DROP CONSTRAINT test_block_account"));}
 }
}
