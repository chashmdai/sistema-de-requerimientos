# GATE U4 · SIGER Windows ↔ Formulario DDN-UBUNTU

Preparación Windows y conectividad verificadas el **02/11/2026**, zona America/Santiago. **E2E entre máquinas pendiente de la ventana coordinada con el responsable Ubuntu.** La QA WSL continúa operativa; no se hizo transición, pausa ni reemplazo de su consumidor.

**Actualización posterior, 02/11/2026 11:04:** Ubuntu instaló la confianza ACK y la sonda independiente Windows confirmó **401/401/404**. U4.2 queda acreditado desde ambos equipos, sin abrir recepción/PULL ni iniciar U4.4. Ver [resultado y propuesta operativa de U4.4](U4_SONDA_CRUZADA_Y_PLAN_2026-11-02.md). Las secciones siguientes conservan el corte inicial de preparación.

## 1. Estado por subgate

| Subgate | Resultado | Alcance acreditado / pendiente |
| --- | --- | --- |
| U4.0 Reconocimiento | Cumplido | Git/H2, Windows/Java/JAR/listeners, perfiles/DPAPI/TLS, MySQL, consumidor, pendientes, custodia y restricciones revisados sin cambiar el runtime activo |
| U4.1 Conectividad | Cumplido para túnel temporal | HTTPS real Windows→SSH→loopback Ubuntu, CA auténtica/SAN/SNI; positivos y negativos TLS. El túnel se cierra al terminar el reconocimiento; no se instala un servicio permanente |
| U4.2 Confianza/configuración | Parcial | CA, token Ubuntu y ambas públicas auténticas disponibles; firma SIGER comprobada offline. Falta instalar el mapa ACK en la configuración activa Ubuntu y habilitar allí la integración técnica; por eso la sonda responde 503 y aún no acredita autenticación 401/401/404 |
| U4.3 Consumidor | Preparación Windows cumplida | Ejecutor de un ciclo aislado, base/identidades/custodia propias, vínculo durable y exclusión real MySQL comprobados. Controles cerrados; ninguna entrega reclamada. El automático QA WSL conserva su origen |
| U4.4 E2E físico | Pendiente, no ejecutado | Requiere las modificaciones temporales que debe ejecutar el Codex Ubuntu, una entrega sintética y la validación cruzada de COM/originales/ACK/confirmación. Las regresiones con productor controlado no sustituyen ese E2E |

Instrucciones transferibles: [procedimiento exacto para Codex Ubuntu](U4_INSTRUCCIONES_CODEX_UBUNTU_2026-11-02.md).

## 2. Fuentes y trazabilidad

- SIGER partió de checkout limpio `main`, SHA **`35d9e6853d756706f8c34973408ccc60323afc5e`**, idéntico al candidato H2 y a `git ls-remote origin refs/heads/main` al reconocer.
- Rama de trabajo nueva: `gate/u4-ddn-ubuntu-2026-11-02`. Main, ramas históricas y runtime preservados. Los cambios U4 se conservan localmente; no se publica ni despliega sobre la QA.
- Se leyeron AGENTS, traspaso/estado H2, operación PULL/ACK, configuración privada, gates, arquitectura/datos/U5 y preparación Windows. También el informe adjunto `RESULTADO_GATES.md` del despliegue físico Ubuntu.
- Formulario instalado desde **`099c3b16e71d57b335496c3a112daccfa38831db`**. El checkout remoto es `7189a95cd449d1a5c6117116f1a360b4fb6b9f63`, posterior documental; no se lo presenta como nueva imagen instalada.
- JAR Formulario verificado: `e7decf6e37ded80dd30e9e60a53cc028994346b82a493aa68b6277c0dd1f427c`. IDs app/proxy observados coinciden con el informe de despliegue.
- Evidencia privada Windows: `C:\req\evidencias\gate-u4-2026-11-02-84be33016b4c4dd59fc0a6e99bcce56d`, puntero `C:\req\scratch\gate-u4.txt`. No se versionan credenciales, originales ni logs privados.

## 3. SIGER QA: estado original y final

