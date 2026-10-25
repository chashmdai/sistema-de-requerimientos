# Cierre U4 — notificaciones, tareas de Casos y Microsoft Graph
**Unidad funcional verificada en laboratorio. Producción no habilitada.**

Rama local: notificaciones-tareas-graph-laboratorio-2026. Base conservada: e0b002c y f665a1c. Implementación: f619b97. El commit posterior incorpora contrato, resultados, evidencia sanitizada, reversión y checkpoint.

## Alcance confirmado durante esta ejecución
El responsable precisó que las tareas son personales y nacen de Casos abiertos. Esa corrección reemplaza los ejemplos iniciales de tareas independientes o impuestas. Se implementó exactamente ese alcance:
- Creación desde el detalle de un Caso abierto; vínculo obligatorio e inmutable a ese Caso.
- Creador y responsable fijados por la sesión, con la misma identidad. Ni TI ni una jefatura pueden imponer o transferir una tarea.
- Mis tareas de Casos es una vista agregada; no ofrece creación suelta.
- Cada lectura exige propiedad personal y autorización vigente sobre el Caso. Tener TI, otra función, la misma unidad o conocer el UUID no concede acceso.
- Estado, prioridad, texto privado, recordatorio, ventana de ejecución y plazo se conservan separados, con historial y finalización manual.
- La ventana registra la planificación; la interfaz advierte cuando se está fuera de ella. SIGER no ejecuta ni constata un contacto real. Un recordatorio o vencimiento no crea actuaciones, no completa tareas y no cierra Casos.
- Caso cerrado: no se crean tareas nuevas ni se cambia su planificación; las existentes se conservan y su cancelación/finalización manual no cambia el Caso.

No se incorporó un modelo ficticio de Solicitud formal ni se obliga a crear otro Requerimiento para una tarea. Se utiliza el Caso ya abierto y sus relaciones existentes.

## Cuenta nexbyte
Se provisionó mediante las API TI existentes una sola identidad:
- Usuario: nexbyte.
- Nombre: Nexbyte Proveedor.
- Correo almacenado: nexbyte.proveedor@defensorianinez.cl.
- Activa en laboratorio; contraseña individual generada en custodia Linux 0600.
- TI y DISTRIBUCION: INSTITUCION.
- DIRECTOR y PROFESIONAL: UPR / DEMO_SEDE_A.
- COORDINADOR: UPD / DEMO_SEDE_A.
- COORDINADOR_REGIONAL: DEMO_SEDE_A.
- ADMINISTRATIVO: UGI / DEMO_SEDE_B.

No hay bandera de superusuario. Se probaron facultades combinadas y denegaciones; las cuentas restringidas de la unidad anterior permanecen. El provisionador reutiliza usuario, identidad, contraseña privada y asignaciones vigentes; no llama al endpoint de reemplazo de clave. Si existiese una cuenta sin su credencial privada, se detiene esa operación en vez de resetearla.

La credencial de acceso local está en nexbyte-cuenta.json dentro del directorio privado existente. El operador autorizado puede consultarla allí privadamente; no pegarla en chat ni incluirla en argumentos, Git, capturas o informes. No es la contraseña del buzón institucional.

## Notificaciones y outbox
V12 agrega tareas/historia/comandos, eventos temporales, registro de eventos procesados, notificaciones por funcionario, outbox, intentos y presupuesto de campaña.

Se reutilizan core_event y operational_event, que ya se confirman junto a sus operaciones. Un proyector transaccional crea notificación y outbox y registra cada evento procesado en la misma transacción. No usa un cursor de máximos que pueda perder commits tardíos. El baseline anterior a V12 queda marcado para evitar envíos retrospectivos; core_notice y sus rutas históricas se conservan.

Disparadores: Comunicación pendiente de distribución, destino para jefatura, Caso asignado/reasignado, hija/antecedente, revisión de término, derivación, reapertura y recordatorio/vencimiento de tarea. La asignación de tareas a terceros quedó excluida por la corrección expresa del responsable.

