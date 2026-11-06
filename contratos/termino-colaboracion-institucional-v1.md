# Término de intervención colaboradora · versión 1

Continuación de EQU-03 y §§09–11 del levantamiento validado el 06/11/2026. Una colaboración pendiente se retira por el circuito U2 anterior. Una colaboración aceptada ya produjo un Caso propio: la principal del REQ puede proponer finalizar esa intervención, conservando la decisión sobre el Caso en su destino.

## Circuito

La jefatura principal vigente solicita término sobre una colaboración ACEPTADA y su Caso abierto, con fundamento y revisiones actuales del REQ/colaboración. Solo existe una propuesta pendiente por Caso. No crea otro REQ/Caso ni cambia la colaboración original, asignación, equipo, tareas, documentos o responsabilidad.

La jefatura efectiva del Caso acepta o rechaza fundadamente. La principal vigente puede retirar mientras esté pendiente. Aceptación/rechazo/retirada conservan identidad, autoría y decisiones anteriores. Un rechazo o retirada permite una nueva petición fundada. Si cambió la principal desde la solicitud, no se acepta como si la petición proviniera del nuevo principal: se resuelve/retira y formula una petición actualizada. Una concesión revocada del solicitante no se sustituye por el cuerpo guardado de la solicitud.

Aceptar expresa la decisión de destino sobre la propuesta; **no cierra el Caso**. Su responsable solicita término y su jefatura confirma por el circuito ordinario, incluidas independencia personal, revisión vigente, obligatorias y demás controles. Mientras una propuesta esté pendiente, la confirmación exige resolverla expresamente. El REQ sigue abierto hasta el cierre humano de su autoridad y no admite cerrar con Casos o decisiones pendientes. No se fabrican actuaciones ni acuses.

## Ámbito y datos

La principal ve un paquete mínimo de coordinación: IDs, estado del Caso, solicitud, decisión y fundamento expresamente comunicado. No obtiene el expediente colaborador. El equipo autorizado del Caso puede consultar la propuesta, pero solo su jefatura decide. La bandeja de destino filtra por ámbito actual en SQL; unidad/sede no incluyen automáticamente áreas. TI, profesionales sin pertenencia y lectura por materia no adquieren facultades.

Se comprueba autorización antes de revelar estado o revisión; un actor fuera de alcance obtiene denegación aunque suministre una revisión diferente o la propuesta ya sea terminal. Consultar, recibir aviso o aceptar no concede documentos. Los avisos de solicitud se dirigen a la función destinataria y los de decisión a las jefaturas autorizadas; no se envía correo real.

## Persistencia, API e interfaz

V15.11 H2/MySQL idénticas, únicamente tabla `institution_collaboration_end`, relaciones e índices. No modifica V1–V15.10, originales, folios o evidencias; no infiere propuestas históricas ni habilita outOfOrder. El rechazo/retirada es reversión funcional explícita conservando evidencia; no se ejecutan migraciones destructivas de descenso.

`POST /nucleo/institucional/colaboraciones/{id}/solicitar-termino` recibe revisión de REQ, revisión de colaboración y fundamento. `POST /nucleo/institucional/colaboraciones/terminos/{id}/decidir` recibe revisión, decisión y fundamento. GET de bandeja y por REQ/Caso verifica el mismo ámbito. Sesión individual, CSRF, Idempotency-Key y cerrojos identidad→COM; una carrera produce una sola decisión y un fallo de auditoría revierte todo el comando.

La UI propone desde el REQ y decide/consulta desde bandeja y Caso. Reutiliza los GET agregados de U2/continuidad para conservar una vista autorizada consistente, mostrando que aceptar mantiene Caso abierto. Historial y fundamentos son reales, no estados visuales simulados. Esta versión no implementa todavía subrogancias, autocierre regional ni SUB-CONT-01.
