import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;import java.util.*;
import tools.jackson.databind.json.JsonMapper;
/** Cinco métodos existentes, sin dobles ni filtros por resultado. */
class DocumentalJUnitWindows {
 public static void main(String[] args)throws Exception{
  var root=Path.of(args[0]).toAbsolutePath().normalize();if(!root.toString().matches("C:\\\\req\\\\testdata\\\\documental-win-[a-f0-9]{32}"))throw new IllegalArgumentException("Solo QA");
  var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass("cl.nexbyte.siger.institucional.formulario.ServiciosDocumentalesRealesTest")).build();var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request);var s=listener.getSummary();
  var failures=new ArrayList<Object>();for(var f:s.getFailures())failures.add(Map.of("test",f.getTestIdentifier().getDisplayName(),"type",f.getException().getClass().getSimpleName(),"message",Objects.toString(f.getException().getMessage(),"")));
  var results=Map.of("scope","CINCO_DOCUMENTALES_REALES_WINDOWS","found",s.getTestsFoundCount(),"passed",s.getTestsSucceededCount(),"failed",s.getTestsFailedCount(),"skipped",s.getTestsSkippedCount(),"failures",failures,"fullU45Closed",false);
  var mapper=JsonMapper.builder().build();Files.write(root.resolve("junit-cinco.json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(results));System.out.println(mapper.writeValueAsString(results));if(s.getTestsFoundCount()!=5||s.getTestsSucceededCount()!=5||s.getTestsSkippedCount()!=0)System.exit(1);
 }
}
