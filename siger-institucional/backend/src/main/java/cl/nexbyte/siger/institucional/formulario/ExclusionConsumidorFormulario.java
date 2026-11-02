package cl.nexbyte.siger.institucional.formulario;

import cl.nexbyte.siger.institucional.robustez.BaseJdbc;
import java.sql.*;

/** Una JVM consumidora por esquema QA, aun con custodias diferentes. Conexión dedicada mientras vive el bean. */
public final class ExclusionConsumidorFormulario implements AutoCloseable {
    static final String KEY="siger:formulario-completo:siger_institucional_windows_qa";
    private final Connection connection;
    public ExclusionConsumidorFormulario(BaseJdbc db){
        Connection c=null;
        try{
            c=java.util.Objects.requireNonNull(db.sql.getDataSource()).getConnection();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT DATABASE(),CURRENT_USER()")){if(!r.next()||!"siger_institucional_windows_qa".equals(r.getString(1))||!"siger_windows_qa_app@localhost".equals(r.getString(2)))throw new SQLException();}
            try(var s=c.prepareStatement("SELECT GET_LOCK(?,0)")){s.setString(1,KEY);try(var r=s.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException();}}
            connection=c;
        }catch(Exception e){if(c!=null){try(var s=c.prepareStatement("SELECT RELEASE_LOCK(?)")){s.setString(1,KEY);s.execute();}catch(SQLException ignored){}try{c.close();}catch(SQLException ignored){}}throw new IllegalStateException("Exclusividad del consumidor QA no disponible");}
    }
    public synchronized void verificar(){
        try(var s=connection.prepareStatement("SELECT IS_USED_LOCK(?)=CONNECTION_ID()")){s.setString(1,KEY);try(var r=s.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new SQLException();}}
        catch(SQLException e){throw new IllegalStateException("Exclusividad del consumidor QA perdida");}
    }
    @Override public synchronized void close(){
        try(var s=connection.prepareStatement("SELECT RELEASE_LOCK(?)")){s.setString(1,KEY);try(var r=s.executeQuery()){/* La conexión es la propietaria; jamás libera locks ajenos. */}}
        catch(SQLException ignored){}finally{try{connection.close();}catch(SQLException ignored){}}
    }
}
