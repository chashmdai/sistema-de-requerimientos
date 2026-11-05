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

## U1.6a · Áreas acreditadas y autorización por recurso

Este corte incorpora área a las concesiones y recursos ordinarios. **Materia, lectura regional transversal de Direcciones, FIR, reservas y excepciones sensibles permanecen pendientes.** Área no significa materia, cargo, unidad, sede ni una adscripción nominal inferida. Los datos de perfil técnico no conceden ámbito de área.

### Evolución firmada de autoridad, versión 1.1

El sobre y el dominio de firma Ed25519 de U1.5 se conservan; los bytes del payload incluyen su versión. `Registro` 1.0 conserva exactamente sus campos, interpretación y firmas, y no admite mandatos AREA. `Registro11` 1.1 agrega explícitamente tres listas obligatorias (pueden estar vacías):

- `areas`: `{id,unidad,sede,nombre,jefatura,desde,hasta}`. Jefatura `COORDINACION` o `DIRECCION`; sin semillas nominales. Cada ID pertenece permanentemente a la misma unidad/sede; no se puede mover por una nueva revisión.
- `ambitos`: `{mandato,area}`. Cada mandato cuyo alcance sea AREA necesita exactamente una referencia. El mandato conserva su forma anterior y debe coincidir en unidad/sede y vigencia. Un mandato de unidad/sede nunca aprueba AREA.
- `cambiosArea`: `{id,persona,tipo,unidad,sede,origen,destino,desde,hasta,referencia,sha256}`. Acreditación específica de `DIRECCION` o `DELEGACION_EXPRESA` para autorizar el movimiento indicado. `origen:null` significa solo recurso histórico sin área, no comodín. No concede lectura ni roles: el actor necesita además concesiones de jefatura en origen y destino aprobadas por U1.5. Tener ambas jefaturas no sustituye esta facultad.

Límite de 32 entradas por lista nueva, además de los límites anteriores y 32 KiB totales. Lectura estricta de versión/campos/duplicados, intervalos finitos, respaldo y vínculos canónicos. El catálogo se actualiza por entradas: omitir un área no la elimina ni extiende su vigencia; caduca al término firmado. Cambiar jefatura o fechas invalida epoch de sus cuentas; cambiar solo el rótulo no amplía autoridad. Mandatos y facultades de cambio proceden exclusivamente del registro firmado vigente. Retirar una facultad de cambio, vencerla, retirar confianza o cambiar su credencial suspende su ejercicio, también en reintentos. Para revocar una concesión de contenido ya ejecutada se conserva la operación institucional explícita U1.5.

No existe editor TI o endpoint para fabricar este catálogo/facultades. Acreditar identidad, designación y control inicial de credencial continúa siendo una dependencia externa; SIGER verifica declaración, firma y vigencia, no certifica el documento de respaldo.

### Concesiones, persistencia y acceso

Solicitud AREA: JSON U1.5 más `{version:"1.1",area:"ID"}`, `alcance:"AREA"`, unidad/sede exactas. Roles admitidos: DIRECTOR, COORDINADOR, PROFESIONAL y ADMINISTRATIVO; DIRECTOR exige área acreditada con jefatura DIRECCION. Se reutilizan solicitud, aprobación independiente, ejecución TI y revocación; la separación de personas, rechazo de ciclos y vinculación de credencial siguen vigentes. API/contexto, identidad y React muestran el área exacta. Las solicitudes históricas sin área conservan su digest idempotente anterior.

V15.3 agrega catálogo, ámbitos de mandato/instrucción/asignación, área de destino/Caso e historial; no altera tablas o CHECK históricos. V11 no admite scope AREA: la asignación nueva usa una **fila ancla inerte** UNIDAD_SEDE con unidad/sede NULL y un vínculo obligatorio `staff_assignment_area`. Nunca se representa como concesión general sobre su unidad. Los lectores actuales reconstruyen AREA desde el vínculo y exigen además instrucción ejecutada y enlace de aprobación coherentes con beneficiario, rol y vigencia. Sin ese respaldo no hay permiso. Los lectores SQL anteriores no pueden hacer coincidir el ancla con unidad/sede reales no nulas. Esto no autoriza volver a un JAR antiguo: ese binario ignora la segregación de recursos nuevos.

