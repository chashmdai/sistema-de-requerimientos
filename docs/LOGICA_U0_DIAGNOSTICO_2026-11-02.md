# Lógica institucional · U0 y plan de adaptación

Fecha: 02/11/2026, America/Santiago. Rama exclusiva `feature/siger-logica-negocio`, worktree `C:\req\worktrees\siger-logica`. Inicio limpio en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`. No se interviene U4.5.

## Fuentes y alcance de evidencia

Se verificó y leyó íntegramente el rector local `SIGER_LOGICA_DE_NEGOCIO_Y_CONTRATO_CODEX_2026-10-28.md`: SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`, 2299 líneas. Se usa como requisitos; sus instrucciones copiables no amplían la autorización de esta sesión. No se incorpora el documento de Descargas a Git. Se publican únicamente referencias y diagnóstico técnico sin datos ciudadanos o designaciones personales.

Entradas leídas: AGENTS, G0, traspaso H2, estado H2, cierre U4.4, arquitectura, datos/roles, pendientes U5, preparación Windows y contratos de núcleo, complejidad/derivación, antecedentes/reaperturas, identidad, documental, consumidor completo, tareas y Solicitudes. La referencia AS-IS del rector `b322c9b` es antecesora de G0: 118 archivos cambiados, 4176 inserciones y 130 eliminaciones; incluye U5 parcial, consumidor automático, H2 y U4. No se restaura aquella referencia.

La inspección es de código/contratos/tests locales. No se consulta SQL de bases preservadas, configuraciones privadas ni sus objetos físicos. Los inventarios/checksums operacionales son evidencia histórica referenciada por H2/U4.4, no mediciones nuevas de esta sesión. La incidencia WSL HTTP_503 de G0 queda fuera del alcance: no se sondea ni corrige infraestructura. Las migraciones **disponibles en código** son V1–V14 en H2/MySQL; no se afirma el historial aplicado actual de QA. El runtime QA usa otro JAR fuera de Git.

## Modelo y arquitectura reales

Monolito Java25/Spring/JDBC/Flyway, React/TypeScript en el mismo JAR. MySQL operativo, H2 para pruebas aisladas. No requiere nuevos frameworks o servicios.

| Área / referencia bajo `siger-institucional` | Implementación comprobada por inspección | Brecha frente al rector |
| --- | --- | --- |
| `backend/.../robustez`, `formulario` | Inbox íntegro, COM única, folio transaccional, recibos, originales, conciliación COMMIT→ACK | Conservar; no crear REQ/CAS en ingesta |
| `nucleo/NucleoInstitucional.java` | COM manual, destinos 1–10, REQ al primer CAS, reevaluación automática | SOL única, principal, asunción sin CAS y cierre REQ explícito pendientes |
| `core_case`, `core_assignment`, `core_action`, `core_review` | Responsable, asignación histórica, actuaciones anexadas, revisión y complejidad BAJA por CAS | Equipo, área/materia, acuse, espera, semáforo REQ y nueva matriz de cierre pendientes |
| `core_transfer`, `core_link`, `core_reopen` | Sucesor por derivación aceptada; evaluación y reapertura autorizadas | Aceptación aún cierra origen; vínculo parte de COM/CAS; faltan colaboración y transferencia principal |
| `identidad/IdentidadInstitucional.java`, `CatalogoLaboratorio` | Cuentas individuales, bcrypt, epoch, roles/ámbitos/vigencias separados, guard transaccional | Falta cambio inicial de clave, autoridad documentada/área, exclusividad de distribución, subrogancias y reservas |
| `identidad/OrganizacionInstitucional.java` | Unidad/sede/región/cobertura y catálogo sintético explícito | Organigrama descriptivo separado de habilitaciones y concesiones pendientes; no sembrar sedes reales |
| `notificaciones/TareasPersonales.java`, V12/V13 | Tarea personal, historia, ejecución manual, contexto CAS/SOL o independiente | Agenda de equipo, obligatoriedad, intentos y TAR-09 pendientes; contexto independiente es histórico U5, no agenda objetivo |
| `notificaciones`, outbox | Avisos filtrados, intentos, simulación Graph y ambigüedad durable | Matriz EV completa, ambos canales para nuevos eventos, enrutamiento y relojes pendientes |
| `documental`, `AutorizacionNucleo` | Originales privados, autorización de recurso, análisis y visor aislados, auditoría documental | Integración de equipo/FIR/versiones editoriales pendiente; reutilizar custodia |
| `solicitudes/SolicitudesInstitucionales.java` | Preparaciones personales, seguimiento, idempotencia, snapshots | No es SOL institucional; responder/canalizar/escalar continúan 409 |
| `front/src` | Pantallas conectadas a API de laboratorio, sesiones/CSRF, gestión y documentos | Vistas de destinos múltiples/complejidad CAS/cierre BAJA siguen históricas hasta sus incrementos |

