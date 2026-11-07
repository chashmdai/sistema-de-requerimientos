# Metadatos y versiones documentales · 07/11/2026

## Corte publicado acreditado

PR21 /HEADe57bd847778d7bf34d72287f42491c3cdc751168,CI42399170765 cerrada8jobs.101clases/735métodosporOS,Win726aprobados/9omitidos,Ubuntu711/24,cero fallos/errores. Particiones completas/disjuntas,mismoHEAD/run/OS;FE218/218 una vezporOS,lint/TypeScript/Vite yJARfinal3archivosidénticosSHA256. Logs128490337214/128490337203 verificados,actaPRactualizada,borradorsinmerge/despliegue. No acredita el hijo de cuarentena.

## Capacidad desarrollada

VersionesDocumentales reutiliza autorización y custodia de documentos de Caso y REQ. Permite declarar/corregir título y tipo descriptivo, distinguir BORRADOR/FINAL declarados, conservar antecedente y sucesora como originales separados y consultar las declaraciones autorizadas. API autenticada/CSRF y componente real VersionDocumento integrado en ambas vistas. Consulta diferida al abrir el control; reintentos inciertos congelan operación/cuerpo/clave y se descartan respuestas tardías por contexto.

No se desarrolla editor ni se atribuye firma,visación o despacho a una declaración en SIGER. Referencia externa se conserva como declaración explícita, sin inventar número Docflow. Rectificar metadatos no altera bytes,nombre original,creador,fecha,hash o finalización anterior. FINAL no vuelve a BORRADOR. Ninguna migración o nombre de archivo fabrica una versión histórica.

Escritura solo del responsable/jefatura propietario efectivo. Colaborador puede leer metadatos de un documento ordinario permitido,sin facultad de modificarlos; no recibe historia reservada ni enlace a sucesora FIR oculta. Alias,TI y otra área no heredan responsabilidad. Documentos históricos sin protección/propiedad acreditada no se regularizan por el circuito de versiones. Cierre impide nueva finalización/sucesión; una corrección descriptiva fundada conserva la evidencia previa y no crea actuación poscierre.

Sucesión exige ambos documentos autorizados en el mismo contexto,protección expresamente equivalente,revisiones actuales,nueva versión BORRADOR sin relaciones y antecedente sin sucesora. Cadena lineal,FK/unique y control transaccional de concurrencia,sin ciclos/ramas ni herencia de reserva. Se preserva el antecedente final y no se genera Caso/REQ/COM/actuación adicional.

## Datos, decisiones y compatibilidad

V15.19 H2/MySQL idéntica,SHA256 **F93BE8798AA9487851EF2A7204C3E8E4E10BE89976A978BA75770A77E273D5FF**.33migraciones. Tablas management_document_metadata,management_document_metadata_history,management_document_successor,management_document_metadata_command. Todas aditivas;V1–V15.18 intactas,sinoutOfOrder ni cambios a originales.

Tipo documental es texto declarado, separado de clasificación de seguridad y permisos; no se inventa catálogo nominal acreditado. FaseFINAL es declaración responsable registrada por servidor,no firma digital. Metadatos actuales son proyección de historia append-only; número de versión se asigna solo al declarar relación. Historia se limita a100movimientos devueltos,conservando la totalidad en BD; no es purga.

Upgrade desde15.18 comprueba originalesREQ,política,comando,reintento y raíz firmada exactamente iguales y cero fases/versión inferidas. AutoridadMigracionTest espera15.19,incluye partida15.18 y prueba igualdadDDL; auxiliarMySQL crea esquemas propios docmeta/docmetaprev y1518,guardias exactas de GUID/loopback. No se migran QA/históricos/U4 ni se modifica configuración global.

## Pruebas por impacto

