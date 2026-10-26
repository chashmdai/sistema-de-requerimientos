package cl.nexbyte.siger.institucional.archivos;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** Archivos privados del servidor. Windows admite únicamente usuario actual, SYSTEM y Administradores. */
public final class ArchivoPrivado {
    private ArchivoPrivado(){}
    public static void proteger(Path path)throws IOException{
        if(PlataformaArchivos.windows())Win32Archivos.proteger(path);
        else Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(Files.isDirectory(path)?"rwx------":"rw-------"));
    }
    public static void validarAcceso(Path path)throws IOException{
        if(PlataformaArchivos.windows()){Win32Archivos.validarCadena(path);Win32Archivos.validarAcl(path);}
        else if(Files.isSymbolicLink(path))throw new IOException("Enlace no permitido");
    }
    public static Path validar(Path path,long max)throws IOException{
        Path p=path.toAbsolutePath().normalize();
        if(PlataformaArchivos.windows()){Win32Archivos.validarCadena(p);Win32Archivos.validarAcl(p);}
        else if(!p.equals(p.toRealPath())||!Files.getPosixFilePermissions(p).equals(PosixFilePermissions.fromString("rw-------")))throw new IOException("Custodia privada no válida");
        if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||Files.size(p)>max)throw new IOException("Archivo privado no válido");return p;
    }
}