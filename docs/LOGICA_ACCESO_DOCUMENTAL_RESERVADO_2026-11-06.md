# Acceso documental con doble respaldo y consulta aislada · 06/11/2026

## Resultado y alcance acreditado

La jefatura del Caso propietario propone una cuenta/documento/Caso/período; Gestión Institucional acreditada de otra persona decide. La aprobación permite exactamente metadatos, consulta y/o descarga, según lo concedido. No agrega roles, gestión, reclasificación ni acceso a otros documentos/Casos. Retirada, rechazo, revocación, expiración y suspensión/cambio de respaldo se aplican en backend.

Registro firmado explícito1.6, con facultad de Gestión distinta de cierre1.5; identidad/credencial/persona canónica, ámbito exacto y vigencia. Alias no heredan la concesión nominal. Una persona con ambas facultades no puede darse ni aprobarse el acceso. Se impiden ciclos de aprobación con las instrucciones U1.5 y las concesiones documentales vigentes que verifica el circuito. El respaldo de jefatura y el de Gestión quedan conservados con hash, raíz firmada e historial.

`ContenidoInstitucional.permisos` conecta el permiso con listados/visores/originales/Range/historial permitido y reevaluación en streaming existentes. No se abre FIR por compartir REQ, Caso, materia o enlace. Nueva protección/área/materia invalida la decisión por revisión/secuencias, incluso al volver al valor previo. Se oculta por igual un documento inexistente y uno fuera del Caso autorizado.

El acceso previo al Caso es precondición del corte C1/firma1.6. C2 agrega una consulta aislada explícita1.7 sin cambiar esa precondición ni las firmas anteriores. Ningún corte implementa una ficha sectorial completa, nuevas clases de reserva, retención/purga o exportación irrestricta. Son pendientes técnicos/configurables, no falta de validación funcional. C1 permite completar SUB-CONT-01 con antecedentes FIR cuando el revisor tiene consulta legítima del Caso y recibe respaldo documental independiente.

## Componentes y persistencia

- `AccesoReservado/Controller`, ámbito de confianza1.6 en `AutoridadesInstitucionales`, integración en `NucleoInstitucional` y `ContenidoInstitucional`.
- React `AccesosReservados`, propuesta en protección de un documento FIR, bandeja real por facultades del backend, permisos de contenido/descarga separados y vigencias explícitas.
- Avisos mínimos por propuesta/decisión/expiración, outbox de correo durable, deduplicación y reautorización de listas/enlaces. Cero envíos reales; no se confunden consulta, correo o actuación.
- **V15.14**, H2/MySQL idénticas, nuevas y aditivas. FK, CHECK de separación/fechas/estados/descarga y unicidad de concesión no terminal. Sin modificar V1–V15.13, outOfOrder, originales, firmas, transporte, COMMIT→ACK o datos preservados. No hay backfill.

Las decisiones se serializan con identidad→COM; autorización, persistencia, auditoría y evento son atómicos. Propuesta sellada, revisión obsoleta rechazada, comando idempotente sin resucitar una concesión revocada. No se inventan autoridades o identidades nominales. Contrato: [acceso-documental-reservado-v1](../contratos/acceso-documental-reservado-v1.md).

## Pruebas y cortes

- Primera selección H2/MockMvc55:54 aprobados y un fallo HTTP del fixture, que intentaba autenticar por UUID en lugar de nombre de usuario. Dominio13/13, migración18/18 (14 rutas V14/V15.1…V15.13→15.14 y cuatro checksums), Contenido16/16 y RevisiónExcepcional7/7 aprobaron. Se conserva el intento; no se presenta como verde.
- Corregido el login de la prueba, sin alterar autenticación/CSRF. Focal posterior **2/2**: recorrido HTTP y nuevo control contra oráculo documental entre Casos. No hubo bypass ni nueva exclusión.
- **MySQL nativo Windows8.0.46 descartable:32/32**, cero fallos/errores/omisiones: dominio final14 + migraciones18. Incluye FIR→SUB-CONT→cierre EXCEPCIONAL del mismo Caso/REQ, aliases/multirrol, reciprocidad, revocación, suspensión/expiración, retirada/rechazo, manipulación de instrucción, concurrencia/idempotencia, metadatos/contenido/descarga, retiro del equipo y avisos dobles sin envío real. Proceso propio cerrado y ausencia verificada.
- Frontend **137/137**, lint, TypeScript/Vite aprobados. Focal14/14 antes de la regresión completa. DOM con dobles no acredita navegador físico ni backend real.
- C1/c767ab7: CI42273313757 aprobada, Windows535/526 aprobados/9 omitidos; Ubuntu535/511/24; cero fallos/errores,75 raíces JUnit completas/disjuntas y frontend137/lint/build/package. Seis jobs, logs consultados; no se reutilizan518 del padre. C2 requiere su propio HEAD/CI. Presupuesto, aserciones, transacciones, timeouts y exclusiones históricas intactos.

