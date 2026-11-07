# Rectificaciones de captura manual · 07/11/2026

## Implementación y alcance acreditado localmente

RectificacionesIngreso/Controller conectados a pestaña real de Comunicación manual. Permite conservar captura original y registrar una corrección tipada con valores anterior/nuevo, motivo, persona/fecha/revisión. La cadena, conteos/páginas y consulta individual comparten autorización actual; original/folio/hash/procedencia no cambian. Usa institution_flow_history y comandos existentes, sin DDL nuevo ni catálogo paralelo. Contrato [rectificacion-ingreso-manual-v1](../contratos/rectificacion-ingreso-manual-v1.md).

Política por etapa en NucleoU2: registrador antes de distribuir; jefatura SOL pendiente; encargado/jefatura principal del REQ después. No permiso global por ser TI, lector de materia, registrador sin responsabilidad, colaborador de Caso u otra cuenta de la misma persona. Revocación efectiva sin heredar acceso desde original. No inferencias de legado no conciliado. La corrección de captura puede registrarse tras cierre como constancia, sin producir actuación/reapertura/tarea o alterar primera respuesta.

Cerrojos identidad→COM, revisión, idempotencia y rollback. Fechas normalizadasUTCms y cronología consistente con registro/respuesta acreditada. Interfaz conserva precisión y mismo intento ante respuesta incierta incluso si cambia versión visible; error de acceso limpia valores/totales/enlaces. No original editable o controles ficticios.

Se retiraron de Comunicacion.tsx CommunicationAction/LinkForm/DestinationAction privados inalcanzables: ningún control abría sus estados, pero conservaban texto/API de crear REQ desde primer Caso/cierre inferido. La pantalla permanece en circuitos institucionales U2 reales y APIs históricas siguen cerradas. Es limpieza localizada justificada; no se declara revisión transversal final ni candidata integrada.

## Verificación

- H2 dominio9/9,01:10, rectificaciones-focal-1.log. Incluye captura íntegra, responsabilidades por etapa, Caso colaborador, alias de misma persona, concurrencia, idempotencia, rollback, datos inválidos, cadena/paginación/individual, cierre de primera atención conservado.
- Revocación posterior H2 focal1/1,14s, rectificaciones-revocacion-h2.log; son9+1 en cortes separados. Misma batería completa MySQL abajo.
- HTTP6/6,36s: nuevas rectificaciones2 más regresión ingreso2/territorio2, login/CSRF/reintentos/listado/individual y retiro de facultad al distribuir. MockMvc/H2, no navegador físico.
- MySQL nativo Windows8.0.46 nuevo descartable10/10, cero fallos/errores/omisiones, selecciónRectificaciones. Instancia024ce24f64644ecd860f4fba06afcf9f cerrada/proceso propio0. Guías de aislamiento, puertos/credenciales/datadir propios, sin servicios/global/QA.
- DOM focal14/14 inicial (rectificaciones5+distribución9), frontend integral168/168/lint/build inicial. Añadidas dos pruebas de integración de pestaña/original ciudadano: **frontend final170/170,lint/TypeScript/Vite correctos**. Sin exclusiones nuevas; DOM no es E2E físico.
- CI del nuevo bloque aún pendiente de publicación; no se atribuyen575/158 del padre ni CI de otra rama.

V1–V15.16 y contratos públicos1.0/1.1, firmas, originales y COMMIT→ACK intactos. No migraciones nuevas, rollback de esquema, datos operacionales, correo/Graph/PISEE/CU reales. Evidencia privada en campaña siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs rectificaciones-*. No se imprimen configuraciones privadas/credenciales.

## Cadena y evidencia anterior

Rama codex/siger-rectificaciones-ingreso hija de PR #12/codex/siger-ingreso-territorio en a3d74eb2a5516f28d81b009368b635f35fc17197. Sin upstream/PR propio al escribir este corte local. Árbol de padre limpio/sincronizado al crear rama, commits anteriores preservados.

PR #11/39df7b9 CI42318438019 cerrada con ocho jobs aprobados y logs consultados:81raíces/575métodos porOS, Windows566aprobados/9omitidos yUbuntu551/24,cero fallos/errores,frontend158/lint/build/package. Partes224+173+178, WinMaven18:23/13:10/08:57,Ub17:06/14:27/07:50. Misma selección/aserciones/30min, ambos agregadores completos/disjuntos del HEADexacto. PR borrador, sinmerge/deploy.

PR #12/a3d74eb CI42319144670: Ubuntu3 falló frontend162/163 en App.test.tsx al pulsar Confirmar evaluación antes de que cargara revisionCaso; la UI cerró correctamente el botón y no ejecutó comando. Se espera versión/revisión asíncrona en test sin sleeps ni aumento de timeout. Otros cuatro jobs completados verdes, partes1 todavía en curso al inspeccionar. NO CI verde ni pruebas backend de esa partición. Evidencia completa privada ci12-ubuntu-part3-a3d74eb-fallida-completa.log,51438bytes,SHA25695e7b624da6e249e7903fea5ded9b01cd400f79ec8295844ff9e11de1adc0dc6. Próximo paso conserva este corte antes de volver al padre para corregir prueba y exigir nuevaCI.

## Guion por roles preparado, no ejecutado

1. Entorno nuevo descartable, cuentas/roles/áreas sintéticos por circuito U1.5. Profesional captura COM manual con texto/canal/recepción y región del asunto. Pestaña rectificaciones muestra datos capturados; corregir con fundamento y comparar ambos valores. Original/hash/folio/autor iguales.
2. Distribuir SOL a otra jefatura; registrador original sin otra facultad deja de consultar/rectificar esa pestaña, conserva original. Jefatura receptora corrige y asigna atención REQsinCaso. Encargado ahora consulta/rectifica con misma cuenta; otra área/TI/alias no.
3. Abrir Caso colaborador en otra área: su responsable consulta su Caso, pero no corrige captura global por esa pertenencia. Modificar región del asunto no cambia domicilioSOL, sede principal o Casos.
4. Enviar dos correcciones con igual revisión: solo una confirma; reintento confirmado no duplica. Simular respuesta perdida: misma clave/payload hasta confirmar. Revocar concesión y consultar listado/constancia con sesión previa: denegados y vista descartada.
5. Primera atención fechada ya finalizada: corrección de captura conserva REQ cerrado, respuesta/fecha efectivas, sinCaso y tareas. Fecha de recepción posterior a respuesta acreditada denegada; corregir actuación es otro circuito.
6. Original ciudadano no ofrece el circuito manual. Histórico no conciliado conserva evidencia y no recibe pertenencias/permiso inferidos.

## Pendientes reales

ING-05 sigue parcial para formularios públicos, contactos/campos sectoriales y fuentes históricas no conciliadas. Falta consumo de datos rectificados en búsqueda/motor temporal, sin reescribir mediciones o originales pasados. Documentos/antecedentesREQsinCaso, notificaciones U2 pendientes, calendarios/plazos, clasificación/invalidaciones/duplicados/conciliación y reportería/exportación continúan programables; ninguna nueva validación funcional necesaria. Matriz138 conserva IDs/orden y distingue evidencia local de CI. Revisión transversal/candidata cuando todas estén implementadas/verificadas o limitadas concretamente, no antes.
