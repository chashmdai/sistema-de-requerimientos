package cl.nexbyte.siger.institucional.robustez;
import java.util.*;
/** Allowlist de los destinos descartables. Nunca admite el esquema histórico importado. */
public final class DestinoJdbcPruebas {
 private DestinoJdbcPruebas(){}
 public static final String WINDOWS="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000&characterEncoding=UTF-8";
 public static void validar(String url){validar(url,System.getenv("ROBUST_DB_USER"),System.getenv("ROBUST_DB_MIGRATION_USER"));}
 public static void validar(String url,String user,String migrationUser){
  if(url!=null&&url.startsWith("jdbc:mysql://127.0.0.1:13306/robust_inst_test?"))return; // Guarda histórica Linux, sin ampliar su esquema.
  if(!WINDOWS.equals(url)||!"siger_windows_qa_app".equals(user)||!"siger_windows_qa_migrator".equals(migrationUser))throw new IllegalStateException("Solo QA descartable autorizado; no usar la copia histórica");
 }
}