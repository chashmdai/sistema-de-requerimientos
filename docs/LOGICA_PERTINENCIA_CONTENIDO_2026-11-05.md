# SIGER · Pertinencia y protección de contenido · 05/11/2026

## Continuidad y alcance

Rama dependiente `feature/siger-pertinencia-documental`, worktree exclusivo `C:\req\worktrees\siger-logica`, origen limpio `d455b98f1067b3fbe56931b0f01bb7b5406362d8`. Base de integración `feature/siger-logica-negocio`, PR #2 en borrador. No se repiten U0–U1.6b.1. Rector local íntegro SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`; no se publica. Contrato: [pertinencia y contenido](../contratos/pertinencia-y-proteccion-contenido-v1.md).

Bloque M1 funcional: acreditación explícita del principal histórico de REQ; pertinencia de COM por Caso y materia; valoración sucesiva por profesional; protección documental y publicación específica de consulta por materia; listados, descarga, visor e interfaz conectados. No es cierre de U1/U2/U6 ni habilitación nominal. Falta aún la regresión completa y publicación de este nuevo bloque; se registrarán al cerrar el hito, sin atribuirle la CI del padre.

## Reglas y decisiones

Cobertura parcial de ORG-07/08/12/13, CON-04/05, ACC-05/09/10, ING-02, DOC-01/02/04/07 y AUD-01/02/04. El principal no se infiere del primer Caso/creador. El registro firmado 1.3 es una evolución explícita de 1.2 y acredita contexto y vigencia mediante respaldo; no transfiere titularidad por configuración. Un cambio de ámbito de principal ya acreditado devuelve 409 hasta implementar aceptación de transferencia.

Pertinencia y valoración conservan decisiones separadas: selección de Casos del mismo REQ, contenido sí/no, fundamento e historia de ámbitos; posterior valoración contextual enlazada a su predecesora. No modifica estado de Caso, REQ ni original. Una selección vacía retira pertinencia; los ámbitos anteriores no reviven al volver a su materia/área. Las rutas históricas excluyen recursos gestionados por la decisión nueva y no permiten reutilizar una valoración antigua para una reapertura incompatible.

Documento y referencia son recursos distintos de COM/REQ/Caso. La política se aplica en metadatos, historial, listado, contenido, original, HEAD, Range, representación y páginas. Verifica nuevamente antes del primer byte y después de preparar vistas. La proyección del formulario excluye descriptores de adjuntos no autorizados; no reescribe original, huellas o firma.

FIR documental es decisión fundada de jefatura competente: responsable actual y jefatura del propietario conservan consulta, otra área, materia o TI no la heredan. BLOQUEADO restringe consulta, no elimina ni declara cuarentena técnica. ORDINARIO no publica por sí mismo: la consulta transversal exige decisión por documento/Caso/materia, contexto, vigencia y flags independientes de contenido/descarga. Cambiar protección invalida publicaciones previas; nunca se reactiva una concesión anterior por desclasificar o volver al ámbito previo. La suspensión puede retirarse sin reactivar la materia del catálogo.

La propiedad de un nuevo documento de gestión se registra en su carga autorizada. El histórico necesita evidencia inequívoca de creación; para original ciudadano se exige principal acreditado, jefatura del contexto y lectura legítima del origen. Si faltan esas facultades no se fabrica un propietario para facilitar publicación. La pertinencia de COM no publica sus adjuntos.

Se reutilizan núcleo, identidad, concesiones U1.5, guardas y custodia/AV/visor existentes. No se introduce otro servicio. Comandos guardan proyección, historia y evento juntos; idempotencia ligada a actor, recurso y payload. La guarda de identidad precede bloqueos ordenados de COM también en adaptadores documentales históricos.

## Persistencia y preservación

V15.5 H2/MySQL: ocho tablas nuevas y un índice de valoración contextual. Se conserva el esquema previo V1–V15.4 sin cambios; no hay outOfOrder. Las tablas de historia son acumulativas. Solo la proyección vigente de destinos de pertinencia se sustituye, en la misma transacción que su snapshot histórico. Sin semillas nominales, retroaprobaciones o pérdida de originales.

Main remoto y referencia U4.5 consultados en G0 `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`; no se publicó ref remota U4.5. Esto no acredita cambios locales no publicados de ese otro worktree. Antes de integrar deben coordinarse versiones pendientes; no se tocó su directorio.

QA/históricos/U4/COM-000001, JAR activos, Formulario, WSL/HTTP_503, servicios y proveedores reales quedan fuera de las operaciones. Transporte 1.0/1.1, firmas Ed25519, recibos y COMMIT→ACK conservados.

## Evidencia focalizada M1

- Selección `focal5.log`: 28/28 H2/MockMvc, sin omisiones/fallos/errores: núcleo nuevo, API, migración 15.4→15.5 y regresiones documentales/de autoridad.
- Selección reforzada `focal6.log`: 20/20 H2/MockMvc, sin omisiones; incluye 11 pruebas de contenido, expiración/suspensión, streaming antes del primer byte, rechazo de transferencia por configuración y auditoría/concurrencia.
- Frontend completo 91/91; seis tests nuevos de protección/pertinencia/consulta y visor. Lint correcto y build correcto después de corregir un valor temporal nulo del fixture TypeScript (`front-build4.log`). No se cambia el contrato para aceptar un fixture inválido.
- MySQL 8.0.46 Windows nativo descartable: **12/12**, sin omisiones/fallos/errores. Instancia `siger-autoridad-mysql-58c1640097fb4e0fae872f32b5d26cdc`; nuevas bases UUID, loopback/propiedad de listener y cierre del helper. Actualizaciones V14/15.1/15.2/15.3/15.4; circuito de concesión y consulta, pertinencia mixta, separación contenido/descarga y FIR con bytes conservados. No es batería H2 completa sobre MySQL ni MySQL operativo.
- Primeras pruebas detectaron rollback marcado al filtrar denegaciones y una redistribución incorrecta del fixture mixto. Se corrigió código y fixture sin retirar controles. Las corridas fallidas se conservan; no se presentan como aprobadas.
- Dobles de AV/inspección/renderer identificados en fixtures. H2, MockMvc y DOM no son E2E físicos ni calificación de workers nativos.

