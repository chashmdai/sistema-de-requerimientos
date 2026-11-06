# Continuidad autónoma SIGER · 06/11/2026

## Decisión y preservación vigentes

Levantamiento validado por el responsable el06/11/2026, decisión versionada DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. No pedir nuevas validaciones funcionales. Configurar valores nominales ausentes, sin inventar personas/competencias. Validación funcional no equivale a código completo, prueba física, carga nominal o despliegue.

Worktree exclusivo C:\req\worktrees\siger-logica. Preservar main/padres, U4.4/U4.5/SMR/QA, COM-000001, bases históricas, JAR/servicios, Formulario/proveedores, originales/firmas/hashes y COMMIT→ACK. Sin merge a main, despliegue o correo real. No detener procesos ajenos ni usar bases preservadas.

## Estado real y siguiente acción

Rama/upstream codex/siger-reservas-documentales / origin/codex/siger-reservas-documentales. HEAD publicado c767ab7bb837f2c3f7a1aef87f9de03ad6003c5f, PR #10 borrador base codex/siger-continuidad-excepcional/PR #9. Cambios C2 locales aún sin commit: conservar, no reiniciar C1/A/B. CI C1 ya aprobada; publicar C2 y exigir su CI propia. No Maven activo: sesiones5431/29864 terminadas satisfactoriamente. Árbol sucio solo por C2 y documentación.

1. Revisar diff/check/guard de nombres privados, commit coherente C2, push normal y actualizar PR #10. Informe y contrato1.1/matriz138 actualizados con evidencia C2. CI del nuevo HEAD pendiente, no usar535 del C1 como prueba de C2.
2. Iniciar bloque independiente bitácora común, fechas efectivas/registro y rectificaciones inmutables para Caso y REQ sin Caso. Antes de cambiar rama exigir árbol limpio y commits preservados; nueva rama/PR dependiente de #10 si el alcance crece. Leer §§15/17 ya localizados, no repetir U0. Preservar hashes/DTO/semántica de comandos históricos.
3. Mientras se desarrolla, observar CI C2 exacta Windows/Ubuntu (dos particiones+agregación por OS). Corregir fallos sin nuevas exclusiones/timeout funcional. Continuar después hacia territorio/conciliación, documentación de gestión y plazos/reportes, conforme dependencias.

## Cadena y evidencia cerrada de padres

#2→#3→#4→#5→#6→#7→#8→#9→#10, todos borradores sin merges a main. Main95cf2ae, última referencia U4.5 localG0 sin novedades remotas observadas antes de15.14/15.15; no se abrió su directorio.

- PR #6/396422d69c29ace39f65ce84a5552b1a45c066f1: equipos/agenda/TAR09, V15.10, MySQL73/73. CI42213485912 Windows478/469/9,Ubuntu478/454/24,frontend119/lint/build/package.
- PR #7/cdb176f2ea81ef66727c2077cbf271fdb05fa9e1: término colaboración, V15.11, MySQL82/82 en corte documentado. CI42220441097 Windows490/481/9,Ubuntu490/466/24,frontend123/lint/build/package.
- PR #8/feb9f01fdf46d5002cae8abb74b36d5c1ee7a06e: funciones/subrogancias Registro1.4, V15.12, H2/MySQL13/13 final y86 previos separados. CI42267102474 seis jobs verdes,71 raíces completas/disjuntas, Windows506/497/9,Ubuntu506/482/24,frontend129. Particiones243+263; MavenWin13:53/16:13,Ub13:49/14:39.
- Intento CI anterior #8/42233659071 conservado: Windows canceló por presupuesto global30min,446/cero fallos/9omitidas, faltaron60. Ubuntu506/482/24 aprobó. Logs privados ci8-windows-30min.log/ci8-ubuntu-aprobada.log y diagnóstico LOGICA_CI_PARTICIONES_2026-11-06.md. Particionar no cambió aserciones, timeouts, transacciones, presupuesto o exclusiones; inventario completo/disjunto y pruebas negativas del agregador obligatorios. Nunca reetiquetar el intento como verde.
- PR #9/5e674363e63a091af7407847dec36501b037bc63: SUB-CONT-01 Registro1.5/V15.13 y doble canal§16 para eventos emitidos, sin permiso FIR implícito. CI42267451118 seis jobs verdes,73 raíces, Windows518/509/9,Ubuntu518/494/24,frontend133/lint/build/package. Integral local518/509/9,Maven22:20, temporales474683b20d424d7380ce312a29f11bfb compilados antes de C; no atribuirlo al C. MySQL23/23 más1/1 solapado, procesos propios cerrados.
- PR #10 C1/c767ab7bb837f2c3f7a1aef87f9de03ad6003c5f: Registro1.6/V15.14, doble respaldo FIR con consulta previa de Caso. CI42273313757 seis jobs verdes/logs consultados,75 raíces completas/disjuntas, Windows535/526/9,Ubuntu535/511/24,frontend137/lint/build/package. Particiones243+292,MavenWin15:31/15:14,Ub13:54/17:41. MySQL32/32 (14 dominio+18 migración) en5662a875d9f5491ba6fc0b44eadefca3 cerrado/ausente; primera H2 55 con54pasadas/1fixturelogin, corrección2/2 conservada.

