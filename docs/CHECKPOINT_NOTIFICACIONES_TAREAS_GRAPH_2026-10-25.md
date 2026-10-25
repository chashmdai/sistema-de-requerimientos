# Checkpoint — cuarta unidad, 25/10/2026
**Unidad cerrada funcionalmente en laboratorio. Recepción de los cuatro correos confirmada por el usuario. Producción no habilitada.**

- Repositorio: /home/nexbyteproveedor/proyectos/sistema-de-requerimientos.
- Rama: notificaciones-tareas-graph-laboratorio-2026.
- Base preservada: e0b002c / f665a1c.
- Implementación: f619b97. El siguiente commit contiene este cierre; consultar git log -2 --oneline.
- Sin push, sin modificaciones a formulario-web ni a su configuración.

## Alcance vigente
La aclaración del responsable sustituyó tareas independientes/impuestas: cada tarea nace desde un Caso abierto, pertenece al funcionario creador y no se transfiere. Mis tareas de Casos es agregación; no crea tareas sueltas. Recordatorio, ventana y plazo son distintos. La finalización es manual; no genera actuaciones ni cierres.

Notificaciones persistentes por identidad y recurso, campana/bandeja, lectura separada de aprobación, deduplicación, proyección transaccional de eventos existentes y outbox recuperable. Inactivos, revocaciones y reasignaciones no reciben contenido por una notificación. Los avisos históricos se conservan.

## Cuenta e infraestructura conservadas
Una cuenta nexbyte, Nexbyte Proveedor, nexbyte.proveedor@defensorianinez.cl, activa; siete roles en ámbitos sintéticos explícitos y una sola identidad. No hay bypass de superusuario ni contraseñas compartidas. Contraseña local en custodia privada 0600; no se imprimió ni reemplazó una clave previa. Cuentas restringidas preservadas.

HTTPS loopback 20443; esquema integrado robust_inst_formulario_lab y aislado robust_inst_test; mismo almacenamiento y workers. JAR activo fuera de target en el directorio privado institucional y coincidente con el build final. V12 aplicada; V1–V11 (22 archivos) intactas.

## Graph real y confirmación
Cuatro envíos secuenciales, todos ACCEPTED_BY_GRAPH/202, hacia el correo resuelto desde MySQL y validado contra allowlist privada:
- CASO_ASIGNADO: 26f9d0a6-ed4c-4409-b426-7239d703ddd2.
- ANTECEDENTE_NUEVO: e24fe275-57d4-43f5-bb2c-f05a7ea64b74.
- TAREA_RECORDATORIO: 2775b415-3ffc-48ca-a85b-4814553113c2.
- TAREA_VENCIDA: 8d5ed2b3-94c0-43a6-a22d-82957051722c.

Se pidió confirmar y el usuario respondió «Recibí los cuatro». Las cuatro filas tienen CONFIRMADA_POR_USUARIO con actor/fecha. Esa declaración se distingue del estado de transporte; no se accedió al buzón. No hay confirmación humana pendiente de esta campaña.

Correo en modo GRAPH, automático=false, allowlist única y campaña u4_20261025_v1 agotada en cuatro intentos. **No reenviar, no resetear presupuesto ni crear otra campaña sin autorización.** Un resultado ambiguo no se reintenta automáticamente.

## Evidencias comprobadas
- Backend: 194 H2 y 194 MySQL correctas; cero fallos/errores/omitidas en selección final. Cinco tests de workers reales por motor; tres exclusiones de carga verificadas.
- Frontend: 42 correctas; lint/build correctos.
- E2E real Edge/HTTPS/MySQL, escritorio/móvil, repetido tras reinicio sin duplicar registros ni reenviar correos.
- Caso propio, TI, distribución, jefatura/profesional, tareas desde Caso, fechas, cierre manual sintético, notificaciones y reasignación.
- Otra identidad/TI no consulta tarea ajena; imposición 403, tarea sin Caso 400, CSRF 403.
- Otra cuenta quedó BLOQUEADO_ALLOWLIST antes de intentar transporte; cero correos a destinos diferentes.
- Nueve Comunicaciones previas y siete documentos públicos/comprobantes/ACK conservados. Dieciséis originales ciudadanos, dos de gestión, seis páginas íntegros; cero temporales/ACK pendientes.
- Estado integrado: once COM únicos, cinco Casos, tres Requerimientos y tres tareas personales de QA. Ninguna tarea impuesta/sin Caso; ningún Caso cerrado por tareas.

## Reanudación
Conservar .local/evidencias/notificaciones/estado.json, baseline y evidencias. El E2E interno Probar-Notificaciones-Tareas.mjs no envía correo por defecto. No borrar su estado al repetir. El verificador SELECT comprueba conservación/Graph; las credenciales solo se leen en sus archivos privados.

Se corrigieron el rollback por comprobaciones de autorización anidadas, una incompatibilidad multirrol de la consulta de reaperturas, una carrera de cabeceras antes del streaming y estados transitorios de carga. No se relajaron permisos ni se modificaron firmas o documentos.

Próximo paso: la siguiente unidad institucional autorizada. SSO, subrogancias definitivas, nuevos SLA, capacidad/recuperación externa y hardening productivo quedan fuera de esta entrega. No hay bloqueos pendientes de Graph ni de confirmación humana para la campaña ejecutada.

[Informe](CIERRE_NOTIFICACIONES_TAREAS_GRAPH_2026-10-25.md) · [Resultados](RESULTADOS_NOTIFICACIONES_TAREAS_GRAPH_2026-10-25.json) · [Evidencia de correo](EVIDENCIA_GRAPH_2026-10-25.json) · [Reversión](REVERSION_NOTIFICACIONES_TAREAS_GRAPH.md).
