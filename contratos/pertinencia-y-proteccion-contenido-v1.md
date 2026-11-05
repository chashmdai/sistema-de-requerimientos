# Pertinencia y protección de contenido institucional · contrato 1

Extensión dependiente de U1.6b.1 (`d455b98`), rama `feature/siger-pertinencia-documental`, base `feature/siger-logica-negocio` (PR #2). Reglas rectoras §§05.2/05.5, 08.3, 12 y 15.3/15.5. No cambia transporte público, firmas, originales ni COMMIT→ACK.

## Autoridad del principal del REQ

El modelo histórico no identifica inequívocamente al principal. No se infiere del primer Caso, primer destino, creador o unidad del usuario. El registro firmado de autoridad **1.3** conserva todos los campos 1.2 y agrega `principales`: hasta 32 acreditaciones con requerimiento UUID existente, unidad, sede, área si corresponde, desde/hasta, ACTIVA/SUSPENDIDA, referencia y SHA-256 del respaldo. Mismo canal privado, clave confiable y dominio de firma U1.5; la versión forma parte de los bytes firmados. 1.0/1.1/1.2 conservan su interpretación estricta y no aceptan campos nuevos.

Cada acreditación se conserva por revisión en `authority_requirement_principal`. La última acreditación de ese REQ debe estar vigente y activa; además el actor necesita jefatura efectiva sobre el ámbito exacto, con el modelo U1.5/U1.6a. TI no adquiere lectura o decisión. Omitir una entrada no extiende fechas ni borra la historia. Una nueva acreditación no cambia unidad/sede/área de un principal ya acreditado: falla 409 hasta disponer del circuito de transferencia con aceptación. No mueve Casos, personas o tareas.

Esta capacidad acredita el contexto de recursos históricos; no implementa el nacimiento U2 de SOL/REQ ni habilita una transferencia. No hay endpoint que configure confianza o importe designaciones libremente desde TI.

## Pertinencia y valoración son decisiones distintas

La jefatura principal selecciona explícitamente los Casos del REQ a los que corresponde una COM hija ya propuesta, con fundamento. Guarda materia actual y secuencias de cambio de área/materia de cada contexto. No elige el distribuidor, no se crea Caso, no se admite globalmente, no se responde al ciudadano ni se reabre.

Una selección vacía retira la pertinencia previa. La lista actual es una proyección sustituible; cada revisión conserva snapshot de decisión y ámbitos, actor, respaldo del principal y momento en historia inmutable. Cambiar área/materia invalida el ámbito anterior, incluso si luego se vuelve al valor previo. La nueva consulta necesita otra decisión expresa. No se migra automáticamente un vínculo histórico.

El responsable profesional actual valora ADMITIDA/NO_ADMITIDA por Caso. Cada revisión referencia su predecesora y revisión de pertinencia; nunca sobrescribe `core_link_evaluation`. El consultor por materia no valora. La valoración en Caso cerrado conserva ese estado. Las valoraciones nuevas no se traducen ficticiamente al esquema de reapertura histórico: esa integración permanece cerrada 409; tampoco una valoración histórica permite eludir la nueva pertinencia.

Las rutas antiguas de antecedentes, conteos y avisos excluyen las COM gestionadas con la nueva pertinencia. Los derechos independientes sobre el ingreso original no se deducen de este vínculo ni se eliminan por esta decisión.

## Protección y consulta documental

Una carga de gestión conserva un único original y registra su Caso propietario, inicialmente SIN_CLASIFICAR para consulta transversal. Responsable y jefatura competente del propietario conservan consulta ordinaria. Una referencia nueva no concede lectura al gestor de otro Caso. Se sustituye expresamente la interpretación provisional V10 que otorgaba consulta por el solo vínculo: prevalece §15.3 del rector. Recursos históricos todavía sin política mantienen únicamente su consulta histórica; no admiten referencias nuevas hasta acreditar propietario y protección. Los vínculos/reintentos históricos se conservan sin reescritura ni publicación a Direcciones. Un histórico sin propietario acreditable no se inventa: requiere evidencia de creación inequívoca o queda cerrado para reclasificación.

La jefatura competente del contexto propietario determina:
- ORDINARIO: no publica por sí solo a otras funciones.
- FIR: solo responsable actual y jefatura competente del Caso propietario; no se hereda por referencia, materia, cargo o TI.
- BLOQUEADO: restricción de consulta; conserva metadatos para responsable/jefatura del propietario, sin contenido ni descarga. No es eliminación ni cuarentena técnica.

El responsable actual puede imponer BLOQUEADO sobre su documento acreditado (o histórico con evidencia inequívoca de creación). Esa facultad no permite clasificar ORDINARIO/FIR, levantar bloqueo, publicar a otra función ni consultar decisiones de otros contextos. Su UI ofrece únicamente restricción y el backend verifica la misma separación. La jefatura competente puede revisar fundadamente la condición.

Para original ciudadano, la acreditación de contexto exige principal vigente, jefatura del contexto y lectura legítima del origen. FIR se limita a ficha de gestión interna, no reetiqueta un original ciudadano.

La publicación por materia es otra decisión fundada, nominativa por **documento y Caso**, materia y contexto actuales, con vigencia finita y estado. Metadatos, contenido y descarga son derechos distintos. El otorgamiento requiere jefatura de origen y destino; retirar una publicación del propio recurso no requiere reabrir catálogo suspendido. Ni compartir REQ/Caso/materia ni acreditar pertinencia de COM publica sus adjuntos. La consulta documental de una COM hija gestionada por pertinencia requiere habilitación documental propia o derecho independiente sobre el original.

Para gestores de otro Caso existe una decisión separada de consulta por Caso, con vigencia, contenido y descarga independientes; no se hereda por materia ni concede gestión. Exige documento ORDINARIO, asociación explícita y jefatura efectiva sobre ambos contextos. No requiere inventar materia en un Caso que aún no la tenga. Las habilitaciones entre unidades permanecen cerradas hasta resolver PE-03. Un lector por materia no puede crear referencias; referenciar exige lectura de gestión del origen y descarga, además de gestión del destino. La respuesta del vínculo puede confirmar la asociación sin habilitar contenido en el destino.

Cambio de protección invalida las publicaciones y consentimientos anteriores por revisión. Cambio de área/materia del destino invalida por secuencia; en el propietario incrementa la revisión de protección con historia, invalidando también todas las salidas a otros Casos. Suspensión/vencimiento del catálogo del contexto propietario impide consulta indirecta por terceros. Omitir el Caso en la ruta ciudadana no evita la autorización del documento. Volver a ORDINARIO o al área/materia previa no revive una publicación. Suspender o expirar publicación/concesión retira esa vía. FIR no admite publicación ordinaria; ACC-10 y otros tipos de reserva PE permanecen cerrados, sin aprobaciones ficticias.

## Rutas y transacciones

Todas bajo sesión HTTPS. Mutaciones con CSRF e Idempotency-Key:
- GET `/nucleo/requerimientos/{req}/pertinencia`: principal, propuestas y metadatos ordinarios de contextos; no contenido de Casos hermanos.
- GET/POST `.../pertinencia/{com}`: revisar propuesta / decidir `{revision,casos,contenido,fundamento}`.
- GET `/nucleo/casos/{caso}/antecedentes-pertinentes`: únicamente contexto vigente; contenido solo cuando fue autorizado.
- POST `.../{com}/valoraciones`: `{revision,anterior,decision,fundamento}`, con predecesora vigente.
- GET/POST `/nucleo/casos/{caso}/proteccion-documental/{doc}`: control/historia y `{revision,clasificacion,fundamento}`.
- POST `.../{doc}/consulta/{destino}`: `{revision,desde,hasta,contenido,descarga,estado,fundamento}`, lectores por materia.
- POST `.../{doc}/compartir/{destino}`: mismo cuerpo, únicamente gestores ya autorizados del Caso; no concede facultades sobre el Caso ni materias. Control devuelve `publicaciones` y `comparticiones` con revisiones separadas.

Se reutilizan núcleo, comandos idempotentes, guarda de identidad y bloqueos ordenados de COM; decisión/proyección/historia/evento confirman juntos. Carrera obsoleta devuelve 409, auditoría fallida revierte. La auditoría de consultas es técnica y no una actuación ni evidencia de lectura humana.

Documentos de gestión y originales ciudadanos aplican política por documento a listados, metadatos, historial, GET/HEAD, Range, vista, páginas y representación. Se revalida después de preparar una vista, antes del primer byte y antes de cada bloque de streaming. Las revalidaciones internas no se registran como descargas nuevas; una retirada detiene los bloques posteriores y registra el rechazo técnico. La representación institucional del formulario filtra descriptores y referencias de adjuntos excluidos; los bytes y sobres firmados conservados permanecen intactos. No es una edición del original ni una nueva versión del contrato público.

La ruta de representación AUDIO/VIDEO actual transmite los bytes originales, por lo que exige **DESCARGA**, también inline/HEAD/Range y durante streaming. Un permiso de contenido sin descarga no habilita ese transporte ni muestra un reproductor ficticio. Las páginas derivadas de PDF/imagen sí utilizan CONTENIDO sin entregar el original. Una reproducción multimedia sin permiso de original necesitará una representación derivada segura; no se inventa esa garantía ni se conceden bytes por cambiar Content-Disposition.

La pertinencia y valoración generan avisos internos genéricos del Caso autorizado, revalidados por recurso y materia, sin nombres de archivos ni contenido de COM. No activan correo nuevo ni conceden permiso de gestión. Los avisos del circuito de vínculos antiguo no vuelven a proyectar una COM gestionada por pertinencia.

UI conecta selección de pertinencia, valoración, protección, publicación y consulta de documentos. Las concesiones de contenido no se convierten en gestión; el visor respeta descarga cerrada. Pruebas con dobles de antivirus/renderer no acreditan workers físicos.

## Persistencia y límites

V15.5 H2/MySQL aditiva: principal acreditado, pertinencia actual/ámbitos/historia, valoraciones sucesivas, protección/historia y publicaciones documentales. V15.6 agrega consentimiento documental por Caso e historia en la tabla de decisiones existente. V1–V15.5 se preservan; sin outOfOrder, retroaprobaciones, semillas nominales ni borrado de originales.

No se acredita downgrade a un JAR anterior que ignore estas políticas. Deben mantenerse juntos código, esquema y UI compatibles al planificar una futura integración/despliegue autorizado.

U1 y U2 siguen parciales. No se habilitan excepciones sensibles, nuevos tipos de reserva, colaboradores, transferencia de principal, nacimiento de SOL institucional, reapertura con la evaluación nueva ni autoridad nominal real. Esas dependencias conservan cierre seguro y deben acreditarse en su incremento, no mediante excepciones de UI.

