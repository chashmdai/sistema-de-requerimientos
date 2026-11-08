# Continuidad autónoma SIGER · 06/11/2026

## Instrucción vigente y preservación

Levantamiento y decisiones funcionales validados por el responsable06/11/2026: DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. No volver a pedir esa validación; parámetros nominales ausentes requieren configuración, no inventar permisos/personas/coberturas. No equivale a código completo o autorización operacional.

Continuar bloques independientes mientras haya capacidad. Cuando las138 estén implementadas/verificadas o tengan una limitación concreta, dejar funciones nuevas y realizar revisión transversal de código/clases/dependencias heredadas, retirada controlada de rutas provisionales incompatibles, consolidación y candidata integrada/compilable con informe138 y escenarios manuales. Todavía hay capacidades programables; no convertir pendientes técnicos en impedimentos externos para anticipar ese corte.

Único worktree C:\req\worktrees\siger-logica. No modificar main/padres, U4.4/U4.5/SMR/QA, COM-000001, históricos, JAR/servicios/Formulario/proveedores/credenciales, originales/firmas/hashes o COMMIT→ACK. Sin merge a main, deploy, correo real o llamadas operacionales. No detener procesos ajenos, usar bases preservadas, force/reset/clean destructivos.

## Estado actual exacto y relevo — 07/11/2026

**Instrucción vigente del responsable: última tarea antes del relevo. No desarrollar más funcionalidades, no repetir pruebas, no abrir PR, no hacer merge ni despliegue. Verificar y publicar la rama de recuperación de plazos y dejarla sincronizada.** Esta instrucción sustituye las próximas acciones antiguas de este documento. Los apartados históricos inferiores conservan evidencia de sus respectivos cortes, no describen el estado actual.

### Git y publicación de recuperación

- Worktree exclusivo: `C:\req\worktrees\siger-logica`.
- Rama actual: **`codex/siger-plazos-registro`**. Commit funcional WIP íntegro: **`54b31ff28dad53d3bcea330ea7e3d6cd9ee4b015`**, padre **`25678fd5fae42fb0cb08d6f250640983f9269929`**. Son 37 archivos, 804 inserciones y 37 eliminaciones; no se reconstruyó ni descartó trabajo.
- Al recuperar: rama PR23 en `448ba6a4e496c9cec93b145f962e0387221ea892`, árbol limpio, sin cambios indexados ni archivos sin seguimiento. Cambio a la rama WIP con árbol limpio; esta seguía exactamente en `54b31ff`, también limpia. No había modificaciones locales adicionales que preservar.
- Esta entrega añade **únicamente este checkpoint** mediante un commit hijo directo de `54b31ff` y publica la rama por push normal con upstream **`origin/codex/siger-plazos-registro`**. El SHA de entrega es el commit que contiene esta sección: obtenerlo con `git rev-parse HEAD` y contrastar con `git rev-parse origin/codex/siger-plazos-registro`; exigir igualdad, `git rev-list --left-right --count 'HEAD...@{upstream}'` igual a `0 0` y `git status --porcelain=v1 --untracked-files=all` vacío. El commit `54b31ff` permanece como ancestro recuperable.
- **Sin PR de plazos, sin CI propia, sin calificación integral de este HEAD.** El push de esta rama sin PR no dispara el workflow actual (push limitado a main; también pull_request y workflow_dispatch). No lanzar CI ni pruebas para este relevo documental.
- No se integró el correctivo CI del PR23: `448ba6a` y `54b31ff` eran dos hijos distintos de `25678fd` (divergencia 1/1 antes del commit documental). Los borradores de CI incluidos en `54b31ff` **no son** el correctivo definitivo acreditado. No sobrescribir la rama PR23 ni atribuirle V15.22.

### Cierre PR23 conservado y acreditado

