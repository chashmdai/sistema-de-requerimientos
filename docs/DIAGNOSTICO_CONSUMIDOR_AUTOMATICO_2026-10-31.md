# De recuperación manual a consumidor automático institucional

## Alcance del dictamen

Diagnóstico del código actual de SIGER, rama local de interoperabilidad basada en `48dd236`. **No habilita automatización, servicios Windows ni producción.** La acreditación de una entrega E2E se registra separadamente; un resultado positivo de una Comunicación no demuestra disponibilidad continua, múltiples caídas ni operación productiva.

La primera entrega real aislada quedó acreditada el31/10: **COM-002497**, documento1.0/TXT, Formulario `CONFIRMADA`, ACK y originales iguales, cero duplicados. Ver [informe E2E](E2E_FORMULARIO_WSL_SIGER_WINDOWS_2026-10-31.md). Ese resultado valida el camino de datos manual; no elimina las brechas operacionales siguientes.

Después de detener el origen se verificaron otra vez registro, firmas y original en Windows, sin cliente HTTP público. Está acreditada la conservación independiente del origen. **No se ejecutó un ciclo automático contra un origen caído**: el comportamiento del planificador que se describe abajo proviene del código revisado y debe verificarse en la campaña operacional propuesta.

No hace falta reconstruir el consumidor ni completar las facultades pendientes de U5 para recibir Comunicaciones. Las extensiones deben conservar contrato 1.0/1.1, COMMIT previo al ACK, firmas, custodia e idempotencia existentes.

## Qué existe

- `FuenteFormulario`: HTTPS validado, CA configurable, Bearer técnico, sin redirects; negociación 1.0/1.1; reclama como máximo una entrega; streaming, límites e integridad; timeout de conexión 5 s y solicitudes 20 s. La transferencia se acota por reserva restante y 240 s por archivo.
- `RecuperarFormulario`: verifica antes de guardar, conserva originales antes del registro y emite ACK después del COMMIT. `AtomicBoolean` evita solapamiento dentro de una instancia. La custodia añade bloqueo de escritor por almacén.
- `FormularioInstitucionalConfiguration.Planificador`: opt-in `automatico`, sondeo de 5 s por defecto, consulta de `operation_control`, conciliación previa al siguiente reclamo y backoff de 10 s hasta 5 min. Estado del sondeo en memoria; no se registra el error que impide reclamar.
- `institutional_inbox`: comprobante firmado y estados persistentes de ACK, reserva, intentos, próximo intento y bloqueo. Reintentos de ACK exponenciales; tres fallos persistentes o hasta ocho transitorios requieren revisión. Existe intervención operacional autenticada para un ACK bloqueado.
- Reservas vencidas: se abandona la reserva anterior y una nueva reclamación puede recuperar el mismo COM y originales. UUID público/registro único impide duplicar Comunicación y folio.
- Windows QA: Java 25, HTTPS loopback, credenciales DPAPI/stdin, aplicación DML y Flyway separado. El bootstrap **fuerza `automatico=false`** y el launcher es temporal: no es un servicio supervisor.

## Lo necesario antes de habilitar un automático supervisado

