import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.security.*;
import cl.nexbyte.siger.institucional.archivos.ArchivoPrivado;
import cl.nexbyte.siger.institucional.formulario.*;
import cl.nexbyte.siger.institucional.robustez.*;
import cl.nexbyte.siger.institucional.formulario.ExclusionEnsayoUbuntu;
import tools.jackson.databind.json.JsonMapper;

/** Preflight U4: vincula origen/almacén nuevos; control CAS solo con acción explícita. Sin fixtures. */
class VerificarMySqlUbuntuU4 {
    public static void main(String[] args) {
        var json=JsonMapper.builder().build();
        try {
            var input=json.readTree(System.in);String user=input.path("username").asString();
            if(args.length!=0||!Set.of("siger_u4_ubuntu_app","siger_u4_ubuntu_migrator").contains(user))throw new IllegalArgumentException();
            String url="jdbc:mysql://127.0.0.1:3306/siger_u4_ddn_ubuntu?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000";
            var out=new TreeMap<String,Object>();
            try(var c=DriverManager.getConnection(url,user,input.path("password").asString());var s=c.createStatement()){
                s.setQueryTimeout(5);
                try(var r=s.executeQuery("SELECT CURRENT_USER(),DATABASE(),VERSION()")){r.next();if(!(user+"@localhost").equals(r.getString(1))||!"siger_u4_ddn_ubuntu".equals(r.getString(2)))throw new IllegalStateException();out.put("user",r.getString(1));out.put("schema",r.getString(2));out.put("version",r.getString(3));}
                try(var r=s.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_version'")){r.next();if(!Set.of("TLSv1.2","TLSv1.3").contains(r.getString(2)))throw new IllegalStateException();out.put("tls",r.getString(2));}
                var privileges=new HashSet<String>();var grants=new ArrayList<String>();
                try(var r=s.executeQuery("SHOW GRANTS")){while(r.next()){String g=r.getString(1);grants.add(g);if(g.startsWith("GRANT USAGE ON *.* TO "))continue;int on=g.indexOf(" ON ");if(on<0||!g.substring(on).startsWith(" ON `siger_u4_ddn_ubuntu`.* TO ")||g.contains(" WITH GRANT OPTION"))throw new IllegalStateException();privileges.addAll(Arrays.asList(g.substring(6,on).split(", ")));}}
                var expected=new HashSet<>(Set.of("SELECT","INSERT","UPDATE","DELETE"));if(user.endsWith("migrator"))expected.addAll(Set.of("CREATE","ALTER","DROP","INDEX","REFERENCES","CREATE VIEW"));if(!privileges.equals(expected))throw new IllegalStateException();out.put("grants",grants);
                for(var schema:List.of("siger_institucional_windows_qa","siger_institucional_windows_lab")){
                    boolean denied=false;try(var r=s.executeQuery("SELECT 1 FROM "+schema+".institutional_inbox LIMIT 0")){/* No se lee contenido. */}catch(SQLException failure){denied=failure.getErrorCode()==1142;}
                    if(!denied)throw new IllegalStateException();out.put(schema+"AccessDenied",true);
                }
                try(var r=s.executeQuery("SELECT COUNT(*) FROM institutional_inbox")){r.next();out.put("inbox",r.getLong(1));}
                try(var r=s.executeQuery("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE")){r.next();out.put("ackPending",r.getLong(1));}
            }
            if(user.endsWith("app"))try(var db=new BaseJdbc(url,user,input.path("password").asString(),4)){
                try(var lock=new ExclusionEnsayoUbuntu(db)){
                    lock.verificar();boolean rejected=false;try(var competing=new ExclusionEnsayoUbuntu(db)){throw new IllegalStateException("Segunda instancia admitida");}catch(IllegalStateException expected){rejected=!"Segunda instancia admitida".equals(expected.getMessage());}
                    if(!rejected)throw new IllegalStateException();lock.verificar();out.put("competingConsumerRejected",true);
                    var profilePath=ArchivoPrivado.validar(Path.of("C:\\req\\config\\formulario-ddn-ubuntu-u4\\perfil.json"),65536);
                    var profile=json.readTree(Files.readString(profilePath));
                    if(!"ENSAYO_SINTETICO_U4_DDN_UBUNTU".equals(profile.path("scope").asString())||!"https://formulario.defensorianinez.gob.cl:25443".equals(profile.path("origin").asString())||!profile.path("archivo").asString().matches("C:\\\\req\\\\testdata\\\\u4-ddn-ubuntu-[a-f0-9]{32}\\\\archivo"))throw new IllegalStateException();
                    for(var pair:List.of(Map.entry("ca","caSHA256"),Map.entry("publicTrust","publicTrustSHA256"))){var file=ArchivoPrivado.validar(Path.of(profile.path(pair.getKey()).asString()),65536);if(!hash(file).equals(profile.path(pair.getValue()).asString()))throw new IllegalStateException();}
                    var own=ArchivoPrivado.validar(Path.of(profile.path("ownTrust").asString()),65536);
                    VinculoOrigenEnsayo.comprobar(db,VinculoOrigenEnsayo.huella(profile.path("origin").asString(),profile.path("caSHA256").asString(),profile.path("publicTrustSHA256").asString(),profile.path("keyId").asString(),hash(own),profile.path("archivo").asString()));
                    out.put("originBound",true);
                    try(var files=new ArchivoInstitucional(Path.of(profile.path("archivo").asString()))){out.put("privateNtfsCustodyReady",true);}
                    var control=new ControlesOperacion(db);String action=input.path("control").asString("ESTADO");
                    if(!Set.of("ESTADO","ABRIR","CERRAR").contains(action))throw new IllegalArgumentException();
                    var state=control.estado();
                    if(!"ESTADO".equals(action)){
                        boolean open="ABRIR".equals(action);if(open&&!input.path("syntheticCoordinated").asBoolean())throw new IllegalArgumentException();
                        state=control.cambiar(state.revision(),open,open,open?"NORMAL":"SOLO_CONSULTA","PRUEBA_LOCAL");
                    }
                    out.put("control",state);
                }
                try(var resumed=new ExclusionEnsayoUbuntu(db)){resumed.verificar();out.put("locksReleasedAndReacquired",true);}
            }
            System.out.println(json.writeValueAsString(out));
        }catch(Exception failure){System.out.println(json.writeValueAsString(Map.of("ok",false,"errorClass",failure.getClass().getSimpleName(),"sqlCode",failure instanceof SQLException sql?sql.getErrorCode():0)));System.exit(1);}
    }
    private static String hash(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
}
