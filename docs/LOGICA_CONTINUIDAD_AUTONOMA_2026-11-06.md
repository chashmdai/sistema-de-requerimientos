# Continuidad autónoma SIGER · 06/11/2026

## Instrucción vigente

Levantamiento validado por el responsable el06/11/2026, DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. No pedir nuevas validaciones funcionales. Configurar valores nominales ausentes sin inventar personas/competencias. No equivale a código completo, prueba física o despliegue.

Continuar automáticamente los bloques independientes mientras haya capacidad. Cuando todas las138 reglas estén implementadas y verificadas o tengan una limitación concreta identificada, dejar de incorporar funciones nuevas y pasar a revisión transversal del código/clases/dependencias del laboratorio, retirada controlada de rutas provisionales incompatibles, consolidación y versión candidata integrada/compilable con cumplimiento y escenarios manuales. Todavía existen capacidades independientes desarrollables: no adelantar artificialmente ese corte.

Worktree exclusivo C:\req\worktrees\siger-logica. Preservar main/padres, U4.4/U4.5/SMR/QA, COM-000001, bases históricas, JAR/servicios, Formulario/proveedores, originales/firmas/hashes y COMMIT→ACK. Sin merge a main, despliegue, correo real o nuevas llamadas operacionales. No detener procesos ajenos ni usar bases preservadas.

## Estado y siguiente acción precisa

Corte de recuperación posterior a361e323: rama actual codex/siger-bitacora-gestion, PR #11 borrador base PR #10/2d98e2122f7bfdf08e102eaf84d7aa496a703371. HEAD publicado361e3239a2715f4e6a7d8369b306f3b3a1165e0b. Git determina el HEAD definitivo del siguiente commit, no suponer que este SHA de procedencia incluye el ajuste del fixture.

CI42311131299 de be2da99 terminó con sus ocho jobs aprobados, pero no acredita el ajuste posterior de ejecutor. CI42312378567 de361e323: tres Ubuntu y Windows1/3 aprobados; Windows2 falló con173 métodos,0 fallos de aserción y7 errores de inicialización H2 en SubroganciasTest. Agregación omitida: NO es CI aprobada. Log íntegro privado ci11-windows-part2-361e323-fallida-completa.log (1174780 bytes), SHA25625beec3556986a08c66ee1eee11e34d48054f583461bee103db0f988dc803c10. No borrarlo ni sustituir ese intento por verde.

Ajuste pendiente de confirmar: DatosNucleoTest usa un solo pool para migración y negocio únicamente en H2 local con las mismas credenciales, como el arranque real. Evita cerrar/reabrir H2 entre esas fases; conserva separados migrador/negocio en destinos explícitos/MySQL y cierra recursos si falla el constructor. Sin cambios de BaseJdbc, timeouts3000ms/4s/5s, URL, DB_CLOSE_DELAY, exclusiones o migraciones. La alternativa de solapar dos pools y cerrar el de migración falló15/15 por restricciones vinculadas a base cerrada (ci11-handoff-focal.log), conservada. Solución final H2:15/15 (Subrogancias13+actualizacionesDocumento1/Bitácora1),02:13, ci11-fixture-pool-unico.log. MySQL nativo Windows8.0.46 FixtureJdbc final2/2 (actualizaciones Documento15.14 y Bitácora15.15), instancia67e708f123be4ce681ec5b455132e708 cerrada/proceso propio0; conserva pools separados. Log ci11-fixture-mysql.log. H2 final15/15; CI nueva pendiente.

Trabajo hijo preservado en commit LOCAL cd0adc9770d1a8008d753263452a2562fd9dd760, rama codex/siger-ingreso-territorio, todavía sin push/upstream/PR. Primera atención fechada/tipada COM+REQ final sin CAS/SOL, circuito real API/UI y compatibilidad de reintentos. H2 dominio8/8; HTTP corregido2/2; MySQL nativo8/8 en instancia228cd3236bc74daf80bde3fb43337e63 cerrada/proceso0; DOM9/9. Selección ampliada27 anterior tuvo25 aprobadas/1fallo+1error de fixture (sede de captura y columna numero), corregidos sin cambiar reglas. Informe/contrato están en ese commit hijo. No reiniciar ni descartarlo. Territorio firmado y V15.17 todavía NO implementados.