PR23 continúa abierto **en borrador**, sin merge: [acta](https://github.com/chashmdai/sistema-de-requerimientos/pull/23). HEAD **`448ba6a4e496c9cec93b145f962e0387221ea892`**, [CI42424511650](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42424511650) **ocho jobs aprobados sobre ese SHA**. Backend:109 raíces/776 métodos por OS; Windows767 aprobados/9 omitidos heredados, Ubuntu752/24, cero fallos/errores. Frontend234/234 por OS, lint/TypeScript/Vite y JAR integrado con3 archivos idénticos SHA256. Windows1 terminó26:00, presupuesto30min intacto. Esta evidencia pertenece exclusivamente a448ba6a, **no** a la rama de plazos.

Acta pública y captura privada `LocalAppData/siger-build-4c652bfd9b7a41d5a8e0d659e180202d/ci-pr23-cierre-final.json`,7195bytes,SHA256 **32F0C8A5532D0B91550AE0CBC1D3901CC13E9874FD6D6711A9FBE62F7EFF3E62**. Captura releída durante este relevo: HEAD/run coinciden y todos sus jobs constan success; no se repitió CI. Informe versionado en la rama PR23: `docs/LOGICA_CIERRE_CI_PR23_2026-11-07.md`; no está incorporado todavía a esta rama WIP.

Conservar intento fallido previo **42406996840**, HEAD25678fd: cinco particiones verdes, Windows1/job128506723800 cancelada a30min,278 métodos/0fallos/0errores/9omitidos, cobertura incompleta y agregación omitida. Log privado `ci23-windows-part1-25678fd-interrumpida-completa.log`,2354382bytes,SHA256 **46390364D60EFE6E4323D95A6602CEA16B2D4EF274837F56C4851EC03A087237**, misma campaña LocalAppData. El cierre posterior no borra esta evidencia.

Correctivo definitivo448ba6a: LPT determinista con109 raíces compiladas y pesos observados; estimación previa32.003/26.643/13.709min, nueva24.121/24.117/24.117min. Manifiesto3, inventario completo/disjunto, huella y XML revalidados;21 controles negativos, clase nueva conservada y orden determinista. Ocho jobs, frontend una vez por OS, cobertura, exclusiones, memoria y timeouts funcionales intactos. El borrador WIP no contiene todas estas correcciones: al retomar desarrollo deberá reconciliarse expresamente con el padre acreditado, conservando ambos historiales y las guardias MySQL de plazos. **No realizar esa integración durante este relevo.**

### Implementado parcialmente en54b31ff — no declarado completo

- Registro firmado explícito **1.10** compatible con raíces anteriores, catálogos versionados de calendario/política de registro48h. `CatalogoPlazos`, `MotorPlazos`, `FuentesRegistro`, `PlazosRegistro`/Controller y `VersionEsquema`.
- Cálculo configurable CORRIDAS/HABILES con zona horaria y límites DST;48h es obligación de registro, no plazo de resolución. Sin calendarios nominales inventados ni parámetros efectivos por defecto.
- Fuentes inmutables anexadas dentro de transacciones reales de COM manual/pública, bitácora Caso/REQ, primera atención y rectificación. Fechas efectivas y persistidas separadas, historia conservada, sin backfill de históricos ni alteración de originales/firmas/COMMIT→ACK.
- API/UI conectadas de consulta y medición en COM, REQ y bitácora de Caso; catálogo TI consultable. Ámbito por recurso, cuenta efectiva/CSRF, idempotencia, vigencia y revalidación de actor/política al COMMIT. Historial de mediciones no equivale a nuevas actuaciones ni concede acceso a expedientes a TI.
- **V15.22** H2/MySQL aditiva,36 migraciones en esta rama. Ambos archivos `V15_22__plazos_de_registro_acreditados.sql` tienen SHA256 **68633D3B8448522AF9CA82A9883975BCEC337926ED24AF13B48A2FE0A0000658**. Ya aplicada solo a H2 descartable: **no editarla**, no habilitar outOfOrder. V1–V15.21 conservadas. V15.22 no está en PR23.
- Auxiliar MySQL con selectores/guardias de plazos y expectativas de autoridad/upgrade22 preparados. No se ejecutó MySQL de plazos. No hay configuración nominal institucional cargada ni habilitación operativa.

### Pruebas ya realizadas — conservar, no repetir durante el relevo

Evidencia privada en `LocalAppData/siger-build-4c652bfd9b7a41d5a8e0d659e180202d/`:

- Compilación inicial correcta. Intentos de fixture sin compilar por nombre de método y luego getter privado: corregidos usando API real/lista TI autorizada; no cuentan como pruebas ejecutadas.
- Primer H2:19 ejecutadas,9 aprobadas/1 fallo DST de fixture/9 errores por perfil sintético sin sede de captura. Perfil corregido mediante APIIdentidad real, sin bypass; DST corregido para probar límite realmente ambiguo, sin debilitar aserciones.
- **Correctivo13/13 H2**,01:31:12 dominio de `PlazosRegistroTest` y1 DST de `MotorPlazosTest`, cero fallos/errores/omitidas. Log `plazos-focal-h2-perfil-actual.log` (85787bytes). Otros6 métodos puros habían aprobado en el primer corte; no sumar repeticiones como una suite nueva. Fuente/política/medición, revocación, concurrencia, rollback y reloj que avanza cubiertos en esos cortes.
- **8/8 DOM**, log `plazos-front-focal.log` (333bytes). Lint/build aprobaron antes del último ajuste de actorId: **no acreditan el estado final**. No hay regresión integral ni CI de plazos.
- Durante este relevo solo se releyeron los resúmenes existentes (H2 BUILD SUCCESS/13 y DOM8); **no se ejecutaron pruebas, builds, migraciones ni proveedores**. H2/DOM no equivalen a MySQL ni a E2E físico/manual.

### Revisión de publicación y pendientes

Revisión local de los37 archivos completos modificados respecto de25678fd: sin coincidencias de claves privadas, tokens de proveedor, JWT, credenciales literales/URLs con contraseña ni literales de alta entropía candidatos; sin archivos de secretos, claves, .env, client.cnf, logs o binarios añadidos. No se imprimieron valores sensibles ni se añadieron evidencias privadas. `git diff --check` limpio. Esta revisión de publicación no constituye auditoría transversal de seguridad ni calificación funcional.

Pendientes técnicos reales de plazos:

1. Reconciliar el correctivo CI definitivo448ba6a con esta rama sin perderWIP ni alterar PR23/padres; cinco archivos CI relevantes: Build-Siger.ps1,CiTestAllocation.ps1,ci-test-duration-hints.json,Test-CiCoverage.ps1,Test-CiCoverageHarness.ps1. Algunos ya coinciden; revisar diffs, no copiar todo el WIP al padre. Conservar Test-AutoridadMySql.ps1 de plazos y evidencia de cierre.
2. Completar upgrade21→22 y compatibilidad de firmas1.7→1.10, HTTP/session/CSRF/actor y regresiones de primera atención, rectificación y recepción pública en transacciones reales. `PlazosMigracionTest` todavía no existe; el auxiliar no prueba una clase futura por mencionarla.
3. Validar persistencia/migraciones en MySQL nativo descartable y aislamiento, sin tocar bases preservadas. Revisar expectativas de migración de pruebas históricas, sin bypasses ni exclusiones.
4. Ejecutar validaciones frontend posteriores al cambio actorId y regresión proporcional; documentación/contrato/matriz de plazos, evidencia de permisos y concurrencia y CI propia al alcanzar un bloque estable. No atribuir los8 jobs de448ba6a alWIP.
5. Solo entonces considerar PR dependiente de `codex/siger-asociaciones-documentales`. **No hay PR24 ni se abre uno en esta entrega.** PLA no declaradas completas; todavía no procede auditoría final/candidata de138 reglas.

**Próxima acción precisa para el otro agente:** leer esta sección, comprobar rama/HEAD/upstream/árbol limpio y confirmar que54b31ff es ancestro del HEAD publicado. Leer el diff `25678fd..54b31ff` para recuperar únicamente trabajo existente. Al recibir continuidad de desarrollo, resolver primero la divergencia con448ba6a y después completar los pendientes técnicos anteriores; no repetir U0–PR23 ni pruebas ya acreditadas sin motivo. El relevo actual termina con la publicación y sincronización de la rama, sin desarrollo adicional.

### Cortes documentales anteriores acreditados

PR22/ef2eb1c70210037d137dc889236a9fc567c8043e CI42404716447 ocho jobs:105raíces756métodos/OS,Windows747aprobados/9omitidos,Ubuntu732/24,cero fallos/errores;frontend225/lint/TypeScript/Vite,JAR3idénticosSHA256. PR21/e57bd84 CI42399170765:101raíces735métodos/OS,Win726/9,Ub711/24,FE218. PR20/29d653a CI42381732338:716métodos/OS,Win707/9,Ub692/24,FE210. Ninguna acredita automáticamente a un hijo.

AsociacionesPR23:V15.21aditiva35migraciones,SHA2566AB06F174B4A01F74FCA6D5CC6055E7792146A3FCB0BB8F6992339BDDA295DEC. H2inicial11(9aprobadas/2conteosfixturecorregidos);focal39/39H2/HTTP. MySQLinicial49(32aprobadas/17setupallowlist),eb7373b6eeff4bc6a8b62ce54c08b034cerrada/PID0;correctivo17/17MySQLen2cbc650676b7442785318d77f70f3499cerrada/PID0,13dominio+3indirectas+1upgrade. No sumarcomo49/49. FE234con233aprobadas/1expectativaantiguacorregida,correctivo9/9DOM/lint/build;finalCI448ba6a234/234 acreditada arriba. Manual preparado/noejecutado;sinE2Efísico/AVreal/proveedores.

### Cortes locales de cuarentena conservados (historia, no instrucciones actuales)

Rama actual **codex/siger-cuarentena-documental**, creada limpia desde **e57bd847778d7bf34d72287f42491c3cdc751168** / PR #21 borrador,basePR20. PR21 publicado/upstream0-0,CI propia **42399170765** cerrada8jobs,101clases/735métodosporOS,Win726/9omitidos,Ub711/24,FE218/lint/build/JAR3idénticosSHA;logs/acta verificados,no atribuida al hijo. PR20/29d653a cerrada8jobsCI42381732338,98clases/716métodosporOS,Win707aprobados/9omitidos,Ub692/24,cero fallos/errores,FE210/lint/build/JAR3idénticosSHA;acta/logs/informeverificados. Padres/main intactos.

Cuarentena técnica desarrollada localmente,SINCOMMIT/SINcalificacióncompleta: V15.20aditivaH2/MySQL,34migraciones,holdactivoúnico/historia/comandos,sin bytesborrados. DisponibilidadDocumental comprobaciónportableFlyway20 ycachepositivaWeakMap;esquemaspreviosmantienencompatibilidad explícita,tablaausentecon20aplicadodaerrornoapertura. ServicioCuarentenaDocumental exigeTIindividualefectivo+personafirmada;aislamientoUUID/hash/ref/fundamento;liberaciónporotrapersona(noalias),integridad/AV/inspectorprivados yrevalidaciónactual alCOMMIT. Intenciones/fallosconservados,inclusosi revoca duranteworker,noprivilegioextra. Controller yconfigbeanconectados;UI CuarentenaTI/tabAdministración yavisosdeflagtecnicoenCaso/REQ/ciudadano/SOLO_DOC desarrolladosSINTODAVÍApruebasDOM/lint/build. PayloadTIopaco,sin nombre/expediente/contenido ni enlacesdescarga.

Integraciónbackend técnicoenContenidoInstitutional/REQ/AccesoReservadoSOLO_DOC ybytes/derivadas/streams,DTOprivado cuarentenaaditivo. Foco inicial **9/9H2**,01:18. Foco posterior35:34aprobadas/1error enmetadatosSOLO_DOC;causareal:concesióninstitucionalseestabareinterpretandocomoobsoletaporelbloqueotécnico. Corregido separandocálculoinstitucionalinterno(packageprivate,sinlecturabytes)de disponibilidadaplicadaatodaslasrutasdecontenido. No elevaciónTI ni asercionesrelajadas. Foco2H2encursoSOLO_DOC+regresiónconcesiónanterior(logcuarentena-solo-documento-h2). Elcorte35yaacreditó30autoridad hasta20,upgrade19→20,originalciudadanofirmadopreservado,revocaciónenworker yrollbackliberación; norepetirsinrazón.

AutoridadMigracionTestactualizadaexpectativa20/partida19/DDLidéntico;VersionesDocMigracionTestexpectativaactual20,sin tocarpadrePR21validado19. AuxiliarMySQLselecciónCuarentena yschemasaisladoscuarentena/prev/solo/publica/1519;guardiasGUIDloopback. **NativoMySQLcuarentenaNOejecutadotodavía**. V1–V15.19intactos;ningunamigraciónoperacional. CompilaciónJavaaprobóantesdeloseventosnuevos;focalrecopilaactual.

CorrecciónSOLO_DOC **2/2H2**,34s (nuevo retenido+regresiónTI1.7). Después disponibilidad global porSHA256 protegecopiaspreexistentes yrechazanuevascargasderepeticiónantes/despuésdelAV;consulta funcional interna continúa independiente,ningúnendpoint de bytesomitebarrera. ActorX-Siger-Actor obligatorio enmutacionesTI+AbortSignal frontend evitan atribuir operación pendientes a otracuenta. **3/3H2/HTTP**,37s (copias/retención duranteAV/headerCSRF/nolecturaTI/liberaciónindependiente). EsquemaV15.20noedita20trascorteaplicado;V1–19intactas.

MySQLnativoWindows8.0.46 inicial **44 ejecutadas:43aprobadas/1errorsetup** poridentificadoresquema65caracteres(sigerGUID_cuarentenapublica). Instancia8b5bec0640a241b08a987133c2f1f0bb cerrada/proceso0;tests.logconservado,noaumentotimeouts. Sufijocuarpub yvalidaciónprevia45nombres/máximo62/límite64,sinserviciosiniciadossinvalidar. Reintento focalciudadano **1/1MySQL** en1ea378b701274b0fa6a639d81949fad3,cerradopendientecomprobarPID0. Nativo2copiasSHAactualesEN CURSO,logcuarentena-huella-mysql. No sumarcomo1suite44pasada,niatribuirresultadospreviosalcódigonuevo.

Frontendfocal33:32aprobadas/1falloenmetadatossolo enDocumentoReservado porreemplazoautomáticocondiciónnegativa;corregidapreservandoaserción. Focalposterior **9/9DOM**,lint/TypeScript/Vite antesdeúltimajuste. API/UIcuarentena desarrollados: estados/lista/conteos/historia/incidentesporUUIDSHA,retener,verificaciónindependienteeincertidumbre; noarchivos/descargas/casosparaTI. Faltaregresiónfrontendintegral ylint/buildfinales. HTTPactorpinmismatchfaltapruebanegativaexplícita,positivoheaderactualya3H2/HTTPacreditados. NoE2Efísico/antivirusreal/proveedores.

MySQLfocalactuales:ciudadano **1/1** en1ea378b701274b0fa6a639d81949fad3;copiasSHA **2/2** en00120142611b4932bc5b22b7dd8b9619;I/O+cachePNGreal **1/1** en9b1c06ff3853430084c8ceb227e4c737. Logscompletosconservados,pendienteverificar0PIDspropiosenúltimos3. No sumarloscomo44/44ni repetir43quepasaronantesdelerrornombre. Nativo inicial44tuvo1errorsetup/43pasadas en8b5bec0640a241b08a987133c2f1f0bb(cerrada0PIDsconfirmado). NuevasguardasI/O obligatoriasenDocumentosGestion,Visor yDocumentoIngreso independientesdeinterfazdeautorización;TestcachePNG mejoraalcortefinalMySQL1,sinvolveracorrerbateríasnoafectadas.

**Frontendcompleto224/224**,lint/TypeScript/Vitefinales;foco9/9correcciónmetadatosy6nuevosTIincluyecambioactorabort/pin,nodebytes.DocumentoReservadocondiciónnegativacorregida,mismaserción.Focalfinal **2/2H2/HTTP**,30s,guardasI/Oyactorpinfaltante400/cambiado409 sinliberar. NuevamejorareintentosconfirmadosdeCase/REQretornanmetadatoslegítimosdeoriginalretenido(sinbytes),nootraalta;barreraSigue423paracontenido. H2focal1encurso,logcuarentena-confirmacion-h2;nativoúltimoreintento1aúnsinejecutar.Códigoantesdelcortefinalcoherente,últimotestconservado.

Reintento deconfirmación **1/1H2**,20s y **1/1MySQL**en93f62f4cb3d74b3f9b287d04390b98c1,cerrada/proceso0verificado. Anterioresinstancias1ea378b701274b0fa6a639d81949fad3/00120142611b4932bc5b22b7dd8b9619/9b1c06ff3853430084c8ceb227e4c737cerradas/procesos0verificados. CachePNG finalprobadoenMySQL1,noatribuiralvarianteTXTprecedente. ActualizaciónfrontendporCaseobjetorefresco(sinbumpdegestión)ybótonesrefreshCaso/ciudadanocierranpreviews: **23/23DOM**,lint/TypeScript/Vitefinalesdespuésde224integrales;CIdebe225exacto.

Contrato cuarentena-tecnica-documental-v1,informeLOGICA_CUARENTENA_DOCUMENTAL_2026-11-07 ymatriz138actualizados. DOC07parcial(cuarentena sí,asociaciones/purgano);DOC04/ACC09/AUD04 amplían protección. **Próximaacciónexacta:**revisardiff/guardas/migracionesanterioresinmutables,confirmar/publicar/PRborradorbasecodex/siger-versiones-documentales(PR21);CIpropia8jobsdelHEADexacto.NoMavenlocalactivo;últimoMySQLhelperterminó0,proceso0verificado. SHA20enpárrafoarriba.SeguirdespuésconDOC07asociacionesuotrosbloquesdefinidosen nuevohijo conárbollimpio,mientrasCIcorre. No138completas/auditoríafinal/candidataprematura.

### Evidencia de versiones publicada en PR21

Bloque publicado e57bd84/PR21: VersionesDocumentales+Controller y V15.19 H2/MySQL aditiva33migraciones; metadatos descriptivos declarados/corregibles con historia, fases BORRADOR/FINAL declaradas,no firma externa; sucesora original distinto con permiso sobre ambos,mismo contexto/cadena lineal,protección equivalente sin herencia; final anterior y bytes inmutables. Lectura de equipo solo metadatos autorizados,no historia/versiónFIR oculta. Interfaz VersionDocumento integrada en Caso/REQ bajo consulta diferida y backend efectivo,API nuevas enapi.ts. AutoridadMigracionTest espera15.19,parte15.18 y compruebaDDL idéntico;fixture/auxiliarMySQL nuevos schemas _docmeta/_docmetaprev con guardias exclusivas.

Pruebas locales **41/41 H2**,02:11 (12dominio+1upgrade+28autoridad),luego **41/41 MySQL nativoWindows8.0.46**,03:35,en65e3fc3604cf4c62b9ccc70f89605cc7,cerrada/proceso0;no física/AVreal. Nuevas negativasalias/histórico **2/2H2+2/2MySQL**,instanciaf6efa7d52d104d49ae6f01676727642e,cerrada/proceso0;no se suma como una sola43. HTTPnuevo1/1. Bandera finalizable explícita para no ofrecer finalizaciónposcierre: **2/2H2/HTTP+8/8DOM**,lint/TypeScript/Vitefinales. Frontendintegral217/217antesdeesabanderapequeña,CIdebe218delHEADpublicado. UIinicial23:16aprobadas/7erroresfireEvent.toggleinexistente;harnesscorregidoconclickreal,sinasercionesrelajadas. TSfallósoloportoggle,luegoaprobado. No Maven/MySQLlocalactivo.

Contrato versiones-documentales-de-gestion-v1,informeLOGICA_VERSIONES_DOCUMENTALES_2026-11-07 ymatriz138publicados. CI21propia pendiente. NoMaven/MySQLlocalactivo. Instancias65e3fc3604cf4c62b9ccc70f89605cc7 y f6efa7d52d104d49ae6f01676727642e cerradas/procesos0. Guionmanual preparado/noejecutado. No138completas/auditoríafinal/candidataprematura.

### Evidencia documental REQ publicada en PR20

Bloque documental REQ publicado en29d653a/PR20: V15.18 H2/MySQL aditiva (SHA F4C225CCBAB861E138AA9EBE7AA3F2DE02341551ECDE41F45EEE69B358A99698),32migraciones. OriginalesGestion extrae antivirus/inspección/custodia comunes sin cambiar hashes/UUID/comandos Caso. DocumentosRequerimiento/API/UI conectados: original privado de atención sin Caso, vínculos a gestión REQ real, protección/versiones optimistas/idempotencia/historia; sin Caso falso ni acceso por materia/TI/equipo. Consulta exige autoridad efectiva del principal y ámbito original, no atención de otro ámbito tras transferencia aunque tenga rol adicional. FiltradoSQL para listados. Descarga Range/HEAD/64KiB revalida permisos. Ver contrato documentos-requerimiento-sin-caso-v1.

Evidencia local separada: compilación inicial falló por clase de auditoría no pública, corregida con tabla de auditoría existente; compilación posterior correcta. H2inicial13:12aprobadas/1errorrollback al omitir bloqueado, corregido filtrando enSQL y evitando excepción transaccional anidada; foco bloqueo+upgrades autoridad27/27 H2,42s. Focal posterior22:21aprobadas/1aserción HTTP esperaba403 para operador refrescado sin ámbito; contrato404 recurso oculto, aserción corregida explícitamente (operador viejo sigue403). Mismo corte acreditó migración de documento previo, casos/alias/transferencia y3regresiónCaso, no sumar repeticiones. Focal final6/6 H2/HTTP,49s (stream revocado a64KiB,concurrencia protección,Caso del mismoREQ,bloqueo+2HTTP), previo a endurecer atención de otro ámbito. DOM39/39; frontend completo209/209,lint/TypeScript/Vite. InicialDOM1fallo porque disabled se heredaba de fieldset; se comprueba fieldset efectivo, controles intactos.

MySQL nativoWindows8.0.46 **45/45**,04:14 (18dominio+1upgradeREQ+26autoridad),32migraciones. Instancia6c9af5f7d93d47efbe352e5564392148,esquemaGUID270079004c5d44ca91fdaf8f029f6598,cerrada/proceso0. PosteriorvisorPNG **1/1 MySQL** en37c374a6a8354b4486855f835e4fca50,cerrada/proceso0;no se suman cortesni repiten45. Foco final **5/5 H2/HTTP**,37s,principaltransferidoconroladicional,PNG/vista/página/HEAD ybloqueoprofesional204+reintento. Frontend209integralantesdeajusteacotadodenegación:8/8focales+lint/TypeScript/Vitefinales;CIpropia debe210. No Maven/MySQL local activo. V1–V15.17 intactos;V15.18SHA arriba,sinoutOfOrder.

Contrato/informeLOGICA_DOCUMENTOS_REQ_2026-11-07 y matriz138 publicados;CI20propia pendiente. Guionmanual preparado/noejecutado. Nuevasfunciones solo después del árbol limpio y rama hija comprobados. No138completas/auditoríafinal/candidataprematura.

### Evidencia PR19 preservada al publicar

PR19 publicado: RegistroAutoridad1.9 explícito extiende1.8,lista SupervisorBandeja(id,actor,persona,bandeja,sede,desde,hasta,referencia,sha256,estado). WEB_CENTRAL solo origenweb;MANUAL_SEDE solo captura específica, sin inferir central por ausencia de catálogo. Raíz firmadaacredita cuenta/persona/credencial/vigencia y respaldo, no rolesTI. EstadosACTIVA/SUSPENDIDA/REVOCADA,Rootanteriorintacto. SinDDLnuevo. Contextomateriaunidad1.7–1.8preservadoen1.9. Gettercapvalidas,catálogo de sede vigente.

SupervisionClasificacion: lista/conteo/paginación filtrados enSQL por concesión exacta antes de responder;solo metadatos de tomas pendientes,noCOMrelato,SOL/REQ/CAS/documentos. Detalle conserva historia/cero revisiónCOM y todos flags de gestiónfalse,404si ninguna toma. ExtiendeEV02para supervisor nominal por permisoactual,noalias,sin accesoexpediente. Core/u2/controllerconectados. Clasifmetadata extraído para lectura, no callbackgestor. También NucleoU2.regional conservaindiciohistóricoregionalcomo restricción cuandoexpiróúltimaconcesión,evitaampliar a central; debe probarse. Capwebnoimplica manualcentral,noorginventado.

SupervisiónLocal: compilaciónproducción9s. Primer intento sin tests por importambigüoVinculo,corregido. Focal23con17aprobadas6errores(códigosauditoríalargos+perfilcapturadorBfixture);seacortaroncódigossinDDL yperfilBrealconconcesión. Posterior10con5aprobadas4fallos+1error por usar aliasUIWEBcomoSQLcanal;corregido usandoprocedencia inmutablepersistida FORMULARIO_PUBLICO_DDN,sinasercionesrelajadas. Focal9/9H2,después **13/13 H2/HTTP**,01:32(Super10+HTTP1+Documento19compat1+últimaCoordinación1). Nuevolectorcapusaunasola raíz verificada/credencialcurrent,nodeversionesmezcladas. Carrera consultaesperarevocación bajoidentityguard acreditada. Docfirmado1.7siguesinCasoen1.9ymateriaUPD/unidadUPRindependientes;históricos yconcesionesnocambian.

MySQLnativoWindows8.0.46 **12/12**,selecciónSupervision(Super10+Doccompat1+regional1),instanciab5437a655640404abcee56c2bfd52d9a cerrada/proceso0 confirmado.31migracionesV15.17,sinDDL nuevo. Logsupervision-mysql. Frontend **202/202**,lint/TypeScript/Vite;DOMfocal21/21,API/vistaSupervisionlista+detallesinacciones/App/enlace real. NuevaclaseHTTPSuper1pasada,sesión/CSRF/no-store/nogestión/revocaciónconlasmisma sesión.

Ajuste final de seguridad:actuante perdióDISTRIBUCIONysolo conservaSuper recibe ruta metadatos,noCOM. Positivoanterior+regresión **2/2 H2 y2/2 MySQL**,instanciaef6cdb6131464fb1b8bde5476b038b4f cerrada/proceso0. Lógicageneraldegrantssincambios;sourceSQLsin cambiosdesdecorte12. UI202/202antesdepresentarcuentaactuante;focalactual6/6,lint/TypeScript/Vite finales. NoMaven/MySQL local activo. Logs supervision-ruta-*,supervision-cuenta-*.

Contrato supervision-clasificacion-v1,informeLOGICA_SUPERVISION_CLASIFICACION_2026-11-07.md ymatriz138actualizados.
Contrato supervision-clasificacion-v1 e informeLOGICA_SUPERVISION_CLASIFICACION_2026-11-07.md publicados; matriz138 actualizada. Firmas1.0–1.8 preservadas, nueva1.9 explícita. Sin migración en PR19,31 migraciones V15.17. CI propia pendiente, guion manual no ejecutado.

### Cortes publicados anteriores

PR #17 /bc27f2de825f2ecfe0aa0258459a40aa4d422377,CI42372396506 acreditada8jobs,92raíces661métodosporOS,Win652aprobados/9omitidos,Ub637/24,ceroerrores/fallos;FE187una vezporOS/lint/build,JAR3archivosidénticosSHA256. Una sola ejecuciónPR,sinduplicadopush. Acta/informecerrados;Windows1Maven26:22,budget30intacto. NingúnMavenlocalcompleto oMySQLse repitiópor cambios soloCI.

PR #16 / **efdceed922ad5e85099419ee06298fa64c479bde** cerrado técnicamente: CI42372030854 ochojobsverdes,92raíces/661métodos/OS,Win652aprobados/9omitidos,Ub637/24,cero fallos/errores;FE187/lint/build/package. Acta e informe cerrados. Push anterior b062a40 CI42356855634 falló Win3/Ub3 cada188métodos/16aserciones por versión esperada15.16 vs17; otras cuatro partes aprobadas/agregación omitida. Logs íntegros y SHA en informeCORREO; no borrar ni repetir fallo para acreditarlo.

Hijo CI bc27f2d: ocho jobs conservados, frontend una vezporOS después de backend completo, push solo main/PR/manual, manifiestos2 del mismo HEAD/run/OS, integración compilable guardada y comparaciónSHA256dist/JAR. Parser+harness13negativos+paquete real local9.5s/3archivos idénticos; no cambios de migración/dominio. Informe LOGICA_CI_SIN_DUPLICACION_2026-11-07.md, CI propia acreditada arriba.

Clasificación SIN COMMIT: ClasificacionComunicaciones reusa core_event/institution_flow_history, sin DDL nuevo. Toma explícita/versión opaca seq/idempotencia, liberar solo cuenta titular con fundamento, recuperar solo actor actualmente autorizado si titular perdió habilitación, sin vencimiento automático inventado. Fuente captura inmutable; web/central por distribuidor, manual regional solo coordinación de captura. Distribución U2 y legacy/vínculo/asunción propia no pasan sobre otra toma; toma cierra al encaminar. Lectura no crea claim/REQ/SOL/CAS y no devuelve revisiones de gestión posteriores al destino. Historial conservado. Reintentos previos confirmados siguen sin nuevo efecto.

EV02 del actuante nominal, intención durable propia AVISO_CLASIFICACION1.0 y dos canales; permiso actual/cuenta exacta/revisión de toma, no reactivar aviso anterior al retomar. Supervisión transversal NO se deriva de jefatura/TI: su facultad específica queda por implementar, por tanto NOT-01/EV02 aún parcial. UI real TomaClasificacion en Encaminar, requiere toma propia vigente para distribuir; controles explícitos y reintento con mismo cuerpo/clave, descartes por revocación/contexto.

Pruebas del hijo: H2inicial12/8aprobadas4erroresfixtureregional;posterior14/13aprobadas1errorfixturealias,correcciones sin cambiar autorización. Alias con vínculo desde primer registro firmado y aprobación/ejecución reales, calificación **48/48 H2** (alias1+NucleoU2+NucleoInstitucional+ContenidoInstitucional),01:56;HTTPnuevo1/1. **MySQL nativo14/14**,instancia58490fc8e40040d19323eaeec42bcad4 cerrada/proceso0,31migracionesV15.17 sin nuevoDDL. Frontend completo **196/196**,lint/TypeScript/Vite;DOM25/25 y2focalesnuevos(18no seleccionados). Reintentos tras toma finalizada: focal posterior1/1 H2 y1/1 MySQL eneb3e6a6a9b1549d5bc7e5954e1f38735 cerrado/proceso0;misma fuente de producción, no repetir14acreditados. Ningún Maven/MySQL local activo. Logs clasificacion-*. Contrato toma-clasificacion-com-v1,informeLOGICA_TOMA_CLASIFICACION_2026-11-07.md ymatriz138actualizados. Guionmanual preparado/noejecutado.

**Próxima acción exacta:** revisión final/diff,commit/pushdel bloque y PRborradorbasePR17;CIpropia ochojobsdelHEADfinal. CI17 cerrada en acta/informe;padres/main intactos. No repetir baterías no afectadas. Continuar supervisión específica u otro bloque independiente con árbol limpio si queda capacidad. No candidata138prematura;pendientesprogramables siguen documentosREQsinCaso,ingresoflexible/duplicados,48h/calendarios,búsqueda/reportes e históricos. Funcionalvalidado06/11,no pedirla otra vez.

### Corte de correos preservado

Rama `codex/siger-correos-trazables`, creada con árbol limpio desde PR #15 / **5d9d3872427c6d7fdbbcc2f177fc304618329af2**. Primer commit publicado **b062a40aa6a3a92c0ddfe48d9bcce4e582d0749a**, upstream sincronizado; corrección de prueba histórica posterior se confirma en este corte. Todos los cambios locales D3 se conservan y revisaron. Padres y main intactos.

PR #15 está cerrado técnicamente: CI **42353760616**, ocho jobs aprobados,90 raíces/642 métodos por OS; Windows633 aprobados/9 omitidos, Ubuntu618/24, cero fallos/errores. Frontend180/180, lint/TypeScript/Vite/package. Particiones completas/disjuntas del mismo HEAD/run, acta PR e informe verificados. PR #14 /395a7b0: CI42351175167,ochojobs/626métodos por OS,FE176. No atribuir estas CI al hijo.

D3 implementado localmente: plantilla AVISO_INTERNO_LAB_1.0 y copia exacta por intento antes de red, huella por aviso/intento/destinatario/correlación/origen/actor/fecha/textos; red usa la copia persistida verificada. MANUAL solicita la cuenta destinataria; AUTOMATICO no inventa persona. Reintento conserva cada copia, históricos sin materialización permanecen desconocidos. BLOQUEADO_INTEGRIDAD sin red ni reintento automático. Allowlist/campaña/límite4/timeouts/ambigüedad intactos, HTTPS requerido para transporte que declara real. No correo real o cambio de contratos públicos.

V15.17 H2/MySQL `mail_message_snapshot`, SHA ambos **5FC30B970B629266880576C92E66410EBA2678732DCE5C127165C7A53803359C**. V1–V15.16 sin cambios;sin outOfOrder/backfill. Catálogo nominal ausente no se inventa. `DatosNucleoTest` respeta targets anteriores; selector MySQL CorreoMaterializado con schemas _correo/_correoprev dentro del GUID/loopback verificado.

Vista `#notificacion/{id}` conectada a detalle propio/estado/intentos/copia, texto plano y no-store. Lectura, recepción humana y acuse de asignación separados. Consulta/revocación auditadas; cambios de contexto/denegación descartan datos y respuestas tardías. Estado/intentos/copia/confirmación y recuperación durable ahora pertenecen a Notificaciones y funcionan sin bean de transporte; solo nuevo envío lo requiere. Motor recupera sin proveedor, sin duplicar ciclo automático.

Pruebas D3 acreditadas (cortes separados, no sumar repeticiones):
- Base H2 22/22,02:19 (`correo-materializado-base.log`).
- Focal H2/HTTP21/21 y HTTP posterior5/5 (`correo-materializado-focal.log`,`correo-materializado-http-final.log`).
- Regresión después de desacoplar proveedor37/37 H2/HTTP,02:24 (`correo-sin-proveedor-regresion.log`).
- Inyección después del COMMIT y antes de red1/1 H2,13s (`correo-integridad-poscommit-h2.log`), sin hook de producción, bloqueo sin transporte/reintento.
- MySQL nativo Windows8.0.46: primer corte13/13 en19b244b9c4d6459a9c9be21fa7597c58; final tras cambios14/14 en **d14edab31e68447183841843cdbf3daf**,31migraciones y upgrade15.16→15.17. Instancias cerradas/proceso propio0. Log final `correo-materializado-mysql-final.log`.
- DOM focal23/23; frontend completo final187/187,lint/TypeScript/Vite (`correo-sin-proveedor-*`). No E2E físico, proveedores ni correo reales.

Contrato `correo-interno-materializado-v1.md`, informe `LOGICA_CORREO_MATERIALIZADO_2026-11-07.md` y matriz138 actualizados. COR-07 parcial de correo interno, no plantilla/aprobación externa ni oficio implementados. NOT-01/07,COR-03/05,AUD-03/04 evidencia específica; IDs/orden/validación06/11 preservados.

Corrección de AutoridadMigracionTest: expectativa final explícita15.17, incluye partida15.16 y DDL idéntico; **24/24 H2 y24/24 MySQL**,instancia04f1dc8e075d40ec933397480f9308f9 cerrada/proceso0. Conserva todas las comprobaciones de historial/concurrencia/revocación. No se repiten pruebas de producción no afectadas.

**Siguiente acción exacta:** confirmar ajuste de prueba/auxiliar y push normal D3; crear PR en borrador con base `codex/siger-avisos-continuidad` (PR #15). Exigir CI propia ocho jobs del HEAD exacto y cerrar acta con logs/contadores. Ningún Maven/MySQL local sigue activo. Mientras CI corre, continuar bloque funcional independiente en nuevo hijo con árbol limpio; no repetir estas pruebas sin cambio/fallo relevante.

Próximos pendientes programables: toma/supervisión de clasificación y EV-02; documentos/antecedentes REQ sin Caso; ingreso flexible/invalidación/duplicados;48h de registro y calendarios; búsqueda/reportes/exportación; conciliación histórica explícita. No nueva validación funcional ni candidata138 prematura. Mantener checkpoint recuperable; al cumplir criterio138, revisión transversal/retirada de provisionales y candidata compilable integrada antes de cerrar.

## Ingreso/territorio desarrollado en hijo

PrimeraGestion privada1.0 COM+REQ final sinCAS/SOL: recepción, respuesta efectiva, servidor, resultado ORIENTACION/RESPUESTA_DIRECTA/REMISION_CONCLUIDA. API/UI reales, captura desde cuenta separada del principal, fecha explícita, rango/ms, idempotencia concurrente, rollback. ReutilizaV15.16; legacy primera-atencion solo reintentos confirmados actor/acción/hash, sin alterar contratos previos o transporte público.

Territorio: reutiliza org_coverage/OrganizacionInstitucional. TI propone y finaliza cobertura; registro firmado explícito1.8 acredita UUID/región/sede/periodo/huella semántica de propuesta/ref/SHA/estado, sin reinterpretar1.0–1.7. Decodificación estricta y firmas vigentes; cambio de propuesta, suspensión, retirada, expiración o raíz no acreditada cierran nuevas asunciones. Materia funcional distinta de unidad mantiene semántica1.7. No asignaciones nominales inventadas.

Asumir SOL y propio ingreso de jefatura admiten domicilio del requiriente utilizable o decisión fundada por ausencia/contradicción/extranjero/inutilizable; nuevo contrato exige fundamento y referencias en excepción regional y conserva declaración. No NNA/hechos/captura como domicilio ni regionalización automática de central. No distribución que cree REQ. Principal histórico existente no cambia al cambiar cobertura, Casos o consulta por materia.

Constancia territorial tipada1.0 se añade a institution_flow_history en la misma transacción de nacimiento, sin relato duplicado ni nueva tabla: REQ_TERRITORIO_ACREDITADO. NO V15.17: decisión técnica de reutilizar historial y registro firmado existentes, evitando índice/catálogo paralelo del diseño preliminar. V1–V15.16 intactas, sin outOfOrder/backfill. Históricos sin constancia siguen desconocidos. Frontend decide mediante rutas explícitas y muestra cobertura actual; preflight propio comprueba autor de COM/jefatura/área antes de revelar. Solo jefatura legítima decide, lectura materia/TI/otra área no habilita.

Pruebas hijo, cortes separados:
- Ingreso dominio8/8 H2, HTTP2/2 final, DOM9/9; MySQL Windows8.0.46 nuevo8/8 en228cd3236bc74daf80bde3fb43337e63 cerrado/proceso0. Ampliación anterior27 tuvo25aprobados/1fallo+1error de fixture de sede/columna; corregidos sin cambiar permisos.
- Territorio focal29/29 H2:11dominio+17U2+1upgradeBitácora,02:45. HTTP posterior4/4 (territorio2+ingreso2),31s. DOM14/14 (territorio5+distribución9). Compilación Java correcta; primer comando desde raíz sinPOM no ejecutó build, corregida ruta explícita. Al primer corte no nativo territorio; calificación final separada abajo, CI hija pendiente.
- Cierre territorio: MySQL nativo Windows8.0.46 **13/13** (12territorio+1compatibilidaddocumental1.7→1.8) en instanciapropiaf606251577c84852987fb16ef936d4e8 cerrada/proceso0. H2focalfinal4/4 (central1+compatibilidad1+HTTPterritorio2),37s, logterritorio-compat-final.log. Frontend completo163/163, lint/TypeScript/Vite correctos.
- Informes LOGICA_INGRESO_TERRITORIO_2026-11-06.md, contratos/primera-gestion-institucional-v1.md y territorio-institucional-v1.md actualizados; matriz138 preservada en IDs/orden/validación06/11 y evidencia actual. CI propia pendiente, guionmanual preparado/noejecutado.

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