Windows Server 2022, MySQL80 **8.0.46**, Java activo **25.0.4.101** explícito; Java global de PATH sigue siendo 21. SIGER conserva:

- PID **163036**, padre launcher **188044**, inicio 01/11/2026 11:20 America/Santiago.
- JAR `C:\req\bin\siger-ae70ab769605c1e9.jar`, SHA-256 **`ae70ab769605c1e93eab0de052ceee46f724a7b9dfe684a45227717d6a07c2f0`**.
- Listener exclusivo `127.0.0.1:8081`, interfaz `https://localhost:8081/`.
- Runtime `C:\req\testdata\runtime-qa-9f6a743c07f1461b93f2ade3ede8d035`.
- Origen consultado: **WSL**, `https://formulario.defensorianinez.gob.cl` por puerto 443, perfil `C:\req\config\formulario-wsl-e2e-20d4349425a943378b12328fb67e5711\perfil.json`.
- Base `siger_institucional_windows_qa`, app exclusivamente DML, JDBC `VERIFY_IDENTITY`, sesión TLS 1.3. Custodia QA: `C:\req\testdata\runtime-qa-b371266c31744ccc96f5f0668fcf7154\archivo`.

| Fotografía operacional | Original registrada | Corte final de preservación |
| --- | --- | --- |
| Control | revisión 13, aceptar/recuperar activos, NORMAL | igual |
| Automático | ESPERANDO_ENTREGAS | igual |
| Ciclos | 15973 | 16272, siguen avanzando |
| Recuperadas / confirmadas / conciliadas | 5 / 5 / 0 | 5 / 5 / 0 |
| Fallos consecutivos | 0 | 0 |
| ACK pendientes / COM duplicados | 0 / 0 | 0 / 0 |
| `communication_registry` / `institutional_inbox` | 15 / 11 | 15 / 11 |

Los contadores son fotografías, no metas de reinicio. Los de H2 eran anteriores; no se intentó retrocederlos. No se hicieron llamadas manuales de recuperación a la QA ni pausas/cambios de control en este gate.

Se comprobaron **33 objetos QA, 3.614.541 bytes**, con cero diferencias de ruta/tamaño/SHA-256 contra el inventario inicial. Sin cambios en hashes/ACL del perfil, tokens DPAPI, trust, privadas temporales del proceso, `run.json`, JAR activo o `hosts`. Los recuentos de las veinte tablas auditadas tampoco cambiaron. La comparación SQL inicial se transcribió de la salida del reconocimiento; el helper reutiliza su nombre de archivo para la fotografía final, indicado en `comparacion-qa.json`.

El histórico `siger_institucional_windows_lab` conserva **59 tablas / 504 filas** y sus huellas por tabla coinciden con H2. Sus **24 objetos / 16.869.263 bytes** coinciden con el manifiesto, con SHA/ACL de objetos y padres verificados. No hubo fixtures, imports, DDL o restauraciones en estas bases preservadas.

## 4. Ruta HTTPS comprobada

```text
Windows https://formulario.defensorianinez.gob.cl:25443
  → 127.0.0.1:25443 (túnel SSH local)
  → admin-ti@192.168.100.14:22
  → Ubuntu 127.0.0.1:24443
  → Caddy → Spring por TLS privado
```

- TCP LAN a SSH: correcto. TCP LAN directo a 24443: no accesible; `ss` remoto muestra únicamente `127.0.0.1:24443` para el Formulario.
- Identidad SSH: clave host ya registrada y coincidente con el servidor, ED25519 **`SHA256:kTJlw0J7HPuhohebA5l1VrrNEaQEzmNEm/TMCL+BF/U`**. Se utilizó `StrictHostKeyChecking=yes`, sin aceptar una clave nueva por conveniencia.
- La misma resolución loopback del dominio ya existente en Windows sirve con el puerto U4 distinto. **No se editó hosts**, DNS, confianza global ni la ruta WSL por 443. El ejecutor rechaza DNS no loopback.
- Sonda Java real: `/api/session` HTTP **200**, **TLSv1.3 / TLS_AES_128_GCM_SHA256**, SAN `formulario.defensorianinez.gob.cl`.
- Certificado servidor SHA-256 DER: `f4c0e5f6f1e4167f658b9a5e0eb08ec0a4ca4b2420bf8f579f2722313e7917e8`.
- CA de archivo: `9ce8ccfc0045660c4f46b88ccee91deca5e88890af7f6dec26c2ef7b7d31557e`, cotejada con copia pública y PKI activa por SSH. CA válida hasta 04/12/2027 UTC.
- Sin CA confiada: **TLS rechazado**. Hostname `127.0.0.1` con la CA correcta: **TLS rechazado**. No se deshabilitaron validación de cadena, hostname ni SNI.
- Curl Windows/Schannel encontró estado de revocación desconocido de la PKI privada. No se empleó `-k`, trust-all ni una excepción de revocación para acreditarlo; la prueba se realizó con el transporte Java y CA explícita que usa el consumidor.

