# Continuidad de lógica institucional · U1.3 en adelante

## Partida y alcance

Continuación autorizada desde `d19fa87d772041455549fd73583d3b2bc96eee0b`, rama `feature/siger-logica-negocio`, worktree exclusivo `C:\req\worktrees\siger-logica`. Se comprobaron HEAD/upstream/remoto iguales y árbol limpio. `origin/main` seguía en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`. Se reutilizan [U0](LOGICA_U0_DIAGNOSTICO_2026-11-02.md), [matriz de 138 reglas](LOGICA_MATRIZ_REGLAS_2026-11-02.csv), [entrega anterior](LOGICA_ENTREGA_2026-11-02.md) y [contrato incremental](../contratos/logica-institucional-incremental-v1.md), sin repetir el diagnóstico ni reconstruir U1.1/U1.2.

El rector local mantiene SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`. Se contrastó §05/ACC-04 para credencial inicial y §05.6/ACC-01 para separación de autorización y administración. No se incorpora el rector a Git ni se leen secretos o configuraciones operacionales.

## U1.3 · ACC-04: credencial temporal y cambio personal

Altas nuevas —incluido bootstrap de un laboratorio nuevo— y restablecimientos TI marcan la credencial como temporal. Una sesión temporal solo permite GET sesión/CSRF y POST cambio personal/logout. La sesión no expone operador, roles, catálogo o contenido de gestión hasta completar el cambio. El filtro de backend protege también llamadas directas a API/documentos; ocultar la navegación no es el control de autorización.

POST `/nucleo/clave-personal` verifica sesión individual/epoch, HTTPS, CSRF, contraseña actual y política existente de 16 caracteres/72 bytes UTF-8 para la nueva. Rechaza reutilizar la actual y campos de actor/cuenta enviados en JSON. Usa bcrypt existente y `identity_guard`; persiste hash, estado, epoch y evento `CLAVE_PERSONAL_CAMBIADA` en una transacción. No almacena contraseña, huella del comando con secretos ni respuesta recuperable de credencial. Ante reenvío posterior al éxito, el epoch anterior ya no puede mutar: requiere login con la clave nueva. No es una orden institucional idempotente con caché de payload.

El éxito invalida la sesión actuante y deja obsoletas las demás mediante epoch. La interfaz vacía sus campos y vuelve al login; no autentica automáticamente ni conserva credenciales en storage. Hay límite de ocho intentos de cambio por cuenta/minuto en el proceso, adicional a los límites de login existentes. Es una defensa local de laboratorio, no un rate limiter distribuido ni política institucional de bloqueo permanente. La administración TI muestra el estado pendiente y explica que el reset vuelve a exigir cambio.

### Persistencia y compatibilidad

V15.1 (`V15_1__credencial_temporal.sql`) agrega **solo `staff_credential_state`** por actor. No altera la forma de `staff_account`, sus columnas ni los INSERT posicionales históricos; los nuevos INSERT de Java nombran columnas. La ausencia de estado preserva cuentas anteriores sin imponer un reset retroactivo. Altas/reset del nuevo servicio escriben el estado explícitamente. No se convierten cuentas de demostración en usuarios institucionales.

Se descartó antes de publicar una primera solución que añadía columna a `staff_account`: aunque sus pruebas eran correctas, rompía el INSERT posicional del JAR anterior. La tabla separada conserva compatibilidad de esquema/SQL. No se habilita operación paralela de escritores antiguos para eludir el nuevo control; la versión de negocio nueva necesita su artefacto/UI juntos y el corte autorizado correspondiente. Volver a un JAR antiguo que no aplica la política no es una reversión de seguridad aprobada.

Se revisaron las migraciones en `main`, rama propia y referencia Git de U4.5: V1–V14; la referencia de U4.5 aún apuntaba a G0 al comprobarla. No se leyó ni modificó su directorio. La versión 15.1 deja libre V15 simple y reduce colisión, pero **no sustituye coordinación**: antes de integrar/desplegar se deben revisar de nuevo todas las versiones/orden. No activar `outOfOrder` para acomodar una migración ajena tardía. Ninguna de estas migraciones se aplica a QA, histórico o U4 preservada.

