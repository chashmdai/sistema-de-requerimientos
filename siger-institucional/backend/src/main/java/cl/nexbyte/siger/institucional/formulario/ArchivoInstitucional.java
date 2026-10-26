package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.archivos.PlataformaArchivos;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Un escritor por almacén. Barreras de plataforma antes de SQL; no hay rutas públicas ni reemplazo. */
public final class ArchivoInstitucional implements AutoCloseable {
    private final Path root;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final PlataformaArchivos plataforma;
    private final PuntoFallo fallo;
    enum Etapa { TEMPORAL_ESCRITO, CONTENIDO_FORZADO, PUBLICADO, DIRECTORIOS_FORZADOS }
    @FunctionalInterface interface PuntoFallo { void pasar(Etapa etapa)throws IOException; }

    public ArchivoInstitucional(Path root)throws IOException{this(root,PlataformaArchivos.actual(),etapa->{});}
    ArchivoInstitucional(Path supplied,PlataformaArchivos plataforma,PuntoFallo fallo)throws IOException{
        this.plataforma=plataforma;this.fallo=fallo;
        Path normalized=supplied.toAbsolutePath().normalize();plataforma.validarCadena(normalized);
        Files.createDirectories(normalized);plataforma.validarCadena(normalized);
        Path real=normalized.toRealPath();
        if(!PlataformaArchivos.windows()&&!real.equals(normalized))throw new IOException("Almacén con enlaces no permitido");
        this.root=real;
        for(var parent=this.root;parent!=null;parent=parent.getParent())if(Files.exists(parent.resolve(".git")))throw new IOException("Almacén fuera de Git requerido");
        plataforma.protegerDirectorio(this.root);
        Path lockFile=this.root.resolve("writer.lock");plataforma.validarCadena(lockFile);
        lockChannel=FileChannel.open(lockFile,CREATE,WRITE);
        try{
            lock=lockChannel.tryLock();if(lock==null)throw new IOException("Almacén ya abierto");
        }catch(IOException|RuntimeException e){lockChannel.close();throw new IOException("Almacén ya abierto");}
        try{
            for(String name:List.of("objects","tmp")){Path directory=this.root.resolve(name);plataforma.validarCadena(directory);Files.createDirectories(directory);plataforma.protegerDirectorio(directory);}
            plataforma.sincronizar(this.root);plataforma.sincronizar(this.root.getParent());
        }catch(IOException|RuntimeException e){lock.release();lockChannel.close();throw e;}
    }
    public String key(UUID operation,UUID attachment){return operation+"/"+attachment;}
    private Path path(String key)throws IOException{
        if(key==null||!key.matches("([a-f0-9-]{36}/[a-f0-9-]{36}|gestion/[a-f0-9-]{36}|vistas/[a-f0-9-]{36}/[A-Za-z0-9_-]{1,40}/[1-9][0-9]{0,2})"))throw new IOException("Referencia inválida");
        Path p=root.resolve("objects").resolve(key+".bin").normalize();
        if(!p.startsWith(root.resolve("objects")))throw new IOException("Referencia fuera del almacén");
        validarUbicacion(p);return p;
    }
    private void validarUbicacion(Path p)throws IOException{
        plataforma.validarCadena(p);
        if(PlataformaArchivos.windows()){for(var parent=p;parent!=null&&parent.startsWith(root);parent=parent.getParent())if(Files.exists(parent,LinkOption.NOFOLLOW_LINKS))cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.validarAcceso(parent);}
        else if(!Files.getPosixFilePermissions(root).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")))throw new IOException("Almacén no privado");
    }
    public boolean disponible(UUID operation,EntregaCompleta.Adjunto a)throws IOException{
        Path p=path(key(operation,a.id()));if(!Files.exists(p,LinkOption.NOFOLLOW_LINKS))return false;
        verificar(p,a);barrera(p);return true;
    }
    public synchronized void guardar(UUID operation,EntregaCompleta.Adjunto a,InputStream input)throws IOException{guardarClave(key(operation,a.id()),a,input);}
    public synchronized void guardarClave(String key,EntregaCompleta.Adjunto a,InputStream input)throws IOException{
        Path destination=path(key);
        // Reintento después de rename/COMMIT perdido: verificar y repetir todas las barreras; no basta exists().
        if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS)){verificar(destination,a);barrera(destination);return;}
        validarUbicacion(root.resolve("tmp"));Path temp=Files.createTempFile(root.resolve("tmp"),"transfer-",".part");
        try{
            var digest=digest();long size=0;byte[] buffer=new byte[65536];
            try(var file=FileChannel.open(temp,WRITE)){
                for(int n;(n=input.read(buffer))!=-1;){size=Math.addExact(size,n);if(size>a.tamanio())throw new IOException("Tamaño excedido");digest.update(buffer,0,n);var bytes=ByteBuffer.wrap(buffer,0,n);while(bytes.hasRemaining())file.write(bytes);}
                if(size!=a.tamanio()||!HexFormat.of().formatHex(digest.digest()).equals(a.sha256()))throw new IOException("Integridad inválida");
                fallo.pasar(Etapa.TEMPORAL_ESCRITO);file.force(true);fallo.pasar(Etapa.CONTENIDO_FORZADO);
            }
            plataforma.validarCadena(destination);Files.createDirectories(destination.getParent());
            plataforma.sincronizar(root.resolve("objects"));plataforma.publicar(temp,destination);fallo.pasar(Etapa.PUBLICADO);
            barrera(destination);fallo.pasar(Etapa.DIRECTORIOS_FORZADOS);
        }finally{Files.deleteIfExists(temp);}
    }
    private void barrera(Path file)throws IOException{
        plataforma.validarCadena(file);
        // Archivo y todos los padres, incluyendo padre de raíz; NTFS usa FlushFileBuffers real sobre handles.
        plataforma.sincronizar(file);
        for(Path p=file.getParent();p!=null&&p.startsWith(root);p=p.getParent())plataforma.sincronizar(p);
        plataforma.sincronizar(root.getParent());
    }
    public Path verificar(String key,EntregaCompleta.Adjunto a)throws IOException{Path p=path(key);verificar(p,a);barrera(p);return p;}
    private static void verificar(Path p,EntregaCompleta.Adjunto a)throws IOException{
        if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||Files.size(p)!=a.tamanio())throw new IOException("Original ausente o incompleto");
        var d=digest();try(var in=Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)d.update(b,0,n);}
        if(!HexFormat.of().formatHex(d.digest()).equals(a.sha256()))throw new IOException("Original alterado");
    }
    public Path temporales(){return root.resolve("tmp");}
    public record Temporal(Path path,long size,String sha256) implements AutoCloseable {
        public void close()throws IOException{Files.deleteIfExists(path);}
        @Override public String toString(){return "Temporal[protegido]";}
    }
    public Temporal preparar(InputStream input,long max)throws IOException{
        validarUbicacion(temporales());Path file=Files.createTempFile(temporales(),"documento-",".part");boolean ok=false;
        try{
            long size=0;var sha=digest();try(var out=Files.newOutputStream(file)){byte[] buffer=new byte[65536];for(int n;(n=input.read(buffer))!=-1;){size=Math.addExact(size,n);if(size>max)throw new IOException("Límite documental excedido");sha.update(buffer,0,n);out.write(buffer,0,n);}}
            if(size==0)throw new IOException("Archivo vacío");ok=true;return new Temporal(file,size,HexFormat.of().formatHex(sha.digest()));
        }finally{if(!ok)Files.deleteIfExists(file);}
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    public void close()throws IOException{try{lock.release();}finally{lockChannel.close();}}
}