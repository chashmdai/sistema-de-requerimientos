# SIGER · Continuidad U1.5 · Autoridad y concesiones

Fecha de desarrollo: 02/11/2026, Windows nativo. Continuidad de `9beeef9c3fc8b6d6f8f02d19cc54b048e6447b53`, exclusivamente en `C:\req\worktrees\siger-logica`, rama `feature/siger-logica-negocio`. No se repite U0 ni se reconstruyen U1.1–U1.4. Este informe es el corte vigente para U1.5; los informes anteriores conservan sus resultados históricos.

## Resultado y corte

Implementado el incremento vertical **solicitud TI → aprobación institucional independiente → ejecución TI → revocación autorizada**, con persistencia, API, interfaz y auditoría conectadas. Se reutilizan roles/ámbitos actuales, cuentas, identidad estable, sesiones/epoch, guard transaccional, exclusividad de distribución y controles HTTPS/CSRF existentes.

La autoridad funcional se acredita mediante mandatos firmados, con persona canónica, función, acción, rol/ámbito exactos, respaldo y vigencia; no se obtiene de un cargo ni del rol TI. No hay autoridades reales, claves privadas o aprobaciones sembradas. Sin una confianza y acreditación válidas, el circuito queda cerrado. Los tests utilizan identidades/firmantes sintéticos y el mismo circuito real de servicios; no se convierte el organigrama de laboratorio en autoridad institucional.

**U1 sigue parcial.** No se habilitan autoridades por área, reservas/FIR, excepciones sensibles ACC-05/10, lectura de Direcciones por materia, delegaciones de gestión o subrogancias. En particular, un mandato para conceder un rol ordinario no sustituye la aprobación conjunta Gestión/jefatura para contenido protegido. No se inicia U2 sobre esas brechas. Se conserva un corte revisable U1 en PR #2 en borrador, sin acumular U2–U9.

## Reglas y matriz de 138 IDs

La matriz conserva exactamente los 138 IDs y sus requisitos. Se revisó el impacto contra todos ellos: seis filas actualizan evidencia; las otras 132 mantienen el estado previo, porque este incremento no cambia su comportamiento institucional. Las regresiones de esos componentes acreditan conservación técnica, no el modelo futuro pendiente.

| Regla | Evidencia nueva y límite |
| --- | --- |
| ACC-01 | Aprobador independiente de solicitante/beneficiario por persona; ejecución TI independiente; respaldo firmado; denegación de otorgamiento directo. Parcial por área/excepciones pendientes. |
| ACC-02 | Soporte parametrizable de acreditación por unidad/sede y vínculo cuenta/persona; no acredita titulares reales ni área. |
| ACC-03 | Cada concesión conserva rol/ámbito/fechas; no combina mandatos distintos para autorizar. Regresión multirrol conservada. |
| ACC-05 | TI no hereda autoridad institucional ni lectura de expedientes. Excepciones sensibles pendientes y sin nuevo permiso. |
| ACC-06 | Revocación institucional de nuevas concesiones, epoch y sesiones, historia intacta; no reasigna carteras. |
| ACC-10 | Infraestructura de respaldo, alcance y vigencia reutilizable; **no se declara implementada la excepción sensible**. |

ORG-02/11, REG-DIS-01 y ACC-04 conservan su cobertura anterior mediante regresiones. ORG-03/04/05/07 y ACC-07/09 siguen pendientes de sus políticas de área, materia y reserva. No se cambia U5 histórico: responder/canalizar/escalar permanecen 409. No se confunden sus Solicitudes con SOL institucional U2.

## Arquitectura y decisiones

