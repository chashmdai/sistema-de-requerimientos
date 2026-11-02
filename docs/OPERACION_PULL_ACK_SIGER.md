# Recuperación automática, PULL, ACK y conciliación

## Flujo y contrato

SIGER inicia la conexión HTTPS al origen del Formulario; no necesita recibir conexiones entrantes del Formulario. La API funcionaria no comparte la sesión ciudadana ni recibe sus tokens.

1. Reclamar con Bearer técnico en `/integracion/v3/formulario-completo/entregas/reclamar`, límite1 y cabecera `X-Formulario-Versiones: 1.0,1.1`.
2. Verificar estructura, canonicalización, SHA-256, procedencia y sello Ed25519 contra claves confiables. Identidad auténtica CU no se confunde con declaración/RUN/SRCeI.
3. Descargar originales por GET autorizado con `X-Reserva`, en streaming de 64 KiB, límites y hash/tamaño. Sin redirects, archivos enteros grandes en heap ni nombres ciudadanos como ruta.
4. Conservar originales privados/durables y, dentro de transacción MySQL, documento completo, metadatos, COM único, folio y registro firmado.
5. **Después del COMMIT**, recuperar el comprobante ya persistido y comprobar nuevamente originales. Solo entonces confirmar con reserva y ACK firmado al Formulario.
6. Marcar `ack_pending=false` únicamente después del 204. Ante respuesta perdida o error, conservar registro/folio/comprobante y conciliar de forma recuperable.

`comunicacion.version`1.0/1.1 no es la versión del ACK. Adultos usan ACK 1.0/canal FORMULARIO_WEB_PUBLICO; NNA ACK 1.1/canal FORMULARIO_WEB_NNA. No transformar documentos/firmas históricos para encajarlos en otra versión. El consumidor histórico `/integracion/v3/entregas` se conserva separado.

## Scheduler y exclusividad actuales

Opt-in Windows QA: `-HabilitarConsumidorAutomaticoQA -MantenerDisponible`, mismo perfil E2E aprobado. El bootstrap mantiene apagados V2 histórico, Graph real, Flyway e integraciones ajenas.

El planificador existente sondea cada 5 s, consulta `operation_control`, concilia antes del siguiente reclamo y retrocede de 10 s a 5 min si hay fallos. El lote es1. Sus contadores/último estado son del proceso, no un registro histórico de entrega. La autoridad durable es MySQL y el comprobante firmado.

La exclusión tiene tres capas: AtomicBoolean por instancia, writerlock de custodia y lock MySQL dedicado para el automático QA, independiente de la raíz de archivos. Se verifica la conexión propietaria antes de reclamar/conciliar y antes de persistir/confirmar. Si se pierde, falla cerrado; no roba/reconstruye silenciosamente el lock. El mismo esquema con otra custodia no autoriza otro consumidor.

La exclusión añadida es específica del esquema/usuario QA actual. El modo institucional remoto necesita un ámbito/origen/exclusión configurables y pruebas propias; no copiar el literal del esquema QA para producción.

## Consultar y controlar sin secretos

`GET /operacion/v1/formulario-completo/estado` exige autenticación operacional y devuelve estado, ciclos, últimoéxito/intento, próximo intento, errores sanitizados y contadores. No confundir con las métricas V2 históricas. Un 401/403 no es cola vacía.

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion estado
```

Pausa y reanudación son mutaciones CAS con If-Match y motivo PRUEBA_LOCAL, registradas en `control_audit`/eventos. El helper obtiene el token por DPAPI y no lo imprime. No desactivar TLS o publicar Bearer para diagnosticar.

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion pausar -AutorizadoLaboratorio
```

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion habilitar -AutorizadoLaboratorio
```

La pausa no mata una transferencia. Para mantenimiento: cerrar nuevos ciclos, esperar estado PAUSADO_POR_CONTROL después de terminar tick y comprobar ACK pendientes; coordinar con el responsable público que cierre ingreso/PULL y compruebe sus reservas. Solo después hacer la sustitución autorizada. Mantener origen/DB/keys; comprobar HTTPS, controles y varios sondeos correctos al reanudar. Las ventanas anteriores preservaron la misma JVM SIGER.

## Fallos y recuperación

| Situación | Conducta requerida |
| --- | --- |
| Cola vacía | Ciclo correcto, sin crear COM/archivos ficticios |
| Origen caído/red/5xx | Backoff y estado técnico; core local conserva consulta |
| TLS/credencial inválida | Falla cerrado; no trust-all, redirects ni bucle manual para “probar” |
| Adjunto o firma alterados | Rechazo, sin COM/ACK indebido; conservar evidencia |
| COMMIT realizado y ACK falló | Mismo COM/folio/recibo, pendiente durable; conciliar antes de repetir |
| Reserva vencida | Reclamación posterior con reserva nueva, registro idempotente; no otro folio |
| Confirmación pública recibida pero respuesta perdida | Repetir confirmación conservada conforme al contrato; no reenviar el formulario ciudadano |
| Lock perdido | No nuevas reclamaciones/ACK; revisión y reinicio controlado después de verificar origen/pendientes |
| Proceso/host reiniciado | Verificar integridad, identidad privada, origen y exclusividad antes de reanudar; no cambiar de perfil con pendientes |

Los ACK bloqueados/antiguos requieren revisión operacional autenticada; no editar tablas o borrar leases/recibos para limpiar un estado ambiguo. La solicitud manual `/operacion/v1/formulario-completo/recuperar` existe para campañas controladas, pero **no se usa para acreditar la campaña automática**.

## Qué está acreditado y qué falta

COM-002497 técnica y COM-002498 interactiva acreditaron el circuito manual real, registros y originales. La campaña automática posterior recibió nuevas Comunicaciones, incluidos COM-002499/2500/2501, sin invocación manual; los contadores y verificaciones específicas están en checkpoints/evidencia privada. No presentar un sondeo vacío como nueva entrega o las pruebas con dobles como ese E2E.

Para operación institucional faltan supervisión permanente aprobada, modo institucional/identidad, origen remoto ligado a pendientes, alertas/monitoreo persistente y campaña acotada de fallos reales de red/disco/DB/ACK. El código ya conserva idempotencia/conciliación; esta lista no autoriza otra campaña ni SLA o producción. [Diagnóstico detallado](DIAGNOSTICO_CONSUMIDOR_AUTOMATICO_2026-10-31.md) y [gates](GATES_DESPLIEGUE_SIGER.md).
