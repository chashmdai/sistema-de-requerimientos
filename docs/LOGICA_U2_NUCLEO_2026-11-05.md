# SIGER · U2 núcleo funcional institucional · 05/11/2026

## Corte, fuentes y dependencia

Continuación de PR #3 aprobado en `c07a420e821973e68e4c52fd372ccc7df58f1656`, en el worktree exclusivo `C:\req\worktrees\siger-logica`. Rama nueva `feature/siger-u2-nucleo-institucional`, base `feature/siger-pertinencia-documental`. PR #2 y #3 se conservan en borrador y no se modifican. Main continúa en `95cf2ae8173b4c6cab3eb8a1531659b8459dd898`.

Rector local íntegro SHA-256 `80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4`, especialmente §§03/04/05/06–12; no se publica el documento de Descargas. Se reutilizan diagnóstico/matriz de 138 reglas, contratos U1/pertinencia, AGENTS y traspasos H2/U4.4. No se repiten U0–U1 ni se inicia U4.5. Contrato de este corte: [núcleo institucional U2](../contratos/nucleo-institucional-u2-v1.md).

## Resultado funcional y reglas

Este corte implementa recorridos nuevos de COM a SOL/REQ, con cero o varios CAS y colaboración aceptada. **No declara U2 completo ni convierte la semántica de las filas históricas por inferencia.**

| Capacidad conectada | Reglas con avance | Evidencia principal |
|---|---|---|
| COM manual/pública preservada; ingesta no crea agregados de gestión | ING-01/03/06/07, SOL-01 | `NucleoU2Test`, recepción firmada existente, `AreasHttpTest` |
| Primera atención propia → COM + REQ finalizado sin Caso, respuesta real e idempotencia atómica | ING-03, SOL-02/03 | `primeraTelefonicaCreaComYReqCerradoSinCasoEsIdempotente`, interfaz de primera atención |
| SOL distinta de U5, un destino vigente y revisión/historia; distribución no crea REQ | DIS-01/02/05/06/08, SOL-01 | Pruebas web, redirección y concurrencia de asunción |
| Jefatura asume/asigna SOL y nace REQ sin CAS; ingreso propio de jefatura sin circuito artificial por central | SOL-01/02, DIS-02 | `jefaturaAsumeSuPropioIngresoSinDistribuidor`, HTTP autenticado/CSRF |
| Principal unidad/sede/área explícito, jefaturas vigentes y encargado de atención | CAS-02, ORG-01/09 | Contexto persistente independiente del primer Caso |
| Atención profesional sin Caso, actuación/respuesta y cierre explícito | SOL-02/03/04, CAS-07 | Prueba web → asunción → REQ terminado con cero CAS; DOM de REQ |
| N CAS de objetivos propios sobre el mismo REQ; gestión y responsabilidades por ámbito | SOL-04, CAS-01/02/03/04, ORG-08/09 | Dos Casos, responsables, objetivos y un solo REQ |
| Complejidad BAJA/MEDIANA/ALTA y semáforo solo en REQ U2 | CAS-05 | Complejidad global; bloqueo de clasificación/descripcion provisional en nuevo Caso |
| Cierre ordinario nuevo CAS con revisión de otra persona; REQ no termina al cerrar el último Caso | CAS-CIERRE, CAS-07 | Cierre de ambos Casos seguido de cierre global explícito; alias y carreras |
| Colaboración propuesta, aceptación con CAS atómico, rechazo y retirada; principal permanece | DIS-06, EQU-03, CAS-02/07 | Caso paralelo UPD, sin asignación unilateral ni REQ nuevo |
| Consulta regional por materia sin permiso de gestión ni lectura global COM/REQ | ORG-01/05/07/08, AUD-01 | `U2AreasTest`, suspensión de materia y conservación de gestión propia |
| Original/documento no heredado por compartir REQ/CAS; documentos propios continúan | ACC-03/05/09, DOC-01/02/04/07 | Original ciudadano 1.1 custodiado y Caso paralelo en otra área |
| Principal nacido en U2 reutilizado para pertinencia, sin falsificar registro firmado anterior | CON-04/05, ORG-12, ING-07 | `principalNacidoEnU2DecidePertinenciaSinReinterpretarRegistroFirmado` |
| Distribución propone vínculo por folio exacto y metadatos mínimos; no elige CAS | CON-04, ING-07 | Referencia mínima, API de propuesta, DOM sin consulta de expediente |