Para volver a abrir el túnel temporal en una consola propia (credencial por entrada privada):

```powershell
ssh -N -L 127.0.0.1:25443:127.0.0.1:24443 -o ExitOnForwardFailure=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=5 admin-ti@192.168.100.14
```

Cerrar esa consola o esa única sesión SSH revierte el transporte. No se instalaron servicios, tareas permanentes, claves SSH nuevas, publicación LAN/Internet o cambios firewall/GPO.

## 5. Confianza y configuración privada

Perfil nuevo, con ACL usuario operativo/SYSTEM/Administradores: `C:\req\config\formulario-ddn-ubuntu-u4\perfil.json`. Tiene CA y trust públicos propios, hash esperado, origen con puerto explícito, key-id, ruta de custodia independiente y flags de recepción/automático cerrados. El token está en `conexion.json.dpapi`, CurrentUser; no se reutiliza el de WSL.

| Material | Estado |
| --- | --- |
| Pública ACK SIGER | Exportada del trust del proceso activo; mapa `siger-ack-trust.json`, key-id `institucional-nna-lab`, SHA de archivo `7888a45c690750b07be974394c327849749d68a98ecbb8e46e12cf43ab836767` |
| Privada ACK SIGER | La existente en DPAPI de SIGER, solo descifrada en memoria y stdin anónimo para el ejecutor; no copiada a Ubuntu |
| Pública Formulario Ubuntu | `publico-candidato`, mapa `formulario-receipt-trust.json`, SHA `5620f27dafcbbc5fc0473d4a158f1823733820381b1322f7061060e5b88c0f2f` |
| CA HTTPS Ubuntu | Auténtica, cotejada por dos ubicaciones y con el informe adjunto; distinta del laboratorio WSL |
| Token técnico Ubuntu | Transferido cifrado y protegido DPAPI; se comprobó en memoria que difiere de WSL, sin imprimir token ni su hash |
| Confianza ACK activa Ubuntu | **Falta**. El archivo público está en intercambio, aún no instalado en `keys/institutional-trust.json` |
| Autenticación técnica del endpoint | **No acreditada todavía**: 503/503/503 por integración deshabilitada; esperar 401/401/404 tras preparación Ubuntu |

El intercambio del token usó una envoltura RSA-OAEP-SHA256 efímera; únicamente la pública de transporte viajó a Ubuntu y únicamente ciphertext volvió. La privada de transporte temporal y la copia pública temporal se retiraron al terminar. Esta clave no es una identidad institucional ni reemplaza firmas/certificados.

## 6. Cambios implementados y aprovisionamiento

1. `EnsayoFormularioUbuntu`: entrada acotada por stdin, acciones SONDA/MIGRAR/CICLO, sin servidor web ni scheduler. Mantiene HTTPS, CA explícita, hostname, no redirects, versiones 1.0/1.1, streaming, firmas y el receptor COMMIT→ACK existente.
2. `ExclusionEnsayoUbuntu`: locks MySQL dedicados a base U4 y origen lógico DDN-UBUNTU, conexión propietaria verificada antes de operaciones; falla cerrado sin readquirir silenciosamente.
3. `VinculoOrigenEnsayo` y **V14 aditiva H2/MySQL**: una base dedicada queda vinculada a origen/CA/trust público/firmante/trust propio/custodia. Cambios se rechazan antes de conciliación. Una base con inbox/COM/documentos anteriores no puede adoptarse como otro origen. V1–V13 y sus checksums no se editaron.
4. Launchers y sondas Windows nuevos: ensayo DPAPI, provisionador administrativo acotado, preflight/control U4, sonda TLS y comprobación de permisos/exclusión/custodia.

