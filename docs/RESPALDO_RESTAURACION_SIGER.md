# Respaldo y restauración institucional

Este procedimiento prepara una capacidad verificable; **H2 no ejecuta restauración sobre el laboratorio preservado ni detiene los procesos actuales**. Una copia de JAR o un dump sin originales/claves no es un respaldo completo.

## Unidad de conservación

- MySQL: inbox firmado, registro COM/folios, core, identidad/roles/territorio, documentos/representaciones, tareas, eventos, notificaciones/outbox, confirmaciones/ACK e idempotencia.
- Almacenamiento privado: originales ciudadanos, gestión funcionaria, derivadas/páginas y diarios necesarios. Mantener relaciones/ACL; no duplicar binarios por Caso.
- Artefacto por SHA, Git SHA, manifiestos de herramientas/definiciones y configuración documental.
- CA/trust y claves firmantes, TLS, identidad de ejecución y recuperación privada de secretos/DPAPI. Respaldo cifrado separado y acceso auditado, no valores dentro de informes.

`institutional_inbox.body`/recibos y documentos originales son inmutables. No normalizar JSON, recomprimir imágenes/PDF o volver a firmar registros al respaldar/restaurar.

## Preparar un backup consistente

1. Identificar exactamente instancia/esquema/custodia/origen, versión MySQL/Flyway y tamaño/espacio. Crear destino privado nuevo; nunca usar `C:\agente` como auxiliar.
2. En una ventana aprobada, pausar el consumidor y drenar; cerrar mutaciones/ingresos según política operativa. Las vistas pueden producir nuevas derivadas: para una copia offline coherente detener solo la aplicación propia después de confirmar drenaje y autorización, no MySQL80 compartido.
3. Dump lógico consistente de InnoDB con herramienta compatible, identidad de backup acotada y TLS verificado. No copiar archivos físicos MySQL 8.4→8.0. Credenciales mediante mecanismo privado/archivo temporal con ACL, nunca `-pPASSWORD` o entorno global. Evaluar permisos SELECT/SHOW VIEW necesarios; no recurrir a root por conveniencia. No incluir objetos ajenos al esquema.
4. Copiar el almacén privado y manifiestos conservando estructura/ACL. Con aplicación drenada/parada, cada documento SQL debe tener un original íntegro con tamaño/SHA correspondiente; registrar aparte temporales u objetos sin vínculo para recuperación, no borrarlos.
5. Generar manifiesto SHA-256 del dump, artefacto y objetos, recuentos/tablas, versión/checksums Flyway y estado ACK/outbox. El manifiesto no incluye relatos, nombres ciudadanos, RUN ni secretos. Cifrar/trasladar el conjunto por canal aprobado y comprobar copia independiente.
6. Reabrir la misma instancia cuando corresponda; confirmar controles/servicios y sondeos. Nunca activar por restauración Graph real, proveedores o retención pública.

No se fija frecuencia/RPO/RTO institucional: necesitan aprobación. `innodb_flush_log_at_trx_commit`/durabilidad, capacidad NTFS y el mecanismo de snapshots deben comprobarse, no degradarse para obtener rendimiento.

## Ensayo de restauración aislada

1. TI provisiona máquina/esquema/almacenamiento nuevos, sin colisión y sin conectividad automática hacia el productor real. Todo control de extracción/envío queda cerrado inicialmente.
2. Restaurar dump lógico con identidad de migración/restauración de alcance limitado y versión compatible. No editar checksums ni ejecutar Flyway repair para ocultar diferencias. V1–V13 no cambian.
3. Restaurar originales con ACL correctas y rutas internas coherentes; comprobar cada descriptor/hash/tamaño, claves referenciadas, FK y ausencia de archivos comprometidos/enlaces.
4. Reprovisionar certificados/trust/secretos bajo identidad aprobada. DPAPI de otro host/usuario no se presume descifrable. Conservar claves públicas históricas; rotar privadas solo mediante procedimiento coordinado.
5. Arrancar cerrado, verificar COM/folios/firmas/documentos y relaciones, permisos/roles y estados de tareas/notificaciones/ACK. La pérdida de una respuesta no debe crear otro registro ni otro folio.
6. Verificar que no se enviarán correos antiguos o confirmaciones a un origen distinto. Clasificar pendientes/leases vencidas con su productor; no limpiar tablas para arrancar.
7. E2E con un productor aislado y datos sintéticos autorizado; demostrar recuperación, integridad e idempotencia. La réplica de ensayo nunca toca la cola original.

Un backup solo se acredita después de un ensayo completo verificable. [Gates](GATES_DESPLIEGUE_SIGER.md) exige esta prueba antes de operación institucional. No se declara ensayada una restauración nueva en H2 si únicamente se compararon hashes de la conservación existente.

## Reversión no destructiva de código

Mantener artefacto/configuración previos inmutables y sus hashes. Git permite conservar/revertir cambios de fuente sin sustituir el JAR activo. Después de una instalación autorizada, revertir binario solo si es compatible con el esquema y bajo ventana drenada; no bajar esquema ni borrar columnas/filas/originales. Las migraciones futuras requieren estrategia aditiva compatible o recuperación independiente aprobada.

Las evidencias históricas de migración 504 filas/24 objetos y [reversión Windows](REVERSION_MIGRACION_WINDOWS.md) permanecen. No usar el esquema histórico o la QA integrada como destino de ensayo.
