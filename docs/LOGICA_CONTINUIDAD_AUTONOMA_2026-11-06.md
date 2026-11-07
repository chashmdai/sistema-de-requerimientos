# Continuidad autónoma SIGER · 06/11/2026

## Instrucción vigente y preservación

Levantamiento y decisiones funcionales validados por el responsable06/11/2026: DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. No volver a pedir esa validación; parámetros nominales ausentes requieren configuración, no inventar permisos/personas/coberturas. No equivale a código completo o autorización operacional.

Continuar bloques independientes mientras haya capacidad. Cuando las138 estén implementadas/verificadas o tengan una limitación concreta, dejar funciones nuevas y realizar revisión transversal de código/clases/dependencias heredadas, retirada controlada de rutas provisionales incompatibles, consolidación y candidata integrada/compilable con informe138 y escenarios manuales. Todavía hay capacidades programables; no convertir pendientes técnicos en impedimentos externos para anticipar ese corte.

Único worktree C:\req\worktrees\siger-logica. No modificar main/padres, U4.4/U4.5/SMR/QA, COM-000001, históricos, JAR/servicios/Formulario/proveedores/credenciales, originales/firmas/hashes o COMMIT→ACK. Sin merge a main, deploy, correo real o llamadas operacionales. No detener procesos ajenos, usar bases preservadas, force/reset/clean destructivos.

## Estado actual exacto y próxima acción

Rama ACTUAL codex/siger-bitacora-gestion, PR #11 borrador base #10, HEAD de procedencia35b27aff6d818147477649b560470805e69c60b7. El siguiente commit confirma el arreglo de reloj PISEE. Hijo preservado localmente en codex/siger-ingreso-territorio, HEAD d62b8c4 (territorio), sobrecd0adc9 (fecha primera gestión) y40bf7d2 (merge del padre35b27af); todavía sin upstream/push/PR. Padres #2–#10 intactos.

CI42316699961 del padre35b27af: Windows2/3 y Ubuntu2 aprobados, Ubuntu3 falló177 métodos/1aserción/0errores en NodoPiseeLecturaTest; partes1 todavía en curso al escribir. La prueba exige429 inmediatamente después de una respuesta HTTP, pero el límite250ms se mide desde la solicitud y esa respuesta tardó más en Ubuntu, por lo que obtuvo404 de la ruta local sin handler. No es PISEE real ni fallo de autorización institucional. Mantener límite250ms y timeouts. Log completo privado ci11-ubuntu-part3-35b27af-fallida-completa.log,1268476bytes,SHA256dc8906feecb7a8c83912c0fb2d8a3177b454a6e2f79d7ed164573f5e26551b65.

Arreglo PISEE terminado localmente: constructor público conserva System.nanoTime, límite250ms, HTTP65s/conexión5s y ninguna ruta de envío. Fuente monotónica inyectable solo por constructor de paquete para pruebas. Rechazo hasta249999999ns y aceptación exacta250000000ns sin depender de demora HTTP; admite origen negativo y wrap del contador mediante diferencia temporal. Dos pruebas aprobadas/0omisiones,9.869s, ci11-pisee-reloj.log; servidor HTTP únicamente loopback. Sin sleeps, cambios de exclusiones o proveedores reales.

Siguiente acción PRIORITARIA: commit/push del arreglo PISEE y acta de incidente a PR #11, exigir CI nueva exacta. Luego con árbol limpio volver a hijo d62b8c4 y merge controlado del padre propio, resolver checkpoint preservando estado real. Completar MySQL territorio, compatibilidad documental1.7→1.8, integral frontend/lint/build, contrato/matriz y publicación PR hijo base #11. No repetir29H2/4HTTP/14DOM salvo cambios relevantes. No Maven/MySQL activo una vez termine sesión30907; focal PISEE ya terminó según log.

## Ingreso/territorio desarrollado en hijo

PrimeraGestion privada1.0 COM+REQ final sinCAS/SOL: recepción, respuesta efectiva, servidor, resultado ORIENTACION/RESPUESTA_DIRECTA/REMISION_CONCLUIDA. API/UI reales, captura desde cuenta separada del principal, fecha explícita, rango/ms, idempotencia concurrente, rollback. ReutilizaV15.16; legacy primera-atencion solo reintentos confirmados actor/acción/hash, sin alterar contratos previos o transporte público.

