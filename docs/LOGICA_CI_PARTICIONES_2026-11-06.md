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
