package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.nucleo.*;import cl.nexbyte.siger.institucional.nucleo.ContratosNucleo.*;
import java.nio.file.*;import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
final class DatosDocumentalesTest implements AutoCloseable {
 final DatosFormularioTest d;final AutorizacionDocumental policy;final DocumentosGestion gestion;final VisorDocumento visor;
 final UUID uno,dos,otra,actuacion;boolean infectado,noDisponible,formatoInvalido;final AtomicInteger renders=new AtomicInteger(),escaneos=new AtomicInteger();
 final byte[] contenido="PDF-SINTETICO-DE-GESTION".getBytes(java.nio.charset.StandardCharsets.UTF_8);
 DatosDocumentalesTest(Path dir)throws Exception{
  d=new DatosFormularioTest(dir);policy=new AutorizacionNucleo(d.core);
  uno=caso("UPR","demo.profesional.upr.a");dos=caso("UPR","demo.profesional2.upr.a");otra=caso("UPD","demo.profesional.upd.a");
  d.core.actuar(d.catalog.operador("demo.profesional.upr.a"),UUID.randomUUID(),uno,new Decision(d.core.caso(d.catalog.operador("demo.profesional.upr.a"),uno).revision(),"Actuación sintética"));actuacion=d.core.caso(d.catalog.operador("demo.profesional.upr.a"),uno).actuaciones().getFirst().id();
  ProcesamientoDocumental.Antivirus av=file->{escaneos.incrementAndGet();if(noDisponible)throw new IOException("No disponible");return !infectado;};
  ProcesamientoDocumental.Inspector inspector=(file,ext)->{if(formatoInvalido)throw new IllegalArgumentException();return new ProcesamientoDocumental.Examen(FormatosDocumento.TIPOS.get(ext),"IMAGEN_DERIVADA",png(0));};
  ProcesamientoDocumental.Renderizador renderer=new ProcesamientoDocumental.Renderizador(){public int paginas(Path p){return 3;}public byte[] pagina(Path p,int n)throws IOException{renders.incrementAndGet();return png(n);}};
  gestion=new DocumentosGestion(d.db,d.files,policy,av,inspector,d.clock);visor=new VisorDocumento(d.db,d.files,av,inspector,renderer,d.clock);
 }
 UUID caso(String unit,String prof){var e=d.delivery("1.0","ADULTO_DECLARADO",List.of(),true,1);var ack=d.receiver.guardarCompleta(e,d.files);var com=ack.registro().comunicacionId();var head=d.catalog.operador("demo.jefatura."+unit.toLowerCase()+".a");d.core.distribuir(d.catalog.operador("demo.distribucion"),UUID.randomUUID(),com,new Distribucion(0,List.of(new NuevoDestino(unit,"DEMO_SEDE_A",head.id()))));var dest=d.core.detalle(head,com).destinos().getFirst();return d.core.abrirCaso(head,UUID.randomUUID(),dest.id(),new Asignacion(dest.revision(),prof,null)).id();}
 static byte[] png(int n)throws IOException{var image=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,n*1234567);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);return out.toByteArray();}
 public void close()throws Exception{d.close();}
}