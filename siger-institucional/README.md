# SIGER Institucional

**Actualización:** [complejidad estructurada y solicitud/aceptación de derivación](../docs/COMPLEJIDAD_Y_DERIVACION_LAB.md). Esa extensión prevalece sobre las limitaciones de la primera iteración descritas abajo.

Aplicación Java 25 / Spring Boot 4.1.1 con React 19/TypeScript propio. Recepción durable MySQL/Flyway, recuperación HTTPS saliente V3 y comprobante institucional Ed25519. No administra ClaveÚnica ni sesión ciudadana. No necesita al backend público encendido; su indisponibilidad solo afecta recuperar nuevos ingresos web.

Nuevo [núcleo provisional de laboratorio](../docs/NUCLEO_INSTITUCIONAL_LAB.md): buzón real, manual, destinos, Requerimiento, Casos independientes, actuaciones y revisión/cierre. [Pruebas y entrega](../docs/ENTREGA_NUCLEO_2026-10-19.md). No es aprobación institucional ni habilitación productiva.

Desde esta carpeta, con la aplicación detenida:

```powershell
.\scripts\Build-Siger.ps1
# Bootstrap cerrado por defecto, sin público ni credenciales CU:
java -jar .\backend\target\siger-institucional-0.1.0-SNAPSHOT.jar
```

El build ejecuta npm ci, lint, pruebas/build de este frontend y Maven clean verify de este backend. Estado sin datos: http://127.0.0.1:8081/api/estado. Sesión funcionaria y gestión solo se habilitan expresamente en el laboratorio MySQL loopback; no existe administrador institucional implícito.

Desde la raíz del repositorio:

```powershell
.\scripts\Nucleo-Local.ps1 iniciar
.\scripts\Nucleo-Local.ps1 acceso
.\scripts\Nucleo-Local.ps1 demostrar
.\scripts\Nucleo-Local.ps1 detener
```

[Interfaz institucional HTTPS](https://localhost:8443/). Solo cuentas sintéticas; contraseña DPAPI local. Para pruebas sin CU: iniciar -SinClaveUnica. No instalar el perfil nucleo-lab en producción. Faltan autenticación/matriz institucionales, reglas DDN y operación productiva.

V5 agrega gestión separada y permite operacion_id nula para manual sin transporte ciudadano ficticio. Cuerpos/huellas/comprobantes web permanecen. Migraciones con cuenta separada, sin DDL al arrancar el servicio. Los perfiles de archivos anteriores son demostraciones históricas, no persistencia vigente.