Siguiente acción exacta:
1. H215 y MySQL2 concluidos: confirmar/push normal del arreglo y documentos en PR #11. Exigir CI nueva del HEAD exacto, ocho jobs y cobertura completa, sin reintentar a ciegas ni ampliar presupuesto30min.
2. Con árbol limpio, volver al hijo cd0adc9 e incorporar explícitamente el padre propio corregido mediante merge controlado (no main). Resolver checkpoint/helper preservando ambas capacidades. Publicar el hijo cuando alcance bloque coherente, PR borrador base PR #11.
3. Continuar territorio: reutilizar org_region/org_coverage/OrganizacionInstitucional; extender contrato firmado de autoridad1.8 para acreditar cobertura concreta/versión/periodo, sin convertir propuesta TI en autorización. Resolver asunción regional por domicilio del requiriente; excepción fundada cuando no hay antecedente utilizable, principal persistente e independiente de Casos. Verificar versiones antes de15.17. Después antecedentes/documentación REQ sin Caso, calendarios/plazos, conciliación, búsqueda/reportes/exportación y eventos restantes. No declarar candidata ni138 completas prematuramente.

## Bitácora desarrollada/calificada localmente

BitacoraGestion/Controller, proyección común de core_action e institution_flow_history, metadatos V15.16 idénticos H2/MySQL. No copia relatos. Versión1.0, fecha efectiva/servidor, registrador/ejecutor propio, rectificación vinculada/motivo/cadena, consulta individual y paginación/conteos autorizados. FK compuestas preservan contexto y original; identidad→COM/revisión/idempotencia/rollback. No tareas/actuaciones automáticas ni escritura poscierre. Consulta por materia solo lectura; TI/SOLO_DOCUMENTO/alias/otra área no reciben bitácora. Retiro de equipo se aplica con sesión vigente.

React común conectado a Caso/REQ sin Caso y Dirección lectora. Históricos no acreditan fecha efectiva/ejecutor por inferencia. Conserva segundos/milisegundos incluso al normalizar ceros el navegador; reintento incierto conserva payload/clave. Nuevo REQ/CAS no se crea por actuar. Cuando REQ ya tiene Casos, atención previa sigue legible pero nueva gestión corresponde al Caso. APIs provisionales U2 sin fecha solo reproducen comandos confirmados; Caso histórico no conciliado conserva compatibilidad documentada. Firma, originales, documentos asociados y hashes anteriores intactos.

Pruebas, cortes separados:
- H2 primero45:1pasado/44errores de limpieza del fixture (comillas Flyway H2); no verde. Segundo intento falló en compilación por import de fixture. Corregidos sin bypass ni exclusiones.
- Luego47/47 H2/MockMvc (11dominio+HTTP2+U2 17+equipo16+documento anterior1), Maven03:23. Ampliación18/18 (dominio12+HTTP3+upgrade1+materiaHTTP1+documento aislado1), Maven01:38. EnlacesHTTP posteriores3/3,26s. SQLState estricto/rollback H2 final1/1,10s.
- MySQL nativo Windows8.0.46 nuevo, selección36:35aprobadas/1fallo por tipo Spring de CHECK3819/HY000, no fallo funcional acreditado pero NO verde. Dominio11/12,22migraciones (16rutas+6DDL),upgrade1 ydocaislado1 pasaron. Instancia937be00782a249599473eaec40b51fff cerrada/proceso0.
- Corregida prueba exige SQLException/constraint/código/SQLState exactos de H2 yMySQL y reversión completa de actuación/revisión/evento/comando. MySQL final focal1/1,16s, nueva instanciad87d311cec69482baab235cfebe3f3da cerrada/proceso0. Son35+1 en cortes distintos, no36 en una sola selección aprobada; no repetir rutas ya verdes por modificar solo esa aserción.
- Frontend final158/158, lint/TypeScript/Vite aprobados. Se conservan intentos7/8 (:disabled),151/154 (expectativas formulario viejo) y154/155 (doble showModal faltante), corregidos en fixtures/adaptación real. DOM no acredita navegador físico.

