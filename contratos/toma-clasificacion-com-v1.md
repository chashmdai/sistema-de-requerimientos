# Toma de Comunicación para clasificación · 1.0

Rector§07.1–07.3/16,ACC-08,DIS-02/03,REG-DIS-01 y EV-02; validación funcional06/11/2026. La toma reserva una COM para una cuenta individual. No crea destino,SOL,REQ,CAS,actuación,acuse ni respuesta ciudadana.

## Ámbito y ciclo

La bandeja se determina por procedencia conservada: web/central por distribuidor central efectivo; manual regional por coordinación de la sede de captura. No por territorio de hechos,domicilio,perfilTI o un destino que el usuario elija. El manual histórico sin procedencia acreditada permanece cerrado, sin inferirla del perfil actual. El rol de jefatura o una cuenta alias no heredan la toma.

GET /nucleo/institucional/comunicaciones/{id}/clasificacion consulta estado y últimos30cambios bajo permiso vigente, con auditoría/no-store, sin tomar o crear agregados. revision es versión opaca basada en secuencia de core_event; no se asume incremento aritmético local. revisionCom permite continuar una operación de la COM todavía pendiente; después del encaminamiento devuelve0 para no revelar revisiones de gestión ajenas.

POST .../clasificacion/{TOMAR|LIBERAR|RECUPERAR} usa Idempotency-Key,revision y fundamento cuando corresponde. Identidad y cerrojo COM existentes serializan petición, historial, revisión e intención de aviso. Una cuenta toma lo que no tiene una toma activa; solo titular exacto puede liberar. Recuperación fundada requiere autorización actual para esa bandeja y pérdida de habilitación del titular anterior (revocación,vencimiento o relevo efectivo). No existe vencimiento automático de tomas por un número de horas inventado, ni recuperación forzada sobre otra cuenta todavía habilitada.

Distribución, propuesta de vínculo y asunción de ingreso propio comprueban la toma en backend. Otra cuenta no pasa sobre ella. Encaminamiento efectivo finaliza la toma en la misma transacción, conservando su historia. Sin toma previa, la distribución autorizada existente continúa serializada por el cerrojo COM: compatibilidad de API no significa omitir una toma activa de otra cuenta. La interfaz nueva exige toma propia antes de habilitar distribución. Las rutas históricas cerradas siguen cerradas; un reintento ya confirmado conserva el resultado sin producir nuevos cambios.

## Persistencia, aviso y seguridad

Se reutilizan core_event e institution_flow_history append-only, más core_command e intention/outbox existentes. No tablas nuevas, migraciones, backfill o interpretación de eventos anteriores como tomas. No se reescriben originales, folios o firmas.

Intención separada AVISO_CLASIFICACION1.0,COM_TOMADA,para la cuenta actuante exacta y la revisión vigente. Su fuente comprueba el evento de toma/recuperación y actor de negocio. Aviso y correo pendiente son canales distintos; consultar no lee ni acusa. Liberar/finalizar/revocar/recuperar por otra cuenta retira el aviso anterior de detalles/listas/conteos y suprime el correo pendiente. Retomar con la misma cuenta no hace vigente el aviso de una toma anterior. Conservación física del registro no concede consulta actual.

EV-02 sigue parcial respecto de supervisión: no se deriva un supervisor de TI,jefatura o rol de unidad/sede. La facultad explícita de supervisión y sus destinatarios se implementarán con respaldo nominal específico; no es falta de validación del levantamiento. Este contrato habilita el circuito operativo de actuantes, no una lectura transversal de contenido.

## Interfaz y compatibilidad

EncaminarU2 incorpora TomaClasificacion con lectura, toma, liberación fundada, recuperación acreditada e historial. La distribución queda deshabilitada al cargar/denegar/perder la toma; también se comprueba al recibir submit programático. Reintento de una solicitud interrumpida conserva cuerpo y clave; cambio de COM/revocación descarta datos y respuestas tardías. Aviso de toma no presenta un destinoSOL inventado.

V1–V15.17 y RegistroAutoridad1.0–1.8 intactos. No cambia transporte Formulario,COMMIT→ACK,permisos documentales/materias/áreas posteriores o facultades de gestión. Configuración nominal y supervisión no se suponen cargadas.
