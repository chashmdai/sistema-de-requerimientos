# Correo interno materializado · 1.0

Fuente: rector§16.1–16.4, COR-03/05/07, NOT-07 y AUD-04, validación funcional06/11/2026. Extensión privada del proyector/outbox existentes; no cambia contratos públicos1.0/1.1, originales, firmas o COMMIT→ACK.

## Preparación y transporte

Cada nuevo intento conserva una copia exacta antes de red: plantilla AVISO_INTERNO_LAB_1.0, asunto, texto, destinatario resuelto del intento, correlación, fecha técnica y origen de preparación. MANUAL registra la cuenta destinataria que solicita su envío; AUTOMATICO no atribuye una solicitud humana. No inventa aprobador, entrega, lectura o actuación. La autorización funcional está en el evento de negocio y destinatario vigentes; esta plantilla interna de laboratorio no sustituye el circuito de comunicación externa/oficio.

V15.17 crea mail_message_snapshot con FK al intento. No actualiza ni recrea intentos anteriores. Su inserción ocurre en la misma transacción que reserva, intento y presupuesto de campaña. Fallo SQL revierte ese intento sin invocar transporte, conservando el aviso y decisión de negocio anteriores. Antes de EN_INTENTO se revalida permiso/destinatario y se carga la copia persistida íntegra; ese Mensaje inmutable es el que se entrega al transporte. Una copia faltante/corrupta bloquea con BLOQUEADO_INTEGRIDAD, sin red ni reintento automático ciego.

SHA-256 de congruencia: para cada cadena en el orden [SIGER_AVISO_MATERIAL_1,notificationUUID,attemptUUID,destinatario,plantilla,materialized_ms decimal,origen,requested_by o cadena vacía,asunto,texto,correlacion], añadir longitud UTF-8 en4bytes big endian y luego bytes UTF-8, sin BOM/normalización. No incluye estado del intento, que evoluciona por resultado técnico. No es firma de remitente, recibo ni evidencia de entrega. Consulta verifica correlación/beneficiario contra su aviso, versión/origen/actor y huella antes de devolver texto.

Un reintento permitido crea otro intento/copia, sin sobrescribir el anterior incluso si cambió enlace/configuración. EN_INTENTO interrumpido se recupera AMBIGUO conservando copia; no se reenvía por no haber confirmación humana. 202, lectura del aviso y confirmación manual siguen siendo hechos distintos.

## Contenido y enlace

Todos los correos internos nuevos incluyen referencia mínima y enlace autenticado #notificacion/{notificationUUID}. No contienen relato, documentos reservados, título/descripción de tareas o datosNNA. institucional.notificaciones.enlace-base debe ser HTTPS absoluto, sin credenciales/query/fragmento y de tamaño técnico acotado. Transporte que declara ser real sin base falla antes de reservar o llamar red; simulación admite referencia relativa sin inventar dominio.

Allowlist única, campaña, presupuesto máximo4, tiempos y guardas del laboratorio permanecen. No se habilitan proveedores, correo real o producción. Remitente/Reply-To/credenciales y políticas nominales se preparan aparte, sin valores inventados.

## API y consulta

GET /nucleo/notificaciones/{id}: aviso propio actualmente visible y disponibilidad de transporte, sin marcar leído. GET /nucleo/notificaciones/{id}/intentos/{intento}/mensaje: copia ligada al intento y mismo aviso, con autorización actual antes de consultar contenido. No permite actorTI universal, otra cuenta ni un intento de otra notificación aunque ambas pertenezcan al mismo usuario. Respuestas no-store y auditoría de consultas sin generar correos/gestiones.

Intento anterior aV15.17: acreditada=false y texto/plantilla/fecha no acreditados ennull. No se renderiza como enviado usando la plantilla actual. El estado técnico previo sí se conserva. Intención preparada no acredita entrega; la interfaz lo expresa también para SIMULADO y AMBIGUO.

La vista #notificacion/{id} conecta aviso, transporte, intentos y copia. Lectura y confirmación de recepción se ejecutan solo mediante controles expresos distintos; no completa tareas o confirma asignaciones. La consulta de estado, intentos y copias, la recuperación durable de reservas y la confirmación manual de recepción pertenecen a NotificacionesInstitucionales y funcionan incluso sin adaptador de correo. Solo preparar/enviar un nuevo intento requiere el proveedor habilitado. El motor recupera reservas vencidas sin duplicar el ciclo si el transporte automático ya lo ejecutó. Sin transporte configurado conserva también aviso interno y lectura manual. Revocación/denegación/cambio de contexto descarta contenido y respuestas tardías; texto se representa como texto plano, sin HTML activo.

## Compatibilidad y reversión

V1–V15.16 intactas; sin outOfOrder, backfill o sustitución de registros. La lectura de intentos mantiene su API y normaliza nombres JSON H2/MySQL; se añaden rutas, no una nueva cola. Los enlaces antiguos de avisos siguen funcionando. Nuevos correos usan el detalle propio genérico; textos/materializaciones anteriores no se reinterpretan.

Rollback transaccional acreditado mediante fallo de inserción. No se proporciona ni ejecuta una reversión destructiva de esquema: no borrar copias para volver a un binario anterior. Compatibilidad de instalación/retorno operacional requiere sus gates, respaldo y autorización de ejecución separados.