Territorio: reutiliza org_coverage/OrganizacionInstitucional. TI propone y finaliza cobertura; registro firmado explícito1.8 acredita UUID/región/sede/periodo/huella semántica de propuesta/ref/SHA/estado, sin reinterpretar1.0–1.7. Decodificación estricta y firmas vigentes; cambio de propuesta, suspensión, retirada, expiración o raíz no acreditada cierran nuevas asunciones. Materia funcional distinta de unidad mantiene semántica1.7. No asignaciones nominales inventadas.

Asumir SOL y propio ingreso de jefatura admiten domicilio del requiriente utilizable o decisión fundada por ausencia/contradicción/extranjero/inutilizable; nuevo contrato exige fundamento y referencias en excepción regional y conserva declaración. No NNA/hechos/captura como domicilio ni regionalización automática de central. No distribución que cree REQ. Principal histórico existente no cambia al cambiar cobertura, Casos o consulta por materia.

Constancia territorial tipada1.0 se añade a institution_flow_history en la misma transacción de nacimiento, sin relato duplicado ni nueva tabla: REQ_TERRITORIO_ACREDITADO. NO V15.17: decisión técnica de reutilizar historial y registro firmado existentes, evitando índice/catálogo paralelo del diseño preliminar. V1–V15.16 intactas, sin outOfOrder/backfill. Históricos sin constancia siguen desconocidos. Frontend decide mediante rutas explícitas y muestra cobertura actual; preflight propio comprueba autor de COM/jefatura/área antes de revelar. Solo jefatura legítima decide, lectura materia/TI/otra área no habilita.

Pruebas hijo, cortes separados:
- Ingreso dominio8/8 H2, HTTP2/2 final, DOM9/9; MySQL Windows8.0.46 nuevo8/8 en228cd3236bc74daf80bde3fb43337e63 cerrado/proceso0. Ampliación anterior27 tuvo25aprobados/1fallo+1error de fixture de sede/columna; corregidos sin cambiar permisos.
- Territorio focal29/29 H2:11dominio+17U2+1upgradeBitácora,02:45. HTTP posterior4/4 (territorio2+ingreso2),31s. DOM14/14 (territorio5+distribución9). Compilación Java correcta; primer comando desde raíz sinPOM no ejecutó build, corregida ruta explícita. No nativo territorio ni integral/CI hija todavía.
- Informes LOGICA_INGRESO_TERRITORIO_2026-11-06.md y contratos/primera-gestion-institucional-v1.md; contrato territorial y matriz pendientes de consolidación al terminar calificación.

## PR #11: evidencia, fallos y correcciones preservados

Bitácora común Caso/REQ sinCaso, metadatosV15.16 sobre core_action/institution_flow_history, fechas efectiva/servidor, ejecutor original por cadena/registrador real, rectificación vinculada inmutable, enlaces individuales y paginación/conteos autorizados, revocación con sesión vigente. No tareas/actuaciones automáticas ni escrituras poscierre; legado sin fechas/ejecutores inferidos. Rutas provisionales nuevas U2 sin fecha409 y reintentos confirmados; Caso histórico no conciliado mantiene compatibilidad explícita.

Commits e472f56 (tres partes CI),be2da99 (bitácora),361e323 (ejecutor original) y35b27af (fixtureH2). V15.16 SHAambasF141300AE003FA4AC9478EDEC98D40F88B7FCFF7237A8AADF48F74A56468314C. H247/47+18/18,HTTP3/3,rollback1/1; MySQL36 inicial35aprobadas/1fallo por tipo Spring CHECK, finalfocal1/1 en otra instancia; 22migraciones aprobaron primer corte. Ajuste ejecutor H217/17,MySQL3/3,d7552717035845618de2656d4b7d8107 cerrado/proceso0,DOM12/12. Frontend158/158/lint/build en corte funcional, requiere CI nueva para cambios posteriores.

CI42311131299 be2da99 ocho jobs aprobados, no acredita fixes posteriores. CI42312378567/361e323: Windows2 falló173/0aserciones/7errores H2setup, otros cinco jobs de prueba aprobaron, agregación omitida. Log íntegro ci11-windows-part2-361e323-fallida-completa.log,1174780bytes,SHA25beec3556986a08c66ee1eee11e34d48054f583461bee103db0f988dc803c10.

35b27af: mismo pool migración/negocio SOLO H2localsa, como arranque real; MySQL/explícitos mantienen pools separados y cierre ante fallos del constructor. No cambio BaseJdbc/URL/DB_CLOSE_DELAY/timeouts3000ms/4s/5s/exclusiones. Alternativa local solapar dos pools falló15errores por CHECK sobre base cerrada (ci11-handoff-focal.log), descartada implementación, evidencia conservada. Solución H215/15,02:13, MySQL2/2 upgrades15.14/15.15, instancia67e708f123be4ce681ec5b455132e708 cerrada/proceso0. Su CI nueva tiene el fallo temporal de PISEE descrito arriba; no declarar verde.