Los scripts H2/MySQL son equivalentes. La prueba de evolución parte de V14, conserva todos los valores de la cuenta anterior y verifica que el SQL de INSERT anterior sigue funcionando tras V15.1. Eso acredita H2 y compatibilidad estática del SQL; no acredita ejecución MySQL. El `ON DELETE CASCADE` del estado se limita al registro técnico dependiente de la cuenta; no hay API de borrado de cuentas y no se eliminan actores, eventos o evidencia histórica.

### Evidencia y alcance

Pruebas nuevas: `CredencialTemporalHttpTest`, `CredencialTemporalMigracionTest`, `CambioClave.test.tsx`. Cubren sesión temporal sin gestión, cambio/login nuevo, errores sin mutación, HTTPS/CSRF, actor no manipulable, reset TI, dos sesiones concurrentes, límite de intentos, conservación de V14, igualdad de scripts y UI conectada al endpoint personal. Las regresiones HTTP antiguas preparan explícitamente cuentas habilitadas mediante el servicio real de cambio, sin flags de bypass ni SQL que borre la obligación.

La primera selección ejecutó 41 métodos backend: 40 correctos y un fallo en la comparación de claves de mapa JDBC del test de migración; fue corregido sin relajar controles. React focalizado: 26/26, lint/build correctos. La primera implementación por columna pasó 292 tests/58 React, pero se conserva como ensayo previo descartado por compatibilidad, no como evidencia de la solución final por tabla.

La solución final por tabla pasó `Build-Siger.ps1` en Windows nativo/Java25/Node24: **292 backend, 283 correctos, nueve omitidos, cero fallos/errores; 58/58 frontend, lint/build correctos**. Después se agregaron y ejecutaron **dos tests transaccionales adicionales, ambos correctos**, en `CredencialTemporalTransaccionTest`: fallo de auditoría revierte hash/estado/epoch; reset TI y cambio personal compiten por la misma revisión y solo uno confirma. No se suman como si la corrida completa anterior hubiese incluido esos dos tests; el hito combinado posterior los incluirá.

V1–V14 conservan sus 28 hashes de baseline. Los dos scripts V15.1 comparten SHA-256 `683bd9131ad2230958d4e1572e855194ae8697a87c26dacd0f7d7dd32b9bffde` en este checkout Windows. No hay ejecución MySQL ni migración sobre una base persistente. Las nueve omisiones siguen siendo workers reales opt-in y se conserva el tag stress histórico sin nuevas exclusiones.

## U1.4 · ACC-01: impedir concesiones a la propia cuenta TI

La API de concesión actual no puede representar un aprobador institucional independiente ni su respaldo. Por eso se deniega `otorgar(admin,beneficiario,...)` cuando ambos IDs son la misma cuenta, incluso si TI está vigente. Es una restricción conservadora de esta API, no una afirmación de que una autorización institucional independiente nunca pueda respaldar una futura ejecución sobre la propia cuenta. Ese mecanismo aún debe desarrollarse.

El actor se deriva de la sesión vigente; no existe un campo JSON para reemplazarlo. El backend retorna 403 `AUTOASIGNACION_NO_PERMITIDA`; no inserta asignación, no cambia epoch/perfil ni invalida una sesión por el solo rechazo. Cada intento deja `AUTOASIGNACION_DENEGADA` en `identity_event`, con actor/beneficiario y hora del servidor, bajo el guard existente. La auditoría confirma antes de devolver la denegación; si no puede persistir, falla cerrado. No guarda el payload, no genera correo ni se considera actuación de expediente. Reintentar genera otro hecho de acceso denegado, no otra concesión.

La UI recibe el ID del operador autenticado y, al editar su cuenta, conserva historial y revocación pero omite el formulario de otorgamiento, con explicación. El backend sigue denegando llamadas directas. La revocación propia conserva el control del último TI; no se amplía ni elimina esa guarda.