1. **Raíz fuera de las concesiones administradas.** Registro Ed25519 importado desde archivos privados, con clave pública explícita. No hay endpoint para configurar confianza o fabricar mandatos. Función/cargo no implica facultades. La referencia y hash del respaldo son una declaración del firmante acreditado; no se descarga ni certifica el documento externo.
2. **Cuenta y persona distintas.** Vínculos firmados permiten reconocer cuentas alias. No se permite reasignar una cuenta ya acreditada a otra persona. Se comprueba separación de personas en cada comando y reintento, además de cuentas activas/credencial habilitada.
   La acreditación vincula además la credencial del aprobador mediante un compromiso SHA-256 privado del hash bcrypt codificado. Reset TI y cambio personal suspenden el ejercicio de mandatos hasta una nueva acreditación independiente. Se prueba el ataque completo de reset, cambio temporal e intento de aprobar/ejecutar; no basta completar el cambio para recuperar autoridad. No se divulga ese compromiso por API/auditoría ni se cambia el formato de tablas históricas.
3. **Registro vigente e íntegro.** Versiones actual+1; replay idéntico de la actual idempotente; conflicto/salto/rollback rechazados. Cada uso vuelve a verificar firma con la confianza actual y deriva mandatos del payload firmado. Retirar/cambiar una clave no autentica registros previos con el mismo ID de clave. Una proyección SQL no amplía autoridad.
4. **Mandatos exactos y finitos.** Rol, ámbito y unidad/sede deben coincidir y cubrir la vigencia; no se supone una jerarquía expansiva. No se admite ámbito AREA ni un permiso nuevo de reserva. Registrar una solicitud/aprobarla no crea rol.
5. **Separación conservadora de personas.** TI puede preparar y ejecutar una instrucción para otra persona, pero no aprobarla. El aprobador no puede ser el solicitante o beneficiario, ni el ejecutor o beneficiario ser la persona del aprobador. Los ciclos de aprobaciones con intervalos superpuestos se rechazan bajo la misma guarda, incluso de tres o más personas. Es una restricción técnica conservadora; no se inventa una excepción institucional.
6. **Transacción única.** Estado/revisión, asignación, vínculo, epoch, evento e idempotencia confirman juntos. Auditoría fallida revierte la concesión. Denegaciones 403/404/409 se conservan separadas del rollback sin payload sensible denegado. Una aprobación cuyo aprobador ya no está activo devuelve 409; no simula que caducó la sesión del TI.
   También se revalidan beneficiario, solicitante y plazo en ejecución/reintento. No se inserta una asignación para un beneficiario desactivado tras la aprobación. La baja del solicitante no invalida ficticiamente la sesión de otro TI. Rechazo y revocación de concesiones de cuentas desactivadas siguen disponibles.
7. **Historial y revocación explícitos.** Registro nuevo invalida aprobaciones aún no ejecutadas; no reinterpreta ni retira implícitamente concesiones ya ejecutadas con su vigencia aprobada. Para estas se usa revocación institucional o desactivación de cuenta. La protección del último TI sigue vigente.
8. **Compatibilidad histórica sin bypasses.** Se conserva `staff_assignment` y su SQL anterior. No se estampan aprobaciones inventadas a roles previos. La API antigua no concede roles nuevos; para asignaciones respaldadas tampoco permite saltar la revocación institucional. El bootstrap vacío sigue separado, solo TI inicial, sin raíz institucional.

La UI informa acreditación pendiente, muestra mandatos propios, prepara solicitudes TI, permite decisiones fundadas, ejecuta una aprobación independiente y revoca con historial real. No hay confirmaciones simuladas. El backend revalida la separación canónica aunque la UI solo conozca IDs de cuenta. La lista limita la consulta a las últimas 500 instrucciones y filtra autorización; historial entrega los últimos 100 eventos conservando todos en persistencia. Paginación de estos metadatos queda como mejora, sin contadores globales.

API, esquema del sobre, propiedades de configuración y semántica de reintentos están en [contrato incremental](../contratos/logica-institucional-incremental-v1.md). No se proporcionan configuraciones operacionales ni claves privadas en el repositorio.

## Componentes modificados

