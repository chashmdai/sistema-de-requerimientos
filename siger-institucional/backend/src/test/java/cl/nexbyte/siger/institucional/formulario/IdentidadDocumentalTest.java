package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.time.*;import java.nio.file.*;import java.util.*;import java.io.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class IdentidadDocumentalTest {
 @TempDir Path temp;DatosDocumentalesTest fixture;IdentidadInstitucional ids;CatalogoLaboratorio cat;NucleoInstitucional core;AutorizacionNucleo policy;DocumentosGestion docs;String ti,prof,head,other;UUID doc;
 String key(){return UUID.randomUUID()+"aZ";}
 @BeforeEach void start()throws Exception{
  fixture=new DatosDocumentalesTest(temp);var d=fixture.d;for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))d.db.sql.update("DELETE FROM "+t);
  ids=new IdentidadInstitucional(d.db,Clock.systemUTC());cat=new CatalogoLaboratorio(ids);core=new NucleoInstitucional(d.db,cat,d.receiver,d.mapper,Clock.systemUTC());policy=new AutorizacionNucleo(core);
  ti=ids.bootstrap(new IdentidadInstitucional.Crear("ti.docs","TI sintético","ti.docs@example.invalid","TI",null,null,key())).id();
  prof=create("prof","UPR","PROFESIONAL");head=create("head","UPR","COORDINADOR");other=create("other","UPD","COORDINADOR");
  d.db.sql.update("UPDATE core_case SET responsible_id=? WHERE id=?",prof,fixture.uno.toString());
  docs=new DocumentosGestion(d.db,d.files,policy,p->true,(p,e)->new ProcesamientoDocumental.Examen("application/pdf","DESCARGA",null),d.clock);
  doc=docs.cargar(cat.operador(prof),UUID.randomUUID(),fixture.uno,null,"documento.pdf",new ByteArrayInputStream(fixture.contenido)).id();
 }
 String create(String name,String unit,String role){var c=ids.crear(ti,new IdentidadInstitucional.Crear(name+".docs",name+" sintético",name+".docs@example.invalid","Cargo",unit,"DEMO_SEDE_A",key()));ids.otorgar(ti,c.id(),new IdentidadInstitucional.Otorgar(role,"UNIDAD_SEDE",unit,"DEMO_SEDE_A",Instant.now().minusSeconds(5),null));return c.id();}
 @AfterEach void end()throws Exception{for(var t:List.of("staff_profile_history","identity_event","staff_assignment","staff_account","org_coverage"))fixture.d.db.sql.update("DELETE FROM "+t);fixture.close();}
 void denied(org.junit.jupiter.api.function.Executable e){assertEquals(404,assertThrows(FalloTecnico.class,e).status);}
 @Test void tiYCargoAjenoNoLeenNiGestionanOriginalesPorUuid(){
  denied(()->docs.listar(cat.operador(ti),fixture.uno));denied(()->docs.listar(cat.operador(other),fixture.uno));
  denied(()->docs.referencia(cat.operador(other),fixture.uno,doc));denied(()->policy.gestionarCaso(cat.operador(ti),fixture.uno));
  assertEquals(doc,docs.listar(cat.operador(head),fixture.uno).getFirst().id());
  denied(()->docs.referencia(cat.operador(prof),fixture.dos,doc));
 }
 @Test void revocarImpideConsultaDocumentalConIdentidadAntesValida(){
  var old=cat.operador(prof);var grant=ids.cuentas(ti).stream().filter(a->a.id().equals(prof)).findFirst().orElseThrow().asignaciones().getFirst().get("id").toString();ids.revocar(ti,prof,grant);
  assertEquals(403,assertThrows(FalloTecnico.class,()->docs.listar(old,fixture.uno)).status);denied(()->docs.listar(cat.operador(prof),fixture.uno));
  assertEquals(1,fixture.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_document WHERE id=?",Integer.class,doc.toString()));
 }
 @Test void rolAdicionalNoMezclaUnidadProfesionalConFacultadDeJefatura(){
  ids.otorgar(ti,other,new IdentidadInstitucional.Otorgar("PROFESIONAL","UNIDAD_SEDE","UPR","DEMO_SEDE_A",Instant.now().minusSeconds(5),null));
  denied(()->docs.listar(cat.operador(other),fixture.uno));assertEquals(doc,docs.listar(cat.operador(prof),fixture.uno).getFirst().id());
 }
}
