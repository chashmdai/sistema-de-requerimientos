# SIGER · U1.6b.1 · Materias y consulta regional de Caso

Continuidad exclusiva de `c451f4464b8a2d8ec450e8d295e1880db6d36c43`, rama `feature/siger-logica-negocio`, worktree `C:\req\worktrees\siger-logica`. Entrada limpia y upstream coincidente; no se repitieron U0–U1.6a. Rector local SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`, íntegro; no se agrega a Git. Se contrastaron §§04/05/09/12/18/25/26/27 y contratos de autoridad, núcleo, documentos y notificaciones.

## Resultado y límites del corte

Implementación vertical de **catálogo firmado de materias/sedes regionales → solicitud/aprobación/ejecución independiente de consulta → clasificación regional fundada → ficha/listado/búsqueda de Caso en solo lectura → retirada por rectificación/revocación/suspensión/expiración**. Backend, persistencia y UI están conectados, sin perfiles nominales inventados.

**U1.6b y U1 permanecen parciales.** Este primer corte no entrega el expediente completo ni resuelve pertinencia de COM hijas/documentos en REQ mixto. La vía nueva no permite originales, documentos, antecedentes, reaperturas, tareas, exportación o Casos hermanos. No se amplían esas rutas para aparentar lectura completa; ORG-07/12 se acreditan solo parcialmente. FIR, reservas reforzadas y excepción conjunta ACC-10 siguen sin habilitarse por concesión ordinaria. La activación nominal real requiere el canal institucional privado y datos PE-01/02/03/04.

## Fuente y decisiones de ingeniería

- ORG-05/07 y §05.3 permiten consulta regional de Direcciones UPR/UPD por materia, no a todos sus profesionales ni gestión por esa sola consulta. Se representa como rol DIRECTOR, ámbito MATERIA, competencia funcional exacta y sede regional explícita, con el circuito U1.5. No depende del texto del cargo, perfil de unidad o domicilio ciudadano.
- Materia, unidad gestora, sede, área y complejidad son dimensiones distintas. La materia tiene competencia UPR/UPD inmutable por ID. Cambiar clasificación no mueve el Caso, REQ, área, responsable ni tareas. Histórico sin materia sigue sin nueva vía de lectura; no se clasifica automáticamente con unidad/área o datos ciudadanos.
- Sede regional no se infiere del nombre o de una concesión de coordinación: necesita acreditación firmada. Catálogos finitos y ACTIVA/SUSPENDIDA, con revisiones/sobres íntegros conservados; sin semillas reales o editor libre TI. Omitir entradas no extiende sus fechas ni equivale a borrarlas.
- Registro de autoridad **1.2** explícito: conserva campos 1.1 y agrega `materias`, `sedesRegionales`, `ambitosMateria`. Lector estricto, firma sobre bytes exactos con versión incluida; 1.0/1.1 mantienen su interpretación y rechazan MATERIA. No cambia transporte público Formulario 1.0/1.1 ni se reutilizan sus claves. Una competencia no cambia silenciosamente de UPR a UPD: se requiere otro ID.
- Concesión de consulta utiliza ancla V11 inerte con unidad/sede NULL, vínculo MATERIA e instrucción ejecutada coherente. La política reconstruye el ámbito exacto sin convertirlo en jefatura. Los predicados SQL/UI aceptan explícitamente los ámbitos de gestión anteriores; no interpretan un ámbito nuevo como permiso general de unidad.
- Se conserva separación de personas/cuentas, aprobación independiente, control de credencial del aprobador, ciclos e idempotencia U1.5. Digests de solicitudes anteriores 1.0 y 1.1 conservados mediante representación compatible; no se reinterpretan comandos históricos al añadir `materia`.
- ORG-13/§12.3 habilitan rectificación regional fundada. En este corte exige coordinación regional vigente y jefatura competente del Caso, simultáneas en el contexto real; una coordinación de sede no hereda áreas. Es una restricción conservadora explícita, no un nuevo poder inferido. La UI usa `puedeRectificar` calculado por servidor.
- **Acreditación ordinaria cerrada por defecto.** La clasificación incluye decisión expresa `consultaOrdinaria` del actor competente. Ante la falta de modelo integral de reservas/pertinencia, solo acredita la gestión ordinaria del Caso; no clasifica documentos ni habilita FIR. Sin esa decisión o sin catálogo/sede vigentes, la consulta por materia no funciona. Derechos independientes anteriores se conservan, sin ratificar normativamente toda la implementación histórica.
- Cambiar área conserva materia pero suspende consulta ordinaria y deja constancia; se requiere nueva acreditación en destino. La revisión y guarda compartida evitan trasladar una habilitación del contexto anterior o aprobarla con una versión obsoleta. No crea nuevo Caso, no mueve tareas ni modifica original.
- Clasificación, revisión, historial, evento e idempotencia confirman juntos, bajo `identity_guard` y bloqueo de COM. Fallo de auditoría revierte; carrera produce un ganador. Cambio de materia retira la vía anterior inmediatamente salvo derecho independiente; revocación usa U1.5 y suspension/fechas del catálogo invalidan epochs. Mandatos solo de revocación pueden retirar permisos en materia/sede suspendidas sin aprobar otros nuevos.

## Lectura, auditoría y vías indirectas

La ficha nueva devuelve `materia` y `soloLectura:true` si la única vía es la concesión MATERIA. Muestra actuaciones, decisiones y responsabilidad ordinarias; la UI no invoca documentos/reaperturas/antecedentes ni presenta botones de gestión. Si existe derecho independiente real, conserva la vista y operaciones que ese derecho habilita.

Listado y búsqueda de Casos por folio/ID, filtro de estado, paginación y total comparten el mismo predicado de materia/sede/acreditación ordinaria. No se amplía la búsqueda de Comunicaciones o del REQ. Originales y documentos se deniegan en su adaptador y servicios actuales; conocer IDs de COM/Caso no sustituye la autorización. La historia de otras materias queda solo para el gestor competente, no se entrega al consultor de materia actual.

Se reutiliza la autorización de tareas, antecedentes, vínculos y derivaciones, manteniendo sus filtros propios. MATERIA no participa en jefatura SQL, ni en históricos sin área. Un multirrol con consulta UPR y jefatura de área B no gestiona área A. Alias de la misma persona no pueden aprobarse la concesión.

La auditoría nueva `matter_access_event` registra consultas de ficha/listado/contexto separadas de actuaciones y administración, sin relato ni comprensión ficticia. La ficha no se entrega si falla esa auditoría. Es parcial respecto de AUD-04: no se declara auditada toda operación/denegación/exportación histórica. Las comprobaciones internas de permisos de notificaciones no simulan aperturas de expediente.

Rectificación genera aviso interno `MATERIA_ACTUALIZADA` con recurso `CASO_CONSULTA`; proyección/listado revalidan acceso actual. La antigua Dirección que perdió acceso no ve el aviso por conocer su ID o por pertenecer al mismo REQ. No se activa correo para esa categoría, ni se llama a Graph real. El aviso es una referencia, no un permiso para tarea o gestión.

## Persistencia y compatibilidad

Nueva **V15.4**, H2/MySQL iguales por bytes: ocho tablas de catálogo, sede regional, ámbitos de mandato/instrucción/asignación, materia actual/historia y auditoría de consulta; índice de ámbito. Sin ALTER/destrucción de tablas históricas ni semillas nominales. No se modifica V1–V15.3 ni se activa outOfOrder.

Antes de elegir versión se verificaron `origin/main` y referencia local `feature/siger-u45-demostracion-manual`, ambas G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`; no había referencia remota U4.5 publicada. Esto no acredita cambios aún no publicados de otro worktree. No se inspeccionó o modificó su directorio/procesos. Deben coordinarse versiones no publicadas antes de integración.