Los conteos OS son total/aprobadas/omitidas; cero fallos/errores en cada corte aprobado. No sumar repeticiones de frontend o nativo a regresión CI ni atribuir éxitos de padres a hijos.

## C2 desarrollado y calificado localmente

Registro1.7 añade autorizantesDocumentoAislado, distinto de1.6. V15.15 aditiva H2/MySQL idéntica: scope_mode default CASO_DOCUMENTO/CHECK. Mantiene los campos/huellas/firmas/comandos anteriores. Nuevo SOLO_DOCUMENTO con discriminador1.7 y ruta/acción explícitas, doble respaldo de personas distintas, cuenta nominal, vigencia y ámbito exactos. Materia funcional distinta de unidad propietaria se acredita explícitamente1.7, sin reinterpretar firmas anteriores. No acceso implícito por TI, alias, unidad, Caso o REQ.

ORDINARIO/FIR acreditados pueden usar el modo aislado; SIN_CLASIFICAR/BLOQUEADO no. Original ciudadano no es FIR: guard FIR_REQUIERE_FICHA_INTERNA intacto. Nueva ruta documental mínima sin Caso/COM/equipo/historia; páginas/metadata distintas de descarga, custodia/visor/Range/HEAD existentes, revalidación antes de cabeceras y cada64KiB. Relación/política/secuencias/respaldo/credencial actuales. Revocación impide el siguiente bloque. React protección/bandeja/página conectados y sin CaseID del beneficiario aislado.

- H2/MockMvc: primero2/2; selección48 con47pasadas/1auditoría demasiado larga. Corregido código32 sin eliminar auditoría/ensanchar columna. Siguiente5: HTTP3/3 y upgrade anterior1/1; fixture de materia sin facultad regional falló. Se ejecutó U1.5 real para esa cuenta; otro fallo de revisión obsoleta corrigió fixture a contexto actual. Final materia1/1.
- Ciudadano1.1: fixture inicial firma rechazado por instantes distintos; siguiente FIR rechazado correctamente. Corrección preserva guard y usa ORDINARIO+facultad1.7. Final ciudadano+ordinario2/2 H2, firma/recibo/bytes intactos, ACK pendiente, ingesta sin REQ y nacimiento por SOL/asunción/CAS. HTTP4 total incluye sesiones/CSRF, Range206/HEAD/416, PNG y revocación65536/200000bytes. AV/inspector dobles, no E2E físico.
- MySQL nativo Windows8.0.46 NUEVO descartable47/47,0fallos/errores/omisiones: aislado12+anterior1+C1 14+migración20 (15rutas y5DDL). Maven08:12, instancia70be9ec7705d49e8aa320e3f4c2a73e7 cerrada y proceso propio0 verificado. Upgrade real15.14→15.15 conserva concesión/hash/firmas/comando.
- Frontend146/146, lint/TypeScript/Vite aprobados, sesión29864 cerrada. Primer DOM22 tuvo21/1selector de fixture, luego22/22 y regresión146. C2 CI propia pendiente de publicación.

Informe/guion LOGICA_ACCESO_DOCUMENTAL_RESERVADO_2026-11-06.md, contrato acceso-documental-reservado-v1.md versión1.1; matriz138 IDs/orden/validación conservados. Guiones preparados, no ejecutados. Catálogos/personas reales sin cargar. Ficha sectorial, retención/purga, documentos REQ sin Caso, bitácora/fechas, plazos/reportes y eventos no emitidos siguen pendientes técnicos, no decisiones funcionales por revalidar.

## Evidencia privada y ejecución segura

Campaña LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs reservas-* y documento-aislado-*; logs MySQL tests.log en GUID propios anteriores. No publicar configuraciones privadas/client.cnf/init.sql ni secretos. Auxiliar solo loopback/GUID con verificación de proceso/datadir y liberación propios; no tocar MySQL80global/firewall/GPO/servicios. Java25 explícito, Maven/Node24/Pwsh7, Build-Siger aislado, nunca C:\req\bin. No Maven concurrente en target compartido.

Rector SHA80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4 verificado. Cuestionario27 no encontrado, sin bloquear capacidades definidas. Graph simulado, cero envíos reales, H2/MockMvc/DOM/MySQL separados de E2E físico/operación. No nuevo diagnóstico U0 ni pruebas acreditadas repetidas innecesariamente.
