# SIGER · U1.6a · Áreas y autorización efectiva

Continuidad desde `62c11cc2e142a0be1fb628772b8e4c40862a6dd3`, exclusivamente en `C:\req\worktrees\siger-logica`, rama `feature/siger-logica-negocio`. Desarrollo iniciado el 02/11/2026, Windows nativo. Se conservan U0–U1.5, su evidencia y los 138 IDs; no se reinicia el diagnóstico. Rector local comprobado nuevamente: SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`; no se publica el documento de Descargas.

## Resultado y corte

**U1.6a implementa áreas ordinarias como ámbito explícito**, desde autoridad firmada y concesión independiente hasta destino, Caso, consultas y operaciones, API y React. No concede contenido por un rol de unidad/sede ni por TI. El circuito U1.5 sigue siendo obligatorio. No hay áreas, autoridades o adscripciones nominales reales sembradas.

**U1.6 y U1 siguen parciales**: materia y lectura regional transversal de Direcciones, FIR/reservas, excepciones sensibles, delegaciones generales/subrogancias y modelo nuevo SOL/REQ no están terminados. No se inicia una etapa amplia U2 en este PR. Se recomienda revisar/integrar el corte U1 antes de ampliar materia/U2, sin merge ni despliegue en esta entrega.

## Reglas y matriz

La [matriz](LOGICA_MATRIZ_REGLAS_2026-11-02.csv) mantiene exactamente 138 reglas, orden e IDs originales. Se actualizan 17 filas; las otras 121 conservan su cobertura previa. No se convierten regresiones técnicas en cumplimiento normativo de funciones futuras.

| Reglas | Evidencia y alcance |
| --- | --- |
| ACC-01/02/03 | Catálogo y mandatos AREA firmados; solicitud 1.1, aprobación independiente, ejecución TI y revocación. Exactitud de ámbito, vigencia y multirrol; sin organigrama nominal inventado. |
| ACC-05/06 | TI, rol de unidad/sede y área ajena no leen Casos/documentos. Revocación y caducidad retiran permisos; cambio de seguridad del catálogo invalida sesiones. No reasigna carteras. |
| ORG-03/04 | Coordinación solo en su área; Dirección requiere concesión AREA explícita y catálogo DIRECCION. No obtiene automáticamente coordinación propia ajena o lectura regional. |
| ORG-08/09 | Unidad/sede/área distintas; profesional principal conserva su asignación. No introduce permiso transversal de lectura por materia. Históricos sin área permanecen explícitos. Equipo institucional completo y materia pendientes. |
| ORG-10/14 | Cambio de área del mismo Caso conserva ID/folio/original/asignación. Exige facultad específica firmada DIRECCION o DELEGACION_EXPRESA, jefatura en origen/destino y responsable habilitado en destino. |
| CAS-CIERRE | Endurecimiento solo para Casos AREA: sin cierre profesional BAJA ni autocierre mediante otra cuenta de la misma persona; confirma otra persona competente. Cierre integral y excepción de titular regional pendientes. |
| ACC-07/09; ORG-05/07/13 | Se aclara que reservas, consulta especial y materia no se habilitan por área. Permanecen pendientes; no se suman como reglas completas. |

## Diseño y justificación

1. **Evolución explícita del registro firmado.** El payload 1.0 y sus bytes/firmas no se reinterpretan. La versión 1.1 incorpora `areas`, `ambitos` de mandatos y `cambiosArea`. Mismo sobre/dominio Ed25519, con versión dentro del contenido firmado y lector estricto. Los mandatos 1.0 nunca autorizan AREA. Las solicitudes sin área mantienen los digests anteriores; también se conserva la idempotencia de distribución sin área.
2. **Catálogo acreditado, sin editor libre TI.** Área estable por unidad/sede, nombre, jefatura COORDINACION/DIRECCION e intervalo finito. Se aprovisiona por el canal privado firmado U1.5, no por CRUD ordinario. Entradas omitidas permanecen hasta su vencimiento; cambiar unidad/sede de un ID existente se rechaza. Cambios de jefatura/fechas invalidan epoch; rótulos no amplían autoridad. Este catálogo describe ámbito funcional, no inventa adscripciones nominales del personal.
3. **Concesión AREA exacta.** U1.5 conserva separación canónica entre cuentas, control de credencial y ciclos de aprobaciones. El ámbito debe coincidir en unidad/sede/área y cubrir fechas. Una aprobación todavía no ejecutada no da acceso. Se exige enlace coherente a instrucción ejecutada para hacer efectivo el permiso de área. Quitar ese enlace falla cerrado; nunca degrada AREA a una concesión general.
4. **Compatibilidad con V11 sin alterar su CHECK.** El esquema histórico no admite AREA en `staff_assignment`. La fila ancla conserva rol/fechas y usa UNIDAD_SEDE con unidad/sede NULL, sin poder coincidir con recursos reales; `staff_assignment_area` acredita el ámbito. La identidad actual reconstruye AREA y exige aprobación. No se eliminan restricciones ni se graba un rol general sobre la unidad para simular el área. **Volver a un JAR que ignore `case_area` no es una reversión de seguridad certificada.**
5. **Política por recurso compartida.** `PoliticaAreas` alinea decisiones Java y filtros SQL. Con área: concesión explícita vigente y jefatura o responsabilidad profesional correspondiente. Sin área: régimen histórico explícito, sin inferir pertenencia. Una concesión AREA no permite históricos sin área. No se ratifican todos los permisos heredados por conservar compatibilidad.
6. **Consultas indirectas.** Listados, búsquedas, conteos, detalles, vínculos, reaperturas, adaptador documental, tareas y avisos usan autorización del recurso. El distribuidor conserva su consulta ordinaria del ingreso/destinos, pero no contenido, historia ni conteos de Casos protegidos por área. Se filtran también las derivaciones históricas cuyo Caso de origen ya tenga área; conservar una fila histórica no autoriza consultarla desde otro ámbito.
7. **Cambio interno sin facultad implícita.** Tener ambas jefaturas es condición necesaria de este corte, pero no autoriza el movimiento por sí solo. Se exige acreditación firmada específica de Dirección/delegación expresa, persona/credencial, unidad/sede, origen/destino, respaldo y fechas. `origen:null` solo sirve para clasificación histórica expresamente autorizada. La UI consulta destinos acreditados al servidor. Se revalida dentro de `identity_guard` y bloqueo de COM; auditoría de autoridad conserva revisión firmada, facultad y respaldo. Retirar o vencer esa facultad impide incluso repetir un comando anterior como éxito aparente.
8. **Atomicidad y preservación.** Cambio de área, revisión, historia, eventos, auditoría e idempotencia confirman juntos. Carrera produce un ganador y un conflicto; fallo de auditoría revierte todo. El responsable debe estar ya habilitado en destino. No se cambia profesional, tarea, original, COM, REQ o folio. Inicializar área en un histórico requiere además acceso histórico legítimo; no hay migración masiva por perfiles.
9. **Controles seguros mientras falta el resto.** Derivaciones desde Casos con área o hacia destinos con área siguen cerradas 409 hasta adaptar su ciclo completo. No se elimina el área para reutilizar el flujo antiguo. Casos AREA no heredan BAJA/autocierre central; la persona del confirmador debe ser distinta del responsable, incluidas cuentas alias. Esto no declara implementados todos los bloqueos de tareas/cierre ni la excepción regional.

El [contrato incremental](../contratos/logica-institucional-incremental-v1.md) contiene formato 1.1, rutas, estados, errores y semántica histórica. Referencias/hash del respaldo siguen siendo declaración del firmante; no se descargan documentos externos ni se certifican nombramientos reales.

## Componentes y migraciones

| Componentes | Cambios |
| --- | --- |
| `AutoridadesInstitucionales`, `ConcesionesInstitucionales`, `IdentidadInstitucional` | Registro 1.1; ámbitos exactos; prueba de ejecución; ancla compatible; epoch; facultad de cambio específica ligada a credencial y respaldo |
| Nuevos `CatalogoAreas`, `PoliticaAreas` | Catálogo acreditado y política efectiva de lectura/gestión Java y SQL |
| `NucleoInstitucional`, `NucleoConfiguration`, `NucleoController`, `ContratosNucleo`, `CatalogoLaboratorio` | Ámbito de destinos/Casos, consultas filtradas, API/historia/cambio de área; inyección de autoridad sin habilitarla por defecto |
| `RecursosNotificacion`, `NotificacionesInstitucionales` | Resolución por área de destinatarios/contextos; las operaciones documentales/tareas reutilizan autorización central |
| Nuevo `AreaCaso`, `Caso`, `Comunicacion`, `Antecedentes`, `Concesiones`, `Identidad`, `api` | Formularios y lecturas conectados; selectores según ámbito; historial real; sin controles de éxito simulado |
| V15.3 H2/MySQL | Siete tablas aditivas: catálogo, mandato/instrucción/asignación, destino/Caso e historial, más índice de área; no rellena ni modifica registros históricos |
| Nuevos `AreasFixture`, `AreasInstitucionalesTest`, `AreasHttpTest`, `AreasMigracionTest`, `AreaCaso.test`; regresiones adaptadas | Fixtures firmados, circuito real, autorización positiva/negativa, alias, concurrencia, rollback, consultas indirectas, migración y UI |
| `Test-AutoridadMySql.ps1`, `AutoridadMigracionTest`, `DatosNucleoTest` | Actualización aislada desde V14, V15.1 y V15.2; esquema adicional de áreas; sigue rechazando bases ajenas |

Antes de elegir V15.3 se verificaron historial y referencias: `origin/main` y la referencia local `feature/siger-u45-demostracion-manual` continuaban en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`. No existía rama U4.5 publicada en el remoto consultado; su referencia local no acredita cambios aún no publicados de otro worktree. Se inspeccionaron solo referencias Git, sin entrar ni alterar su directorio. V15.3 es posterior a 15.1/15.2; no se reserva V15 ni se usa outOfOrder. Los 28 hashes V1–V14 coinciden con el baseline; V15.1/15.2 no cambian. Los dos scripts V15.3 son idénticos por bytes.

