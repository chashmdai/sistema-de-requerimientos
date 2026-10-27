# U5 · Preparación de Solicitudes y tareas personales

26/10/2026. Contrato interno de laboratorio, aditivo a U1–U4.5. No cambia recepción pública 1.0/1.1, HTTP V3, firmas, COM, ACK ni el ciclo de Requerimientos/Casos.

## Límite institucional explícito

El usuario indicó continuar y dejar pendientes las facultades para responder/canalizar una Solicitud y autorizar su escalamiento. Esas tres decisiones están cerradas en el servidor con `409 DEFINICION_INSTITUCIONAL_PENDIENTE`, después de verificar acceso al recurso. No hay excepción TI o multirrol. No se envía respuesta, distribuye un destino, crea Requerimiento/Caso ni emite otro COM.

La entidad implementada es una **preparación de Solicitud**, personal y vinculada a una Comunicación autorizada. Estados técnicos: `EN_PREPARACION` y `PREPARADA`. No son estados de resolución institucional. Una preparación puede guardar un borrador `RESPUESTA`, `CANALIZACION` o `ESCALAMIENTO`; los dos últimos conservan un destino propuesto del catálogo vigente. Guardar no ejecuta la acción.

Mientras falta la matriz, se exige cuenta individual activa, autor/responsable personal y permiso actual de lectura del ingreso. No se deducen facultades de decisión de ese permiso. Esta frontera conservadora no define al responsable único como lector institucional definitivo. El punto de extensión está en `RecursosNotificacion.solicitud`; deberá reemplazarse por la matriz confirmada y sus pruebas antes de habilitar decisiones o lectores adicionales.

Las respuestas directas históricas de Comunicación/destino conservan sus reglas acreditadas. No se convierten silenciosamente en operaciones de la nueva entidad.

## API interna autenticada

Todas las rutas pertenecen a `/nucleo`, exigen HTTPS y sesión vigente. Mutaciones con CSRF; crear/editar/preparar/seguir requieren `Idempotency-Key` UUID. Clave repetida con idéntico actor, operación y datos recupera el resultado original; su reutilización incompatible produce 409. La autorización se evalúa de nuevo en el reintento. Campos desconocidos se rechazan.

| Ruta | Comportamiento |
|---|---|
| GET `/solicitudes?comunicacion=UUID` | Hasta 500 preparaciones personales visibles; filtro opcional, ingreso autorizado |
| POST `/solicitudes` | `{revision:0,comunicacion,titulo,descripcion}`; autor, responsable, fecha y ámbito derivados del servidor |
| GET/PUT `/solicitudes/{id}` | Lectura/edición optimista; origen y responsable inmutables |
| POST `/solicitudes/{id}/preparacion` | `{revision,tipo,texto,unidadDestino,sedeDestino}`; solo guarda borrador |
| GET/POST `/solicitudes/{id}/seguimientos` | Nota privada, autor/fecha servidor; POST `{revision,texto}` |
| GET `/solicitudes/{id}/historial` | Hasta 100 snapshots recientes con autor, evento, fecha y revisión |
| GET `/solicitudes/{id}/relaciones` | Referencias existentes al ingreso y Requerimientos; Casos filtrados por su autorización propia |
| POST `/solicitudes/{id}/responder`, `/canalizar`, `/escalar` | 409 por definición pendiente; 404/403 si no hay autorización |

Título hasta 180 caracteres; descripción, borrador y seguimiento hasta 2000. El relato ciudadano íntegro permanece en su ingreso original. No hay archivos copiados: la consulta documental conserva la autorización de Comunicación/Caso existente. No se incorpora un canal de adjuntos ni actuaciones de Solicitud sin su definición.

## Tareas personales

Se amplía `/nucleo/tareas` sin cambiar las operaciones existentes. `tipoRecurso/recurso` admiten:

- `null/null`: tarea independiente; no crea Comunicación, Requerimiento ni Caso.
- `CASO/UUID`: comportamiento U4, con acceso vigente y creación solo en Caso abierto.
- `SOLICITUD/UUID`: tarea personal de una preparación accesible.

El responsable sigue siendo el propio creador. No se delegan tareas, incluso por TI o jefatura. El contexto se fija al crear y no se cambia al editar. Se mantienen prioridades, recordatorio, ventana, plazo, estados, historial y confirmación manual de finalización; ninguna tarea ejecuta una gestión ni cierra su recurso.

Una cuenta activa sin facultades operativas puede gestionar sus propias tareas independientes; eso no concede acceso a un ingreso, Caso o documento. Un cambio de rol no elimina sus datos personales, pero invalida sesiones anteriores y vuelve a evaluar los recursos vinculados. Desactivar la cuenta impide también el acceso a tareas independientes.

## Persistencia y avisos

V13 agrega `institution_request`, `request_history`, `request_followup`, `request_command` y `institution_task.request_id`; permite contexto nulo conservando las FK/checks de Caso previos y agrega una restricción contra contextos ambiguos. No modifica V1–V12. No hay migración destructiva ni actualización del histórico Windows.

Identidad, revisión, operación idempotente, historial y evento se confirman en una transacción. Los bloqueos siguen el guard de identidad existente. Seguimientos se agregan; no sustituyen notas anteriores. Relaciones se consultan por referencias, sin duplicar ingresos ni documentos.

Eventos de preparación proyectados a avisos persistentes personales con categorías `SOLICITUD_CREADA`, `SOLICITUD_MODIFICADA`, `SOLICITUD_PREPARADA`, `SOLICITUD_SEGUIMIENTO`. El mismo outbox transaccional se conserva: esos avisos tienen correo `NO_APLICA`. Recordatorios/vencimientos reutilizan el motor de tareas y transporte **SIMULADO** en Windows QA. Los textos reservados no forman parte del correo. La notificación nunca concede acceso.

## Decisiones pendientes

Facultades y ámbitos de decisión, responsables institucionales distintos del autor, transiciones finales, canal de respuesta, autorizadores de escalamiento, plazos institucionales y documentos/actuaciones específicos de Solicitud. No se propone ni habilita una matriz definitiva en esta entrega. No se sustituye el circuito acreditado de Casos ni se introduce un nuevo SLA.