La matriz mantiene 138 IDs y su orden; 44 filas se actualizan en este corte. Cada fila actualizada conserva su evidencia previa y agrega el alcance U2; las reglas compuestas continúan parciales cuando quedan acuses, protocolos, catálogos reales u otras transiciones pendientes.

## Cinco recorridos de aceptación

| Recorrido | Resultado desarrollado y comprobable | Capa de acreditación |
|---|---|---|
| Teléfono simple → COM + REQ finalizado sin CAS | Escritura atómica, original verificable, respuesta efectiva, reintento sin duplicado; TI por sí solo no responde | H2, MySQL descartable y formulario DOM |
| Web → SOL única → asunción jefatura → REQ sin CAS | Ingesta firmada sintética sin agregados; distribución genera solo SOL; asunción crea REQ activo, atención y cierre sin Caso | H2, MySQL descartable, MockMvc con sesiones y CSRF |
| REQ existente → varios CAS | Dos objetivos y Casos distintos, un REQ, sin semáforo de Caso; cierres independientes y cierre humano del REQ | H2 y MySQL descartable |
| REQ regional principal + consulta UPR/UPD por materia y gestión independiente | Principal regional persistente; materia/consulta específica, Dirección en solo lectura, otra materia y raíz COM/REQ denegadas; suspensión no retira gestión profesional independiente | H2; pruebas generales de materias también en MySQL descartable |
| Redirección SOL no asumida | Misma SOL y dos destinos históricos, uno vigente; no REQ, sin falsa transferencia; anterior destino pierde acceso por esa vía | H2 y MySQL descartable |

Los recorridos usan identidades, mandatos, firmas, sedes y materias **sintéticos**. No son E2E físicos de navegador/servicios desplegados ni acreditación de MySQL operativo, Graph, ClaveÚnica o PISEE.

## Arquitectura, persistencia y autorización

`NucleoU2` contiene las transacciones de aplicación y `NucleoU2Controller` su API. Reutiliza identidad/concesiones U1.5, guarda de identidad, bloqueo COM, comandos idempotentes y gestión/custodia de Casos existentes. No se añaden servicios permanentes ni motores paralelos.

V15.7 H2/MySQL agrega `institution_sol`, `institution_req`, `institution_case`, `institution_collaboration`, `institution_flow_history`, índices y `principal_origin` en historia de pertinencia. Los REQ usan `core_requirement`; los CAS usan `core_case`, área, materia, asignación, actuaciones, documentos y revisión existentes. Las filas institucionales distinguen explícitamente el circuito nuevo; `reevaluate` no calcula ni reactiva un REQ U2 a partir de hijos.

La SOL mantiene el destino vigente por puntero único; redirecciones conservan filas anteriores, clasificación, actor, fundamento y revisión. La aceptación de colaboración no puede quedar aceptada sin Caso. El padre se bloquea también al abrir Caso o cerrar REQ, evitando terminar con REQ cerrado y nuevo Caso abierto en la misma carrera. Un fallo de historia revierte el nacimiento y permite repetir la misma clave.

Listas/búsquedas/conteos COM U2 usan el mismo predicado de ámbito que el detalle. Antiguos destinos no otorgan acceso por historia. Compartir REQ no concede detalle CAS, original, documentos ni FIR. La jefatura principal ve estados mínimos para coordinar; el encargado profesional recibe solo Casos que gestiona. El registrador conserva consulta del ingreso propio según §05.4/ING-02, sin obtener gestión ajena. La proyección ordinaria completa de avances para registrador queda parcial.

Mandatos firmados 1.0–1.3 y concesiones conservan su interpretación. El principal U2 nace de una decisión de jefatura/primera atención real, guarda su origen y revisión de autoridad, y no fabrica una acreditación retrospectiva 1.3. La historia de pertinencia diferencia ese origen de `REGISTRO_1_3`.

## Sustitución del funcionamiento provisional

