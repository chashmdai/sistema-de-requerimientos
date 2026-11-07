# Rectificación de asociaciones documentales · 07/11/2026

## Alcance efectivo

Hijo de PR #22/ef2eb1c, rama codex/siger-asociaciones-documentales. Originales de gestión de Caso y REQ: corrección de referencias, retirada/restauración del propietario con fundamento e historia, sin mover custodia, alterar originales o fabricar actuaciones. Responsable y jefatura efectivos; no TI, colaborador, lector de materia ni alias por equivalencia de persona. Ver contrato asociaciones-documentales-de-gestion-v1.

Backend, API, persistencia y componente React común conectados. Retirada separada de protección, cuarentena y borrado. Vistas/descargas/historiales/listas/consultas por materia y referencias compartidas protegidos. La revisión documental invalida consentimientos/publicaciones/excepciones anteriores, también después de restaurar. I/O del visor verifica el propietario aun si conserva una referencia privada anterior; los streams vuelven a comprobar antes de cada bloque64KiB. Metadatos del propietario para recuperar la asociación no permiten entregar contenido.

## Arquitectura y archivos

AsociacionesDocumentales coordina autorización, revisión, idempotencia y transacción; AsociacionesVigentes resuelve proyección y guardas comunes compatibles con targets previos. Integración en ContenidoInstitucional, DocumentosGestion, DocumentosRequerimiento y VisorDocumento. RevisionExcepcional no exige como nuevo antecedente un documento retirado, conservando los conjuntos/fingerprints de solicitudes anteriores. Configuración/controller nuevos reutilizan sesión/CSRF/no-store y auditoría existentes. Nueva UI AsociacionDocumento en ambos contextos reemplaza el antiguo formulario REQ de referencia sin fundamento; las rutas históricas siguen sin poder reactivar proyecciones retiradas.

V15.21 H2/MySQL aditiva,35 migraciones. SHA256 **6AB06F174B4A01F74FCA6D5CC6055E7792146A3FCB0BB8F6992339BDDA295DEC**. Cuatro tablas de control/destinos/historia/comando; originales y vínculos históricos intactos, sin migración inferida. V1–V15.20 conservadas; sin outOfOrder. Referencias remotas comprobadas antes de elegir versión: main95cf2ae y gateU4 0f10ddb; sin una nueva rama U4.5 publicada. No se consultó ni modificó su directorio o runtime.

## Pruebas y evidencia por corte

- Compilación Java inicial correcta. H2 inicial11:9aprobadas/2fallos por contar un Caso histórico de fixture en vez del REQ concreto. Se corrigió la consulta de prueba para exigir cero Casos de ese REQ, sin cambiar reglas o aserciones de integridad.
- Un intento focal no compiló por import de JsonEstricto; corregido sin ejecución de pruebas en ese intento.
- **39/39H2/HTTP**,01:26:2correctivas,3indirectas,1upgrade20→21,1HTTP y32autoridad/versiones/DDL. Concurrencia, rollback, clausura explícita, sesiones/CSRF/pin, revocación, referencias/materia no reviven, PNGcacheado y stream64KiB. Logs asociaciones-focal-h2-corregido.
- DOM inicial15/15 antes de sustituir el formulario antiguo. Frontend integral234:233aprobadas/1fallo de expectativa antigua que prohibía consultar bitácora al responsable de un REQ cerrado. La rectificación necesita esa consulta autorizada; prueba adaptada para exigir el GET de ese REQ concreto, conservando denegación de carga/Caso artificial. Correctivo **9/9DOM**,lint/TypeScript/Vite aprobados. La CI del HEAD deberá acreditar234; no declarar este corte234/234 inexistente.
- MySQL nativoWindows8.0.46 inicial49:32aprobadas/17errores de preparación, ninguna aserción fallida. Lista cerrada de destinos de DatosNucleoTest no incluía asoc/asocprev/asocmix. Instancia eb7373b6eeff4bc6a8b62ce54c08b034 cerrada/proceso0, tests.log íntegro conservado. Los32 de autoridad sí ejecutaron las35migraciones. Se añaden exclusivamente los tres sufijos al patrón loopback+GUID; no se relaja la guardia ni se reutiliza la instancia. Correctivo **17/17MySQL**,02:13, en2cbc650676b7442785318d77f70f3499,cerrada/proceso0:13dominio(incluyealias yrectificación de actuación),3indirectas y1upgrade20→21. No repetir32 no afectados ni sumar cortes como49/49.
- CI propia pendiente. CI22/42404716447 corresponde a ef2eb1c y no acredita este hijo. Mantener evidencia propia por HEAD.

