package cl.nexbyte.siger.institucional.robustez;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class GuardasDatosTest {
 @Test void truststoreExplicitoSoloCApublicaLocalSinFallback(){
  String base="jdbc:mysql://127.0.0.1:3306/siger_institucional_windows_qa?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&characterEncoding=UTF-8";
  assertThatCode(()->ConfiguracionDatos.validar(base,false)).doesNotThrowAnyException();
  for(String invalid:java.util.List.of(base.replace("fallbackToSystemTrustStore=false","fallbackToSystemTrustStore=true"),base.replace("file:/C:/req/config/siger-mysql-truststore.p12","https://no-autorizado.invalid/ca.p12"),base.replace("/config/","/../"),base.replace("PKCS12","JKS"),base.replace("public-trust","password-no-admitida"),base.replace("VERIFY_IDENTITY","REQUIRED"),base+"&sslMode=DISABLED",base.replace("&trustCertificateKeyStoreType=PKCS12","")))assertThatThrownBy(()->ConfiguracionDatos.validar(invalid,false)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void baseFueraDelLaboratorioRequiereVerificarCAyHostname(){
  assertThatThrownBy(()->ConfiguracionDatos.validar("jdbc:mysql://servidor.institucion:3306/siger?sslMode=DISABLED",false)).isInstanceOf(IllegalArgumentException.class);
  assertThatCode(()->ConfiguracionDatos.validar("jdbc:mysql://servidor.institucion:3306/siger?sslMode=VERIFY_IDENTITY",false)).doesNotThrowAnyException();
 }
 @Test void excepcionLocalSoloEnEsquemasYPuertoDeLaboratorio(){
  assertThatCode(()->ConfiguracionDatos.validar("jdbc:mysql://127.0.0.1:13306/robust_public_local?sslMode=DISABLED",true)).doesNotThrowAnyException();
  assertThatThrownBy(()->ConfiguracionDatos.validar("jdbc:mysql://127.0.0.1:3306/siger?sslMode=DISABLED",true)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->ConfiguracionDatos.validar("jdbc:mysql://127.0.0.1:13306/robust_public_local?sslMode=DISABLED",false)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void secretosNuncaVanEnURLyAtributosNulosSeRechazan(){
  assertThatThrownBy(()->ConfiguracionDatos.validar("jdbc:mysql://servidor/siger?sslMode=VERIFY_IDENTITY&password=secreto-test",false)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new EntregaVerificada.Atributo(null,null,null,null)).isInstanceOf(IllegalArgumentException.class);
 }

 @Test void parametrosNoPuedenAnularTLSNiEnganarAlMigrador(){
  for(var query:java.util.List.of("sslMode=VERIFY_IDENTITY&sslMode=DISABLED","sslMode=DISABLED&sslMode=VERIFY_IDENTITY","sslMode=VERIFY_IDENTITY&SSLMODE=DISABLED","sslMode=VERIFY_IDENTITY&useSSL=false","sslMode=VERIFY_IDENTITY&verifyServerCertificate=false","sslMode=VERIFY_IDENTITY%26sslMode=DISABLED","%73slMode=VERIFY_IDENTITY","sslMode=VERIFY_IDENTITY;sslMode=DISABLED","sslMode=VERIFY_IDENTITY&&connectTimeout=3","sslMode=VERIFY_IDENTITY&allowPublicKeyRetrieval=true","sslMode=VERIFY_IDENTITY&autoReconnect=true")){
   for(boolean lab:java.util.List.of(false,true)){
    assertThatThrownBy(()->ConfiguracionDatos.validar("jdbc:mysql://127.0.0.1:13306/robust_public_test?"+query,lab)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->MigrarDatos.validarDestino("jdbc:mysql://127.0.0.1:13306/robust_public_test?"+query,lab)).isInstanceOf(IllegalArgumentException.class);
   }
  }
  assertThatCode(()->ConfiguracionDatos.validar("jdbc:mysql://db.ddn.gob.cl:3306/siger?connectTimeout=3000&sslMode=VERIFY_IDENTITY&socketTimeout=5000",false)).doesNotThrowAnyException();
  assertThatCode(()->MigrarDatos.validarDestino("jdbc:mysql://db.ddn.gob.cl/siger?sslMode=VERIFY_IDENTITY",false)).doesNotThrowAnyException();
  assertThatCode(()->MigrarDatos.validarDestino("jdbc:mysql://127.0.0.1:13306/robust_public_local?sslMode=DISABLED&allowPublicKeyRetrieval=true",true)).doesNotThrowAnyException();
 }
}