## Validación y evidencia

Todos los participantes, respaldos y firmantes son sintéticos. Las concesiones del fixture atraviesan solicitud/aprobación/ejecución reales; no hay bypass de autoridad. Las filas de defecto/historia se inyectan explícitamente solo para probar fallo cerrado, rollback o compatibilidad; no se presentan como operaciones válidas habilitadas por API.

- Focalizado de áreas antes del último filtro de derivaciones: **22/22**, 19 dominio/seguridad, un circuito HTTP MockMvc y dos de migración. Frontend **74/74**, lint/build correctos. Las pruebas incluyen concesión exacta, unidad/sede/TI denegados, multirrol, alias, retirada de respaldo, revocación/sesión, expiración, cambio firmado, concurrencia, rollback y preservación de originales/asignación.
- Regresión completa anterior al último filtro: **344 backend, 335 aprobados, nueve omitidos, cero fallos/errores; 74/74 frontend**, lint/build y empaquetado correctos. Se conserva como corrida anterior, distinta de la final siguiente.
- **MySQL 8.0.46 nativo descartable**, seis pruebas sin omisiones, tras extender la autoridad específica: tres rutas V14/V15.1/V15.2 a V15.3 y equivalencia SQL, circuito de concesión concurrente/revocación, áreas, cambio/historia y cierre independiente. Instancia nueva, datos/credenciales nuevos, puerto efímero loopback, datadir privado, memoria limitada; apagado solo del proceso propio. No acredita MySQL operativo ni los privilegios reales de sus usuarios.