La limitación H2 original permanece: `ArranqueWindows`, `HabilitacionNucleo`, launcher QA y lock QA no se modificaron. Además, el receptor histórico usa un `source` lógico compartido y la conciliación no separa instalaciones: **cambiar solo la URL del perfil QA no es una transición segura**. Por eso U4 usa base/custodia propias; no pretende habilitar varios productores dentro de la QA preservada.

El usuario autorizó expresamente leer el PSCredential root DPAPI administrativo preexistente para aprovisionar desarrollo. Se validaron ACL/identidad, TLS 1.3, privilegios y ausencia de colisiones. Esa lectura fue una excepción acotada a la credencial indicada por la referencia histórica; no se modificaron archivos/servicios de `C:\agente`. Root se usó exclusivamente en el helper de aprovisionamiento, nunca para SIGER.

Recursos nuevos:

- `siger_u4_ddn_ubuntu`, utf8mb4/utf8mb4_0900_ai_ci, V1–V14 aplicadas.
- `siger_u4_ubuntu_app@localhost`: exclusivamente SELECT/INSERT/UPDATE/DELETE en ese esquema.
- `siger_u4_ubuntu_migrator@localhost`: DML y CREATE/ALTER/DROP/INDEX/REFERENCES/CREATE VIEW exclusivamente en ese esquema, sin GRANT OPTION.
- Ambas cuentas nuevas requieren SSL; passwords aleatorios distintos de 256 bits, persistidos como PSCredential DPAPI CurrentUser en `C:\req\config\mysql-u4-ubuntu-{app,migrator}.credential.xml`, con ACL privadas.
- Ambas rechazaron SELECT contra QA/histórico (MySQL 1142). No se alteraron cuentas existentes ni configuración global de MySQL80.
- Custodia nueva `C:\req\testdata\u4-ddn-ubuntu-…\archivo`, ruta exacta en el perfil privado. NTFS privado y barreras reales comprobados; cero originales de entrada.
- Base U4 con cero inbox/COM/ACK pendientes; vínculo de origen preparado y controles revisión 0, `false,false,SOLO_CONSULTA`.

Artefacto inmutable preparado: **`C:\req\bin\siger-9a9e14f9c664b121.jar`**, SHA-256 **`9a9e14f9c664b1217d325a1228942bc236c594726d884fcad37f4d46b5e1194a`**. No está ejecutándose como servidor ni reemplaza al artefacto QA. Sus COM futuros se conservarán en U4; no aparecerán por esa sola ejecución en el buzón de la base QA existente. La unicidad de folio se verifica dentro del esquema del ensayo; no se autoriza importar sus folios al registro QA/institucional.

## 7. Pruebas y límites de evidencia

| Prueba | Resultado |
| --- | --- |
| Build oficial final Windows/Java25/Node24 | Correcto: npm ci, lint, frontend **51/51**, build; Maven verify correcto |
| Backend final | **274 métodos: 265 correctos, 9 omisiones opt-in, cero fallos/errores**. Las tres pruebas históricas de estrés siguen excluidas por tag, sin exclusiones nuevas |
| Guardas U4 nuevas | Rechazo de QA/histórico/root/TLS débil/origen DB remoto, separación app/migrador, rechazo de cambio de vínculo y adopción de datos anteriores |
| Exclusión | Cuatro pruebas con dobles JDBC; además MySQL real rechazó segunda instancia y permitió readquirir ambos locks tras liberarlos, sin reclamar entregas |
| MySQL U4 real | Migración correcta, permisos exactos, VERIFY_IDENTITY/TLS1.3, aislamiento 1142, vínculo durable/custodia preparados, controles cerrados |
| Sonda HTTPS real entre máquinas | 200 con CA/hostname correctos; negativos sin CA y SAN incorrecto rechazados |
| Sonda de integración real | 503 sin token/falso/autorizado por gate cerrado; firma SIGER offline correcta, **0 claims / 0 ACK** |
| Recuperación/idempotencia | Regresión existente con productor controlado: firma/hash alterados, descarga interrumpida, respuesta perdida, reserva vencida, reinicio, duplicados, pérdida de exclusión antes/después de COMMIT. No E2E físico Ubuntu |
| PowerShell / preservación | Scripts nuevos parseados; rechazo de apertura U4 sin flag de ventana antes de JDBC; hashes, ACL, originales y estado QA/histórico conservados |

