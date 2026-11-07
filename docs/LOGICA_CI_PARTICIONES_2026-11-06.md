# CI completa por particiones · 06/11/2026

## Incidencia conservada

PR #8, corte `adb7eebf8dabe59c5467d746be158a8326dba79c`, [intento 42233659071](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42233659071): Ubuntu aprobó 506 métodos (482 aprobados, 24 omitidos), frontend 129/129, lint/build/package; Maven 19:12. Windows fue cancelado por el presupuesto global de 30 minutos: registró 446 métodos, cero fallos/errores y nueve omitidos. Faltaron 60 métodos; ese job **no aprobó**.

Logs íntegros de ambos jobs preservados fuera de Git en la campaña privada `siger-build-4c652bfd9b7a41d5a8e0d659e180202d`, archivos `ci8-windows-30min.log` y `ci8-ubuntu-aprobada.log`. No contienen evidencia de ejecución de los 60 métodos restantes de Windows. La regresión local anterior sí completó 506/497/9, frontend 129/129 y lint/build/package (27:33 Maven).

Diagnóstico: costo acumulado de preparación y suite secuencial. En Windows, Áreas/Subrogancias/Materias consumieron 236,7/163,4/146,6 segundos frente a 142,1/108,2/96,12 en Ubuntu. No se detectaron aserciones fallidas. Se conserva el presupuesto de 30 minutos por job; no se cambian timeouts funcionales, transacciones, datos, aserciones ni exclusiones.

## Corrección verificable

Dos particiones por sistema operativo, cada una con compilación, frontend completo, pruebas de su selección y package. Un inventario de anotaciones del bytecode JUnit compilado, sin inicializar clases, incluye métodos heredados, interfaces, clases anidadas y meta-anotaciones. SHA-256 del nombre completo reparte cada raíz a una sola partición. La selección Surefire usa rutas completas, no una selección manual de suites rápidas.

Cada job compara su inventario con los XML Surefire reales y publica un manifiesto sin relatos, credenciales o logs institucionales. Un job de cobertura por sistema valida HEAD/ejecución, ambas particiones, reparto determinista, igualdad de inventarios, unión completa sin duplicaciones, resultados y totales. Un job cancelado, un manifiesto ausente, una clase no ejecutada o una aserción fallida impiden aprobar el conjunto. Checkout usa el HEAD exacto del PR. No se reutiliza el éxito de otro commit.

`Build-Siger.ps1` conserva su modo integral ordinario cuando no recibe partición. Las tres pruebas históricas `stress` siguen con la exclusión preexistente; los workers reales mantienen sus condiciones declaradas. No se añaden exclusiones. Frontend se repite en cada job: sus 129 métodos no se suman como si fueran distintos.

La herramienta auxiliar se verifica con bytecode y XML sintéticos, separados de las pruebas funcionales: seis raíces (directa, herencia, interfaz, anidación, meta-anotación y parametrización), dos particiones y diez controles negativos. Rechaza metadatos de HEAD/ejecución/esquema/estado incorrectos, particiones duplicadas, clases omitidas/no ejecutadas, fallos, inventario incompleto y XML ausente. La primera ejecución detectó suma incorrecta de diccionarios y una enumeración mutable en el propio auxiliar; se corrigieron antes de publicarlo. Esto no constituye un E2E físico ni prueba MySQL.

Selección local del padre: inventario limpio de 71 raíces, partición 1 con 33; frontend 129/129 y lint/build aprobados. Partición1 local terminada: 243 métodos,234 aprobados/9 omitidos/cero fallos/errores, las33 clases cubiertas; verify/package y control XML/manifiesto correctos. Log ci8-particion-local.log y temporales3d9ee0457b634a4fa08310bed6646ef2 privados. La CI del nuevo HEAD se consigna en el acta PR #8 al completar los jobs; la partición2 local no se repite porque la regresión integral previa del mismo dominio506 ya está acreditada y ambas particiones deben aprobar en CI. Las referencias de acciones de artefactos y los selectores se contrastaron con documentación primaria: [upload-artifact](https://github.com/actions/upload-artifact), [download-artifact](https://github.com/actions/download-artifact), [Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html).

