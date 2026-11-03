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

## U1.4 · ACC-01: denegación de concesión propia

POST `/nucleo/ti/funcionarios/{id}/roles` rechaza 403 `AUTOASIGNACION_NO_PERMITIDA` si el destinatario es el propio actor de sesión TI. Registra `AUTOASIGNACION_DENEGADA` sin insertar concesión ni cambiar epoch/perfil. Cada petición denegada es un intento auditado distinto; la denegación no genera correo ni actuación. El fallo de auditoría no habilita la concesión.

React conserva historial/revocación de la propia cuenta y omite el formulario de otorgamiento. La guarda del último TI permanece. Bootstrap inicial de laboratorio vacío sigue separado y no es un bypass del endpoint ordinario.

La API actual carece de aprobador/respaldo independiente; no puede aceptar una pretendida aprobación desde JSON. La prevención integral de autoelevación, las concesiones a terceros y la autoridad por área continúan pendientes. No se declara ACC-01 completo ni se otorga aprobación institucional a otro TI por ser una cuenta distinta. No hay nueva migración para U1.4.

## U1.5 · Instrucciones y autoridad acreditada para concesiones ordinarias

Este apartado sustituye la limitación de concesiones a terceros descrita en U1.4. Reutiliza los roles y ámbitos existentes; no implementa área, reserva, lectura por materia ni excepciones ACC-05/10. Una aprobación de este circuito **no concede una excepción de contenido sensible** ni reemplaza el doble respaldo Gestión/jefatura que esas excepciones necesitan.

### Raíz independiente

La autoridad no se obtiene de TI, cargo, correo, rol DIRECTOR/COORDINADOR ni de una concesión recién ejecutada. Se acredita por un registro firmado, importado exclusivamente desde configuración privada. No hay endpoint para crear autoridades o configurar claves de confianza.

Propiedades nuevas: `institucional.autoridad.confianza-archivo` y `institucional.autoridad.sobre-archivo`, dentro de `institucional.configuracion-privada.directorio`, fuera de Git y bajo las ACL/permisos privados existentes. La primera contiene `{claves:[{id,pem}]}` con claves **públicas** Ed25519; la segunda `{keyId,payload,firma}`. No suministrarlas deja el circuito sin nuevas acreditaciones. Un archivo configurado inválido impide el arranque con un mensaje saneado. La aplicación no contiene firmantes privados, personas raíz ni habilitaciones nominales por defecto.

El payload es JSON UTF-8 exacto de `AutoridadesInstitucionales.Registro`, codificado Base64 estándar: versión `1.0`, revisión monótona, emisión, expiración, vínculos cuenta/persona y mandatos. Firma Ed25519 sobre los bytes UTF-8 `SIGER_AUTORIDAD_1` seguidos de un byte cero y los bytes del payload; no se recanonicaliza JSON para verificar. Límite 32 KiB decodificados, 128 vínculos y 32 mandatos; lector privado estricto con rechazo de campos desconocidos y duplicados. El lector HTTP existente no amplía sus límites.

Cada vínculo lleva actor estable, identificador opaco de persona, vigencia finita y `autenticacion`: SHA-256 hexadecimal minúsculo de los bytes UTF-8 del valor completo `staff_account.password_hash` vigente, incluido su prefijo. Este compromiso de credencial se entrega solo por el canal privado de acreditación; ni hash bcrypt ni compromiso se publican en APIs o auditoría. Varias cuentas pueden pertenecer a la misma persona. Un actor que ya tuvo vínculo no puede cambiar de persona entre revisiones. El mandato lleva persona, función declarada, rol/ámbito exactos, unidad/sede, acciones aprobar/revocar, vigencia, referencia y SHA-256 del respaldo. El firmante acreditado responde por esa declaración; SIGER verifica firma/estructura, **no verifica el contenido de un documento externo ni el nombramiento real**. No se importó ningún registro real.

Para ejercer un mandato se exige que la credencial actual coincida con la acreditada. Restablecerla mediante TI, completar después el cambio temporal o cambiarla personalmente suspende el mandato hasta una nueva acreditación firmada independiente. Así TI no puede tomar la cuenta de un aprobador mediante su facultad de reset y reutilizar su acreditación anterior. No se firma automáticamente la nueva credencial ni se supone MFA/SSO. La acreditación inicial debe verificar externamente persona y control de credencial; este mecanismo no reemplaza esa garantía pendiente PE-02. Los participantes sin mandato no necesitan reacreditarse por un cambio de contraseña para consultar sus instrucciones.

Las revisiones se serializan bajo `identity_guard`: solo actual+1 o repetición idéntica de la actual. Se preservan sobres y proyecciones históricas. Cada uso vuelve a verificar la firma con la clave de confianza vigente y obtiene identidad/mandatos del payload firmado, evitando que una proyección SQL alterada amplíe autoridad. Retirar o sustituir la clave impide usar los registros que ya no autentica.

