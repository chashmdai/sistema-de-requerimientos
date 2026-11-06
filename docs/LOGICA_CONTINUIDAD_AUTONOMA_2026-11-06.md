# Continuidad autónoma SIGER · 06/11/2026

## Precedencia y alcance

El responsable confirmó la validación funcional del levantamiento el 06/11/2026. Ver DECISION_VALIDACION_FUNCIONAL_2026-11-06.md. Las 138 reglas mantienen IDs/orden y esa decisión. No pedir otra validación funcional. Faltantes nominales se configuran, sin inventarlos. La validación no equivale a código terminado, prueba física o habilitación operacional.

Worktree exclusivo C:\req\worktrees\siger-logica. Mantener identidad, autorización, ámbito, vigencia y segregación de personas/cuentas. No tocar padres/main/U4.4/U4.5/SMR/QA, COM-000001, históricos, JAR/servicios, Formulario/proveedores, originales/firmas/hashes o COMMIT→ACK. Sin merge/despliegue, correo real ni E2E físico.

## Git y evidencia publicada

Rama actual codex/siger-subrogancias, base cdb176f2ea81ef66727c2077cbf271fdb05fa9e1. El commit de implementación conserva este checkpoint; consultar git log -1 para su SHA. Conservar todos los cambios locales de subrogancias: no reconstruir A/colaboraciones. Todavía sin PR/upstream propio; crear PR dependiente de #7 tras calificación.

Cadena preservada: main → #2 → #3 → #4 → #5 → #6 → #7. Main remoto verificado95cf2ae; referencia U4.5 sin nuevas migraciones concurrentes observadas; no se modifica su directorio. Revisar refs antes de publicar.

- PR #6 / codex/siger-equipos-agenda /396422d69c29ace39f65ce84a5552b1a45c066f1: CI42213485912 aprobada. Windows478/469 aprobadas/9 omitidas; Ubuntu478/454/24; frontend119/119, lint/build/package, cero fallos/errores. MySQL nativo descartable73/73 y proceso propio cerrado.
- PR #7 / codex/siger-continuidad-funcional /cdb176f2ea81ef66727c2077cbf271fdb05fa9e1: CI42220441097 aprobada. Windows490/481/9, Ubuntu490/466/24; frontend123/123, lint/build/package; cero fallos/errores. Logs íntegros comprobados, Maven27:51/24:52 y acta PR actualizada. Local488/479/9 anterior a dos tests nuevos, más focal posterior2/2. MySQL82/82 no se reatribuye a esos dos métodos.

No repetir esas regresiones por rutina ni atribuirlas a nuevos commits.

## Incremento actual desarrollado, en calificación

Registro firmado explícito1.4, sin reinterpretar1.0–1.3. Función estable/ámbitos, rutas REGIONAL/Jefatura Sedes y CENTRAL/Jefatura Gestión, titular, candidatos, autoridades/delegados, respaldo y vigencias configurables. V15.12 H2/MySQL nuevas e idénticas. V1–V15.11 intactas. Sin datos nominales, backfill u outOfOrder.

SubroganciasInstitucionales/Controller, React/navegación, resolver efectivo en IdentidadInstitucional, integración de decisiones/cierres/avisos:
- Candidato solicita su cuenta; autoridad canónica independiente aprueba; TI de tercera persona ejecuta. Sin autoactivación, autoelevación o reciprocidad mediante alias. Solicitud sellada por hash; no alterar período aprobado.
- Inicio futuro, fin/revisión finito, prórroga nueva enlazada, retiro/rechazo, revocación/retorno, caducidad por predicado aunque no haya monitor. Fechas efectivas/registradas diferenciadas. Épocas de sesiones y permisos actuales.
- Ámbitos persistentes mantienen denegación al omitir designaciones y tras reinicio. Suspensión bloquea función; autoridad acreditada puede revocar. Titular necesita concesión base exacta; subrogante solo cuenta solicitante. Facultades profesionales/lectura por materia independientes se conservan. Área no se hereda de sede/unidad; TI no obtiene expedientes.
- Se transfieren decisiones funcionales temporalmente, no cartera/equipo/tareas. Auxiliar específico conserva actos legítimos de origen para transferencias/derivaciones/término de colaboración, sin permitir actos nuevos. Mantiene bajas ordinarias y contexto actual.
- Autocierre regional titular por ruta específica: responsabilidad profesional actual, designación/ámbito, controles ordinarios y evento reforzado de ambas funciones. Subrogante no autocierra Caso propio; retorno exige revisión expresa.
- Avisos internos mínimos, deduplicados y autorizados al consultar, sin correo adicional. TI ve instrucción mínima, sin relato de ausencia ni registro firmado completo.

