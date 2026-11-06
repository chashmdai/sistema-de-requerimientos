# Bitácora institucional común · 06/11/2026

## Capacidad desarrollada

Una actuación de Caso o de REQ sin Caso registra fecha efectiva, fecha persistida de servidor y autor/ejecutor real. Rectificar crea otra constancia con motivo y enlace, conservando original y cadena. Listado paginado, total y consulta individual se autorizan por contexto. La vista React está conectada a las rutas reales de Caso/REQ y consulta por materia; no se mantiene una segunda copia del relato en una bitácora paralela.

Reutiliza `core_action` y `institution_flow_history`. `BitacoraGestion/Controller` proyecta esos registros y añade metadatos V15.16. `NucleoInstitucional.insertarActuacion` extrae la persistencia común preservando el comando/hash anterior. Los documentos asociados conservan el ID de actuación. `NucleoController/NucleoU2Controller` cierran las rutas provisionales de escritura nueva institucional sin fecha efectiva, conservando exclusivamente reintentos anteriores verificables. Cierre, principal, equipo y tareas siguen separados.

Identidad→COM, revisión y transacción incluyen actuación/metadatos/evento/comando. Positivos y negativos para jefatura/profesional/colaborador, materia de solo lectura, otra área/unidad, TI/documento aislado y cuentas alias. Una rectificación no puede apuntar a otro contexto; una constancia ya rectificada requiere continuar desde su sucesora. No hay PUT/DELETE ni actuaciones de tareas/correos/vistas. Retiro de equipo revoca consulta y escritura sin otro login.

Contrato [bitacora-gestion-comun-v1](../contratos/bitacora-gestion-comun-v1.md), versión1.0. Levantamiento validado06/11/2026; no se reabren decisiones funcionales. Faltan parámetros nominales y extensiones técnicas, no esa validación.

## Persistencia y compatibilidad

V15.16 H2/MySQL idénticas, posteriores a15.15, sin colisiones remotas U4.5 observadas al elegir versión. SHA-256 de ambas copias V15.16: F141300AE003FA4AC9478EDEC98D40F88B7FCFF7237A8AADF48F74A56468314C. Dos tablas de metadatos sobre las fuentes originales; FK compuestas/contexto, versión tipada1.0, unicidad de sucesor y motivo de corrección. Índice único adicional sobre historia/identificador de recurso para FK. No modifica migraciones aplicadas, no usa outOfOrder/backfill ni escribe bases preservadas.

La prueba de actualización crea actuaciones reales de Caso y atención REQ en15.15, guarda filas/comandos/firmas, actualiza15.16 y exige igualdad íntegra, cero metadatos inventados y reintentos válidos. La primera atención resuelta previa se presenta sin fecha efectiva acreditada, no se confunde con recepción o cierre. Una nueva actuación del REQ no crea CAS ni REQ adicional; al abrir Casos se conserva su atención previa y se impide seguir escribiendo gestión global por esa ruta.

## Evidencia y fallos conservados

- Primera selección H2 de45:1aprobada/44errores en limpieza del fixture, por comillas incorrectas del historial Flyway en H2. Sin aserciones fallidas, pero no se considera verde. Consulta corregida por proveedor, sin omitir limpieza ni controles. Log bitacora-focal-1.log.
- Segundo intento falló al compilar el nuevo fixture HTTP por import faltante; corregido, sin ejecución acreditada. Log bitacora-focal-2.log.
- Selección posterior **47/47 H2/MockMvc**, cero fallos/errores/omisiones, Maven03:23: dominio11, HTTP2, U2 17, equipo16 y conservación documental anterior1. Conserva también la evidencia de los errores previos.
- Selección ampliada **18/18 H2/MockMvc**, cero fallos/errores/omisiones, Maven01:38: dominio12, HTTP3, actualización real15.15→15.16, materia HTTP y documento aislado. Añade carrera cierreREQ/registro y compatibilidad de rutas provisionales/reintentos. Log bitacora-focal-final.log.
- Enlaces individuales posteriores: **HTTP3/3**, Maven26s, incluye lectura autorizada de original vinculado y revocación por cambio de materia. Log bitacora-enlaces-http.log. Login/CSRF y BD de prueba reales; no navegador físico.
- DOM: inicialmente7/8, por comprobar propiedad disabled del descendiente de fieldset; corregido para semántica :disabled. Regresión154 tuvo151aprobados/3expectativas del formulario provisional; adaptadas a API/vista nuevas, focal31/31. Al agregar diálogo, faltó el doble showModal de jsdom:154/155 y un error; corregido exclusivamente en fixture, focal9/9. No se ocultan estos intentos.
- MySQL nativo Windows8.0.46 descartable, primera selección36: **35 aprobadas/1fallo** por esperar DataIntegrityViolationException donde MySQL CHECK3819/HY000 se traduce como UncategorizedSQLException. Dominio11/12, migraciones22 (16rutas y6DDL), actualización con actuaciones1 y documento aislado1 aprobaron. No se declara36 verde. Instancia937be00782a249599473eaec40b51fff cerrada/proceso0.
- Prueba corregida exige DataAccessException y causa SQLException con constraint bitacora_rollback, SQLState/código exactos: H2 23513/23513; MySQL HY000/3819. Conserva aserciones de reversión de actuación, revisión, evento y comando. **MySQL final focal1/1**, Maven16s, instancia nuevad87d311cec69482baab235cfebe3f3da cerrada/proceso0; **H2 focal1/1** posterior,Maven10s. Son cortes separados35+1, no36 de una sola ejecución. No se repiten22 rutas ya aprobadas por cambiar únicamente la aserción del fallo inyectado.
- **Frontend final158/158**, lint/TypeScript/Vite aprobados; incluye reintento con revisión visible cambiada sin duplicar payload/clave, revocación, enlaces y conservación de segundos/milisegundos.
- CI integral del nuevo hijo pendiente de publicación; no se atribuye al hijo la CI554/146 del padre.

