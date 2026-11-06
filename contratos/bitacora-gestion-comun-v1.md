# Bitácora común de gestión · contrato1.0

Fuente: rector §§08.5,09,14–15,17–18; BIT-01/02/03/06, TAR-05, PLA-01 y ACC. Validación funcional confirmada el06/11/2026. Se acredita técnicamente cada corte en su informe.

## Registro y contexto

La actuación tiene un único contexto: Caso o REQ atendido sin Caso. El texto permanece en su registro original (`core_action` o `institution_flow_history`). Dos tablas de metadatos V15.16 incorporan versión1.0, fecha efectiva y rectificación; no duplican el relato ni fabrican gestiones a partir de tareas, lecturas, correos o decisiones de cierre.

Una nueva constancia requiere versión1.0, revisión vigente del contexto, fecha efectiva UTC de una gestión ya realizada y descripción. La fecha técnica la asigna el servidor al persistir. Se conserva precisión de milisegundos. La cuenta autenticada es registrador y ejecutor de esta ruta; no acepta un ejecutor libre, titular ausente o campos sectoriales arbitrarios. Un eventual registro por otra persona requiere una facultad y un contrato posteriores expresos; no se simula como disponible.

El REQ conserva atención sin Caso mientras no tenga ninguno; después las nuevas gestiones corresponden al Caso autorizado. Se sigue consultando la atención previa del REQ sin copiarla a sus Casos. No se crea otro REQ/CAS para registrar una gestión. Cierre, principal, equipo, asignación y tareas son circuitos independientes.

## Inmutabilidad y rectificación

No hay PUT o DELETE de actuación formalizada. Una rectificación es otra constancia con fecha, autor real, descripción corregida, motivo y referencia al original del mismo contexto. El original permanece íntegro. Una constancia puede tener un sucesor de rectificación: una corrección posterior se vincula a ese sucesor, evitando ramas ambiguas. Se conserva la cadena completa, consultable mediante enlaces autorizados incluso entre páginas.

FK compuestas impiden asociar una rectificación o metadatos a otro Caso/REQ; las referencias de REQ deben ser actuaciones de atención, no decisiones administrativas. Revisiones e identidad→COM serializan registro, cierre, apertura y rectificación. Actuación, metadatos, revisión, comando idempotente y auditoría se confirman o revierten juntos. Repetir el mismo comando confirmado no crea otra constancia ni reactiva la gestión. Modificar su payload con la misma clave se rechaza.

Se registra en EN_GESTION del Caso o ACTIVO del REQ sin Caso, con su facultad vigente. El cierre conserva consulta e historia y no permite nuevas actuaciones por esta ruta. La reapertura autorizada conserva el mismo Caso y admite gestión en su nuevo período; una COM hija no abre un período por sí sola.

## Autorización y consulta

El Caso usa su política efectiva de equipo, área y materia: gestor puede registrar; consulta ordinaria por materia solo lee. El REQ usa responsable de atención o jefatura principal vigente, sin obtener gestión de Casos de otras unidades. TI, otra área/unidad, un alias sin asignación propia o un permiso SOLO_DOCUMENTO no abren la bitácora. Retirar equipo/ámbito afecta las consultas posteriores con la misma sesión.

Permisos se comprueban antes de listar, contar, paginar o consultar una constancia individual. La ruta individual exige simultáneamente contexto y constancia: conocer su identificador no abre otra bitácora. Consultas y registros generan auditoría, sin generar acuses o correos de lectura. Los documentos enlazados mantienen sus permisos independientes; este incremento no copia sus nombres o contenidos a la respuesta de bitácora.

## API e interfaz

- GET/POST `/nucleo/casos/{id}/bitacora`.
- GET/POST `/nucleo/institucional/requerimientos/{id}/bitacora`.
- GET `{base}/constancias/{accion}` para original o rectificación del mismo contexto.
- GET admite `pagina` desde0 y `tamanio`1–100 (por defecto25). Solo devuelve total autorizado del contexto.
- POST requiere sesión individual, CSRF e Idempotency-Key; cuerpo `version`, `revision`, `realizadaEn`, `descripcion`, `rectifica` y `motivo`. Motivo es obligatorio únicamente al rectificar. Campos desconocidos y fechas futuras/no representables se rechazan.

Una sola vista React reemplaza la lista provisional de Caso y la atención provisional del REQ. Muestra fechas efectiva/persistida, registrador, ejecutor acreditado y cadena de correcciones. La consulta por materia utiliza la misma vista sin controles de escritura. Se captura fecha en la zona declarada del navegador, persiste UTC y presenta cronología en America/Santiago. Rectificar solo texto conserva segundos/milisegundos del instante conocido; no redondea la fecha histórica.

Una respuesta incierta conserva payload y clave para reintentar, incluso si se actualiza la revisión visible. No permite modificar ese intento mientras su resultado sea incierto. Un conflicto definitivo conserva el borrador y exige actualizar la revisión. Errores o pérdida de acceso retiran la vista anterior; enlaces tardíos no sustituyen una ficha que cambió.

## Compatibilidad y límites

V15.16 es nueva, aditiva e idéntica H2/MySQL. Conserva V1–V15.15, originales, firmas, PULL/ACK y registros históricos sin backfill. Actuaciones previas aparecen con fecha efectiva y ejecutor no acreditados si esos datos no existían: no se deducen de fecha de registro o autor. Las referencias documentales a `core_action` conservan sus IDs.

La antigua API de actuaciones del Caso institucional U2 y del REQ admite únicamente reintentos de comandos ya confirmados, verificando actor/acción/payload originales. Un comando nuevo recibe409 BITACORA_FECHA_EFECTIVA_REQUERIDA y utiliza la ruta nueva. El Caso histórico todavía no conciliado conserva su compatibilidad anterior; esta ruta no lo reclasifica ni le inventa un principal. Los servicios históricos de prueba/compatibilidad mantienen sus hashes y semántica.

La primera atención resuelta anterior conserva su respuesta e historia: su fecha efectiva de gestión todavía necesita la evolución específica de ese ingreso, no se inventa a partir de la recepción. Tipos/campos sectoriales y registro autorizado por otra persona quedan como extensión técnica versionada. No se fija un calendario de48 horas por suposición: se conservan los hechos temporales para PLA-01, que exige registro y no resolución. Tareas y actuaciones permanecen independientes.
