# Avisos institucionales SOL y REQ · 07/11/2026

## Capacidad desarrollada

EV-03/04/05 conectan distribución, redirección y asunción con referencia interna y outbox de correo. Las decisiones conservan intención y contexto mínimo en la propia transacción; no dependen de disponibilidad de Graph. Bandeja, contador, consulta individual, enlace, lectura y correo revalidan cuenta y autorización por ámbito. Origen de redirección ve una referencia de coordinación sin recuperar SOL del destino. Lectura no es asignación, acuse ni gestión.

AvisosNucleo reutiliza institution_flow_history/workflow_event, NotificacionesInstitucionales y CorreosInstitucionales. No tabla de mensajes duplicada ni DDL nuevo. Fuente tipada1.0 y vínculos exactos seq/recurso/código; cerrojos identidad→notificaciones y negocio identidad→COM. Proyección concurrente idempotente, pendiente sin destinatario reconsiderado sin atascar otras fuentes, fuentes obsoletas suprimidas sin reenvío histórico. Roles múltiples se conservan; cuenta alias no hereda un aviso nominativo. Caducidad/revocación operan desde el catálogo vigente, sin privilegios de TI o lectura por materia.

Interfaz AvisoInstitucional integrada en App y estado de enrutamiento en SOL/REQ. Solo incluye folio/ámbitos/fecha/motivo genérico y enlace autorizado. Descarta respuestas tardías de otra notificación y datos tras denegación. Marcar leído usa ruta real con CSRF. Correo mínimo con enlace HTTPS configurable; transporte real sin base cerrado antes de red. Graph simulado/dobles durante pruebas, sin destinatarios reales ni apertura de infraestructura.

Contrato: [avisos-nucleo-institucional-v1](../contratos/avisos-nucleo-institucional-v1.md). Reglas parcialmente avanzadas NOT-01/03/04/05/07,COR-03/05 y evidencia AUD-01/03/04. No se declara el catálogo entero de eventos ni138 reglas terminadas.

## Verificación por cortes

- Focal inicial H2:39/39,03:22; AvisosNucleo11,CorreosOutbox10,NucleoU217,upgradeBitácora1. Logavisos-u2-focal-1.log.
- DOM focal21/21:Aviso5,Territorio5,Distribución11. Frontend completo176/176. Lint inicial detectó import no usado waitFor; retirado, lint/TypeScript/Vite finales aprobados.
- PruebaHTTP inicial no compiló por import estático Assertions faltante; corregido conservando aserciones. Corte posterior12 métodos:HTTPavisos2 y notificaciones8 aprobados, dominio2 con1fallo. Endurecimiento de encargado detectó pérdida de normalización de nombres JDBC al copiar fila: corregida la copia a claves minúsculas, se conserva requisito actor_id/attention_actor/contexto exactos. Logfallidoavisos-u2-http-alias-final.log preservado.
- Recalificación posterior H2/HTTP **22/22**,01:59:AvisosNucleo12,HTTPavisos2,regresiónNotificacionesHttp8; logavisos-u2-http-dominio-cierre.log. Acredita normalización JDBC y cuenta alias con misma persona/rol sin acceso al aviso nominativo ajeno.
- MySQL nativo Windows8.0.46 nuevo descartable **12/12**,cero omisiones/fallos/errores,selecciónAvisosU2. Instanciaba337869150e49de96987cca8ff392aa cerrada/proceso propio0;30migraciones hasta15.16 validadas. No uso de MySQL80 compartido/red/firewall/QA.
- CI propia aún pendiente de publicación. No atribuir resultados de padres o cortes previos a cambios posteriores.

Evidencia privada campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d. Sin E2E físico, navegador manual, correo real o proveedores reales. Guion siguiente preparado, no ejecutado. V1–V15.16 intactas; no outOfOrder/backfill o migración sobre bases preservadas. Main remoto95cf2ae sin nuevas refsU45 al verificar07/11; worktreeU45 no abierto ni cambiado.

## Cadena y CI anterior cerrada

Rama codex/siger-avisos-institucionales hija de PR13/2c85cb027136532c7de4ded652d5e6a7bae7bb80. Al escribir este corte no tiene commit/upstream/PR propio; cambios locales recuperables descritos en checkpoint. No se atribuye al hijo CI de padres.

PR12/61accdb CI42348399598: ocho jobs aprobados Windows/Ubuntu;85raícesJUnit,600métodos porOS,Win591aprobados/9omitidos,Ubuntu576/24,cero fallos/errores; agregación completa y disjunta del HEADexacto. Front164/lint/build/package. ActaPR12 actualizada. Intento anteriora3d74eb conserva fallo DOM y log íntegro51438bytes,SHA25695e7b624da6e249e7903fea5ded9b01cd400f79ec8295844ff9e11de1adc0dc6; no reetiquetado.

PR13/2c85cb0 CI42348721550: ocho jobs aprobados Windows/Ubuntu;87raícesJUnit,612métodos porOS,Win603aprobados/9omitidos,Ubuntu588/24,cero fallos/errores; agregación completa y disjunta del HEADexacto. Front171/lint/build/package. ActaPR13 actualizada. No cambio en padres/main/ambientes para cerrar estas actas.

## Guion de aceptación por roles — preparado

1. Entorno nuevo descartable y cuentas sintéticas acreditadas por U1.5. Distribuidor envía COM a jefaturaáreaA. Jefatura y asignador reciben aviso en sus sesiones y outbox; profesional, TI y otraárea no reciben ni consultan ese UUID. Marcar leído mantiene SOL pendiente y no crea REQ.
2. JefaturaA redirige aáreaB. Origen conserva referencia mínima sin enlaceSOL; jefaturaB abre SOL con su autorización. La notificación anterior no habilita gestión ni correo pendiente obsoleto. Redirección no crea REQ/transferencia.
3. JefaturaB asume/asigna profesional de su área. REQsinCaso informa encargado+jefatura sin acuse adicional. Marcar leído no cambia revisión, responsable, tareas o estadoREQ. Abrir Caso antes de proyectar suprime falsa asignación pendiente sinCaso.
4. Revocar concesiones de destinatarios antes de proyectar: fuente queda pendiente de destinatario, sin difundir a TI; otraSOL con destinatario sí progresa. Restituir autorización por circuito independiente y reproyectar crea un solo aviso. Revocar después retira detalle/lista/conteo/correo con sesión previa.
5. Dos proyecciones simultáneas no duplican avisos/outbox. Fallo SQL en intención revierte SOL/destino/comando pero conserva COM; reintento con misma clave confirma una vez. Cuenta alias de la misma persona no abre el aviso nominativo ajeno.
6. Simulación de correo lleva enlace autenticado a notificación propia y ningún relato/documento. Doble que declara transporte real sin HTTPS configurado se bloquea sin red. No ejecutar envío real como parte de este guion sin autorización de ejecución.

## Pendientes técnicos

Completar eventos restantes y contingencia superior nominal solo con configuración efectiva; comunicación externa/plantillas y preparación operativa de correo. Documentación/antecedentesREQsinCaso, clasificación/invalidación/duplicados, campos sectoriales/FIR, calendarios/plazos48h, conciliación histórica, búsqueda/reportes/exportaciones siguen programables. No bloquearlos por validación funcional ya confirmada06/11. Revisión transversal/candidata cuando las138 estén implementadas/verificadas o tengan limitación concreta, sin anticipar ese corte.
