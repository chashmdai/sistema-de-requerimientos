# Documentos de Requerimiento sin Caso · 07/11/2026

## Corte publicado acreditado

PR #20 /HEAD29d653a359cacac089b229e53f69d571b69c0ed9. CI42381732338: ochojobs aprobados,98clases/716métodosporOS;Windows707aprobados/9omitidos,Ubuntu692/24,cero fallos/errores. Inventario completo/disjunto,mismoHEAD/run/OS;FE210/210 una vezporOS,lint/TypeScript/Vite yJARfinal3archivosidénticosSHA256. Logs agregaciones128437444939/128437445145 verificados. Borrador sinmerge/despliegue. No acredita al hijo de metadatos/versiones ni pruebas manuales/proveedores.

## Capacidad desarrollada

Un profesional asignado a la atención o la jefatura principal puede conservar un original de gestión en un REQ activo sin Caso. Puede asociarlo al REQ o a una gestión realmente registrada en su bitácora, y consultar el original, sus derivados y la historia correspondiente. No se crea un Caso ficticio ni una actuación por cargar o referenciar. Interfaz integrada en Requerimiento institucional y API privada autenticada/CSRF, sin controles simulados.

Se reutilizan el almacén privado, los límites/formatos, antivirus/inspector configurados, visor PNG y transferencia protegida existentes. `OriginalesGestion` extrae el proceso común sin reinterpretar los hashes, UUID, comandos o contratos de Caso. Los servicios documentales reales siguen siendo requisito de habilitación; los dobles se limitan a pruebas.

La autorización requiere conjuntamente responsabilidad efectiva sobre el principal actual y coincidencia con unidad/sede/área de origen del documento. Un profesional que conserva atención de otro ámbito tras transferir principal no recibe documentación nueva del destino, incluso si adquiere además un rol en ese ámbito. TI, supervisión de clasificación, Dirección por materia, equipo/Caso del mismo REQ y cuenta alias no heredan documentos.

La creación comprueba identidad/área/estado antes del procesamiento y otra vez antes del COMMIT. Apertura de Caso durante antivirus o cambio de principal/ámbito impiden el alta. El cierre o apertura posteriores conservan los documentos previos y sus consultas autorizadas; nuevas cargas quedan cerradas. Reintento confirmado conserva el efecto sin nueva carga si continúa autorizado.

Protección por documento: SIN_CLASIFICAR,ORDINARIO,FIR,BLOQUEADO. Jefatura propietaria puede clasificar con fundamento/revisión; profesional de atención solo restringe a BLOQUEADO. FIR no concede acceso excepcional ni permisos a otro ámbito. Bloqueado no entrega bytes; jefatura conserva metadatos para revisar protección. No se presenta la clasificación como formulario sectorial FIR terminado.

Listados filtran contexto/ámbito/protección en SQL. Detalle, historia, vista, páginas, HEAD y Range verifican permisos propios; descarga revalida cada64KiB. Consultas/solicitudes de bytes se auditan separadamente de mutaciones, sin presumir lectura humana. Cabeceras no-store/CSP/nosniff. Descartes de UI ante revocación/cambio de contexto y solicitudes tardías, reintentos con cuerpo/clave congelados.

## Persistencia y compatibilidad

V15.18 H2/MySQL, idénticas porSHA256 **F4C225CCBAB861E138AA9EBE7AA3F2DE02341551ECDE41F45EEE69B358A99698**.32migraciones. Añade `institution_document.owner_context` nullable y tablas `requirement_document_policy`, `requirement_document_link`, `requirement_document_command`. Contexto explícito REQ y FK a gestión del mismo Requerimiento. No cambia filas históricas por inferencia ni elimina tablas/datos. Historia append-only REQ_DOCUMENTO separada de las actuaciones originales.

V1–V15.17 preservadas, sin outOfOrder. Expectativa final de AutoridadMigracionTest explícita15.18, incluye partida15.17 y DDL idéntico. Upgrade de originales de Caso desde15.17 comprueba todos los campos anteriores, política, vínculo, comando, raíz firmada y reintento; solo nueva columna nula. El contexto REQ queda denegado desde rutas de Caso, evitando el fallback histórico.

Main remoto95cf2ae y referencia U4.4 preservada0f10ddba observados sin nueva referencia remotaU4.5. No se abrió/modificó su worktree; cambios no publicados de otra rama no son verificables. Ningún cambio de contratos públicos, firmas/originales ciudadanos, PULL/ACK, QA/JAR/servicios o proveedores.

## Evidencia y límites de cada corte