Se conserva la historia de fallos: al diseñar el ámbito se detectó que V11 rechazaba AREA; se resolvió con ancla inerte y vínculo, sin editar la migración. Se corrigieron errores intermedios de compilación/SQL y el conteo de destinos de distribución sin debilitar su aserción histórica. Una primera corrida completa tuvo **335 métodos, un fallo y nueve omisiones** por una aserción de temporales de `FormularioCompletoInstitucionalTest.originalIncompletoCorruptoOAusenteImpideRegistroYNoPublicaTemporal`. No se modificó ese código ni su prueba. Pasó después 1/1 aislada, la clase completa junto con áreas 38/38 y las corridas completas posteriores. **La causa inicial no está acreditada**; no se atribuye al antivirus ni se toca infraestructura. No se oculta la corrida fallida ni se añaden reintentos automáticos/exclusiones.

Nueve omisiones Windows corresponden a workers reales opt-in no configurados. Las tres exclusiones de estrés mantienen su tag histórico; sin cambios de POM, dependencias, lockfile o CI. H2, MockMvc, DOM y runners CI **no son E2E físicos**. No se ejecutó un nuevo E2E del Formulario, workers reales, Graph, CU/PISEE, consumidor ni integración productiva. npm conserva diez vulnerabilidades moderadas heredadas, sin actualización masiva.

Logs y XML privados bajo LOCALAPPDATA `siger-logica-u16-5b038f1f2ec949238c274ddda8bcf21f`; MySQL guarda cada instancia en `siger-autoridad-mysql-<UUID>`. No se publica configuración privada, sobres, contraseñas, datos personales, originales o logs completos.

## Git, compatibilidad y siguiente corte

