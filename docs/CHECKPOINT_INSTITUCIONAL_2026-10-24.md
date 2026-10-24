# Checkpoint institucional — 24/10/2026

**Unidad cerrada en laboratorio:** Comunicación completa 1.0/1.1 y originales conservados en SIGER, COM único, lectura autorizada y ACK firmado después del COMMIT. Producción no habilitada.

- Repositorio: `/home/nexbyteproveedor/proyectos/sistema-de-requerimientos`.
- Rama: `integracion-formulario-completo-multimedia-2026`.
- Base conservada: `86cfe31`.
- Commit principal de implementación y pruebas: `1c74688`.
- Corrección final de errores y verificador: `ec98dbf`.
- El commit que contiene este checkpoint añade contratos, informe, resultados y reversión. Consultar `git log -3 --oneline`.
- Referencia pública comprobada: `a84d2e718a824e01cc8bd8cc78fba5e918d483d3`. `formulario-web` limpio y sin cambios; no se hizo push.

## Comprobaciones

118 pruebas H2 y 118 MySQL correctas, con tres métodos históricos de carga/concurrencia excluidos explícitamente. Las 33 pruebas nuevas cubren contratos, procedencia, custodia, fallos y acceso documental. Frontend: 18 pruebas, lint y build correctos.

Tres circuitos E2E reales sobre HTTPS 19443/20443: adulto declarado 1.0 sin archivos y NNA sin RUN 1.1 con cinco originales. CU y fallos se probaron con datos/dobles sintéticos; cero llamadas gubernamentales. No se confunden estos dobles con el E2E real.

Estado final de laboratorio: **7 Comunicaciones sintéticas, 7 folios únicos, 16 originales con tamaño y SHA correctos, 0 ACK pendientes, 0 Casos y 0 Requerimientos**. Documento y comprobante coinciden con el origen. Reinicio institucional y validación de las nueve migraciones correctos. V1–V8 y fixtures históricos intactos.

## Estado operativo conservado

- Público multimedia existente: HTTPS hostname oficial local, puerto 19443. Configuración, claves y servicios anteriores conservados; CU/PISEE deshabilitados en esa instancia.
- Institucional nuevo: HTTPS loopback 20443; cookie `SIGERINTEG2026`, recuperación automática desactivada, control operacional habilitado solo para pruebas manuales acotadas.
- Base integrada: `robust_inst_formulario_lab`. Base exclusiva de pruebas: `robust_inst_test`.
- Secretos, JAR activo, PID y custodia: `/home/nexbyteproveedor/.local/share/siger/institucional-completo-lab`. Nada de esto se incorpora a Git.
- Arranque/detención verificada: `scripts/Institucional-Completo-Linux.py`. Nunca compilar sobre el JAR activo; se ejecuta una copia privada fuera de target.
- Evidencias completas ignoradas por Git: `.local/evidencias/integracion-completa/`.

## Pendientes de la segunda unidad

1. Documentos de gestión funcionaria y su ciclo de carga; origen/autor preparados, sin reconstruir actuaciones ni bitácoras.
2. Ampliación del visor con representaciones aisladas de imágenes/PDF. Hoy: audio/video 1.1 nativos; PDF, imágenes, Office y medios 1.0 mediante descarga autorizada.
3. Aplicar las definiciones confirmadas de TI/accesos, cargos equivalentes, subrogancias, ingreso manual y equipos sobre los puntos de autorización existentes. No se inventaron facultades, aprobadores ni plazos; el catálogo de laboratorio no es política final.
4. Campaña posterior conjunta de resiliencia, capacidad, backup/recuperación, retención y hardening productivo. Sin estrés en esta unidad.

**Próximo paso exacto:** revisar el informe y la reversión antes de iniciar la segunda unidad autorizada. Reutilizar esquema/custodia y estos commits; no retomar checkpoints históricos RUN/SRCeI, no recrear infraestructura y no convertir documentos completos en V3 histórico.

[Informe técnico](INTEGRACION_FORMULARIO_COMPLETO_2026-10-24.md) · [Resultados agregados](RESULTADOS_FORMULARIO_COMPLETO_2026-10-24.json) · [Reversión no destructiva](REVERSION_FORMULARIO_COMPLETO.md) · [Contrato del receptor](../contratos/consumidor-formulario-completo-1.1.md).
Nota operacional: se revocó y rotó el token técnico local que mostró una traza del comprobador auxiliar. El lanzador espera HTTPS y los verificadores omiten cabeceras sensibles. Firma y origen público se conservaron.
