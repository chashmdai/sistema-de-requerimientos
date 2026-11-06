# Acceso documental reservado · contrato 1.0

Estado: desarrollo C en calificación. Requisitos funcionales confirmados el06/11/2026; la calificación técnica se acredita por separado. Fuente: rector §§05.5–05.6,13.5,15–16, ACC-05/09/10 y SUB-CONT-01.

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