Evidencia privada LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs bitacora-*. Guion manual preparado, no ejecutado; H2/MockMvc/DOM y MySQL se distinguen de E2E físico/operación. Sin envíos reales ni workers documentales nuevos.

## Cadena y presupuesto CI

Rama codex/siger-bitacora-gestion, commit CI e472f56437789f45658e6bef2761476c3a569906 seguido por el commit funcional del presente informe, hija de PR #10/codex/siger-reservas-documentales en2d98e2122f7bfdf08e102eaf84d7aa496a703371. PR padre10 mantiene su HEAD y borrador: CI42283881844 verde con Windows554/545/9,Ubuntu554/530/24,78raíces y146frontend/lint/build/package.

El mismo inventario completo pasa a tres particiones por OS. Medición de los78 resultados Windows del padre:11,47/26,56minutos de tests en2partes; el reparto determinista3 estima17,99/11,90/8,15, sin prometer esa duración futura. Se conserva30min/job, todos los métodos, aserciones, timeouts, transacciones y exclusiones históricas. Agregador exige3 manifiestos del mismo HEAD/ejecución y unión disjunta completa. Harness local:6raíces/2partes y7raíces/3partes,12rechazos de cobertura inválida, aprobado; es una prueba de metadatos, no de negocio.

## Aceptación manual por roles, preparada

1. En entorno nuevo descartable, configurar identidades/roles sintéticos por circuito U1.5, áreas y materia autorizadas. Una jefatura, profesional principal, colaborador, Dirección lectora de materia, otra área y TI como cuentas diferentes. No cargar nombres institucionales reales.
2. COM→SOL→asunción→REQ sin Caso. Profesional registra gestión con hora efectiva anterior y descripción. Consultar fecha técnica del servidor y verificar que no existe Caso ni otro REQ. Rectificar con motivo: original permanece, enlace permite consultarlo.
3. Jefatura abre un Caso en ese REQ. No se puede seguir agregando gestión global; su atención anterior sigue visible. Profesional registra la nueva gestión en el Caso. Documentos asociados y tareas conservan sus circuitos independientes.
4. Incorporar colaborador; registra como sí mismo. Retirarlo legítimamente sin pendientes: misma sesión deja de consultar/registrar; su actuación anterior conserva autor. Dirección por materia solo consulta; otra área/TI/alias sin asignación no obtienen filas, totales ni constancia por ID.
5. Rectificar una actuación con segundos/milisegundos conocidos y cambiar solo texto: conservar instante. Seguir los enlaces entre páginas; intentar corregir la rama antigua, otro Caso y una decisión administrativa del REQ: denegados.
6. Enviar dos correcciones simultáneas con la misma revisión: una queda confirmada y otra en conflicto. Reintentar la confirmada no duplica. Una respuesta incierta conserva clave/payload y un conflicto definitivo conserva borrador para revisión expresa.
7. Solicitar/cerrar conforme al circuito de jefatura; consulta persiste, nueva actuación se deniega. COM hija no reabre. Reapertura legítima crea período del mismo Caso, sin borrar el cierre anterior.
8. Actualizar una base sintética15.15 a15.16: conservar textos, IDs, fechas técnicas, firmas y comandos; fecha efectiva desconocida sigue desconocida. No aplicar este guion sobre QA/históricos/U4.

## Pendientes reales

Fechas explícitas de la primera atención resuelta; antecedentes/documentos para REQ sin Caso; categorías/campos sectoriales y registro en nombre de otra persona con autorización específica; políticas/calendarios de48h; territorio acreditado/conciliación reversible; búsqueda/reportes/exportación y eventos aún no implementados. No se declara BIT-01 o PLA-01 completos por almacenar dos fechas. No se ha realizado una revisión transversal final ni preparado una versión candidata integrada: corresponde cuando el inventario de138 esté implementado/verificado o tenga limitación concreta, conforme a la última instrucción del responsable.

QA/U4.4/U4.5/SMR,main/padres,bases preservadas,JAR/servicios,Formulario/proveedores y contratos originales intactos. Sin merge/despliegue/operación real.
