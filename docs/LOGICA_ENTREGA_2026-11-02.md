# Entrega progresiva de lógica SIGER · 02/11/2026

Continuidad posterior a `d19fa87d`: [U1.3 y siguientes](LOGICA_CONTINUIDAD_U1_2026-11-02.md). Los resultados siguientes conservan el corte acreditado de U1.1/U1.2; no se presentan como las pruebas nuevas de continuidad.

## U0

[Diagnóstico inicial](LOGICA_U0_DIAGNOSTICO_2026-11-02.md), [matriz de 138 reglas](LOGICA_MATRIZ_REGLAS_2026-11-02.csv), [baseline de migraciones](LOGICA_BASELINE_MIGRACIONES_2026-11-02.csv). Documento rector verificado y leído íntegramente, sin incorporarlo a Git. G0 limpio `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`; U0 registrado en `0a384cc`.

## Incremento U1.1 · límite regional

REG-DIS-01 y ACC-03: reutiliza `distributionAllowed`, concesiones vigentes, `identity_guard`, idempotencia y transacción existentes. Agrega validación de todos los destinos contra sede de origen antes de ejecutar y bajo bloqueo. No necesita DDL. La coordinación de dos sedes no permite mezclarlas para distribuir; una concesión central independiente sí autoriza el alcance institucional. React recibe el actor vigente y filtra la selección. Se sustituye una expectativa histórica de prueba que permitía distribuir desde A hacia B.

Archivos: `NucleoInstitucional.java`, `Comunicacion.tsx`, pruebas `IdentidadInstitucionalTest`, `IdentidadHttpTest`, `Distribucion.test.tsx` y [contrato incremental](../contratos/logica-institucional-incremental-v1.md). No hay migración ni cambio al Formulario.

Prueba antes del cambio: tres fallos backend acreditaron aceptación indebida o código distinto del 403 esperado; una prueba UI acreditó destino ajeno visible. Se corrigió primero el fixture de dialog de jsdom para distinguir un fallo de infraestructura de prueba de la brecha funcional. Tras implementar: **31/31 backend**, **23/23 frontend**, lint y build React correctos, sin exclusiones nuevas. Datos sintéticos sobre H2 y HTTP con MockMvc; UI con DOM simulado. No se presenta como E2E físico ni como prueba MySQL. Cobertura de origen/destino regional, lote mixto atómico, dos concesiones locales, facultad central independiente, revocación y reintento, HTTPS/CSRF, persistencia/idempotencia. No se cambia COM, folio ni se crean REQ/CAS por distribuir.

## Pendientes y límites vigentes

U1 todavía parcial: faltan autoridad/áreas, reservas, lectura de Direcciones por materia, credencial temporal y otras concesiones; U2–U9 pendientes según U0. SOL con destino único, REQ sin Caso, semáforo REQ, cierres y agenda de equipo aún no están completos. Se conservan bloqueos U5 histórico. Esta entrega no certifica el resto del negocio por haber pasado regresiones históricas.

## Incremento U1.2 · exclusividad central

ORG-02/11: comprobación de solapamientos de intervalos `[desde,hasta)` dentro de `identity_guard`, tanto al conceder como al habilitar una cuenta. Reutiliza asignaciones, perfil, epoch y eventos V11; sin DDL/backfill. Las concesiones inactivas no son operativas y no pueden activarse con solapamiento. Los intervalos pasados se conservan sin fingir nueva designación. La resolución de permisos falla cerrada ante ambigüedad histórica, manteniendo otras funciones y permitiendo revocación explícita. No selecciona automáticamente un distribuidor ni le transfiere pendientes; el entrante consulta los mismos COM.

Archivos: `IdentidadInstitucional.java`, `Identidad.tsx`, mensaje seguro en `api.ts`, pruebas `DistribuidorUnicoTest`, `IdentidadHttpTest` e `Identidad.test.tsx`; contrato incremental y matriz. El guard existente ya protege mutaciones de identidad y núcleo, por lo que no se introduce un segundo orden de bloqueo ni un lock de proceso incapaz de coordinar otras JVM.

