# Revisión excepcional de cierre por Caso · v1

SUB-CONT-01 del levantamiento validado el 06/11/2026 (§§10/13). Es una habilitación nominativa para `CONFIRM_CASE_CLOSURE` sobre un Caso concreto. La aprobación se ejecuta transaccionalmente como decisión de negocio por Caso; las cuentas y roles continúan gestionadas por el circuito independiente U1.5.

## Autoridad y contrato firmado

El registro firmado evoluciona explícitamente a **1.5**: conserva 1.4 y añade `autorizantesCierre`, con ID de facultad, persona canónica, unidad, sede, área, materia, período, referencia/SHA-256 y estado ACTIVA/SUSPENDIDA. No fija una lista cerrada de cargos. La autoridad efectiva procede del respaldo firmado independiente, con identidad/credencial vigente. Un cargo escrito o un rol TI no acreditan esa competencia.

El ámbito es exacto. Área nula corresponde a recursos sin área, y materia nula a recursos sin materia; no son comodines. Se verifica el catálogo vigente. Las versiones 1.0–1.4 conservan sus DTO/significados; una raíz 1.4 con campos 1.5 se rechaza aunque esté firmada por un firmante confiable.

## Circuito

1. El profesional responsable de un Caso institucional propio, con ejercicio de subrogancia acreditado, registra primero la solicitud profesional de término. La revisión excepcional se solicita durante REVISION_TERMINO, con razón de imposibilidad de revisión titular, persona/cuenta revisora, inicio y término finitos y antecedentes necesarios.
2. El revisor ya debe tener lectura autorizada del Caso. Se conservan las políticas documentales por contenido y recurso. La habilitación no agrega membresía, jefatura, gestión de tareas, rectificación o permisos de lectura sobre otros Casos.
3. Una autoridad acreditada en ese ámbito aprueba o rechaza. Solicitante, revisor y autorizante son personas canónicas diferentes. Se comprueban de nuevo solicitud de cierre, responsable, unidad/sede/área/materia, período y acceso a documentos. La solicitud tiene huella de sus campos inmutables.
4. El permiso HABILITADA solo lo utiliza la cuenta revisora nominada, con identidad acreditada y dentro del período. Revocar el permiso, suspender/retirar la facultad de autoridad, cambiar credenciales acreditadas o retirar la lectura del Caso deniega su uso con las sesiones existentes. Un alias con lectura propia no hereda la confirmación.
5. La persona abre el paquete actual de revisión, consulta gestión, decisiones y documentos mediante los componentes existentes. La confirmación manual envía revisión de permiso, revisión del Caso y huella de antecedentes. Un cambio documental exige actualizar/revisar; la falta de acceso a una reserva impide confirmar.
6. El cierre verifica transaccionalmente obligatorias, espera, derivación/término de colaboración y demás controles ordinarios. Marca el permiso UTILIZADA, conserva la solicitud y registra autoría real/modalidad EXCEPCIONAL. Un cierre de origen por derivación conserva esa modalidad y la evidencia adicional de autorización. REQ y Casos hermanos continúan hasta sus decisiones propias.

El solicitante retira un pendiente; la autoridad vigente revoca con fundamento. La expiración es inmediata por fecha aunque todavía no se haya materializado el evento. La bandeja materializa EXPIRADA y distingue término efectivo/registro. Los comandos usan idempotencia del núcleo y el orden identity_guard → COM. La auditoría y el cierre se revierten juntos ante un fallo.

## Antecedentes y reservas

Se incluyen documentos propios del Caso y referencias de gestión, más referencias adicionales explícitas autorizadas. Se exige acreditación documental; HISTORICO_SIN_ACREDITAR y SIN_CLASIFICAR no se aceptan como revisión excepcional completa. La huella combina IDs, hashes de originales y revisión/clasificación documental; nunca modifica originales.

FIR/BLOQUEADO conservan su política. El permiso de cierre no crea una autorización de reserva. Cuando los antecedentes necesarios requieren acceso sensible todavía no concedido, la operación permanece cerrada por ese control concreto. El circuito específico de excepciones sensibles ACC-05/10 —respaldo de Gestión Institucional y jefatura responsable, alcance/vigencia— se implementa separadamente, sin inferirlo de esta facultad.

La búsqueda/bandeja/avisos del revisor exigen lectura actual del Caso además de relación con la instrucción. La autoridad recibe el paquete mínimo de decisión dentro de su facultad; no recibe por ello el expediente. Una revocación de equipo/lectura retira también listados, paquetes y avisos.

## API e interfaz

`GET /nucleo/casos/{id}/revision-excepcional` proporciona contexto y candidatos con lectura vigente; POST solicita con revisión/fundamento. `GET /nucleo/revisiones-excepcionales` es la bandeja autorizada; GET `/{id}` entrega el paquete al revisor nominal. POST `/{id}/aprobar`, `rechazar`, `retirar`, `revocar` o `confirmar` exige sesión individual, CSRF, UUID y revisiones. La interfaz se conecta a esos endpoints y conserva el fundamento ante errores.

Un empate temporal ambiguo entre solicitudes históricas no se resuelve por orden de UUID. Se exige una solicitud actual explícita mediante el circuito de revisión, conservando las anteriores.

Los eventos de solicitud/decisión/uso/expiración se integran en la infraestructura durable de avisos. Aplicar el [contrato de doble canal](notificaciones-funcionales-doble-canal-v1.md): destinatarios actuales, mensaje mínimo, deduplicación y transporte real condicionado a configuración/autorización operacional.

## Persistencia

V15.13 H2/MySQL aditiva: `institution_closure_exception`, FK a Caso, solicitud, cuentas/catálogos y lote de autoridad, restricciones de personas/estado/intervalos e índices. Se reutilizan core_command, historias y outbox. Sin modificar V1–V15.12, backfill, outOfOrder, reescritura histórica o migraciones operacionales. Revocar/expirar es reversión funcional conservando evidencia; no se elimina DDL ni historia.
