# Adaptación de lógica institucional · contrato incremental v1

Fuente: NX-SIGER-DOM-2026-01, SHA-256 registrado en el diagnóstico U0. Este contrato documenta únicamente incrementos acreditados en `feature/siger-logica-negocio`. No declara U1–U9 completas ni habilita producción. Para el resto de operaciones sigue siendo visible la implementación histórica identificada en U0; no representa todavía el modelo objetivo.

## U1.1 · REG-DIS-01: distribución regional local

POST `/nucleo/comunicaciones/{id}/distribuir`, con sesión individual HTTPS, CSRF, `Idempotency-Key` y revisión existente:

- La coordinación regional necesita concesión vigente sobre la sede de origen y todos los destinos deben pertenecer a **esa misma sede**. Poseer coordinación en A y B no permite distribuir la bandeja A hacia B.
- Una facultad central de DISTRIBUCION independiente y vigente mantiene distribución institucional. El domicilio/región declarados no conceden esta facultad.
- Web sin sede sigue en central. Una redirección intersede no se simula mediante distribución y continúa pendiente de su incremento U2.
- Se valida antes del comando y dentro de la transacción, bajo el guard de identidad y el bloqueo de COM. Un destino ajeno devuelve 403 sin destino, evento de distribución, comando idempotente ni incremento de revisión parciales. Revocar la facultad central impide repetir una distribución intersede usando solo coordinación local.
- React filtra los destinos del panel por sede de origen para coordinación. El servidor sigue siendo la autoridad.

No cambia forma del DTO, transporte 1.0/1.1/V3, folios, originales, firmas o ACK. La cardinalidad 1–10 del laboratorio aún existe; su sustitución por SOL/destino único pertenece a U2. No se declara DIS-06 cubierta por este cambio.

Pruebas: `IdentidadInstitucionalTest`, `IdentidadHttpTest`, `Distribucion.test.tsx`. AT-013 cubierto en su restricción de distribución (origen/destino); el circuito de redirección completo aún no está implementado.

## U1.2 · ORG-02/11: una designación central efectiva

Se reutilizan las asignaciones V11 y su guard de identidad; no se crean tablas ni designaciones nominales nuevas. La administración de laboratorio existente conserva su frontera TI, cuya acreditación completa de autoridad es una brecha U1 separada.

- Otorgar DISTRIBUCION a una cuenta activa rechaza solapamientos operativos presentes o futuros con cualquier otra designación central no revocada en cuentas activas, incluso de la misma persona: HTTP 409 `DISTRIBUIDOR_CENTRAL_SOLAPADO`. Intervalos UTC `[desde,hasta)`; el fin de uno puede coincidir exactamente con el inicio del relevo.
- Las asignaciones de cuentas desactivadas son inefectivas. Habilitar o actualizar como activa una cuenta comprueba **todos** sus intervalos de distribución contra los efectivos de cuentas activas. El conflicto revierte perfil, epoch y eventos de esa operación. No se revocan asignaciones históricas por desactivar la cuenta.
- Se compara solo la porción presente/futura del intervalo; no se reescribe ni bloquea por solapamientos que ya terminaron. Un permiso futuro no concede acceso antes de su inicio; uno vencido deja de concederlo sin requerir reinicio.
- El guard `identity_guard` serializa concesión/activación/revocación y comandos del núcleo. Dos concesiones competidoras producen una sola asignación y un conflicto; los eventos positivos se confirman junto con el cambio. La auditoría general de denegaciones sigue siendo brecha U7.
- Si los datos históricos ya contienen más de una designación central efectiva, la resolución de acceso elimina **solo esa facultad** de todos los candidatos. No escoge el primer registro, no elimina filas ni suspende sus otras funciones. TI conserva la consulta de asignaciones y debe registrar una revocación autorizada para resolver la ambigüedad.
- El relevo se registra mediante vigencias no solapadas o revocación y nueva concesión. La revocación invalida sesiones/actores anteriores por epoch y revalidación. No se trasladan ni recrean COM, pendientes o tomas. Puede existir un intervalo sin distribuidor; no se atribuye automáticamente a un suplente.

React explica la exclusividad en el formulario de concesión y conserva los datos ante 409. El error no revela identidad/correo del titular. No se cambian rutas, DTO de roles ni contratos del Formulario.

Cobertura: `DistribuidorUnicoTest` (H2, intervalos/relevo/carrera/activación/datos históricos/integridad), `IdentidadHttpTest` (MockMvc/HTTPS/CSRF/conflicto sanitizado), `Identidad.test.tsx` (DOM/formulario). AT-017 parcialmente acreditado: exclusividad y revocación de la facultad; todavía falta el proceso de autoridad/respaldos nominales del relevo y la reserva/toma de COM del modelo nuevo.

## U1.3 · ACC-04: credencial temporal

V15.1 agrega `staff_credential_state` sin modificar `staff_account`. Las cuentas anteriores conservan su estado; las altas/bootstrap nuevos y resets TI requieren cambio personal antes de acceder a gestión. La ausencia de registro es compatibilidad histórica, no un flag modificable desde JSON. La administración no acepta campos para desactivar la obligación.

GET `/nucleo/session` agrega `cambioClaveObligatorio`; cuando es true devuelve `operador:null` y capacidades de gestión desactivadas. La sesión temporal solo permite GET `/nucleo/session`, GET `/nucleo/csrf`, POST `/nucleo/clave-personal` y POST `/nucleo/logout`. Las demás rutas `/nucleo/**` devuelven 403 `CAMBIO_CLAVE_REQUERIDO` antes de exponer contenido. HTTPS y CSRF permanecen obligatorios.

POST `/nucleo/clave-personal`: JSON `{actual,nueva}`, actor y revisión de seguridad derivados de la sesión, sin cuenta objetivo. Se verifica clave actual, nueva distinta, 16 caracteres mínimos/72 bytes UTF-8 máximos y controles existentes. Responde 204 tras transacción de hash/estado/epoch/evento e invalida sesión; el usuario vuelve a autenticarse. 400 para validación/reutilización, 403 `CREDENCIAL_ACTUAL_INVALIDA`, 401 para sesión obsoleta y 429 al superar ocho intentos/cuenta/minuto en el proceso. No se guardan secretos o digest de payload en comandos idempotentes; el reenvío de una sesión consumida no puede volver a modificar la cuenta.

La cuenta TI conserva su endpoint de reset, que marca temporal e invalida las sesiones por epoch. Cuenta administrativa y sesión funcionaria no son credenciales de proveedor. Se mantiene pendiente fuente maestra, SSO/MFA y recuperación institucional PE-02. La evolución/compatibilidad y los resultados están en [continuidad U1](../docs/LOGICA_CONTINUIDAD_U1_2026-11-02.md).
