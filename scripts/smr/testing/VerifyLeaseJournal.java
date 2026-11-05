import cl.nexbyte.siger.institucional.robustez.*;import cl.nexbyte.siger.institucional.formulario.*;import java.util.*;
class VerifyLeaseJournal {
 public static void main(String[] args)throws Exception{var json=EntregaCompleta.lector();var p=json.readTree(System.in);try(var db=new BaseJdbc(p.path("url").asString(),p.path("user").asString(),p.path("password").asString(),2)){
  long rows=db.sql.queryForObject("SELECT COUNT(DISTINCT i.id) FROM institutional_inbox i JOIN operational_event e ON e.aggregate_id=i.id AND e.category='FORMULARIO_RESERVA' WHERE e.code LIKE CONCAT(i.ack_lease,'|%')",Long.class);
  if(rows<1)throw new IllegalStateException("Reserva/vencimiento/intento no recuperables");System.out.println(json.writeValueAsString(Map.of("leaseMetadataDurable",rows,"records",db.sql.queryForObject("SELECT COUNT(*) FROM communication_registry",Long.class),"pendingAck",db.sql.queryForObject("SELECT COUNT(*) FROM institutional_inbox WHERE ack_pending=TRUE",Long.class),"tokensExposed",false)));
 }}
}
