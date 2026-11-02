# Preparar y compilar SIGER en Windows Server

Este manual prepara desarrollo/artefactos y define la instalación futura. **No instala producción, no copia la QA a una VM nueva y no abre los gates del núcleo.** Las acciones sobre la VM destino esperan datos y autorización de TI.

## Herramientas y clon limpio

Base comprobada: Windows Server 2022, PowerShell 7.4+, Java 25 explícito, Maven 3.9+ y Node 24/npm con `package-lock.json`. En desarrollo Java 21 es global y Java 25 se selecciona solo para SIGER. No reinstalar herramientas existentes ni cambiar PATH/JAVA_HOME globales por conveniencia.

Elegir un directorio nuevo; no ejecutar estos comandos sobre el checkout operativo ni reemplazarlo:

```powershell
git clone https://github.com/chashmdai/sistema-de-requerimientos.git DIRECTORIO_NUEVO
```

```powershell
git -C DIRECTORIO_NUEVO status --short
```

Si el repositorio requiere autenticación, usar el mecanismo Git autorizado del operador; nunca tokens en la URL. Registrar SHA de `main` antes de construir. No se necesita `formulario-web`, MySQL, claves o contraseñas para el build H2.

Desde la raíz del clone, seleccionando rutas verificadas localmente:

```powershell
pwsh -NoProfile -File .\siger-institucional\scripts\Build-Siger.ps1 -JavaHome 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot' -Maven C:\maven\bin\mvn.cmd
```

Esas rutas son las del equipo de desarrollo, no un instalador del JDK. Cambiarlas solo por las herramientas existentes del destino. El script verifica Java 25/Node 24, ejecuta npm ci/lint/tests/build y Maven clean verify/H2. Crea temporales privados nuevos fuera de Git, limita heap y restaura su entorno de proceso. Rechaza variables de fixtures MySQL/workers reales. No modifica servicios ni datos de la QA.

Las 3 pruebas históricas de estrés tienen tag `stress` excluido por defecto. Los workers reales son otra calificación: un skip por no suministrar configuración privada no equivale a aprobarlos. Leer conteos Surefire/estado H2, no solo el código de salida.

## Artefacto e instalación en desarrollo

Salida de build: `siger-institucional/backend/target/siger-institucional-0.1.0-SNAPSHOT.jar`. React está dentro de su directorio `static`; no se necesita Node como runtime institucional.

Calcular SHA, copiar a un nombre inmutable fuera de `target` y registrar Git SHA/herramientas/resultados. En el equipo actual existe este preparador idempotente:

```powershell
& C:\req\repo\scripts\Windows-Preparar-Artefacto.ps1
```

No sobreescribe dependencias distintas o un artefacto con hash incompatible. **No arranca ni sustituye el JAR activo.** El proceso vigente se reconoce por el puntero privado y listener, no por el último JAR compilado. `target` y `front/dist` nunca son directorios de ejecución permanente.

## Servicios necesarios para un runtime completo

1. MySQL aprobado: esquema propio, TLS/hostname verificado, app DML y migrador separado. V1–V13 inmutables. Validación/migración en ventana propia, nunca con la cuenta app.
2. NTFS privado con espacio/capacidad de respaldo, ACL solo identidad operativa/SYSTEM/Administradores, sin enlaces/junctions. No usar `C:\agente`.
3. ClamAV/definiciones, qpdf/Poppler/FFmpeg e inspector Office jlink privados y verificados por manifiesto. No copiar binarios sin verificar ni añadir parsers al token del backend.
4. TLS de servidor y CA/trust del origen público, claves Ed25519 y credenciales privadas. [Configuración](CONFIGURACION_PRIVADA_SIGER.md).
5. Identidades/roles persistentes y provisión TI inicial privada; sin cuentas/contraseñas predeterminadas.

Los preparadores actuales `Windows-Preparar-Herramientas-Documentales.ps1` y `Windows-Preparar-Inspector-Oficina.ps1` están acotados a `C:\req` y manifiestos del laboratorio. [Operación documental](OPERACION_DOCUMENTAL_WINDOWS.md) describe su calificación, límites y ocho controles. No ejecutarlos por rutina sobre una instalación preservada ni ampliar exclusiones de endpoint protection. Antivirus real exige 4 GiB físicos libres.

## Arranque de la QA actual

`Windows-Iniciar-QA.ps1` consume DPAPI por stdin, fuerza Java 25/TLS/loopback/esquema QA/DML, Graph simulado y Flyway apagado. PULL está cerrado por defecto; los switches manual y automático son explícitos y excluyentes. `-MantenerDisponible` mantiene esta sesión de laboratorio hasta cierre humano, sin registrar servicio/tarea.

**No ejecutar otro arranque si la QA está activa.** El comando y los parámetros exactos de reanudación están en [operación del automático](CONSUMIDOR_AUTOMATICO_QA_2026-11-01.md). El arranque continuo actual fue creado por API local Windows bajo el mismo usuario y no depende de un comando temporal del agente. Los archivos privados y la cuenta DPAPI deben corresponder a ese usuario; no arrancarlo como SYSTEM suponiendo acceso.

Para consulta no destructiva:

```powershell
& C:\req\repo\scripts\Windows-Control-Consumidor-QA.ps1 -Run ((Get-Content C:\req\scratch\runtime-consumidor-automatico-qa.txt -Raw).Trim()) -Accion estado
```

No registrar un servicio o tarea permanente en H2. No activar scheduler a otro origen ni abrir firewall. El bootstrap también restringe actualmente el origen a la resolución loopback autorizada.

## Futura VM institucional: preparación, no habilitación

TI debe definir nombre/FQDN/IP, usuario de servicio, puertos, certificados, DB/almacenamiento, backups y conectividad saliente a DDN-UBUNTU. El repositorio puede clonarse/compilarse y los binarios prepararse en una raíz nueva autorizada.

**No basta cambiar variables:** `HabilitacionNucleo` rechaza prod/production y exige perfiles de laboratorio; `ArranqueWindows` fija esquema/usuario/rutas QA y origen local. Antes de atender funcionarios/datos reales hace falta un modo institucional revisado y probado, identidad/alcances/catálogos aprobados, arranque portátil con secretos bajo la identidad real y supervisión con drenaje. No quitar esos guardas para conseguir arranque. Esta limitación es un gate explícito, no una instrucción escondida en el chat.

Secuencia posterior aprobada: inventario→provisión privada→migración separada→calificación NTFS/workers→arranque cerrado→TLS/login/autorizaciones→backup/restauración→E2E sintético con Formulario aprobado→autorización operativa. Los gates y pruebas están en [despliegue](GATES_DESPLIEGUE_SIGER.md).