Antes del cambio: seis pruebas nuevas, cinco fallaron por permitir solapamientos/activación/concurrencia o lectura efectiva ambigua. Después: **24/24 backend**, **10/10 frontend**, lint/build correctos. Se corrigió el uso inicial de matchers DOM no instalados en el fixture de prueba, sin añadir dependencias. Incluye carrera de dos conexiones H2, corte temporal exacto, sesión saliente revocada, rollback de perfil/epoch/eventos y conservación exacta del registro firmado/inbox/folio sintéticos. HTTP usa MockMvc; no es red física ni MySQL. No se acredita aún la autoridad nominal/respaldos y toma de COM de AT-017 completo.

La primera regresión completa posterior detectó **un error entre 283 tests**: el fixture de `TareasNotificacionesTest.distribucionNoAnulaLasFacultadesAdicionalesDeJefatura` creaba un segundo distribuidor central. Se conserva la prueba multirrol, abriendo primero su Caso y registrando después revocación del distribuidor anterior y concesión a la jefatura. Se añaden aserciones de facultad extinguida/central vigente/jefatura vigente. No se excluye el test, no se relaja la política y no se cambia producción para aceptar el fixture obsoleto. El resultado de esa corrida fallida se conserva separado del build final.

No se ejecutaron migraciones ni fixtures sobre MySQL, QA/histórico/U4, consumidores reales, nuevos COM operacionales, proveedores, workers reales, cambios a JAR activos, procesos, servicios, infraestructura o worktree U4.5. La compatibilidad se verifica en código y pruebas sintéticas; inventarios físicos H2/U4.4 son evidencia histórica, no auditoría nueva. PE-01–14 siguen acotadas como en el diagnóstico. La incidencia WSL HTTP_503 de G0 no se corrige en esta rama.

## Validación consolidada y preservación

Build final oficial `Build-Siger.ps1`, Windows nativo, Java25 explícito, Node24, Maven y temporales privados nuevos: **BUILD SUCCESS** el 02/11/2026 a las 17:03 America/Santiago. React: **54/54**; lint y compilación TypeScript/Vite correctos. Backend: **283 tests, 274 correctos, 9 omitidos, 0 fallos y 0 errores**. Las omisiones corresponden a cinco tests documentales reales y cuatro Windows reales sin configuración opt-in; tres stress siguen fuera por su tag histórico. No se añaden exclusiones. Se ejecutan las regresiones de firmas/recibos/idempotencia/COMMIT→ACK, autorización documental, identidad, tareas, U5 y arranque con sus dobles/fixtures aislados. No equivalen a proveedores ni a E2E físico nuevo.

Artefacto exclusivamente en `siger-institucional/backend/target/siger-institucional-0.1.0-SNAPSHOT.jar`, SHA-256 `1148baf98ecc1f613c92e19095954d09aad526b37686764415b58ca4dbb09fad`. No se copia a `C:\req\bin`. Log final privado `final-build.log`: SHA-256 `b7df7fa06f82a5f87302bb5d1e2ca07fd29797b2b3d76c54bf95d2e81b7ac21c`, conservado junto con baseline, rojo/verde y primera regresión fallida en el subdirectorio `logica-evidencia` del temporal privado inicial. Los XML Surefire finales permanecen en target y se preservan también en esa evidencia privada; no se publican logs/fixtures completos.

Los **28 archivos V1–V14 de ambos motores conservan sus SHA-256 de baseline Windows** y no hay nuevas migraciones. Se aplican V1–V14 solamente en H2 nuevos de los tests. Cero diferencias de código en paquetes `formulario`, `robustez`, `windows`, SQL, POM, build scripts y CI frente a G0. No se abre un puerto ni se actúa sobre U4.5. La compatibilidad U4.5 acreditada es de cambios acotados/sin DDL ni arranque modificado; su demostración operacional no se ejecuta desde esta rama.

