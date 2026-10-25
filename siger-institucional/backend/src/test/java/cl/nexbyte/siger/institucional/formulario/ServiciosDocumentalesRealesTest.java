package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.documental.*;
import java.nio.file.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
/** Workers reales existentes; no utiliza fuentes gubernamentales ni modifica la aplicación pública. */
@EnabledIfEnvironmentVariable(named="DOCUMENTAL_REAL_CONFIG",matches=".+")
class ServiciosDocumentalesRealesTest {
    @TempDir Path temp;AntivirusDocumentoLocal av;InspectorDocumentoLocal inspector;PdfAislado pdf;Path fixture;
    @BeforeEach void abrir()throws Exception{
        var props=new Properties();try(var reader=Files.newBufferedReader(Path.of(System.getenv("DOCUMENTAL_REAL_CONFIG")))){props.load(reader);}
        av=new AntivirusDocumentoLocal(Path.of(props.getProperty("institucional.documental.antivirus-socket")),Path.of(props.getProperty("institucional.documental.antivirus-configuracion")),FormatosDocumento.MAX);
        inspector=new InspectorDocumentoLocal(Path.of(props.getProperty("institucional.documental.inspector-socket")));pdf=new PdfAislado(Path.of(props.getProperty("institucional.documental.sandbox")),temp);fixture=Path.of(System.getenv("DOCUMENTAL_REAL_FIXTURES"));
    }
    @Test void pdfTresPaginasSeValidaYAislaYRenderizaCadaPagina()throws Exception{var file=fixture.resolve("tres-paginas.pdf");assertTrue(av.limpio(file));assertEquals("application/pdf",inspector.examinar(file,"pdf").tipo());assertEquals(3,pdf.paginas(file));var first=pdf.pagina(file,1);var second=pdf.pagina(file,2);assertTrue(first.length>33);assertFalse(Arrays.equals(first,second));assertTrue(pdf.pagina(file,3).length>33);}
    @Test void imagenDerivadaRealTienePngYAntivirusLimpio()throws Exception{var file=fixture.resolve("imagen.png");assertTrue(av.limpio(file));var result=inspector.examinar(file,"png");assertEquals("image/png",result.tipo());assertNotNull(result.png());assertEquals((byte)137,result.png()[0]);}
    @Test void controlAntivirusRealSeRechaza()throws Exception{assertFalse(av.limpio(fixture.resolve("control-antivirus.txt")));}
    @Test void pdfActivoYExtensionFalsaNoSeRepresentan(){assertThrows(IllegalArgumentException.class,()->inspector.examinar(fixture.resolve("activo.pdf"),"pdf"));assertThrows(IllegalArgumentException.class,()->inspector.examinar(fixture.resolve("falso.png"),"png"));}
    @Test void officeSeInspeccionaSinEjecutarYReferenciasExternasSeRechazan()throws Exception{assertEquals("DESCARGA",inspector.examinar(fixture.resolve("oficina.docx"),"docx").vista());assertThrows(IllegalArgumentException.class,()->inspector.examinar(fixture.resolve("enlace-falso.docx"),"docx"));}
}