- Compilación Java correcta. **41/41 H2**,02:11:12dominio+1upgrade+28autoridad. Incluye corrección/autoría,finalización,historia,reintentos,cadena/anti ciclos,concurrencia,rollback,reserva,otros ámbitos y lector de equipo sin sucesoraFIR.
- **41/41 MySQL8.0.46 nativo Windows**,03:35, mismos12+1+28. Instancia65e3fc3604cf4c62b9ccc70f89605cc7,cerrada/procesos0,33migraciones y upgrades14–15.18. No se repite el corte íntegro por ajustes acotados de presentación/flags.
- **HTTP1/1**,22s:sesión real/CSRF/no-store,declaración,final y sucesión,denegaciónTI/otraárea,revocación sin nuevo login. BD/custodia reales,AV/inspector con dobles; noE2Efísico.
- Ajuste posterior: documento histórico sin propiedad/protección acreditada devuelve editablefalse. Nuevas negativas de alias e histórico **2/2 H2**,27s; **2/2 MySQL nativo** posteriores en f6efa7d52d104d49ae6f01676727642e,cerrada/procesos0. No anticipar resultados ni sumarlos a41como una misma ejecución.
- DOM inicial23:16aprobadas/7errores del harness por fireEvent.toggle inexistente. Se usa click real sobre summary;foco7/7posterior. Lint inicial aprobado;TS falló solo por ese método inexistente,corregido. **Frontend217/217**,lint/TypeScript/Vite. Después se hizo explícita la bandera finalizable del backend para no ofrecer finalización en gestión cerrada: **2/2 H2/HTTP** y **8/8 DOM**,lint/TypeScript/Vite finales. No se repite217 por ese ajuste;CI debe acreditar218 enHEADpublicado.
- CI propia del hijo pendiente de publicación. CI del padre PR20/29d653a,42381732338,8jobs/98clases716métodosporOS/FE210,pertenece al padre exclusivamente.

Evidencia privada conservada en campaña LOCALAPPDATA/siger-build-4c652bfd9b7a41d5a8e0d659e180202d/versiones-doc-*. Sin exclusiones,aserciones o timeouts funcionales relajados. No físicoE2E,nueva carga operacional,proveedor o correo real.

## Guion manual preparado, no ejecutado

1. En entorno nuevo descartable,concesiones sintéticas independientes y servicios documentales aislados,crear un REQ sinCaso y/o Caso en áreaA;tener responsable,jefatura,colaborador legítimo,cuentaalias,otraárea yTI.
2. Cargar original sintético. Abrir “Metadatos y versiones”:SIN_ACREDITAR,ninguna fase inferida del nombre. Declarar título/tipo/fundamento: BORRADOR,cuenta/fecha/historia reales.
3. Corregir título con fundamento. Consultar historia y descargar original: título previo,nombre/hash/bytes/autor de origen conservados. Colaborador no dispone de edición.
4. Declarar FINAL,referencia externa solo si existe. Ver declaración del servidor,sin afirmar firma o despacho. Corregir descripción y comprobar que no cambia la finalización ni habilita retorno aBORRADOR.
5. Cargar otro original,declarar sus metadatos BORRADOR y elegir antecedente autorizado. Enlazar sucesora:versión2,primeraFINAL conservada,ambos originales descargables por autorización propia. Repetir mismo envío no duplica.
6. Dos responsables intentan distintas sucesoras del mismo antecedente/revisión:una confirmación y un conflicto,no rama. Intentar ciclo o archivo de otro Caso/REQ:denegado.
7. Proteger sucesora comoFIR bajo jefatura. Colaborador que solo lee antecedente ordinario no ve esa referencia/historia ni bytes. Alias de responsable,TI y otraárea no pueden consultar o modificar por asociación.
8. Cerrar gestión o revocar con sesión abierta: no nuevafinalización/sucesión; consultas requieren autorización actual. Reintento previamente confirmado no reabre. No confundir corrección descriptiva con gestión nueva.

## Pendientes y siguiente bloque

DOC-03 y metadatos de DOC-01 quedan desarrollados con límites declarados de integración formal. DOC-07 aún requiere rectificación de asociación y cuarentena técnica sin lecturaTI; retención/purga requiere mecanismos de política y habilitación, sin números inventados. Formulario sectorialFIR,48h/calendarios,invalidación/duplicados,búsqueda/reportes y conciliación histórica siguen programables.

Al completar/verificar las138 o registrar límites concretos genuinos se hará revisión transversal de clases/rutas/dependencias heredadas y candidata integrada. No se declara esa candidata en este bloque. PR hijo dependiente dePR20,sin modificar padres/main,merge,despliegue o recursos preservados.
