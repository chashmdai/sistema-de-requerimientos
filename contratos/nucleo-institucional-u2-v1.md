# Núcleo institucional U2 · contrato 1

Rector del 28/10/2026, §§03/04/06–12. Evolución de aplicación sobre el bloque de pertinencia del PR #3 (`c07a420`). No modifica contratos Formulario–SIGER 1.0/1.1, comprobantes, custodia, firmas ni COMMIT→ACK.

## Agregados y decisiones

- COM: recepción manual/pública conservada íntegra e idempotente. La ingesta no crea SOL/REQ/CAS. El registro manual pendiente continúa en `/nucleo/comunicaciones/manual`, sin respuesta embebida. Una respuesta resuelta usa la primera atención institucional.
- SOL: entidad `institution_sol`, distinta de `institution_request` del U5 histórico. Única por COM y con un único puntero al destino actual. Estados PENDIENTE/ASUMIDA, revisión optimista e historia acumulativa de clasificación/redirección. Los destinos anteriores se conservan para historia; no otorgan acceso actual a la COM U2.
- REQ: nace al asumir/asignar SOL por jefatura competente; cero Casos es un estado funcional válido. La asignación formal de atención se limita a profesional/jefatura del ámbito; la facultad administrativa de resolver su propia primera atención no se convierte en responsabilidad profesional por una asignación. `institution_req` acredita contexto principal unidad/sede/área, persona encargada de atención, origen y revisión de autoridad que respalda la decisión. La jefatura vigente se resuelve por concesiones actuales de ese contexto. No se infiere del primer Caso ni se incorpora un principal ficticio al registro firmado 1.3.
- Primera atención: COM manual + REQ terminado, cero CAS, respuesta concreta con autor/fecha, en una transacción. Funcionario habilitado como profesional, administrativo o jefatura en el contexto; TI/distribución por sí solos no habilitan la respuesta. La captura conserva su sede y el principal requiere facultad explícita para su ámbito, sin forzar que ambas sedes coincidan. Se requiere jefatura vigente acreditada del ámbito, pero no una aprobación de Caso.
- Ingreso propio de jefatura: su COM manual pendiente puede ser asumida/asignada directamente por ella en su ámbito competente; genera SOL asumida y REQ sin paso artificial por distribución.
- CAS: requiere REQ activo ya existente, objetivo independiente y responsable profesional/jefatura en su ámbito. N Casos reutilizan el mismo REQ. Las áreas obligatorias, equipos/permisos vigentes, materia y protección documental se conservan.
- Complejidad: BAJA/verde, MEDIANA/amarillo, ALTA/rojo, o sin clasificación registrada. Solo REQ U2, decisión fundada de jefatura principal. No se calcula de los Casos. El nuevo Caso no recibe complejidad provisional ni clasificación BAJA.
- Atención sin CAS: la persona encargada habilitada y la jefatura pueden registrar actuaciones/respuesta y cerrar expresamente. Una vez que existen Casos, las actuaciones se registran en su Caso; el cierre global corresponde a jefatura principal.
- Cierre REQ: decisión humana fundada, sin Casos abiertos ni colaboraciones pendientes. Cerrar el último Caso no cierra REQ. Abrir Caso, aceptar colaboración y cerrar REQ serializan la COM raíz. No se usan tareas personales o recordatorios como bloqueadores artificiales.
- Cierre de nuevo CAS: solicitud del responsable y confirmación de una persona diferente con jefatura vigente. No cierre profesional BAJA ni autocierre por rol genérico de coordinación. Se comparan personas canónicas, también entre cuentas alias. Las excepciones regionales específicas no se presumen.

## Colaboración y territorio

La principal solicita un objetivo limitado a otro ámbito y su jefatura decide aceptar/rechazar con fundamento; origen puede retirar mientras esté pendiente. Aceptación y creación/asignación del Caso paralelo ocurren juntas. No hay asignación unilateral a profesionales ajenos, transferencia del principal ni cierre del Caso origen. Reintentar conserva la misma decisión/Caso. Antes de aceptación se expone únicamente el paquete de evaluación —contexto, identificadores y objetivo—, sin actuaciones ni expediente original.

Web entra a clasificación central. Para manual en sede regional acreditada, la coordinación distribuye solo dentro de la sede de captura, incluso si tiene otras funciones. Redirigir SOL compete a la jefatura receptora y no es transferencia de REQ. Captura, región del asunto y domicilio del requiriente son datos diferentes.

La región de domicilio se registra expresamente en la clasificación, sin modificar lo declarado en la COM. **No se utiliza `subject_region` como domicilio ni la cobertura técnica histórica como aprobación institucional.** Sin cobertura territorial acreditada, una SOL regional con domicilio utilizable queda cerrada para asunción (`COBERTURA_TERRITORIAL_NO_ACREDITADA`). Si falta antecedente utilizable, la jefatura regional decide con fundamento y referencias conservados. No se inventa cartografía nominal ni se habilita una excepción general a coberturas conocidas.

## Autorización y documentos

Todas las decisiones comprueban identidad, concesión, vigencia y área desde backend. Roles por unidad/sede no cubren áreas acreditadas. Gestión y consulta por materia siguen separadas.

