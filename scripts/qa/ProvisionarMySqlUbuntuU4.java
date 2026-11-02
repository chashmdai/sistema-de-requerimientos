import java.sql.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Administración U4 acotada. Root solo por stdin, nunca disponible al consumidor. Sin ALTER USER ni DROP. */
class ProvisionarMySqlUbuntuU4 {
    public static void main(String[] args) {
        var json = JsonMapper.builder().build();
        String phase = "INPUT";
        try {
            if (args.length != 0 || !System.getProperty("os.name").startsWith("Windows")) throw new IllegalArgumentException();
            var p = json.readTree(System.in); if (!"root".equals(p.path("username").asString())) throw new IllegalArgumentException();
            String url = "jdbc:mysql://127.0.0.1:3306/?sslMode=VERIFY_IDENTITY&fallbackToSystemTrustStore=false&trustCertificateKeyStoreUrl=file:/C:/req/config/siger-mysql-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=public-trust&connectTimeout=5000&socketTimeout=15000";
            phase = "CONNECT";
            try (var c = DriverManager.getConnection(url, "root", p.path("password").asString()); var s = c.createStatement()) {
                s.setQueryTimeout(10);
                phase = "IDENTITY";
                try (var r = s.executeQuery("SELECT CURRENT_USER()")) { if (!r.next() || !"root@localhost".equals(r.getString(1))) throw new IllegalStateException(); }
                phase = "TLS"; String tls;
                try (var r = s.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_version'")) { r.next(); tls = r.getString(2); if (!Set.of("TLSv1.2", "TLSv1.3").contains(tls)) throw new IllegalStateException(); }
                phase = "GRANTS"; boolean admin = false;
                try (var r = s.executeQuery("SHOW GRANTS")) { while (r.next()) {
                    String g=r.getString(1); int on=g.indexOf(" ON *.* TO ");
                    if (g.startsWith("GRANT ") && on>6 && g.endsWith(" WITH GRANT OPTION")) {
                        var privileges = new HashSet<>(Arrays.asList(g.substring(6,on).split(", ")));
                        admin |= privileges.contains("ALL PRIVILEGES") || privileges.containsAll(Set.of("CREATE USER","CREATE","SELECT","INSERT","UPDATE","DELETE","ALTER","DROP","INDEX","REFERENCES","CREATE VIEW"));
                    }
                } }
                if (!admin) throw new IllegalStateException();
                phase = "COLLISIONS"; long schemas, users;
                try (var r = s.executeQuery("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='siger_u4_ddn_ubuntu'")) { r.next(); schemas=r.getLong(1); }
                try (var r = s.executeQuery("SELECT COUNT(*) FROM mysql.user WHERE User IN ('siger_u4_ubuntu_app','siger_u4_ubuntu_migrator')")) { r.next(); users=r.getLong(1); }
                boolean provision = "PROVISIONAR".equals(p.path("accion").asString());
                if (provision) {
                    if (schemas != 0 || users != 0) throw new IllegalStateException();
                    String app = p.path("appPassword").asString(), migrator = p.path("migratorPassword").asString();
                    if (!app.matches("[a-f0-9]{64}") || !migrator.matches("[a-f0-9]{64}") || app.equals(migrator)) throw new IllegalArgumentException();
                    phase = "PROVISION";
                    s.execute("CREATE DATABASE siger_u4_ddn_ubuntu CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                    s.execute("CREATE USER 'siger_u4_ubuntu_app'@'localhost' IDENTIFIED BY '" + app + "' REQUIRE SSL");
                    s.execute("CREATE USER 'siger_u4_ubuntu_migrator'@'localhost' IDENTIFIED BY '" + migrator + "' REQUIRE SSL");
                    s.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON siger_u4_ddn_ubuntu.* TO 'siger_u4_ubuntu_app'@'localhost'");
                    s.execute("GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES, CREATE VIEW ON siger_u4_ddn_ubuntu.* TO 'siger_u4_ubuntu_migrator'@'localhost'");
                } else if (!"INSPECCIONAR".equals(p.path("accion").asString())) throw new IllegalArgumentException();
                System.out.println(json.writeValueAsString(Map.of("ok",true,"adminTls",tls,"schemaExistsBefore",schemas,"userCollisionsBefore",users,"provisioned",provision,"scope","SOLO_U4_NUEVO")));
            }
        } catch (Exception rejected) {
            System.out.println(json.writeValueAsString(Map.of("ok",false,"code","APROVISIONAMIENTO_U4_RECHAZADO","phase",phase,"errorClass",rejected.getClass().getSimpleName(),"sqlCode",rejected instanceof SQLException sql ? sql.getErrorCode() : 0)));
            System.exit(1);
        }
    }
}
