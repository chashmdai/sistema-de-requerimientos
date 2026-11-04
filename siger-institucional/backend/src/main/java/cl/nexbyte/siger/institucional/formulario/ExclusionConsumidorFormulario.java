package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.sql.*;

/** Una JVM consumidora por esquema QA, aun con custodias diferentes. Conexión dedicada mientras vive el bean. */
public final class ExclusionConsumidorFormulario implements AutoCloseable {
    static final String KEY="siger:formulario-completo:siger_institucional_windows_qa";
    private final Connection connection;
    private final String key;
    public ExclusionConsumidorFormulario(BaseJdbc db){
        this(db,"siger_institucional_windows_qa","siger_windows_qa_app",KEY,true);
    }
    public ExclusionConsumidorFormulario(BaseJdbc db,String schema,String user,String origin){
        this(db,schema,user,lockKey(schema,origin),true);
    }
    private static String lockKey(String schema,String origin){try{return "smr:"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((schema+"\0"+origin).getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,60);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private ExclusionConsumidorFormulario(BaseJdbc db,String schema,String user,String lock,boolean qaCompatibility){
        key=lock;
        Connection c=null;
        try{
            c=java.util.Objects.requireNonNull(db.sql.getDataSource()).getConnection();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT DATABASE(),CURRENT_USER()")){if(!r.next()||!schema.equals(r.getString(1))||!(user.contains("@")?user:user+"@localhost").equals(r.getString(2)))throw new SQLException();}
            try(var s=c.prepareStatement("SELECT GET_LOCK(?,0)")){s.setString(1,key);try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException();}}
            connection=c;
        }catch(Exception e){if(c!=null){try(var s=c.prepareStatement("SELECT RELEASE_LOCK(?)")){s.setString(1,key);s.execute();}catch(SQLException ignored){}try{c.close();}catch(SQLException ignored){}}throw new IllegalStateException("Exclusividad del consumidor no disponible");}
    }
    public synchronized void verificar(){
        try(var s=connection.prepareStatement("SELECT IS_USED_LOCK(?)=CONNECTION_ID()")){s.setString(1,key);try(var r=s.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new SQLException();}}
        catch(SQLException e){throw new IllegalStateException("Exclusividad del consumidor QA perdida");}
    }
    @Override public synchronized void close(){
        try(var s=connection.prepareStatement("SELECT RELEASE_LOCK(?)")){s.setString(1,key);try(var r=s.executeQuery()){/* La conexión es la propietaria; jamás libera locks ajenos. */}}
        catch(SQLException ignored){}finally{try{connection.close();}catch(SQLException ignored){}}
    }
}