COM U2: acceso por jefatura del destino SOL vigente, principal/encargado actual de REQ, o registrador individual del ingreso conforme a §05.4/ING-02. La consulta del propio ingreso no da gestión del REQ asignado a otra persona ni pertenencia a sus equipos. Un destino anterior, un Caso colaborador, TI o lectura MATERIA no conceden lectura de la COM raíz. Los mismos predicados se usan en buzón, búsqueda y conteo. FIR y permisos documentales se verifican separadamente; la proyección ordinaria completa del avance para registrador sigue parcial.

Jefatura principal obtiene estados mínimos de todos sus Casos para coordinación; el encargado profesional solo recibe identificadores de Casos que gestiona. Las colaboraciones de coordinación se exponen a jefatura principal; cada destino recibe su paquete limitado. La pertenencia a REQ no concede acceso al cuerpo de otro Caso.

El Caso colaborador no hereda el original ciudadano por compartir COM/REQ. También se controla el acceso al proyectar el original y al autorizar documentos. Documentos propios de gestión siguen funcionando. Un consentimiento documental expreso mantiene los controles de contenido/descarga, propietario, materia, área y revisión implementados en PR #3. No se amplían FIR o reservas por colaboración.

Pertinencia de COM hijas reutiliza el principal explícito nacido en U2. Su historia incorpora `principal_origin=ASUNCION_SOL/PRIMERA_ATENCION`; el histórico firmado conserva `REGISTRO_1_3`. Las firmas anteriores no se reinterpretan.

## API conectada

Prefijo `/nucleo/institucional`; sesión individual y CSRF existentes. Cada POST exige `Idempotency-Key`, vinculado a actor, acción, recurso y payload. Las revisiones obsoletas dan 409; recursos fuera de ámbito, 404.

| Ruta | Efecto |
|---|---|
| POST `/primera-atencion` | COM + REQ terminado sin CAS, respuesta efectiva |
| POST `/comunicaciones/{id}/distribuir` | SOL de destino único, clasificación; no REQ |
| POST `/comunicaciones/{id}/asumir-propia` | Ingreso manual propio de jefatura → SOL asumida + REQ |
| GET `/solicitudes`, `/solicitudes/{id}` | SOL autorizadas y decisiones disponibles |
| POST `/solicitudes/{id}/redirigir` | Mismo SOL, nuevo destino e historia; no REQ |
| POST `/solicitudes/{id}/asumir` | Asume/asigna y crea REQ sin CAS |
| GET `/requerimientos`, `/requerimientos/{id}` | Principal vigente, atención, estado y coordinación autorizada |
| GET `/requerimientos/referencia?folio=…` | Referencia mínima por folio exacto para distribución, sin contenido ni Casos |
| POST `/requerimientos/{id}/actuaciones` | Atención sin Caso |
| POST `/requerimientos/{id}/complejidad` | Clasificación exclusiva del REQ |
| POST `/requerimientos/{id}/casos` | Caso adicional bajo el mismo REQ |
| POST `/requerimientos/{id}/cerrar` | Cierre explícito y validación de intervenciones pendientes |
| POST `/requerimientos/{id}/colaboraciones` | Solicitud de intervención paralela |
| GET `/colaboraciones` | Paquetes del ámbito receptor |
| POST `/colaboraciones/{id}/decidir` | Aceptación con Caso atómico, rechazo o retirada |

Interfaz: SOL y Requerimientos, primera atención, clasificación desde COM, asunción/redirección, REQ con/sin CAS, complejidad, colaboración y gestión de Caso existente. Se conservan los comandos de actuación y revisión de CAS, con restricciones U2 desde el dominio.

## Compatibilidad y límites del corte

V15.7 es aditiva: cinco tablas de aplicación, dos índices y columna de procedencia en historia de pertinencia. V1–V15.6 intactas, sin outOfOrder ni semillas nominales. Se prueba creación nueva y actualización desde V14, V15.1…V15.6 en bases descartables.

Compatibilidad aditiva del esquema no acredita ejecutar un binario anterior sobre datos U2: ignoraría los nuevos agregados y predicados. No se habilita downgrade de autorización ni despliegue por publicar la rama.

Las rutas públicas antiguas de distribución múltiple, responder/descartar COM, abrir Caso desde destino y resolver/finalizar destino rechazan **ejecuciones nuevas** con 409 `FLUJO_INSTITUCIONAL_REQUERIDO`. También se rechaza una respuesta nueva embebida en el registro manual antiguo. Un reintento exacto de un comando ya confirmado conserva su resultado: identidad y fila del comando bloqueadas, actor/acción/recurso/hash verificados por el servicio original, sin volver a ejecutar trabajo histórico. Una clave nueva o payload diferente no rehabilitan esas operaciones. La ingesta pendiente manual conserva su contrato. U5 responder/canalizar/escalar continúa cerrado.

No se convierten automáticamente los REQ/CAS/destinos históricos: no se deduce principal, complejidad o colaboración de sus filas. Sus componentes internos y pruebas conservan las semánticas históricas como evidencia de compatibilidad; las transiciones históricas de Casos anteriores aún requieren conciliación institucional y no se acreditan como modelo U2. Una COM con gestión provisional previa rechaza su alta U2 con `HISTORICO_REQUIERE_CONCILIACION`.

Derivación formal, transferencia principal, reapertura institucional de nuevos Casos, acuses/asignación prolongada, avisos específicos SOL/REQ y conciliación histórica no se declaran terminados. Los nuevos Casos no pueden usar la derivación/reapertura histórica como atajo. No se habilitan reservas extraordinarias, autoexcepciones regionales, productores externos ni despliegues.
