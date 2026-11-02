# Comenzar aquí · Traspaso SIGER GATE H2

Este documento permite continuar sin historial de chat. Describe código institucional de laboratorio, con recepción real acreditada entre Formulario WSL y SIGER Windows. **No es autorización productiva ni cierre funcional de U5.**

## 1. Orden de lectura y primera actuación

1. Leer `AGENTS.md`, este documento y [estado/comprobaciones H2](ESTADO_GATE_H2_SIGER_2026-11-01.md).
2. Revisar rama, estado, últimos commits y diferencias contra remoto antes de modificar. Conservar cambios y ramas existentes.
3. Leer [arquitectura y módulos](ARQUITECTURA_MODULOS_SIGER.md), [datos/roles/buzón](DATOS_ROLES_OPERACION_SIGER.md) y [U5 separado](U5_PENDIENTES_INSTITUCIONALES.md).
4. Para compilar, seguir [preparación Windows](INSTALACION_PREPARACION_WINDOWS_SIGER.md). Para operación, [PULL/ACK](OPERACION_PULL_ACK_SIGER.md) y [secretos/TLS/DPAPI](CONFIGURACION_PRIVADA_SIGER.md).
5. Para una nueva VM o recuperación, leer [respaldo/restauración](RESPALDO_RESTAURACION_SIGER.md) y [gates de despliegue](GATES_DESPLIEGUE_SIGER.md). No usar un checkpoint histórico como receta productiva.

Comandos de reconocimiento sin mutación, desde la raíz:

```powershell
git status --short
git branch -vv
git log -10 --oneline --decorate
```

No detener, reconstruir, limpiar o volver a importar el laboratorio para reconocerlo. Un clone nuevo compila sin secretos con H2; ejecutar el núcleo completo requiere configuración privada y servicios verificados.

## 2. Qué está implementado

| Área | Implementación vigente | Límite |
| --- | --- | --- |
| Comunicaciones | Manual por funcionario activo y recepción pública completa; COM/folio único, cuerpo inmutable, procedencia y ACK firmado | Recibir no abre Requerimiento/Caso |
| Gestión | Distribución central/regional, destinos, evaluación, Requerimientos/Casos, complejidad, actuaciones, derivación y reaperturas | No inventar reglas adicionales de jefaturas/equipos |
| Identidad | Cuentas individuales, bcrypt, múltiples roles/alcances/vigencias, administración TI, revocaciones efectivas | Autenticación local de laboratorio; SSO/producción pendientes |
| Documentos | Originales ciudadanos y gestión separados, vínculos sin copias, descarga y visor seguro, integridad NTFS | Workers nativos calificados y configuración privada requeridos |
| Avisos/tareas | Notificaciones por funcionario, eventos/outbox recuperable y tareas personales | Graph real apagado; tareas no se imponen a terceros |
| U5 | Tareas independientes/de Caso/de preparación de Solicitud; borradores, seguimientos y trazabilidad | Responder/canalizar/escalar deshabilitados |
| Consumidor | HTTPS saliente, versiones 1.0/1.1, reservas, streaming, SHA-256/Ed25519, COMMIT→ACK, conciliación, scheduler y exclusión QA | Solo laboratorio actual; adaptar/aprobar arranque para futuro origen institucional |

El código tiene perfiles históricos de demostración; no son el estado operativo vigente. El estado saludable se comprueba con sesión funcionaria, API operacional autenticada, SQL y custodia. Un 200 de `/api/estado` no acredita por sí solo el núcleo ni sus integraciones.

## 3. Topología: desarrollo frente a destino

| Equipo | Rol confirmado | Situación |
| --- | --- | --- |
| DDN-TEST-SIGER | Desarrollo Windows Server 2022, SIGER nativo en `C:\req`; Formulario Ubuntu 24.04 en WSL del mismo equipo | Laboratorio actual. WSL no es otra máquina |
| DDN-UBUNTU | Destino del Formulario Público Linux/Docker | H1 prepara instalación; preservar el laboratorio existente allí. H2 no instala ni modifica ese equipo |
| Futura VM Windows Server institucional | Destino de SIGER | Nombre, IP, DNS, almacenamiento, identidad de ejecución y reglas de red pendientes de TI |

No convertir `localhost`, puertos de QA, cuentas `DEMO_*` ni rutas de esta máquina en decisiones institucionales. Formulario y SIGER tienen bases/sesiones/certificados/despliegues independientes. SIGER inicia HTTPS hacia el Formulario; el navegador ciudadano no llama al núcleo institucional. No publicar SIGER a Internet para recibir Comunicaciones.