Los destinatarios se obtienen de cuentas y autorizaciones actuales: distribuidor principal, coordinación de origen cuando corresponde, evaluador explícito, responsable del Caso o propietario de la tarea. Una derivación sin evaluador individual se dirige a las jefaturas autorizadas del destino. No se envía contenido a TI por el solo hecho de ser TI. Sin destinatario vigente se conserva el diagnóstico; no se inventa un reemplazo ni se difunde a todo el personal.

La campana cuenta pendientes visibles; la bandeja permite leer y navegar. Cada acceso vuelve a comprobar identidad, propiedad y recurso. Leer no aprueba, admite ni completa nada. El historial de una notificación autorizada puede conservarse después de resolver su gestión; la vigencia para un envío pendiente se comprueba por separado. Revocación/reasignación impide consultar recursos que ya no corresponden y suprime salidas pendientes. No se puede retirar un mensaje que ya fue aceptado externamente.

El motor interno procesa lotes acotados cada 15 segundos. Es una cadencia técnica, no un SLA. Se conservan las notificaciones por evento/destinatario y los temporales por fecha programada; editar un título no repite un recordatorio ya emitido.

## Graph real y custodia
Se inspeccionó la referencia local C:/siger-prueba-institucional/siger-mail-graph-minimo. Se adaptó su protocolo de aplicación client_credentials y sendMail; no se ejecutaron su campaña de seis destinatarios ni sus instrucciones de arranque/confirmación. La cuenta funcionaria continúa usando autenticación local individual; esto no implementa SSO.

El material adjunto sclient.txt se trató solo como secreto OAuth. Su valor se leyó desde la ubicación autorizada y se custodió en Linux fuera de Git, con directorio 0700 y archivo 0600. La configuración contiene la ruta, no el valor. No se imprime el secreto ni el token; no se guardan cuerpos remotos o cabeceras Authorization. Las excepciones se convierten en fase/código HTTP/estado y request-id con formato validado. El secreto solo se utiliza contra el endpoint OAuth fijo; el bearer solo se utiliza contra Graph. No se siguen redirecciones.

El destinatario se resuelve desde staff_account en MySQL en cada intento y se contrasta con la allowlist privada. El cuerpo/asunto son plantillas genéricas: no incorporan títulos/descripciones de tareas, relatos, RUN, nombres de NNA ni datos de terceros. Se conserva evento, destinatario, correo resuelto, fechas, correlación, estado e HTTP sanitizado.

Hay reserva e intento durable antes de la red. Después de la red se persiste el resultado. Un lease vencido después de EN_INTENTO se recupera como AMBIGUO y no se reenvía automáticamente. Una reserva anterior a esa fase puede recuperarse. 429 conserva espera; rechazos y resultados ambiguos no se confunden con aceptación. La campaña tiene un presupuesto persistente máximo de cuatro intentos reales; reiniciar no lo restablece.

Se mantuvieron intactos los permisos del tenant/Exchange, los servicios existentes y las credenciales anteriores. No se pidió acceso al buzón ni su contraseña, ni se usaron permisos de lectura de correo.

## Campaña y confirmación humana
Se ejecutaron **cuatro solicitudes reales secuenciales**, todas dirigidas al correo de nexbyte resuelto desde MySQL. Las cuatro devolvieron HTTP 202 y ACCEPTED_BY_GRAPH.

| Tipo | Correlación | Transporte | Confirmación humana |
|---|---|---|---|
| CASO_ASIGNADO | 26f9d0a6-ed4c-4409-b426-7239d703ddd2 | HTTP 202 | CONFIRMADA_POR_USUARIO |
| ANTECEDENTE_NUEVO | e24fe275-57d4-43f5-bb2c-f05a7ea64b74 | HTTP 202 | CONFIRMADA_POR_USUARIO |
| TAREA_RECORDATORIO | 2775b415-3ffc-48ca-a85b-4814553113c2 | HTTP 202 | CONFIRMADA_POR_USUARIO |
| TAREA_VENCIDA | 8d5ed2b3-94c0-43a6-a22d-82957051722c | HTTP 202 | CONFIRMADA_POR_USUARIO |