| Componente | Cambio |
| --- | --- |
| Nuevos `AutoridadesInstitucionales`, `ConcesionesInstitucionales` | Firma y vigencia, identidad canónica, mandatos, ciclo, comandos/versiones, auditoría/transacciones |
| Nuevos `ConcesionesConfiguration`, `ConcesionesController` | Configuración privada opcional y API autenticada; bootstrap TI se ordena antes de la importación privada |
| `IdentidadInstitucional` | Cierre de otorgamiento directo; acceso interno transaccional para instrucciones; protección de revocación; vínculo en DTO de asignación |
| Nuevo `Concesiones.tsx`, `api`, `App`, `Identidad` | Recorrido conectado y navegación; administración TI deriva concesiones nuevas y revocaciones respaldadas al circuito |
| V15.2, ambos motores | Registro/sobres/vínculos/mandatos, instrucciones, vínculo a asignación, eventos y comandos |
| Pruebas nuevas y existentes | Dominio, HTTP autenticado, configuración privada, migración, UI; fixtures anteriores atraviesan el circuito real |
| `Test-AutoridadMySql.ps1` | Instancia nativa nueva, privada, limitada a loopback/puerto efímero, datos/credenciales nuevos, cierre exclusivo de procesos propios |
| Informe/contrato/matriz | Cobertura, decisiones, evidencia y límites actualizados |

En tests se conserva el escenario de distribución histórica vencida mediante un registro SQL explícitamente histórico en H2 descartable; el flujo nuevo no fabrica autorizaciones retroactivas. La prueba de reactivación obtiene la concesión mientras la cuenta está activa, la desactiva y luego prueba el conflicto de reactivación. Los demás helpers de concesión crean firmante/revisor sintéticos y ejecutan solicitud/aprobación/ejecución reales. No hay flag para saltarse la aprobación ni llamada de producción abierta para fixtures.

## Migraciones y compatibilidad

