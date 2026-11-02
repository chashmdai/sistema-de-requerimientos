# Primera Comunicación efectiva · Formulario WSL → SIGER Windows

## Estado de la campaña

**E2E REAL ACREDITADO: una Comunicación sintética 1.0 con un TXT limpio.** Se ejecutó una sola recuperación HTTPS desde SIGER Windows contra el proceso del Formulario WSL y se comprobó su confirmación en ambos sistemas. No confundir con las pruebas sintéticas en proceso del 27/10 o la preparación de lectura del 31/10. No acredita todavía automatización ni producción.

## Autorización y aislamiento

El usuario autorizó una única recuperación manual de una Comunicación sintética con adjunto limpio, su registro, custodia y ACK real. No autorizó automatización, producción, proveedores gubernamentales, correos reales ni cambios U5 pendientes.

Se coordinó con el agente responsable del Formulario, chat «Leer encargo maestro SIGER», sin delegarle modificaciones de SIGER. El Formulario original estaba detenido tras la pausa WSL para liberar memoria. No se ejecutó su `resume.py` ni se restauraron todos los contenedores.

El responsable público preparó recursos QA nuevos:

- Raíz `/home/nexbyteproveedor/.local/share/siger/e2e-siger-8e59add7c88f48aaa652f308d4f1f090`.
- MySQL QA `siger-e2e-8e59add7c88f-mysql`, volumen exclusivo `siger-e2e-8e59add7c88f48aaa652f308d4f1f090-mysql`. Esquema `robust_public_test`, doce migraciones existentes aplicadas intactas al esquema vacío. No montó volúmenes históricos.
- Una JVM del mismo JAR público, PID2556/18443, Xmx192m, y el Caddy existente iniciado sin cambios, HTTPS canónico `https://formulario.defensorianinez.gob.cl`. Clamd real local13310. Demás servicios anteriores detenidos.
- CU/PISEE/multimedia/retirada/online/aceptación72h deshabilitados; ninguna dependencia del agente de respaldo.
- Antes de crear el ingreso: **cero submissions, journeys y attachments**; prueba `empty-queue-proof.json` conservada en evidencia Windows.

La instancia se considera Formulario real porque ejecuta el JAR de la aplicación, API ciudadana, persistencia MySQL, antivirus, endpoints de integración y ACK reales. Usa una cola/base exclusivas sintéticas; no representa producción ni el estado histórico del Formulario.

## Identificación de procesos y configuración conservada

| Componente | Identificador |
|---|---|
| Repositorio público | `c8d1a073880d6657b4e52297d04b084b17a077d1`, limpio |
| JAR público | SHA-256 `11038950760953e166ed49d3bb15d9900a147ee84f4b0e80263b3de0773d41df` |
| JAR SIGER conservado | `C:\req\bin\siger-9897abd7cd0bd57d.jar` |
| SHA-256 SIGER | `9897abd7cd0bd57df6f0199e28904517c10d650bd7dd2795e34b2aa7b85084bf` |
| Runtime SIGER | `C:\req\testdata\runtime-qa-ba5aada6c21c4068a6220ef3b29a731a`, PID192804, HTTPS localhost8081 |
| Custodia QA preservada | `C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo` |
| Copia DPAPI temporal | `C:\req\config\formulario-wsl-e2e-8e59add7c88f48aaa652f308d4f1f090\perfil.json` |
| Evidencia privada | `C:\req\evidencias\e2e-formulario-conjunto-2026-10-31-b7641f2f6c724b2a9781bda545e3246a` |

El perfil DPAPI original, certificados, trust original y candidato no se sustituyeron. El agente público creó token/firmante QA nuevos; SIGER los consume mediante una copia DPAPI privada. Solo se exportó a su cola la clave **pública** institucional, nunca la privada. Los scripts ahora admiten esa copia con rutas acotadas y guardan su asociación al run. No se recompiló el candidato ni cambió el origen HTTPS autorizado.

Preflights repetidos: Windows/Java HTTPS válido, HEAD sin credencial401/inválida401/válida404, firma ACK verificada offline con confianza del productor QA, MySQL Windows TLS1.3 VERIFY_IDENTITY y aplicación DML. Cero ACK pendientes previos. Memoria: 6,89GiB inicial, 5,03GiB con origen QA, **4,66GiB después de ambas JVM**; se conserva la guarda4GiB y todos los límites. No se detuvieron procesos ajenos.

El primer preflight de esta sesión encontró el proceso público apagado. La inspección heredada iniciaba implícitamente la distro al invocar WSL; no inició aplicaciones. Se agregó una guarda que consulta primero distribuciones en ejecución y no arranca una detenida para inspeccionarla.

## Verificación acordada

