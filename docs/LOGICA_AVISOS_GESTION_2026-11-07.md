# Avisos de asignación, cierre y reactivación · 07/11/2026

## Implementación

AvisosGestion integra EV-06/11/18 con apertura y reasignación efectiva, devolución/cierre de Caso, cierre explícito de REQ y reactivación efectiva coordinada. Incluye primera atención finalizada sinCaso, sin crear una asignación pendiente ficticia. Backend, API y pantalla de aviso reutilizan proyector/outbox/bandeja/contador/correo del bloque anterior; enrutamiento visible en Caso/REQ, acuse manual separado. No nuevo servicio externo o tabla de mensajes duplicada.

La constancia al saliente está anclada a su asignación anterior exacta y cuenta nominativa. Conserva solo folio/ámbito/motivo genérico; no restituye Caso/tareas/documentos. Raíz, vigencia, vínculo y huella de credencial vigentes se comprueban al consultar/enviar; alias, TI u otra área no heredan aviso. Responsables/equipo/jefatura actuales consultan según su ámbito. Lectura por materia no concede gestión. Cambio de contexto en la interfaz descarta avisos/acuses anteriores antes de resolver la siguiente autorización.

Intención tipada1.0 en historia y evento de la misma transacción, usando el escritor compartido AvisosNucleo. Datos1.0SOL/REQ del padre y registros firmados anteriores conservan semántica. Asignaciones, períodos y cierres inmutables no se borran; lectura no confirma acuse ni realiza cierre/reapertura. Reactivación autorizada pero aún no efectiva no genera aviso de reactivación. Cierres deCaso no cierranREQ.

El proyector trata de forma segura fuentesCORE antiguas que carecen de resource_type, evita duplicar señal provisional de asignación si existe intención tipada delCaso y conserva límites/lógica de reintentos. Los fallos JDBC se propagan; no se maquillan como404. DatosAvisosTest conecta Core con Autoridades reales del fixture firmado existente como el arranque, sin SQL de privilegios ni bypass de aprobación. Las nuevas comprobaciones de identidad no modifican mandatos ni firmas antiguas.

Contrato: [avisos-gestion-institucional-v1](../contratos/avisos-gestion-institucional-v1.md). Sin nueva migración; V1–V15.16 y contratos públicos1.0/1.1, custodia/folios/hashes/COMMIT→ACK intactos. Ningún ambiente, servicio, proveedor real o padre/main intervenido.

## Pruebas y estado del corte

- Compilación inicial detectó nombre de recordAcuse inexistente en test; se usa el contrato realAcusar.
- Focal2 H222métodos:2aprobados,20errores por Set.of.contains(null) al generalizar fuentesCORE sin resource_type. Logavisos-gestion-focal-2.log conservado. Guardia nula corregida, sin excluir pruebas ni relajar permisos/timeouts/aserciones.
- Focal3 **28/28 H2/HTTP**,03:48:dominio nuevo13,HTTPavisos3 y regresiónAvisosNucleo12. Incluye referencia saliente sinCaso,acuse independiente,revocación de credencial,alias,devolución/cierre/equipo,REQsinCaso,autorización vs reactivación,rollback/concurrencia,primera atención y raíz/versión obsoletas. Posterior retiro de parámetro JsonMapper sinuso y propagación explícita DataAccessException se incluyen en la calificación nativa/regresión siguiente.
- Frontend final **180/180**,lint/TypeScript/Vite correctos, tras prueba controlada de descarte de aviso/acuse al cambiarCaso. DOM focal anterior26/26; no navegador físico.
- MySQL nativo Windows8.0.46 en dos instancias nuevas descartables: batería15 con **14aprobadas/1error de fixture** (test había utilizado UUID de propuesta como si fuera Caso, compilado antes de su corrección),instancia7ed9a7db6fa444f29c9bdbe3a266456e; focal corregida **1/1**,instancia9f1672ce0813496690a6ebe093aed1ff. Ambas cerradas/proceso propio0. Se acredita cada caso en esos cortes, no una ejecución integral15/15.30migraciones hasta15.16 validadas; sinDDLnuevo. Logfallido preservado.
- Regresión afectada **87métodos,86aprobados/1fallo/0errores**,06:00: continuidad de origen revocado devolvía403 en vez de404 al conectar Autoridades reales en el fixture. Se conserva la aserción404: jefaturaOrigen normaliza la denegación de comprobación alternativa de acto conservado sin conceder autoridad; fallos ajenos se propagan. Las otras baterías U2/Equipo/Tareas/Correos/Ingreso/HTTP aprobaron.
- Calificación H2 posterior **23/23**,02:00,ContinuidadU2 completa17+Subrogancias1 de actos conservados+AvisosGestion2+HTTP3. Incluye denegación404 por revocación y aceptación legítima durante ausencia/retorno, sin cambiar autores. Cierra vía indirecta del acuse: saliente sin consulta delCaso tampoco conoce el acuse del entrante. Logavisos-gestion-seguridad-final.log.
- MySQL focal de seguridad **4/4** (AvisosGestion2+revocaciónContinuidad1+actoConservadoSubrogancia1),instanciab5d8427fe3a34c729dd2e8c7f8d603f6 cerrada/proceso0. Acredita exclusión del estado de acuse para saliente,denegación404 y conservación legítima de actos. CI del hijo pendiente. Sin navegador manual/E2E físico o Graph/correo real. Dobles/simulaciones no acreditan transporte real.