Antes de elegir versión, `main` remoto y referencia Git U4.5 continuaban en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`, con V1–V14. Esta rama tenía V15.1. Se elige **V15.2**, no V15, sin `outOfOrder` ni edición de versiones previas. Los scripts H2/MySQL son idénticos y aditivos; ninguna tabla histórica cambia. Se conserva V15.1 y el baseline V1–V14.

Pruebas de actualización desde V14 y desde V15.1 conservan exactamente la cuenta y asignación anteriores, no crean autoridad ni aprobación histórica, migran a 15.2 y ejecutan concesión/revocación con dos conexiones concurrentes. El SQL anterior permanece válido; regresar a un JAR que ignore los controles de autorización **no es una reversión de seguridad aprobada**. Un futuro despliegue deberá revisar historia real y versiones concurrentes por el canal correspondiente; aquí no se ejecuta.

No cambian contratos de transporte 1.0/1.1/V3, firmas/recibos, COM/folios, originales, procedencia o COMMIT→ACK. No hay cambios de producción en paquetes de integración/documental/núcleo ni dependencias/CI; se ajustan fixtures documentales/notificaciones para respetar la concesión. U4.5 no se modifica ni se ejecuta: deberá adoptar este circuito para generar nuevos roles; los helpers de otorgamiento directo ahora obtendrán 409. No se integran cambios ajenos ni se supone acreditada su demostración física.

## Pruebas y evidencia

Primera selección consolidada: **30/30 backend**, cero fallos/errores/omisiones, antes del último endurecimiento explícito del lector de campos desconocidos. Tras vincular la credencial del aprobador: **31/31**, incluidos 17 tests de seguridad/dominio/concurrencia, dos de configuración privada, nueve HTTP y tres casos de migración/igualdad de scripts. Frontend focalizado inicial: 16/16; después del ajuste para mandatos separados de aprobación/revocación: **67/67 frontend**, lint/build correctos. No depende del orden de mandatos para ofrecer cada acción.

MySQL **8.0.46 nativo Windows**, instancia creada para esta sesión y luego detenida: tres pruebas correctas, sin omisiones, con las dos rutas de actualización y concesión/ejecución idempotente concurrente/revocación. Repetidas mediante el auxiliar versionado en una segunda instancia nueva: correctas. Uso exclusivo de esquemas `siger_u15_test_<UUID>_14/_151`, memoria limitada y loopback en puerto efímero; no se reutilizaron servidor, credenciales o configuración de QA. Esta evidencia acredita MySQL descartable, **no MySQL operativo ni proveedores**.

Ensayos fallidos conservados por separado: primer intento MySQL denegó conexión por el host de la instancia nueva; se corrigió su aprovisionamiento privado. La primera corrida completa H2 tuvo 316 métodos, un fallo por el tratamiento 401 al retirar al aprobador y nueve omisiones históricas; se corrigió a aprobación obsoleta 409, sin retirar la aserción de ausencia de rol. Una compilación intermedia de pruebas detectó visibilidad de los métodos de configuración, corregida para probar el canal privado real. No se cambian exclusiones ni dependencias para lograr verde.

Un segundo build completo anterior al vínculo de credencial pasó **318 backend, 309 correctos y nueve omisiones; 66/66 frontend, lint/build**, y se conserva separado. La revisión posterior detectó que TI podía intentar apropiarse de la credencial local de un aprobador mediante reset; se añadió el compromiso firmado y su prueba de ataque. Después se acreditaron 319/67 antes de los dos tests adicionales de desactivación; estos ensayos no sustituyen la corrida final siguiente.

## Hito consolidado, evidencia y Git

**`Build-Siger.ps1` final, Windows nativo / Java25 explícito / Node24 / Maven / temporales privados nuevos: BUILD SUCCESS, 02/11/2026 22:14 America/Santiago. Backend: 321 casos, 312 aprobados, nueve omitidos, cero fallos/errores. Frontend: 67/67, lint y build correctos.** Incluye los 19 tests finales de dominio/seguridad/concurrencia y las regresiones de identidad, temporalidad, documentos, ingesta/recibos/idempotencia/COMMIT→ACK, núcleo, notificaciones, tareas, U5 y gates. Los dos escenarios de desactivación pasaron primero 19/19 focalizados y después dentro de esta corrida completa.

Nueve omisiones: workers reales opt-in no configurados; tres stress mantienen su exclusión histórica. No se añade exclusión, no se modifica POM, lockfile o CI. H2, MockMvc y DOM no son E2E físico. No se ejecutaron proveedores, ClaveÚnica/PISEE/Graph reales, consumidores institucionales ni validación física del Formulario. MySQL descartable se volvió a ejecutar con la solución final mediante el auxiliar versionado: **3/3**, sin omisiones, y se confirmó la ausencia de instancias propias activas.

Los 28 hashes V1–V14 coinciden con el baseline. V15.1 mantiene SHA-256 `683bd9131ad2230958d4e1572e855194ae8697a87c26dacd0f7d7dd32b9bffde`; ambos scripts V15.2 tienen `66431905fff6a4b69e6b9bd2fc9b6fbf3d8b2f4bdd90a93322fcf234e2a86baa` en este checkout Windows. El rector conserva el hash de referencia y no se incorpora a Git.

Artefacto solo de desarrollo, `siger-institucional/backend/target/siger-institucional-0.1.0-SNAPSHOT.jar`: SHA-256 `94a666bdc32403861ede67442fbe5f41a7c991377b3bce1ece690374b02a6c2d`. Log `build-cierre.log`: `87ce902614a59fb3dec5ebc39edcc8cf270128f35ed7c249e94a753de561f9b3`. Logs y XML Surefire finales se conservan en el directorio privado `siger-logica-u15-e2ca3fd33d3e41ea8d9150b46b12eeac` bajo LOCALAPPDATA; evidencia MySQL final en `siger-autoridad-mysql-b4df66fee1224e51a752d69c61911137`. No se publican logs, credenciales de prueba, sobres de autoridad o archivos privados. Búsqueda de patrones/nombres sensibles y `git diff --check` correctos; no equivale a una auditoría exhaustiva de secretos.

Código y pruebas confirmados en **`739e3fcdeb8a68b5410252a003a3a5410ecdf9f8`**, `feat(logica): exigir autoridad independiente para concesiones de acceso`. El commit siguiente contiene el contrato, la matriz y este informe; no cambia Java/React/SQL/scripts/pruebas ya validados. El SHA del informe se resuelve con `git log -1 --format=%H -- docs/LOGICA_U1_5_AUTORIDAD_2026-11-02.md`, evitando una autorreferencia imposible.

Publicación: push normal de `feature/siger-logica-negocio`, con upstream existente `origin/feature/siger-logica-negocio`; [PR #2](https://github.com/chashmdai/sistema-de-requerimientos/pull/2) contra `main` permanece en borrador, sin merge/auto-merge/despliegue. El comprobante final de entrega registra HEAD local/remoto, árbol Git, push y los IDs/resultados CI Windows/Ubuntu del último HEAD publicado; su evidencia remota queda en el cuerpo/checks del PR. No se atribuye aquí un resultado remoto antes de su ejecución. La CI usa H2 y dobles; MySQL nativo se acredita separadamente arriba.

Fetch previo al cierre: `origin/main` y referencia Git `feature/siger-u45-demostracion-manual` continuaban en G0. No se integran cambios ajenos ni se inspecciona/modifica su directorio, HEAD operativo o configuración. No hay colisión Flyway confirmada en esas referencias; cambios aún no publicados deben coordinarse con su responsable antes de integración. No se reserva retrospectivamente V15 ni se activa `outOfOrder`.

## Brechas, riesgos y próximo incremento

- Área y materia todavía no forman parte del ámbito de recurso. No acreditar ORG-03/04/07 con un rol de unidad/sede más amplio. Siguiente incremento recomendado: ámbitos de área y política efectiva de consulta/gestión/documentos, con migración compatible y tests de segregación.
- FIR y excepciones sensibles requieren la política específica y aprobación conjunta, con recurso/acción/contenido; este circuito ordinario no las habilita. Los permisos históricos tampoco reciben una ratificación institucional por esta entrega.
- Aprovisionamiento real requiere fuente maestra de persona/cuenta, firmante de confianza y respaldos de autoridades competentes. No hay nombres reales ni activación automática.
- La creación de cuentas/perfiles y organización técnica histórica no se convierte aquí en un circuito integral de instrucciones; su evolución sigue pendiente. No se declara completa ACC-01/02 ni U1.
- El guard global existente privilegia integridad y una implementación revisable; puede limitar throughput de administración, sin afectar las integraciones. Registro acotado y listado limitado requieren evolución si crece el volumen.
- Se mantiene el guard MySQL de pruebas heredado que menciona la QA histórica: **no se invocó**. El nuevo auxiliar usa propiedad/validación propias y bases nuevas; no usa `ROBUST_DB_*`. Ese guard histórico no transforma una QA preservada en descartable ni autoriza su uso.
- El humo MySQL usa el usuario exclusivo de la instancia descartable, no las cuentas operacionales de aplicación/migración. No acredita sus permisos reales; el aprovisionamiento autorizado deberá contemplar DML/lectura de las tablas nuevas.
- npm conserva diez vulnerabilidades moderadas heredadas; no se modificaron dependencias ni lockfile. El lanzador Python del perfil aprovisionó automáticamente Python 3.15.2 al invocarse para editar un test; no participa en el build SIGER. Las siguientes ediciones usaron PowerShell/patch; no se intentó revertir a ciegas una herramienta del perfil.
- U2 necesitará SOL institucional única, REQ sin CAS, principal/asunción y estados conectados, con un corte separado. No se inicia mientras falten sus políticas necesarias ni se abre un PR apilado incompleto.

QA/históricos/U4, COM-000001/ACK/originales, JAR activos, servicios y worktree U4.5 permanecen fuera de las operaciones realizadas. No se interviene HTTP_503, Formulario, CU/PISEE/Graph, Cloudflare o servicios compartidos. Las únicas instancias nuevas fueron descartables de pruebas. No hubo merge, auto-merge o despliegue.

## Preguntas institucionales pendientes

1. Para activar autoridades reales: acreditar por canal privado el responsable de confianza/firma, vínculos persona/cuenta y respaldos/vigencias de mandatos. No hace falta inventarlos para continuar el desarrollo sintético.
2. Para áreas/reservas: aportar catálogo y habilitaciones nominales según las dependencias PE del rector; se conservan las decisiones funcionales ya confirmadas. No se solicita reconsiderar la separación TI/institucional ni permitir autoaprobación.
