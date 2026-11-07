# Asociaciones documentales de gestión · contrato privado 1.0

Reglas DOC-07, DOC-04, DOC-01, ACC-09 y AUD-04; rector §15.5. Validación funcional confirmada el 06/11/2026. Esta implementación no concede autorizaciones nominales ni habilita despliegue.

## Recurso y autoridad

`GET/POST /nucleo/{casos|requerimientos}/{recurso}/documentos-gestion/{documento}/asociaciones`. Documento de gestión con propietario acreditado del mismo contexto. Cuenta individual, credencial y ámbito actuales; responsable profesional o jefatura efectiva del propietario. Colaboración, materia, registrador, TI o cuenta alias no sustituyen esa responsabilidad. Documento histórico sin propietario acreditado requiere su circuito de acreditación, no se regulariza mediante esta operación.

GET devuelve revisión (-1 si todavía no fue rectificada), estado, cuenta actuante, referencias vigentes e historia de los últimos 100 cambios. No devuelve enlaces a otros expedientes. POST requiere sesión/CSRF, Idempotency-Key, X-Siger-Actor igual a la cuenta actual y cuerpo `{revision,asociaciones,fundamento}`. CASO usa `CASO` o UUID de una actuación real de ese Caso; REQ usa `REQ` o secuencia decimal canónica de una constancia real de ese REQ. Lista completa, sin duplicados, máximo técnico 500. Conjunto vacío significa retirada explícita. No se acepta referencia a una actuación o REQ ajeno ni se fabrica una gestión para poder referenciarla.

## Persistencia y transacción

V15.21 aditiva e idéntica en H2/MySQL. Control/revisión y destinos son proyección vigente; historia conserva conjuntos anterior/nuevo, fundamento, actor y fecha del servidor. Los vínculos originales se conservan y cualquier vínculo nuevo validado también queda preservado. Solo la proyección se reemplaza; no se borran originales, actuaciones ni historia. Sin backfill o inferencia sobre registros previos. Una base anterior conserva su semántica de lectura; la API nueva requiere V15.21. Presencia positiva de migración cacheada, nunca decisiones de acceso o de retirada. Si falta la tabla de una migración acreditada, la consulta falla, no abre el acceso.

Orden de bloqueo identidad → Comunicación. Revalidación de autoridad y revisión dentro de la transacción; una sola revisión ante solicitudes concurrentes. Comando vinculado a cuenta, documento, contexto y cuerpo exactos. Reintento confirmado no agrega historia; misma clave con cuerpo distinto da409. Una asociación sin cambios da409. Error de FK/historia revierte proyección, vínculos nuevos, revisión de protección y comando.

## Efectos de la retirada

Retirar la asociación del propietario suspende contenido, descarga, historial ordinario y versiones descriptivas, también mediante referencias de otros Casos, consulta por materia, HEAD/Range, vistas ya cacheadas y revalidaciones cada64KiB. Equipo y lectores indirectos no obtienen el documento retirado ni lo cuentan en su lista. Responsable/jefatura conserva metadatos mínimos y el control explícito de asociaciones para corregir el error; no obtiene bytes por esa excepción de gestión. Los bytes entregados antes de una retirada no se pueden recuperar retroactivamente.

Cada rectificación incrementa también la revisión de protección y conserva su propia constancia. Restaurar no revive publicaciones, consentimientos o excepciones firmadas ligados a una revisión anterior: necesitan la autorización vigente correspondiente. La clasificación FIR/BLOQUEADO y la cuarentena técnica permanecen independientes. La retirada no copia un archivo, mueve su propietario, cambia principal/equipo, reabre un Caso ni elimina nada.

Una ruta histórica de agregar vínculo no puede reactivar una referencia previamente retirada o modificar una proyección explícita sin fundamento; devuelve409 ASOCIACION_REQUIERE_RECTIFICACION cuando corresponde. Cargas confirmadas pueden devolver metadatos autorizados retirados, nunca bytes ni un original nuevo.

## Interfaz y límites

AsociacionDocumento conectado al Caso y al REQ. Consulta diferida, conjuntos visibles, selección de actuaciones/gestiones reales, retirada/restauración fundada e historia. Incertidumbre conserva cuerpo, clave y cuenta; cambio de contexto aborta y descarta respuestas tardías. Pin de cuenta backend impide ejecutar una operación pendiente bajo otra sesión. Controles de bytes se cierran incluso ante banderas antiguas del cliente.

No modifica asociaciones de adjuntos dentro del sobre ciudadano firmado, ni traslada la propiedad entre REQ/Casos. Esos contextos requieren pertinencia/publicación o conciliación explícitas. No implementa eliminación física ni activa una política de retención. Cierre/ACK/transferencia nunca ordenan borrado.
