package cl.nexbyte.siger.institucional.nucleo;

import cl.nexbyte.siger.institucional.identidad.*;
import java.security.*;import java.time.*;import java.util.*;

/** Acreditación independiente sintética 1.1; no agrega accesos con SQL ni elimina gates. */
final class AreasFixture {
 final DatosNucleoTest d;final IdentidadInstitucional ids;final Clock clock;final String ti,headA,headB,profA,profB,legacy,dist;
 final ConcesionFixture.Contexto f;final CatalogoLaboratorio cat;final NucleoInstitucional core;
 final UUID legacyCom,legacyCase,legacyDestination,emptyDestination,legacyDistributionKey,legacyCaseKey;
 final Map<String,String> passwords=new HashMap<>();
 final Instant start=Instant.parse("2000-01-01T00:00:00Z"),end=Instant.parse("2099-01-01T00:00:00Z");
 AreasFixture(DatosNucleoTest d,Clock clock)throws Exception{
  this.d=d;this.clock=clock;ids=new IdentidadInstitucional(d.db,clock);cat=new CatalogoLaboratorio(ids);
  String p=UUID.randomUUID()+"AZ",n=UUID.randomUUID()+"AZ";ti=ids.bootstrap(new IdentidadInstitucional.Crear("ti.areas","TI sintético","ti.areas@example.invalid","Sin facultad de contenido",null,null,p)).id();ids.cambiarClavePersonal((IdentidadInstitucional.Principal)ids.autenticar("ti.areas"),new IdentidadInstitucional.CambioClave(p,n));passwords.put("ti.areas",n);
  headA=user("jefe.a");headB=user("jefe.b");profA=user("prof.a");profB=user("prof.b");legacy=user("jefe.historico");dist=user("distribuidor");
  f=ConcesionFixture.contexto(d.db,ids,ti,clock);core=new NucleoInstitucional(d.db,cat,d.recepcion,d.mapper,clock,"COM",6,f.autoridades);
  ConcesionFixture.otorgar(d.db,ids,ti,legacy,new IdentidadInstitucional.Otorgar("DIRECTOR","UNIDAD_SEDE","UPR","DEMO_SEDE_A",clock.instant().minusSeconds(1),clock.instant().plusSeconds(3600)),clock);
  ConcesionFixture.otorgar(d.db,ids,ti,dist,new IdentidadInstitucional.Otorgar("DISTRIBUCION","INSTITUCION",null,null,clock.instant().minusSeconds(1),clock.instant().plusSeconds(3600)),clock);
  legacyCom=d.nuevaWeb();legacyDistributionKey=UUID.randomUUID();core.distribuir(a(dist),legacyDistributionKey,legacyCom,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",legacy))));legacyDestination=core.detalle(a(legacy),legacyCom).destinos().getFirst().id();legacyCaseKey=UUID.randomUUID();legacyCase=core.abrirCaso(a(legacy),legacyCaseKey,legacyDestination,new ContratosNucleo.Asignacion(0,legacy,null)).id();
  var emptyCom=d.nuevaWeb();core.distribuir(a(dist),UUID.randomUUID(),emptyCom,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",legacy))));emptyDestination=core.detalle(a(legacy),emptyCom).destinos().getFirst().id();
  publicar(end);
  otorgar(headA,"COORDINADOR","AREA_A");otorgar(headB,"COORDINADOR","AREA_B");otorgar(profA,"PROFESIONAL","AREA_A");otorgar(profB,"PROFESIONAL","AREA_B");
 }
 String user(String name){String p=UUID.randomUUID()+"AZ",n=UUID.randomUUID()+"AZ";var a=ids.crear(ti,new IdentidadInstitucional.Crear(name,"Persona sintética "+name,name+"@example.invalid","El cargo no autoriza",null,null,p));ids.cambiarClavePersonal((IdentidadInstitucional.Principal)ids.autenticar(name),new IdentidadInstitucional.CambioClave(p,n));passwords.put(name,n);return a.id();}
 void publicar(Instant areaUntil)throws Exception{
  var mandates=new ArrayList<>(f.mandatos.values());var scopes=new ArrayList<AutoridadesInstitucionales.AmbitoMandato>();
  for(String ar:List.of("AREA_A","AREA_B"))for(String role:List.of("COORDINADOR","PROFESIONAL")){String id=ar+"_"+role;mandates.add(new AutoridadesInstitucionales.Mandato(id,"p-"+f.revisor,"AUTORIDAD_SINTETICA",role,"AREA","UPR","DEMO_SEDE_A",start,areaUntil,true,true,"RESPALDO_SINTETICO","b".repeat(64)));scopes.add(new AutoridadesInstitucionales.AmbitoMandato(id,ar));}
  var bindings=ids.cuentas(ti).stream().filter(IdentidadInstitucional.Cuenta::activo).map(c->new AutoridadesInstitucionales.Vinculo(c.id(),"p-"+c.id(),start,end,ConcesionFixture.huella(d.db,c.id()))).toList();
  long rev=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;
  var r=new AutoridadesInstitucionales.Registro11("1.1",rev,clock.instant(),end,bindings,mandates,List.of(new AutoridadesInstitucionales.Area("AREA_A","UPR","DEMO_SEDE_A","Área sintética A","COORDINACION",start,areaUntil),new AutoridadesInstitucionales.Area("AREA_B","UPR","DEMO_SEDE_A","Área sintética B","DIRECCION",start,areaUntil)),scopes,List.of(new AutoridadesInstitucionales.CambioArea("DELEGACION_A_B","p-"+headA,"DELEGACION_EXPRESA","UPR","DEMO_SEDE_A","AREA_A","AREA_B",start,areaUntil,"RESPALDO_SINTETICO","d".repeat(64)),new AutoridadesInstitucionales.CambioArea("DIRECCION_HISTORICO_A","p-"+legacy,"DIRECCION","UPR","DEMO_SEDE_A",null,"AREA_A",start,areaUntil,"RESPALDO_SINTETICO","e".repeat(64))));
  importar(r);
 }
 void importar(Object r)throws Exception{byte[] payload=d.mapper.writeValueAsBytes(r);var signer=Signature.getInstance("Ed25519");signer.initSign(f.firma.getPrivate());signer.update(AutoridadesInstitucionales.contenidoFirmado(payload));f.autoridades.incorporar(new AutoridadesInstitucionales.Sobre("fixture-autoridad",Base64.getEncoder().encodeToString(payload),Base64.getEncoder().encodeToString(signer.sign())));}
 UUID otorgar(String actor,String role,String area){var s=new ConcesionesInstitucionales.Solicitud(actor,role,"AREA","UPR","DEMO_SEDE_A",clock.instant().minusSeconds(1),clock.instant().plusSeconds(1800),"Respaldo sintético de área","1.1",area);var id=f.concesiones.solicitar(ti,UUID.randomUUID(),s).id();f.concesiones.decidir(f.revisor,UUID.randomUUID(),id,new ConcesionesInstitucionales.Decision(0,"Aprobación independiente sintética"),true);f.concesiones.ejecutar(ti,UUID.randomUUID(),id,new ConcesionesInstitucionales.Ejecucion(1));return id;}
 CatalogoLaboratorio.Operador a(String actor){return cat.operador(actor);}
 UUID caso(String area,String head,String prof){UUID com=d.nuevaWeb();core.distribuir(a(dist),UUID.randomUUID(),com,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",head,area))));UUID dest=core.detalle(a(head),com).destinos().getFirst().id();return core.abrirCaso(a(head),UUID.randomUUID(),dest,new ContratosNucleo.Asignacion(0,prof,null)).id();}
}
