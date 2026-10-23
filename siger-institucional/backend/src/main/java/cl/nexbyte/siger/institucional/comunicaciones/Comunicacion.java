package cl.nexbyte.siger.institucional.comunicaciones;
import java.time.Instant;import java.util.UUID;
/** Registro mínimo. No define estados, asignaciones ni relación Caso/Requerimiento. */
public record Comunicacion(UUID id,long numeroFolio,String folio,Instant registradaEn,String canalOrigen,
        String origenTecnico,UUID operacionPublicaId) {}
