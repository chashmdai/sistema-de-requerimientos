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
 record Original(UUID comunicacion,UUID documento,UUID caso,EntregaCompleta entrega,byte[] bytes){}
 Original original()throws Exception{
  Instant received=clock.instant();
  byte[] bytes="PDF-SINTETICO-CIUDADANO".getBytes(java.nio.charset.StandardCharsets.UTF_8);var attachment=new EntregaCompleta.Adjunto(UUID.randomUUID(),"anexo-reservable.pdf",bytes.length,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)),"application/pdf","VALIDADO");
  var c=d.mapper.createObjectNode();c.put("version","1.1");c.put("id",UUID.randomUUID().toString());c.put("recorridoId",UUID.randomUUID().toString());c.put("tipo","NNA");c.put("recibidaEn",received.toString());c.put("avisoVersion","SINTETICO_1");c.put("aceptaCanalDigital",false);
  var data=c.putObject("datos");data.putObject("campos").put("nna-story","Relato ciudadano sintético").put("motivo","consulta");data.putArray("personas");data.putArray("instituciones");data.putArray("adjuntos").add(attachment.id().toString());c.putArray("adjuntos").add(d.mapper.valueToTree(attachment));
  var p=c.putArray("personas").addObject();p.put("id","presentador-nna");p.put("rol","PRESENTADOR");p.put("autenticada",false);p.put("modulo11",false);p.putNull("runNumero");p.putNull("runDv");p.put("estadoConsulta","NO_HABILITADO");p.putNull("consultadaEn");p.putNull("consultaRef");p.putObject("discrepancias");var attrs=p.putObject("atributos");
  for(String field:List.of("primerNombre","segundoNombre","primerApellido","segundoApellido")){var a=attrs.putObject(field);a.put("valor","NOMBRE SINTETICO");a.putNull("codigo");a.put("fuente","DECLARACION_CIUDADANA");a.put("nivel","DECLARADO_NO_CONTRASTADO");a.put("observadoEn",received.toString());a.put("evidencia",UUID.randomUUID().toString());}
  var sources=d.mapper.createObjectNode();sources.set("presentador-nna",d.mapper.createArrayNode().add(p.path("atributos")).add(p.path("discrepancias")));
  String hash=EntregaCompleta.huella("CONTENIDO",c,d.mapper),provenance=EntregaCompleta.huella("PROCEDENCIA",sources,d.mapper);
  var proof=new cl.nexbyte.siger.institucional.robustez.ComprobanteDigital.Contenido("1.0",UUID.fromString(c.path("id").asString()),received,"CUSTODIA_PUBLICA",hash,provenance,"FORMULARIO_PUBLICO_DDN","SINTETICO_1");
  var e=new EntregaCompleta(c,hash,provenance,cl.nexbyte.siger.institucional.robustez.ComprobanteDigital.firmar(proof,"nucleo-test",d.publicKeys.getPrivate(),d.mapper));
  files.guardar(e.id(),attachment,new ByteArrayInputStream(bytes));UUID com=d.recepcion.guardarCompleta(e,files).registro().comunicacionId();
  f.core.distribuir(f.a(f.dist),UUID.randomUUID(),com,new ContratosNucleo.Distribucion(0,List.of(new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",f.headA,"AREA_A"))));var dest=f.core.detalle(f.a(f.headA),com).destinos().getFirst();UUID caso=f.core.abrirCaso(f.a(f.headA),UUID.randomUUID(),dest.id(),new ContratosNucleo.Asignacion(dest.revision(),f.profA,null)).id();m.clasificar(caso,0,"MAT_UPR",true);acreditar(f.core.caso(f.a(f.headA),caso).requerimientoId(),"ACTIVA",f.end);
  return new Original(com,UUID.fromString(d.db.sql.queryForObject("SELECT id FROM institution_document WHERE communication_id=? AND origin='INGRESO_CIUDADANO'",String.class,com.toString())),caso,e,bytes);
 }
 void ordinario(UUID caso,UUID doc,long rev){c().proteger(f.a(f.headA),UUID.randomUUID(),caso,doc,new ContenidoInstitucional.Proteccion(rev,"ORDINARIO","Clasificación expresa ordinaria"));}
 ContenidoInstitucional.Publicacion publicacion(long rev,boolean contenido,boolean descarga,String estado){return new ContenidoInstitucional.Publicacion(rev,clock.instant().minusSeconds(1),clock.instant().plusSeconds(1200),contenido,descarga,estado,"Publicación sintética por recurso");}
 public void close()throws Exception{files.close();d.close();}
}

