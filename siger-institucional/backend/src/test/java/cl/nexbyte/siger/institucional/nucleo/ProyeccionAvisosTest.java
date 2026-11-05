package cl.nexbyte.siger.institucional.nucleo;
import java.nio.file.Path;import java.util.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class ProyeccionAvisosTest {
 @TempDir Path temp;DatosAvisosTest x;
 @BeforeEach void open()throws Exception{x=new DatosAvisosTest(temp);}
 @AfterEach void close(){x.close();}
 long evento(UUID com){return x.d.db.sql.queryForObject("SELECT seq FROM core_event WHERE communication_id=? AND code='COMUNICACION_MANUAL_REGISTRADA'",Long.class,com.toString());}
 long fuente(long seq){return x.d.db.sql.queryForObject("SELECT COUNT(*) FROM notification_source WHERE stream_code='CORE' AND event_key=?",Long.class,Long.toString(seq));}
 long avisos(long seq){return x.d.db.sql.queryForObject("SELECT COUNT(*) FROM institution_notification WHERE stream_code='CORE' AND event_key=?",Long.class,Long.toString(seq));}
 long outbox(long seq){return x.d.db.sql.queryForObject("SELECT COUNT(*) FROM notification_outbox o JOIN institution_notification n ON n.id=o.notification_id WHERE n.stream_code='CORE' AND n.event_key=?",Long.class,Long.toString(seq));}
 @Test void falloDeUnaFuenteRevierteSusAvisosYConservaLasFuentesConfirmadas(){
  long primero=evento(x.manual()),segundo=evento(x.manual());
  x.d.ddl(sql->sql.execute("ALTER TABLE notification_source ADD CONSTRAINT test_fuente CHECK(NOT(stream_code='CORE' AND event_key='"+segundo+"'))"));
  try{assertThrows(org.springframework.dao.DataAccessException.class,()->x.notices.proyectar());}finally{x.d.ddl(sql->sql.execute("ALTER TABLE notification_source DROP CONSTRAINT test_fuente"));}
  assertEquals(1,fuente(primero));assertEquals(1,avisos(primero));assertEquals(1,outbox(primero));
  assertEquals(0,fuente(segundo));assertEquals(0,avisos(segundo));assertEquals(0,outbox(segundo));
  x.notices.proyectar();x.notices.proyectar();
  for(long seq:List.of(primero,segundo)){assertEquals(1,fuente(seq));assertEquals(1,avisos(seq));assertEquals(1,outbox(seq));}
 }
 @Test void dosProyectoresConcurrentesNoPierdenNiDuplicanFuentes()throws Exception{
  var fuentes=List.of(evento(x.manual()),evento(x.manual()),evento(x.manual()));var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){Callable<Integer> task=()->{start.await();return x.notices.proyectar();};var a=pool.submit(task);var b=pool.submit(task);start.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}
  for(long seq:fuentes){assertEquals(1,fuente(seq));assertEquals(1,avisos(seq));assertEquals(1,outbox(seq));}
  assertTrue(x.notices.listar(x.a(x.ti),0,50).items().isEmpty());
 }
}