Informes LOGICA_BITACORA_GESTION_2026-11-06.md/LOGICA_CI_PARTICIONES_2026-11-06.md y contrato bitacora-gestion-comun-v1.md preservan cortes y guiones. Matriz138 conserva IDs/orden/validación funcional06/11. No138 completas: campos sectoriales/reserva de actuación/registro por otra persona, documentosREQsinCaso, clasificación/invalidación/rectificación manual, calendarios/plazos, conciliación históricos, búsqueda/reportes/exportación y eventos restantes pendientes técnicos. Datos territoriales oficiales todavía no cargados; org.oficina creaLAB y mecanismo de carga nominal se prepara sin inventarlos. No usar falta de validación funcional como bloqueo.

## Cadena anterior acreditada, no repetir

#2→#3→#4→#5→#6→#7→#8→#9→#10→#11→hijo; todos borradores ysinmerge. Main95cf2ae, sinrefremotaU45nueva al revisar para territorio, worktreeU45G0intacto sin abrirlo.
- PR6/396422d:V15.10 equipo/agenda/TAR09,MySQL73;CI42213485912 Win478/469/9,Ub478/454/24,FE119.
- PR7/cdb176f:V15.11 término colaboración,MySQL82;CI42220441097 Win490/481/9,Ub490/466/24,FE123.
- PR8/feb9f01:V15.12 funciones/subrogancias1.4,MySQL13final+86previos;CI42267102474 Win506/497/9,Ub506/482/24,FE129,71raíces. Fallidaanterior42233659071 Win30min446/0fallos/9omitidos,faltaron60,Ub506aprobado,logsprivadosconservados.
- PR9/5e67436:V15.13 SUB-CONT01/Registro1.5+doblecanalSIGER/mail para eventos funcionales emitidos,MySQL23+1solapado;CI42267451118 Win518/509/9,Ub518/494/24,FE133,73raíces.
- PR10C1/c767ab7:V15.14 FIR por documento conCaso/Registro1.6,MySQL32;CI42273313757 Win535/526/9,Ub535/511/24,FE137,75raíces.
- PR10C2/2d98e21:V15.15 SOLO_DOCUMENTO/Registro1.7,ciudadanojamásFIR,MySQL47;CI42283881844 Win554/545/9,Ub554/530/24,FE146,78raíces,seisjobs. Winpartes11:34/26:40,Ub09:21/23:47. Instancia70be9ec7705d49e8aa320e3f4c2a73e7cerrada/proceso0. Padreintacto.

TotalesOS=total/aprobados/omitidos,ceroerrores/fallos en cortesverdes. No sumar repeticiones ni atribuir al hijo CI de un padre. Guiones preparados,noejecutados. H2/MockMvc/DOM/nativeMySQL no acreditan E2E físico o proveedores reales.

## Herramientas y evidencia

Java25 C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot, MavenC:\maven\bin\mvn.cmd, Node24.22/Pwsh7. Build-Siger aislado,noC:\req\bin. Cwdbackend paraMaven o-fpomexplícito; rootpara rutasrepositorio. NoMavenconcurrentes sobre target. Focales-Xmx256m/UsePerfDataapagado; no alterar parámetrosfuncionales.

Campaña privada LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs bitacora-*/ci11-*/ingreso-gestion-*/territorio-*. MySQL auxiliares GUIDloopbackpropios,no-defaults,bindprobe/owner,puerto55000–61000,ACLprivadasysoloapagadoownPID. No imprimir client.cnf/init.sql/config/secretos,ni borrar evidencia. Selecciones IngresoGestion8 yFixtureJdbc2 preservadasenmerge; añadirTerritorio antesdelnativo.

CI tres partesSHA256porOS+dosagregaciones,ochojobs,30minsinexclusionesnuevas. InventariobytecodeJUnitcompilado,XMLreales,HEAD/run,unióncompleta/disjunta;harness12negativosaprobado. ModoBuild-Sigerintegralintacto. No reintentos ciegos o aumentobudgetparaocultarfallos.

V1–V15.16 se preservan; nuevas migraciones solo si hacen falta, verificar refs/orden, sinoutOfOrder. RectorSHA80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4reverificado. Cuestionario27noencontrado;noimpide reglasclaras. AGENTS/H2/U4leídos:preservación,noactivación. Sinfechas/autoridadesnominalesinventadas,correos/proveedoresreales0.