Evidencia privada: campaña `siger-build-4c652bfd9b7a41d5a8e0d659e180202d`, logs `reservas-*`; MySQL `siger-autoridad-mysql-5662a875d9f5491ba6fc0b44eadefca3`/tests.log, Maven03:42. H2/MockMvc, MySQL, DOM y guion manual se distinguen; no se ejecutaron workers/E2E físicos ni envíos reales. No se considera la recepción técnica como revisión humana.

## Padres cerrados y cadena

PR #8 HEADfeb9f01fdf46d5002cae8abb74b36d5c1ee7a06e, [CI42267102474](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42267102474): seis jobs aprobados,71 raíces, Windows506/497/9,Ubuntu506/482/24; frontend129/lint/build/package. Intento cancelado42233659071 conservado: Windows446/cero fallos/9omitidos, faltaron60; Ubuntu sí aprobó. No se reetiquetó ese intento como verde.

PR #9 HEAD5e674363e63a091af7407847dec36501b037bc63, [CI42267451118](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42267451118): seis jobs aprobados,73 raíces, Windows518/509/9,Ubuntu518/494/24; frontend133/lint/build/package. Particiones243+275, MavenWindows15:19/17:31,Ubuntu13:59/14:32. Integral local del mismo corte518/509/9,frontend133,Maven22:20; temporales474683b20d424d7380ce312a29f11bfb. Clases/tests/estáticos compilados antes de iniciar C: esa evidencia no se atribuye al nuevo código. Actas PR #8/#9 actualizadas, ambos borradores.

Rama C `codex/siger-reservas-documentales`, dependiente de #9; matriz138 mantiene IDs/orden y validación funcional06/11/2026. Avanza ACC-01/05/09/10, DOC-01/02/04, SUB-CONT-01/CAS-AUT-02, NOT-01/05/07, AUD-01/03 con límites por regla. No se declaran138 reglas o U1–U9 completas. Main/padres/U4.4/U4.5/SMR/QA/históricos/JAR/Formulario/proveedores intactos; sin merge ni despliegue.

## Guion por roles preparado, no ejecutado

1. En instalación nueva aislada, configurar cuentas sintéticas y registro1.6 firmado con jefatura, Gestión y revisor como personas distintas. Conceder perfiles por U1.5. Crear dos Casos del mismo REQ y dos documentos FIR mediante sus controles legítimos.
2. Dar al revisor consulta explícita del primer Caso. Comprobar que no ve sus FIR ni el Caso hermano. TI y una cuenta alias tampoco reciben esos documentos.
3. Jefatura abre Protección del documento y propone solo metadatos, período finito y fundamento. Antes de aprobación, el revisor sigue sin ver el documento. Gestión consulta solamente la instrucción mínima; no obtiene contenido por ser aprobadora.
4. Gestión aprueba con fundamento. Revisor consulta metadatos; no visor, original, Range o historial de contenido. Revocar y comprobar la denegación con la misma sesión.
5. Nueva propuesta de contenido sin descarga; otra con descarga expresa después de revocar la anterior. Comprobar controles reales de páginas/medios/original según representación. No interpretar una apertura como lectura completada.
6. Repetir con alias, autoridad que acumula función de jefatura, reciprocidad, período vencido/suspendido y revisión obsoleta. Cambiar protección y comprobar que la decisión anterior no abre una nueva FIR.
7. En Caso propio de subrogante pendiente de término, conceder FIR al revisor; solicitar/aprobar SUB-CONT por otra persona acreditada, revisar antecedentes y confirmar manualmente. Se cierra solo ese Caso, conserva REQ/folio/historia y no se resuelve ninguna obligatoria por inferencia.
8. Retirar al revisor del equipo; comprobar documentos, lista y avisos con sesión vigente. Conservar historia/originales y verificar aviso interno/outbox sin ejecutar transporte real.

## Siguiente capacidad

C2 implementa la consulta aislada descrita a continuación. Se continúa con bitácora común, fecha efectiva/registro y rectificaciones inmutables para Caso y REQ sin Caso; después territorio/conciliación y plazos/reportes. Son tareas de implementación y parámetros, no nuevas preguntas institucionales.

## C2: consulta aislada sin expediente

Registro firmado1.7 con autorizantesDocumentoAislado separados de1.6; V15.15 nueva e idéntica H2/MySQL. Modo SOLO_DOCUMENTO con discriminador sellado, ruta/acción nuevas y preservación del DTO/hash/reintentos CASO_DOCUMENTO. La clasificación acreditada ORDINARIO o FIR admite propuesta de jefatura y aprobación independiente de Gestión. Original ciudadano no puede ser FIR: el test mantiene ese control y usa ORDINARIO con facultad1.7. No abre SIN_CLASIFICAR/BLOQUEADO, otro documento, COM, Caso, listas, equipo, bitácora o rutas documentales normales. Las personas canónicas y la cuenta nominal exacta se revalidan; alias no heredan el acceso. Se distinguen la unidad propietaria y la competencia funcional de materia mediante configuración explícita1.7, sin reinterpretar firmas anteriores.