Una sola invocación a `Windows-Recuperar-Formulario-Una-Vez.ps1`, después de prueba de exclusividad y manifiesto del ingreso. El planificador permanece falso. Se compararán UUID, hashes de contenido/procedencia, descriptor y bytes, COM/folio y firmas; el agente público verificará su estado confirmado y el mismo ACK. La comprobación local posterior abrirá la custodia solo con el runtime detenido y repetirá idempotencia contra el documento ya conservado, **sin otro reclamo público**.

Antes: QA tenía ocho COM, diez documentos, un Requerimiento y un Caso. El histórico independiente conserva504filas/24objetos. No se ejecutan regresiones destructivas ni se reutiliza el histórico para la prueba.

## Resultado comprobado

| Dato | Valor / comprobación bilateral |
|---|---|
| Operación pública | `8c728e99-fa44-40dd-8779-8fb2a877a933` |
| Comunicación institucional | `893a552c-67cd-4f91-9495-1af5f12dae8f` |
| Folio | **COM-002497**; el número no se reinició ni se exige continuidad sin huecos |
| Documento / ACK | 1.0 / 1.0, canal `FORMULARIO_WEB_PUBLICO` |
| Tipo | `ADULTO_DECLARADO`, `DOCUMENTO_EXTRANJERO`, sin RUN y sin autenticación CU |
| Grupo NNA | Un grupo conservado en campos; una persona presentadora, ninguna persona ficticia por el grupo |
| Hash contenido | `52f4b670745a0212c7a674a0183c608ef359e4e3ed58da75e94b814e4acf061b` |
| Hash procedencia | `92705c4d142cec594c819c49dc21fc61bae65945fa02175dffbbd3747614866c` |
| Adjunto | `59fcc6b0-8c4e-4e27-8273-16c923e8e917`, `prueba-integracion-siger.txt`, text/plain, **126bytes** |
| SHA-256 original | `3b916bc0002526beb06af325e41f469ef49c43fa67d9b23c14623d0bf2dea0ff` |
| SHA-256 firma ACK decodificada | `864aa1a8e535a7178c5725196d8acb68dd4ad75497d647817051f44faa9e02f6` |
| SHA-256 payload ACK decodificado | `fed17128097d105e9efc8ef7fa83c9d91f8b277d74adf7b0a6de94dbf3a358a2` |
| Respuesta SIGER | recuperadas1, confirmadas1, fallidas0, solapadafalse |
| Formulario | API `REGISTRADA_SIGER`, SQL `CONFIRMADA`, un intento, una Comunicación, cero reclamables |
| Custodia / firmas | Documento y fuentes íntegros; original idéntico en MySQL público y NTFS institucional; firmas Ed25519 verificadas por ambos lados, también OpenSSL por el responsable público |
| ACK pendiente SIGER | false; comprobante almacenado idéntico al aceptado por el Formulario |

El Formulario creó la Comunicación por su **API ciudadana real** con sesión/CSRF y antivirus INSTREAM real de `/usr/sbin/clamd` (ClamAV1.5.4/28163). No se usó un productor HTTP doble ni se insertó directamente una entrega en la cola. El reenvío ciudadano idempotente devolvió el mismo recibo sin crear otra Comunicación. La captura y validación pública correspondieron a su agente; el SIGER Windows hizo la reclamación, descarga, registro y ACK.

La fuente cerró nuevas recepciones antes del reclamo. La evidencia `unique-queue-proof.json` acreditó exactamente una fila `PENDIENTE`, intentos0 y reserva/ACK ausentes. Después, `public-final-sanitized.json` confirmó un intento y cero reclamables. El lease-id retenido en la fila confirmada es metadato histórico de idempotencia, no una reserva activa.

El verificador institucional, en una **JVM nueva tras parar el runtime**, cargó clases del mismo JAR candidato, validó ambas firmas/canonicalización y comparó hashes con el manifiesto anterior a la extracción. Verificó los bytes y metadatos privados. Repetir localmente `guardarCompleta` devolvió el mismo recibo y dejó invariables los conteos; no construyó un cliente público ni efectuó otro PULL/ACK. La comparación programática de diez campos entre los JSON finales de ambos responsables quedó en `verificacion-bilateral.json`.

Conteos QA finales: **9 COM y11documentos**, frente a8/10; permanecen **un Requerimiento y un Caso**, cero COM duplicados, cero ACK pendientes y cero aceptaciones Graph. Recibir no abrió una gestión nueva. El histórico conservado sigue idéntico en sus59tablas/504filas y24originales/16.869.263bytes; se verificaron SHA-256 y ACL. El candidato y los cuatro archivos del perfil DPAPI original tienen las mismas huellas previas.

## Incidencias y límites de lo acreditado

