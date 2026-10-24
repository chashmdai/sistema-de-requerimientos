# Consumidor institucional del Formulario Público completo 1.0/1.1

Referencia: [contrato publicado en a84d2e7](https://github.com/chashmdai/formulario-web/blob/a84d2e718a824e01cc8bd8cc78fba5e918d483d3/contratos/formulario-completo-multimedia-v1.1.md). Esta extensión pertenece exclusivamente a SIGER institucional. No sustituye ni modifica `/integracion/v3/entregas`, sus fixtures o los documentos históricos.

## Intercambio

1. POST HTTPS `/integracion/v3/formulario-completo/entregas/reclamar`, Bearer técnico, `X-Formulario-Versiones: 1.0,1.1`, cuerpo `{"limite":1}`. Se procesa una entrega por ciclo, sin descargas paralelas grandes.
2. Verificar estructura, versión, claves públicas confiables y comprobante Ed25519. Rechazar claves desconocidas, contenido alterado, referencias inconsistentes y autenticaciones incompatibles con la procedencia.
3. Descargar cada original GET `/{operacion}/adjuntos/{adjunto}` con Bearer y `X-Reserva`. No seguir redirecciones. Leer con buffer de 64 KiB, comprobar tamaño y SHA-256, cerrar y cancelar transferencias vencidas. Límite total 250 MiB para 1.1; límites históricos 1.0 preservados. Las respuestas JSON se limitan a 2 MiB.
4. Publicar originales privados mediante archivo temporal, fsync, rename atómico y fsync del directorio. Los nombres ciudadanos nunca construyen rutas.
5. En una transacción SQL guardar documento íntegro, metadatos, Comunicación única, folio, comprobante institucional y confirmación pendiente. No crear Requerimientos ni Casos.
6. Después del COMMIT recuperar y verificar el comprobante persistido y comprobar nuevamente todos los originales. Solo entonces POST `/{operacion}/confirmacion` con `reserva` y `recepcion:{registro,comprobante}`.
7. Cerrar la confirmación pendiente únicamente tras respuesta 204. Un error o respuesta perdida conserva el registro y el mismo comprobante para conciliación.

`comunicacion.version` es la versión del documento ciudadano. El comprobante institucional usa **1.0 / FORMULARIO_WEB_PUBLICO para adultos** y **1.1 / FORMULARIO_WEB_NNA para NNA**, independientemente de que el documento ciudadano sea 1.0 o 1.1.

## Integridad y procedencia

Se ordenan recursivamente las claves de objetos; se mantiene el orden de arrays. SHA-256 sobre UTF-8 con prefijos `SIGER_FORMULARIO_COMPLETO_1\0CONTENIDO\0` y `SIGER_FORMULARIO_COMPLETO_1\0PROCEDENCIA\0`. La procedencia incluye, por identificador de persona, sus atributos y discrepancias. Se verifican las huellas vinculadas por el sello público, identidad de operación, fecha y versión del aviso.

El receptor conserva el árbol JSON completo, incluidos campos opcionales desconocidos, valores nulos y componentes de nombres. No transforma documentos 1.0 en 1.1 ni modifica firmas. La representación legible del frontend no cambia el documento conservado.

`ADULTO_CU` exige presentador autenticado y atributos obligatorios con fuente `CLAVE_UNICA_USERINFO`, RUN consistente con esos atributos y valores disponibles. `ADULTO_DECLARADO` y `NNA` no admiten autenticación autoafirmada ni atributos CU. SRCeI y declaración ciudadana conservan sus fuentes individuales; un dato registral no puede declarar nivel `IDENTIDAD_AUTENTICADA`.

## Conservación y recuperación

V9 amplía `institutional_inbox.body` a MEDIUMTEXT en MySQL y CLOB en H2. `document_kind` separa las colas histórica y completa. No se reescriben registros previos ni migraciones V1–V8.

`institution_document` vincula cada original a su Comunicación, operación pública, descriptor firmado, huella, tamaño y referencia interna. La unicidad `(operation_id, attachment_id)` evita duplicados. Los objetos físicos usan claves internas `operacion/adjunto.bin` en una raíz privada fuera de Git. Un único escritor posee el bloqueo del almacén.

Fallo antes del COMMIT: no existe Comunicación confirmada ni se emite ACK; archivos ya íntegros se reutilizan al reintentar. Fallo después del COMMIT: se recuperan Comunicación, folio y comprobante existentes. Una reserva vencida obliga a reclamar otra; nunca a emitir otro COM. La conciliación vuelve a comprobar evidencia y firma. Los rechazos persistentes quedan pendientes/bloqueados conforme al mecanismo operacional existente; no se convierten en confirmaciones.

Un reinicio puede dejar temporales o archivos sin vínculo SQL. Se conservan para recuperación; esta unidad no los elimina automáticamente. El filesystem privado debe ofrecer fsync y rename atómico. La transacción SQL no pretende abarcar filesystem y red.

## Documentos y autorización

Origen fijado por el servidor: `INGRESO_CIUDADANO`. El modelo reserva `GESTION_INSTITUCIONAL` con autor funcionario obligatorio y sin identificador de operación pública. No se añade todavía un endpoint de carga de gestión.

Las vistas `institution_document_case` y `institution_document_requirement` derivan vínculos de Comunicación sin copiar binarios. Los antecedentes asociados se consultan usando su contexto de Caso y los permisos existentes; no se difunden a todas las unidades.

Rutas institucionales:

- GET `/nucleo/comunicaciones/{comunicacion}/documentos`.
- GET/HEAD `.../{documento}/original`.
- GET/HEAD `.../{documento}/representacion` cuando existe representación segura.
- `?caso={caso}` para el contexto autorizado de antecedentes vinculados.

Cada solicitud verifica sesión funcionaria, Comunicación y contexto. Un UUID no concede acceso. Descarga con `attachment`, `application/octet-stream`, `no-store`, `nosniff` y CSP restrictiva. Se verifican bytes antes de servir y se registra actor, documento, Comunicación y modalidad de solicitud.

En 1.1 se soportan audio/video con controles nativos y Range simple, If-Range, ETag SHA-256, HEAD y 206/416. PDF, imágenes, Office y medios históricos 1.0 ofrecen descarga autorizada; no se incrustan originales PDF/Office. Las derivaciones PNG aisladas y la ampliación del visor corresponden a la segunda unidad. No se ejecutan Office ni conversores inseguros.

## Configuración y extensión

Activación explícita `institucional.formulario-completo.habilitado=true`, perfil `jdbc-reception`; origen HTTPS, token técnico, CA opcional, confianza pública/institucional y clave de firma fuera de Git. `institucional.documentos.directorio` requiere almacenamiento privado durable. `automatico=true` activa ciclos pequeños con backoff y controles operacionales; por defecto no se activa. El laboratorio integrado usa `automatico=false` y POST técnico `/operacion/v1/formulario-completo/recuperar`.

Los permisos actuales son los del núcleo provisional. `SesionFuncionario`, el catálogo y `NucleoInstitucional.autorizarLectura` son puntos de integración para TI/accesos, cargos equivalentes, subrogancias y equipos. Esta unidad no convierte el catálogo sintético ni la responsabilidad individual vigente en una política institucional definitiva. No establece nuevos aprobadores, plazos, facultades, tareas o notificaciones.