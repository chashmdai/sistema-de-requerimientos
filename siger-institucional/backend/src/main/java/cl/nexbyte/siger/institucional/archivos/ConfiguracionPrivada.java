package cl.nexbyte.siger.institucional.archivos;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.core.env.Environment;

/** Frontera de archivos privados configurable; no cambia permisos al leer una configuración. */
public final class ConfiguracionPrivada {
    private ConfiguracionPrivada(){}
    public static Path ruta(Environment env,String name)throws IOException{
        String configured=env.getProperty("institucional.configuracion-privada.directorio");
        if(PlataformaArchivos.windows()&&(configured==null||configured.isBlank()))throw new IOException("Falta raíz privada Windows");
        Path root=(configured==null?Path.of(System.getProperty("user.home"),".local","share","siger"):Path.of(configured)).toAbsolutePath().normalize();
        ArchivoPrivado.validarAcceso(root);
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)||!root.equals(root.toRealPath()))throw new IOException("Raíz privada no válida");
        for(Path p=root;p!=null;p=p.getParent())if(Files.exists(p.resolve(".git"),LinkOption.NOFOLLOW_LINKS))throw new IOException("Configuración dentro de Git");
        Path file=Path.of(name).toAbsolutePath().normalize();
        if(file.equals(root)||!file.startsWith(root))throw new IOException("Archivo fuera de raíz privada");
        return file;
    }
    public static Path archivo(Environment env,String name,long max)throws IOException{return ArchivoPrivado.validar(ruta(env,name),max);}
}