Informe/guion LOGICA_BITACORA_GESTION_2026-11-06.md, contrato bitacora-gestion-comun-v1.md. Matriz138 IDs/orden/validación preservados: BIT-01/02/03/05/06,TAR-05,PLA-01,SOL-02 y C2 anterior actualizados con límites. Fecha efectiva de primera respuesta, campos sectoriales/reserva de actuación, registro por otra persona, calendarios/reportes y documentación REQ sin Caso siguen pendientes técnicos. No declarar BIT-01, PLA-01, U2–U9 o candidata completos.

## CI completa y padres preservados

Cadena #2→#3→#4→#5→#6→#7→#8→#9→#10→hijo bitácora; todos borradores, sin merges a main. Main95cf2ae, sin ref remota U4.5 nueva observada al elegir15.16; worktree U4.5 localG0 intacto, no se abrió su directorio.

- PR #6/396422d: equipos/agenda/TAR09,V15.10,MySQL73/73. CI42213485912 Windows478/469/9,Ubuntu478/454/24,frontend119.
- PR #7/cdb176f: término colaboración,V15.11,MySQL82/82 en corte documentado. CI42220441097 Windows490/481/9,Ubuntu490/466/24,frontend123.
- PR #8/feb9f01: funciones/subrogancias1.4,V15.12,MySQL13/13 final y86 previos separados. CI42267102474 verde71raíces,Windows506/497/9,Ubuntu506/482/24,frontend129.
- CI #8 anterior42233659071: Windows cancelado30min,446/cero fallos/9omitidos, faltaron60; Ubuntu506/482/24 aprobado. Logs ci8-windows-30min.log/ci8-ubuntu-aprobada.log preservados. No reetiquetar ese intento como verde.
- PR #9/5e67436: SUB-CONT-01/Registro1.5/V15.13 y doble canal§16 para eventos emitidos. MySQL23/23+1/1 solapado. CI42267451118 verde73raíces,Windows518/509/9,Ubuntu518/494/24,frontend133. Integral local518/509/9 anterior a C, no atribuirla al hijo.
- PR #10 C1/c767ab7: Registro1.6/V15.14, doble respaldo FIR con Caso legible. MySQL32/32, instancia5662a875d9f5491ba6fc0b44eadefca3 cerrada. CI42273313757 verde75raíces,Windows535/526/9,Ubuntu535/511/24,frontend137.
- PR #10 C2/2d98e2122f7bfdf08e102eaf84d7aa496a703371: Registro1.7/SOLO_DOCUMENTO/V15.15, sin expediente ni rol implícito; ORDINARIO/FIR acreditados, ciudadano nunca FIR. MySQL47/47, instancia70be9ec7705d49e8aa320e3f4c2a73e7 cerrada/proceso0. CI42283881844 seis jobs verdes y logs consultados,78raíces completas/disjuntas,Windows554/545/9,Ubuntu554/530/24,frontend146/lint/build/package. Acta PR #10 actualizada; su rama sigue en2d98e21, intacta al desarrollar hijo.

Conteos OS = total/aprobados/omitidos; cero fallos/errores en cortes aprobados. Mantener distintos H2/MockMvc/DOM/MySQL y E2E físico. No sumar repeticiones o atribuir CI de un padre a su hijo.

