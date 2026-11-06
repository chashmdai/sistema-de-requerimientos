# SIGER · continuidad institucional U2 · 06/11/2026 UTC

## Base y corte

Parte del PR #4 aprobado en `6ae296e00be861319e388065af8cb3ecc67e53b1`. Rama nueva `feature/siger-u2-continuidad-institucional`, en `C:\req\worktrees\siger-logica`, dependiente de `feature/siger-u2-nucleo-institucional`. PR en borrador; sin merge, auto-merge o despliegue. U0–U1.6 y el núcleo inicial COM/SOL/REQ/CAS no se reconstruyeron.

Se verificaron árbol limpio, HEAD/upstream 0/0, referencias y migraciones antes de modificar. Rector local íntegro SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`; lectura focalizada §§09–11, 14, 18 y 21, matriz de 138 reglas, contrato/informe U2, AGENTS y traspasos vigentes. El documento privado no se incorporó a Git.

## Capacidades implementadas y conectadas

| Capacidad | Comportamiento efectivo | Evidencia principal |
| --- | --- | --- |
| Transferencia principal REQ | Origen solicita, destino acepta/rechaza, origen retira antes de aceptación. Conserva REQ/COM/folio, Casos, responsables y tareas; el origen sigue principal hasta aceptación. Nuevo principal tiene coordinación mínima y no contenido universal de Casos. | `ContinuidadU2Test`, `ContinuidadAreasTest`, `ContinuidadHttpTest`; interfaz REQ/bandeja. |
| Derivación CAS | Solo jefatura del Caso solicita; destino recibe paquete limitado y decide. Aceptación crea/asigna sucesor en el mismo REQ, sin cerrar origen, copiar bitácora, documentos, equipo o tareas ni transferir principal. | Pruebas de origen trabajando durante evaluación, rechazo/retiro sin sucesor, reintento sin duplicado y fallo de auditoría con rollback. |
| Cierre explícito de origen | Responsable solicita término por derivación después de aceptación. Revisión independiente de jefatura confirma; modalidad vinculada a esa revisión. Sucesor permanece abierto. | Circuito de cierre existente reutilizado; `institution_case_closure_request` y cierres por revisión. |
| Reapertura ordinaria | Responsable solicita con fundamento y antecedentes/error existente; jefatura decide, preservando mismo Caso y cierre anterior. Cada aprobación abre nuevo período. Rechazo no exige inventar una COM nueva para revisión posterior. | Rechazo, nueva solicitud sobre mismos antecedentes, aprobación/reintento, historial y período. |
| REQ terminado | Principal autoriza expresamente reactivación vinculada a solicitud/revisión. Autorizar no abre nada; aprobar reapertura reactiva REQ y Caso atómicamente. Principal ajeno al Caso no gana su gestión. | Dos autoridades en ámbitos diferentes; rollback conjunto de REQ/Caso/período ante fallo de auditoría. |
| Acuse por asignación | Responsabilidad inmediata; actuaciones permitidas antes del acuse. Acuse manual ligado al UUID de asignación actual. Otra asignación deja nuevo acuse pendiente y conserva anterior. | Lectura/aviso no acusa; jefatura no acusa por el profesional; asignación anterior obsoleta. |
| Atención sin Caso tras transferencia | Persona y ámbito de atención conservados explícitamente al transferir principal. Puede continuar sin Caso mientras tenga facultad en su ámbito original; no gana permisos de destino. | Detalle/listados/búsqueda/escritura y revocación aun con otro rol en destino; UI diferencia principal y atención. |
| Avisos internos | Proyección recuperable existente para solicitudes/decisiones de continuidad, revalidación por recurso y enlace a bandeja. Títulos sin expediente ni resumen sensible. | Notificaciones autorizadas y negativas TI; ninguna categoría nueva habilita Graph. |
| Pertinencia tras transferencia | Principal actual conserva facultad contextual; la historia cita `TRANSFERENCIA_U2` y autoridad de aceptación. Se conserva autoridad/origen del nacimiento del REQ y mandatos firmados anteriores. | Prueba específica de COM hija y principal transferido; sin reinterpretar registro firmado 1.3. |

El paquete contiene identificadores, motivo, materia informada, resumen expresamente escrito y objetivo cuando corresponde. No adjunta documentos: su consulta posterior continúa exigiendo pertinencia/publicación/consentimiento y autorización independiente del módulo documental. Compartir raíz o conocer un UUID no concede acceso. Los listados nuevos se filtran en SQL por ámbito actual, sin herencia unidad→área, y se acotan a 100 entradas; su historia expone únicamente decisiones del paquete autorizado.

## Diseño, seguridad y concurrencia

`ContinuidadU2` separa estas transiciones del servicio inicial SOL/REQ. Reutiliza `core.execute`, cerrojo de identidad/COM, ámbito acreditado, apertura/asignación y revisión de cierre existentes. No crea otro servicio desplegable. API `/nucleo/institucional/continuidad` con sesión/CSRF, UUID de idempotencia y revisiones. API, persistencia y React están conectados.

La aceptación revalida facultades del solicitante y destino, área, materia/responsable/contexto de origen, estado y persona canónica. Una cuenta alias no es aprobación independiente. Casos antiguos no se convierten en institucionales por usar una ruta nueva. Las rutas históricas incompatibles siguen cerradas para agregados U2; la decisión histórica de reapertura no puede aprobar una solicitud institucional. Los reintentos confirmados conservan el resultado previo sin volver a ejecutar trabajo.

La solicitud pendiente no cambia responsabilidad ni estado del Caso. Aceptar/retirar compiten por una única revisión bajo la misma transacción. Fallos en auditoría revierten sucesor/asignación/decisión o reactivación/período completos. Cierre global no admite Casos abiertos ni decisiones pendientes de colaboración/transferencia/derivación/reapertura. No se agregaron exclusiones ni cambios a los límites funcionales de consulta/transacción.

## Persistencia y compatibilidad

**V15.8 y V15.9 H2/MySQL**, aditivas e idénticas entre motores. Añaden ocho tablas: solicitudes REQ/CAS, ámbito de atención conservado por transferencia, respaldo reapertura, cierres y modalidad solicitada por revisión, asignación actual y acuse. No modifica V1–V15.7, no habilita `outOfOrder`, no backfill ni semillas nominales. La transferencia conserva en su solicitud la autoridad de aceptación y destino aceptado; no sobrescribe respaldo de nacimiento.

SHA-256 H2/MySQL V15.8: `aa16ee086ef472c7b50074c6764cf9c6e29f35abd272bc7899e1f31b689d16fe`; V15.9: `dc5440ce12cec5a98bfc2c5dadc72f963a06fc61165fdd4a7f61a6e2e26d41e7`. V15.8 permanece idéntica a `230d167`; la conservación de atención se añadió mediante V15.9, después de comprobar nuevamente las referencias concurrentes.

Actualizaciones verificadas desde V14/V15.1…V15.8 y prueba específica V15.7→V15.9 con folio, REQ/Caso cerrado, destino y asignación conservados, incluida segunda ejecución sin cambios. No se infieren principal, acuses o cierres nuevos en historia previa. Las asignaciones U2 anteriores sin puntero actual requieren un acto humano de confirmación, conservando las asignaciones existentes; nunca se presume un acuse.

Main y referencia conocida U4.5 permanecían en G0 al elegir V15.8; no había referencia remota U4.5 publicada observada. No se leyó ni modificó su worktree. Antes de integrar, comprobar cambios locales no publicados y posibles colisiones V15.8/V15.9, DTO, autorización y núcleo. Esta rama requiere toda la cadena PR #2→#3→#4; no es compatible con ejecutar un binario anterior que ignore sus predicados.

## Validaciones y evidencia

El corte de código inicial y el ajuste final se distinguen explícitamente; no se atribuyen al último commit los resultados de una versión anterior.

- Primer focal H2: 9/9. Selección posterior: 41 ejecutadas, cero fallos y un error de preparación al intentar firmar un área ya vencida; el registro rechazó esa configuración. Se corrigió usando reloj controlado y expiración real de concesión, sin omitir la aserción. Selección siguiente 17/17 y ampliada 19/19, con solapamiento; no se suman como pruebas únicas.
- Regresión integral local de `230d167`: **451 backend, 442 aprobadas, nueve omitidas, cero fallos/errores**, frontend 107/107, lint/build/empaquetado correctos. Maven 15:10. Esta ejecución terminó con las clases de ese corte; no se presenta como integral local del ajuste posterior.
- Ajuste final `f33478e`: **H2/MockMvc 51/51**, cero omisiones/fallos/errores, Maven 03:09; incluye núcleo inicial, autoridad, áreas, HTTP, transferencias, reapertura, atención conservada, revocación y migraciones.
- Frontend final: **109/109**, lint y build correctos; diez pruebas nuevas. Incluye retirada de controles/contenido tras perder acceso y ámbito de atención separado. DOM no es navegador físico. Empaquetado final correcto; esa última orden de package no repitió tests y no se cuenta como ejecución adicional.
- Validación final MySQL nativo Windows 8.0.46 descartable: **54/54**, cero fallos/errores/omisiones; nueve rutas V14/V15.1…V15.8→V15.9, conservación histórica, procedencia de principal y atención independiente. Proceso propio cerrado y ausencia verificada.
- Corte intermedio MySQL V15.8: 51/51 antes del ajuste de atención V15.9; preservado sin sustituir la validación final.
- Primera validación MySQL nativo Windows 8.0.46 descartable: 50/50, cero fallos/errores/omisiones. Corte previo al ajuste final de respaldo de transferencia; no se usa como sustituto de la nueva validación final.
- Bases nuevas por ensayo, esquemas UUID y puertos loopback propios, `--no-defaults`, credenciales/ACL privadas y cierre del proceso propio. No se reutilizan bases/credenciales operacionales ni se toca MySQL80 global, firewall o servicios.
- H2/MockMvc verifica dominio, JDBC, sesiones/CSRF y API; MySQL acredita ese motor descartable. No son E2E físicos. AV/renderer reales y proveedores no se ejecutaron; dobles/omisiones heredados se identifican en los resultados integrales.

Los logs intermedios y finales se conservan fuera de Git en carpeta privada de esta campaña. **La evidencia CI definitiva y sus enlaces están en el acta de publicación del PR dependiente de PR #4**: identifica el HEAD y ambos jobs de Windows/Ubuntu comprobados al cerrar. Se exige éxito de ambos en ese HEAD; no se reutiliza el éxito del padre. Este informe no genera un commit adicional solo para autocitar su SHA. Se conservan las exclusiones stress y omisiones heredadas; no se alteraron consultas, transacciones, aserciones, dependencias o presupuesto CI de 30 minutos.

## Git, publicación y archivos

Commits de código: `230d1677cc01e2900d0dd496760c0476bf248834` (circuitos, V15.8, pruebas, contrato y guion) y `f33478e65252b2e9f61985f0bd4bb6ae0d826113` (atención preservada, V15.9, revocación y retirada de controles obsoletos). El commit documental siguiente consolida este informe y las 17 filas. Publicación mediante push normal, upstream de la misma rama y PR en borrador con base en `feature/siger-u2-nucleo-institucional`. El acta final del PR registra HEAD, árbol limpio, sincronización y CI; PR #2/#3/#4 y main se conservan.

## Archivos y trazabilidad

Backend: `ContinuidadU2`, controlador, ganchos acotados en `NucleoInstitucional`/`NucleoU2`, procedencia actual de principal en `ContenidoInstitucional` y proyección/autorización de avisos. Frontend: `ContinuidadU2.tsx` conectado con bandeja, REQ y Caso; mensajes de errores y pruebas. Persistencia: V15.8/V15.9 en dos motores. Pruebas: cuatro clases nuevas de dominio/áreas/HTTP/migración, ruta adicional de actualización y limpieza referencial de fixtures. Auxiliar MySQL extendido sin perder comprobación de puertos/propiedad/credenciales/cierre.

Contrato: [continuidad institucional](../contratos/continuidad-institucional-u2-v1.md). [Guion manual por roles](GUION_ACEPTACION_U2_CONTINUIDAD_2026-11-06.md), con cuentas/catálogos sintéticos, preparación mediante U1.5, tres recorridos principales, variantes negativas y registro de resultados. Preparado para uso posterior: no acredita una sesión física ejecutada.

La matriz conserva sus 138 IDs y orden. Se actualizaron 17 filas afectadas con pruebas y límites acreditados; no se declara la totalidad de U2/U3/U4/U6 terminada por este bloque transversal de continuidad.

## Pendientes, riesgos y próximo bloque

1. **Equipo local y TAR-09 completos pendientes técnicos.** V12/V13 impone creador=ejecutor en tareas. No se elimina la restricción ni se cambia creador para aparentar traspaso. Reasignar un Caso U2 con tareas no terminales queda cerrado 409; confirmar al mismo responsable no las mueve. Este control es una prevención, no TAR-09 implementado. Evolucionar agenda, integrantes, obligatoriedad, recordatorios y traspaso transaccional en un bloque vertical siguiente.
2. Cobertura acreditada domicilio→sede y conciliación explícita de REQ/CAS históricos: sin decisiones reales no se asignan principales o territorios por inferencia. Se conserva el bloqueo existente; este bloque no entrega un conciliador ni catálogo territorial nuevos.
3. Escalamiento de desacuerdos, FIR de actuaciones, reservas extraordinarias, autocierre regional titular y SUB-CONT-01 requieren circuitos/facultades específicos; permanecen cerrados. FIR documental existente conserva sus controles.
4. Valoración estructurada de COM hijas en REQ sin Caso, captura especializada, agenda/equipo y avisos específicos SOL/REQ aún parciales. Los avisos de continuidad no acreditan entrega de correo ni acuse automático.
5. Listas propias acotadas a 100: paginación específica de decisiones/paquetes pendiente. Sin acceso global por TI, Dirección o principal de REQ; el historial previo no se reescribe para facilitar nuevas rutas.

QA, U4.4/U4.5, COM-000001, bases preservadas, JAR activos, servicios/consumidor, Formulario Público, Graph/CU/PISEE y HTTP_503 WSL no intervenidos. Transporte, originales/recibos, hashes, firmas Ed25519 y COMMIT→ACK conservados. Sin E2E físico, merge, despliegue o habilitación productiva.

## Preguntas institucionales para habilitación real

1. ¿Qué catálogo y respaldo acreditarán domicilio→sede y sus vigencias?
2. ¿Quién acreditará principal/área/materia de cada registro histórico mediante conciliación explícita?
3. ¿Qué autoridades/delegaciones funcionales se designarán para escalamiento, reservas y continuidad excepcional? Hasta acreditarlas, esas operaciones permanecen cerradas.

No hay una decisión institucional nueva indispensable para ejecutar los tres circuitos ordinarios entregados con identidades y mandatos sintéticos válidos.
