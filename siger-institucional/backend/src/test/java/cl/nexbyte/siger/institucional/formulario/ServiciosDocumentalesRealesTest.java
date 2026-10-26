package cl.nexbyte.siger.institucional.formulario;
import cl.nexbyte.siger.institucional.documental.*;
import java.nio.file.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
/** Workers reales existentes; no utiliza fuentes gubernamentales ni modifica la aplicación pública. */
@EnabledIfEnvironmentVariable(named="DOCUMENTAL_REAL_CONFIG",matches=".+")
class ServiciosDocumentalesRealesTest {
    @TempDir Path temp;ProcesamientoDocumental.Antivirus av;InspectorDocumentoLocal inspector;PdfAislado pdf;Path fixture;Properties props;
    @BeforeEach void abrir()throws Exception{
        props=new Properties();try(var reader=Files.newBufferedReader(Path.of(System.getenv("DOCUMENTAL_REAL_CONFIG")))){props.load(reader);}
        if(System.getProperty("os.name","").startsWith("Windows")){
            if(!"WINDOWS_CLAMAV".equals(props.getProperty("institucional.documental.antivirus-proveedor")))throw new java.io.IOException("Proveedor real Windows requerido");
            var manifest=cl.nexbyte.siger.institucional.archivos.ArchivoPrivado.validar(path("windows.manifest"),65536);
            var tree=EntregaCompleta.lector().readTree(Files.readAllBytes(manifest));var hashes=new HashMap<String,String>();for(var entry:tree.path("files"))hashes.put(entry.path("name").asString(),entry.path("sha256").asString());
            av=new AntivirusDocumentoWindows(path("windows.tools"),path("windows.definitions"),path("windows.jobs"),hashes,path("windows.evidence"));
        }else av=new AntivirusDocumentoLocal(path("institucional.documental.antivirus-socket"),path("institucional.documental.antivirus-configuracion"),FormatosDocumento.MAX);
        fixture=Path.of(System.getenv("DOCUMENTAL_REAL_FIXTURES"));
    }
    private Path path(String key)throws java.io.IOException{var value=props.getProperty(key);if(value==null||value.isBlank())throw new java.io.IOException("Worker real no configurado: "+key);return Path.of(value);}
    // Inicialización diferida: el control AV no requiere inventar un inspector/renderizador.
    private InspectorDocumentoLocal inspector()throws java.io.IOException{if(inspector==null)inspector=new InspectorDocumentoLocal(path("institucional.documental.inspector-socket"));return inspector;}
    private PdfAislado pdf()throws java.io.IOException{if(pdf==null)pdf=new PdfAislado(path("institucional.documental.sandbox"),temp);return pdf;}
    @Test void pdfTresPaginasSeValidaYAislaYRenderizaCadaPagina()throws Exception{var file=fixture.resolve("tres-paginas.pdf");assertTrue(av.limpio(file));assertEquals("application/pdf",inspector().examinar(file,"pdf").tipo());assertEquals(3,pdf().paginas(file));var first=pdf().pagina(file,1);var second=pdf().pagina(file,2);assertTrue(first.length>33);assertFalse(Arrays.equals(first,second));assertTrue(pdf().pagina(file,3).length>33);}
    @Test void imagenDerivadaRealTienePngYAntivirusLimpio()throws Exception{var file=fixture.resolve("imagen.png");assertTrue(av.limpio(file));var result=inspector().examinar(file,"png");assertEquals("image/png",result.tipo());assertNotNull(result.png());assertEquals((byte)137,result.png()[0]);}
    @Test void controlAntivirusRealSeRechaza()throws Exception{assertFalse(av.limpio(fixture.resolve("control-antivirus.txt")));}
    @Test void pdfActivoYExtensionFalsaNoSeRepresentan(){assertThrows(IllegalArgumentException.class,()->inspector().examinar(fixture.resolve("activo.pdf"),"pdf"));assertThrows(IllegalArgumentException.class,()->inspector().examinar(fixture.resolve("falso.png"),"png"));}
    @Test void officeSeInspeccionaSinEjecutarYReferenciasExternasSeRechazan()throws Exception{assertEquals("DESCARGA",inspector().examinar(fixture.resolve("oficina.docx"),"docx").vista());assertThrows(IllegalArgumentException.class,()->inspector().examinar(fixture.resolve("enlace-falso.docx"),"docx"));}
}