Evidencia privada campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logsavisos-gestion-*. Matriz138 conserva IDs/orden/validación06/11 y añade evidencia D2 acreditada, sin declarar todos los eventos/reglas completos. Guion siguiente preparado, no ejecutado.

## Cadena

Rama codex/siger-avisos-continuidad, hija creada con árbol limpio del PR14/395a7b0bce9da208738c7a356ce41976da970e56. CI del padre42351175167 cerrada:89raíces/626métodos porOS,Windows617aprobados/9omitidos,Ubuntu602/24,FE176/lint/build/package,cero fallos/errores,ocho jobs completos/disjuntos. ActaPR14 y reporteD1 actualizados, sin modificar rama. No reutilizar éxitos de otrosHEADs. PadresPR12/61accdb yPR13/2c85cb0 cerraron CI completaWindows/Ubuntu (600/612métodos respectivamente yFE164/171); actas y fallos históricos preservados. Este bloque aún sin commit/PR al escribir; próximo paso preciso en checkpoint.

## Guion por roles — preparado

1. Cuenta/jefatura/profesionales/equipo sintéticos mediante circuito firmado independiente. Abrir Caso del mismoREQ: aviso a entrante+jefatura, dos canales durables, sin acuse automático. Marcar leído conserva revisión/actuaciones; acusar después marca únicamente la asignación exacta.
2. Jefatura reasigna a otro profesional vigente. Entrante consultaCaso; saliente recibe constancia mínima y su GETcaso/documento/tarea se deniega salvo facultad independiente real. Alias de misma persona no abre notificación ajena. Credencial cambiada o raíz expirada retira la constancia; nunca basta rolTI.
3. Incorporar colaborador; profesional solicita término, jefatura devuelve, luego confirma cierre tras resolver obligaciones. Responsable/equipo/jefatura reciben resultados. Se conserva un cierre yREQactivo hasta decisión explícita. Notificación no cumple tareas pendientes.
4. CerrarREQsinCaso tras atención efectiva. Aviso de término sinCase,acuse oasignación ficticia. Primera orientación resuelta también informa término y conserva respuesta/fecha reales.
5. ConCaso yREQcerrados, solicitar reapertura y autorizar reactivaciónREQ: no aviso de reactivación efectiva todavía. JefaturaCase aprueba mediante circuito vigente: mismoREQ/Caso,nuevo período,cierre anterior conservado y aviso correspondiente. COMhija/lectura no reabren nada.
6. Simular dos proyecciones y fallo SQL de intención en base descartable: no duplicados ni cierre parcial; reintento confirma un evento. Cambiar rápidamente entreCasos en navegador: avisos/acuses previos desaparecen antes de resolver autorización nueva.

## Pendientes

Otros eventos§16 (toma de clasificación, valoración/pertinencia completa, urgencia y contingencia de correo), materialización exacta/plantilla versionada del correo interno y documentaciónREQsinCaso, calendarios/plazos48h, ingreso flexible, conciliación histórica, búsqueda/reportes/exportación y cierre transversal del producto siguen pendientes técnicos. La validación funcional06/11 permanece confirmada; no se solicita otra. No declarar138 completas/candidata final hasta implementar/verificar o limitar concretamente cada brecha.
