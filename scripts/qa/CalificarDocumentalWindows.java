import cl.nexbyte.siger.institucional.archivos.*;import cl.nexbyte.siger.institucional.documental.*;
import java.io.*;import java.nio.file.*;import java.net.*;import java.security.*;import java.util.*;import java.time.*;import tools.jackson.databind.json.JsonMapper;

/** Negativos sobre EL MISMO lanzador de los parsers. Canario explícito, separado del AV real. */
class CalificarDocumentalWindows {
 static Path root,tools,defs,jobs,evidence;static Map<String,String> hashes;static final JsonMapper JSON=JsonMapper.builder().build();
 static ProcesoDocumentoWindows core()throws Exception{return new ProcesoDocumentoWindows(tools,defs,jobs,hashes,evidence);}
 static Path input(String text)throws Exception{var p=root.resolve("input-"+UUID.randomUUID());Files.writeString(p,text);ArchivoPrivado.proteger(p);return p;}
 static void require(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 static void clean(ProcesoDocumentoWindows p)throws Exception{var c=p.ultimaLimpieza();require(c!=null&&c.hijoTerminado()&&c.aclRestauradas()&&c.perfilRetirado()&&c.temporalesRetirados(),"Limpieza incompleta");try(var s=Files.list(jobs)){require(s.noneMatch(v->v.getFileName().toString().startsWith("av-")),"Residuo pendiente");}}
 static void init(Path r)throws Exception{root=r.toAbsolutePath().normalize();require(root.toString().matches("C:\\\\req\\\\testdata\\\\calificacion-doc-[a-f0-9]{32}"),"Solo QA");tools=root.resolve("tools");defs=root.resolve("defs");jobs=root.resolve("jobs");evidence=root.resolve("evidence");hashes=Map.of("clamscan.exe",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tools.resolve("clamscan.exe")))));}
 public static void main(String[] a)throws Exception{
  init(Path.of(a[0]));if(a.length>1){core().procesar(input("sleep"),ProcesoDocumentoWindows.Operacion.AV,"");return;}
  var checks=new ArrayList<Object>();String phase="probe";
  try{
   var outside=root.resolve("secret-decoy");Files.writeString(outside,"SINTETICO_RESERVADO");ArchivoPrivado.proteger(outside);var write=root.resolve("fuera");
   try(var tcp4=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));var tcp6=new ServerSocket(0,1,InetAddress.getByName("::1"));var udp=new DatagramSocket(0,InetAddress.getLoopbackAddress())){
    tcp4.setSoTimeout(400);tcp6.setSoTimeout(400);udp.setSoTimeout(400);
    // Controles positivos de la red antes del proceso aislado.
    for(var server:List.of(tcp4,tcp6)){try(var client=new Socket(server.getInetAddress(),server.getLocalPort());var accepted=server.accept()){} }
    try(var sender=new DatagramSocket()){sender.send(new DatagramPacket(new byte[]{1},1,udp.getLocalAddress(),udp.getLocalPort()));udp.receive(new DatagramPacket(new byte[8],8));}
    String external=Arrays.stream(InetAddress.getAllByName("www.microsoft.com")).filter(v->v instanceof Inet4Address).findFirst().orElseThrow().getHostAddress();try(var positive=new Socket()){positive.connect(new InetSocketAddress(external,443),5000);}
    var p=core();var r=p.procesar(input("probe\n"+outside+"\n"+write+"\n"+tcp4.getLocalPort()+"\n"+tcp6.getLocalPort()+"\n"+udp.getLocalPort()+"\n"+external),ProcesoDocumentoWindows.Operacion.AV,"");
    require(r.resultado().scannerExit()==0,"Canario de frontera rechazado");for(var server:List.of(tcp4,tcp6)){try(var client=server.accept()){throw new AssertionError("Red TCP alcanzada");}catch(SocketTimeoutException expected){}}
    try{udp.receive(new DatagramPacket(new byte[8],8));throw new AssertionError("UDP alcanzado");}catch(SocketTimeoutException expected){}
    require(!Files.exists(write)&&Files.readString(outside).equals("SINTETICO_RESERVADO"),"Canario externo alterado");clean(p);checks.add(Map.of("mode",phase,"correct",true,"result",r.resultado()));
   }
   for(var mode:List.of("memory","cpu","crash","output","timeout")){
    phase=mode;var p=core();boolean failure=false;ProcesoDocumentoWindows.Ejecucion result=null;try{result=p.procesar(input(mode),ProcesoDocumentoWindows.Operacion.AV,"");}catch(IOException e){failure=true;}
    if(mode.equals("memory"))require(result!=null&&result.resultado().scannerExit()==0,"Límite de memoria no acreditado");
    if(mode.equals("cpu"))require(result!=null&&result.resultado().scannerExit()==(int)0xC0000044L,"CPU no terminada por Job");
    if(mode.equals("crash"))require(result!=null&&result.resultado().scannerExit()==37,"Crash no recuperado");
    if(mode.equals("timeout")||mode.equals("output"))require(failure,"No se aplico límite");clean(p);checks.add(Map.of("mode",mode,"correct",true,"cleanup",p.ultimaLimpieza(),"exit",result==null?"IO_RECHAZADO":result.resultado().scannerExit()));
   }
   phase="interruption";var p=core();var in=input("sleep");var fail=new java.util.concurrent.atomic.AtomicReference<Throwable>();var thread=new Thread(()->{try{p.procesar(in,ProcesoDocumentoWindows.Operacion.AV,"");}catch(Throwable e){fail.set(e);}});thread.start();
   long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);boolean started=false;while(System.nanoTime()<deadline){try(var list=Files.walk(jobs)){started=list.anyMatch(v->v.getFileName().toString().equals("stdout")&&readStarted(v));}if(started)break;Thread.sleep(50);}require(started,"Hijo no comenzó");boolean mutexClosed=false;try{core();}catch(IOException expected){mutexClosed=true;}require(mutexClosed,"Mutex no excluye otra instancia");checks.add(Map.of("mode","mutex","correct",true));thread.interrupt();thread.join(10000);require(!thread.isAlive()&&fail.get() instanceof IOException,"Interrupción no recuperada");clean(p);checks.add(Map.of("mode",phase,"correct",true));
   phase="jvm-abort";var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java.exe").toString(),"--enable-native-access=ALL-UNNAMED","-XX:-UsePerfData","-Xmx256m","-Djava.io.tmpdir="+root.resolve("tmp"),"-cp",System.getProperty("java.class.path"),"CalificarDocumentalWindows",root.toString(),"victim");builder.environment().clear();builder.environment().put("SystemRoot",System.getenv("SystemRoot"));var victim=builder.redirectOutput(root.resolve("victim-out").toFile()).redirectError(root.resolve("victim-err").toFile()).start();
   try{deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);Path journal=null;while(System.nanoTime()<deadline){try(var list=Files.walk(jobs)){journal=list.filter(v->v.getFileName().toString().equals("journal.properties")).findFirst().orElse(null);}if(journal!=null&&Files.exists(journal.getParent().resolve("stdout"))&&readStarted(journal.getParent().resolve("stdout")))break;Thread.sleep(50);}require(journal!=null&&readStarted(journal.getParent().resolve("stdout")),"Víctima no comenzó");victim.destroyForcibly();require(victim.waitFor(10,java.util.concurrent.TimeUnit.SECONDS),"Víctima no terminó");core();try(var list=Files.list(jobs)){require(list.noneMatch(v->v.getFileName().toString().startsWith("av-")),"No recuperó journal");}checks.add(Map.of("mode",phase,"correct",true));}finally{if(victim.isAlive()){victim.destroyForcibly();victim.waitFor();}}
   var result=Map.of("scope","CANARIO_LANZADOR_JAVA_NO_PARSER","checks",checks,"correct",true,"fullU45Closed",false);Files.write(root.resolve("qualification.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));System.out.println(JSON.writeValueAsString(result));
  }catch(Throwable e){var result=Map.of("scope","CANARIO_LANZADOR_JAVA_NO_PARSER","phase",phase,"checks",checks,"correct",false,"type",e.getClass().getSimpleName(),"message",Objects.toString(e.getMessage(),""));Files.write(root.resolve("qualification.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(result));System.out.println(JSON.writeValueAsString(result));System.exit(1);}
 }
 static boolean readStarted(Path p){try{return Files.readString(p).contains("CANARIO_INICIADO");}catch(IOException e){return false;}}
}