Un recurso con área solo admite la concesión AREA exacta y vigente, para jefatura o profesional responsable según operación. Ni unidad, sede, distribución, TI ni combinar profesional A con jefatura B habilita jefatura A. Política común en detalle, listados, búsqueda, conteos, vínculos, reaperturas, contexto documental, tareas y avisos. El distribuidor conserva su consulta ordinaria del ingreso y destinos de distribución; no recibe contenido, historial ni conteos de Casos protegidos por área por esa facultad.

Los recursos históricos sin área conservan explícitamente el régimen previo y no se atribuyen a áreas por perfil o texto. Una concesión AREA no autoriza esos históricos. Este tratamiento no ratifica normativamente todos los permisos heredados. El Caso hereda el área acreditada del destino. Un destino sin área no permite gestionar sus Casos que después sean protegidos por área.

**Cierre de alta sin área (continuación 04–05/11/2026):** si la unidad/sede tiene áreas registradas por el canal acreditado, todo destino o Caso nuevo exige área explícita vigente, compatible con su evaluador/responsable. Omitirla devuelve 409 `AREA_REQUERIDA` dentro de la transacción, sin destino, Caso, REQ, evento o comando parcial. Conservar un área conocida pero vencida no reabre el camino sin área: se requiere reacreditación; no se borran catálogos para conseguirlo. GET catálogo añade `ambitosConAreas` (pares unidad/sede conocidos, aunque sus áreas no estén vigentes), que permite mantener la interfaz cerrada sin ofrecer una opción vacía como permiso alternativo. En ámbitos sin áreas acreditadas se conserva la opción sin área formal, sin inventar áreas.

Los destinos históricos sin área se conservan; cuando su ámbito ya exige área, no admiten abrir un Caso nuevo por la ruta histórica. Los Casos anteriores y reintentos idénticos legítimos de comandos confirmados permanecen disponibles con su autorización actual; se recuperan antes de la validación de alta, sin cambiar sus digests ni crear registros adicionales. La validación de alta se ejecuta bajo la misma guarda de identidad usada para importar acreditaciones. Las derivaciones nuevas o aceptaciones pendientes hacia un ámbito con áreas conocidas devuelven 409 `DERIVACION_AREA_PENDIENTE`, tanto si intentan crear destino como reutilizar uno histórico: no pueden crear un sucesor sin área por una vía indirecta.

### Cambio de área conectado

- GET `/nucleo/areas` y campo `areas` del catálogo: áreas vigentes acreditadas, sin contenido de expedientes.
- GET `/nucleo/casos/{id}/areas/disponibles`: destinos de cambio acreditados para el actor y recurso; sin facultad válida devuelve lista vacía. Requiere lectura del Caso.
- POST `/nucleo/casos/{id}/area`: `{revision,origen,area,fundamento}`, sesión individual, CSRF e Idempotency-Key. Exige jefatura actual, facultad firmada específica y jefatura de destino; `origen` debe coincidir con la persistencia al ejecutar. Mismo ámbito unidad/sede, responsable actual habilitado en destino y ninguna derivación pendiente. No reasigna al responsable ni mueve tareas; no crea Caso, REQ, COM o folio.
- GET `/nucleo/casos/{id}/areas/historial`: historia real bajo la misma lectura autorizada del Caso.

Se serializa con identidad, revisión del Caso y COM. Área, revisión, historial, evento, auditoría de autoridad (revisión firmada, facultad, respaldo) e idempotencia confirman o revierten juntos. Se revalida la facultad antes de devolver una repetición; revocarla impide obtener éxito aparente de un comando anterior. La primera clasificación de un histórico exige además acceso histórico de origen y autorización expresa con origen nulo; no se ejecuta una migración automática de pertenencias. No se ofrece la acción si el servidor no acredita destinos.

