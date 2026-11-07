# Avisos del núcleo institucional · 1.0

Fuente: rector28/10 §16, NOT-01/03/04/05/07 y COR-03/05; validación funcional vigente06/11/2026. Contrato privado aditivo, sin modificar transporte público1.0/1.1.

## Eventos y destinatarios

EV-03 SOL_DISTRIBUIDA informa a jefatura receptora y asignador. EV-04 SOL_REDIRIGIDA conserva origen/destino e informa a sus jefaturas y al asignador que conserve facultad de distribución en el ámbito de captura. EV-05 REQ_ASIGNADO informa al encargado real y jefatura de un REQ activo sin Caso, tanto por asunción de SOL como de ingreso propio. No se inventa un acuse adicional ni una tarea. Primera atención ya finalizada no genera falsa asignación pendiente.

Cada candidato debe conservar cuenta habilitada, identidad, mandato/concesión/área y vigencia efectiva. Coincidir en materia, unidad, TI, cuenta alias o persona canónica no concede la notificación nominativa de otra cuenta. La jefatura se verifica por ámbito vigente; la distribución regional se limita a la sede de captura. Ninguna notificación concede acceso al expediente.

## Persistencia y proyección

La misma transacción e idempotencia del comando de negocio conserva un snapshot tipado1.0 mínimo en institution_flow_history, action_code=AVISO_INSTITUCIONAL_PROGRAMADO. La intención workflow_event lleva event_key=AVISO_U2:<seq_historia>, resource_type=AVISO_U2 y resource_id=SOL/REQ reales. El seq del evento, recurso, código, versión, clase y contexto de historia se verifican conjuntamente; una fuente mal formada falla cerrada. No se duplican relatos ni originales, ni se genera una tabla paralela.

El proyector existente crea notificación individual y notification_outbox PENDIENTE de forma atómica. Mantiene límites de lote, deduplicación y cerrojo identidad→notificaciones. Fuente vigente sin destinatario queda SIN_DESTINATARIO_VIGENTE y puede reconsiderarse una vez por lote sin bloquear las demás. Fuente obsoleta queda SUPRIMIDO_SIN_PENDIENTE. Una fuente ya PROYECTADO no se reproduce; no se promueven notificaciones históricas NO_APLICA ni se envía el pasado masivamente.

EV-03 deja de ser pendiente tras redirección/asunción. EV-05 deja de ser pendiente al cerrar REQ, cambiar principal/encargado/contexto o abrir Caso. La referencia histórica creada puede seguir visible únicamente a su cuenta destinataria con la facultad contextual vigente. Se revalida antes de correo y consulta.

## API y navegación

GET /nucleo/notificaciones/{notificationUUID}/contexto-institucional exige sesión individual y notificación propia visible. Devuelve tipo, fecha, folioCOM, ámbitos origen/destino, pendienteActual y ruta opcional. No devuelve relato, clasificación, nombresNNA, documentos, Casos ni historial del expediente. Una jefatura de origen puede consultar referencia de redirección sin abrir SOL del nuevo ámbito. La ruta al recurso solo se entrega si su autorización actual permite consultarlo.

La ruta de navegador #aviso-institucional/{notificationUUID} pasa por el mismo control. Marcar leído es POST con CSRF y registra lectura; no asume SOL, cambia REQ, confirma asignación, genera actuación o cumple tarea. SOL/REQ autorizados muestran el estado de preparación/enrutamiento separado del transporte y de la lectura.

## Correo y configuración

Aviso interno y outbox son los dos canales del evento habilitado, sin preferencia para apagar uno. El correo contiene motivo genérico, correlación y enlace autenticado a la notificación; no datos del ingreso. Propiedad opcional institucional.notificaciones.enlace-base: URI HTTPS absoluta sin usuario, query ni fragmento. Sin base, un transporte que declara ser real falla409 ENLACE_INSTITUCIONAL_NO_CONFIGURADO antes de reservar intento o invocar red. Simulación puede conservar referencia relativa, sin inventar dominio.

Se reutilizan intentos durables, ambigüedad sin reenvío ciego, esperas y revalidación de destinatario. Aceptación técnica no equivale a entrega/lectura/gestión. Allowlist y presupuesto de campaña de laboratorio permanecen; este cambio no habilita Graph, envío real ni entorno productivo. Configuración nominal/operativa se prepara separadamente.

## Compatibilidad y límites

Sin nueva migración: conserva V1–V15.16 y reutiliza las tablas vigentes. No modifica firmas, custodias, folios ni COMMIT→ACK. Sin backfill de avisos anteriores a este contrato. Los demás eventos de §16 y el circuito externo de comunicación se califican por separado; este contrato no acredita la matriz completa de notificaciones ni U2–U9.