DocumentoReservadoController reutiliza custodia, visor y TransferenciaDocumento. DTO mínimo sin expediente/ruta privada; páginas y metadatos independientes de descarga; original/medios exigen permiso de descarga. Revalida antes de cabeceras y cada64KiB, incluyendo relación actual, política/secuencias, jefatura y Gestión. Revocar durante streaming impide el siguiente bloque. Range/HEAD/integridad/no-store conservados. React conectado a propuesta por ámbito, resolución de destinatario exacto auditada, bandeja con Caso oculto al beneficiario aislado y consulta dedicada. No hay nuevos roles ni servicios operativos.

### Evidencia C2, cortes separados

- H2 inicial2/2. Selección ampliada48:47 aprobadas/1fallo HTTP por código de auditoría superior a32 caracteres. Se corrigió el código, sin ensanchar la columna, omitir auditoría o debilitar aserciones.
- Siguiente selección5: HTTP3/3 y actualización de concesión anterior1/1; un error de fixture de materia sin facultad regional legítima. Se completó el circuito real U1.5 para esa cuenta. Otro intento detectó revisión de protección obsoleta; el fixture consulta la revisión actual. Focal final de materia1/1. Estos intentos fallidos se conservan.
- Cuarto HTTP de original ciudadano1.1: primer fixture rechazado correctamente por fechas distintas entre firma/prueba; segundo rechazado por intentar FIR sobre original ciudadano. Se corrigieron los datos sintéticos y se conservó la prohibición. Recorrido final ciudadano+ordinario2/2: firma Ed25519/recibo/original intactos, ACK pendiente, ingesta sin REQ, nacimiento real por SOL/asunción/CAS y descarga aislada sin COM legible. No se llamó ACK ni Formulario real.
- HTTP usa login/CSRF reales de prueba, archivos sintéticos privados, Range206/HEAD/416, visor PNG y revocación tras65536 de200000 bytes. Antivirus/inspector son dobles: no E2E físico.
- **MySQL nativo Windows8.0.46 descartable47/47**, cero fallos/errores/omisiones; dominio aislado12, compatibilidad de concesión anterior1, dominio C1 14 y migraciones20. Incluye15 rutas V14/V15.1…V15.14→15.15 y cinco comparaciones de DDL. Concesión creada realmente en15.14 conserva campos, hash, comando y firma al actualizar. Maven08:12; instancia privada70be9ec7705d49e8aa320e3f4c2a73e7 cerrada, ausencia de proceso propio verificada.
- **Frontend146/146**, lint, TypeScript/Vite aprobados. Primer DOM22 tuvo21 aprobados/1consulta accesible de fixture demasiado exacta; corregida la consulta,22/22 y luego regresión completa. No se presenta DOM como navegador físico.
- C2/2d98e2122f7bfdf08e102eaf84d7aa496a703371: [CI42283881844](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42283881844) seis jobs aprobados y logs consultados; Windows554/545 aprobados/9omitidos yUbuntu554/530/24,cero fallos/errores,78raíces completas/disjuntas. Frontend146/lint/build/package; particiones247+307,MavenWin11:34/26:40,Ubuntu09:21/23:47. No se reutiliza535/137 del C1. PR #10 sigue en borrador y su rama permanece intacta al continuar en hija bitácora.

Logs privados documento-aislado-* en la campaña ya identificada; native tests.log bajo siger-autoridad-mysql-70be9ec7705d49e8aa320e3f4c2a73e7. No contienen evidencia operativa nueva ni se publican secretos. V15.14 permanece intacta; V15.15 añade únicamente modo/default/CHECK. Guion de aceptación preparado, todavía no ejecutado.

### Extensión del guion manual por roles

1. Configurar registro1.7 sintético con jefatura, Gestión y beneficiario de personas distintas y facultad exacta, concediendo perfiles por U1.5. Beneficiario sin lectura del Caso, alias sin concesión propia.
2. Proteger un documento interno FIR y otro ORDINARIO. Desde Protección elegir Solo documento, resolver usuario exacto, indicar fechas/permisos/fundamento y proponer. Antes de aprobar no hay enlace utilizable. Gestión recibe instrucción mínima y no contenido.
3. Aprobar de otra persona. Beneficiario consulta únicamente el documento desde Accesos reservados; no se expone Caso ni COM. Intentar Caso/hermano/ruta documental normal y cuenta alias: denegados. Probar metadatos sin contenido, contenido sin descarga y descarga expresa como decisiones separadas.
4. Revocar/suspender/expirar y comprobar con la misma sesión. Cambiar área/materia/política o relación documental: permiso anterior deja de servir. Comprobar historial y notificaciones mínimas sin transportar correo real.
5. En instalación nueva15.14 crear permiso Caso+FIR y conservar sus huellas; actualizar a15.15. Reintento antiguo y acceso previo conservan su semántica; la nueva ruta aislada no acepta esa concesión.
