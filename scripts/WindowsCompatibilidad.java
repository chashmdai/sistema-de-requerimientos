import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
public class WindowsCompatibilidad {
 public static void main(String[] args)throws Exception{
  var results=new TreeMap<String,Object>();Path root=Path.of("C:/req/scratch/probe-portabilidad").resolve(UUID.randomUUID().toString());Files.createDirectories(root);
  results.put("os",System.getProperty("os.name"));results.put("java",Runtime.version().toString());results.put("filesystem",Files.getFileStore(root).type());
  try{Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));results.put("posixPermissions",true);}catch(Exception e){results.put("posixPermissions",e.getClass().getSimpleName());}
  try(var channel=FileChannel.open(root,StandardOpenOption.READ)){channel.force(true);results.put("directoryFsync",true);}catch(Exception e){results.put("directoryFsync",e.getClass().getSimpleName());}
  try(var channel=SocketChannel.open(StandardProtocolFamily.UNIX)){results.put("unixSocketApi",true);}catch(Exception e){results.put("unixSocketApi",e.getClass().getSimpleName());}
  Path original=root.resolve("move-source.tmp"),dest=root.resolve("move-dest.tmp");Files.writeString(original,"SIGER U4.5 synthetic");try(var c=FileChannel.open(original,StandardOpenOption.WRITE)){c.force(true);results.put("fileForce",true);}
  Files.move(original,dest,StandardCopyOption.ATOMIC_MOVE);results.put("atomicMove",true);
  try(var c=FileChannel.open(root.resolve("writer.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=c.tryLock()) {results.put("exclusiveWriterLock",lock!=null);}
  results.put("linuxPrlimit",Files.isExecutable(Path.of("/usr/bin/prlimit")));results.put("linuxDevNull",Files.exists(Path.of("/dev/null")));
  System.out.println(JsonMapper.builder().build().writeValueAsString(results));
 }
}