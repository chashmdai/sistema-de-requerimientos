# Cierre prioritario de CI · PR23 · 07/11/2026

## Corte y alcance

Corrección de CI sobre codex/siger-asociaciones-documentales, desde25678fd5fae42fb0cb08d6f250640983f9269929. El responsable exige ocho jobs aprobados en el HEAD definitivo antes de continuar funciones. Se conserva PR23 en borrador,basecodex/siger-cuarentena-documental/PR22. Sin merge,despliegue ni intervención operacional. Backend,frontend,tests de negocio,POM,migraciones yworkflowYAML sin cambios en este correctivo.

El trabajo posterior de plazos queda en **codex/siger-plazos-registro /54b31ff28dad53d3bcea330ea7e3d6cd9ee4b015**,commit local NO publicado. IncluyeV15.22 y capacidades todavía parciales. No se incorpora al PR23. Su estado y recuperación se detallan en el checkpoint.

## Evidencia del intento interrumpido

CI **42406996840** /25678fd: cinco particiones backend aprobaron; Windows1/job128506723800 se canceló al alcanzar presupuesto global30min. Último resumen:278métodos,0fallos,0errores,9omitidos,pero partición incompleta. La agregación se omitió; no es CI aprobada ni acredita frontend de ese HEAD.

Log decodificado completo conservado fuera deGit: `ci23-windows-part1-25678fd-interrumpida-completa.log`,2354382bytes,SHA256 **46390364D60EFE6E4323D95A6602CEA16B2D4EF274837F56C4851EC03A087237**,campañaLocalAppData/siger-build-4c652bfd9b7a41d5a8e0d659e180202d. Fuente verificable: [job interrumpido](https://github.com/chashmdai/sistema-de-requerimientos/actions/runs/42406996840/job/128506723800). No se borró ni reemplazó por el éxito de otro commit.

## Causa y solución

El hash del nombre distribuye cantidad de clases sin considerar que su coste difiere. Las duraciones máximas observadas por raíz entre Windows/Ubuntu de CI22 y clases completadas de CI23 suman72.35445min. Son pesos heurísticos, no una nueva ejecución ni acreditación de la partición interrumpida.

| Partición | Reparto anterior estimado | Reparto nuevo estimado | Clases nuevas |
|---|---:|---:|---:|
|1|32.003min|24.121min|33|
|2|26.643min|24.117min|39|
|3|13.709min|24.117min|37|

LPT determinista: ordenar por peso decreciente y nombre ordinal; asignar a la partición de menor carga acumulada,desempate por índice. Clase sin peso recibe máximo observado y siempre participa del inventario. Los pesos no definen qué clases existen ni cuáles son obligatorias. `ci-test-duration-hints.json` conserva procedencia y distingue clases completadas de una ejecución global aprobada.

Manifiesto explícito **SIGER_CI_COVERAGE_3**,algoritmoLPT_DURACION_1,huella de pesos/inventario/particiones. Plan yAggregate recalculan el mismo reparto; Validate vuelve a inspeccionar clases compiladas y comprueba selección,huella,XML reales,totales y estado. Se rechazan contadores negativos/inconsistentes. Se conservan comprobaciones de HEAD/run/OS,particiones completas/disjuntas,no duplicadas,todas las raíces ejecutadas y compilación/integración del JAR.

Se mantienen **ocho jobs** (tres particiones porOS ydos agregaciones),frontend una vezporOS,disparadores sin duplicación,presupuesto30min,memoria,timeouts funcionales/transaccionales,aserciones yexclusiones históricas. No nuevas exclusiones ni cambios de permiso. No MySQL nuevo:este correctivo no cambia datos,migraciones,transacciones oautorización.

## Validación local por impacto

- Compilación limpia `clean test-compile` Java25:aprobada,10.592s. Necesaria tras volver de la rama de plazos para eliminar clases/recursosV15.22 deltarget; no cambia runtime niartefactosQA.
- Inventario real compilado delPR23: **109raíces**,repartidas33/39/37,unión109sin duplicados u omisiones. Huella del plan local **40abe4b69e6fb6dd7b3b57e5a614b75ee86bdb77cd493f5adbd5a2b1609bb127**. Estimaciones anteriores,no son tiempos nuevos de ejecución.
- Harness de metadatos:6raíces/2particiones y7raíces/3particiones positivas; **21controles negativos** más equilibrio,determinismo ante orden invertido y clase nueva conservada. Son fixtures de XML/compilador, no regresión backend ni E2Efísico.
- Primer harness terminó0pero el negativoXMLfaltante usabaJava global anterior yfallaba por compilador,instrucción distinta a la que debía probar. Corregido para pasar Java25/ClassesDirectory explícitos y exigir mensaje de partición incompleta;harness definitivo sin esos errores. No se acepta rechazo por causa incidental como evidencia del control.
- No se repiten MySQL ofrontend locales sin cambios correspondientes. La CI del HEAD publicado ejecuta la regresión completa backend/frontend y la integración en ambosOS.

## Evidencia definitiva por HEAD

El acta del [PR23](https://github.com/chashmdai/sistema-de-requerimientos/pull/23) registra SHA definitivo,ejecución,IDs de ocho jobs,conteos y resultados después de verificarlos. Sus [checks del HEAD actual](https://github.com/chashmdai/sistema-de-requerimientos/pull/23/checks) son la fuente de cierre y permiten recuperar el estado sin atribuir un éxito anterior a un commit posterior. Hasta que los ocho estén aprobados,se continúa exclusivamente el cierre CI. Este documento conserva el diagnóstico y los controles realizados antes de publicar;no presume el resultado de una ejecución futura.

No cambios funcionales nuevos,catálogos nominales,proveedores,correo real,E2Efísico odespliegue. QA,U4.4/U4.5,SMR,Formulario,originales/firmas,COMMIT→ACK,JAR yservicios preservados.
