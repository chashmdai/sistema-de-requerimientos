# Acceso documental reservado · contrato 1.1

Estado: C1/c767ab7 y C2/2d98e21 acreditados por sus CI Windows/Ubuntu (42273313757 y42283881844), además de H2/MockMvc, MySQL aislado y DOM. Requisitos funcionales confirmados el06/11/2026; la calificación técnica se acredita por separado. Fuente: rector §§05.5–05.6,13.5,15–16, ACC-05/09/10 y SUB-CONT-01. Los apartados de contrato firmado1.6 y consulta del Caso describen el alcance conservado de1.0; la ampliación1.1 tiene un contrato firmado1.7 explícito.

## Dos respaldos institucionales

La jefatura competente del Caso propietario propone una cuenta beneficiaria, documento FIR, alcance y período. La propuesta es su respaldo expreso, no una concesión efectiva. Gestión Institucional, acreditada en el registro firmado, aprueba o rechaza de manera independiente. La cuenta beneficiaria es una persona diferente de jefatura y Gestión; ambas autoridades también son personas distintas. Se comparan identidades canónicas, incluidas cuentas alternativas. No se permiten ciclos de aprobaciones entre concesiones documentales y las instrucciones U1.5 que conoce el circuito.

Esta es una decisión sobre consulta de contenido, como los consentimientos documentales existentes, con un segundo respaldo obligatorio. No crea perfiles ni facultades de jefatura, no delega configuración institucional a TI y no añade un ejecutor técnico a una decisión de contenido que el levantamiento atribuye a Gestión y jefatura. Los perfiles siguen concediéndose por U1.5. Ser TI no basta para solicitar, aprobar o consultar el documento.

## Contrato firmado explícito 1.6

Conserva los campos de1.5 y añade `autorizantesReserva`: id, persona, unidad, sede, área, materia, desde, hasta, referencia, SHA-256 y estado ACTIVA/SUSPENDIDA. Representa la facultad acreditada de Gestión para respaldar este acceso sensible en ese ámbito. No se infiere de cargos, jerarquía o pertenencia a una unidad. Área/materia nulas designan exclusivamente un contexto sin acreditar, nunca un comodín.

Reutiliza validaciones estructurales, sin reutilizar la semántica de una autorización de cierre. Firmas1.0–1.5 no adquieren facultades nuevas; un campo1.6 bajo versión1.5 es rechazado. Identidad, credencial, firma Ed25519, respaldo y fechas son verificados. La instalación requiere V15.14 al incluir esas facultades. No hay endpoint HTTP para importar confianza o designaciones. No se suministran titulares ni valores nominales reales.

## Alcance y transacciones

V15.14 agrega `institution_sensitive_access`: documento, Caso propietario, política/revisión, secuencias de área/materia, cuenta/persona beneficiaria, respaldo de jefatura, solicitud sellada, respaldo firmado de Gestión y estado/revisión. FK, CHECK de separación/fechas/descarga y unicidad del permiso no terminal por documento/Caso/cuenta. Cada modificación serializa identidad y COM del Caso; autorización, decisión, auditoría y evento se confirman juntos.

Estados PENDIENTE→ACTIVA o RECHAZADA; retirada antes de decidir; revocación por jefatura o Gestión competente; expiración por fecha efectiva. No hay activación sin aprobación. Se conservan las decisiones anteriores. Un reintento confirmado no reactiva una concesión revocada. Una autorización actualizada se solicita como nueva constancia; no se altera el período o beneficiario ya respaldado.

La huella impide mutar datos de la propuesta. Antes de aprobar y de cada acceso se verifican la política FIR original, ámbito/secuencias actuales, autoridades, identidad/credencial, respaldo firmado vigente, fechas y consulta vigente del Caso para la cuenta destinataria. Cambiar protección, área o materia no rehabilita una aprobación antigua al volver al valor previo. La concesión no cambia clasificación ni bytes del documento.

## Consulta efectiva

Metadatos, contenido y descarga se distinguen; descargar exige contenido explícito. No se concede administrar, restringir, reclasificar, referenciar otros expedientes o gestionar el Caso. El control reutiliza `ContenidoInstitucional.permisos`: listados, metadatos, visor/páginas, originales/Range, historial permitido y revalidación durante streaming pasan por la misma decisión. Las reservas BLOQUEADO o SIN_CLASIFICAR no se abren por este circuito.

La consulta previa del Caso y la relación documental son precondiciones. Este incremento no es una concesión de lectura de expedientes a TI, ni un permiso global por compartir REQ/materia. Una cuenta alternativa de la persona beneficiaria no hereda el permiso. El propietario y su jefatura conservan la consulta ordinaria de FIR que corresponde.

La bandeja muestra solamente propuestas de la jefatura actual, ámbito de Gestión acreditado o cuenta beneficiaria con consulta actual del Caso. Gestión recibe metadatos mínimos de decisión y fundamento; su facultad de aprobar no concede contenido. La cuenta beneficiaria no recibe fundamento ni identificador documental antes de pasar por sus controles de consulta. Retirar pertenencia o respaldo afecta inmediatamente documentos, listas, conteos de esa lista y avisos, sin esperar un nuevo login.

## API e interfaz

- GET/POST `/nucleo/casos/{cas}/proteccion-documental/{doc}/acceso-reservado`: contexto/elegibles y propuesta de jefatura.
- GET `/nucleo/accesos-reservados`: bandeja autorizada, sin totales globales.
- POST `/nucleo/accesos-reservados/{id}/{aprobar|rechazar|retirar|revocar}`: fundamento, revisión actual y `Idempotency-Key`.