Las tablas históricas no son automáticamente el nuevo modelo: `institution_request` es preparación U5, `core_requirement` agrupa Casos y `core_destination` admite varios destinos. No renombrar estados históricos ni crear aceptaciones ficticias por mapeo.

## Brechas GAP-01–28 revalidadas

| GAP | Evidencia local / cambio necesario | Unidad |
| --- | --- | --- |
| 01, 02, 04, 22 | `abrirCaso`, `registrarManual`, `resolverDestino`, V5/V13: REQ nace con CAS; primera atención solo responde COM; falta principal/SOL/actuación sin CAS | U2 |
| 03 | `distribuir` y `CommunicationAction`: hasta 10 destinos simultáneos | U2 |
| 05 | `reevaluate`: termina/reactiva REQ por conteos automáticamente | U2/U5 |
| 06 | `clasificar`, V6, `Caso.tsx`: BAJA en CAS | U2/U5 |
| 07, 08 | `revisarCaso`: cierre PROFESIONAL_BAJA y jefatura genérica | U5 |
| 09, 10 | `aceptarDerivacion`: cierra origen; decisiones restantes 409 | U6 |
| 11 | `vincular`, `linkReader`, `evaluarAntecedente`: COM raíz y CAS opcional, sin pertinencia REQ/materia separada | U6 |
| 12 | `canCase`, `grantSql`, `AutorizacionNucleo`: ámbitos actuales sin equipo/FIR/lectura direcciones por materia | U1/U3 |
| 13 | `distributionAllowed` restringe origen; `distribuir` no limita destino a sede del ingreso regional; prueba histórica lo permite | U1, primer incremento |
| 14, 15 | Organización sintética, asignaciones sin autoridad funcional/área/subrogancia; múltiples distribuidores posibles | U1 |
| 16 | Creación/reset bcrypt y epoch; sin obligación de cambio de clave temporal | U1 |
| 17, 21 | Asignación inmediata; sin acuse ni espera confirmada/revisión titular | U3 |
| 18, 19, 20 | `TareasPersonales`: propietario único creador=ejecutor, sin obligatorias ni traspaso | U4 |
| 23 | Auditoría mutaciones/documentos existente; falta auditoría de todas las consultas y exportación separada | U7 |
| 24 | Infraestructura outbox válida; preparaciones U5 con correo NO_APLICA y eventos anteriores no cubren matriz EV nueva | U7 |
| 25 | Custodia reutilizable; falta política nueva de equipo/reserva y versionado | U3/U6/U7 |
| 26, 27 | Contratos/tests describen negocio histórico; deben cambiar conjuntamente por incremento | Todas |
| 28 | G0 contiene U5 estable parcial posterior a referencia del rector; se preserva y no se declara SOL completa | U2 |

La matriz [por identificador](LOGICA_MATRIZ_REGLAS_2026-11-02.csv) cubre los 112 IDs originales y 26 decisiones adicionales, con estado inicial, implementación, cambio y prueba objetivo. “Parcial” no acredita el escenario de aceptación completo; las pruebas históricas solo validan sus expectativas anteriores. Los IDs AT son escenarios diseñados del rector, salvo las coberturas expresamente acreditadas en el informe de entrega.

## Orden y límites de implementación

