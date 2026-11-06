# Notificaciones funcionales en SIGER y correo · v1

Aplica §16 del rector y la validación funcional confirmada el 06/11/2026. La columna final de EV-01..18 describe **acuse**; “sin acuse adicional” no elimina el correo. Esta precisión corrige la interpretación provisional de algunos informes anteriores, cuya evidencia de avisos internos sigue siendo válida.

Cada señal funcional emitida por la proyección crea atómicamente la notificación interna y su fila de correo **PENDIENTE**, con correlación y destinatario resuelto por autorización vigente. La selección de señales sigue en la lógica de eventos; lecturas/auditorías sin señal no producen correo. No hay preferencia individual que suprima un canal obligatorio.

Las filas históricas NO_APLICA se conservan. No se promueven a PENDIENTE ni se envían retrospectivamente por actualizar el código. Esta evolución no contiene backfill ni habilitación operacional de Graph.

Se reutilizan identidad/ámbito por recurso, idempotencia/deduplicación, intento durable anterior a red, reintentos con espera, supresión de contexto revocado y tratamiento de ambigüedad. Un resultado ACCEPTED_BY_GRAPH acredita aceptación técnica; no acredita entrega, lectura, acuse o gestión. Leer el aviso interno no confirma la recepción del correo. Un estado ambiguo no se reenvía automáticamente.

El mensaje contiene referencia/correlación y motivo genérico. No transporta relatos de NNA, títulos privados de tareas, originales o información reservada. Antes de cada intento se verifica destinatario y autorización actuales. Mantener límites, allowlist y guardas operacionales existentes; las pruebas utilizan TransporteCorreo/Graph simulados y direcciones .invalid.

Al activar inmediatamente una subrogancia, su evento de activación y la ejecución técnica de la misma transición originan un solo aviso funcional; la auditoría técnica completa se conserva. Programación futura, activación posterior, revocación/retorno y expiración mantienen sus hechos distintos.

Este contrato cubre el doble canal de los eventos ya emitidos. Los eventos EV todavía no generados y la ampliación del despachador institucional general siguen siendo brechas técnicas explícitas. Código publicado no autoriza envíos reales ni despliegues.