- Compilación inicial falló por referenciar clase de auditoría no pública; se reutilizó directamente la tabla de auditoría existente, compilación posterior correcta. No pruebas ejecutadas en el intento de compilación fallido.
- H2 inicial13:12aprobadas/1errorrollback al omitir un bloqueado desde una transacción anidada. Se corrigió el filtrado/autorización, manteniendo las aserciones; foco bloqueo+upgrades autoridad27/27,42s.
- Focal posterior H2/HTTP22:21aprobadas/1aserción HTTP403 esperada frente a404 por recurso fuera de alcance con operador refrescado. El contrato exige404; operador capturado previamente conserva403 por versión de acceso obsoleta. Corrección explícita de expectativa, sin aceptar varios estados ni cambiar backend para obtener verde. En ese corte aprobaron upgrade documental, compatibilidad anterior y tres pruebas de documentos/equipos de Caso.
- Focal siguiente6/6 H2/HTTP,49s: descarga interrumpida tras64KiB por expiración, clasificación concurrente, Caso del mismoREQ sin herencia documental,bloqueo y2HTTP. Posterior endurecimiento de atención tras transferencia requiere foco propio, registrado al cerrar.
- **MySQL8.0.46 nativo Windows45/45**, cero fallos/errores/omitidas:18dominio,1upgrade documentalREQ,26autoridad. Instancia descartable `siger-autoridad-mysql-6c9af5f7d93d47efbe352e5564392148`; esquemaGUID270079004c5d44ca91fdaf8f029f6598.32migraciones, partidas14–15.17. Final04:14, apagada y procesos propios0 comprobados. Log privado documentos-req-mysql.log y tests.log conservados. No acredita MySQL QA/productivo.
- Frontend integral209/209,luego ajuste acotado de limpieza al denegar referencia:8/8 DOM focales,lint/TypeScript/Vite finales. No repetir209 sobre el mismo componente sin razón; CI propia debe ejecutar210 en HEAD publicado.
- Vista PNG/HTTP y transferencia con rol adicional: **5/5 H2/HTTP**,37s, incluido bloqueo profesional204 y reintento,derivada realPNG protegida sinCaso y principal transferido sin herencia de atención por rol adicional. Adicional **1/1 MySQL nativo** del visorPNG en nueva instancia37c374a6a8354b4486855f835e4fca50, cerrada/procesos propios0. No se repiten45 sin cambio de producción ni se suman como una única46/46.
- CI propia del hijo aún pendiente de publicación. CI de PR19/263729b,42378024688,aprobada8jobs/691métodosporOS/FE202,pertenece exclusivamente al padre.

Logs privados de campaña `LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d/documentos-req-*`. Los fallos se conservan. H2/MockMvc/DOM y MySQL aislado con antivirus/inspector dobles no son un E2E físico, prueba de antivirus real o aceptación manual.

## Guion manual preparado, no ejecutado

1. Preparar entorno nuevo descartable con servicios documentales aislados y cuentas sintéticas: distribuidor, jefaturaA/profesionalA del áreaA, jefaturaB/profesionalB del áreaB y TI. Concesiones mediante circuito independiente firmado, sin SQL para conceder permisos ni credenciales en el informe.
2. Distribuidor encamina COM sintética a SOL únicaA; jefaturaA asume y asigna profesionalA. Verificar REQ activo,ceroCAS.
3. ProfesionalA registra una gestión efectiva en bitácora, carga archivo sintético al REQ y otro ligado a esa gestión. Verificar autor/fecha/hash, ausencia de nuevas actuaciones por cargar, descarga y vista PNG.
4. ProfesionalB/TI intentan listado/detalle/historia/bytes con las referencias copiadas:denegación y ausencia de conteos/contenido. Alias de profesionalA con concesión propia no hereda su asignación.
5. JefaturaA clasifica FIR; profesionalA restringe a bloqueado. Profesional no puede levantarlo; jefatura revisa los metadatos y decide con fundamento/revisión. Reintento profesional de su bloqueo responde204 sin reabrir acceso.
6. Abrir Caso del REQ para otro profesional autorizado:conserva su Caso pero no documentos globales del REQ. Nuevas cargasREQ denegadas; consulta anterior legítima preservada. Reintento de una carga confirmada no crea otra.
7. En otroREQ transferir principalA→B con aceptación. Verificar documentosA no publicados aB ni documentosB accesibles al antiguo profesionalA por rol adicional; originales e historia siguen conservados. La publicación entre ámbitos queda pendiente de circuito propio.
8. Vencer/revocar facultad con sesión abierta:actualizar retira documentos/acciones y petición de bytes se deniega. Una transferencia en curso se interrumpe al próximo bloque, sin prometer retirar bytes ya entregados.

## Pendientes reales y continuidad

Publicación/conciliación documental entre ámbitos tras transferencia; metadatos corregibles,versiones documentales/finales y rectificación de asociaciones; cuarentena técnica sin lecturaTI; formulario sectorial FIR; retención/purga solo bajo política y habilitación específicas. Este bloque no implementa esos mecanismos ni inventa una política temporal.

Reglas con cobertura ampliada:ACC-09,DOC-01/02/04,BIT-01/05,AUD-04. Matriz138 conserva IDs/orden/validaciónfuncional06/11; no se declara cumplimiento total. Siguientes bloques programables incluyen versiones/asociaciones,48h/calendarios,invalidación/duplicados,búsqueda/reportes y conciliación histórica. Después del criterio138 procede la auditoría transversal y candidata integrada, todavía no acreditadas.
