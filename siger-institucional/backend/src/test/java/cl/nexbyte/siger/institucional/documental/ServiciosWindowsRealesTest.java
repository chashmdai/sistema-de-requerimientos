package cl.nexbyte.siger.institucional.documental;
import java.nio.file.*;import java.util.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.*;import static org.junit.jupiter.api.Assertions.*;
@EnabledOnOs(OS.WINDOWS) @EnabledIfEnvironmentVariable(named="DOCUMENTAL_REAL_CONFIG",matches=".+")
class ServiciosWindowsRealesTest {
    ServiciosDocumentalesWindows s;Path fixtures;
    @BeforeEach void abrir()throws Exception{var p=new Properties();try(var in=Files.newInputStream(Path.of(System.getenv("DOCUMENTAL_REAL_CONFIG")))){p.load(in);}s=new ServiciosDocumentalesWindows(Path.of(p.getProperty("windows.servicios")));fixtures=Path.of(System.getenv("DOCUMENTAL_REAL_FIXTURES"));}
    @Test void officeYTextoAdmitidosSinEjecutarlos()throws Exception{for(var ext:List.of("docx","xlsx","pptx","odt","txt")){var name=ext.equals("txt")?"texto.txt":"oficina."+ext;assertEquals("DESCARGA",s.inspector().examinar(fixtures.resolve(name),ext).vista(),name);}}
    @Test void zipActivoEnlacesYEntidadesExternasSeRechazan(){for(var name:List.of("xxe.docx","traversal.docx","macro.docx","formula.docx","symlink.docx","enlace-falso.docx"))assertThrows(IllegalArgumentException.class,()->s.inspector().examinar(fixtures.resolve(name),"docx"),name);assertThrows(IllegalArgumentException.class,()->s.inspector().examinar(fixtures.resolve("binario.txt"),"txt"));}
    @Test void multimediaRealSeDecodificaConPerfilRestringido()throws Exception{for(var ext:List.of("jpg","webp","mp3","wav","m4a","ogg","mp4","mov","webm")){var v=s.inspector().examinar(fixtures.resolve("medio."+ext),ext);assertEquals(FormatosDocumento.TIPOS.get(ext),v.tipo());assertEquals(Set.of("jpg","webp").contains(ext)?"IMAGEN_DERIVADA":Set.of("mp4","mov","webm").contains(ext)?"VIDEO":"AUDIO",v.vista());}}
    @Test void parserInvalidoYPerfilMultimediaIncorrectoFallanCerrados(){assertThrows(IllegalArgumentException.class,()->s.inspector().examinar(fixtures.resolve("roto.pdf"),"pdf"));assertThrows(IllegalArgumentException.class,()->s.inspector().examinar(fixtures.resolve("medio.wav"),"mp4"));assertThrows(IllegalArgumentException.class,()->s.inspector().examinar(fixtures.resolve("imagen.png"),"webp"));}
}
