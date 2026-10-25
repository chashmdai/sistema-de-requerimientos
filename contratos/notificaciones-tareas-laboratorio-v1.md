# Contrato interno U4 — notificaciones y tareas personales de Casos
Extensión compatible de laboratorio. No modifica HTTP V3 público, firmas, COM, ACK o migraciones V1–V11.

## Tareas
La aclaración vigente exige un Caso abierto y propiedad personal. No existen tareas independientes ni tareas impuestas. La relación al Caso no cambia y creator_id=responsible_id se valida en servidor y SQL.

- GET /nucleo/tareas?estado=...: propias y con acceso actual al Caso; hasta 500.
- GET /nucleo/tareas/{id} y /historial: propiedad y recurso autorizados.
- POST /nucleo/tareas: Idempotency-Key, título/descripcion, prioridad, recordatorio, ventanaDesde/ventanaHasta, plazo; tipoRecurso=CASO y recurso obligatorio.
- PUT /nucleo/tareas/{id}: revisión esperada; no cambia Caso ni propietario.
- POST /nucleo/tareas/{id}/estado: revisión, PENDIENTE/EN_CURSO/COMPLETADA/CANCELADA. COMPLETADA exige confirmacionManual=true.
- Creador, responsable y fechas de registro/finalización son del servidor. Un responsable ajeno se rechaza aunque el actor sea TI/jefatura.
- Caso CERRADO rechaza creación/planificación nuevas; las tareas previas se conservan y pueden finalizarse/cancelarse manualmente con permiso.
- La ventana es planificación de ejecución, distinta del instante del recordatorio y del plazo. Ninguna fecha ejecuta ni constata una gestión real.

El historial conserva snapshots privados con autor/fecha/revisión; las lecturas vuelven a comprobar el recurso. No se crean actuaciones, Casos, Requerimientos ni cierres por una tarea.

## Notificaciones
- GET /nucleo/notificaciones?despues=...&limite=...: lista propia autorizada, pendientes y cursor; máximo 50 por página.
- GET /nucleo/notificaciones/contador.
- POST /nucleo/notificaciones/{id}/leer: idempotente; no aprueba una operación ni confirma un correo.
- POST /nucleo/notificaciones/operacion/procesar: TI, lote interno acotado; no hace envíos por este endpoint.
- GET/POST /nucleo/notificaciones/{id}/correo: estado/intento de esa notificación propia autorizada.
- GET /nucleo/notificaciones/{id}/intentos: auditoría sanitizada de su envío.
- POST /nucleo/notificaciones/{id}/confirmar-recepcion: declaración expresa de recepción, separada del transporte.

El id/URI de una notificación no es una credencial. Se valida destinatario, cuenta, asignaciones actuales, propiedad y autorización del recurso. Los eventos previos a V12 no generan correos retrospectivos; los commits tardíos nuevos se procesan mediante registro por evento, sin cursor máximo.

## Outbox y Graph
Notificación/outbox/registro de fuente se confirman juntos. Se recuperan reservas/intentos; una fase EN_INTENTO abandonada pasa a AMBIGUO. No se reenvía automáticamente un ambiguo ni un aceptado. Se conserva intento previo a red y resultado posterior sin acumular una transacción SQL durante HTTP.

Estados de transporte: PENDIENTE, RESERVADO, EN_INTENTO, ACCEPTED_BY_GRAPH, AMBIGUO, REINTENTABLE, RECHAZADO, BLOQUEADO_ALLOWLIST, SUPRIMIDO_CONTEXTO/PERMISOS, SIMULADO y NO_APLICA. La confirmación humana es NO_ENVIADO, PENDIENTE_CONFIRMACION_USUARIO o CONFIRMADA_POR_USUARIO.

Se resuelve dirección desde staff_account en cada intento y se comprueba allowlist. Correos genéricos con correlación, sin texto ciudadano ni de tareas. OAuth/client/sendMail usan autoridades fijas HTTPS y no siguen redirecciones. Se conserva solo fase, código HTTP, request-id validado y estado; nunca secreto, bearer, respuesta completa o Authorization.

La campaña actual admite como máximo cuatro reservas de intento real, conservadas entre reinicios. El automático está desactivado. Configuración DESACTIVADO/SIMULADO/GRAPH solo en laboratorio habilitado. Un 202 es aceptación; la recepción requiere declaración humana. No se accede al buzón ni se cambian permisos Exchange.

Los avisos previos core_notice y su URL se conservan como consulta histórica. Los nombres de roles y el control de recurso vienen del núcleo existente, no de claims o etiquetas del navegador.
