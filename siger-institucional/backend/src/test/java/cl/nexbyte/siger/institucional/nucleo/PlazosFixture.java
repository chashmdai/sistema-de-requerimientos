package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.identidad.CatalogoPlazos.*;
import java.nio.file.*;import java.time.*;import java.util.*;
final class PlazosFixture implements AutoCloseable {
 final DocumentosReqFixture x;final PlazosRegistro service;
 PlazosFixture(Path p)throws Exception{this(p,null,"_plazos");}
 PlazosFixture(Path p,String target,String suffix)throws Exception{x=new DocumentosReqFixture(p,target,suffix);var cuenta=x.f.ids.cuentas(x.f.ti).stream().filter(a->a.id().equals(x.f.profA)).findFirst().orElseThrow();x.f.ids.actualizar(x.f.ti,x.f.profA,new IdentidadInstitucional.Perfil(cuenta.nombre(),cuenta.correo(),cuenta.cargo(),"UPR","DEMO_SEDE_A",true,cuenta.revision()));service=new PlazosRegistro(x.d.db,x.f.core,x.f.cat,x.f.f.autoridades,x.clock);}
 tools.jackson.databind.node.ObjectNode registro(List<Calendario> calendars,List<Politica> policies){
  var n=(tools.jackson.databind.node.ObjectNode)x.d.mapper.readTree(Base64.getDecoder().decode(x.d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class))).deepCopy();n.put("version","1.10");n.put("revision",x.d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1);
  for(String key:List.of("materias","sedesRegionales","ambitosMateria","principales","funciones","autorizantesCierre","autorizantesReserva","autorizantesDocumentoAislado","coberturas","supervisoresClasificacion"))if(!n.has(key))n.set(key,n.arrayNode());n.set("calendarios",x.d.mapper.valueToTree(calendars));n.set("plazos",x.d.mapper.valueToTree(policies));return n;
 }
 void instalar(List<Calendario> calendars,List<Politica> policies)throws Exception{x.f.importar(registro(calendars,policies));}
 Politica politica(String objetivo,String modo,String estado){return new Politica("TEST_"+objetivo,"v1",objetivo,modo,172800,modo.equals("HABILES")?"TEST_CAL":null,modo.equals("HABILES")?"v1":null,"INSTITUCION",null,null,null,null,x.clock.instant().minus(Duration.ofDays(30)),x.clock.instant().plusSeconds(3600),estado,"RESPALDO_SINTETICO_TIEMPO","a".repeat(64));}
 Calendario calendario(){return new Calendario("TEST_CAL","v1","America/Santiago",LocalDate.parse("2026-09-01"),LocalDate.parse("2026-12-01"),java.util.stream.IntStream.rangeClosed(1,5).mapToObj(i->new Franja(i,"09:00","17:00")).toList(),List.of(),"CALENDARIO_SINTETICO","b".repeat(64));}
 UUID manual(long antiguedadMs){return x.f.core.registrarManual(x.f.a(x.f.profA),x.key(),new ContratosNucleo.Manual("Atención telefónica completamente sintética","TELÉFONO",x.clock.instant().minusMillis(antiguedadMs),"DEMO_SEDE_A",null)).id();}
 PlazosRegistro.Entrada entrada(String tipo,UUID recurso){return service.listar(x.f.a(x.f.profA),tipo,recurso,0,25).items().getFirst();}
 PlazosRegistro.Solicitud solicitud(PlazosRegistro.Entrada e){var r=e.regla();return new PlazosRegistro.Solicitud(e.id(),e.huella(),r.id(),r.version(),r.huella(),r.huellaCalendario());}
 long count(String t){return x.d.db.sql.queryForObject("SELECT COUNT(*) FROM "+t,Long.class);}
 public void close()throws Exception{x.close();}
}