Referencia pública del Formulario H1: [`bfb950690e85381b69e6dcfac167f0a237c80071`](https://github.com/chashmdai/formulario-web/commit/bfb950690e85381b69e6dcfac167f0a237c80071), [traspaso H1](https://github.com/chashmdai/formulario-web/blob/bfb950690e85381b69e6dcfac167f0a237c80071/docs/TRASPASO_DDN_UBUNTU_GATE_H1_2026-11-01.md). Publicación y JAR activo son hechos distintos: verificar el proceso/artefacto antes de una campaña.

## 4. Desarrollo actual: datos que no se deben perder

- Repositorio `C:\req\repo`. Ramas y SHA conservados: [estado H2](ESTADO_GATE_H2_SIGER_2026-11-01.md).
- Runtime actual: puntero privado `C:\req\scratch\runtime-consumidor-automatico-qa.txt`; leer su `run.json` sin mostrar secretos. JAR inmutable por SHA en `C:\req\bin`, fuera de `target`.
- SIGER QA `https://localhost:8081/`, cuenta individual existente `nexbyte`, siete roles con alcances sintéticos explícitos. No restablecer contraseña ni crear duplicado. Acceso privado: [configuración](CONFIGURACION_PRIVADA_SIGER.md).
- Buzón `https://localhost:8081/#comunicaciones`; buscar el folio, abrir **Original recibido → Documentos del ingreso**. COM-002497 y los E2E posteriores se conservan; no son fixtures para borrar/recrear.
- MySQL `siger_institucional_windows_qa` contiene la QA integrada y originales E2E: **NO DESCARTABLE**, aunque el nombre diga QA. El esquema histórico `siger_institucional_windows_lab` conserva 504 filas/59 tablas y24 objetos. No ejecutar limpieza contra ninguno.
- La custodia QA vigente, trust y perfil público están referenciados en `run.json` y checkpoints privados. No regenerarlos ni usar rutas de una campaña anterior por parecido de nombre.
- El consumidor puede permanecer activo durante commits/builds: no usa el árbol Git, `target` o `front/dist` como runtime. No ejecutar fixtures MySQL mientras se desarrolla sobre esta base preservada.

Los PIDs, contadores, directorios de campaña y SHA activos cambian; la autoridad es la lectura actual del puntero, proceso, listener y controles. El [checkpoint automático](CHECKPOINT_CONSUMIDOR_AUTOMATICO_QA_2026-11-01.md) conserva la cronología. No asumir que cambiar `main` instala un JAR.

## 5. Configuración que una sesión nueva NO encontrará en Git

La sesión necesita acceso autorizado a: credenciales DPAPI app/migrador separadas, CA/truststore MySQL, TLS de servidor, claves/trust Ed25519, perfil de origen y tokens técnicos, configuración/manifiestos de workers y almacenamiento privado. Nada se obtiene pegando secretos en el chat. Las rutas/formato y procedimiento de provisión están en [configuración privada](CONFIGURACION_PRIVADA_SIGER.md).

Un clone limpio no incluye originales, bases, certificados privados, accesos, binarios documentales ni campañas. Eso es intencionado; debe compilar y ejecutar pruebas aisladas sintéticas. No activar simuladores para aparentar un runtime completo cuando falte un worker.

## 6. Pendientes que no puede resolver un Codex por suposición

- Facultades y estados finales de responder/canalizar/escalar Solicitudes: [U5](U5_PENDIENTES_INSTITUCIONALES.md).
- Habilitación institucional del núcleo y un arranque aprobado para futura VM/origen remoto: hoy `HabilitacionNucleo` y `ArranqueWindows` cierran expresamente producción y acotan QA a loopback/esquema/usuario/origen.
- Identidad operativa/SSO, matriz definitiva, equipos/subrogancias y supervisión de servicio: no instalados ni inventados.
- Infraestructura definitiva, DNS/TLS, ACL/identidad, secretos nuevos/rotación y backup/restauración de la futura VM.
- Calificación de workers/políticas de endpoint protection en la VM destino; no extrapolar la exclusión sintética de desarrollo a archivos ciudadanos.

No se elimina un gate porque falte una variable. La preparación puede avanzar hasta artefactos, documentación y pruebas aisladas; la habilitación se decide en los [gates](GATES_DESPLIEGUE_SIGER.md).

## 7. Documentación activa frente a historia

Este índice y sus manuales vinculados son la entrada actual. Los documentos fechados U1–U4.5/U5 y los checkpoints detallados son evidencia de esas fases: pueden decir «no publicar», «automático apagado», «faltan cuentas» o usar un puerto anterior. Esas frases describen el corte original y no sustituyen este gate ni autorizaciones nuevas.

Contratos vigentes: [`contratos/README.md`](../contratos/README.md), [`consumidor completo`](../contratos/consumidor-formulario-completo-1.1.md), [`documental`](../contratos/documental-institucional-v1.md), [`identidad`](../contratos/identidad-institucional-laboratorio-v1.md) y [`U5`](../contratos/solicitudes-tareas-u5-laboratorio-v1.md). Leer el código real cuando una instrucción antigua discrepe y documentar la diferencia sin reescribir documentos ciudadanos.
