# Continuidad institucional U2 · contrato 1

Extensión del núcleo U2 de PR #4, conforme al rector de 28/10/2026, §§09–11, 18 y 21. No modifica los contratos Formulario–SIGER 1.0/1.1 ni reinterpreta mandatos firmados 1.0–1.3.

## Comandos y autorización

Prefijo `/nucleo/institucional/continuidad`. Sesión individual actual, CSRF para mutaciones y `Idempotency-Key` UUID. Las revisiones son obligatorias. La identidad, la COM raíz, los cambios y la auditoría se verifican en una transacción; los reintentos confirmados conservan el resultado sin duplicar trabajo. Un rol de sede/unidad no cubre áreas acreditadas; lectura por materia y TI no son gestión.

| Ruta POST | Actor y resultado |
| --- | --- |
| `/requerimientos/{id}/transferir` | Jefatura principal vigente solicita; REQ activo, destino explícito, fundamento y resumen limitado. Solo una pendiente por REQ. |
| `/transferencias/{id}/decidir` | Destino acepta/rechaza; origen puede retirar mientras pendiente. Aceptar exige otra persona acreditada y que la facultad de origen siga vigente. Cambia principal del mismo REQ, sin modificar Casos, equipos, profesionales, tareas, complejidad, folio u original. |
| `/casos/{id}/derivar` | Jefatura del Caso abierto solicita. Destino acreditado obligatorio, con área cuando corresponde; motivo, resumen y objetivo del sucesor. Una pendiente por origen. |
| `/derivaciones/{id}/decidir` | Destino acepta/rechaza; origen retira antes de aceptación. Aceptar crea y asigna un sucesor dentro del mismo REQ de forma atómica. No cierra origen ni transfiere principal/equipo/documentos/tareas. Revalida persona, facultad de origen, área, materia y responsable. El origen puede registrar actuaciones mientras se evalúa. |
| `/casos/{id}/termino-por-derivacion` | Responsable solicita cierre después de una aceptación. Jefatura independiente confirma por el circuito de término existente. La modalidad se vincula a esa revisión, sin convertir aceptación en cierre. |
| `/casos/{id}/reaperturas` | Responsable competente solicita reapertura fundada de Caso cerrado ordinariamente. Antecedentes o error documentado; no se exige una COM nueva ni un antecedente nunca usado. No procede para cierre DERIVACION. |
| `/reaperturas/{id}/reactivacion-req` | Si el REQ terminó, su principal autoriza expresamente reactivarlo para esta solicitud y revisión. Sigue terminado hasta aprobar la reapertura. No otorga gestión del Caso al principal de otra unidad/área. |
| `/reaperturas/{id}/decidir` | Jefatura del Caso acepta/rechaza. Aceptar exige persona distinta del solicitante, cierre y asignación no obsoletos y facultades vigentes. Si el REQ terminó, requiere autorización principal vigente y coincidente; reactivación y nuevo período del mismo Caso son atómicos. |
| `/casos/{id}/acuse` | Responsable acusa su asignación concreta vigente. Responsabilidad inmediata; abrir ficha, aviso, correo o registrar actuación no acusa. Una reasignación crea otro acuse pendiente y conserva el anterior. |

Decisiones terminales no se convierten retrospectivamente en retiradas. Rechazos y retiradas conservan historia. Una solicitud obsoleta puede rechazarse/retirarse y sustituirse mediante nueva decisión; no se borran filas. No se acreditan excepciones de autocierre, árbitros universales o facultades nominales.

## Lectura y documentos

`GET` del prefijo devuelve paquetes de evaluación del ámbito de origen/destino, con historia de decisiones y bandera `puedeDecidirDestino`. Incluyen identificadores, motivo, materia informada y resumen expresamente escrito; no actuaciones, FIR, originales ni documentos automáticos. Cada lectura revalida ámbito actual, incluso para avisos y conteos. Conocer un UUID o compartir REQ no autoriza el recurso.

`GET /requerimientos/{id}` requiere principal actual y devuelve transferencias y solicitudes mínimas que necesitan reactivación. `GET /casos/{id}` requiere gestión efectiva de ese Caso y devuelve derivaciones, reaperturas, asignación/acuse y cierres conservados; consulta por materia no habilita este circuito. El nuevo principal mantiene los estados mínimos de coordinación del núcleo U2 y debe acreditar por separado acceso al contenido de cada Caso/documento.

Los avisos internos usan la proyección recuperable existente; no contienen resumen sensible ni envían nuevo correo Graph. Leer un aviso no acusa asignación. Las categorías nuevas quedan fuera de la lista de correo. Los avisos revalidan permisos al consultar y enlazan a la bandeja institucional.

## Persistencia y compatibilidad

V15.8 añade solicitudes de transferencia y derivación, respaldo de reapertura/reactivación, cierres por revisión y acuses por asignación. Sin cambios de V1–V15.7, backfill, borrado, `outOfOrder` ni semillas nominales. Los datos de cierre previos permanecen; cada cierre nuevo U2 registra su instantánea. Una reapertura crea `core_management_period` reutilizando `core_reopen` como decisión, diferenciada por `institution_reopen`.

Asignaciones U2 previas a V15.8 no reciben acuses ni una asignación vigente inferida. La jefatura puede confirmar explícitamente la asignación mediante la operación existente; el acuse nuevo se asocia al acto nuevo. Los flujos históricos se conservan para evidencia; sus rutas incompatibles no ejecutan derivación o reapertura sobre nuevos agregados U2.

## Corte pendiente

TAR-09 y equipo operativo completo necesitan evolucionar la agenda personal V12/V13, que exige creador=ejecutor. No se simula el traspaso. En Casos U2, cambiar responsable con tareas no terminales queda 409 `TAR09_REQUIERE_EVOLUCION_AGENDA`; confirmar al mismo responsable no mueve tareas. Transferir principal, aceptar derivación y reabrir tampoco las mueve. Tareas y actuaciones continúan independientes.

Catálogo domicilio→sede y conciliación histórica siguen pendientes; no se usan coberturas técnicas ni el primer Caso para acreditarlos. Reservas extraordinarias, FIR de actuaciones y excepciones regionales no se habilitan por este contrato. La prueba automática no autoriza ejecutar este circuito sobre QA preservada ni demuestra E2E físico.
