package cl.nexbyte.siger.institucional.nucleo;
import java.time.Instant;import java.util.*;
import cl.nexbyte.siger.contrato.DeclaracionNna;
import cl.nexbyte.siger.institucional.integracion.SobreComunicacion;

/** Contratos concretos del recorrido provisional; sin motor de reglas ni formularios arbitrarios. */
public final class ContratosNucleo {
    private ContratosNucleo(){}
    public record Resultado(UUID id,long revision){}
    public record Manual(String texto,String canalDeclarado,Instant recibidaEnDeclarada,String sede,String respuestaYaEntregada){@Override public String toString(){return "Manual[datos protegidos]";}}
    public record NuevoDestino(String unidad,String sede,String evaluador){}
    public record Distribucion(long revision,List<NuevoDestino> destinos){}
    public record Decision(long revision,String texto){@Override public String toString(){return "Decision[datos protegidos]";}}
    public record Asignacion(long revision,String responsable,String complejidadProvisional){}
    public record Clasificacion(long revision,String clasificacion,String fundamento){}
    public record ClasificacionHistorica(String clasificacion,String fundamento,String autor,Instant fecha){}
    public record SolicitarDerivacion(long revision,String unidad,String sede,String motivo,String antecedentesEvaluacion){}
    public record AceptarDerivacion(long revision,long revisionOrigen,UUID destinoId,Long revisionDestino,String fundamento){}
    public record DestinoCompatible(UUID id,long revision,String estado){}
    public record Derivacion(UUID id,UUID casoOrigen,UUID requerimientoId,String unidadOrigen,String sedeOrigen,String unidadDestino,String sedeDestino,
        String solicitante,Instant fechaSolicitud,long revisionOrigenSolicitada,String motivo,String antecedentesEvaluacion,String estado,long revision,long revisionOrigen,String estadoOrigen,
        String decididoPor,Instant fechaDecision,String fundamentoDecision,UUID casoDestino,List<DestinoCompatible> destinosCompatibles){}
    public record Vinculacion(UUID padre,UUID casoReferencia,String motivo){@Override public String toString(){return "Vinculacion[datos protegidos]";}}
    public record EvaluarAntecedente(long revision,long revisionCaso,String decision,String fundamento){}
    public record SolicitarReapertura(long revision,List<UUID> antecedentes,String fundamento,UUID sustituye,Long revisionSustituida){}
    public record DecidirReapertura(long revision,long revisionCaso,String fundamento){}
    public record Antecedente(UUID hija,String folio,UUID padre,UUID requerimientoId,UUID casoId,String estado,long revision,String motivo,String vinculadoPor,Instant fecha,String evaluador,String decisionFundamento,String decididoPor,Instant decididoEn,Contenido contenido){}
    public record Reapertura(UUID id,UUID casoId,UUID requerimientoId,String unidad,String sede,String solicitante,Instant fechaSolicitud,long revisionCaso,long revisionCasoActual,String responsableActual,Instant cierreAnterior,String modalidadCierreAnterior,String fundamento,List<UUID> antecedentes,String estado,long revision,String decididoPor,Instant fechaDecision,String fundamentoDecision,UUID sustituye){}
    public record Periodo(UUID id,UUID autorizacion,Instant inicio,String autorizadoPor){}
    public record CasoResumen(UUID id,UUID comunicacionId,String folio,UUID requerimientoId,String unidad,String sede,String responsable,String clasificacion,String estado,long revision,boolean derivacionPendiente,boolean reaperturaPendiente){}
    public record Pagina<T>(List<T> items,long total,int pagina,int limite){}
    public record Bandeja(long comunicaciones,long casosAsignados,long antecedentesPendientes,long sinEncaminar,long reaperturasPendientes,long derivacionesPendientes,long revisionesPendientes){}
    public record Resumen(UUID id,long secuencia,String folio,Instant registradaEn,String canal,String estado,long revision,UUID requerimientoId,String estadoRequerimiento,int casosAbiertos,int destinosPendientes){}
    public record Contenido(String texto,String tipoIdentidad,SobreComunicacion.Identidad identidad,DeclaracionNna declaracionNna,Instant recibidaEnDeclarada,String canalDeclarado,String registradoPor){}
    public record Destino(UUID id,String unidad,String sede,String evaluador,String estado,long revision,String decision,String textoDecision,String decididoPor,Instant decididoEn,int casos){}
    public record Actuacion(UUID id,String autor,Instant fecha,String texto){}
    public record Revision(UUID id,String autor,Instant fecha,String decision,String texto){}
    public record AsignacionHistorica(String responsable,String complejidadProvisional,String autor,Instant fecha){}
    public record Caso(UUID id,UUID comunicacionId,UUID requerimientoId,UUID destinoId,String unidad,String sede,String responsable,String complejidadProvisional,String estado,long revision,
        List<Actuacion> actuaciones,List<Revision> revisiones,List<AsignacionHistorica> asignaciones,String clasificacion,List<ClasificacionHistorica> clasificaciones,
        String modalidadCierre,UUID casoOrigen,boolean derivacionPendiente){}
    public record Evento(long secuencia,String tipo,UUID recurso,String autor,Instant fecha,String codigo){}
    public record Vinculo(UUID hija,UUID padre,UUID casoReferencia,String autor,Instant fecha,String motivo){}
    public record Aviso(UUID id,UUID hija,UUID padre,Instant fecha,boolean leido){}
    public record Detalle(Resumen comunicacion,Contenido contenido,List<Destino> destinos,List<Caso> casos,List<Evento> historial,List<Vinculo> vinculos,String respuestaDirecta,String decisionAutor,Instant decisionFecha,
        boolean derivacionHabilitada,boolean reaperturaHabilitada,List<String> pendientes){}
}