Hubo una pasada Maven manual sin temporales NTFS privados: falló en las guardas de archivos. Se conserva `backend-final.log`; **no se reporta esa pasada como aprobada**. Se corrigió la invocación utilizando de nuevo `Build-Siger.ps1`, sin rebajar ACL ni excluir tests; la pasada final completa consta en `build-final-u4.log`.

No se repitió antivirus/inspector real: los workers no cambiaron y la memoria libre inicial era inferior a la guarda de 4 GiB. Sus omisiones opt-in no acreditan una calificación nueva. No hubo llamadas Graph, CU o PISEE, fixtures destructivos ni una Comunicación sintética creada en el Formulario por este Codex Windows.

Incidencia de herramientas: una invocación de reconocimiento a `python` activó PyManager, que autoactualizó su gestor a 26.4 e instaló Python 3.15.2 en el perfil del usuario. El runtime nuevo se retiró por ID exacto con el gestor; no es dependencia de SIGER. La autoactualización del gestor permanece registrada en evidencia, sin tocar runtimes/procesos institucionales.

## 8. Coordinación pendiente y reversión

El siguiente paso pertenece al Codex Ubuntu: instalar el mapa público ACK auténtico, habilitar solo integración técnica con recepción aún cerrada, recrear exclusivamente app y pedir repetir la sonda. Después se acuerda una ventana sintética con el [procedimiento transferible](U4_INSTRUCCIONES_CODEX_UBUNTU_2026-11-02.md). No faltan credenciales Windows ni es necesario enviar privadas por chat.

Ese procedimiento ya está copiado en Ubuntu como `/home/admin-ti/operacion-formulario/2026-11-02/U4_INSTRUCCIONES_DESDE_WINDOWS.md`, SHA-256 `92d41a73a00b7158acbb69048c2433d57b363688d12239f5b49809ebf11bbaef`, idéntico al archivo Windows. Basta transferir al Codex Ubuntu la instrucción de leerlo y ejecutar su preparación cerrada. El túnel temporal de este reconocimiento ya fue cerrado normalmente, sin alterar el listener WSL/SIGER.

No hay evidencia nueva de reserva, documento, COM, ACK o CONFIRMADA del origen Ubuntu. Esos puntos se completarán con UUID/hashes/folios/recibos reales de la campaña, incluidos ensayos de interrupción delimitados. U4.4 permanece pendiente hasta entonces.

Reversión actual:

1. La QA no requiere restauración/reinicio: se conservaron su mismo proceso, JAR, perfil, DB y custodia.
2. Cerrar únicamente la sesión/túnel SSH U4. No terminar procesos por nombre ni servicios globales.
3. Conservar inactivos el perfil/credenciales/base/custodia/artefacto U4 y la evidencia; no borrar bases/usuarios/originales como rutina.
4. Si después se abre una ventana, cerrar nuevos ingresos, drenar el ciclo y conciliar ACK pendientes contra **ese mismo origen** antes de cerrar PULL o retirar confianza. Cerrar control U4 mediante helper CAS; restaurar únicamente flags/proxy temporales Ubuntu por su responsable. No purgar leases ni recibos.
5. Para volver al código H2, conservar el commit/rama U4 y cambiar a la rama previa con árbol limpio, sin reset/clean ni eliminar ramas. El runtime no depende del checkout.

U5, Graph, CU, PISEE, futura VM Windows, firewall/GPO y servicios institucionales ajenos permanecen fuera de intervención.
