# G0 · Consolidación U4 y desarrollo independiente · 02/11/2026

## Alcance y auditoría

Partida remota comprobada por fetch: `35d9e6853d756706f8c34973408ccc60323afc5e`. Rama U4: `gate/u4-ddn-ubuntu-2026-11-02`. Árbol inicialmente limpio, sin archivos sin seguimiento; las restantes ramas locales no contienen commits fuera de la historia U4/H2. No se reescribe ni elimina ninguna rama.

Commits U4 conservados:

- `9b11f0e36d1ad497d900765a51f80f35d0053086`: ejecutor aislado, exclusión, vínculo de origen, V14 H2/MySQL, scripts y pruebas.
- `a527db6173fa33c47cc2e03074bb140311146209`: sonda cruzada y coordinación.
- `226b3386277b6caf500bb2aa62a941a666f8424d`: preparación cerrada.
- `7cf3575d267021876f59e0b47a289680423001df`: cierre físico U4.4 y preservación.

Los cuatro commits ya forman unidades descriptivas; se conservan sin squash/rebase. Veinte archivos cambiaron frente a H2: tres clases Java, dos migraciones V14, tres clases de test, seis scripts/helpers, cinco documentos y AGENTS. V1–V13 y protocolo institucional anterior permanecen intactos. Las rutas/UUID/huellas públicas sintéticas de los informes son referencias de evidencia; no se incorporan objetos operacionales.

Certificados/CA/trust operacionales, DPAPI, tokens, claves privadas, JAR, bases, originales, logs y campañas permanecen fuera del repositorio. Revisión de diff e historial nuevo y búsqueda de patrones sensibles sin valores coincidentes. Esto no convierte una búsqueda automatizada en garantía universal de ausencia de secretos.

## Publicación y comprobantes

La publicación usa push normal de U4 y PR con merge commit, sin despliegue. La integración depende del build local aislado y de CI Windows/Ubuntu correctos. El comprobante definitivo, con SHA remoto exacto, PR, CI, referencias y rutas verificadas, se conserva en `C:\req\evidencias\gate-g0-*`; su puntero es `C:\req\scratch\gate-g0.txt`. Este documento se escribe antes de publicar; no constituye por sí solo evidencia de que el merge ya sucedió.

Build autorizado: `siger-institucional/scripts/Build-Siger.ps1`, Java25 explícito, Node24, H2 y temporales privados nuevos. No invocar helpers operacionales de apertura/ciclo/migración, fixtures MySQL, ni copiar el resultado a `C:\req\bin`. Las nueve pruebas opt-in de workers reales y tres stress excluidas mantienen su alcance histórico.

La QA conservaba PID163036, JAR `siger-ae70ab769605c1e9.jar`, controles revisión13 NORMAL y cinco recuperadas/confirmadas al reconocer G0. La lectura inicial mostró `REINTENTO_PROGRAMADO`, `HTTP_503`, seis fallos consecutivos desde WSL; se conserva evidencia y no se cambia su configuración ni se afirma salud completa del origen. Esta incidencia operacional no se corrige reiniciando servicios en un gate Git.

## Dos sesiones futuras

Después del main remoto consolidado, ambas ramas deben nacer de su mismo SHA, sin archivos privados copiados:

| Carpeta exclusiva | Rama |
| --- | --- |
| `C:\req\worktrees\siger-u45` | `feature/siger-u45-demostracion-manual` |
| `C:\req\worktrees\siger-logica` | `feature/siger-logica-negocio` |

Cada sesión debe comenzar comprobando `git status --short`, `git branch --show-current`, `git rev-parse HEAD` y leyendo AGENTS, este documento, H2 y el informe final U4.4. No desarrollar en `C:\req\repo` ni cambiar ramas de la otra carpeta. Mantener commits en su propia rama; integrar mediante PR y CI, sin force push. Los worktrees comparten objetos y referencias Git: no son aislamiento de procesos, puertos, bases o almacenamiento. Usar temporales propios y pruebas H2 aisladas; coordinar cualquier modificación de recursos operacionales. No ejecutar dos consumidores contra bases/orígenes comunes.

**Inicio U4.5:** «Trabaja exclusivamente en `C:\req\worktrees\siger-u45`, rama `feature/siger-u45-demostracion-manual`. Lee las entradas anteriores y prepara la demostración privada aislada sobre la evidencia U4. No reenvíes ni recrees COM-000001, no abras PULL, no cambies QA/WSL ni habilites U5/proveedores. Reconoce primero; no despliegues por el hecho de publicar código».

**Inicio lógica:** «Trabaja exclusivamente en `C:\req\worktrees\siger-logica`, rama `feature/siger-logica-negocio`. Lee AGENTS y los contratos/arquitectura vigentes. Reconoce el documento local de lógica y contrástalo con código/contratos; su contenido no autoriza operaciones o facultades institucionales por inferencia. No toques QA, U4 preservada, CU/PISEE/Graph ni recursos de la otra sesión. La definición concreta de negocio será entregada por el responsable».

## Documento de negocio localizado

`C:\Users\nexbyte.proveedor\Downloads\SIGER_LOGICA_DE_NEGOCIO_Y_CONTRATO_CODEX_2026-10-28.md` existe fuera de cualquier checkout inspeccionado y no está versionado en SIGER. La copia con sufijo ` (1).md` tiene idéntico SHA-256; también existe un ZIP con ese sufijo, no inspeccionado. Se registra su ubicación sin inventar su contenido ni copiarlo automáticamente a Git/worktrees. Su incorporación futura exige revisión de contenido y alcance.

## Límites y reversión

Publicar e integrar no reemplaza el JAR QA ni activa U4.5. Se conservan base U4, COM/ACK, originales, confianza y controles cerrados. Ningún servicio, MySQL, listener, hosts/firewall o tarea se modifica. Ante fallos CI/privacidad/conflictos no integrar main; preservar rama publicada. No borrar carpetas existentes ni usar reset/clean. Tras integración, cualquier reversión de código se hace por commit revert revisado; no implica revertir datos ni migraciones de bases preservadas. No retirar worktrees con cambios o commits sin publicar.