- Distribuir ya no ofrece una colección de destinos en la interfaz ni crea REQ.
- Las rutas públicas anteriores de distribuir, responder/descartar COM, abrir Caso desde destino y resolver/finalizar destino devuelven 409 y remiten al circuito institucional. El registro manual antiguo acepta solo ingreso pendiente; la respuesta se tramita por primera atención.
- Se preserva el reintento exacto de un comando histórico confirmado: devuelve su resultado después de verificar actor/acción/recurso/hash y bloquear la fila. Nunca ejecuta trabajo nuevo; clave nueva o payload alterado permanecen rechazados. La clausura de rutas no destruye la idempotencia anterior.
- El nuevo CAS exige REQ previamente creado, objetivo y responsable. No usa complejidad provisional/BAJA para autorizar cierres.
- Derivación y reapertura históricas se rechazan para nuevos Casos U2 hasta implementar su circuito correcto.
- Los componentes internos y pruebas del laboratorio conservan sus cortes históricos como compatibilidad. Las filas antiguas no se convierten en principales, colaboraciones aceptadas o nuevas clasificaciones. Su conciliación y las transiciones provisionales de Casos anteriores **no se acreditan como U2**.
- U5 histórico continúa separado y responder/canalizar/escalar permanece 409. No hay habilitación por coincidencia del nombre Solicitud.

## Validación y evidencia

Código funcional confirmado: `1ddcc20d3d28b5d6ac2c9a99c7e6b7c64c14c1a1`.

- **H2/MockMvc focalizadas: 39/39** (16 dominio U2, cinco áreas/contenido, 18 HTTP). Cierre de compatibilidad **24/24** (17 dominio y siete HTTP), con selecciones solapadas; no se suman como casos únicos. Actualización de DTO de navegación comprobada además en el pase MySQL final.
- **Frontend final 99/99**, lint y build correctos. DOM comprueba registro pendiente/primera atención, destino único y área, REQ sin CAS, propuesta por folio sin cargar expediente y navegación COM→SOL/REQ.
- **MySQL 8.0.46 Windows nativo descartable: 27/27** en la selección integral de migración/autoridad/áreas/materias/contenido/U2; siete rutas V14/V15.1…V15.6→V15.7. Tras ajustes finales, **17/17 U2 sobre código final**, sin omisiones: primera atención profesional/administrativa, sedes captura/principal distintas con facultad expresa, SOL/redirección/asunción, CAS paralelos, colaboración, rollback, carreras, referencias mínimas, conteos y reintentos confirmados. La selección intermedia 16/16 también se conserva, sin contarla como evidencia única adicional.
- Instancias, datadirs, puertos loopback, credenciales y esquemas UUID propios; binario con `--no-defaults`, comprobación de propiedad y cierre. Se verificaron **cero procesos propios restantes**. No acredita MySQL operativo. DDL H2/MySQL idéntico SHA-256 `f08bce31fb609fa88eb188d95141cef1c66137e2ab3c2f6db266339717d13136`.
- La regresión local integral intermedia ejecutó **424 pruebas, un fallo de expectativa histórica, cero errores y nueve omisiones**. No se presenta como aprobada: la ruta provisional ya estaba cerrada; se actualizó su prueba para exigir 409/ninguna escritura, conservando autenticación/CSRF y agregando el reintento confirmado. El cierre focalizado posterior aprobó; los resultados íntegros fallidos se conservan.
- Empaquetado final correcto, actualizado con el frontend final, después de las selecciones anteriores. El pase de empaquetado no sustituye pruebas ni acredita un despliegue.
- **Regresión completa del HEAD definitivo en Windows/Ubuntu: cierre remoto posterior en PR #4**, con SHA/run/jobs/totales concretos. Este informe no atribuye al hijo la CI del padre ni convierte una ejecución fallida en verde. La entrega solo se cierra tras comprobar ambos jobs del HEAD publicado.
- No se ejecutaron E2E físicos ni workers/proveedores reales. Nueve avisos npm moderados heredados, sin actualización masiva.

Las pruebas no se excluyen para conseguir verde. Los tres stress históricos y omisiones de workers/plataforma se mantienen. AV/renderer e inspecciones con dobles se distinguen de workers reales. La CI del padre no se atribuye al nuevo HEAD.

Fallos de desarrollo conservados: invocación local inicial con temporales heredados rechazados por ACL; repetida por el mecanismo privado de build, sin debilitar custodia. Se corrigieron normalización JDBC/JSON H2, contexto documental sin Caso, alcance de documentos de gestión propios, expectativas antiguas de rutas sustituidas y fixtures DOM de navegación. Un ensayo focalizado utilizó límite 100 no admitido por la API paginada y se corrigió a 20. No se cambiaron timeouts, exclusiones, POM, dependencias, workflow ni controles de custodia.

## Cambios y trazabilidad Git

