# Checkpoint U5 · 26/10/2026

**U4.5 publicado. U5 parcial y local. No reiniciar el trabajo.**

- `origin/main`: `b322c9b905b533605fb53c8d706dbc144ed074a3`, publicado por fast-forward autorizado y comprobado remotamente. No volver a publicar la rama U5.
- Rama de trabajo: `feature/u5-solicitudes-tareas-independientes`.
- Código: `7b318f27dbf49f8b77ca2ac8997c8b5a32981105`; commit documental posterior conserva este corte.
- Java25/JAR por SHA `C:\req\bin\siger-7a50166482066535.jar`; no ejecutar ni sobrescribir `target` como runtime.
- QA: `siger_institucional_windows_qa`, V13 aplicada con migrador y runtime DML/TLS `VERIFY_IDENTITY`. Histórico sigue V12: 504 filas y 24 objetos intactos. No importar/restaurar ni migrar el histórico.
- Tareas independientes, de Solicitud y de Caso: implementadas, personales e idempotentes. Preparación de Solicitudes, borrador, seguimiento, historial, relaciones y avisos: implementados y probados. Las tres decisiones finales devuelven409 después de autorizar; el usuario indicó expresamente dejarlas pendientes.
- H2 y MySQL: 245 ejecutadas por motor, 242 correctas, tres errores de AV por RAM, cero omitidas. No declarar regresión completa verde. Frontend49, lint/build correctos. Edge: U5 86, reinicio26, territorio34; núcleo sin documental59/45. Custodia ciudadana: cuatro ingresos/ocho originales verificados, negativa TI y vista503 controlada; no E2E documental aprobado.

## Punto exacto de continuación

Leer [informe](CIERRE_U5_2026-10-26.md) y contrato. No reconstruir tareas ni preparación. Falta disponibilidad física para acreditar AV del candidato y el circuito documental positivo. El último control aislado ni siquiera ejecutó scanner: disponibles4.242.354.176, exigidos4.294.967.296bytes. No detener procesos ajenos ni modificar límites, Kaspersky, políticas o servicios.

Primer paso cuando exista margen real de RAM:

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Control-AV-Real.ps1
```

Continuar con `Windows-Probar-Antivirus-Java.ps1`, calificación documental y baterías H2/MySQL con `-DocumentalRun` actual, sin reinstalar dependencias. El código distingue un temporal secundario retirado de un reparse point y conserva fallo cerrado para raíz/enlaces/otros errores. Su nueva prueba Win32 pasó; la validación AV positiva definitiva está pendiente, no se sustituye por esa prueba.

No ejecutar la regresión MySQL destructiva mientras el runtime QA opere. Esa batería borra fixtures QA; si se ejecuta, los baselines Edge por UUID dejan de ser reutilizables contra la nueva base. Conservarlos como evidencia y preparar nuevas identidades/ingresos sintéticos mediante los helpers existentes, sin recuperar datos históricos otra vez.

Para reabrir el QA actual sin borrar nada: usar el JAR citado, configuración documental `C:\req\testdata\documental-win-4c816361d6ad4322a1bfd2e8c1dd2009\windows.properties`, `-Archivo C:\req\testdata\runtime-qa-0458a858ee2940cea52cfaecf2b08ff0\archivo` y `-TrustPublicoQA C:\req\testdata\runtime-qa-0458a858ee2940cea52cfaecf2b08ff0\recepcion\publico-trust.json` con `Windows-Iniciar-QA.ps1`. No es necesario volver a crear sus cuatro ingresos.

Baseline U5 válido: `C:\req\testdata\runtime-qa-6e742f628f594c67b0ca69c292e65617\e2e-u5-operacion.json`. Los resultados86/26/34 están en esa raíz. El parcial documental y las rutas de evidencia constan en el informe. Los últimos runtimes están detenidos y sus secretos efímeros retirados.

Las decisiones de responder, canalizar y autorizar escalamiento no son un problema técnico que deba resolverse inventando reglas: permanecen pendientes por instrucción. No habilitarlas hasta tener su definición. No activar producción, Graph real, proveedores, recuperación pública, agente, WSL ni nuevos servicios. No hacer push de U5.
