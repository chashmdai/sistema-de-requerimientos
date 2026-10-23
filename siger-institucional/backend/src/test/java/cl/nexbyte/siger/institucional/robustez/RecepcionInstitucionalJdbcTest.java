package cl.nexbyte.siger.institucional.robustez;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.security.PublicKey;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class RecepcionInstitucionalJdbcTest {
 @TempDir Path dir;BaseJdbc db;RecepcionInstitucionalJdbc store;EntregaVerificada fixture;Map<String,PublicKey> trust;
 java.security.KeyPair institutionKeys,publicKeys;
 final JsonMapper mapper=JsonMapper.builder().build();
 @BeforeEach void open()throws Exception{
  var supplied=System.getenv("ROBUST_DB_URL");
  var url=supplied==null?"jdbc:h2:file:"+dir.resolve("institucion").toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":supplied;
  if(supplied!=null&&!url.startsWith("jdbc:mysql://127.0.0.1:13306/robust_inst_test?"))throw new IllegalStateException("Solo esquema local aislado");
  try(var ddl=new BaseJdbc(url,supplied==null?"sa":System.getenv("ROBUST_DB_MIGRATION_USER"),supplied==null?"":System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){ddl.migrate("institucional");}
  db=new BaseJdbc(url,supplied==null?"sa":System.getenv("ROBUST_DB_USER"),supplied==null?"":System.getenv("ROBUST_DB_PASSWORD"),12);
  if(supplied!=null){for(var table:List.of("communication_registry","institutional_inbox","control_audit","external_operation","operational_event"))db.sql.update("DELETE FROM "+table);db.sql.update("UPDATE operation_control SET revision=0,accept_new=FALSE,pull_enabled=FALSE,contingency='SOLO_CONSULTA'");}
  new ControlesOperacion(db).cambiar(0,true,true,"NORMAL","PRUEBA_LOCAL");
  fixture=mapper.readValue(getClass().getResourceAsStream("/contratos/entrega-v2.json"),EntregaVerificada.class);
  var tree=mapper.readTree(getClass().getResourceAsStream("/contratos/receipt-public-test.json"));var values=new HashMap<String,PublicKey>();
  publicKeys=java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();values.put("public-test",publicKeys.getPublic());
  tree.properties().forEach(e->values.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));trust=Map.copyOf(values);institutionKeys=java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();store=newStore("COM",6);
 }
 RecepcionInstitucionalJdbc newStore(String prefix,int width){return new RecepcionInstitucionalJdbc(db,mapper,trust,"institution-test",institutionKeys.getPrivate(),Map.of("institution-test",institutionKeys.getPublic()),prefix,width,java.time.Clock.systemUTC());}
 EntregaVerificada synthetic(){
  var id=UUID.randomUUID();var now=java.time.Instant.now();
  var body=cl.nexbyte.siger.institucional.integracion.SobreComunicacion.crear(id,now,fixture.comunicacion().identidad(),"Solo prueba sintética","laboratorio-v1");
  var provenance=EntregaVerificada.procedencia(now,UUID.randomUUID(),now,id);
  var content=new ComprobanteDigital.Contenido("1.0",id,now,"CUSTODIA_PUBLICA",body.sha256(),EntregaVerificada.huella(provenance),"FORMULARIO_PUBLICO_DDN","laboratorio-v1");
  return new EntregaVerificada("2.0",body,provenance,ComprobanteDigital.firmar(content,"public-test",publicKeys.getPrivate(),mapper));
 }
 @AfterEach void close(){if(db!=null)db.close();}
 @Test void registraIdentidadEstructuradaSinCambiarComponentesNiComprobanteHistorico(){
  var body=mapper.readValue(getClass().getResourceAsStream("/contratos/comunicacion-identidad-v1.1.json"),cl.nexbyte.siger.institucional.integracion.SobreComunicacion.class);
  var provenance=EntregaVerificada.procedencia(body.recibidaEn(),UUID.randomUUID(),body.recibidaEn(),body.id());
  var content=new ComprobanteDigital.Contenido("1.0",body.id(),body.recibidaEn(),"CUSTODIA_PUBLICA",body.sha256(),EntregaVerificada.huella(provenance),"FORMULARIO_PUBLICO_DDN",body.avisoPrivacidadVersion());
  var delivery=new EntregaVerificada("2.0",body,provenance,ComprobanteDigital.firmar(content,"public-test",publicKeys.getPrivate(),mapper));
  var receipt=store.guardar(delivery);var stored=mapper.readValue(db.sql.queryForObject("SELECT body FROM institutional_inbox WHERE id=?",String.class,body.id().toString()),EntregaVerificada.class);
  assertThat(stored.comunicacion().identidad()).isEqualTo(body.identidad());
  assertThat(newStore("OTR",9).guardar(delivery)).isEqualTo(receipt);
  var previous=store.guardar(fixture);assertThat(store.guardar(fixture)).isEqualTo(previous);
  assertThat(fixture.comunicacion().identidad().nombresComponentes()).isNull();
 }
 @Test void vectorInteroperableYDuplicadosConcurrentesTienenUnaConfirmacionEstable()throws Exception{
  fixture.validar(trust,mapper);
  try(var threads=Executors.newFixedThreadPool(24)){
   var gate=new CountDownLatch(1);var tasks=new ArrayList<Future<Object>>();
   for(int i=0;i<72;i++)tasks.add(threads.submit(()->{gate.await();return store.guardar(fixture);}));gate.countDown();var first=tasks.getFirst().get(30,TimeUnit.SECONDS);
   for(var t:tasks)assertThat(t.get(30,TimeUnit.SECONDS)).isEqualTo(first);
  }
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Long.class)).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM operational_event WHERE code='COMUNICACION_REGISTRADA'",Long.class)).isEqualTo(1);
 }
 @Test void doscientosFoliosDistintosBajoConcurrenciaYFormatoHistoricoEstable()throws Exception{
  var deliveries=new ArrayList<EntregaVerificada>();for(int i=0;i<200;i++)deliveries.add(synthetic());
  var results=new ArrayList<RegistroInstitucional>();
  try(var executor=Executors.newFixedThreadPool(12)){
   var tasks=new ArrayList<Future<RegistroInstitucional>>();for(var d:deliveries)tasks.add(executor.submit(()->store.guardar(d)));
   for(var task:tasks)results.add(task.get(40,TimeUnit.SECONDS));
  }
  assertThat(results.stream().map(x->x.registro().numeroFolio()).distinct().count()).isEqualTo(200);
  assertThat(results.stream().map(x->x.registro().folio()).distinct().count()).isEqualTo(200);
  assertThat(results.stream().map(x->x.registro().comunicacionId()).distinct().count()).isEqualTo(200);
  assertThat(newStore("ING",9).guardar(deliveries.getFirst())).isEqualTo(results.getFirst());
  assertThat(newStore("ING",9).guardar(synthetic()).registro().folio()).startsWith("ING-").hasSize(13);
  for(var r:results)assertThat(r.comprobante().verificar(Map.of("institution-test",institutionKeys.getPublic()),mapper)).isEqualTo(r.registro());
 }
 @Test void respuestaDelACKPerdidaDespuesDeCommitPublicoSeConciliaSinReclamar(){
  var claims=new java.util.concurrent.atomic.AtomicBoolean(true);var responses=new java.util.concurrent.atomic.AtomicInteger();
  var expected=new java.util.concurrent.atomic.AtomicReference<RegistroInstitucional>();
  var source=new FuenteEntregasV2(){
   public List<Reserva> reclamar(){return claims.getAndSet(false)?List.of(new Reserva(UUID.randomUUID(),0,1,fixture)):List.of();}
   public void confirmar(Reserva r,RegistroInstitucional ack){if(expected.get()==null)expected.set(ack);else assertThat(ack).isEqualTo(expected.get());if(responses.incrementAndGet()==1)throw new FalloTecnico(503);}
   public void fallo(Reserva r,String code){}
  };
  var job=new RecuperarEntregasV2(source,store);assertThat(job.ejecutar().fallidas()).isEqualTo(1);
  assertThat(job.ejecutar().recuperadas()).isZero();
  assertThat(job.conciliarConfirmaciones()).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class)).isFalse();
  assertThat(store.guardar(fixture)).isEqualTo(expected.get());
 }
 @Test void recepcionTecnicaLegadaSeRegistraAhoraSinReutilizarSuFechaNiInventarFolio(){
  var legacy="{\"recepcionInstitucionalId\":\"11111111-1111-4111-8111-111111111111\",\"registradaEn\":\"2025-01-01T00:00:00Z\",\"sha256\":\""+fixture.comunicacion().sha256()+"\"}";
  var fingerprint=java.util.HexFormat.of().formatHex(hash((fixture.comunicacion().sha256()+":"+EntregaVerificada.huella(fixture.procedencia())).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  db.sql.update("INSERT INTO institutional_inbox(source,id,content_hash,body,receipt,legacy_receipt,ack_pending,created_ms) VALUES(?,?,?,?,?,?,FALSE,0)","FORMULARIO_PUBLICO_DDN",fixture.comunicacion().id().toString(),fingerprint,mapper.writeValueAsString(fixture),legacy,legacy);
  var registered=store.guardar(fixture);
  assertThat(registered.registro().registradaEn()).isAfter(java.time.Instant.parse("2026-01-01T00:00:00Z"));
  assertThat(db.sql.queryForObject("SELECT legacy_receipt FROM institutional_inbox",String.class)).isEqualTo(legacy);
  assertThat(store.guardar(fixture)).isEqualTo(registered);
 }
 static byte[] hash(byte[] bytes){try{return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);}catch(Exception e){throw new IllegalStateException(e);}}
 @Test void rollbackNoPublicaFolioNiComprobanteYReintentoRegistraUnaSolaComunicacion(){
  ddl(sql->sql.execute("ALTER TABLE operational_event ADD CONSTRAINT reject_registration CHECK(code <> 'COMUNICACION_REGISTRADA')"));
  try{
   assertThatThrownBy(()->store.guardar(fixture)).isInstanceOf(org.springframework.dao.DataAccessException.class);
   assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isZero();
   assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Long.class)).isZero();
  }finally{ddl(sql->sql.execute("ALTER TABLE operational_event DROP "+(System.getenv("ROBUST_DB_URL")==null?"CONSTRAINT":"CHECK")+" reject_registration"));}
  var registered=store.guardar(fixture);assertThat(store.guardar(fixture)).isEqualTo(registered);
 }
 void ddl(java.util.function.Consumer<org.springframework.jdbc.core.JdbcTemplate> action){
  if(System.getenv("ROBUST_DB_URL")==null){action.accept(db.sql);return;}
  try(var migration=new BaseJdbc(System.getenv("ROBUST_DB_URL"),System.getenv("ROBUST_DB_MIGRATION_USER"),System.getenv("ROBUST_DB_MIGRATION_PASSWORD"),2)){action.accept(migration.sql);}
 }
 @Test void consumidorParaleloAcotadoConservaUnicidadYConfirmaCadaRegistro(){
  var deliveries=new ArrayList<FuenteEntregasV2.Reserva>();for(int i=0;i<20;i++)deliveries.add(new FuenteEntregasV2.Reserva(UUID.randomUUID(),0,1,synthetic()));
  var ack=new ConcurrentHashMap<UUID,RegistroInstitucional>();var claimed=new java.util.concurrent.atomic.AtomicBoolean();
  var source=new FuenteEntregasV2(){
   public List<Reserva> reclamar(){return claimed.compareAndSet(false,true)?deliveries:List.of();}
   public void confirmar(Reserva r,RegistroInstitucional receipt){assertThat(ack.putIfAbsent(r.entrega().comunicacion().id(),receipt)).isNull();}
   public void fallo(Reserva r,String code){throw new AssertionError(code);}
  };
  try(var worker=new RecuperarEntregasV2(source,store,4)){
   assertThat(worker.ejecutar().confirmadas()).isEqualTo(20);assertThat(worker.metricas().get("paralelismo")).isEqualTo(4);
  }
  assertThat(ack).hasSize(20);assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Long.class)).isZero();
  assertThat(ack.values().stream().map(x->x.registro().folio()).distinct().count()).isEqualTo(20);
 }
 @Test void confirmacionCorruptaSeAislaYNoBloqueaLasComunicacionesIntegras(){
  var damaged=synthetic();var healthy=synthetic();store.guardar(damaged);store.guardar(healthy);
  store.prepararConfirmacion(damaged.comunicacion().id(),UUID.randomUUID());store.prepararConfirmacion(healthy.comunicacion().id(),UUID.randomUUID());
  db.sql.update("UPDATE institutional_inbox SET receipt='{}' WHERE id=?",damaged.comunicacion().id().toString());
  var pending=store.confirmacionesPendientes();assertThat(pending).hasSize(1);
  assertThat(pending.getFirst().registro().registro().operacionPublicaId()).isEqualTo(healthy.comunicacion().id());
  assertThat(db.sql.queryForObject("SELECT ack_lease FROM institutional_inbox WHERE id=?",String.class,damaged.comunicacion().id().toString())).isNull();
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isEqualTo(2);
 }
 @Test void reaperturaDelRepositorioRecuperaElMismoRegistroYDocumento()throws Exception{
  var original=store.guardar(fixture);
  var supplied=System.getenv("ROBUST_DB_URL");
  var url=supplied==null?"jdbc:h2:file:"+dir.resolve("institucion").toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0":supplied;
  db.close();db=new BaseJdbc(url,supplied==null?"sa":System.getenv("ROBUST_DB_USER"),supplied==null?"":System.getenv("ROBUST_DB_PASSWORD"),12);
  store=newStore("NUEVO",8);assertThat(store.guardar(fixture)).isEqualTo(original);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isEqualTo(1);
 }
 @Test void ackPerdidoSeRecuperaSinVolverAGuardarNiCambiarRecibo(){
  var first=store.guardar(fixture);var attempts=new java.util.concurrent.atomic.AtomicInteger();
  var source=new FuenteEntregasV2(){
   public List<Reserva> reclamar(){return List.of(new Reserva(UUID.randomUUID(),System.currentTimeMillis()+300000,1,fixture));}
   public void confirmar(Reserva r,RegistroInstitucional ack){assertThat(ack).isEqualTo(first);if(attempts.incrementAndGet()==1)throw new FalloTecnico(503);}
   public void fallo(Reserva r,String code){assertThat(code).isEqualTo("CONFIRMACION_AMBIGUA");}
  };
  var job=new RecuperarEntregasV2(source,store);assertThat(job.ejecutar().fallidas()).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class)).isTrue();
  store=newStore("COM",6);
  assertThat(new RecuperarEntregasV2(source,store).ejecutar().confirmadas()).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class)).isFalse();
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Long.class)).isEqualTo(1);
 }
 @Test void firmaAlteradaNuncaLlegaALaPersistenciaNiALaConfirmacion(){
  var receipt=fixture.comprobante();var invalid=new ComprobanteDigital(receipt.algoritmo(),receipt.keyId(),receipt.payload(),"A".repeat(86));
  var damaged=new EntregaVerificada("2.0",fixture.comunicacion(),fixture.procedencia(),invalid);
  assertThatThrownBy(()->store.guardar(damaged)).isInstanceOf(IllegalArgumentException.class);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox",Long.class)).isZero();
 }
 @Test void controlesCerradosNoImpidanReentregaYaConfirmadaPeroSiIngresosNuevos(){
  var controls=new ControlesOperacion(db);controls.cambiar(1,false,false,"SOLO_CONSULTA","MANTENIMIENTO");
  assertThatThrownBy(()->store.guardar(fixture)).isInstanceOf(FalloTecnico.class);
  controls.cambiar(2,true,true,"NORMAL","REANUDACION");var ack=store.guardar(fixture);
  controls.cambiar(3,false,false,"SOLO_CONSULTA","MANTENIMIENTO");assertThat(store.guardar(fixture)).isEqualTo(ack);
 }

 @Test void masDeVeinteRechazosNoImpidenACKPosterioresYSeAcotanReintentos(){
  var bad=new HashSet<UUID>();
  for(int i=0;i<25;i++){var d=synthetic();bad.add(d.comunicacion().id());store.guardar(d);store.prepararConfirmacion(d.comunicacion().id(),UUID.randomUUID());}
  for(int i=0;i<10;i++){var d=synthetic();store.guardar(d);store.prepararConfirmacion(d.comunicacion().id(),UUID.randomUUID());}
  var source=new FuenteEntregasV2(){
   public List<Reserva> reclamar(){return List.of();}
   public void confirmar(Reserva r,RegistroInstitucional ack){if(bad.contains(r.entrega().comunicacion().id()))throw new FalloTecnico(409);}
   public void fallo(Reserva r,String code){}
  };
  try(var worker=new RecuperarEntregasV2(source,store)){
   assertThat(worker.conciliarConfirmaciones()).isZero();
   assertThat(worker.conciliarConfirmaciones()).isEqualTo(10);
   for(int i=0;i<4;i++){db.sql.update("UPDATE institutional_inbox SET ack_next_ms=0");worker.conciliarConfirmaciones();}
   assertThat(worker.conciliarConfirmaciones()).isZero();
  }
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=FALSE",Long.class)).isEqualTo(10);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE AND ack_blocked=TRUE AND ack_attempts=3",Long.class)).isEqualTo(25);
  assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isEqualTo(35);
  var id=bad.iterator().next();store.reintentarConfirmacion(id,3);
  assertThat(store.confirmacionesPendientes()).hasSize(1);
  assertThatThrownBy(()->store.reintentarConfirmacion(id,3)).isInstanceOf(FalloTecnico.class);
 }
 @Test void falloTransitorioSeAplazaSinConfirmarYSeRecuperaConMismoComprobante(){
  var ack=store.guardar(fixture);var id=fixture.comunicacion().id();var lease=UUID.randomUUID();store.prepararConfirmacion(id,lease);
  store.aplazarConfirmacion(id,lease,503);assertThat(store.confirmacionesPendientes()).isEmpty();
  assertThat(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class)).isTrue();
  db.sql.update("UPDATE institutional_inbox SET ack_next_ms=0");
  assertThat(store.confirmacionesPendientes().getFirst().registro()).isEqualTo(ack);
  assertThat(db.sql.queryForObject("SELECT ack_blocked FROM institutional_inbox",Boolean.class)).isFalse();
 }

 @Test void recibeNnaFirmadoSinRunYRegistraCanalPropioSinAlterarHistoricos()throws Exception{
  var nna=mapper.readValue(getClass().getResourceAsStream("/contratos/entrega-nna-v2.1.json"),EntregaVerificada.class);
  var keys=mapper.readTree(getClass().getResourceAsStream("/contratos/public-nna-test.json"));
  var expanded=new HashMap<>(trust);keys.properties().forEach(e->expanded.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));trust=Map.copyOf(expanded);store=newStore("COM",6);
  var previous=store.guardar(fixture);var ack=store.guardar(nna);assertThat(store.guardar(nna)).isEqualTo(ack);
  assertThat(ack.registro().canalOrigen()).isEqualTo("FORMULARIO_WEB_NNA");assertThat(ack.registro().version()).isEqualTo("1.1");ack.validar(Map.of("institution-test",institutionKeys.getPublic()),mapper,nna);
  var stored=mapper.readValue(db.sql.queryForObject("SELECT body FROM institutional_inbox WHERE id=?",String.class,nna.comunicacion().id().toString()),EntregaVerificada.class);
  assertThat(stored.comunicacion().declaracionNna()).isEqualTo(nna.comunicacion().declaracionNna());assertThat(stored.comunicacion().identidad()).isNull();
  assertThat(stored.procedencia().values()).allMatch(a->a.fuente().equals("CIUDADANO"));assertThat(store.guardar(fixture)).isEqualTo(previous);
 }

 @Test void nnaRecuperaAckPerdidoYReintentosMantienenUnFolioYComprobante(){
  var nna=mapper.readValue(getClass().getResourceAsStream("/contratos/entrega-nna-v2.1.json"),EntregaVerificada.class);
  var expanded=new HashMap<>(trust);mapper.readTree(getClass().getResourceAsStream("/contratos/public-nna-test.json")).properties().forEach(e->expanded.put(e.getKey(),ComprobanteDigital.publicPem(e.getValue().asString())));trust=Map.copyOf(expanded);store=newStore("COM",6);
  var times=new java.util.concurrent.atomic.AtomicInteger();var claimed=new java.util.concurrent.atomic.AtomicBoolean();var previous=new java.util.concurrent.atomic.AtomicReference<RegistroInstitucional>();
  var source=new FuenteEntregasV2(){
   public List<Reserva> reclamar(){return claimed.getAndSet(true)?List.of():List.of(new Reserva(UUID.randomUUID(),0,1,nna));}
   public void confirmar(Reserva r,RegistroInstitucional ack){if(previous.get()==null)previous.set(ack);else assertThat(ack).isEqualTo(previous.get());if(times.incrementAndGet()==1)throw new FalloTecnico(503);}
   public void fallo(Reserva r,String code){}
  };
  try(var worker=new RecuperarEntregasV2(source,store)){assertThat(worker.ejecutar().fallidas()).isEqualTo(1);assertThat(worker.conciliarConfirmaciones()).isEqualTo(1);}
  assertThat(store.guardar(nna)).isEqualTo(previous.get());assertThat(db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class)).isEqualTo(1);
  assertThat(db.sql.queryForObject("SELECT ack_pending FROM institutional_inbox",Boolean.class)).isFalse();
 }
}
