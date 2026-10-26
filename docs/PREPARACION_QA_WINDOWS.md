> **Atendida el 26/10/2026 por administración autorizada.** Preflight JDBC y batería MySQL QA nativa correctos. Ver CONTINUACION_NTFS_WINDOWS_2026-10-26.md. Se conserva la solicitud original a continuación.

# MySQL QA Windows — solicitud administrativa acotada

El esquema histórico siger_institucional_windows_lab no es descartable y no debe usarse en la batería que borra fixtures. Las cuentas existentes no disponen de CREATE DATABASE ni GRANT OPTION fuera de ese esquema. No se solicita modificar sus privilegios ni acceder a credenciales de C:\agente.

Administración requerida: comprobar primero en la sesión MySQL administrativa autorizada si ya existen el esquema siger_institucional_windows_qa y los usuarios siger_windows_qa_app@localhost / siger_windows_qa_migrator@localhost. Ante colisión detenerse, comprobar titularidad y no alterar ni reemplazar cuentas/contraseñas existentes.

Crear únicamente si están ausentes:

- Esquema siger_institucional_windows_qa, utf8mb4 / utf8mb4_0900_ai_ci.
- siger_windows_qa_app@localhost: SELECT, INSERT, UPDATE, DELETE exclusivamente sobre ese esquema.
- siger_windows_qa_migrator@localhost: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, CREATE VIEW exclusivamente sobre ese esquema. Los tests alteran restricciones y requieren DDL; no se usará esa identidad como runtime.
- Sin permisos globales, GRANT OPTION, cuentas con host %, cambios de servicio o configuración TLS.

Gestionar las dos contraseñas aleatorias mediante el canal privado administrativo, nunca por chat/argumentos/Git. Entregar PSCredential DPAPI CurrentUser de la cuenta operativa en:

C:\req\config\mysql-windows-qa-app.credential.xml

C:\req\config\mysql-windows-qa-migrator.credential.xml

UserName exactos: siger_windows_qa_app y siger_windows_qa_migrator. ACL restringidas iguales a las credenciales nativas existentes. Estos nombres y rutas son una propuesta de QA; no autorizan por sí solos ejecución administrativa.

El runner usará VERIFY_IDENTITY con el truststore SIGER existente, y variables solo del proceso hijo. La guarda de pruebas rechazará expresamente siger_institucional_windows_lab y cualquier destino ajeno. No se borrará el esquema al terminar: conservar resultados/estado para auditoría y reutilización autorizada.