Se comprueban actualizaciones desde V14, V15.1, V15.2 y V15.3, preservando registros y concesiones anteriores sin fabricar aprobaciones retroactivas. El auxiliar MySQL crea esquemas nuevos `_153` y `_materia`, con guardas propias, loopback efímero, datadir/credenciales nuevos y comprobación de propiedad de listener. No usa ROBUST_DB ni bases preservadas. No se acredita downgrade a JAR que ignore materias/áreas o la nueva autorización.

Transporte Formulario–SIGER, originales/folios, firmas Ed25519, hashes/procedencia y COMMIT→ACK conservados. Ningún código de integración pública cambia. U5 responder/canalizar/escalar sigue 409; no se inicia U2 sobre este corte.

## Componentes, matriz y pruebas

Backend: nuevos `CatalogoMaterias`, `PoliticaMaterias`; extensiones de autoridad/concesiones/identidad, DTO y núcleo/controller, adaptador documental y notificaciones. Frontend: `MateriaCaso`/vista de consulta, selectores de concesión 1.2, búsqueda por folio/ID y tipos API. Pruebas nuevas de dominio, HTTP, migración y DOM; fixtures usan firma sintética y solicitud/aprobación/ejecución reales.

La matriz mantiene exactamente **138 IDs** y su orden. Se actualizan 16 filas: ACC-01/02/03/05/06, DIS-01, ORG-01/05/07/08/09/12/13, AUD-01/02/04. Todas conservan estado parcial con límites concretos; no se suman catálogos sintéticos como habilitaciones reales ni se declara implementada pertinencia completa.

Primera selección: tres fallos de conteos revelaron que un filtro heredado interpretaba cualquier ámbito no AREA como unidad/sede. Se corrigió el predicado backend y UI a lista explícita; no se cambiaron las expectativas de cero recursos ajenos. Después: nueve tests de materia y 23 de áreas correctos. HTTP autenticado/CSRF y rutas de migración focalizadas pasaron; frontend 83/83, lint/build correctos. Se conservan pruebas de suspensión/revocación, alias, multirrol, caducidad, cambio de ámbito, idempotencia 1.1, fallo de auditoría y concurrencia.

MySQL nativo descartable inicial: nueve pruebas sin omisiones, cuatro rutas de actualización, catálogo, concesión, consultas, conteos, rectificación/revocación e historia; sin permisos operacionales acreditados. H2/MockMvc/DOM/runners CI no son E2E físicos.