- El primer envío ciudadano encontró el control de aceptación de la BD QA nueva cerrado por defecto y recibió503, **antes de crear una Comunicación**. El agente público lo abrió por su API operacional auditada, creó la única entrega y volvió a cerrar aceptación, dejando recuperación habilitada para el reclamo. Conservó un borrador preparatorio no recibido como evidencia. No se cambió código, contrato, datos o controles históricos para resolverlo.
- El primer verificador Windows terminó sus comprobaciones Java correctamente, pero el wrapper PowerShell seleccionó el último carácter de un único JSON en vez de su última línea. Se corrigió el selector con un array explícito y se repitió únicamente la verificación local. No hubo segunda extracción ni ACK. La incidencia y sus límites están en `correccion-harness-posparada.json`.
- Se probó **una Comunicación 1.0/TXT**, no multimedia1.1, UI de navegador, múltiples recepciones ni toda la resiliencia del automático. El procesamiento y comprobante fueron reales, con datos exclusivamente sintéticos. No se volvieron a contabilizar las pruebas H2/MySQL/frontend antiguas como resultados nuevos.
- No se ejecutó otra calificación del AV Windows: se respetó su guarda física y se habilitó el modo documental ya calificado. El análisis de este original de ingreso lo hizo el antivirus real del Formulario; SIGER verificó firma, tamaño, SHA-256 y durabilidad. No se ejecutó un parser bajo el token del backend ni se amplió una exclusión antivirus.

## Cierre y reproducción

SIGER se detuvo mediante el `stop` de **su propio run**. Se comprobó PID192804 ausente, secretos efímeros retirados y scheduler deshabilitado. Su perfil temporal queda inerte sin opt-in. El responsable público verifica y cierra exclusivamente su QA, conservando volumen y evidencia; el histórico no se restaura ni se elimina. La constancia final de su parada se conserva en la campaña.

**Cierre bilateral confirmado:** el responsable público cerró `aceptarNuevos=false` y `recuperar=false` por API auditada, detuvo su JVM2556, ese Caddy y el MySQL QA nuevo (exit0, sin OOM), preservando volumen/configuración/claves. Clamd previo quedó activo; no se reanudaron históricos. No quedan listeners443/18443/13306/8081. Su dump QA final tiene35.339bytes y SHA-256 `1a62b516dc494842b74a3e35ed0fa64853673f7480a25b0b93d7a41964657db7`; no se copia a Git. Se conservaron `publico-shutdown-final.json` y `publico-control-final-closed.json`.

Con el Formulario **ya detenido**, se releyeron y verificaron desde Windows el registro, las firmas y el original privado: mismo COM y huellas, sin conexión pública ni nuevo ACK. Esta prueba acredita independencia de la evidencia conservada; no equivale a haber probado el planificador automático ante una caída.

Para una repetición futura, no reutilizar ciegamente los UUID o recibos de esta prueba ni vaciar su cola confirmada. Coordinar otra entrega sintética o instancia QA con su responsable, confirmar exclusividad y RAM, y reutilizar el procedimiento de preparación con `-RuntimeRoot` de la nueva raíz y `-Perfil` DPAPI copia independiente. No ejecutar `resume.py` de la pausa general.

Comando institucional utilizado (una invocación):

```powershell
& C:\req\repo\scripts\Windows-Recuperar-Formulario-Una-Vez.ps1 -Run C:\req\testdata\runtime-qa-ba5aada6c21c4068a6220ef3b29a731a -AutorizadoE2ELocal
```

El marcador durable `intencion-recuperacion-unica.json` se escribió antes de la solicitud. Ante ambigüedad no repetir ese comando: leer primero el COM/ACK y el estado público. Para inspeccionar el registro ya conservado, sin red pública y con el runtime detenido:

```powershell
& C:\req\repo\scripts\Windows-Verificar-Comunicacion-E2E.ps1 -Run C:\req\testdata\runtime-qa-ba5aada6c21c4068a6220ef3b29a731a -Operacion 8c728e99-fa44-40dd-8779-8fb2a877a933 -ContenidoSHA256 52f4b670745a0212c7a674a0183c608ef359e4e3ed58da75e94b814e4acf061b -ProcedenciaSHA256 92705c4d142cec594c819c49dc21fc61bae65945fa02175dffbbd3747614866c -Adjunto 59fcc6b0-8c4e-4e27-8273-16c923e8e917 -AdjuntoSHA256 3b916bc0002526beb06af325e41f469ef49c43fa67d9b23c14623d0bf2dea0ff -Tamanio 126 -Evidencia RUTA_NUEVA_PRIVADA_DE_EVIDENCIA
```

El verificador está acotado a este escenario extranjero/grupo y al SHA de este candidato; no es una API administrativa ni un recibidor alternativo. Usa credenciales DPAPI por stdin y no imprime cuerpos ni claves.

## Operación posterior

El diagnóstico específico para el automático está en [DIAGNOSTICO_CONSUMIDOR_AUTOMATICO_2026-10-31.md](DIAGNOSTICO_CONSUMIDOR_AUTOMATICO_2026-10-31.md). Ninguna de esas recomendaciones activa servicios o scheduler. U5 pendiente sigue sin cambios.
