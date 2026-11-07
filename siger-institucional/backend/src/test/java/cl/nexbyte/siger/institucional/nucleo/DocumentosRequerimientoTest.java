package cl.nexbyte.siger.institucional.nucleo;
import cl.nexbyte.siger.institucional.documental.*;import cl.nexbyte.siger.institucional.identidad.*;import cl.nexbyte.siger.institucional.robustez.*;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.io.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DocumentosRequerimientoTest {
 @TempDir Path temp;DocumentosReqFixture x;
 @BeforeEach void setup()throws Exception{x=new DocumentosReqFixture(temp);}
 @AfterEach void close()throws Exception{if(x!=null)x.close();}
 void denied(int code,Runnable op){assertEquals(code,assertThrows(FalloTecnico.class,op::run).status);}
 long count(String table){return x.d.db.sql.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
 @Test void originalConGestionReqNoCreaCasoNiActuacionNiCopiaDeContenido()throws Exception{
  long seq=x.gestion(),cases=count("core_case"),reqs=count("core_requirement"),history=count("institution_req_action");
  var doc=x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,seq,"aporte.txt",x.bytes());
  assertEquals(List.of(new DocumentosRequerimiento.Contexto(x.req,seq)),doc.contextos());assertEquals("SIN_CLASIFICAR",doc.proteccion());assertTrue(doc.consultable());
  assertEquals(cases,count("core_case"));assertEquals(reqs,count("core_requirement"));assertEquals(history,count("institution_req_action"));assertEquals(0,count("management_document_link"));
  var original=x.docs.referencia(x.f.a(x.f.profA),x.req,doc.id(),"DESCARGA");assertArrayEquals(x.bytes().readAllBytes(),Files.readAllBytes(x.docs.verificarArchivo(original)));
  assertEquals("REQ",x.d.db.sql.queryForObject("SELECT owner_context FROM institution_document WHERE id=?",String.class,doc.id().toString()));assertEquals(1,x.docs.listar(x.f.a(x.f.headA),x.req).documentos().size());
  assertEquals("REQ_DOCUMENTO_CREADO",x.docs.historial(x.f.a(x.f.profA),x.req,doc.id()).getFirst().tipo());assertEquals(1,x.f.core.bitacora().requerimiento(x.f.a(x.f.profA),x.req,0,25).total());
 }
 @Test void otraAreaTiDistribucionYMismoReqNoConcedenAccesoDocumental(){
  UUID doc=x.cargar().id();for(String actor:List.of(x.f.profB,x.f.headB,x.f.ti,x.f.dist,x.f.legacy)){
   denied(404,()->x.docs.listar(x.f.a(actor),x.req));denied(404,()->x.docs.referencia(x.f.a(actor),x.req,doc,"CONTENIDO"));denied(404,()->x.docs.historial(x.f.a(actor),x.req,doc));}
  UUID cas=x.abrir();denied(404,()->x.f.core.contenidos().permisos(x.f.a(x.f.profA),cas,doc));
  denied(404,()->x.f.core.contenidos().proteger(x.f.a(x.f.headA),x.key(),cas,doc,new ContenidoInstitucional.Proteccion(0,"ORDINARIO","No reinterpretar contexto")));
  assertEquals(1,x.docs.listar(x.f.a(x.f.profA),x.req).documentos().size());assertFalse(x.docs.listar(x.f.a(x.f.profA),x.req).editable());
 }
 @Test void reintentoConCasoPosteriorOCierreConservaOriginalYNoReejecutaCarga(){
  UUID key=x.key();var first=x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"aporte.txt",x.bytes());x.abrir();
  assertEquals(first,x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"aporte.txt",x.bytes()));assertEquals(1,count("requirement_document_command"));
  denied(409,()->x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"nuevo.txt",x.bytes()));
  denied(409,()->x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"cambiado.txt",x.bytes()));
 }
 @Test void cierreSinCasoConservaConsultaPeroImpideNuevaCarga(){var doc=x.cargar();x.f.core.u2().actuar(x.f.a(x.f.headA),x.key(),x.req,new ContratosNucleo.Decision(x.revision(),"Cierre explícito"),true);assertFalse(x.docs.listar(x.f.a(x.f.profA),x.req).editable());assertEquals(doc,x.docs.documento(x.f.a(x.f.profA),x.req,doc.id()));denied(409,()->x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"posterior.txt",x.bytes()));}
 @Test void referenciasExigenGestionDelMismoReqYNoFabricanGestiones(){
  var doc=x.cargar();long seq=x.gestion(),before=count("institution_req_action");UUID key=x.key();x.docs.vincular(x.f.a(x.f.profA),key,x.req,doc.id(),seq);x.docs.vincular(x.f.a(x.f.profA),key,x.req,doc.id(),seq);
  assertEquals(2,x.docs.documento(x.f.a(x.f.headA),x.req,doc.id()).contextos().size());assertEquals(before,count("institution_req_action"));assertEquals(2,count("requirement_document_link"));
  UUID other=x.nuevoReq();denied(404,()->x.docs.cargar(x.f.a(x.f.profA),x.key(),other,seq,"ajeno.txt",x.bytes()));
 }
 @Test void firYBloqueoTienenFacultadesDistintasYRevisionOptimista(){
  UUID doc=x.cargar().id();denied(404,()->x.docs.proteger(x.f.a(x.f.profA),x.key(),x.req,doc,new DocumentosRequerimiento.Proteccion(0,"ORDINARIO","No habilita a clasificar")));
  x.docs.proteger(x.f.a(x.f.headA),x.key(),x.req,doc,new DocumentosRequerimiento.Proteccion(0,"FIR","Contenido interno reservado"));assertTrue(x.docs.documento(x.f.a(x.f.profA),x.req,doc).consultable());
  UUID key=x.key();var block=new DocumentosRequerimiento.Proteccion(1,"BLOQUEADO","Restricción preventiva del profesional");assertNull(x.docs.proteger(x.f.a(x.f.profA),key,x.req,doc,block));assertNull(x.docs.proteger(x.f.a(x.f.profA),key,x.req,doc,block));
  assertTrue(x.docs.listar(x.f.a(x.f.profA),x.req).documentos().isEmpty());var meta=x.docs.documento(x.f.a(x.f.headA),x.req,doc);assertFalse(meta.consultable());assertFalse(meta.descargable());assertEquals(2,meta.revision());denied(404,()->x.docs.referencia(x.f.a(x.f.headA),x.req,doc,"CONTENIDO"));
  denied(409,()->x.docs.proteger(x.f.a(x.f.headA),x.key(),x.req,doc,new DocumentosRequerimiento.Proteccion(0,"ORDINARIO","Versión obsoleta")));
  x.docs.proteger(x.f.a(x.f.headA),x.key(),x.req,doc,new DocumentosRequerimiento.Proteccion(2,"ORDINARIO","Revisión fundada de restricción"));assertEquals(4,x.docs.historial(x.f.a(x.f.profA),x.req,doc).size());
 }
 @Test void revocacionConOperadorCapturadoNoEsperaOtraSesion(){var actor=x.f.a(x.f.profA);UUID doc=x.cargar().id();x.clock.avanzar(1801);denied(403,()->x.docs.referencia(actor,x.req,doc,"CONTENIDO"));denied(403,()->x.docs.listar(actor,x.req));}
 @Test void noInfieraAreaHistoricaNiCambieAmbitoDelOriginal(){UUID doc=x.cargar().id();var before=x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",doc.toString());x.d.db.sql.update("UPDATE institution_req SET area_id=NULL WHERE requirement_id=?",x.req.toString());denied(404,()->x.docs.referencia(x.f.a(x.f.profA),x.req,doc,"CONTENIDO"));assertEquals(before,x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",doc.toString()));}
 @Test void casoAbiertoDuranteAntivirusImpidePersistenciaParcial(){var docs=x.servicio(p->{x.abrir();return true;},x.inspector);denied(409,()->docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"carrera.txt",x.bytes()));assertEquals(0,count("requirement_document_policy"));assertEquals(0,count("institution_document"));assertEquals(0,count("requirement_document_command"));}
 @Test void antivirusYFormatoRechazadosNoConservanMetadatosNiComandos(){
  denied(422,()->x.servicio(p->false,x.inspector).cargar(x.f.a(x.f.profA),x.key(),x.req,null,"rechazo.txt",x.bytes()));
  denied(422,()->x.servicio(p->true,(p,e)->{throw new IllegalArgumentException();}).cargar(x.f.a(x.f.profA),x.key(),x.req,null,"rechazo.txt",x.bytes()));
  assertEquals(0,count("institution_document"));assertEquals(0,count("requirement_document_command"));assertEquals(2,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM matter_access_event WHERE action_code IN ('ANTIVIRUS_RECHAZADO','FORMATO_RECHAZADO')",Long.class));
 }
 @Test void falloDePersistenciaRevierteDocumentoPoliticaVinculoComandoEHistoria(){
  x.d.ddl(sql->sql.execute("ALTER TABLE requirement_document_policy ADD CONSTRAINT reqdoc_rollback CHECK(revision=99)"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->x.cargar());}finally{x.d.ddl(sql->sql.execute("ALTER TABLE requirement_document_policy DROP "+(System.getProperty("siger.test.autoridad.mysql")==null?"CONSTRAINT":"CHECK")+" reqdoc_rollback"));}
  assertEquals(0,count("institution_document"));assertEquals(0,count("requirement_document_policy"));assertEquals(0,count("requirement_document_link"));assertEquals(0,count("requirement_document_command"));assertEquals(0,x.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_flow_history WHERE resource_kind='REQ_DOCUMENTO'",Long.class));
 }
 @Test void cargasConcurrentesConMismaClaveConservanUnSoloOriginal()throws Exception{
  UUID key=x.key();var go=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){Callable<UUID> op=()->{go.await();return x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"unico.txt",x.bytes()).id();};var a=pool.submit(op);var b=pool.submit(op);go.countDown();assertEquals(a.get(8,TimeUnit.SECONDS),b.get(8,TimeUnit.SECONDS));}
  assertEquals(1,count("institution_document"));assertEquals(1,count("requirement_document_policy"));assertEquals(1,count("requirement_document_link"));assertEquals(1,count("requirement_document_command"));
 }
 @Test void corrupcionDeCustodiaImpideReintentoYLecturaDeBytes()throws Exception{UUID key=x.key();var doc=x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"unico.txt",x.bytes());var ref=x.docs.referencia(x.f.a(x.f.profA),x.req,doc.id(),"CONTENIDO");Files.writeString(x.docs.verificarArchivo(ref),"Cambio ajeno a la custodia");denied(409,()->x.docs.verificarArchivo(ref));denied(409,()->x.docs.cargar(x.f.a(x.f.profA),key,x.req,null,"unico.txt",x.bytes()));assertEquals(1,count("institution_document"));}
 @Test void aliasDeLaMismaPersonaConRolPropioNoHeredaAtencionNiDocumento()throws Exception{
  UUID doc=x.cargar().id();String alias=x.f.user("alias.req");var r=x.d.mapper.readValue(Base64.getDecoder().decode(x.d.db.sql.queryForObject("SELECT signed_payload FROM authority_batch ORDER BY revision DESC LIMIT 1",String.class)),AutoridadesInstitucionales.Registro11.class);
  var bindings=new ArrayList<>(r.vinculos());bindings.add(new AutoridadesInstitucionales.Vinculo(alias,"p-"+x.f.profA,x.f.start,x.f.end,ConcesionFixture.huella(x.d.db,alias)));
  x.f.importar(new AutoridadesInstitucionales.Registro11("1.1",r.revision()+1,x.clock.instant(),r.expira(),bindings,r.mandatos(),r.areas(),r.ambitos(),r.cambiosArea()));x.f.otorgar(alias,"PROFESIONAL","AREA_A");
  denied(404,()->x.docs.listar(x.f.a(alias),x.req));denied(404,()->x.docs.referencia(x.f.a(alias),x.req,doc,"CONTENIDO"));assertNotNull(x.docs.referencia(x.f.a(x.f.profA),x.req,doc,"CONTENIDO"));
 }
 @Test void principalTransferidoNoPublicaAntecedentesDelAmbitoAnterior(){
  UUID doc=x.cargar().id();var before=x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",doc.toString());
  var proposal=x.f.core.continuidad().transferir(x.f.a(x.f.headA),x.key(),x.req,new ContinuidadU2.Trasladar(x.revision(),new ContratosNucleo.NuevoDestino("UPR","DEMO_SEDE_A",x.f.headB,"AREA_B"),"Cambio explícito de principal","Metadatos de evaluación")).id();
  x.f.core.continuidad().decidirTransferencia(x.f.a(x.f.headB),x.key(),proposal,new ContinuidadU2.Resolver(0,"ACEPTADA",null,"Aceptación expresa"));
  assertEquals("AREA_B",x.f.core.u2().requerimiento(x.f.a(x.f.headB),x.req).get("area_id"));assertTrue(x.docs.listar(x.f.a(x.f.headB),x.req).documentos().isEmpty());
  denied(404,()->x.docs.referencia(x.f.a(x.f.headB),x.req,doc,"CONTENIDO"));denied(404,()->x.docs.referencia(x.f.a(x.f.profA),x.req,doc,"CONTENIDO"));assertEquals(before,x.d.db.sql.queryForMap("SELECT * FROM requirement_document_policy WHERE document_id=?",doc.toString()));
  x.f.otorgar(x.f.profA,"PROFESIONAL","AREA_B");var nuevo=x.docs.cargar(x.f.a(x.f.headB),x.key(),x.req,null,"destino.txt",x.bytes());assertNotNull(x.docs.referencia(x.f.a(x.f.headB),x.req,nuevo.id(),"CONTENIDO"));denied(404,()->x.docs.referencia(x.f.a(x.f.profA),x.req,nuevo.id(),"CONTENIDO"));denied(404,()->x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"sin-asignacion.txt",x.bytes()));
 }
 @Test void profesionalDeCasoDelMismoReqNoHeredaDocumentoNiHistorial(){
  UUID doc=x.cargar().id();x.f.otorgar(x.f.profB,"PROFESIONAL","AREA_A");UUID cas=x.f.core.u2().abrirCaso(x.f.a(x.f.headA),x.key(),x.req,new NucleoU2.Apertura(x.revision(),x.f.profB,"Gestión del Caso independiente")).id();assertNotNull(x.f.core.caso(x.f.a(x.f.profB),cas));
  denied(404,()->x.docs.listar(x.f.a(x.f.profB),x.req));denied(404,()->x.docs.historial(x.f.a(x.f.profB),x.req,doc));denied(404,()->x.f.core.contenidos().permisos(x.f.a(x.f.profB),cas,doc));
 }
 @Test void descargaRevalidaAntesDeCadaBloqueYNoEntregaElRestoAlExpirar()throws Exception{
  byte[] bytes=new byte[200000];Arrays.fill(bytes,(byte)'a');var doc=x.docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"grande.txt",new ByteArrayInputStream(bytes));var a=x.f.a(x.f.profA);var ref=x.docs.referencia(a,x.req,doc.id(),"DESCARGA");
  var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/sintetico");var response=TransferenciaDocumento.responder(x.docs.verificarArchivo(ref),doc.tamanio(),doc.sha256(),doc.nombre(),doc.tipo(),false,request,mode->{},()->x.docs.revalidar(a,x.req,doc.id(),"DESCARGA"));
  var output=new ByteArrayOutputStream(){@Override public synchronized void write(byte[] b,int off,int len){super.write(b,off,len);x.clock.avanzar(1801);}};
  denied(403,()->{try{response.getBody().writeTo(output);}catch(IOException e){throw new UncheckedIOException(e);}});assertEquals(65536,output.size());assertArrayEquals(Arrays.copyOf(bytes,65536),output.toByteArray());
 }
 @Test void clasificacionesConcurrentesNoPierdenUnaDecisionNiDuplicanVersion()throws Exception{
  UUID doc=x.cargar().id();var go=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){Callable<Boolean> op=()->{go.await();try{x.docs.proteger(x.f.a(x.f.headA),x.key(),x.req,doc,new DocumentosRequerimiento.Proteccion(0,"FIR","Decisión concurrente fundada"));return true;}catch(FalloTecnico e){assertEquals(409,e.status);return false;}};var a=pool.submit(op);var b=pool.submit(op);go.countDown();assertNotEquals(a.get(8,TimeUnit.SECONDS),b.get(8,TimeUnit.SECONDS));}
  assertEquals(1,x.docs.documento(x.f.a(x.f.headA),x.req,doc).revision());assertEquals(2,x.docs.historial(x.f.a(x.f.headA),x.req,doc).size());
 }
 @Test void vistaPngDeReqGeneraDerivadaSinCrearCasoNiModificarElOriginal()throws Exception{
  var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var buffer=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",buffer);byte[] png=buffer.toByteArray();
  ProcesamientoDocumental.Inspector inspector=(p,e)->new ProcesamientoDocumental.Examen("image/png","PAGINAS",Files.readAllBytes(p));var docs=x.servicio(p->true,inspector);long cases=count("core_case");
  var doc=docs.cargar(x.f.a(x.f.profA),x.key(),x.req,null,"imagen.png",new ByteArrayInputStream(png));var ref=docs.referencia(x.f.a(x.f.profA),x.req,doc.id(),"CONTENIDO");var renderer=new ProcesamientoDocumental.Renderizador(){public int paginas(Path p){throw new AssertionError();}public byte[] pagina(Path p,int n){throw new AssertionError();}};
  var visor=new VisorDocumento(x.d.db,x.files,p->true,inspector,renderer,x.clock);assertEquals(1,visor.vista(ref).paginas());assertArrayEquals(png,Files.readAllBytes(visor.pagina(ref,1).path()));assertArrayEquals(png,Files.readAllBytes(docs.verificarArchivo(ref)));assertEquals(cases,count("core_case"));assertEquals(1,count("document_representation"));assertEquals(1,count("document_page"));
  docs.proteger(x.f.a(x.f.headA),x.key(),x.req,doc.id(),new DocumentosRequerimiento.Proteccion(0,"BLOQUEADO","Retirar acceso también a la derivada"));denied(404,()->docs.revalidar(x.f.a(x.f.profA),x.req,doc.id(),"CONTENIDO"));assertEquals(1,count("document_page"));
 }
}
