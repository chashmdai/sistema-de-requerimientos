import cl.nexbyte.siger.institucional.documental.AntivirusDocumentoWindows;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;

/** Launcher JUnit ya disponible: un método real, sin mantener Maven en memoria durante ClamAV. */
class ControlAntivirusJUnitWindows {
    public static void main(String[] args)throws Exception {
        var root=Path.of(args[0]).toAbsolutePath().normalize();
        if(!root.toString().matches("C:\\\\req\\\\testdata\\\\av-junit-[a-f0-9]{32}"))throw new IllegalArgumentException("Solo QA");
        var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectMethod("cl.nexbyte.siger.institucional.formulario.ServiciosDocumentalesRealesTest#controlAntivirusRealSeRechaza")).build();
        var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request);var s=listener.getSummary();
        boolean passed=s.getTestsFoundCount()==1&&s.getTestsSucceededCount()==1&&s.getTestsFailedCount()==0&&s.getTestsSkippedCount()==0&&s.getTestsAbortedCount()==0;
        String extra="";boolean memory=false;
        for(var f:s.getFailures()){var t=f.getException();if(t instanceof AntivirusDocumentoWindows.MemoriaInsuficiente m){memory=true;extra=",\"availablePhysicalBytes\":"+m.disponible()+",\"requiredFreeBytes\":"+m.requerida();}else extra=",\"failureClass\":\""+t.getClass().getSimpleName()+"\"";}
        String json="{\"scope\":\"JUNIT_AV_REAL_WINDOWS\",\"state\":\""+(passed?"APROBADO":memory?"BLOQUEADO_RECURSOS":"NO_ACREDITADO")+"\",\"found\":"+s.getTestsFoundCount()+",\"passed\":"+s.getTestsSucceededCount()+",\"failed\":"+s.getTestsFailedCount()+",\"skipped\":"+s.getTestsSkippedCount()+",\"fullDocumentalRuntime\":false"+extra+"}";
        Files.writeString(root.resolve("junit-resultado.json"),json);System.out.println(json);if(!passed)System.exit(1);
    }
}