Contrato funciones-subrogancias-institucionales-v1.md e informe LOGICA_SUBROGANCIAS_2026-11-06.md con guion manual por roles preparado, no ejecutado.

## Pruebas y siguiente acción precisa

Evidencia privada: LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d, logs subrogancias-*. Java25 explícito/Node24/Build-Siger.ps1 aislado; no Maven concurrente sobre target.

1. MySQL en calificación: selección completa99 tuvo86 aprobados y13 bloqueados por guarda del nuevo esquema; corregida únicamente para el sufijo descartable subrog. Segundo intento ejecutó1/13 y falló12 por cuentas sintéticas no limpiadas entre métodos; se completó limpieza exclusiva del esquema validado. Intento final13 EN CURSO, sesión57962, log subrogancias-mysql-cierre.log. Auxiliar conserva datadir GUID privado y detiene solo su proceso. Comprobar resultado y ausencia del proceso propio. No declarar motor aprobado antes del resultado. No mostrar client.cnf/init.sql/credenciales.
2. H2 primera6/6. Selección47 tuvo dos fallos de continuidad por mapa copiado que perdía lectura de columnas; corregido, continuidad posterior17/17. Selección28 tuvo dos fixtures sin fundamento territorial regional: completados sin eliminar gate. Intento4 falló compilación por argumento opcional faltante, sin tests. Intento5: migración14 y HTTP1 aprobadas, dominio13 con una expectativa403 frente a404 protegido; corregida, autocierre final focal1/1 aprobado. Intentos fallidos conservados.
3. Frontend completo129/129. Selección previa13/13; lint/build aprobados antes de los dos últimos DOM/export del componente, se cubren de nuevo en integral. H2/MockMvc/DOM/MySQL no son E2E físico.
4. Tras MySQL: corregir si corresponde, ejecutar Build-Siger integral una vez, actualizar resultados/matriz/informe, revisar diff/secreto/refs y confirmar bloques coherentes. Push normal, PR en borrador base codex/siger-continuidad-funcional. CI Windows/Ubuntu del HEAD publicado y logs comprobados; no reutilizar padres ni alterar exclusiones/timeouts por rutina.
5. No detenerse al aprobar: rama dependiente limpia para SUB-CONT-01 por Caso, con autoridad configurable, revisor nominal independiente, acceso a antecedentes/reservas, vigencia/revocación, revisión actual y obligatorias. No dar jefatura global para sustituir ese circuito. Continuar C/D/E en cortes verificables.

## Pendientes técnicos y parámetros

SUB-CONT-01 y cierres excepcionales específicos; territorio acreditado y conciliación explícita/reversible; antecedentes/documentación adicional REQ, FIR/bitácoras/reservas; plazos/calendarios, búsqueda/reportes/exportaciones y demostración física. Son implementación o valores concretos no cargados, no falta de validación funcional. No declarar terminadas138 reglas ni U1–U9.

Rector SHA80a8f09424d105eabb755a79e9523ac58fb55c731647cbbb57c3a1a357d5eed4 verificado. Cuestionario27 no encontrado localmente; rector suficiente para este circuito. Investigar y avanzar sin preguntas ordinarias.

