# CI sin ejecuciones duplicadas · 07/11/2026

## Diseño y equivalencia exigida

Rama hija de PR #16 / efdceed922ad5e85099419ee06298fa64c479bde. Se conserva la matriz de ocho jobs: seis particiones backend (tres Windows y tres Ubuntu) y dos agregaciones por OS. No se cambian pruebas, aserciones, exclusiones ni tiempos funcionales. El presupuesto de cada job sigue en30 minutos.

Los pushes a ramas de trabajo dejan de iniciar una ejecución que se duplicaba al abrir o actualizar el PR. Se conserva pull_request y workflow_dispatch; push solo se usa para main. Esta configuración no modifica main ni autoriza su merge. Antes de abrir un PR se puede solicitar ejecución explícita. El estado exigido para cerrar un bloque es su CI propia del HEAD publicado, no el push aislado ni la ejecución del padre.

El frontend completo (npm ci, lint, todas las pruebas, TypeScript y Vite) se ejecuta una vez por OS en su job de agregación, después de acreditar el backend completo. Las seis particiones ejecutan el mismo Maven clean/test-compile y verify, mismos selectores derivados del bytecode y mismos informes Surefire. No necesitan Node ni distribuciones web previas: sus JAR intermedios no se publican como candidata integrada.

Cada agregación requiere las tres evidencias completas/disjuntas del mismo HEAD, run y familia de OS. El manifiesto evoluciona explícitamente a SIGER_CI_COVERAGE_2 con platform; no mezcla evidencias históricas u otro sistema. Build-Siger -IntegrarParticiones vuelve a comprobar esa evidencia antes de ejecutar cualquier integración. No ofrece un modo libre de omitir tests: necesita la acreditación previa de todas las clases. El build local normal sin selección conserva frontend completo y Maven clean verify.

Después de verificar el frontend, la agregación compila y empaqueta con Maven clean compile jar:jar spring-boot:repackage. No reejecuta Surefire ya acreditado ni usa skipTests/exclusiones adicionales. Test-IntegracionWeb compara todos los archivos dist contra BOOT-INF/classes/static dentro del JAR por ruta, cantidad y SHA256: faltantes, extras, duplicados o bytes distintos hacen fallar el job. Esto acredita ensamblado compilable, no ejecución física, instalación o autorización productiva.

Así una ejecución estable conserva todas las pruebas y ocho jobs, reduce de seis a dos los ciclos completos de frontend y elimina la duplicación push/PR de las ramas. La validación Windows/Ubuntu del HEAD propio debe acreditar equivalencia antes de declarar este cambio aprobado.

## Evidencia focal y límites

Parser PowerShell de los cinco scripts correcto. Test-CiCoverageHarness: inventario real compilado de herencia/interfaces/anidación/meta-anotaciones, dos y tres particiones,13 controles negativos aprobados, incluidos OS ajeno/HEAD/run/faltantes/duplicados/totales inválidos. El control anterior de cobertura se conserva y refuerza.

Test-IntegracionWebHarness: una integración sintética correcta y cuatro rechazos (faltante, alterado, extra, duplicado). No es un JAR ejecutable ni reemplaza la integración real.

Logs privados ci17-cobertura-harness.log y ci17-integracion-harness.log. Ensamblado local real correcto en9.5s (ci17-integracion-paquete-local.log);3archivos dist idénticos dentro del JAR por SHA256 (ci17-integracion-static-local.log). Integración con manifiestos ausentes y combinación de selección parcial rechazadas antes de frontend/Maven. Esto verifica el ensamblado sobre la fuente frontend ya acreditada, no repite ni reatribuye sus187tests al nuevoHEAD. Calificación CI propia todavía pendiente de publicación. No repetir regresiones Java/MySQL por cambios exclusivos del workflow/auxiliares, sin código de dominio ni migraciones afectadas. No atribuir resultados de PR #16 a este hijo.

No cambia V1–V15.17, contratos de datos o transporte, permisos, originales, runtime QA, servicios o proveedores. No E2E físico/Graph/correos reales. Matriz138: evidencia de calidad transversal; no nueva funcionalidad institucional declarada completa.