1. **U1 parcial**: corregir destino regional y exclusividad central con el guard existente; pruebas de mezcla de roles, revocación, intervalos y carreras. Sin nueva facultad nominal ni migración. Después completar organización/autoridad por área, reservas y credencial temporal.
2. **U2** depende de autoridad: modelar asunción/REQ sin CAS/principal, SOL única, clasificación REQ y primera atención; desactivar rutas sustituidas en el mismo incremento, conservando historia. No usar botones U5 como atajo.
3. **U3** sobre U1/U2: varios CAS, equipos, materia, asignación/acuse/espera. **U4**: agenda del Caso, intentos, obligatoriedad y TAR-09, manteniendo independientes actuaciones/tareas.
4. **U5 del rector** (diferente de U5 histórico): revisión/cierre/subrogancias/excepciones y cierre REQ explícito. **U6**: colaboración, derivación sin cierre automático, transferencia y antecedentes a REQ, reapertura coherente.
5. **U7**: eventos/ambos canales simulados, auditoría, exportación y relojes sin inventar calendario. **U8**: ensayo integral de mapeo/reversión únicamente en copias descartables autorizadas. **U9**: producto con roles sintéticos; QA preservada no se usa como destino de campaña.

No se declara U1 completa por resolver dos controles. El resto del laboratorio conserva reglas históricas identificadas y no representa todavía el negocio objetivo. No hay impedimento técnico para continuar las unidades posteriores; esta ejecución se puede entregar en un punto estable tras completar y verificar el incremento iniciado.

## Migraciones, compatibilidad y reversión

V1–V14 se mantienen íntegras. El incremento U1 inicial no necesita DDL ni backfill: las asignaciones y `identity_guard` de V11 bastan. Eso evita colisión de versiones con U4.5. Para futuros cambios reservar versiones fechadas por unidad, comprobar refs/PR de ambas ramas antes de crear SQL y documentar equivalencia H2/MySQL. No editar V13 para convertir preparaciones en SOL.

Evolución futura: expandir tablas/campos, mapear con incidencias explícitas, comprobar, cambiar escritor y recién retirar compatibilidad activa. Múltiples destinos y complejidad histórica no determinan principal ni semáforo automáticamente. Preservar ID/folio/fechas/actores/originales y actos antiguos sin falsificar aprobadores. No se promete rollback de JAR antiguo ante un modelo nuevo. El incremento inicial admite revert de código sin borrar datos; reabrir facultades antiguas no es una reversión institucional aprobada.

No hay cambios al Formulario, DTO de transporte, firmas, claves, custodia, folios COM o COMMIT→ACK. No se ejecuta receptor/consumidor real. Las bases QA/histórica/U4, COM-000001, JAR activos y recursos U4.5 están fuera de pruebas. Integración MySQL nueva y workers reales no se acreditan por H2; se registrará su omisión.

## Dependencias institucionales

PE-01–14 del rector siguen distinguiéndose de ingeniería: titulares/coberturas/delegaciones, identidad corporativa, reservas extraordinarias, materias, urgencia, folios, calendarios/escalamientos, correo/visaciones, auditoría/retención, políticas documentales, infraestructura y aceptación/corte. No se requieren personas reales para pruebas sintéticas. No se repreguntan decisiones Rush resueltas ni se usa falta de catálogo para inventar autoridades.

La administración TI actual aún no contiene respaldo completo de cada concesión; la exclusividad nueva es una restricción técnica adicional a esa frontera de laboratorio, no su aprobación normativa. La política de regularización de tareas residuales y las reservas extraordinarias permanecen acotadas. Las funciones U5 históricas siguen bloqueadas mientras se sustituye su modelo con la matriz adecuada.

## Baseline ejecutado antes de implementar

`Build-Siger.ps1` en Windows nativo, Java25 explícito y Node24: lint y build frontend correctos; 51/51 tests React; 274 tests backend, 265 correctos, 9 omitidos, cero fallos/errores. Las nueve omisiones son workers reales opt-in (5 documentales, 4 Windows). Tres stress continúan excluidos por tag existente. Flyway V1–V14 se aplicó únicamente en fixtures H2 nuevos. No se ejecutó MySQL, navegador físico, antivirus/renderer real ni E2E externo. npm reportó 10 vulnerabilidades moderadas heredadas, sin actualización masiva de dependencias.

Log de baseline local (ignorado por Git): `siger-institucional/backend/target/logica-evidencia/baseline.log`; el build final limpia target, por lo que la evidencia consolidada debe copiarlo a su directorio privado antes de esa validación. [Manifiesto de 28 migraciones](LOGICA_BASELINE_MIGRACIONES_2026-11-02.csv) generado antes de cambios funcionales. No contiene checksums de bases preservadas.
