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

## Checkpoint de implementación A

Commit documental de entrada `41c502d`; código posterior todavía sin commit. V15.10 en desarrollo usa una migración Java por dialecto, con DDL común y checksum explícito: identifica únicamente el CHECK creador=ejecutor histórico, lo sustituye por contexto CASO y mantiene las demás restricciones. Añade equipo/historia de pertenencia, obligatoriedad/asignador/generación de recordatorio, intentos y solicitudes de espera. No edita V1–V15.9.

Implementado localmente: `EquipoCaso`/controlador, ampliación de `TareasPersonales`, autorización de colaboradores en listas/detalles/gestión ordinaria, TAR-09 transaccional, cierre con obligatorias y espera explícita, UI `EquipoAgenda` conectada. Continúan pendientes de cierre los avisos específicos, pruebas documentales/HTTP/DOM, adaptación de expectativas históricas y validación integral/MySQL/CI.

Pruebas acreditadas en este corte: migración H2 2/2; compilación backend correcta; `EquipoAgendaTest` 9/9. Selección conjunta de 23 métodos: dos fallos y dos errores en `TareasNotificacionesTest` por expectativas personales/revisiones antiguas; no es una selección aprobada. Se están adaptando a asignación de equipo, lectura por jefatura y TAR-09, conservando negativas TI/ámbito y versiones actuales. Frontend lint/build iniciados; comprobar su resultado antes de atribuir éxito.

Evidencia privada de desarrollo: carpeta `siger-build-4c652bfd9b7a41d5a8e0d659e180202d` bajo LOCALAPPDATA, sin secretos versionados. Siguiente acción: terminar avisos de equipo/espera y UI global de tareas, añadir pruebas de documentos/áreas, HTTP, recordatorios y revocación; ejecutar focales y MySQL con esquema nuevo; corregir antes de commit/publicación de código. Recuperar con `git status` y diffs; no descartar los archivos locales nuevos.

## Parámetros y preservación

No se cargaron identidades/mandatos/coberturas reales. No confundir esos valores ausentes con falta de validación del diseño. QA histórica, U4.4/U4.5, SMR, servicios/JAR, Formulario, contratos/firmas/originales y proveedores reales no intervenidos. Las pruebas usarán datos sintéticos y recursos nuevos; no hay autorización de despliegue, migración real ni correo real.