Evidencia privada bajo LOCALAPPDATA `siger-pertinencia-49b422b77ecc46259590b3c58d7cda1f`; no se publican sobres, configuración o logs completos. La regresión completa backend/frontend y CI del HEAD final se registrarán al cierre del bloque.

## Ampliación M2 y revisión conjunta

Responsable actual puede restringir consulta de su documento sin obtener publicación o desclasificación. La jefatura conserva la facultad de revisar la condición. Se conectan controles limitados en UI y servidor. Un histórico necesita evidencia inequívoca; el modelo no inventa asociaciones ni propietarios para mostrar un botón.

Original ciudadano protegido se prueba con entrega 1.1 y recibo Ed25519 sintéticos, custodia NTFS real en directorio descartable y HTTP simulado. La proyección excluye tanto descriptores como IDs de adjuntos; GET/HEAD/Range aplican la política independiente. Se conserva exactamente la entrega firmada. Los dobles documentales están identificados, sin ensayo físico de proveedores/recorrido público.

Se prueban revocación de concesión y sesión vigente, retirada durante streaming, valoración concurrente, rollback de pertinencia si falla historia y cuerpos incompletos sin efectos. Las revalidaciones por bloque no se cuentan como aperturas/descargas nuevas. La auditoría registra solicitudes y rechazos técnicos, no comprensión. Los avisos nuevos son internos, genéricos por Caso y revalidados; no activan Graph ni publican nombres/documentos.

Selección final de desarrollo M2: **30/30 H2/MockMvc**, sin omisiones/fallos/errores (`focal-validacion.log`). Frontend **92/92**, lint/build correctos (`front-m2.log`, `lint-m2.log`, `front-build-m2.log`); una aclaración de representación autorizada posterior se verifica también en el build completo. MySQL M2: **12/12**, sin omisiones/fallos/errores, instancia nueva `siger-autoridad-mysql-52955c13a0dc4b67907b033f60e94afe`; incluye original ciudadano firmado, restricción por responsable y comparación del original conservado.

El auxiliar descarta MYSQL_PWD heredado, redirige MYSQL_TEST_LOGIN_FILE a una ruta privada del ensayo y fija protocolo/host/puerto/usuario del cliente. El mecanismo de login-path está documentado por [MySQL 8.0](https://dev.mysql.com/doc/mysql-installation-excerpt/8.0/en/environment-variables.html); no se inspecciona ni modifica el archivo compartido. Se conservan la comprobación de propiedad de listener/datadir y cierre exclusivamente propios. Un comando genérico de edición fue rechazado antes de ejecutarse por revisión automática; se aplicó un diff explícito del script y se validó con el ensayo aislado, sin cambiar infraestructura.

La matriz conserva 138 reglas; este hito revisa 17 filas con evidencia y límites de FIR documental/pertinencia. El bloque no declara completas las reservas de bitácora, exportación, reasociación integral, reaperturas o U2. PR dependiente exclusivamente de PR #2; no se acumulan U2–U9. No se acredita downgrade seguro a un binario que desconozca la política nueva. Falta calificar rendimiento documental en la VM destino; las comprobaciones por bloque priorizan revocación efectiva y no sustituyen esa calificación.

M1 confirmado en `a266693c2b845801aecef9f9ae4176577d075838`. M2 conserva V15.5 sin modificar; H2/MySQL iguales, SHA-256 del checkout `88401f11d316d4b7a36859c13affcaed8be9b79667799943cf7792fc64f3a4a7`. Logs privados: focal `ca137fd56d568053decd0385e49a6230c33e8b216d35d2948b0bf00808bfdaf6`; MySQL M2 `747d3a417ac48b4eae2e08747cf1ee082c6db5958cd7e60bd9556dceef988272`. Procesos y listener propios finales: cero. La regresión completa incorpora además la aserción de que una descarga con varias revalidaciones conserva un solo evento de solicitud y un rechazo al retirarse permiso; no se sustituye esa corrida por los focalizados.

## Dependencias pendientes

FIR de actuaciones/bitácora completa, reservas adicionales y excepción conjunta ACC-10 requieren su alcance y autoridades; no se habilitan con concesión ordinaria. Colaboradores, reapertura con valoración nueva, transferencia con aceptación, SOL institucional/nacimiento U2 de REQ, fuente maestra/SSO/MFA y datos nominales continúan pendientes. No se inicia una etapa amplia U2 dentro de este PR.

## Preguntas institucionales

1. Para habilitar datos reales: catálogo, principal histórico, autoridades y respaldos por el canal institucional privado.
2. Para excepciones sensibles: habilitaciones de Gestión y jefatura competente y alcance de reservas PE-03; mantener cierre hasta acreditarlas.

