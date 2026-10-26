package cl.nexbyte.siger.institucional.windows;
import cl.nexbyte.siger.institucional.archivos.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
class ConfiguracionPrivadaTest {
 @TempDir Path temp;
 @Test void archivoPrivadoAcotadoYRaizExplicita()throws Exception{Path root=Files.createDirectory(temp.resolve("privado"));ArchivoPrivado.proteger(root);Path p=Files.createFile(root.resolve("allowlist.txt"));ArchivoPrivado.proteger(p);Files.writeString(p,"sintetico@example.invalid");var e=new MockEnvironment().withProperty("institucional.configuracion-privada.directorio",root.toRealPath().toString());assertEquals(p.toAbsolutePath(),ConfiguracionPrivada.archivo(e,p.toString(),100));assertThrows(java.io.IOException.class,()->ConfiguracionPrivada.archivo(e,p.toString(),2));assertThrows(java.io.IOException.class,()->ConfiguracionPrivada.ruta(e,temp.resolve("externo").toString()));}
 @Test void nuncaSeAceptanSecretosEnGit()throws Exception{Path root=Files.createDirectory(temp.resolve("checkout"));Files.createDirectory(root.resolve(".git"));ArchivoPrivado.proteger(root);var e=new MockEnvironment().withProperty("institucional.configuracion-privada.directorio",root.toRealPath().toString());assertThrows(java.io.IOException.class,()->ConfiguracionPrivada.ruta(e,root.resolve("secret").toString()));}
}
