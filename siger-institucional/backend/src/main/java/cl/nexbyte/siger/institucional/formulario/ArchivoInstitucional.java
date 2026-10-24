package cl.nexbyte.siger.institucional.formulario;

import java.io.*;import java.nio.*;import java.nio.channels.*;import java.nio.file.*;import java.nio.file.attribute.PosixFilePermissions;import java.security.*;import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Un escritor por almacén; publicación fsync/rename antes de la transacción SQL. Nunca sirve rutas públicas. */
public final class ArchivoInstitucional implements AutoCloseable {
    private final Path root;private final FileChannel lockChannel;private final FileLock lock;
    public ArchivoInstitucional(Path root)throws IOException{
        this.root=root.toAbsolutePath().normalize();if(Files.isSymbolicLink(this.root))throw new IOException("Almacén inválido");
        Files.createDirectories(this.root);if(!this.root.toRealPath().equals(this.root))throw new IOException("Almacén con enlaces no permitido");
        for(var parent=this.root;parent!=null;parent=parent.getParent())if(Files.exists(parent.resolve(".git")))throw new IOException("Almacén fuera de Git requerido");
        Files.setPosixFilePermissions(this.root,PosixFilePermissions.fromString("rwx------"));Files.createDirectories(this.root.resolve("objects"));Files.createDirectories(this.root.resolve("tmp"));sync(this.root);sync(this.root.getParent());
        lockChannel=FileChannel.open(this.root.resolve("writer.lock"),CREATE,WRITE);lock=lockChannel.tryLock();if(lock==null)throw new IOException("Almacén ya abierto");
    }
    public String key(UUID operation,UUID attachment){return operation+"/"+attachment;}
    private Path path(String key)throws IOException{
        if(key==null||!key.matches("[a-f0-9-]{36}/[a-f0-9-]{36}"))throw new IOException("Referencia inválida");var p=root.resolve("objects").resolve(key+".bin");
        if(Files.isSymbolicLink(p.getParent())||Files.isSymbolicLink(p))throw new IOException("Enlace no permitido");return p;
    }
    public boolean disponible(UUID operation,EntregaCompleta.Adjunto a)throws IOException{var p=path(key(operation,a.id()));if(!Files.exists(p))return false;verificar(p,a);return true;}
    public synchronized void guardar(UUID operation,EntregaCompleta.Adjunto a,InputStream input)throws IOException{
        var destination=path(key(operation,a.id()));if(Files.exists(destination)){verificar(destination,a);return;}
        var temp=Files.createTempFile(root.resolve("tmp"),"transfer-",".part");
        try{
            var digest=digest();long size=0;byte[] buffer=new byte[65536];
            try(var file=FileChannel.open(temp,WRITE)){for(int n;(n=input.read(buffer))!=-1;){size=Math.addExact(size,n);if(size>a.tamanio())throw new IOException("Tamaño excedido");digest.update(buffer,0,n);var bytes=ByteBuffer.wrap(buffer,0,n);while(bytes.hasRemaining())file.write(bytes);}if(size!=a.tamanio()||!HexFormat.of().formatHex(digest.digest()).equals(a.sha256()))throw new IOException("Integridad inválida");file.force(true);}
            Files.createDirectories(destination.getParent());sync(root.resolve("objects"));Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE);sync(destination.getParent());
        }finally{Files.deleteIfExists(temp);}
    }
    public Path verificar(String key,EntregaCompleta.Adjunto a)throws IOException{var p=path(key);verificar(p,a);return p;}
    private static void verificar(Path p,EntregaCompleta.Adjunto a)throws IOException{if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||Files.size(p)!=a.tamanio())throw new IOException("Original ausente o incompleto");var d=digest();try(var in=Files.newInputStream(p)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)d.update(b,0,n);}if(!HexFormat.of().formatHex(d.digest()).equals(a.sha256()))throw new IOException("Original alterado");}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    private static void sync(Path p)throws IOException{try(var channel=FileChannel.open(p,READ)){channel.force(true);}}
    public void close()throws IOException{lock.release();lockChannel.close();}
}