Interfaz desde la protección de un documento FIR y bandeja Accesos reservados, con decisiones reales del backend. Ausencia de configuración mantiene el acceso cerrado. CSRF/sesiones, idempotencia y revisiones se conservan.

Eventos mínimos por propuesta/decisión/expiración en SIGER y outbox de correo, deduplicados y reautorizados al consultar/enviar. No se incluyen nombre del archivo, relato, fundamento libre o contenido en el correo. Consultar no emite correo ni acredita revisión humana. Las pruebas mantienen transporte simulado y cero envíos reales.

## Límites explícitos

Se habilita la excepción documental FIR necesaria para que SUB-CONT-01 pueda revisar un Caso con esos antecedentes, previa concesión independiente. No se crea una nueva clase de reserva, una ficha sectorial completa, una regla de retención/purga o una facultad de consulta transversal de auditoría/TI. Tipos/catálogos adicionales y documentación estructurada siguen como implementación/configuración técnica; no falta una nueva validación del levantamiento. Originales, firmas, contratos de transporte y COMMIT→ACK permanecen intactos.

## Ampliación explícita: solo documento, firma1.7

Registro1.7 añade `autorizantesDocumentoAislado`, independiente de `autorizantesReserva`. Mantiene los campos y facultades anteriores. Cada facultad especifica persona, unidad, sede, área y materia exactas, vigencia, referencia, hash y estado. La materia es una competencia funcional y no equivale a la unidad propietaria del Caso:1.7 permite acreditar expresamente esa combinación, sin cambiar la interpretación de1.0–1.6. Nulos no son comodines. Una firma1.6 que incluya el campo nuevo se rechaza. La instalación con facultades nuevas exige V15.15.

El modo `CASO_DOCUMENTO` conserva la precondición de consulta del Caso, los comandos, el DTO y la huella anteriores. El nuevo `SOLO_DOCUMENTO` exige la facultad1.7, propuesta de jefatura y aprobación independiente de Gestión, con las mismas restricciones de personas canónicas, reciprocidad, ámbito, credencial, período, revocación e historial. Su sello añade un discriminador1.7; cambiar el modo en persistencia invalida la instrucción. No se reinterpreta una aprobación anterior ni se amplía su ámbito silenciosamente.

`SOLO_DOCUMENTO` admite documentos clasificados como ORDINARIO o FIR por la jefatura competente; SIN_CLASIFICAR y BLOQUEADO siguen cerrados. FIR sigue limitada a documentación interna: un original ciudadano conserva su origen/clasificación y no se convierte en FIR para usar esta excepción. Compartir Caso/REQ/materia no abre el documento. El nuevo permiso tampoco habilita las rutas documentales ordinarias del Caso.

La cuenta beneficiaria exacta puede consultar ese documento sin recibir el Caso, COM, equipo, bitácora o documentos hermanos. TI no tiene ninguna facultad por defecto; también necesita esta decisión nominal vigente. Su cuenta alternativa no hereda el permiso. Gestión recibe la instrucción mínima, nunca el contenido por el solo hecho de aprobarla. El destinatario solo se resuelve por usuario exacto desde una jefatura autorizada, con auditoría; no se publica un directorio institucional.

## Rutas e interfaz de documento aislado

- GET/POST `/nucleo/casos/{cas}/proteccion-documental/{doc}/acceso-reservado/solo-documento`: contexto y propuesta explícita1.7.
- GET `/nucleo/casos/{cas}/proteccion-documental/{doc}/destinatario-aislado?usuario=...`: resolución exacta para la jefatura.
- GET `/nucleo/accesos-reservados/{id}/documento`: DTO mínimo de nombre, tipo, tamaño, hash, representación y permisos; no incluye Caso/COM/ruta privada.
- GET `/vista`, GET/HEAD `/paginas/{n}`, `/original` y `/representacion` bajo esa misma base: transporte documental existente, sin copiar originales. Original/representación de medios requieren descarga; páginas requieren consulta.

Antes de entregar metadatos/cabeceras y en cada bloque de64KiB se comprueban sesión/identidad, instrucción, fecha, respaldos, política, secuencias y relación documental actual. Si se revoca durante una transferencia, no se entrega el siguiente bloque. Range/HEAD, integridad, custodia privada, CSP y no-store mantienen sus controles. Se distingue permiso para metadatos, contenido y descarga. Denegaciones documentales se presentan uniformemente como404, sin revelar el expediente asociado.

La bandeja omite el identificador del Caso al beneficiario aislado sin facultad sobre él. Solo ofrece el enlace cuando el backend acredita el permiso efectivo. La página de consulta usa exclusivamente la ruta concedida; errores/cambios de autorización descartan la vista anterior. La protección ORDINARIO/FIR permite elegir expresamente el ámbito; ORDINARIO no ofrece el modo1.6 reservado a FIR. No hay controles ficticios ni acceso inferido desde el rol del navegador.

V15.15 es aditiva y añade `scope_mode` con default CASO_DOCUMENTO y CHECK de los dos modos. Las constancias anteriores conservan campos, huellas, firmas, comandos y alcance; se prueba una concesión real creada en V15.14 antes de actualizar. No hay backfill institucional ni migración sobre datos preservados.
