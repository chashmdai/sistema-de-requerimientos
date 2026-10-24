# Reversión no destructiva de la integración completa

La unidad se ejecuta en una instancia y esquema institucional nuevos. El Formulario Público y los servicios anteriores no se reconfiguraron.

1. Detener exclusivamente la instancia nueva con `python3 scripts/Institucional-Completo-Linux.py /home/nexbyteproveedor/.local/share/siger/institucional-completo-lab --detener`. El script verifica el PID, JAR y configuración antes de enviar SIGTERM. No usar kill por nombre ni detener Docker/MySQL compartidos.
2. Conservar íntegros `robust_inst_formulario_lab`, el directorio privado `archivo`, claves, comprobantes y las evidencias. Los ACK ya aceptados no se deshacen: SIGER mantiene la obligación de conservar esos originales.
3. No aplicar migraciones DOWN, no modificar checksums V1–V9, no reducir MEDIUMTEXT, no borrar tablas, esquemas, volúmenes ni archivos. Si se necesita retirar la funcionalidad en una instalación compatible, detener el consumidor y desactivar su flag explícito. Las confirmaciones pendientes permanecen durables.
4. El código anterior puede seguir ejecutándose sobre sus servicios/esquemas anteriores. No apuntar el JAR anterior a la base nueva: desconoce el tipo `FORMULARIO_COMPLETO` y no puede interpretar sus documentos. Un rollback de código no convierte documentos completos en históricos.
5. Para reanudar el laboratorio, usar el mismo directorio, esquema y custodia: arrancar el JAR verificado con el script y `--migrar`. Flyway valida lo existente. La conciliación recupera el mismo COM y comprobante; verificar integridad antes de permitir nuevos ACK.
6. Ante un original ausente/alterado, cerrar la recuperación mediante el control operacional, conservar evidencia y restaurar desde respaldo autorizado o recuperar el mismo original desde el origen con reserva válida si todavía está disponible. No sustituir huellas, re-firmar contenido ni forzar ACK.

Credenciales privadas de laboratorio: fuera de Git. Su eventual revocación/retirada debe coordinarse tras cerrar pendientes y resguardar la custodia; no borrar confianzas necesarias para verificar documentos históricos.