Se pidió revisar expresamente la bandeja y el usuario respondió **«Recibí los cuatro»**. Solo entonces se registró esa confirmación para las cuatro correlaciones, con actor y fecha. No se infirió entrega a partir de 202 ni de leer el aviso interno. La confirmación es declaración del destinatario, no telemetría de entrega de Exchange.

La documentación oficial distingue aceptación de entrega: [sendMail](https://learn.microsoft.com/en-us/graph/api/user-sendmail?view=graph-rest-1.0), [OAuth de aplicación](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-client-creds-grant-flow), [throttling](https://learn.microsoft.com/en-us/graph/throttling).

Después se probaron reinicios y repetición del E2E interno sin nuevos envíos. Una cuenta sintética con correo diferente quedó BLOQUEADO_ALLOWLIST con cero intentos; no consumió presupuesto ni llamó a Graph. El total real permanece en cuatro.

## Pruebas finales
| Verificación | Resultado |
|---|---|
| H2 | 194 correctas; cero fallos/errores/omitidas en la selección final |
| MySQL 8.4 | 194 correctas; cero fallos/errores/omitidas en la selección final |
| Frontend | 42 correctas; lint/build correctos |
| Workers documentales | Cinco pruebas reales por motor, expresamente habilitadas |
| Cliente Graph/outbox sintéticos | OAuth, caché, secreto, archivo privado, 202, rechazo, 429, timeout, fallo SQL después de aceptación, recuperación, presupuesto y allowlist |
| HTTP | Identidad individual, CSRF/HTTPS, propiedad, manipulación, notificación ajena y confirmación separada |
| E2E integrado | Nexbyte multirrol, TI, distribución, jefatura/profesional, tarea desde Caso, campana/bandeja, fechas, finalización manual, reasignación, negativos, escritorio/móvil |
| Graph real | Cuatro HTTP 202; cuatro recepciones confirmadas expresamente por el destinatario |

Las tres exclusiones históricas de carga están verificadas por el lanzador de regresiones. No se ejecutaron estrés ni consultas gubernamentales. Los tests Graph usan conexiones falsas sin credenciales reales; se distinguen de la campaña real. La campaña no se repitió para completar regresiones.

Se corrigieron durante la unidad:
1. Una denegación esperable dentro de una comprobación que abría otra TransactionTemplate marcaba la proyección entera para rollback. Se agregaron comprobaciones de recurso sin abrir otra transacción, reutilizando las mismas reglas del núcleo.
2. Distribución anulaba por una condición histórica el acceso combinado a la consulta de reaperturas. Ahora una asignación adicional de jefatura/profesional conserva su facultad y la consulta se limita al ámbito de gestión, sin convertir Distribución en permiso de Caso.
3. Una carrera de escritura de cabeceras en streaming documental asíncrono apareció en la regresión HTTP. Las mismas cabeceras se emiten antes del streaming; las pruebas completas volvieron a pasar.
4. Los estados de carga ya no se presentan como bandejas vacías y la confirmación humana se muestra separada de la aceptación técnica.

## Conservación y estado integrado
V1–V11 permanecen byte a byte iguales: veintidós archivos H2/MySQL. V12 es la única migración nueva. Se compararon las nueve Comunicaciones previas y los siete documentos públicos/comprobantes/ACK; sus huellas permanecen iguales. Los dieciséis originales ciudadanos, dos de gestión y seis páginas derivadas están íntegros; cero temporales y ACK pendientes.

La QA agregó expresamente dos Comunicaciones sintéticas, dos Casos y un Requerimiento mediante los comandos ya existentes. Total: once COM únicos, cinco Casos y tres Requerimientos. Hay tres tareas: dos personales de nexbyte (una completada manualmente en prueba) y una de otra identidad para comprobar privacidad/allowlist. No hay tareas sueltas ni impuestas. Las tareas no crearon actuaciones ni cerraron Casos.

Se reutilizaron HTTPS loopback 20443, robust_inst_formulario_lab, robust_inst_test, almacenamiento, ClamAV e inspector. No se crearon bases, volúmenes o servicios de infraestructura. El JAR activo es una copia privada fuera de target y coincide con el build final. formulario-web quedó limpio y no se modificó su configuración.

## Operación y reproducción
- Provisionar-Nexbyte-Linux.py <privado>: usa TI y las API existentes; custodia la clave local, no la imprime ni reemplaza.
- Preparar-Notificaciones-Linux.py <privado> --modo DESACTIVADO|SIMULADO|GRAPH: controla flags, custodia y allowlist. No ejecuta envíos.
- Regresion-Identidad-Linux.py <privado> --fixtures <fixtures-documental-propias>: H2; agregar --mysql para robust_inst_test. Valida las exclusiones de carga.
- En front: npm test; npm run lint; npm run build. Compilar el JAR inactivo y usar el lanzador institucional con la instancia detenida. --migrar valida/aplica V12 sin reconstruir datos.
- Probar-Notificaciones-Tareas.mjs: E2E interno reanudable. Usa QA_PLAYWRIGHT_PACKAGE, INSTITUCIONAL_LAB_PRIVATE y NODE_EXTRA_CA_CERTS, sin omitir TLS. No envía correos por defecto.
- La fase --correo consulta/reutiliza los estados de las cuatro correlaciones ya seleccionadas; solo intenta un pendiente permitido. La campaña actual está agotada y confirmada: **no repetir ni crear otra campaña sin autorización**.
- --confirmacion-humana-recibida se utilizó solo tras la respuesta humana de este chat; no debe automatizarse como simulación de recepción.
- Verificar-Custodia-Notificaciones-Linux.py <privado> --evidencias .local/evidencias/notificaciones: SELECT y SHA por streaming; acceso Docker autorizado.

Evidencias privadas ignoradas: .local/evidencias/notificaciones/, con estado reanudable, baseline, capturas, resultado de navegador, envíos sanitizados y confirmación humana. Logs de pruebas: .test-u4-h2.log, .test-u4-mysql.log, .test-u4-front.log, .lint-u4.log, .build-u4-front.log, .package-u4.log. El informe JSON adjunto contiene resultados agregados; no secretos.

## Límites y siguiente unidad
El correo automático queda desactivado. El transporte real está configurado, con allowlist única y presupuesto agotado en cuatro intentos. No hay recepción pendiente de confirmación en esta campaña. Los próximos ensayos reales requieren su autorización específica y conciliación previa; no cambiar el identificador para repetir un resultado ambiguo.

Las listas personales de tareas tienen un límite de 500 y el historial muestra los últimos 100 movimientos conservando la base completa. Debe ampliarse la paginación para una escala mayor. Los eventos sin destinatario vigente conservan diagnóstico; no hay escalamiento ni reemplazo institucional inventado. No se puede revocar una solicitud que ya salió hacia Graph.

No se implementaron SSO, subrogancias definitivas, tareas impuestas, nuevos SLA ni hardening productivo. Quedan para sus unidades la matriz definitiva de reemplazos, métricas operativas, retención y recuperación externa/capacidad. Producción no habilitada y ningún push realizado.

[Contrato](../contratos/notificaciones-tareas-laboratorio-v1.md) · [Reversión](REVERSION_NOTIFICACIONES_TAREAS_GRAPH.md) · [Checkpoint](CHECKPOINT_NOTIFICACIONES_TAREAS_GRAPH_2026-10-25.md).
