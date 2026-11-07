# Correo interno trazable por intento · 07/11/2026

## Capacidad implementada y límites

MensajesCorreo conserva versión, copia exacta y huella de cada intento antes de red. CorreosInstitucionales reusa outbox/guardas/ambigüedad y entrega al transporte el mensaje persistido verificado. Solicitud manual y procesamiento automático quedan diferenciados; no se atribuye preparación automática a un funcionario ni se fabrica una aprobación humana. Los intentos antiguos sin copia permanecen desconocidos, sin retroconstrucción de asunto/texto/enlace/plantilla enviados.

API y vista real #notificacion/{id} muestran aviso propio, estado de correo, intentos y copia. Texto plano, respuesta no-store, auditoría de consulta, descartes tras denegación y cancelación de respuestas de otro contexto. Consultar no marca leído ni confirma recepción/asignación; controles manuales independientes. Sin adaptador de transporte siguen disponibles estado, intentos, copia, recuperación durable y confirmación humana explícita; solo crear/enviar un intento nuevo requiere el adaptador. La recuperación del motor no se duplica con el modo automático. Un saliente conserva únicamente lo autorizado por el evento, sin recuperar expediente al consultar correo.

Todos los nuevos correos internos usan enlace HTTPS configurable a notificación propia; un transporte que declara real sin base falla cerrado. No se inventa dominio. Se mantienen allowlist/campaña/límite4/esperas y parámetros funcionales. No correo/Graph real, proveedores o despliegue. Esta copia interna de laboratorio no implementa comunicaciones ciudadanas/oficios/Docflow, remitente institucional real, Reply-To o todaCOR-07.

Contrato [correo-interno-materializado-v1](../contratos/correo-interno-materializado-v1.md). Reglas avanzadas parcialmente COR-03/05/07,NOT-01/07,AUD-04; evidencia AUD-03. Matriz138 conserva IDs/orden/validaciónfuncional06/11 y no declara todo el catálogo completado.

## Persistencia y arquitectura

V15.17__constancia_correo_interno agrega mail_message_snapshot, FK a mail_attempt y solicitante, versiones/origen/fecha acotados. H2 y MySQL con texto idéntico, SHA256 **5FC30B970B629266880576C92E66410EBA2678732DCE5C127165C7A53803359C**. V1–V15.16 no editadas, sin outOfOrder o backfill. No se copian originales/formulario ni se altera custodia, firmas, folios o COMMIT→ACK.

Misma transacción para reserva/intento/copia/presupuesto; fallo de copia no revierte el negocio ya confirmado, pero deja el outbox pendiente sin falsa llamada de transporte. Fuente/Copia se revalida por aviso/cuenta/intento/correlación/hash. Una corrupción bloquea consulta/transporte, sin reconstruir otro texto. SHA es congruencia de registro, no firma o prueba de entrega. Reintento guarda otra copia; aceptación/ambigüedad/confirmación humana siguen en sus estados propios.

Nuevo código separado en MensajesCorreo, sin servicio externo ni cola duplicada; NotificacionesInstitucionales autoriza, detalla, audita, consulta y recupera independientemente del transporte. Correos delega estas responsabilidades y se concentra en preparar/transportar. No consultas de metadatos globales porTI. DatosNucleoTest limpia nueva tabla solo siV15.17 aplicada, preserva targets15.16 previos; nuevo fixture opcionaltarget y schemas sintéticos _correo/_correoprev se rechazan fuera de prefijoGUID/loopback verificado.

## Validaciones ejecutadas