**Regresión completa local: Build-Siger.ps1 correcto el 05/11/2026 a las 02:06 America/Santiago, Java25/Node24 explícitos. Backend H2: 365 casos, 356 aprobados, nueve omitidos, cero fallos/errores; frontend 83/83, lint/build y empaquetado correctos.** Incluye 12 pruebas de dominio de materias y toda la regresión de áreas, identidad, núcleo, transporte, documentos, tareas/notificaciones y gates. Los resultados no se presentan como calificación de workers reales.

Después se reforzó el ensayo de migración con cambio de área/suspensión de consulta e historial, se eliminó un envoltorio privado sin cambio de política y se ejecutó la selección final **15/15 H2/MockMvc**, cero omisiones: 12 dominio, un HTTP autenticado y dos de migración. Esos resultados se conservan separados de la corrida completa; el checkout publicado se vuelve a verificar íntegramente en CI Windows/Ubuntu.

**MySQL 8.0.46 nativo Windows final: 9/9**, cero omisiones/fallos/errores, después de las comprobaciones reforzadas, instancia `siger-autoridad-mysql-4d33370d1db44731af19908f7c19bb37`. Procesos propios y listener residual: cero. Cuatro rutas de actualización, equivalencia SQL, concesión/revocación concurrente, áreas y materia, listado/bandeja, rectificación, cambio de área que suspende consulta e historial y revocación de la facultad MATERIA. Solo bases nuevas y descartables; no toda la batería H2 sobre MySQL ni cuentas operacionales.

Los 28 hashes V1–V14 coinciden con el baseline; V15.1/15.2/15.3 intactas. V15.4 H2/MySQL: SHA-256 `731b73c65988ff009e4f455687496a9addbaae8521e04d7d071cebb98e2efa38` en este checkout Windows. Logs privados: `build-final.log` `91989bd4ff7b3c5e192f63642ad5528e97158e482781e2c974344f35b20d3c18`; `focal-cierre.log` `569ae6a8ba26f3bca065c4c15695fb044f79240b4f18f2996ec0d66e1c27938d`; `mysql-cierre.log` `d49aa9073baffa2ca22c255c89ef503ea06ca47f59c18e43563c4ff6fd406021`. XML de la corrida H2 completa y de la selección final conservados por separado antes de la batería MySQL.

Logs privados bajo LOCALAPPDATA `siger-logica-u16b-d2bc96bc8406440dbeb0f20cfe23cf10`; instancias MySQL `siger-autoridad-mysql-<UUID>`. Nada de esa configuración, sobres o logs completos se añade a Git. Dependencias/POM/exclusiones CI no se modifican; omisiones opt-in y tres stress históricos se conservan. Vulnerabilidades npm moderadas heredadas, sin actualización masiva.

## Corte Git y siguientes incrementos

PR #2 permanece en borrador. Antes de añadir este corte tenía 68 archivos, 2689 adiciones y 14 commits; se mantiene U1 y sus dependencias inmediatas, con funcionalidades nuevas acotadas. No se acumulan U2–U9. Antes de ampliar pertinencia documental integral conviene revisar este corte de seguridad y organizar el siguiente PR dependiente si procede, sin merge prematuro.

Código, UI, pruebas, migraciones y auxiliar confirmados en **`6cbafb14246b769b3644d764b20e0261515f23aa`**, `feat(logica): acreditar materias y consulta regional de Casos sin gestion`. El commit documental posterior incluye informe, contrato, índice y matriz; no modifica código ya verificado. Su SHA se obtiene con `git log -1 --format=%H -- docs/LOGICA_U1_6B_MATERIAS_2026-11-05.md`, evitando autorreferencia. Push normal sobre la misma rama y upstream. Los checks/cuerpo del PR y el comprobante privado registran el HEAD definitivo y CI Windows/Ubuntu después de publicar; no se atribuye a este nuevo SHA la CI anterior de U1.6a.

Siguiente: pertinencia explícita COM hija/REQ/materia y publicación/protección por documento, conservando reservas y diferenciales por contenido. Ese modelo necesita su incremento vertical y no puede sustituirse por una autorización sobre todo REQ. Completar fuente maestra/provisión/recuperación institucional y autoridad de cuentas sigue siendo dependencia PE-02; identidad local de laboratorio no se convierte en SSO/MFA.

QA, históricos/U4, COM-000001, originales/ACK, JAR activos, servicios, U4.5, Formulario y proveedores reales fuera de las operaciones efectuadas. Sin merge, auto-merge, despliegue o corrección de HTTP_503 WSL.

## Preguntas institucionales pendientes

1. Para activar datos reales: acreditar por canal privado catálogo/submaterias y correspondencias UPR/UPD, sedes regionales, autoridades/vínculos y respaldos vigentes. No se inventan nominales para desarrollar.
2. Para el siguiente alcance de lectura de documentos/COM: acreditar las habilitaciones de reserva/protección pendientes PE-03 y datos de catálogo PE-04. La regla de pertinencia por materia ya está confirmada; no se pide reconsiderarla.
