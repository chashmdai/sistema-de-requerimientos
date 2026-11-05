package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.*;import java.time.*;import java.util.*;
final class MateriasFixture {
 final AreasFixture f;final DatosNucleoTest d;final Clock clock;final String upr,upd;final AutoridadesInstitucionales.Registro11 common;
 MateriasFixture(DatosNucleoTest d,Clock clock)throws Exception{
  this.d=d;this.clock=clock;f=new AreasFixture(d,clock);upr=f.user("direccion.upr");upd=f.user("direccion.upd");
  ConcesionFixture.otorgar(d.db,f.ids,f.ti,f.headA,new IdentidadInstitucional.Otorgar("COORDINADOR_REGIONAL","SEDE",null,"DEMO_SEDE_A",clock.instant().minusSeconds(1),clock.instant().plusSeconds(3600)),clock);f.publicar(f.end);
  common=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);publicar("ACTIVA",f.end);
  otorgar(upr,"MAT_UPR");otorgar(upd,"MAT_UPD");
 }
 void publicar(String estado,Instant hasta)throws Exception{
  var m=new ArrayList<>(common.mandatos());var scopes=new ArrayList<AutoridadesInstitucionales.AmbitoMateria>();for(String unit:List.of("UPR","UPD")){String id="LECTURA_"+unit;boolean activa=unit.equals("UPD")||estado.equals("ACTIVA");{m.add(new AutoridadesInstitucionales.Mandato(id,"p-"+f.f.revisor,"DIRECCION_SINTETICA", "DIRECTOR","MATERIA",unit,"DEMO_SEDE_A",f.start,hasta,activa,true,"RESPALDO_SINTETICO","e".repeat(64)));scopes.add(new AutoridadesInstitucionales.AmbitoMateria(id,"MAT_"+unit));}}
  long rev=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;
  f.importar(new AutoridadesInstitucionales.Registro12("1.2",rev,clock.instant(),f.end,common.vinculos(),m,common.areas(),common.ambitos(),common.cambiosArea(),List.of(new CatalogoMaterias.Materia("MAT_UPR","Materia sintética UPR","UPR",f.start,hasta,estado),new CatalogoMaterias.Materia("MAT_UPD","Materia sintética UPD","UPD",f.start,hasta,"ACTIVA")),List.of(new CatalogoMaterias.SedeRegional("DEMO_SEDE_A",f.start,hasta,"ACTIVA")),scopes));
 }
 UUID otorgar(String actor,String materia){String unit=materia.equals("MAT_UPR")?"UPR":"UPD";var id=f.f.concesiones.solicitar(f.ti,UUID.randomUUID(),new ConcesionesInstitucionales.Solicitud(actor,"DIRECTOR","MATERIA",unit,"DEMO_SEDE_A",clock.instant().minusSeconds(1),clock.instant().plusSeconds(1800),"Consulta sintética acreditada","1.2",null,materia)).id();f.f.concesiones.decidir(f.f.revisor,UUID.randomUUID(),id,new ConcesionesInstitucionales.Decision(0,"Aprobación independiente de lectura"),true);f.f.concesiones.ejecutar(f.ti,UUID.randomUUID(),id,new ConcesionesInstitucionales.Ejecucion(1));return id;}
 UUID caso(){return f.caso("AREA_A",f.headA,f.profA);}
 void clasificar(UUID c,long revision,String materia,boolean ordinary){f.core.rectificarMateria(f.a(f.headA),UUID.randomUUID(),c,new ContratosNucleo.RectificarMateria(revision,materia,ordinary,"Decisión regional sintética"));}
}
