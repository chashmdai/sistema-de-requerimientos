# U5: código estable y decisiones todavía pendientes

**U5 sigue funcionalmente parcial.** Consolidar su código técnicamente verificado enmain no habilita las decisiones pendientes ni constituye su cierre funcional. La rama `feature/u5-solicitudes-tareas-independientes` conserva su checkpoint original.

## Incluido y probado

- V13 aditiva, sin modificar V1–V12.
- Preparación personal de Solicitud vinculada a Comunicación autorizada; estados `EN_PREPARACION`/`PREPARADA`, no estados de resolución institucional.
- Borradores de respuesta/canalización/escalamiento con destinospropuestos; guardar no ejecuta la acción.
- Seguimientos, historial/snapshots, control optimista, idempotencia y relaciones con el ingreso/gestión existente.
- Tareas personales independientes, de Caso abierto o de preparación accesible; mismo creador/responsable, sin imponer tareas a terceros.
- API/React/navegación/avisos, autorización renovada en reintentos y conservación de originales.

## Cerrado en servidor e interfaz

| Operación | Comportamiento actual | Definición necesaria |
| --- | --- | --- |
| Responder Solicitud |409DEFINICION_INSTITUCIONAL_PENDIENTE después de validar acceso | Quién responde, en qué ámbito, registro/confirmación y estados |
| Canalizar Solicitud |Mismo bloqueo; no distribuye ni crea destino | Facultades, destinatarios, decisiones y trazabilidad |
| Escalar a Requerimiento |Mismo bloqueo; no abre Requerimiento/Caso ni otro COM | Quién autoriza y circuito de evaluación/conversión |

No hay excepción TI/multirrol. No deducir esas facultades de poder leer la Comunicación ni del cargo genérico de jefatura. Se exige autorización institucional explícita antes de diseñar transiciones finales. Las respuestas directas históricas de Comunicación/destino conservan sus reglas y no se reinterpretan como estas operaciones nuevas.

## Siguiente unidad funcional

1. Obtener la matriz/estados confirmados, distinguiendo decisión institucional de propuesta técnica.
2. Definir autorización por recurso/ámbito, responsabilidades y concurrencia; mantener COM/ingreso original único.
3. Agregar migraciones nuevas si son necesarias; no editar V13 para completar el modelo.
4. Implementar transiciones, UI y auditoría/avisos coherentes; pruebas negativas, recuperación e idempotencia.
5. E2E sintético aislado y regresiones del núcleo antes de habilitar.

También siguen pendientes sus unidades específicas: equipos/subrogancias, tareas delegadas si se llegaran a autorizar, notificaciones avanzadas/auditoría transversal y nuevos SLA. No añadirlos para rellenar huecos de H2.

Referencias: [contrato U5](../contratos/solicitudes-tareas-u5-laboratorio-v1.md), [validación técnica27/10](VALIDACION_TECNICA_U5_2026-10-27.md), [checkpoint27/10](CHECKPOINT_U5_2026-10-27.md), [operación](OPERACION_U5_WINDOWS.md), [reversión](REVERSION_U5_WINDOWS.md). Sus resultados históricos245 H2/245 MySQL no sustituyen la batería de H2; el estado de [Gate H2](ESTADO_GATE_H2_SIGER_2026-11-01.md) identifica lo recién ejecutado.