CI hija: tres particiones, presupuesto30min intacto. Medidos78 tiempos Windows padre11,47/26,56min en2; reparto3estima17,99/11,90/8,15. Misma selección determinista, métodos/aserciones/timeout/transacciones/exclusiones. Harness local6raíces/2partes+7raíces/3partes y12 controles negativos aprobado. Inventario compilado completo/disjunto, manifiestos del mismo HEAD/run y ocho jobs obligatorios. LOGICA_CI_PARTICIONES_2026-11-06.md conserva diagnóstico y evidencia. El modo normal integral de Build-Siger no cambia.

## Herramientas y evidencia segura

Java25 explícito C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot; Maven C:\maven\bin\mvn.cmd, Node24, Pwsh7; Build-Siger aislado, nunca C:\req\bin. No Maven concurrente sobre target compartido. Campaña LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs bitacora-* y documento-aislado-*/reservas-*; tests.log en GUID MySQL anteriores. No imprimir/copiar client.cnf/init.sql, configuraciones privadas o secretos. Auxiliar solo binario/loopback/GUID propios y apagado del proceso verificado, no MySQL80global/firewall/servicios. No borrar evidencias por rutina.

V1–V15.16 preservadas después de su calificación; nuevas migraciones posteriores, sin outOfOrder. V15.16 SHA ambas copiasF141300AE003FA4AC9478EDEC98D40F88B7FCFF7237A8AADF48F74A56468314C. Rollback probado es transaccional, no deshacer esquema/históricos. Guiones por roles preparados, no ejecutados. Ningún dato real nominal cargado, cero correo/Graph/proveedor real.

Rector SHA80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4 verificado, cuestionario27 no encontrado sin bloquear reglas suficientes. H2/U4/AGENTS leídos; son preservación, no activación operacional. No repetir diagnósticos cerrados.

## Ajuste de autoría en ejecución, preservar

PR #11 borrador creado: codex/siger-bitacora-gestion base #10, HEAD publicadobe2da9965dd4fccf95d4079993fb548829e1aa3d; CI42311131299 de ese corte en curso (3partes+agregación). Se abrió rama vacía codex/siger-ingreso-territorio enbe2 para continuar, sin código todavía; luego se volvió con árbol limpio a bitácora para corregir un defecto detectado en revisión: rectificar no debe atribuir al rectificador la ejecución de la gestión original.

Cambios locales nuevos: BitacoraGestion resuelve ejecutor original por cadena del mismo contexto, caché por consulta, histórico desconocido sigue desconocido; registrador real intacto y ciclos fallan cerrados. DTO/filas/migraciones/firmas/contratos anteriores no se reescriben. UI cuenta constancias, no gestiones nuevas. Nuevos tests de Caso/REQ, raíz desconocida, cadena y ciclo; HTTP exige ejecutor profesional y registrador jefatura. H2 final17/17 (14dominio+3HTTP), Maven01:16; DOMfocal12/12. MySQL focal3 en curso, sesión del último exec, log bitacora-ejecutor-mysql.log privado: NO solapar Maven ni declarar aprobado antes del resultado/proceso propio0.

SIGUIENTE: terminar MySQL3, actualizar informe/matriz/checkpoint con resultados, commit fix/pushnormal a PR #11, exigir CI del HEAD corregido (el resultado de be2 no califica el ajuste). Luego volver a rama ingreso-territorio con árbol limpio y avanzar por fast-forward explícito de este padre. Fuentes §§06/07/08/15/17 leídas: primera atención requiere fecha de gestión propia, y territorio usa domicilio del requiriente, nunca NNA/hechos/captura; ámbito central no se regionaliza por inferencia. Próximo bloque previsto fecha explícita de respuesta + catálogo territorial firmado/configurable con prueba sintética, sin valores nominales reales.

Ajuste de autoría TERMINADO localmente: H217/17 y MySQL nativo3/3, instancia d7552717035845618de2656d4b7d8107 cerrada/proceso0; sesión79732 terminada. DOM focal12/12. Sin cambios de migración/filas/firmas. Commit/push del ajuste y CI exacta siguientes; luego continuar ingreso-territorio (rama vacía be2) por fast-forward del padre corregido. No hay Maven/MySQL pendiente.