### Límites seguros de esta entrega

Las derivaciones de Casos con área y hacia destinos con área quedan cerradas con 409 `DERIVACION_AREA_PENDIENTE`, hasta adaptar íntegramente sucesor, principal y permisos. No se degrada el área a unidad para reutilizar una transferencia histórica. Las derivaciones previas sin área mantienen su comportamiento histórico, todavía parcial respecto del rector.

Los Casos con área no heredan el autocierre BAJA ni el autocierre de una jefatura central: responsable solicita y otra persona con jefatura competente confirma. Cuentas alias se comparan por persona acreditada en las concesiones ejecutadas, no por ID de cuenta; identidad ambigua cierra la operación. La excepción de titular regional y las demás políticas integrales de cierre/tareas no quedan acreditadas por este endurecimiento.

No se habilitan por este contrato lectura por materia, reservas/FIR, excepciones ACC-10, SOL institucional U2 ni acciones U5 responder/canalizar/escalar. No se modifican transporte Formulario–SIGER 1.0/1.1, recibos, firmas, originales, folios o COMMIT→ACK. El informe U1.6a identifica pruebas H2, MySQL descartable, MockMvc y DOM por separado; no son E2E físicos.

## U1.6b.1 · Materia y consulta ordinaria de Caso regional

Este apartado incorpora un primer alcance de consulta por materia; sustituye únicamente la ausencia de esa capacidad indicada en U1.6a. **No completa la lectura de todo expediente:** originales, documentos, COM hijas y antecedentes necesitan pertinencia y protección por contenido todavía pendientes. La nueva facultad no autoriza esas rutas. Tampoco habilita FIR, reservas o excepciones ACC-10.

### Autoridad 1.2 y concesión independiente

`Registro12` agrega explícitamente a los campos 1.1:

- `materias:[{id,nombre,competencia,desde,hasta,estado}]`, competencia solo UPR/UPD, estado ACTIVA/SUSPENDIDA, vigencia finita. ID/competencia inmutables; cambiar competencia necesita otro ID, sin reinterpretar aprobaciones anteriores.
- `sedesRegionales:[{id,desde,hasta,estado}]`, referenciando una sede existente. No se deduce región desde nombre, domicilio ciudadano o rol. Sin esa acreditación no existe lectura ni clasificación regional nueva.
- `ambitosMateria:[{mandato,materia}]`, referencia exacta para cada mandato MATERIA. Las listas nuevas admiten hasta 32 entradas cada una, dentro de los límites previos del sobre. Sin datos reales el catálogo queda vacío.

Se conservan sobre, firma Ed25519 y dominio U1.5; versión dentro de los bytes firmados. Los lectores 1.0/1.1 mantienen estructura y semántica y rechazan facultades MATERIA. La versión 1.2 no se confunde con versiones del transporte Formulario. Los catálogos se actualizan por entradas firmadas, con revisión e historia de sobres; omitir una entrada no elimina ni extiende su vigencia. Suspensión/fechas de materia o sede invalidan epoch de cuentas afectadas. No hay editor libre TI ni claves privadas en código.

Solicitud nueva: `{beneficiario,rol:"DIRECTOR",alcance:"MATERIA",unidad:"UPR"|"UPD",sede,desde,hasta,fundamento,version:"1.2",materia}`. La unidad debe coincidir con la competencia acreditada de la materia; sede regional y catálogo deben estar vigentes y cubrir el intervalo. Área no forma parte de esta concesión de consulta. Las asignaciones conservan el ancla inerte V11 (unidad/sede NULL), con vínculo de materia e instrucción ejecutada coherente. Se reconstruye un Acceso MATERIA independiente; jamás se trata como jefatura de unidad/sede/área.