El bootstrap sigue siendo una operación distinta, de laboratorio vacío y configuración privada, con rol TI inicial fijo y exclusión transaccional: no usa el comando ordinario de concesión y no admite repetirlo con cuentas existentes. No crea permisos operativos por inferencia. No se eliminan ni reinterpretan roles históricos ya otorgados. No se modifica la cuenta multirrol QA ni los helpers operacionales para evitar la denegación.

Pruebas nuevas: `IdentidadInstitucionalTest.acc01TiNoSeConcedeRolesNiConsumeRevisionYAuditaCadaDenegacion`, `IdentidadHttpTest.acc01ApiTiNoPermiteOtorgarseUnRolOperativo` e `Identidad.test.tsx`. Antes del cambio: dos fallos backend (aceptación en vez de 403) y uno UI (concesión propia ofrecida). La matriz conserva ACC-01 **parcial**: esto no acredita aprobación/respaldo de concesiones a terceros, autoridad por área ni identidad maestra que relacione varias cuentas con una persona real.

## Pendientes de U1 y gates posteriores

No se declara U1 completa: autoridad por área, respaldos/aprobación de concesiones, reservas y excepciones, lectura de Direcciones por materia y demás atribuciones siguen pendientes. El control de credencial no resuelve fuente maestra, SSO/MFA, recuperación corporativa ni habilitación nominal (PE-02).

La prevención de autoelevación debe distinguir cuenta técnica de persona institucional: bloquear una concesión a la propia cuenta es un control necesario, pero no prueba identidad única entre cuentas ni evita por sí solo aprobaciones cruzadas. La autoridad y sus respaldos deben modelarse antes de acreditar ACC-01/10 completos. Las funciones U5 históricas continúan 409; U2 requiere su nueva SOL/principal/asunción y no se habilita mediante atajos a preparación U5. U2 y posteriores no se declaran implementados en esta continuidad.

Se mantienen fuera de alcance QA/histórico/U4, originales/COM-000001/ACK, JAR de `C:\req\bin`, procesos/servicios/certificados/custodias, WSL/HTTP_503, Formulario, Cloudflare, Graph/CU/PISEE reales y directorio U4.5. Las pruebas usan H2 nuevos, MockMvc/DOM y dobles existentes; no son E2E físicos ni habilitación productiva.

## Hito consolidado y trazabilidad

**Build final Windows nativo/Java25/Node24, `Build-Siger.ps1`: correcto el 02/11/2026 a las 20:08 America/Santiago. Backend: 296 métodos, 287 correctos, nueve omitidos, cero fallos/errores. Frontend: 59/59, lint y build correctos.** Incluye U1.3/U1.4, los dos tests transaccionales adicionales y regresión de U1.1/U1.2, identidad, documental, ingesta/recibos/COMMIT→ACK, notificaciones/tareas, U5 histórico y gates. Las nueve omisiones son workers reales opt-in; tres stress conservan su exclusión previa. No se cambian POM, dependencias, lockfile, script de build o CI para lograr verde. npm conserva 10 vulnerabilidades moderadas heredadas, sin actualización masiva ajena al alcance.

Artefacto de desarrollo solo en `siger-institucional/backend/target/siger-institucional-0.1.0-SNAPSHOT.jar`: SHA-256 `2cfa3f19b95de6beb2ed9ff5b9b349f8e8f21e32fd656cbae1aba1eb74a764b7`. Log final privado `build-u14-final.log`: SHA-256 `260b8f485d502910f113a48cda24d0717b1eb63dcc7355eb07b8aae9a34141f8`. Logs iniciales/solución descartada/red-green y Surefire final se conservan en el directorio privado `siger-logica-u13-8a4d53b3c2f24baca9d1677c490db207` bajo LOCALAPPDATA. No se publican contraseñas, originales, DPAPI, configuraciones privadas ni logs completos. La inspección de diff/nombres/patrones sensibles no mostró candidatos nuevos; no es una garantía universal de ausencia de secretos.