- Regresión inicial **22/22 H2**,02:19:CorreosOutbox10+AvisosNucleo12,logcorreo-materializado-base.log.
- Focal **21/21 H2/HTTP**,01:23:CorreoMaterializado12+CorreoMigracion1+NotificacionesHttp8. Copia exacta contra mensaje capturado por doble; automático sin autor humano; retry429/cambio de enlace conserva copia anterior; ambigüedad/reinicio sin reenvío; falloSQL rollback; concurrencia; permisos/UUID ajeno/revocación/integridad; consultas sin lectura/acuse/correos. Target15.16 conserva intentos/outbox/firmas de autoridad y no crea copias retroactivas.
- Nuevos controlesHTTP posteriores **5/5**,33s:2copia+3avisos; sesión/CSRF/no-store,detalle propio,campos JSONuniformes,denegación de otro actor,intento ajeno y texto corrupto,transporte no configurado. MockMvc no navegador físico.
- MySQL nativo Windows8.0.46 nuevo descartable **13/13**,cero fallos/errores/omisiones,selecciónCorreoMaterializado. Instancia19b244b9c4d6459a9c9be21fa7597c58 cerrada/proceso propio0;31migraciones en bases nuevas y actualización15.16→15.17 con registros anteriores conservados.
- Regresión posterior al desacoplamiento **37/37 H2/HTTP**,02:24:CorreoMaterializado12+CorreoMigracion1+CorreosOutbox10+NotificacionesHttp10+AvisosNucleoHttp4. Incluye API sin bean de correo con constancias y confirmación disponibles, y envío cerrado409.
- Inyección de alteración **después del COMMIT** de preparación y antes de red: **1/1 H2**,13s. Conexión independiente al mismo destino descartable, sincronización exclusiva del test; BLOQUEADO_INTEGRIDAD, cero llamadas de transporte y sin reintento. Producción no incorpora un hook de prueba.
- Calificación final **14/14 MySQL nativo Windows8.0.46**,CorreoMaterializado13+upgrade1,después del desacoplamiento y la nueva prueba poscommit. Instancia d14edab31e68447183841843cdbf3daf cerrada y proceso propio0;31migraciones y actualización15.16→15.17. No se reutiliza el corte anterior como calificación del cambio posterior.
- Corrección de regresión `AutoridadMigracionTest`: expectativa explícita final15.17, nueva ruta de actualización desde15.16 y comparación de DDL17. Conserva igualdad de registros históricos, concesión concurrente única y revocación. **24/24 H2**,30s, y **24/24 MySQL nativo Windows8.0.46**,instancia04f1dc8e075d40ec933397480f9308f9 cerrada/proceso0; sin cambios adicionales de producción o SQL. Logs correo-autoridad-v1517-{h2,mysql}. No se repite frontend al cambiar solo test/auxiliar.
- DOM focal **23/23**,nuevo detalle7+TareasNotificaciones9+AvisoInstitucional7. Frontend integral final **187/187**,lint/TypeScript/Vite correctos, repetidos después del ajuste de lectura histórica sin proveedor. Incluye texto escapado,recepciónsoloexpresa,revocación,respuesta tardía,sin transporte y rutaApp real con API doblada. No E2E físico/manual ni entrega real.
- Auxiliar PowerShell parseado; aislamiento nativo con datadir/credenciales/puerto propios,no-defaults,binding/owner comprobados y cierre de procesos solo de campaña. No MySQLglobal/firewall/red/QA modificados.

Evidencia privada campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d,logs correo-materializado-*,correo-sin-proveedor-* y correo-integridad-poscommit-h2.log. Sin exclusiones o timeouts relajados. CI propia todavía pendiente de publicación; no atribuir resultados del padre. No reversión de esquema destructiva; rollback de transacciones probado.

## Cadena y estado del corte

Rama codex/siger-correos-trazables, hija creada con árbol limpio de PR15/5d9d3872427c6d7fdbbcc2f177fc304618329af2. PR15borrador basePR14,CI42353760616 aprobada en ocho jobs:90raíces/642métodos porOS,Windows633aprobados/9omitidos,Ubuntu618/24,cero fallos/errores;FE180/lint/TypeScript/Vite/package. Cobertura completa/disjunta del mismoHEAD/run y acta verificada. No cambiar padres/main. PR14/395a7b0 CI42351175167 completa:89raíces/626métodos porOS,Win617aprobados/9omitidos,Ubuntu602/24,FE176/lint/build/package,cero fallos/errores,ochojobs completos/disjuntos; acta cerrada. El hijo actual aún sin commit/upstream/PR al escribir este corte; checkpoint conserva siguiente acción.

## Guion por roles — preparado, no ejecutado

1. Nuevo entorno descartable, sesión de profesional sintético con tarea/asignación legítima. Generar aviso y abrir detalle desde bandeja o enlace de correo simulado. Consultar transporte/copia no registra lectura/acuse/cumplimiento.
2. Preparar intentoSIMULADO. Comparar asunto/texto/destinatario/correlación/plantilla con copia registrada; mensaje no incluye título/descripción de tarea ni relatoNNA. Preparación automática no muestra persona como solicitante.
3. Simular429 y reintento después de espera, cambiando base de enlace en configuración aislada. Dos intentos/copia; primera permanece idéntica. Resultado ambiguo conserva copia y no permite reenvío ciego.
4. Históricos restaurados únicamente en base descartable aprobada: intento anterior sin copia muestra no acreditada, no usa plantilla actual ni reenvía uno ya aceptado.
5. TI/otra cuenta/intento de otra notificación no leen copia. Revocar permiso con sesión previa y abrir enlace: denegado,vista descartada. Cambiar aviso con petición de copia pendiente no muestra respuesta tardía anterior.
6. Correo no configurado: aviso interno, lectura manual, estado, intentos, copias y recuperación funcionan sin proveedor. Si existe aceptación técnica o ambigüedad, confirmar recepción solo con control expreso. Nunca usar estos pasos para enviar correo real sin autorización de ejecución.

## Pendientes reales

Otros eventos§16, toma/supervisión de clasificación, contingencia de correo/Reply-To/remitente nominal, comunicaciones externas y oficios, documentosREQsinCaso, plazos48h/calendarios, ingreso flexible/invalidaciones, conciliación histórica, búsqueda/reportes/exportaciones y revisión transversal de laboratorio siguen técnicos pendientes. Parámetros oficiales ausentes se configuran sin inventarlos; la validación funcional ya está confirmada. No candidata final ni138reglas completas todavía.
