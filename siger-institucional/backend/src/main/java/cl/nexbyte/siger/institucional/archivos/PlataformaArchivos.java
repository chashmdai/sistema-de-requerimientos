package cl.nexbyte.siger.institucional.archivos;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import static java.nio.file.StandardOpenOption.READ;

/** Operaciones de custodia por plataforma; no hay fallback cuando falla una barrera de persistencia. */
public interface PlataformaArchivos {
    static boolean windows(){return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");}
    static PlataformaArchivos actual(){return windows()?new Ntfs():new Posix();}
    void protegerDirectorio(Path path)throws IOException;
    void validarCadena(Path path)throws IOException;
    void sincronizar(Path path)throws IOException;
    void publicar(Path source,Path destination)throws IOException;

    final class Posix implements PlataformaArchivos {
        public void protegerDirectorio(Path p)throws IOException{Files.setPosixFilePermissions(p,PosixFilePermissions.fromString("rwx------"));}
        public void validarCadena(Path p)throws IOException{for(var a=p.toAbsolutePath();a!=null;a=a.getParent())if(Files.isSymbolicLink(a))throw new IOException("Enlace no permitido");}
        public void sincronizar(Path p)throws IOException{try(var c=FileChannel.open(p,READ)){c.force(true);}}
        public void publicar(Path a,Path b)throws IOException{Files.move(a,b,StandardCopyOption.ATOMIC_MOVE);}
    }
    final class Ntfs implements PlataformaArchivos {
        public void protegerDirectorio(Path p)throws IOException{Win32Archivos.proteger(p);if(!Files.getFileStore(p).type().equalsIgnoreCase("NTFS"))throw new IOException("Custodia Windows requiere NTFS local");}
        public void validarCadena(Path p)throws IOException{Win32Archivos.validarCadena(p);}
        public void sincronizar(Path p)throws IOException{Win32Archivos.sincronizar(p);}
        public void publicar(Path a,Path b)throws IOException{Win32Archivos.publicar(a,b);}
    }
}