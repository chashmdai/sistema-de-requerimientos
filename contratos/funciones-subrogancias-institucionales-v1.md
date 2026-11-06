# Funciones y subrogancias institucionales · v1

Implementación del levantamiento validado el 06/11/2026 (§§10/13). Este contrato de aplicación no cambia el transporte Formulario–SIGER, COM, firmas originales ni COMMIT→ACK. No habilita despliegues ni designaciones reales.

## Acreditación independiente

El registro de autoridad firmado evoluciona explícitamente a **1.4**: conserva los campos 1.3 y añade `funciones`. Las versiones 1.0–1.3 conservan su forma, bytes y significado. La firma Ed25519 incluye la versión y todo el payload, con la confianza privada previamente configurada. No hay endpoint para importar raíces ni configurar firmantes.

Cada designación contiene `id`, `codigoFuncion` estable, `ruta` REGIONAL/CENTRAL, `autoriza` JEFATURA_SEDES_REGIONALES/JEFATURA_GESTION respectivamente, sede, titular canónico, candidatos preidentificados, autoridades/delegados canónicos, ámbitos explícitos, inicio/fin, referencia, SHA-256 del respaldo y estado ACTIVA/SUSPENDIDA. El catálogo regional y las áreas forman parte del mismo registro acreditado; no se infieren del nombre de la sede o del cargo del usuario. Identidades sintéticas solo en pruebas.

Los ámbitos reutilizan rol/alcance/unidad/sede/área. No admiten TI, DISTRIBUCION, privilegio institucional global ni MATERIA. Una autorización de consulta por materia sigue siendo de lectura. Los ámbitos sin área no heredan las áreas. Se rechazan funciones simultáneas en el mismo contexto aunque utilicen nombres de rol distintos. No se reasigna el código estable de un ámbito registrado.

La designación no concede al titular un acceso base: necesita concesión U1.5 vigente en el ámbito exacto, identidad/credencial acreditadas y organización vigente. Los ámbitos registrados persisten aunque una raíz posterior omita funciones; la omisión o suspensión deniega la función. Las facultades profesionales y de consulta independientes se conservan.

## Circuito y estados

1. El candidato preidentificado solicita para su cuenta, con motivo, inicio y término conocido o revisión finita. Cuando existen ambos, rige el primer límite. No hay vigencia indefinida por omisión.
2. La autoridad canónica acreditada para la ruta y función aprueba/rechaza. Es otra persona respecto del titular y del candidato. Las cuentas alternativas no representan independencia. Se impide la aprobación recíproca en períodos que se solapan, incluyendo la trazabilidad de aprobaciones revocadas.
3. Una tercera persona con TI vigente ejecuta la instrucción aprobada. No elige beneficiario, ámbito, período o motivo distintos. Se comprueban identidad y credencial del aprobador y del beneficiario nuevamente. La administración técnica recibe alcance y respaldo mínimo, sin el relato de ausencia ni el payload completo de autoridad.
4. EJECUTADA solo produce facultades dentro de `[max(inicio,ejecución), fin)`, para la cuenta solicitante concreta. Un alias no recibe la función. La coordinación titular del ámbito queda excluida durante ese ejercicio; su perfil profesional no se retira.
5. Prorrogar crea una solicitud nueva enlazada con período nuevo, aprobación y ejecución nuevas. No modifica el término anterior. La autoridad/delegado revoca con motivo; el titular registra su retorno; el candidato puede retirar una solicitud pendiente.
6. Al término se retiran facultades por predicado temporal incluso si ningún monitor ha ejecutado la materialización de EXPIRADA/REVISION_REQUERIDA. La consulta de la bandeja materializa activación/fin e historial, distinguiendo fecha efectiva y registrada. No exige un servicio permanente nuevo.

Los estados terminales conservan decisiones y autores. Los comandos llevan UUID de idempotencia y revisión; actor, acción y payload deben coincidir para un reintento. La exclusión `identity_guard`, los comandos, la auditoría y los avisos se confirman en una transacción. La solicitud aprobada tiene huella de sus valores inmutables; modificar su período/fundamento/identidades invalida la ejecución. Las bajas, el retorno, la ejecución y las modificaciones del registro invalidan las épocas de las cuentas participantes afectadas.

## Continuidad y límites

No se modifica responsable, equipo, tareas, folio, historial o REQ al activar/terminar una subrogancia; TAR-09 solo corresponde a una reasignación efectiva del profesional. El subrogante recibe las bandejas de función existentes, bajo los mismos predicados de área/unidad y protección documental. No recibe por esta vía privilegios extraordinarios FIR, reservas o consulta interunidad.

La comprobación de un acto de origen ya emitido admite el titular temporalmente sustituido con concesión base vigente, o una subrogancia acreditada que cubría la fecha del acto. Este auxiliar solo valida la petición original al decidir transferencia/derivación/término de colaboración; no autoriza emitir nuevos actos ni sustituye una baja ordinaria de rol. Se conservan las comprobaciones de principal, contexto y responsable actuales y la independencia de quien decide.

El subrogante conserva sus Casos profesionales y puede solicitar cierre propio; no lo confirma. El titular al retornar debe revisarlo expresamente. SUB-CONT-01 requiere un circuito específico por Caso y no se deriva de este rol temporal.

El autocierre del titular regional utiliza una ruta separada: exige designación REGIONAL titular vigente, función efectiva, responsabilidad profesional de la misma cuenta y competencia sobre ese Caso. Verifica revisión actual, tareas obligatorias, espera y decisiones pendientes. Conserva cierre e historia y registra explícitamente las dos funciones ejercidas y el respaldo. No cierra el REQ institucional ni los Casos hermanos. La ruta ordinaria sigue exigiendo independencia.

## API y avisos

`GET /nucleo/subrogancias` devuelve solo funciones/solicitudes relacionadas con la persona y las instrucciones aprobadas que TI puede ejecutar. POST en la colección solicita; `/{id}/aprobar`, `rechazar`, `ejecutar`, `revocar`, `retornar`, `retirar` ejecutan los comandos con sesión individual, CSRF, UUID y revisión. El backend decide los controles disponibles. La UI no crea designaciones ni simula acciones.

`GET/POST /nucleo/casos/{id}/autocierre-regional` consulta la habilitación o confirma con revisión/fundamento. Consultar el botón no concede facultad.

Los eventos durablemente registrados producen avisos internos mínimos para titular, subrogante y autoridad con identidad vigente, deduplicados y autorizados de nuevo al consultar. Estos avisos no tienen envío de correo adicional por defecto. Recibir un aviso no constituye aprobación ni acuse y no concede acceso a expedientes.

## Persistencia y habilitación

V15.12 H2/MySQL añade `institution_function_scope`, `institution_substitution` y `function_command`, FK, índices y restricciones de separación/estado. Sin cambios de V1–V15.11, outOfOrder, backfill, titulares por inferencia o migraciones operacionales. Desactivar una función es una decisión nueva trazada; no borra su historia. No hay reversión destructiva de DDL: una revocación/retorno preserva datos y quita la facultad.

La acreditación funcional no equivale a cargar titulares, materias, sedes o credenciales reales. Esos parámetros siguen requiriendo el canal de configuración independiente y los respaldos nominales efectivos.