Solicitud, aprobación independiente, ejecución TI, revocación, persona canónica, exclusión de alias/ciclos y vínculo de credencial del aprobador reutilizan U1.5. Un mandato exclusivamente de revocación puede retirar una concesión aunque materia/sede estén suspendidas; no permite aprobar accesos nuevos a catálogo inactivo. Concesiones 1.0 y 1.1 mantienen sus digests idempotentes originales.

### Clasificación y acreditación ordinaria

GET/POST `/nucleo/casos/{id}/materia`. POST `{revision,materia,consultaOrdinaria,fundamento}`, HTTPS, sesión individual, CSRF e Idempotency-Key. Exige simultáneamente coordinación regional vigente de la sede acreditada y jefatura competente del Caso, respetando áreas. Es una condición técnica conservadora de este corte; una coordinación de sede sola no hereda la jefatura de sus áreas.

Materia y complejidad son independientes. Asignar/rectificar no mueve Caso, REQ, unidad, sede, área, profesional o tareas. `consultaOrdinaria` queda cerrada por defecto: la jefatura/coordinación acredita explícitamente la gestión ordinaria consultable. No es un CRUD de FIR ni declara ordinarios los originales o documentos. Un Caso sin esa acreditación, sin materia o con catálogo/sede suspendidos/vencidos no admite la nueva consulta. Los derechos independientes anteriores se conservan sin ratificación normativa automática.

Área de Caso que cambia conserva materia e historia, pero suspende consulta ordinaria hasta nueva acreditación en destino. Esto evita trasladar silenciosamente una habilitación del contexto anterior. Todo confirma bajo guarda de identidad, bloqueo de COM y revisión del Caso; clasificación, revisión, historial, evento e idempotencia revierten juntos. Repetición idéntica revalida actor/ámbito y catálogo y no duplica historia. Cambio de materia retira inmediatamente la vía anterior de consulta, salvo derecho independiente vigente.

### Lectura y límites por recurso

GET `/nucleo/casos` incorpora casos de la materia/sede exactamente concedidas, además de los derechos previos independientes. Búsqueda opcional `q` por folio o ID, paginación, estado y total usan el mismo filtro. No permite búsquedas por relato ni amplía la búsqueda de Comunicaciones. GET `/nucleo/casos/{id}` devuelve `materia` y `soloLectura:true` cuando la única vía es MATERIA. La UI presenta ficha, actuaciones, decisiones e historial de responsabilidad ordinarios, sin controles de gestión ni llamadas laterales a documentos/antecedentes/reaperturas.

Acceso a gestión, tarea, asignación, cierre, rectificación, original, documento, antecedente o REQ sigue requiriendo su autorización propia. La consulta de un Caso no entrega COM hermanas ni la totalidad del REQ. La historia de cambios de materia se muestra solo al gestor competente; el consultor recibe su materia actual, sin revelar materias anteriores. Listados/bandeja/consulta ordinaria no usan el ámbito MATERIA como jefatura SQL, ni siquiera en históricos sin área.

Consultas por materia de ficha/listados/contexto registran `matter_access_event` separado de actuaciones y administración, sin texto sensible ni comprensión ficticia. Si falla su auditoría, la ficha no se entrega. La evaluación interna de autorización para avisos no se registra como visualización del expediente.

La rectificación genera aviso interno `MATERIA_ACTUALIZADA` sobre recurso `CASO_CONSULTA`; se revalida el acceso al proyectarlo y consultarlo. Un destinatario que perdió su vía no ve el aviso anterior. No concede tarea ni gestión y no activa correo/Graph para esa categoría. Integración por correo y auditoría general completa siguen pendientes.

V15.4 es aditiva, sin semillas nominales ni edición de V1–V15.3. Ver [informe U1.6b.1](../docs/LOGICA_U1_6B_MATERIAS_2026-11-05.md) para pruebas y límites. ORG-07/12 siguen parciales por pertinencia y protección de documentos/COM; no se declaran U1.6b ni U1 completos.
