package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.identidad.*;
import cl.nexbyte.siger.institucional.documental.*;
import cl.nexbyte.siger.institucional.formulario.*;
import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;
final class ContenidoFixture implements AutoCloseable {
 final DatosNucleoTest d;final MateriasFixture m;final AreasFixture f;final Clock clock;final ArchivoInstitucional files;final DocumentosGestion docs;
 final AutoridadesInstitucionales.Registro12 base;final Map<UUID,AutoridadesInstitucionales.PrincipalReq> principales=new LinkedHashMap<>();
 ContenidoFixture(Path temp,Clock clock,String url)throws Exception{
  this.clock=clock;d=new DatosNucleoTest(temp,url);m=new MateriasFixture(d,clock);f=m.f;
  f.otorgar(f.headA,"COORDINADOR","AREA_B");f.otorgar(f.profA,"PROFESIONAL","AREA_B");f.otorgar(f.profB,"PROFESIONAL","AREA_A");
  base=d.mapper.readValue(Base64.getDecoder().decode(d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro12.class);
  files=new ArchivoInstitucional(temp.resolve("custodia"));
  docs=new DocumentosGestion(d.db,files,new AutorizacionNucleo(f.core),p->true,(p,e)->new ProcesamientoDocumental.Examen(FormatosDocumento.TIPOS.get(e),"DESCARGA",null),clock);
 }
 UUID[] par()throws Exception{
  UUID a=m.caso();m.clasificar(a,0,"MAT_UPR",true);var c=f.core.caso(f.a(f.headA),a);UUID com=c.comunicacionId();
  var dest=f.core.detalle(f.a(f.headA),com).destinos().getFirst();
  UUID b=f.core.abrirCaso(f.a(f.headA),UUID.randomUUID(),dest.id(),new ContratosNucleo.Asignacion(dest.revision(),f.profB,null)).id();
  f.core.cambiarArea(f.a(f.headA),UUID.randomUUID(),b,new ContratosNucleo.CambiarArea(0,"AREA_B","Ámbito sintético de línea independiente","AREA_A"));m.clasificar(b,1,"MAT_UPD",true);
  UUID req=c.requerimientoId();acreditar(req,"ACTIVA",f.end);UUID child=d.nuevaWeb();
  f.core.vincular(f.a(f.dist),UUID.randomUUID(),child,new ContratosNucleo.Vinculacion(com,null,"Propuesta sintética sin Caso"));
  return new UUID[]{a,b,req,child};
 }
 void acreditar(UUID req,String estado,Instant hasta)throws Exception{
  principales.put(req,new AutoridadesInstitucionales.PrincipalReq(req,"UPR","DEMO_SEDE_A","AREA_A",f.start,hasta,estado,"RESPALDO_SINTETICO_REQ","a".repeat(64)));
  long rev=d.db.sql.queryForObject("SELECT revision FROM authority_registry WHERE id=1",Long.class)+1;
  f.importar(new AutoridadesInstitucionales.Registro13("1.3",rev,clock.instant(),base.expira(),base.vinculos(),base.mandatos(),base.areas(),base.ambitos(),base.cambiosArea(),base.materias(),base.sedesRegionales(),base.ambitosMateria(),List.copyOf(principales.values())));
 }
 DocumentosGestion.Documento cargar(UUID caso){return docs.cargar(f.a(f.profA),UUID.randomUUID(),caso,null,"sintetico.txt",new ByteArrayInputStream("Antecedente sintético".getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
 ContenidoInstitucional c(){return f.core.contenidos();}
 void ordinario(UUID caso,UUID doc,long rev){c().proteger(f.a(f.headA),UUID.randomUUID(),caso,doc,new ContenidoInstitucional.Proteccion(rev,"ORDINARIO","Clasificación expresa ordinaria"));}
 ContenidoInstitucional.Publicacion publicacion(long rev,boolean contenido,boolean descarga,String estado){return new ContenidoInstitucional.Publicacion(rev,clock.instant().minusSeconds(1),clock.instant().plusSeconds(1200),contenido,descarga,estado,"Publicación sintética por recurso");}
 public void close()throws Exception{files.close();d.close();}
}

