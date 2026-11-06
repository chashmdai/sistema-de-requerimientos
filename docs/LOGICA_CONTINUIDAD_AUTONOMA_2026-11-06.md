# Continuidad autónoma SIGER · 06/11/2026

## Instrucción vigente

[Validación funcional confirmada](DECISION_VALIDACION_FUNCIONAL_2026-11-06.md): no reabrir aprobaciones funcionales ya confirmadas. Separar mecanismos pendientes de implementación de parámetros nominales aún no cargados y de habilitación operativa.

## Git y dependencias

Worktree exclusivo `C:\req\worktrees\siger-logica`. Rama `codex/siger-equipos-agenda`, creada desde PR #5/HEAD `ef4d378a4c5afb9e23d56b0d4fa93a1b7c2aade5`. Cadena PR #2→#3→#4→#5; siguiente PR en borrador tendrá base `feature/siger-u2-continuidad-institucional`. Sin merge/despliegue. HEAD y cambios pendientes se verifican con `git status`, `git log` y `git diff`; este archivo no intenta autocitar su propio SHA.

## Estado al iniciar

U0–U1 y núcleo/continuidad U2 acreditados, sin reconstruirlos. PR #5: CI Windows/Ubuntu 454 métodos, frontend 109; H2 focal 51 y MySQL descartable 54. Son evidencia del corte anterior, no de código nuevo.

Bloque A en implementación: equipo local, agenda compartida del Caso, obligatoriedad, intentos, TAR-09, retiro con resolución de pendientes y espera de antecedentes. Migración conocida última V15.9; se inspeccionaron main y referencias publicadas y no hay versión posterior publicada observada ni referencia remota U4.5. No se accede a su worktree ni a SMR/QA.

## Siguiente acción precisa

Implementar evolución aditiva de agenda V12/V13 y equipo; reutilizar autorización del núcleo, cerrojos identidad/COM y proyección durable existente. Ejecutar focales durante desarrollo, después MySQL nuevo/descartable y regresión integral/CI del HEAD publicado. Actualizar aquí migraciones, resultados, commits, defectos reales y siguiente bloque. No detenerse solo por completar A si queda capacidad para B/C/D/E.

## Parámetros y preservación

No se cargaron identidades/mandatos/coberturas reales. No confundir esos valores ausentes con falta de validación del diseño. QA histórica, U4.4/U4.5, SMR, servicios/JAR, Formulario, contratos/firmas/originales y proveedores reales no intervenidos. Las pruebas usarán datos sintéticos y recursos nuevos; no hay autorización de despliegue, migración real ni correo real.
