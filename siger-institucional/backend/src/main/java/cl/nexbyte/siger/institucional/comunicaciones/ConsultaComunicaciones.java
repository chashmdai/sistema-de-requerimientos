package cl.nexbyte.siger.institucional.comunicaciones;
import cl.nexbyte.siger.institucional.robustez.*;
import java.time.Instant;import java.util.*;
import org.springframework.context.annotation.Profile;import org.springframework.web.bind.annotation.*;
/** Consulta técnica protegida por el control operacional existente; no es una bandeja funcionaria. */
@RestController @Profile("jdbc-reception") @RequestMapping("/operacion/v1/comunicaciones")
public class ConsultaComunicaciones {
    private final BaseJdbc db;
    public ConsultaComunicaciones(BaseJdbc db){this.db=db;}
    @GetMapping List<Comunicacion> listar(@RequestParam(defaultValue="0")long despues,@RequestParam(defaultValue="100")int limite){
        if(despues<0||limite<1||limite>200)throw new FalloTecnico(400);
        return db.sql.query("SELECT numero,comunicacion_id,registrada_ms,canal,source,operacion_id,folio_prefix,folio_width FROM communication_registry WHERE numero>? ORDER BY numero LIMIT ?",(rs,n)->new Comunicacion(UUID.fromString(rs.getString("comunicacion_id")),rs.getLong("numero"),
            rs.getString("folio_prefix")+"-"+String.format(Locale.ROOT,"%0"+rs.getInt("folio_width")+"d",rs.getLong("numero")),Instant.ofEpochMilli(rs.getLong("registrada_ms")),rs.getString("canal"),rs.getString("source"),rs.getString("operacion_id")==null?null:UUID.fromString(rs.getString("operacion_id"))),despues,limite);
    }
}
