# Checkpoint U4.5 — capacidad ClamAV acreditada, adaptación Java por validar

26/10/2026. `C:\req\repo`, rama `migracion-windows-laboratorio-2026`, continuación de `c224360`. Código **82cf340**; documentación en el commit posterior de este corte, consultable con `git log`. Sin push. **U4.5 sigue abierta, 100% Windows y producción deshabilitada. No U5.**

## Estado que conservar

- Dos pasadas del scanner independiente: Clean=0, EICAR=1, Unavailable=2. La ampliada verifica token/Job, salidas separadas, memoria máxima, temporales y limpieza. No se cambió Kaspersky; TI solo excluyó testdata.
- Candidato Java de antivirus implementado y compilado, con cuatro guardas; **sin bean, sin activación en el entrypoint**. Su ensayo real fue bloqueado por la guarda de 4 GiB. No confundirlo con la pasada C# aprobada.
- Informe detallado y resultados: [validación](VALIDACION_CLAMAV_WINDOWS_2026-10-26.md), [JSON del corte](RESULTADOS_CLAMAV_WINDOWS_2026-10-26.json).
- Checkpoints, paquetes/bases, trabajos y logs anteriores conservados. No se reimportó nada. Histórico y24objetos permanecen en su ubicación anterior. No hay servicios documentales nuevos ni scanner permanente.
- H2 y MySQL QA Windows: **223 seleccionadas,218 correctas,0 fallos/errores,5 omitidas por motor**. Cuatro guardas finales del candidato también correctas; están incluidas en el conjunto de métodos y no se suman. Seis canarios actualizados correctos, distintos de los tests documentales reales.
- Fingerprint histórico:59tablas/504filas, cero diferencias contra la importación. Custodia:24objetos/16.869.263bytes, SHA/tamaños iguales y cero ALLOW inesperados. Runtime/scanner propios detenidos;8080/8081 sin listener propio.
- Último preflight del candidato: **2.211.160.064 bytes libres**, requeridos4.294.967.296; scannerExecuted=false. Su evidencia es `C:\req\evidencias\antivirus-java-70f848e5f19e413bb4a68348ddc56d5a.json`.

## Próximo paso exacto

1. Revisar Git y los resultados, conservando todo. El host volvió a tener menos de4GiB: mantener el fallo cerrado hasta un preflight satisfactorio, sin detener otros procesos. No repetir instalación, FreshClam, transferencias o tests completos solo para obtener memoria.
2. Ejecutar el harness del candidato cuando haya margen. El wrapper comprueba4GiB y el adaptador vuelve a comprobarlos antes de crear un trabajo. Usa Java25 y clases de compilación únicamente como QA, nunca un JAR activo. No permite un scanner falso para aprobar los escenarios reales.
3. Si el candidato falla, conservar su raíz y resultado, revisar el error concreto. No asumir Kaspersky. No aceptar exit2 como limpio. Si quedan `av-*`/`pending` o DACL/perfil sin conciliar, investigar su proceso/identidad y restaurar de forma acotada; no borrar la raíz para hacer desaparecer el bloqueo.
4. Calificar ese adaptador, concurrencia entre JVM/raíces, interrupción y recuperación. Las sondas anteriores usan otra implementación; no transfieren automáticamente sus resultados a Java. El mutex por identidad protege las DACL compartidas. No ejecutar las sondas C# mientras exista una instancia Java sobre esas herramientas/bases.
5. Portar el inspector y renderizador mediante herramientas nativas fijas y aisladas. Conservar la semántica de PDF/Office/XML/imágenes/audio/video del inspector de referencia. No ejecutar parsers con el token del backend ni sustituir inspección estructural por búsquedas de texto.
6. Acreditar los ocho controles para la cadena definitiva y ejecutar los cinco métodos de ServiciosDocumentalesRealesTest. Después configurar el modo documental completo y ejecutar regresión/E2E HTTPS/Edge con QA sintético. **ArranqueWindows aún fuerza documental=false.**

## Comandos del operador: una línea cada uno

Ensayo independiente ya acreditado; repetir solo ante cambios o una necesidad concreta, nunca simultáneamente con el adaptador:

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Capacidad-AV.ps1
```

Ensayo pendiente del candidato Java, luego de comprobar que sus clases corresponden al código compilado:

```powershell
& C:\req\repo\scripts\qa\Windows-Probar-Antivirus-Java.ps1
```

El bloqueo de memoria no inicia scanner ni concede una excepción. Un fallo de API, scanner, integridad o limpieza no cuenta como aprobado.

## Evidencia clave

- `C:\req\evidencias\fixture-av-exclusion-27a727374a094306b1686e779864b0ac.json`.
- Primera pasada: `C:\req\testdata\isolation-f8b7a69ca2594bbab36f853de9e3cfc1`.
- Pasada instrumentada: `C:\req\evidencias\capacidad-av-475e581eb13d4ba38833037657a85b5f.json` y `C:\req\testdata\isolation-475e581eb13d4ba38833037657a85b5f`.
- Candidato inicialmente bloqueado: `C:\req\testdata\av-java-5387e72ce4ab44438f50ab7e2a7069b0\resultado.json`.
- Logs de guardas: `C:\req\logs\guardas-antivirus-windows.log`. Logs finales: `backend-{h2,mysql}-clamav-final-2026-10-26.log` bajo logs.

No usar el esquema histórico como QA; no borrar/importar/cambiar hashes para conciliar; no llamar Graph/Formulario Público reales, gobierno, WSL o servicios ajenos. No eliminar la exclusión aprobada por TI como parte de una reversión técnica automática. [Reversión conservadora](REVERSION_MIGRACION_WINDOWS.md).
