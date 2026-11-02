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

## Pendientes de U1 y gates posteriores

No se declara U1 completa: autoridad por área, respaldos/aprobación de concesiones, reservas y excepciones, lectura de Direcciones por materia y demás atribuciones siguen pendientes. El control de credencial no resuelve fuente maestra, SSO/MFA, recuperación corporativa ni habilitación nominal (PE-02).

La prevención de autoelevación debe distinguir cuenta técnica de persona institucional: bloquear una concesión a la propia cuenta es un control necesario, pero no prueba identidad única entre cuentas ni evita por sí solo aprobaciones cruzadas. La autoridad y sus respaldos deben modelarse antes de acreditar ACC-01/10 completos. Las funciones U5 históricas continúan 409; U2 requiere su nueva SOL/principal/asunción y no se habilita mediante atajos a preparación U5. U2 y posteriores no se declaran implementados en esta continuidad.

Se mantienen fuera de alcance QA/histórico/U4, originales/COM-000001/ACK, JAR de `C:\req\bin`, procesos/servicios/certificados/custodias, WSL/HTTP_503, Formulario, Cloudflare, Graph/CU/PISEE reales y directorio U4.5. Las pruebas usan H2 nuevos, MockMvc/DOM y dobles existentes; no son E2E físicos ni habilitación productiva.

## Preguntas y dependencias institucionales

No se requieren respuestas nuevas para el cambio de credencial. Se mantienen las entradas PE nominales/de autoridad/reserva/catálogo/calendario/aceptación del rector; no se repreguntan decisiones ya confirmadas. La continuidad de ingeniería debe completar el modelo parametrizable y sus bloqueos antes de solicitar o activar datos nominales.
