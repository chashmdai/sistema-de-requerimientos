package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.archivos.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchivoPlataformaTest {
    @TempDir Path base;
    byte[] bytes="Original exclusivamente sintético".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    EntregaCompleta.Adjunto descriptor(UUID id)throws Exception{return new EntregaCompleta.Adjunto(id,"prueba.txt",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"text/plain","DESCARGA");}
    @ParameterizedTest @EnumSource(ArchivoInstitucional.Etapa.class)
    void interrupcionEnCadaFronteraSeRecuperaSinReemplazar(ArchivoInstitucional.Etapa etapa)throws Exception{
        Path root=base.resolve("archivo");UUID operation=UUID.randomUUID(),id=UUID.randomUUID();var a=descriptor(id);
        try(var f=new ArchivoInstitucional(root,PlataformaArchivos.actual(),current->{if(current==etapa)throw new IOException("Interrupción sintética");})){
            assertThrows(IOException.class,()->f.guardar(operation,a,new ByteArrayInputStream(bytes)));
        }
        try(var f=new ArchivoInstitucional(root)){
            f.guardar(operation,a,new ByteArrayInputStream(bytes));assertTrue(f.disponible(operation,a));
            var original=f.verificar(f.key(operation,id),a);assertArrayEquals(bytes,Files.readAllBytes(original));
            f.guardar(operation,a,new InputStream(){public int read(){throw new AssertionError("No descargar duplicado");}});
            try(var entries=Files.walk(root.resolve("objects"))){assertEquals(1,entries.filter(Files::isRegularFile).count());}
        }
    }
    @Test void falloDeBarreraDespuesDeRenameImpideDisponibilidadHastaReintento()throws Exception{
        Path root=base.resolve("archivo");UUID op=UUID.randomUUID(),id=UUID.randomUUID();var a=descriptor(id);Path object=root.resolve("objects").resolve(op.toString()).resolve(id+".bin");var deny=new AtomicBoolean(true);var real=PlataformaArchivos.actual();
        PlataformaArchivos fault=new PlataformaArchivos(){
            public void protegerDirectorio(Path p)throws IOException{real.protegerDirectorio(p);}public void validarCadena(Path p)throws IOException{real.validarCadena(p);}
            public void publicar(Path p,Path q)throws IOException{real.publicar(p,q);}public void sincronizar(Path p)throws IOException{if(deny.get()&&p.equals(root)&&Files.exists(object))throw new IOException("Fallo de flush sintético");real.sincronizar(p);}
        };
        try(var f=new ArchivoInstitucional(root,fault,e->{})){
            assertThrows(IOException.class,()->f.guardar(op,a,new ByteArrayInputStream(bytes)));assertTrue(Files.exists(object));
            assertThrows(IOException.class,()->f.disponible(op,a));assertThrows(IOException.class,()->f.guardar(op,a,new ByteArrayInputStream(bytes)));
            deny.set(false);f.guardar(op,a,new ByteArrayInputStream(bytes));assertTrue(f.disponible(op,a));
        }
    }
    @Test void escritorUnicoYReinicioConservanOriginal()throws Exception{
        Path root=base.resolve("archivo");try(var first=new ArchivoInstitucional(root)){assertThrows(IOException.class,()->new ArchivoInstitucional(root));}
        try(var after=new ArchivoInstitucional(root)){assertTrue(Files.isDirectory(after.temporales()));}
    }
    @Test void integridadInvalidaNoPublicaYOriginalCorruptoNoSeReemplaza()throws Exception{
        UUID op=UUID.randomUUID(),id=UUID.randomUUID();var a=descriptor(id);try(var f=new ArchivoInstitucional(base.resolve("archivo"))){
            assertThrows(IOException.class,()->f.guardar(op,a,new ByteArrayInputStream(new byte[]{1,2})));assertFalse(f.disponible(op,a));
            f.guardar(op,a,new ByteArrayInputStream(bytes));Path object=f.verificar(f.key(op,id),a);byte[] altered=bytes.clone();altered[0]^=1;Files.write(object,altered);
            assertThrows(IOException.class,()->f.disponible(op,a));assertThrows(IOException.class,()->f.guardar(op,a,new ByteArrayInputStream(bytes)));assertArrayEquals(altered,Files.readAllBytes(object));
        }
    }
    @ParameterizedTest @ValueSource(strings={"../escape","gestion/../../escape","gestion/NUL","gestion/11111111-1111-1111-1111-111111111111:stream","vistas/../otro"})
    void clavesNoEscapanDeCustodia(String key)throws Exception{try(var f=new ArchivoInstitucional(base.resolve("archivo"))){assertThrows(IOException.class,()->f.guardarClave(key,descriptor(UUID.randomUUID()),new ByteArrayInputStream(bytes)));}}
    @Test void junctionDeDestinoNoEsSeguidoNiEscrito()throws Exception{
        Path root=base.resolve("archivo"),outside=base.resolve("fuera");Files.createDirectory(outside);Path marker=outside.resolve("canario");Files.writeString(marker,"SIN CAMBIOS");
        UUID op=UUID.randomUUID();Path junction=root.resolve("objects").resolve(op.toString());
        try(var f=new ArchivoInstitucional(root)){
            if(PlataformaArchivos.windows()){var process=new ProcessBuilder("cmd.exe","/d","/c","mklink","/J",junction.toString(),outside.toString()).redirectErrorStream(true).start();process.getInputStream().readAllBytes();assertEquals(0,process.waitFor());}else Files.createSymbolicLink(junction,outside);
            try{assertThrows(IOException.class,()->f.guardar(op,descriptor(UUID.randomUUID()),new ByteArrayInputStream(bytes)));assertEquals("SIN CAMBIOS",Files.readString(marker));try(var entries=Files.list(outside)){assertEquals(1,entries.count());}}
            finally{Files.deleteIfExists(junction);}
        }
    }
    @Test void raizConEnlaceSeRechazaAntesDeModificarSuDestino()throws Exception{
        Path outside=base.resolve("otra-custodia"),link=base.resolve("raiz-link");Files.createDirectory(outside);Path canary=outside.resolve("canario");Files.writeString(canary,"NO MODIFICAR");
        if(PlataformaArchivos.windows()){var process=new ProcessBuilder("cmd.exe","/d","/c","mklink","/J",link.toString(),outside.toString()).redirectErrorStream(true).start();process.getInputStream().readAllBytes();assertEquals(0,process.waitFor());}else Files.createSymbolicLink(link,outside);
        try{assertThrows(IOException.class,()->new ArchivoInstitucional(link));assertEquals("NO MODIFICAR",Files.readString(canary));try(var paths=Files.list(outside)){assertEquals(1,paths.count());}}finally{Files.deleteIfExists(link);}
    }    public static final class Hijo {
        public static void main(String[] args)throws Exception{
            Path root=Path.of(args[0]);UUID op=UUID.fromString(args[1]),id=UUID.fromString(args[2]);byte[] bytes="CRASH SINTETICO".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var a=new EntregaCompleta.Adjunto(id,"crash.txt",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"text/plain","DESCARGA");
            try(var f=new ArchivoInstitucional(root,PlataformaArchivos.actual(),stage->{if(stage==ArchivoInstitucional.Etapa.PUBLICADO)Runtime.getRuntime().halt(37);})){f.guardar(op,a,new ByteArrayInputStream(bytes));}
            throw new AssertionError("No ocurrió la interrupción");
        }
    }
    @Test void procesoInterrumpidoDespuesDePublicarLiberaLockYSeRecupera()throws Exception{
        Path root=base.resolve("crash");UUID op=UUID.randomUUID(),id=UUID.randomUUID();String exe=Path.of(System.getProperty("java.home"),"bin",PlataformaArchivos.windows()?"java.exe":"java").toString();
        var child=new ProcessBuilder(exe,"--enable-native-access=ALL-UNNAMED","-XX:-UsePerfData","-cp",System.getProperty("java.class.path"),Hijo.class.getName(),root.toString(),op.toString(),id.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try{assertTrue(child.waitFor(20,java.util.concurrent.TimeUnit.SECONDS),"Proceso de prueba vencido");assertEquals(37,child.exitValue());}finally{if(child.isAlive()){child.destroyForcibly();child.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);}}
        byte[] payload="CRASH SINTETICO".getBytes(java.nio.charset.StandardCharsets.UTF_8);var a=new EntregaCompleta.Adjunto(id,"crash.txt",payload.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)),"text/plain","DESCARGA");
        try(var f=new ArchivoInstitucional(root)){assertTrue(f.disponible(op,a));f.guardar(op,a,new InputStream(){public int read(){throw new AssertionError("No reemplazar original recuperado");}});assertArrayEquals(payload,Files.readAllBytes(f.verificar(f.key(op,id),a)));}
    }}