# Documentos institucionales y visor — unidad 2

Extensión institucional compatible con la recepción cerrada en `29428e0`. No cambia el contrato público 1.0/1.1, sus huellas, firmas, COM ni ACK. Implementación: `2e54edd`.

## Modelo y autorización

Un original tiene un registro `institution_document`. Los documentos ciudadanos conservan `INGRESO_CIUDADANO`, operación/adjunto público y descriptor firmado. La carga funcionaria asigna exclusivamente en servidor `GESTION_INSTITUCIONAL`, autor de sesión, fecha y huella calculada. No admite etiquetas de origen, autor o SHA declaradas por el navegador.

V10 añade:

| Tabla | Función |
|---|---|
| `management_document_link` | Referencias de un original a varios Casos/actuaciones, con autor y fecha del vínculo |
| `document_command` | Idempotencia ligada a actor, operación, Caso, contenido y documento |
| `document_representation` | Perfil, SHA del original, número de páginas y fecha de preparación |
| `document_page` | Página PNG, tamaño, SHA propio y clave privada |
| `document_event` | Movimientos y solicitudes documentales por actor y contexto |

La clave compuesta añadida a `core_action(id,case_id)` permite exigir por FK que la actuación pertenezca al Caso del vínculo. No cambia las actuaciones existentes. V1–V9 se conservan sin cambios.

`AutorizacionDocumental` es el punto de política por recurso: lectura de ingreso y contexto, lectura de Caso y gestión documental de Caso. `AutorizacionNucleo` delega en las reglas vigentes. La incorporación/referencia usa la misma facultad y estado `EN_GESTION` que registrar actuación. Los lectores de un Caso cerrado conservan consulta; no reciben una facultad de carga nueva.

Los vínculos no conceden acceso global: cada solicitud verifica el Caso pedido y su referencia al documento. Para crear una referencia se exige lectura del contexto de origen y gestión del destino, revalidadas dentro de la transacción con bloqueos ordenados. Un lector autorizado en el segundo Caso puede consultar el original compartido sin convertirse en lector del primero.

La política se inyecta y puede evolucionar con TI, cargos equivalentes, equipos, subrogancias y permisos múltiples. No se fijan aquí facultades, aprobadores ni plazos de esos módulos. La provisión de identidad y el núcleo conservan sus adaptadores provisionales; no se anuncia una matriz institucional definitiva.

## API de gestión

Todas las rutas están bajo HTTPS y sesión funcionaria. Mutaciones requieren CSRF y `Idempotency-Key` UUID.

- GET `/nucleo/casos/{caso}/documentos-gestion`: metadatos y vínculos del contexto autorizado.
- POST misma ruta, multipart con **un** `archivo` y `actuacion` UUID opcional. Origen, autor, hash u otras etiquetas no forman parte de la entrada.
- POST `.../{documento}/vinculos`, JSON `{ "casoOrigen":"UUID", "actuacion":null }`: referencia al Caso de la ruta o a su actuación.
- GET `.../{documento}/historial`: últimos 100 movimientos del documento en ese contexto; no incluye movimientos de otros Casos.
- GET/HEAD `.../{documento}/original`: original como attachment, con integridad, streaming y Range simple.
- GET/HEAD `.../{documento}/representacion`: audio/video validado cuando corresponde; nunca original PDF/Office activo.
- GET `.../{documento}/vista`: metadatos de representación segura.
- GET/HEAD `.../{documento}/paginas/{pagina}`: PNG privado verificado.

Los documentos del ingreso usan sus rutas institucionales existentes, ampliadas con `/{documento}/vista` y `/{documento}/paginas/{pagina}`. El parámetro `caso` conserva el alcance de antecedentes asociados. La recepción y sus endpoints permanecen independientes.

Los nombres y UUID no son capacidades de acceso. No se devuelven rutas físicas, tokens ni enlaces públicos permanentes. Las respuestas usan no-store, nosniff, política de mismo origen y CSP restrictiva. PDF/Office no se incrustan mediante iframe/object/embed.

## Carga y conservación

Límite técnico de gestión: **25 MiB por archivo**, solicitud multipart 26 MiB, almacenamiento multipart en disco privado desde el primer byte. Formatos admitidos: PDF, PNG/JPEG/WebP, TXT, DOCX/XLSX/PPTX/ODT, MP3/M4A/WAV/OGG y MP4/MOV/WebM bajo el perfil del inspector. No se confía en el Content-Type del cliente.

Secuencia: autorización y contexto → staging por streaming y SHA-256 → ClamAV INSTREAM → inspección efectiva del contenido → publicación privada fsync/rename/fsync → revalidación del contexto y transacción de metadatos/vínculo/comando/auditoría. La firma de custodia ciudadana nunca se amplía con documentos de gestión.

El original se escribe una sola vez en `objects/gestion/{id}.bin`. Las referencias solo insertan vínculos. La misma clave y contenido recuperan el documento existente, incluso ante respuesta perdida; otro contenido o contexto con esa clave produce conflicto. No existen operaciones de sustitución o borrado de originales.

Fallo SQL después de publicar: el archivo queda privado, sin acceso por API, y se reutiliza en el reintento exacto. No hay limpieza automática de evidencia huérfana. Rechazos de contenido/antivirus y fallos previos a publicación no crean documento confirmado. El scanner indisponible devuelve error; no se aprueba por defecto.

## Perfil de previsualización

Imágenes PNG/JPEG/WebP y PDF se presentan como PNG derivado. PDF admite **1–100 páginas**, generadas bajo demanda. Perfil `PNG1600_V1`: dimensión máxima 1600 px y hasta 6 MiB por página; las imágenes reencodificadas por el inspector existente pueden tener resolución menor. La UI ofrece navegación, zoom 50–300%, carga, reintento y errores controlados. La descarga del original es opcional.

Los originales siguen intactos. Las derivadas tienen SHA y metadatos independientes, se conservan en el mismo almacén privado y se reutilizan entre contextos. Cada consulta verifica autorización e integridad del original; cada página servida comprueba también su tamaño/hash.

Se reutilizan ClamAV y el inspector Unix existentes. Para páginas PDF se usa el helper Landlock/seccomp revisado y Poppler local ya instalado, en un proceso descartable. No se despliega otro servicio. El proceso recibe exclusivamente una copia temporal del archivo actual; no accede al almacén completo ni a secretos. Entorno limpio, red denegada, NPROC=1, límites CPU/memoria/archivo/descriptores y deadline de 20 s. El autotest exige denegar lectura externa, sockets y fork antes de habilitar el renderer.

PDF activo/cifrado, contenido inválido y documentos fuera del perfil no se muestran como vistas completas. Office se valida estructuralmente y ofrece descarga; no hay ejecutores ni conversores Office. Audio/video conservan controles y fallback cuando el navegador no soporta el codec.

## Auditoría y límites de esta unidad

Se conservan creación, vínculos, rechazos antivirus/formato, fallos de carga y solicitudes documentales; la autorización negativa de las rutas de gestión también se registra sin revelar recursos ajenos. El historial consultable está acotado al contexto autorizado. Una solicitud de página/descarga no demuestra que una persona terminó de leer o descargar.

No se implementa la auditoría transversal institucional, retención definitiva, borrado, versionado editorial, notificaciones o matriz TI pendiente. Capacidad global, backups externos, múltiples nodos y hardening productivo corresponden a la campaña posterior. El perfil actual conserva un escritor del almacén y trabajos de visualización acotados.