Código confirmado en **`46270f2ccb6bb08f4f492bbd54c6f742e99cdc2f`**, `feat(logica): segregar Casos y concesiones por area acreditada`. El commit posterior contiene informe, contrato y matriz; no cambia Java/React/SQL/scripts ya probados. El SHA del informe se obtiene con `git log -1 --format=%H -- docs/LOGICA_U1_6_AREAS_2026-11-02.md`, evitando autorreferencia. Se publica por push normal a upstream existente `origin/feature/siger-logica-negocio` y se actualiza [PR #2](https://github.com/chashmdai/sistema-de-requerimientos/pull/2) **en borrador**. Los checks/cuerpo del PR y comprobante privado registran HEAD y CI Windows/Ubuntu del último SHA publicado; no se presupone éxito remoto antes de ejecutarse.

**Cierre local: Build-Siger.ps1 correcto el 02/11/2026 a las 23:56 America/Santiago, Java25/Node24 explícitos y temporales privados nuevos. Backend H2: 345 casos, 336 aprobados, nueve omitidos, cero fallos/errores; frontend 74/74, lint/build correctos, empaquetado aprobado.** Incluye 20 pruebas finales de dominio de áreas, un circuito HTTP y dos de migración, además de toda la regresión histórica. Tras ajustar únicamente los rótulos de conteos visibles se repitieron lint, 74/74 y build frontend; la CI del HEAD publicado empaqueta el checkout completo exacto. El artefacto local de desarrollo no sustituye el runtime QA ni se presenta como entregable instalado.

MySQL final independiente: **6/6 sin omisiones**, con la solución de código final, en `siger-autoridad-mysql-5e05d6c318d74179bd9f25fa318c8120`; ausencia de sus procesos propios comprobada. Log privado `build-cierre.log` SHA-256 `4ba8a6594865f8c29bc5fe0fd1313e0070f2066758b047c4abb364d6d4ab38fc`; `mysql-cierre.log` `c2e79fa53f3e5a395626b3d4d3203be2651cd1c83f307c50c2905eb950f1c7c1`. XML Surefire finales conservados en la misma carpeta privada U1.6. Ambos scripts V15.3: SHA-256 `8b5fafa1342395cb455a08fc8af4b189df883d55ca536b8ff93cf14db77e6c75` en el checkout Windows. Validación de sintaxis PowerShell sin errores, `git diff --check` correcto y búsqueda acotada de material privado sin coincidencias; no equivale a auditoría exhaustiva de secretos.

No cambian transporte Formulario–SIGER 1.0/1.1/V3, recibos/firma Ed25519, hashes/procedencia, originales/folios ni COMMIT→ACK. Los cambios están dentro de identidad/núcleo/interfaz y nuevas tablas; regresiones completas conservan los contratos existentes. No se acredita físicamente U4.5: deberá respetar U1.5 y ámbitos nuevos al preparar sus fixtures. Cambios concurrentes no publicados, DTO compartidos, configuración de autoridad y versiones Flyway requieren revisión antes de integración. La compatibilidad aditiva no autoriza desplegar un JAR antiguo ignorando áreas.

QA histórica Windows/WSL, bases históricas/U4, COM-000001 y evidencia, JAR activos, servicios, consumidor, Formulario y proveedores quedan fuera de las operaciones realizadas. No se reinicia ni corrige HTTP_503 WSL. No hay merge, auto-merge o despliegue.

Próximo incremento recomendado: **materia y lectura regional ordinaria de Direcciones UPR/UPD**, con catálogo acreditado, ámbito de lectura separado de gestión, rectificación auditada y cierre de permisos por materia retirada. Antes de ampliarlo, revisar el tamaño y corte U1 del PR #2; una etapa U2 amplia debe tener PR dependiente trazable, sin arrastrar U1–U9 incompletos. FIR/reservas y excepción ACC-10 necesitan controles propios y no se habilitan mediante concesión ordinaria AREA.

## Preguntas institucionales pendientes

1. Para una activación real, acreditar por canal privado firmante de confianza, vínculos persona/cuenta, catálogos/funciones de área, autoridades y respaldos/vigencias. El mecanismo sintético no acredita esos datos reales ni reemplaza fuente maestra/SSO/MFA.
2. Confirmar únicamente los datos institucionales aún ausentes del catálogo de materias y su correspondencia UPR/UPD para la futura lectura regional; no se solicita reconsiderar las reglas ya confirmadas del rector.
3. Las reservas extraordinarias y accesos sensibles requieren habilitaciones expresas por recurso/contenido y respaldo conjunto aplicable; no se activan a partir de esta entrega.