Backend: nuevo servicio/controlador U2; contratos de Caso; guardas en núcleo/consulta y adaptador de contenido; migraciones nuevas. Frontend: módulos SOL/REQ/primera atención/colaboración/referencia, navegación, captura pendiente y controles del nuevo Caso. Pruebas: dominio/seguridad/carreras/rollback, áreas/documentos/materia/alias, HTTP real con sesión/CSRF, evolución desde V15.6 y DOM. Auxiliar MySQL ampliado con esquemas propios y selección completa/focalizada, verificando resultados reales.

El cierre remoto del commit que contenga este informe se registra posteriormente en **PR #4** y manifiesto de evidencia privado: HEAD íntegro, upstream, árbol limpio, run/jobs Windows y Ubuntu y sus totales. Así no se atribuye al informe una CI anterior ni se introduce autorreferencia del hash del propio commit. Publicación normal, PR en borrador con base PR #3. Sin merge, auto-merge ni despliegue.

## Compatibilidad operacional y límites

V1–V15.6 intactas, sin outOfOrder; versiones publicadas inspeccionadas en main/PR #2/PR #3 y referencia conocida U4.5, sin colisión observada con V15.7. No hay referencia remota U4.5 publicada; sus cambios locales no publicados requieren coordinación antes de integrar. No se accede a su worktree ni se modifica su rama.

No se acredita downgrade a binarios que ignoren los agregados/predicados U2. El esquema conserva datos, pero una aplicación antigua no implementa la nueva autorización.

QA histórica Windows/WSL, `siger_institucional_windows_lab`, `siger_u4_ddn_ubuntu`, COM-000001 y evidencia, `C:\req\bin`, JVM/servicios, Formulario Público, Graph/CU/PISEE y HTTP_503 WSL no intervenidos. Listados SOL/REQ nuevos acotados a 100 entradas; el buscador COM paginado existente y sus enlaces institucionales permiten localizar registros anteriores por folio dentro del ámbito autorizado. Paginación propia SOL/REQ pendiente. Compilaciones y bases de prueba nuevas/descartables; auxiliares propios se cierran sin alterar MySQL80, firewall, red o servicios compartidos. Originales, huellas, folios, firmas Ed25519, idempotencia y COMMIT→ACK conservados.

## Pendientes y próximo bloque

1. Cobertura territorial acreditada por domicilio utilizable. No se utiliza la tabla de cobertura técnica histórica para habilitarla. La asunción regional con ese antecedente queda 409; cuando falta antecedente utilizable se conserva decisión fundada de jefatura. No se inventan equivalencias reales de regiones/sedes.
2. Conciliación explícita de REQ/CAS históricos y sustitución de sus transiciones provisionales, con principal respaldado y sin inferencia del primer Caso. Altas U2 sobre COM con gestión provisional previa permanecen cerradas.
3. Transferencia principal origen solicita/destino acepta, derivación formal que no cierre automáticamente origen y reapertura U2; no se confunden con redirección o colaboración.
4. Acuse separado, integración TAR-09, trabajo colaborativo de equipo local, avisos específicos SOL/REQ y seguimiento ordinario del registrador. La responsabilidad actual es efectiva; no se simula el acuse.
5. Clasificación tipada completa, invalidación/duplicados por jefatura y captura especializada flexible. No se declara finalizada la totalidad de §06 por disponer de relato/respuesta y canal.
6. Valoración estructurada de COM hijas en REQ sin Caso; la pertinencia a Casos existentes sí reutiliza el nuevo principal. Documentos/bitácora especializados sin CAS requieren su ampliación específica.
7. FIR de actuaciones, reservas reforzadas/excepciones PE-03 y autoexcepción de titular regional: siguen cerradas hasta controles específicos y respaldo. FIR documental anterior se conserva.

Próximo bloque recomendado: revisar e integrar esta cadena apilada y continuar U2 con conciliación histórica y territorio acreditado; después, transferencia/derivación/reapertura explícitas y acuses. No acumular U3–U9 en este PR ni asumir aprobaciones reales pendientes.

## Preguntas institucionales al habilitar datos reales

1. ¿Qué catálogo y respaldo institucional acreditarán domicilio→región/sede y sus vigencias, sin confundirlo con territorio de hechos o cobertura técnica?
2. ¿Qué decisión y evidencia se utilizarán para conciliar cada REQ/CAS histórico de laboratorio y acreditar su principal sin deducirlo del primer Caso?
3. ¿Qué titulares/delegaciones y procedimientos específicos se acreditarán para las excepciones regionales y reservas extraordinarias pendientes? Hasta entonces no se habilitan.
