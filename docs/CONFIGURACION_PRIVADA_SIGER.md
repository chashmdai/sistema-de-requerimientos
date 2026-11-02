# Configuración privada, TLS, DPAPI e identidad de ejecución

Git contiene código, contratos, manifiestos de descargas públicas y ejemplos sin secretos. No contiene cuentas/contraseñas, certificados privados, claves firmantes, originales, bases o logs de autenticación. Nunca imprimir el contenido completo de una configuración institucional; auditar presencia, ACL y hashes sin revelar valores.

## Inventario de desarrollo: formatos y ubicación

| Entrada fuera de Git | Uso |
| --- | --- |
| `C:\req\config\mysql-windows-qa-app.credential.xml` | PSCredential DPAPI Current User de aplicación QA, solo DML |
| `mysql-windows-qa-migrator.credential.xml` | Identidad de migración QA; no disponible al runtime |
| `mysql-windows-app.credential.xml` y `mysql-windows-migrator.credential.xml` | Copia histórica preservada, no destino descartable |
| `siger-mysql-truststore.p12` y CA pública aprobada | JDBC `sslMode=VERIFY_IDENTITY`, sin fallback inseguro |
| `origen-dpapi\institucional-key.pem.dpapi` | Privada Ed25519 institucional; descifrado solo en proceso autorizado |
| `origen-dpapi\institucional-trust.json.dpapi` | Claves públicas para comprobar ACK históricos |
| `origen-dpapi\publico-trust.json.dpapi` o trust QA combinado aprobado | Claves públicas de productores admitidos; conservar las anteriores |
| `windows-qa-localhost-tls.json.dpapi` | PFX/password TLS de esta QA, no certificado de la futura VM |
| Perfil `formulario-wsl-e2e-…\perfil.json` | Descriptor público del origen, CA/trust e identidad de la instancia QA |
| `conexion.json.dpapi` junto al perfil | Bearer de integración y token operacional, separados de la sesión funcionaria |
| `origen-dpapi\nexbyte-cuenta.json.dpapi` | Acceso de la única cuenta QA preexistente, no bootstrap público |
| `documental-win-…\windows.properties` y manifiestos/definiciones | Cadena documental real, hashes, trabajos y evidencia privados |

Los nombres describen el laboratorio actual. No son credenciales por defecto ni un mandato de replicarlos en producción. El truststore solo con certificados públicos no equivale a custodia de una clave privada; no reutilizar su contraseña pública para otros secretos.

## Provisión sin revelar valores

1. TI crea identidades/esquema y permisos mínimos del destino aprobado. App y migrador no son intercambiables; no usar root.
2. Bajo el usuario Windows que ejecutará SIGER, capturar credenciales mediante diálogo privado `Get-Credential` y `Export-Clixml` en un archivo absoluto nuevo con ACL restringidas. No argumentos con password, transcript o consola que muestre el objeto completo.
3. Para secretos binarios/JSON, utilizar DPAPI Current User mediante el procedimiento privado aprobado; el launcher usa `ProtectedData.Unprotect` y entrega configuración por stdin anónimo al Java 25.
4. Verificar identidad, dueño/ACL, inexistencia de reparse points y conexión JDBC real por TLS. Una extensión `.xml` por sí sola no acredita DPAPI.
5. Las copias temporales exactas que el proceso necesita quedan en su raíz privada y se eliminan al cerrar normalmente. Si un proceso murió abruptamente, inventariarlas/limpiarlas solo después de comprobar su dueño y que no está activo; no borrar directorios de campañas ni originales.

Java no descifra directamente un `Export-Clixml`. No pasar credenciales mediante `-Dpassword`, URL JDBC con password, flags de CLI, `.env` versionado o variables globales persistentes. Un entorno efímero de un test autorizado no se reutiliza en runtime.

DPAPI Current User está ligado a máquina/perfil/identidad. Copiar sus archivos a otra VM no garantiza recuperación. La migración de secretos necesita exportación cifrada por canal privado/reprovisión bajo la nueva identidad, con recuperación y rotación aprobadas; no transportar una cuenta temporal de desarrollo como identidad de servicio.

## TLS y firmas

- Navegador→SIGER: certificado válido para el FQDN real y cookies Secure/HttpOnly. No omitir TLS en pruebas autenticadas.
- SIGER→MySQL: `VERIFY_IDENTITY`, CA confiable y hostname/SAN correctos, sin desactivar validaciones o exigir privilegios globales.
- SIGER→Formulario: HTTPS, CA configurada expresamente, hostname verificado y sin redirects. El laboratorio usa resolución local del dominio; el destino remoto exige su gate de origen/red.
- Claves Ed25519: privada institucional solo en SIGER; pública institucional confiada por el Formulario. Claves públicas del productor confiadas por SIGER. Rotación usa identificadores nuevos y conserva confianza histórica. No volver a firmar/transformar documentos antiguos.
- Certificados de desarrollo Current User y localhost no se trasladan a la confianza global de la futura VM sin aprobación TI.

## Cuentas y bootstrap TI

La cuenta QA `nexbyte` ya existe y está vinculada a una identidad estable/roles/alcances sintéticos. No resetearla ni duplicarla al preparar el entorno. Visor local privado, contraseña enmascarada inicialmente:

```powershell
& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -STA -File C:\req\repo\scripts\Windows-Ver-Acceso-QA.ps1
```

No automatizar capturas del diálogo, copiar su valor al chat ni guardarlo en evidencias.

`IdentidadConfiguration` dispone de bootstrap inicial por archivo privado y esquema `IdentidadInstitucional.Crear`: usuario/nombre/correo/cargo/unidad/sede/password. No hay contraseña predeterminada. Solo opera si la condición de bootstrap lo permite; se registra y renombra el archivo a `.aplicado`. Ese archivo puede contener un secreto y debe seguir protegido, fuera de Git, con ciclo de custodia/retirada aprobado. No copiarlo como receta pública ni ejecutarlo sobre cuentas preexistentes. La administración posterior usa TI autenticado y protege al último TI.

## Recuperación y seguridad pendiente

Backups deben contemplar claves/trust, identidad DPAPI y certificados por un canal de recuperación aprobado, separado de los datos. No asumir que guardar el XML cifrado permite restaurar en otro host. No enviar secretos al agente de respaldo por conveniencia.

Hubo un incidente de lectura de una credencial temporal WSL contenida en documentación institucional, comunicado y registrado en evidencia privada sin repetir su valor. Su rotación administrativa necesita confirmación privada; no reutilizarla ni copiar documentación reservada sin filtrar a consola/Git. No se trata del Bearer/Graph/CU del runtime SIGER. H2 no realiza pruebas con esa credencial ni autoriza nuevas llamadas a proveedores.
