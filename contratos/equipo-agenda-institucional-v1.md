# Equipo operativo, agenda y espera · versión 1

Implementa el levantamiento validado por el responsable del proyecto el 06/11/2026, §§09, 10 y 14. Extiende el núcleo U2 y las tablas de agenda V12/V13; no crea otra clase de Solicitud ni modifica transporte, firmas, originales o ACK.

## Equipo y ámbito

El profesional principal conserva su asignación y acuse. La jefatura efectiva o el principal incorporan colaboradores mediante fundamento, revisión e idempotencia. El invitado debe contar previamente con facultad profesional o de jefatura vigente en la misma unidad, sede y área. Una invitación no es una concesión de rol. El colaborador accede únicamente a ese Caso: listados, conteos, gestión ordinaria y documentos autorizados por contenido. No adquiere el COM raíz, los demás Casos del REQ, facultad de reasignación, solicitud formal de término ni reservas FIR.

El retiro conserva incorporación, motivo original, autorías y motivo de retiro. Si existen tareas no terminales del integrante, el retiro exige reasignarlas expresamente a otro integrante habilitado en la misma transacción. Revocar su facultad institucional elimina acceso aunque la pertenencia todavía figure activa; la jefatura regulariza los pendientes. Un cambio de área debe mantener las facultades efectivas de los integrantes o resolver previamente las pertenencias incompatibles. El sucesor por derivación tiene equipo propio; no hereda pertenencias.

## Agenda

Cada tarea tiene creador, asignador, un ejecutor, objetivo, descripción opcional, recordatorio/ventanas/fecha prevista opcionales, obligatoriedad, estado e historial. Los integrantes autorizados crean y asignan dentro de su Caso. La jefatura también administra la agenda, pero ser jefatura no la convierte automáticamente en ejecutora: para ejecutar debe ser principal o integrante explícita.

Estados: PENDIENTE, EN_CURSO, COMPLETADA, CANCELADA. Únicamente el ejecutor confirma manualmente cumplimiento. Intentar contacto añade una observación y revisión sin completar ni crear actuación. Cancelación de obligatoria y cambios de obligatoriedad/asignación requieren fundamento. El creador/asignador autorizado determina obligatoriedad; editar no borra valores previos del historial. La prioridad histórica de agenda se conserva, con valor técnico NORMAL si se omite, sin constituir prioridad institucional ni semáforo del REQ.

La solicitud de cierre y su confirmación usan la revisión actual del Caso. Solo obligatorias no terminales bloquean confirmación; las ordinarias generan advertencia. La agenda modifica esa revisión bajo el mismo cerrojo identidad→COM que el cierre. Un Caso cerrado no admite tareas nuevas ni intentos nuevos; sus tareas ordinarias existentes permiten regularización administrativa explícita y trazada, sin actuación o reapertura automática.

## TAR-09

Cuando cambia efectivamente el principal profesional del Caso, la transacción de asignación transfiere todas sus tareas no terminales al nuevo principal, incluidas las de colaboradores. Conserva ID, creador, fechas, recordatorio, ventana, obligatoriedad, intentos, estado e historial. Las terminales permanecen intactas. La promoción de un colaborador a principal concluye su pertenencia anterior, sin duplicar funciones de equipo. No ocurre traspaso por cambio de principal del REQ, área, derivación o activación/retorno de subrogancia.

El cambio aumenta una generación de destinatario de recordatorios. Los avisos antiguos se conservan como evidencia, pero dejan de ser entregables al ejecutor anterior; el nuevo ejecutor puede recibir el recordatorio vigente una sola vez por generación, sin crear otra tarea. Leer un aviso no acusa la asignación del Caso ni confirma cumplimiento.

## Espera explícita

Un integrante solicita espera indicando antecedente faltante, destinatario y fecha efectiva de solicitud. Jefatura confirma o rechaza. Se conserva el estado de gestión abierto, principal, tareas y relojes; la espera es un estado funcional explícito adicional, visible en el panel del equipo. La recepción/evaluación de antecedentes requiere reanudación expresa. Una espera solicitada o confirmada impide tramitar cierre por atajo. Ni el transcurso del tiempo ni una COM nueva reanudan o cierran automáticamente.

## API y persistencia

`GET/POST /nucleo/casos/{id}/equipo`, `POST .../equipo/retirar`, `GET/POST .../agenda`, operaciones de lectura/edición/estado/historial/intentos en `/nucleo/agenda/tareas/{id}`, y solicitud/decisión en `/nucleo/casos/{id}/espera`. Mutaciones requieren sesión individual, CSRF e Idempotency-Key; se revalida ámbito vigente en backend. La interfaz utiliza estas APIs incluso si la proyección de notificaciones está deshabilitada.

V15.10 añade pertenencias actuales e históricas, espera, intentos, asignador, obligatoriedad, motivo de historial y generación de recordatorio. La migración Java por motor identifica exclusivamente el CHECK histórico creador=ejecutor, cuyo nombre generaba el motor, y lo reemplaza por una condición que permite distintos actores solo en tareas CASO. Conserva restricciones personales para contextos históricos independientes/U5 y todas las claves foráneas. Comparte DDL y checksum entre H2/MySQL; no modifica V1–V15.9, no infiere equipos ni obligatoriedad histórica, no habilita outOfOrder.

Los avisos de equipo y espera reutilizan cola/proyección durable y autorización por recurso. No se envía correo real ni se crean objetos Graph To Do/Planner/calendario. Esta implementación no acredita por sí sola subrogancias, autocierre regional, concesiones excepcionales, catálogos reales ni habilitación operativa.
