# Checkpoint U5 · 27/10/2026

**Validación técnica por RAM acreditada. U5 permanece funcionalmente parcial.**

- Repositorio `C:\req\repo`, rama `feature/u5-solicitudes-tareas-independientes`.
- Backend/frontend institucional: código `7b318f2`, conservado sin cambios. `98fd092` registra únicamente la evidencia positiva del harness. Este checkpoint se conserva en el commit documental posterior.
- `main` continúa en `b322c9b905b533605fb53c8d706dbc144ed074a3`; no se modificó ni se publicó U5.
- Mismo JAR `C:\req\bin\siger-7a50166482066535.jar`, SHA-256 `7a5016648206653517a1625d89bcde241fa4af1e970ef8fb99387cfb52593402`. Sus235 clases coinciden con el candidato probado.
- AV real0/1/2, JUnit EICAR1/1, aislamiento9/9. H2 **245/245** y MySQL QA **245/245**, cero errores/omitidas; cinco documentales reales5/5 y cuatro adicionales4/4 por motor. Frontend **49/49**, lint/build correctos.
- Edge con documental activo: **127** operación y **110** tras reinicio; U5 documental **57**, U5 **86**, U5 tras reinicio **26**, territorio **34**, todos correctos.
- Histórico conservado:504 filas/59tablas y24objetos/16.869.263bytes con las mismas huellas. QA V13 y runtime DML/TLS separados del migrador. No hay migraciones nuevas.
- Graph exclusivamente simulado; cero envíos reales y recuperación pública desactivada. Productor/ACK ciudadanos sintéticos en proceso, no un circuito público real.
- Runtimes/Edge/workers propios detenidos, sin listeners QA, trabajos pendientes ni secretos efímeros remanentes.

## Punto exacto para continuar

No reconstruir módulos ni repetir la validación técnica sin una razón nueva. Tareas independientes/de Caso/de Solicitud, preparación, borradores, seguimiento, historial y avisos siguen implementados. **Responder, canalizar y escalar siguen deshabilitados en UI y API409**. Sus facultades deben confirmarse institucionalmente antes de implementar las transiciones finales. Esta unidad no las define ni habilita.

Leer [validación técnica](VALIDACION_TECNICA_U5_2026-10-27.md) y [contrato](../contratos/solicitudes-tareas-u5-laboratorio-v1.md). Los informes del26/10 se conservan como cortes históricos; sus errores de RAM quedaron resueltos en la campaña del27/10.

## QA preservado y operación

La batería MySQL limpió únicamente fixtures QA. Se prepararon nuevos ingresos sintéticos y se conservó la evidencia anterior. **No reutilizar los UUID de los baselines del26/10 contra el QA actual.** No importar ni restaurar el histórico.

- Campaña privada: `C:\req\evidencias\u5-validacion-2026-10-27-6f6bc769756245548d2bf0bebb5c8556`; resumen y archivos de resultados en esa raíz.
- Configuración documental existente: `C:\req\testdata\documental-win-4c816361d6ad4322a1bfd2e8c1dd2009\windows.properties`.
- Custodia vigente: `C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo` y trust en `recepcion\publico-trust.json` de esa raíz. No regenerar esos cuatro ingresos.
- Baselines Edge válidos: `C:\req\testdata\runtime-qa-1abbbf44e3674b1ab49bb316e1148350\e2e-operacion.json` y `e2e-u5-operacion.json`.
- Estado final QA: ocho COM, un Requerimiento/Caso, siete preparaciones, cuatro tareas, diez originales, cinco representaciones/ocho páginas, veintiséis avisos y tres intentos simulados. Cero COM duplicados, aceptados Graph o ACK pendientes.

Para reabrir el QA actual, usar el mecanismo existente, sin contraseñas en argumentos:

```powershell
& C:\req\repo\scripts\Windows-Iniciar-QA.ps1 -Jar C:\req\bin\siger-7a50166482066535.jar -Seconds 900 -Archivo C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo -TrustPublicoQA C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\recepcion\publico-trust.json -DocumentalConfig C:\req\testdata\documental-win-4c816361d6ad4322a1bfd2e8c1dd2009\windows.properties
```

Conservar el mínimo de4GiB físicos para AV y sus límites. No ejecutar regresión MySQL destructiva con runtime QA activo. Consultar [operación](OPERACION_U5_WINDOWS.md) y [reversión](REVERSION_U5_WINDOWS.md). No activar producción, Graph real, proveedores o recuperación pública; no modificar servicios ajenos, WSL, histórico ni `main`. No hacer push de U5.
