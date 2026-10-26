package cl.nexbyte.siger.institucional.robustez;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class DestinoJdbcPruebasTest {
 @Test void qaWindowsExigeIdentidadesSeparadasYVerificacionTls(){assertDoesNotThrow(()->DestinoJdbcPruebas.validar(DestinoJdbcPruebas.WINDOWS,"siger_windows_qa_app","siger_windows_qa_migrator"));}
 @Test void esquemaHistoricoNuncaEsDestinoDescartable(){assertThrows(IllegalStateException.class,()->DestinoJdbcPruebas.validar(DestinoJdbcPruebas.WINDOWS.replace("windows_qa?","windows_lab?"),"siger_windows_qa_app","siger_windows_qa_migrator"));}
 @Test void tlsDegradadoYParametrosAdicionalesSeRechazan(){for(String u:new String[]{DestinoJdbcPruebas.WINDOWS.replace("VERIFY_IDENTITY","DISABLED"),DestinoJdbcPruebas.WINDOWS+"&sslMode=DISABLED",DestinoJdbcPruebas.WINDOWS.replace("127.0.0.1","otro.host")})assertThrows(IllegalStateException.class,()->DestinoJdbcPruebas.validar(u,"siger_windows_qa_app","siger_windows_qa_migrator"));}
 @Test void cuentasHistoricasORootNoSeAceptan(){for(String u:new String[]{"root","siger_windows_app","siger_windows_migrator"})assertThrows(IllegalStateException.class,()->DestinoJdbcPruebas.validar(DestinoJdbcPruebas.WINDOWS,u,"siger_windows_qa_migrator"));}
}