La matriz mantiene exactamente **138 identificadores**. Se actualizan ACC-04 (credencial temporal acreditada, PE-02 pendiente) y ACC-01 (denegación propia acreditada, autorización integral pendiente). Se conservan las coberturas acreditadas U1.1/U1.2 y el resto de pendientes, sin convertir regresiones históricas en aceptación del modelo futuro. No se repite U0 ni se declara cubierto un escenario de reserva/área que no tiene implementación.

| Componentes | Cambios |
| --- | --- |
| `IdentidadInstitucional`, nuevo `CuentaPersonalController` | Estado de credencial, cambio propio, reset temporal, transacción/epoch/eventos y denegación de concesión propia |
| `SeguridadInstitucional`, `NucleoController` | Sesión temporal restringida, DTO sin operador/capacidades durante el cambio, límite de intentos |
| `App`, nuevo `CambioClave`, `Identidad`, `api` | Flujo inicial, retorno al login, mensajes, estado TI y eliminación de concesión propia en UI |
| V15.1 H2/MySQL | Tabla técnica separada, sin alterar V1–V14 ni forma de `staff_account`; pruebas de expansión desde V14 |
| Pruebas | Tres clases backend nuevas de credencial, UI nueva, extensión de pruebas de identidad y preparación explícita de credenciales en fixtures HTTP existentes |
| Contrato/informes/matriz | Semántica, evidencias, pendientes y límites actualizados |

### Git, PR y CI

U1.3 está confirmado/publicado en **`37f735bed3173a4ec457ea8ccab53b653b67c886`**. Su [CI 41542276666](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41542276666) terminó correctamente en [Windows](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41542276666/job/125609155213) y [Ubuntu](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/41542276666/job/125609154687). Son runners remotos, no workers documentales reales ni MySQL. El commit siguiente contiene U1.4 y este cierre probado localmente; los checks del HEAD publicado se verifican en el [PR #2](https://github.com/chashmdai/sistema-de-requerimientos/pull/2) y su comprobante final, sin atribuir resultados futuros a esta redacción.

Se mantiene `feature/siger-logica-negocio` y push normal/upstream, PR #2 en borrador. El HEAD exacto del informe se resuelve con `git log -1 --format=%H -- docs/LOGICA_CONTINUIDAD_U1_2026-11-02.md`; el comprobante privado y la entrega de chat registran el SHA final/CI/remoto/árbol limpio. No se abre PR apilado porque los cambios siguen dentro de U1 y dependencias inmediatas. No hay merge, auto-merge o despliegue.

Fetch de `main` previo al cierre y referencia Git U4.5 seguían en G0. No se integraron cambios ajenos, no se modificó su worktree, ni se migraron/provisionaron recursos operacionales. La compatibilidad U4.5 es de código/esquema preparados y pruebas aisladas; nuevos operadores de laboratorio deberán completar cambio de credencial y los helpers que intenten autoasignarse roles deberán respetar la autorización independiente. No se valida su demostración operacional desde esta rama.

### Próximo incremento

Diseñar/completar autoridad funcional por unidad/sede/área e instrucciones de concesión con aprobador, respaldo y vigencia, aplicando la misma política a lectura/gestión/documentos y separando facultad institucional de ejecución TI. No falta una nómina para desarrollar ese mecanismo con fixtures; sigue siendo trabajo de ingeniería pendiente, no una razón para inventar autoridades. Después: FIR/excepciones y Direcciones por materia, antes de activar los recorridos U2 que dependan de esas facultades. Se entrega un punto estable con dos incrementos cerrados y recuperables, sin iniciar una migración amplia de SOL/REQ/CAS a medias.

## Preguntas y dependencias institucionales

No se requieren respuestas nuevas para el cambio de credencial. Se mantienen las entradas PE nominales/de autoridad/reserva/catálogo/calendario/aceptación del rector; no se repreguntan decisiones ya confirmadas. La continuidad de ingeniería debe completar el modelo parametrizable y sus bloqueos antes de solicitar o activar datos nominales.