`git diff --check` correcto; revisión del diff nuevo y nombres de archivos sin candidatos a DPAPI, claves, certificados privados, JAR u objetos operacionales. Búsqueda de patrones de claves privadas/tokens sin coincidencias, sin imprimir valores. Esto no sustituye una auditoría exhaustiva de secretos. El rector de Descargas conserva el hash de referencia y no se publica. npm informa 10 vulnerabilidades moderadas heredadas; no cambian dependencias ni lockfile.

## Riesgos y próximos incrementos

La restricción nueva puede dejar distribución central sin facultad efectiva si hay designaciones históricas ambiguas; exige revocación autorizada, no elige una persona automáticamente. No se ha inspeccionado ni modificado la configuración operacional para averiguar si tal condición existe allí. La frontera de administración TI sigue siendo de laboratorio: respaldo de concesiones, prevención integral de autoelevación y autoridad funcional por área son trabajo U1 pendiente. Los perfiles/visibilidades y cierres históricos pendientes de adaptación siguen identificados en U0; esta versión no debe declararse SIGER institucional completo.

Orden recomendado: completar autoridad/áreas/reservas y credencial inicial U1; luego el incremento vertical U2 de primera atención/REQ sin Caso y SOL única, con rutas históricas sustituidas conjuntamente. Después U3 equipos/asignación/acuse, U4 agenda y TAR-09, U5 del rector cierres/subrogancias, U6 movimientos/antecedentes, U7 avisos/auditoría/reportes, U8 mapeo y U9 aceptación. Los datos nominales PE no bloquean el desarrollo con fixtures, pero sí su activación real.

## Git y publicación

Rama exclusiva `feature/siger-logica-negocio`, directorio `C:\req\worktrees\siger-logica`. Commits previos al cierre: `0a384cc` U0; `65a4658` REG-DIS-01; `91068fc` ORG-02/11. El commit de cierre agrega únicamente el ajuste del fixture multirrol y esta evidencia; todo su código Java/React es el de la regresión final anterior. `origin/main` se verificó por fetch y seguía en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`, ancestro de la rama. No se incorporaron cambios ajenos ni se consultó/modificó el árbol U4.5.

Push normal y upstream comprobados para el HEAD funcional **`abf90c13c8bf0f773205f4699162a6560edef378`** (incluye `abf90c1`, adaptación de fixture y regresión consolidada). Árbol limpio; HEAD local, upstream y `ls-remote` de la rama coincidieron. `main` remoto permanecía en G0. [PR #2](https://github.com/chashmdai/sistema-de-requerimientos/pull/2) abierto **en borrador** contra `main`, sin merge ni auto-merge.

CI del HEAD funcional: [ejecución 41515899678](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41515899678), ambos jobs completos con `success`: [Windows](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41515899678/job/125519273643) y [Ubuntu](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41515899678/job/125519273822). Son runners remotos distintos del Windows local; ninguno acredita workers reales, MySQL o proveedores. La CI incluye lint/tests/build mediante el mismo script aislado versionado.

El último commit posterior al HEAD funcional actualiza únicamente este informe con los comprobantes de publicación; no modifica Java, React, SQL, pruebas o configuración. Para resolver el SHA del propio informe versionado usar `git log -1 --format=%H -- docs/LOGICA_ENTREGA_2026-11-02.md`; el manifiesto final privado y la entrega de chat registran además el HEAD exacto comprobado, evitando una autorreferencia imposible al SHA del commit que contiene este texto. Los checks de ese último commit documental se consultan en el PR; no se atribuyen aquí resultados futuros. La entrega no incluye merge ni despliegue.

## Preguntas institucionales

No se requiere una respuesta para los controles implementados. Las dependencias nominales de autoridad, reserva, calendario, habilitación y aceptación del rector serán necesarias para activar sus capacidades; no se repreguntan las decisiones funcionales ya confirmadas ni se habilita producción por publicar código.