### API y transacciones

Con sesión individual habilitada, HTTPS/CSRF y `Idempotency-Key` UUID:

| Operación | Efecto y autoridad |
| --- | --- |
| POST `/nucleo/concesiones` | TI prepara solicitud inmutable `{beneficiario,rol,alcance,unidad,sede,desde,hasta,fundamento}`. No concede acceso. |
| POST `/{id}/aprobar` o `/rechazar` | Mandato vigente y exacto, persona distinta de solicitante y beneficiario, `{revision,fundamento}`. No ejecuta la concesión. |
| POST `/{id}/ejecutar` | TI distinto de aprobador y beneficiario, `{revision}`. Revalida cuentas, respaldo, identidad, vigencia y exclusividad de distribución; crea la asignación y su vínculo a la instrucción. |
| POST `/{id}/revocar` | Mandato expreso de revocación, independiente de solicitante/beneficiario, `{revision,fundamento}`. Revoca pendiente/aprobada/ejecutada; retira asignación si existe e invalida epoch. |

Los sufijos usan la base `/nucleo/concesiones`. Estados: `PENDIENTE → APROBADA/RECHAZADA`, `APROBADA → EJECUTADA`, revocación autorizada desde pendiente/aprobada/ejecutada. No se edita una solicitud; un error o aprobación obsoleta requiere revocación y nueva instrucción. Vigencias UTC finitas, persistidas en milisegundos; ninguna etapa anterior a ejecución concede permisos, aunque el inicio solicitado sea anterior. Actor/reloj/identidades de aprobación se derivan en servidor, nunca del JSON.

Asignación, epoch, estado/revisión, vínculo, auditoría e idempotencia confirman o revierten juntos. La guarda compartida serializa cambios de identidad, aprobación/ejecución/revocación y exclusividad. El reintento idéntico devuelve el resultado original **solo después de reautorizarlo**; clave reutilizada con otro actor/payload/operación devuelve 409. La revocación o retirada del aprobador impide reutilizar una ejecución anterior para obtener éxito aparente. El hash de solicitud se vuelve a comprobar al aprobar/ejecutar. Denegaciones 403/404/409 se auditan separadas del rollback, sin almacenar payloads denegados.

Solicitar/aprobar exige beneficiario activo; ejecutar y repetir ejecución vuelven a comprobar beneficiario activo, solicitante habilitado y plazo no vencido. Desactivar a cualquiera de ellos no crea una asignación que pueda reaparecer al habilitar la cuenta. Un actor referenciado deshabilitado produce conflicto 409, sin fingir que venció la sesión del TI ejecutor. Rechazar/revocar una instrucción de un beneficiario desactivado sigue permitido para su autoridad competente.

Se rechazan cuentas alias de la misma persona y ciclos de aprobaciones entre personas con vigencias superpuestas, incluso de más de dos participantes. Es una restricción conservadora DT: no hay una excepción automática ni se infiere legitimidad de una aprobación recíproca. Retirar una aprobación/revisión raíz antes de ejecución la vuelve inejecutable. Cambiar el registro raíz no revoca implícitamente roles ya ejecutados: conservan su vigencia aprobada; su retiro explícito o la desactivación de cuenta invalidan permisos/sesiones. Esta separación evita reinterpretar historia o simular una revocación masiva.

GET `/contexto`, base, `/{id}` y `/{id}/historial` ofrecen acreditación y metadatos de acceso. TI ve instrucciones técnicas; participantes y autoridades exactas ven las autorizadas. Un tercero obtiene 404; una identidad no acreditada no lista instrucciones. La UI presenta el estado cerrado, solicitud TI, decisión fundada independiente, ejecución y revocación, con historial real. La lista devuelve las últimas 500 instrucciones y filtra por autorización; el historial conserva todo y entrega los últimos 100 eventos. La paginación de estos metadatos queda como mejora posterior, sin contadores globales expuestos.

POST TI histórico `/funcionarios/{id}/roles` conserva denegación propia 403 y devuelve 409 `APROBACION_INSTITUCIONAL_REQUERIDA` para terceros. No hay otorgamiento directo alternativo. DELETE histórico rechaza 409 si la asignación tiene instrucción: su revocación utiliza el circuito institucional. Las asignaciones anteriores se conservan sin aprobaciones inventadas; su retiro técnico anterior y la protección del último TI permanecen. Bootstrap inicial vacío continúa separado, sin crear autoridades institucionales.

V15.2 H2/MySQL agrega únicamente tablas nuevas, posterior a V15.1, sin `outOfOrder`. Las pruebas sintéticas incluyen evolución desde V14 y V15.1 y circuito real de servicios sobre ambos motores. El informe U1.5 detalla evidencia, límites y próximos cortes. No se altera transporte/custodia, U5 bloqueado ni ambientes preservados.