| Área | Extensión concreta | Criterio de aceptación |
|---|---|---|
| Arranque | Modo automático explícito distinto del opt-in manual; origen/trust fijos, preflight de custodia/DB/certificados y exclusividad. Registrar quién lo habilita. | Un reinicio no cambia origen, identidad técnica o custodia; modo cerrado sigue siendo predeterminado. |
| Supervisión Windows | Elegir un mecanismo institucional aprobado para supervisar el proceso, con identidad mínima, reinicio acotado, logs privados y parada con drenaje. No instalarlo en esta campaña. | Arranque/reinicio reproducibles; se detiene el sondeo, termina o recupera la operación y se cierran recursos. No matar procesos por nombre. |
| Secretos | DPAPI bajo la identidad real de ejecución, no suponer que SYSTEM/otro usuario puede leer el secreto del operador. Procedimiento de rotación de token/CA/claves sin mostrar valores. | Prueba con esa identidad, permisos mínimos y confianza correcta; sin contraseñas en argumentos, logs o Git. |
| Sondeo | Reutilizar el planificador existente, con jitter, clasificación de fallos, estado de último intento/éxito y backoff observable. Mantener lote y concurrencia acotados. | Caída prolongada no produce bucle rápido ni impide trabajo institucional local; recuperación sin avalancha. |
| Reintentos | Distinguir ausencia de entregas, red/5xx/408, 429 y fallos permanentes de credencial/TLS/contrato. Considerar `Retry-After`. | Fallos permanentes generan alerta y revisión; no se interpretan como cola vacía ni se corrigen desactivando TLS. |
| Conciliación | Priorizar ACK pendientes; conservar el comprobante original y revalidar la custodia. Probar caída después de COMMIT y antes de guardar reserva, después de ACK público y antes de marcarlo recibido. | Un UUID mantiene COM, folio, firma y originales. ACK perdido se concilia; un archivo alterado nunca se confirma. |
| Exclusividad | Una instancia activa por consumidor/custodia; no confiar solamente en `AtomicBoolean` para varias JVM. Evitar apuntar la misma DB a archivos distintos. | Un segundo proceso no consume en paralelo con un almacén diferente ni disputa reconciliaciones. |
| Origen | Una DB/cola de conciliación debe estar asociada al origen autorizado. Prohibir cambio de perfil con pendientes ajenos; si se admiten varios productores, vincular explícitamente sus pendientes al productor. | Nunca enviar un ACK de QA o de otro productor al Formulario equivocado. |
| Observabilidad | Métricas específicas del consumidor **completo** y alertas: último éxito, último fallo sanitizado, próximos intentos, ACK pendientes/bloqueados, antigüedad, integridad y espacio de custodia. | No confundir `/operacion/v1/recuperacion/metricas` actual (V2 histórico) con la salud de 1.0/1.1. Sin cuerpos, tokens ni datos personales en telemetría. |
| Almacenamiento | Monitorear capacidad, temporales y originales sin transacción SQL confirmada; procedimiento de recuperación bajo el lock existente. Respaldo consistente DB+archivos. | No borrar originales ni ACK para “desatascar” el proceso; no activar retención pública ni depender del agente independiente. |
| Pruebas operacionales | Campaña acotada con proceso público aislado: corte de red, Formulario detenido, reserva vencida, COMMIT/ACK ambiguos, disco/DB no disponibles y reinicio de ambos. | Cero COM duplicados, cero ACK prematuros y recuperación comprobada en ambos sistemas. Los dobles siguen sirviendo para fallos sin tráfico institucional. |

La complejidad debe permanecer proporcionada: un único consumidor supervisado, lote pequeño, estado persistente que ya existe y métricas útiles. No se justifica incorporar un bus de mensajes o un orquestador nuevo para este primer automático.

## Comportamiento durante indisponibilidad

| Situación | Comportamiento actual / requerido |
|---|---|
| Formulario no escucha o red cae | No hay registro ni ACK nuevo; el cliente falla y el planificador retrocede. El core institucional con datos ya conservados debe seguir operando. Hoy falta visibilidad específica de este fallo. |
| TLS/certificado inválido | Falla cerrado. No seguir redirects ni omitir validación. Si la CA configurada es inválida, la creación del bean puede impedir el arranque con consumidor habilitado: prever arranque institucional con recuperación cerrada y alarma, sin aceptar un origen inseguro. |
| Credencial revocada / 401–403 | No reintentar indefinidamente como si fuera una caída transitoria. Alertar y renovar por mecanismo privado. No imprimir respuesta ni cabecera Authorization. |
| ACK responde con timeout después de COMMIT | Conservar Comunicación y recibo, dejar pendiente y conciliar el mismo registro. No emitir otro folio ni volver a enviar el formulario ciudadano. |
| Reserva vence | No forzar ACK con la reserva vieja. Reclamación posterior y registro idempotente permiten continuar; verificar que los originales siguen íntegros. |
| SIGER reinicia | Pendientes SQL y originales sobreviven; el backoff del sondeo se pierde porque hoy es memoria. La exclusividad y el drenaje del launcher deben evolucionar antes del automático. |
| Firma o archivo inválido | Rechazo sin ACK y evento sanitizado. No mantener un documento inválido en un bucle de reintentos sin revisión del contrato/origen. |

## Orden recomendado

1. Acreditar la entrega manual E2E aislada y su persistencia, sin automatización.
2. Definir operación supervisada e identidad Windows autorizada; implementar únicamente el modo automático explícito, drenaje, exclusividad y métricas que faltan.
3. Validar fallos y recuperación con datos sintéticos, aprobar la operación y recién entonces habilitar el sondeo del laboratorio.

No se fijan nuevos SLA, facultades U5, políticas de retención o disponibilidades institucionales. No se registró servicio permanente ni tarea programada, ni se modificó MySQL80 global, firewall, Kaspersky o WSL.
