# Checkpoint interoperabilidad · 31/10/2026

> **Checkpoint vigente posterior:** [primer E2E real acreditado, COM-002497](CHECKPOINT_E2E_FORMULARIO_2026-10-31.md). El corte de preparación conservado abajo ya fue superado por una campaña autorizada de una entrega. Recuperación automática sigue deshabilitada; runtimes de la campaña se detienen y sus evidencias se preservan.

**Preparación terminada; E2E real pendiente. Recuperación pública deshabilitada.**

- Repo Windows `C:\req\repo`, rama `preparacion-interoperabilidad-formulario-2026-10-31`, basada en U5 `48dd236`. Cambios conservados sin commit/push; `main` intacto en `b322c9b`. No reconstruir U5 ni habilitar responder/canalizar/escalar.
- Fuente pública **c8d1a073880d6657b4e52297d04b084b17a077d1**, limpia. Se verificó PID93226/18443, JAR SHA `11038950760953e166ed49d3bb15d9900a147ee84f4b0e80263b3de0773d41df`, configuración y funciones actuales (extranjero/grupos/catálogo249). La referencia de JAR desactualizado era histórica. WSL solo leído, sin recompilar/reiniciar/modificar.
- Origen correcto `https://formulario.defensorianinez.gob.cl`, loopback443→18443. HTTPS Windows200 y Java con CA propia; Bearer ausente401/inválido401/válido404 mediante HEAD inexistente. Cero reclamaciones/descargas/ACK reales.
- Se preparó confianza pública y DPAPI Windows; firma institucional sintética verificada **offline** contra el trust del proceso público activo. No se leyó su clave privada ni se enviaron comprobantes.
- Ajustes: opt-in manual explícito del bootstrap/launcher Windows, siempre sin scheduler; preflights de lectura, helper de recuperación futura una vez, etiquetas extranjeras/grupos y pruebas de compatibilidad. Contratos/firmas/migraciones intactos. Graph siempre simulado.
- **41/41** backend nativo H2 seleccionadas (22 contrato/persistencia,13HTTP,6bootstrap); **51/51** frontend, lint/build y paquete correctos. Se conservan intentos fallidos intermedios y diagnóstico en el informe. No se repitió MySQL destructivo ni la campaña U5/documental completa.
- Candidato `C:\req\bin\siger-9897abd7cd0bd57d.jar`, SHA completo `9897abd7cd0bd57df6f0199e28904517c10d650bd7dd2795e34b2aa7b85084bf`. Smoke preliminar HTTPS200 **con consumidor y workers deshabilitados**, detenido, secretos efímeros retirados. No arrancó el opt-in público manual.
- MySQL QA solo lectura, app DML y TLS1.3 VERIFY_IDENTITY: cero ACK pendientes/COM duplicados/Graph aceptados. Conteos QA preservados. Control persistente `pullEnabled=true` no alterado, consumidor/scheduler cerrados.
- Histórico: **504 filas/59tablas idénticas**, **24objetos/16.869.263bytes** SHA intactos y ACL sin accesos inesperados.
- RAM final observada1,78GiB: no ejecutar AV Windows por debajo de4GiB ni reducir su guarda.
- Evidencia privada `C:\req\evidencias\interop-formulario-2026-10-31-1ac9fc5812e34fddbb62cb590c20e935`; perfil inerte `C:\req\config\formulario-wsl-integracion\perfil.json`. No versionar el directorio privado ni binarios.

## Siguiente paso exacto

Leer [informe y procedimiento](PREPARACION_INTEROPERABILIDAD_FORMULARIO_2026-10-31.md). En una **nueva campaña E2E autorizada**, coordinar con el agente del Formulario una cola exclusivamente sintética: reclamar no permite seleccionar UUID. Verificar RAM y preflights, iniciar candidato documental QA con el opt-in manual y la custodia/trust QA preservados, presentar una sola Comunicación sintética y ejecutar una recuperación. Verificar COM, originales, procedencia y ACK en **ambos procesos** antes de declarar integración E2E. No hacerlo automáticamente al retomar este checkpoint.

Para revertir operación, usar el JAR previo sin opt-in ni perfil; conservar working tree, esquema, custodias y evidencias. No hay migraciones nuevas ni eliminación que ejecutar.
