# Checkpoint — identidad, roles y operación territorial
**Tercera unidad cerrada funcionalmente en laboratorio. Producción no habilitada.**

- Repositorio: /home/nexbyteproveedor/proyectos/sistema-de-requerimientos.
- Rama local: identidad-territorial-laboratorio-2026.
- Base documental conservada: 2e54edd y 3e5af85.
- Implementación: e0b002c. El siguiente commit agrega este cierre; consultar git log -2 --oneline.
- Sin push; formulario-web y su configuración no fueron modificados.

## Verificado
Cuentas individuales y administración TI; roles múltiples, ámbito/vigencia y autores; revocación/desactivación de sesiones existentes; cargo separado de autorización; registro manual por cualquier funcionario activo; origen fijado por servidor y región independiente; distribución principal y regional de origen propio; permisos de Caso/documento sin cruce entre unidades; coberturas históricas y sede adicional configurable.

Pruebas finales: **160 H2, 160 MySQL, 33 frontend**, lint/build correctos. Los cinco tests de workers reales fueron ejecutados. E2E real Edge + HTTPS + MySQL repetido después de reiniciar: TI → creación/rol → login → manual/distribución → Caso autorizado; negativos de TI, otra unidad, CSRF y revocaciones; PDF histórico íntegro; escritorio/móvil. No se usaron proveedores gubernamentales.

V11 aplicada/validada. V1–V10, documentos públicos, COM/firmas/ACK y originales previos sin cambios. Se verificaron siete ingresos públicos previos, dieciséis originales ciudadanos, dos de gestión, tres vínculos y seis páginas. Cero ACK pendientes y temporales. Dos manuales y un Caso/Requerimiento explícito de esta QA dejan nueve COM, tres Casos y dos Requerimientos; los reintentos no crean otros.

## Operación conservada
HTTPS loopback 20443; esquema integrado robust_inst_formulario_lab y aislado de pruebas robust_inst_test; almacén/ClamAV/inspector existentes. JAR activo en /home/nexbyteproveedor/.local/share/siger/institucional-completo-lab/runtime.jar, fuera de target, coincidente con el build final.

institucional.identidad.habilitada=true; institucional.identidad.legado-pruebas=false. Las cuentas sintéticas antiguas no permiten login compartido. Ocho cuentas individuales sintéticas habilitadas; credenciales exclusivamente privadas, distintas y con modo 0600. Bootstrap marcado .aplicado; no se repite al reiniciar.

Evidencias reanudables en .local/evidencias/identidad/. Conservar estado.json y baseline documental. Probar-Identidad-Institucional.mjs no escribe credenciales: prepararlas en Linux con Preparar-Credenciales-QA-Identidad-Linux.py. Verificar-Custodia-Identidad-Linux.py compara custodia y archivos sin modificar registros.

## Incidencias y límites
Se corrigieron accesibilidad de selectores, reanudación de QA y permisos del archivo sintético creado desde Windows. Una batería preliminar incluyó por error un test concurrente H2; se detuvo y se corrigió el filtro. Las dos baterías finales excluyen los tres tests de carga; el lanzador comprueba los nombres exactos. Ver informe para detalle.

No se definieron catálogos definitivos, SSO productivo, equipos, subrogancias, tareas ni notificaciones. Puntos de extensión: principal estable y control de versión de sesión, asignaciones con vigencia, política por recurso, entidades territoriales y eventos con autor. Próximo paso: la siguiente unidad institucional autorizada sobre esos modelos y levantamiento, manteniendo las garantías verificadas. La campaña de hardening/capacidad sigue separada.

[Informe y reproducción](CIERRE_IDENTIDAD_2026-10-25.md) · [Resultados](RESULTADOS_IDENTIDAD_2026-10-25.json) · [Reversión](REVERSION_IDENTIDAD.md).
