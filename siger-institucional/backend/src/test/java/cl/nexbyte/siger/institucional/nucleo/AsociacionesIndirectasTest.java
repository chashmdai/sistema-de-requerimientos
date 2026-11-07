package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class AsociacionesIndirectasTest {
 @TempDir Path temp;
 void denied(int status,Runnable work){assertEquals(status,assertThrows(FalloTecnico.class,work::run).status);}
 @Test void referenciaConteoHistoriaMateriaYPublicacionesNoRevivenAlRestaurar()throws Exception{
  String mysql=System.getProperty("siger.test.autoridad.mysql");try(var x=new ContenidoFixture(temp,Clock.systemUTC(),mysql==null?null:mysql+"_asocmix?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=10000")){
   var f=x.f;var p=x.par();UUID doc=x.cargar(p[0]).id();x.docs.vincular(f.a(f.headA),UUID.randomUUID(),doc,p[0],p[1],null);x.ordinario(p[0],doc,0);x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA"));x.c().publicar(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(0,true,true,"ACTIVA"));assertEquals(1,x.docs.listar(f.a(f.profB),p[1]).size());assertEquals(1,x.docs.listar(f.a(x.m.upd),p[1]).size());
   var reqs=new DocumentosRequerimiento(x.d.db,f.core,x.files,q->true,(q,e)->new ProcesamientoDocumental.Examen(FormatosDocumento.TIPOS.get(e),"DESCARGA",null),x.clock);var service=new AsociacionesDocumentales(x.d.db,f.core,f.cat,x.docs,reqs,x.clock);
   service.rectificar(f.a(f.profA),UUID.randomUUID(),"CASO",p[0],doc,new AsociacionesDocumentales.Rectificar(-1,List.of(),"Asociación equivocada"));
   for(String actor:List.of(f.profB,x.m.upd)){assertEquals(0,x.docs.listar(f.a(actor),p[1]).size());denied(404,()->x.docs.documento(f.a(actor),p[1],doc));denied(404,()->x.docs.historial(f.a(actor),p[1],doc));denied(404,()->service.ver(f.a(actor),"CASO",p[1],doc));}
   service.rectificar(f.a(f.headA),UUID.randomUUID(),"CASO",p[0],doc,new AsociacionesDocumentales.Rectificar(0,List.of("CASO"),"Restauración fundada del propietario"));assertTrue(x.docs.listar(f.a(f.profB),p[1]).isEmpty());assertTrue(x.docs.listar(f.a(x.m.upd),p[1]).isEmpty());assertEquals(2,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM management_document_link WHERE document_id=?",Long.class,doc.toString()));assertNotNull(x.docs.referencia(f.a(f.profA),p[0],doc));
   x.c().compartir(f.a(f.headA),UUID.randomUUID(),p[0],doc,p[1],x.publicacion(1,true,true,"ACTIVA"));assertNotNull(x.docs.referencia(f.a(f.profB),p[1],doc));assertTrue(x.docs.listar(f.a(x.m.upd),p[1]).isEmpty());
  }
 }
 @Test void retiradaDuranteStreamDetieneElSiguienteBloque()throws Exception{
  try(var x=new DocumentosReqFixture(temp,null,"_asoc")){byte[] bytes=new byte[200000];Arrays.fill(bytes,(byte)'x');var doc=x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"grande.txt",new ByteArrayInputStream(bytes));var actor=x.f.a(x.f.profA);var ref=x.docs.referencia(actor,x.req,doc.id(),"DESCARGA");var casos=new DocumentosGestion(x.d.db,x.files,new AutorizacionNucleo(x.f.core),p->true,x.inspector,x.clock);var service=new AsociacionesDocumentales(x.d.db,x.f.core,x.f.cat,casos,x.docs,x.clock);
   var response=TransferenciaDocumento.responder(x.docs.verificarArchivo(ref),doc.tamanio(),doc.sha256(),doc.nombre(),doc.tipo(),false,new org.springframework.mock.web.MockHttpServletRequest("GET","/sintetico"),mode->{},()->x.docs.revalidar(actor,x.req,doc.id(),"DESCARGA"));
   var out=new ByteArrayOutputStream(){@Override public synchronized void write(byte[] b,int off,int len){super.write(b,off,len);service.rectificar(x.f.a(x.f.headA),x.key(),"REQ",x.req,doc.id(),new AsociacionesDocumentales.Rectificar(-1,List.of(),"Retirada durante transferencia"));}};
   denied(404,()->{try{response.getBody().writeTo(out);}catch(IOException e){throw new UncheckedIOException(e);}});assertEquals(65536,out.size());assertArrayEquals(Arrays.copyOf(bytes,65536),out.toByteArray());
  }
 }
 @Test void cachePngYReferenciaPrivadaAnteriorNoEludenLaRetirada()throws Exception{
  try(var x=new DocumentosReqFixture(temp,null,"_asoc")){var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);byte[] png=out.toByteArray();var doc=x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"imagen.png",new ByteArrayInputStream(png));var ref=x.docs.referencia(x.f.a(x.f.profA),x.req,doc.id(),"CONTENIDO");var render=org.mockito.Mockito.mock(ProcesamientoDocumental.Renderizador.class);var visor=new VisorDocumento(x.d.db,x.files,p->true,(p,e)->new ProcesamientoDocumental.Examen("image/png","PAGINAS",Files.readAllBytes(p)),render,x.clock);assertEquals(1,visor.vista(ref).paginas());var page=visor.pagina(ref,1);var casos=new DocumentosGestion(x.d.db,x.files,new AutorizacionNucleo(x.f.core),p->true,x.inspector,x.clock);new AsociacionesDocumentales(x.d.db,x.f.core,x.f.cat,casos,x.docs,x.clock).rectificar(x.f.a(x.f.profA),x.key(),"REQ",x.req,doc.id(),new AsociacionesDocumentales.Rectificar(-1,List.of(),"Retirada expresa"));denied(404,()->visor.vista(ref));denied(404,()->visor.pagina(ref,1));assertTrue(Files.isRegularFile(page.path()));org.mockito.Mockito.verifyNoInteractions(render);}
 }
}
