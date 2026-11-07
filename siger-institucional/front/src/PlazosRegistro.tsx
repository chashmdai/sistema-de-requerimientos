import {useEffect,useRef,useState} from 'react';
import * as api from './api';import {date} from './ui';

export function PlazosRegistroVista({contexto,recurso,version,actorId}:{actorId?:string;contexto:'comunicaciones'|'requerimientos'|'casos';recurso:string;version:number}){
 const [open,O]=useState(false),[data,D]=useState<api.PaginaPlazos|null>(null),[page,N]=useState(0),[refresh,F]=useState(0),[error,E]=useState(''),[busy,B]=useState(false),[pending,P]=useState(false);
 const generation=useRef(0),abortSend=useRef<AbortController|null>(null),attempt=useRef<{body:api.SolicitudMedicion;actor:string;key:string}|null>(null);
 useEffect(()=>{N(0);},[contexto,recurso]);
 useEffect(()=>{if(!open)return;const abort=new AbortController();generation.current++;D(null);E('');B(false);P(false);attempt.current=null;
  void api.plazosRegistro(contexto,recurso,page,abort.signal).then(v=>{if(!abort.signal.aborted){if(actorId&&v.actor!==actorId){E("La cuenta de consulta cambió. Actualiza las fechas autorizadas.");return;}D(v);}}).catch(e=>{if(!abort.signal.aborted)E(e.message);});return()=>{abort.abort();abortSend.current?.abort();generation.current++;};
 },[contexto,recurso,version,actorId,page,refresh,open]);
 async function medir(fuente?:api.FuentePlazo){if(!data||busy||actorId&&data.actor!==actorId)return;if(!attempt.current){const r=fuente?.regla;if(!r||fuente?.estado!=='MEDICION_DISPONIBLE')return;attempt.current={body:{fuente:fuente.id,huellaFuente:fuente.huella,politica:r.id,versionPolitica:r.version,huellaPolitica:r.huella,huellaCalendario:r.huellaCalendario},actor:data.actor,key:crypto.randomUUID()};}
  const a=attempt.current,current=generation.current,abort=new AbortController();abortSend.current=abort;B(true);E('');try{await api.medirRegistro(contexto,recurso,a.body,a.key,a.actor,abort.signal);if(current===generation.current){attempt.current=null;P(false);F(v=>v+1);}}
  catch(e){if(current===generation.current){E((e as Error).message);if(e instanceof api.ApiError&&e.status<500){attempt.current=null;P(false);if(e.status===403||e.status===404||e.status===409)D(null);}else P(true);}}finally{if(current===generation.current)B(false);}
 }
 return <details className="card" onToggle={e=>O(e.currentTarget.open)}><summary>Fechas y mediciones de registro</summary>{open&&<>
  <p>Las 48 horas del Memorándum DDN N.º 18/2024 se refieren al registro de atenciones y gestiones. No son un plazo de resolución del Requerimiento o del Caso.</p>
  <p>Recepción o gestión efectiva y registro persistido se conservan por separado. Una reasignación, espera o ausencia no reinicia ni pausa estas fechas. Los históricos sin fuente temporal acreditada requieren conciliación explícita.</p>
  {error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Consultando fechas autorizadas…</p>}
  {data&&(!actorId||data.actor===actorId)&&<><p>{data.total} constancias temporales autorizadas.</p>{!data.items.length&&<p>No hay fuentes temporales acreditadas en este contexto. No se deducen fechas ni cumplimiento de los registros anteriores.</p>}
   {data.items.map(f=><article key={f.id} className="card"><h3>{f.objetivo==='REGISTRO_ATENCION'?'Registro de atención recibida':'Registro de gestión efectuada'}</h3><p>{f.origen==='RECTIFICACION_MANUAL'?'Recepción rectificada con constancia conservada':f.origen==='FORMULARIO'?'Ingreso del Formulario':f.origen==='MANUAL'?'Atención manual':'Gestión institucional'} · {f.vigente?'Constancia actual':'Constancia rectificada; conservada como antecedente'}</p>
    <dl><dt>Recepción o realización efectiva</dt><dd>{date(f.efectiva)}</dd><dt>Registro persistido</dt><dd>{date(f.registrada)}</dd><dt>Constancia temporal conservada</dt><dd>{date(f.capturada)}</dd><dt>Registrado por</dt><dd>{f.registradoPor??'Registro automático del Formulario'}</dd></dl>
    {f.duracionCorridaMs!==null&&f.duracionCorridaMs>=0&&<p>Tiempo corrido entre ambos hechos: {(f.duracionCorridaMs/3600000).toLocaleString('es-CL',{maximumFractionDigits:3})} horas. Este dato por sí solo no declara incumplimiento.</p>}
    {f.estado==='SIN_POLITICA_APLICABLE'&&<p>No hay una política de cómputo acreditada aplicable a esta constancia. No se declara cumplimiento ni incumplimiento.</p>}
    {['FECHAS_FUERA_COMPUTO','FECHAS_NO_COMPARABLES'].includes(f.estado)&&<p>Las fechas conservadas requieren revisión; no se emite una medición de cumplimiento.</p>}
    {f.regla&&<p>Política {f.regla.id} · versión {f.regla.version} · horas {f.regla.modo==='HABILES'?'hábiles del calendario aprobado':'corridas'}. Referencia: {f.regla.referencia}{f.regla.calendario&&<> · Calendario {f.regla.calendario} / {f.regla.versionCalendario}</>}</p>}
    {f.estado==='MEDICION_DISPONIBLE'&&!pending&&<button disabled={busy} onClick={()=>void medir(f)}>Medir este registro con la política acreditada</button>}
    {f.mediciones.map(m=><p key={m.id}>{m.resultado==='FUERA_PLAZO'?'Fuera del plazo de registro':'Dentro del plazo de registro'} · {(m.transcurridoMs/3600000).toLocaleString('es-CL',{maximumFractionDigits:3})} horas computadas · referencia de 48 horas: {date(m.vencimiento)} · política {m.politica}/{m.versionPolitica} · medición por {m.autor} el {date(m.fecha)}. {m.actual?'Aplicable a esta constancia bajo la configuración actual.':'Medición histórica; no representa la configuración o constancia actual.'}</p>)}
   </article>)}
   <button disabled={busy||pending||page===0} onClick={()=>N(p=>p-1)}>Constancias anteriores</button><button disabled={busy||pending||(page+1)*data.tamanio>=data.total} onClick={()=>N(p=>p+1)}>Constancias siguientes</button>
  </>}
  {pending&&<><p role="status">Medición pendiente de confirmación. El reintento conserva política, fuente, cuenta y clave.</p><button disabled={busy} onClick={()=>void medir()}>Reintentar la misma medición</button></>}
  <button disabled={busy||pending} onClick={()=>F(v=>v+1)}>Actualizar fechas y mediciones autorizadas</button>
 </>}</details>;
}