Antivirus/inspector y algunos límites HTTP usan dobles; H2, MockMvc, DOM y MySQL real aislado se distinguen de E2E físico o antivirus real. Sin proveedores/correo real, fixtures en QA, despliegue o merge.

## Guion de aceptación manual preparado, no ejecutado

1. Con identidades sintéticas, jefatura asume SOL y asigna REQsinCaso a profesional de su área. Este carga un original y registra una gestión; abre rectificación del documento y cambia referencia general por esa gestión con fundamento. Comprobar historial, mismo UUID/hash y ausencia de Caso nuevo.
2. Profesional retira todas las asociaciones. Comprobar indicador, ausencia de botones de bytes/versiones y respuesta404 a HEAD/Range anterior; jefatura conserva recuperación fundada. Restaurar y comprobar protección FIR/ordinaria sin cambios y bytes idénticos.
3. Caso con responsable y colaborador del mismo ámbito: documento ordinario inicialmente visible para ambos. Responsable lo retira; colaborador deja de verlo/listarlo y no consulta historia/asociaciones. Jefatura restaura con nueva constancia. Otro ámbito, TI y alias sin asignación continúan denegados.
4. Referencia compartida a otro Caso y publicación por materia: acreditar permisos iniciales, retirar y restaurar desde propietario; ambos siguen cerrados hasta autorización documental nueva. La restauración no cambia principal ni equipo.
5. Dos rectificaciones simultáneas con la misma revisión: una confirma, otra409; no pierde original/historia. Reintentar la confirmada con misma clave/cuerpo no agrega efecto. Cambio de sesión antes de POST devuelve409por pin de cuenta.
6. Retirar tras generar una vistaPNG o mientras descarga un archivo sintético grande: vista anterior no entrega contenido; siguiente bloque del stream se deniega. Los bytes previamente enviados no se retractan.

## Pendientes reales

DOC-07 compuesta sigue parcial: este bloque rectifica asociaciones dentro del propietario y retira sus publicaciones, no traslada propiedad entre expedientes ni reescribe asociaciones del sobre ciudadano. Pertinencia de COM/documento y publicación por recurso conservan circuitos anteriores. Política versionada de retención, bloqueo por investigación, purga autorizada y conciliación histórica explícita siguen programables. No requieren pedir otra validación funcional del levantamiento. Sin catálogos sectoriales/nominales reales cargados ni prueba manual/browser en este corte.

Matriz138 actualizada por evidencia específica; no se declara U2–U9 completa ni candidata final. Siguiente bloque recomendado: medición de registro48h y motor de políticas/calendarios configurables, manteniendo el cómputo normativo cerrado si faltan parámetros efectivos.


## Cierre de CI y preservación posterior

CI42406996840/25678fd se interrumpió en Windows1 a30min (278métodos registrados,0fallos/0errores/9omitidos);las otras cinco particiones aprobaron yla agregación fue omitida. No se declara ese HEAD aprobado. Evidencia completa/hash ydiagnóstico en LOGICA_CIERRE_CI_PR23_2026-11-07.md. Se optimiza reparto manteniendo109raíces,ochojobs y todos los controles,sin cambios de dominio/frontend/migración. El acta ylos checks actuales del [PR23](https://github.com/chashmdai/sistema-de-requerimientos/pull/23/checks) identifican HEAD/CI definitivos;no se reutilizaCI22. Trabajo funcional posterior54b31ff preservado localmente fuera dePR23,pendiente de desarrollo/verificación propios.
