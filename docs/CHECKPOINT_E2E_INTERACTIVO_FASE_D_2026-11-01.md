# E2E interactivo · Recepción completada, revisión humana pendiente

**No detener SIGER ni Formulario. No repetir la recuperación. No activar automatización.**

El humano autorizó expresamente una única recuperación HTTPS de `d4b72201-26fe-4334-aabb-a910ec076fc3`. Se ejecutó **una vez**, resultado1recuperada/1confirmada/0fallidas. El nuevo registro es **COM-002498**, UUID institucional **c717e0d1-7bf0-4de8-a654-17431de01361**. No se generó ni reenvió el ingreso ciudadano ni se utilizó COM-002497 como sustituto.

## Estado definitivo

- Documento1.0, adulto declarado, negociación1.0/1.1. ACK1.0 del canalFORMULARIO_WEB_PUBLICO, ambas firmas y huellas válidas.
- PNG622233bytes y PDF457214bytes conservados. Descriptores, originales públicos, custodia privada NTFS y descargas institucionales coinciden por SHA-256. Hashes intactos después de previsualizar ambos documentos.
-26controles Edge/HTTPS con `nexbyte` correctos: búsqueda del nuevoCOM, detalle, formulario íntegro, descargas, previsualización real de primera página PNG/PDF y denegación sin sesión.
- MySQL QA: exactamente1COM para esa operación y2documentos; sin ACK pendientes, duplicados, Casos o Requerimientos creados por la recepción.
- COMMIT antes de ACK: verificado en secuencia transaccional del artefacto ejecutado y eventos4043COMUNICACION_REGISTRADA →4044ACK_PUBLICO_CONFIRMADO. No se hizo captura de paquetes del protocolo COMMIT.
- Formulario: **CONFIRMADA**, exactamente1intento, mismoCOM/UUID, ACK Ed25519 y hashes de payload/firma coincidentes. Cola1total/1confirmada/0pendientes/0reclamables.
- Controles de ambos lados **recuperar=false**, revisión4. Nuevas recepciones públicasfalse. Schedulerfalse y Graph realfalse.
- Histórico504filas/59tablas idéntico y24objetos/16869263bytes SHA-256/ACL intactos. Sin migraciones, push, cambios de facultades U5 ni proveedores reales.

## Interfaces dejadas disponibles

- SIGER `https://localhost:8081/`, cuenta QA **nexbyte** y contraseña privada existente sin cambio. Menú **Comunicaciones** → buscar **COM-002498** → **Original recibido** → **Documentos del ingreso**.
- Enlace directo: `https://localhost:8081/#comunicacion/c717e0d1-7bf0-4de8-a654-17431de01361`.
- Formulario: mantener pestaña original `https://formulario.defensorianinez.gob.cl/?vista=formulario-adulto#communication-receipt`, pulsar **Actualizar estado**. Con sesión propietaria vigente debe presentar **Comunicación registrada**, nuevo folio y **Descargar comprobante institucional**. No se usaron ni copiaron cookies del humano para verificarlo. La JVM se había detenido antes de la prueba; si responde404 por sesión ya no vigente, informar al agente público sin reenviar, alterar propietario ni ocultar esa limitación. El ACK y la recepción ya están comprobados en ambos sistemas.

SIGER actual: JavaPID246096, launcherPID258800, run `C:\req\testdata\runtime-qa-73993af4cf8f4991b5fe6e5d0412c732`. Binding127.0.0.1:8081, HTTPS válido. Launcher QA con **`-MantenerDisponible`**, sin vencimiento automático para respetar la revisión humana; no se instaló servicio ni tarea. Sigue admitiendo `stop` únicamente cuando el humano autorice cerrar. Perfil20d434…, JAR9897abd…, custodia y trust combinados anteriores sin cambio.

FormularioJVM2056, contenedorMySQL20d434… yCaddy permanecen activos bajo responsabilidad de su agente. Última comprobación Windows de ambasURL:HTTP200 con TLS normal. No llamar a scripts de parada al entregar resultados.

## Evidencia y continuación

Informe `docs/E2E_INTERACTIVO_SIGER_2026-11-01.md`. Campaña privada `C:\req\evidencias\e2e-interactivo-fase-d-2026-11-01-7b6cfd75abbc44ca8e089d441267f43b`. Punteros `C:\req\scratch\ultimo-e2e-fase-d.txt` y `runtime-e2e-interactivo.txt`.

El marcador `una-invocacion-autorizada.json` fue creado de forma exclusiva y forzado a disco antes de invocar. No repetir el script de extracción. Nuevos verificadores solo leen SQL/archivos o acceden como funcionario; no requieren parar SIGER, no firman de nuevo ni reclaman/ACK. Claves privadas y cookies no aparecen en evidencias/Git. Logs abiertos pendientes de hash hasta un cierre humano autorizado.

**Siguiente paso:** entregar al humano nuevo folio y enlaces; esperar su revisión de ambas interfaces. Ante un problema de consulta, diagnosticar con lectura y conservar servicios. No interpretar una consulta fallida de la sesión ciudadana como autorización para reenviar o cambiar propietarios. No avanzar a automatización ni U5 pendiente.