export function CatalogoPlazosTI({actorId}:{actorId?:string}){
 const [data,D]=useState<api.CatalogoPlazos|null>(null),[error,E]=useState(''),[refresh,F]=useState(0);
 useEffect(()=>{const abort=new AbortController();D(null);E('');void api.catalogoPlazos(abort.signal).then(v=>{if(!abort.signal.aborted)D(v);}).catch(e=>{if(!abort.signal.aborted)E(e.message);});return()=>abort.abort();},[actorId,refresh]);
 const dias=['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
 return <section><h2>Políticas y calendarios de registro</h2><p>La configuración aprobada se incorpora mediante el registro de autoridad firmado. Administrar esta configuración no concede acceso a las atenciones o gestiones.</p><button onClick={()=>F(v=>v+1)}>Actualizar catálogo de plazos</button>{error&&<p role="alert">{error}</p>}{data&&<><p>Registro de autoridad: revisión {data.autoridad}. Las versiones acreditadas se conservan; cambiar su contenido requiere una versión nueva.</p>{!data.politicas.length&&<p>No hay políticas de cómputo cargadas. No se presupone jornada, feriado ni modalidad hábil o corrida.</p>}
  {data.politicas.map(p=><article key={`${p.id}:${p.version}`}><h3>{p.id} / {p.version}</h3><p>{p.objetivo==='REGISTRO_ATENCION'?'Registro de atención':'Registro de gestión'} · {p.segundos/3600} horas {p.modo==='HABILES'?'hábiles':'corridas'} · {p.estado}</p><p>Ámbito: {p.alcance}{p.unidad&&<> · {p.unidad}</>}{p.sede&&<> · {p.sede}</>}{p.area&&<> · {p.area}</>}{p.prioridad&&<> · prioridad {p.prioridad}</>}</p><p>Aplicación a hechos desde {date(p.desde)} hasta {date(p.hasta)} · respaldo {p.referencia}</p></article>)}
  {data.calendarios.map(c=><article key={`${c.id}:${c.version}`}><h3>Calendario {c.id} / {c.version}</h3><p>{c.zona} · {c.desde} a {c.hasta} (fin exclusivo) · {c.referencia}</p><ul>{c.jornada.map((j,i)=><li key={i}>{dias[j.dia]} · {j.inicio}–{j.fin}</li>)}</ul><p>Feriados configurados: {c.feriados.length?c.feriados.join(', '):'Sin fechas cargadas en esta versión'}</p></article>)}
 </>}</section>;
}