## Continuidad

El hijo `codex/siger-continuidad-excepcional`, commit `60b25e9`, conserva SUB-CONT-01, V15.13 y la corrección de doble canal funcional. Su calificación integral/CI se realiza separadamente. El padre todavía conserva la emisión solo interna de algunos avisos: la corrección del hijo responde a §16, cuya última columna indica acuse, no dispensa de correo. No se enviaron correos reales ni se promovieron filas históricas.

Sin migraciones por esta corrección de CI. Sin cambios de main, padres #2–#7, U4.4/U4.5, QA, servicios, proveedores, bases preservadas o contratos de transporte. Los PR siguen en borrador; no hay merge ni despliegue.

## Evolución comprobada a tres particiones

PR #10/2d98e2122f7bfdf08e102eaf84d7aa496a703371, CI42283881844: ambos OS aprobados con554 métodos/78raíces, frontend146/lint/build/package. Windows247+307 métodos, Maven11:34/26:40; Ubuntu09:21/23:47. El presupuesto30min no falló, pero la segunda partición quedó cerca de él. Se conservan ese éxito y sus logs; no se presenta como fallo.

Al continuar en la rama hija bitácora, se agruparon los78 tiempos reales de clases Windows por el mismo SHA-256, sin cambiar el algoritmo de selección. Suma por dos particiones11,47/26,56min; distribución tres17,99/11,90/8,15min; cuatro7,31/7,29/4,16/19,27min. Se eligen tres por menor máximo previsto y menor costo de preparación que cuatro. Son estimaciones sobre ese corte, no tiempos acreditados del nuevo HEAD.

Workflow declara tres particiones por OS y agregación obligatoria de las tres; presupuesto30min inalterado. Inventario, XML reales, HEAD/run, unión completa/disjunta, aserciones, transacciones y exclusiones permanecen iguales. Frontend se repite en cada parte sin sumarlo como cobertura única. La compilación integral ordinaria de Build-Siger sigue disponible.

Harness local aprobado: conserva6raíces/dos partes/diez controles negativos y agrega7raíces/tres partes, rechazo de cantidad de partes incompatible y tercer manifiesto ausente;12controles negativos acumulados. Fixtures de bytecode/XML privados, no E2E ni nuevas pruebas físicas. El nuevo HEAD requiere su propia CI de ocho jobs (seis partes+dos agregados).

## Incidencia CI Windows y corrección del fixture

CI42311131299 de be2da99 completó ocho jobs aprobados. La del ajuste361e323 (42312378567) aprobó los tres Ubuntu y Windows1/3, pero Windows2 tuvo173 métodos,0 fallos y7 errores de conexión H2 en la inicialización de SubroganciasTest. Agregación omitida, no se declara verde. Falló al adquirir la primera conexión de negocio después de cerrar el pool migrador. Se preserva el log completo privado ci11-windows-part2-361e323-fallida-completa.log,1174780bytes,SHA25625beec3556986a08c66ee1eee11e34d48054f583461bee103db0f988dc803c10.

Se elimina ese límite de ciclo de vida artificial solo en el fixture H2 local: migración y negocio comparten pool/credenciales como el arranque real. Destinos explícitos/MySQL conservan migrador separado; errores del constructor cierran el pool. BaseJdbc, adquisición3000ms, consulta4s, transacción5s, URL, aserciones y exclusiones sin cambios. No DB_CLOSE_DELAY=-1 ni nuevas migraciones. Una alternativa local de solapar pools y cerrar el migrador falló15/15 por restricciones sobre sesión/base cerrada, log ci11-handoff-focal.log; no se conserva esa implementación. Solución final15/15 H2 (Subrogancias13+actualizaciones reales1+1),02:13, log ci11-fixture-pool-unico.log. Requiere CI propia del nuevo HEAD; el éxito anterior no lo sustituye.

MySQL nativo Windows8.0.46 FixtureJdbc final2/2 (actualizaciones Documento15.14 y Bitácora15.15), instancia67e708f123be4ce681ec5b455132e708 cerrada/proceso propio0; conserva pools separados. Log ci11-fixture-mysql.log. H2 final15/15; CI nueva pendiente.
