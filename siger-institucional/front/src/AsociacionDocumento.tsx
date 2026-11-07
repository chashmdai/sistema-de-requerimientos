import {useEffect,useRef,useState} from 'react';
import * as api from './api';import {date} from './ui';

export function AsociacionDocumento({base,nombre,raiz,opciones,version,changed,denegado}:{base:string;nombre:string;raiz:'CASO'|'REQ';opciones:{id:string;nombre:string}[];version:string;changed:()=>void;denegado:()=>void}){
 const [open,O]=useState(false),[data,D]=useState<api.AsociacionDocumental|null>(null),[keys,K]=useState<string[]>([]),[reason,R]=useState(''),[error,E]=useState(''),[busy,B]=useState(false),[pending,P]=useState(false),[refresh,F]=useState(0);
 const generation=useRef(0),deny=useRef(denegado),done=useRef(changed),abortSend=useRef<AbortController|null>(null);deny.current=denegado;done.current=changed;
 const attempt=useRef<{body:object;key:string;actor:string}|null>(null);
 useEffect(()=>{if(!open)return;const abort=new AbortController();generation.current++;D(null);K([]);R('');E('');B(false);P(false);attempt.current=null;
  void api.asociacionDocumental(base,abort.signal).then(v=>{if(!abort.signal.aborted){D(v);K(v.asociaciones);}}).catch(e=>{if(!abort.signal.aborted){E(e.message);if(e instanceof api.ApiError&&(e.status===403||e.status===404))deny.current();}});
  return()=>{abort.abort();abortSend.current?.abort();generation.current++;};
 },[base,version,open,refresh]);
 function check(id:string,selected:boolean){K(v=>selected?[...v,id]:v.filter(k=>k!==id));}
 async function send(){if(!data||busy||!reason.trim())return;attempt.current??={body:{revision:data.revision,asociaciones:[...keys].sort(),fundamento:reason},key:crypto.randomUUID(),actor:data.actor};const a=attempt.current,current=generation.current,abort=new AbortController();abortSend.current=abort;B(true);E('');
  try{const v=await api.rectificarAsociacionDocumental(base,a.body,a.key,a.actor,abort.signal);if(current===generation.current){D(v);K(v.asociaciones);R('');P(false);attempt.current=null;done.current();}}
  catch(e){if(current===generation.current){E((e as Error).message);if(e instanceof api.ApiError&&e.status<500){attempt.current=null;P(false);if(e.status===403||e.status===404){D(null);deny.current();}if(e.status===409){D(null);}}else P(true);}}
  finally{if(current===generation.current)B(false);}
 }
 const options=[{id:raiz,nombre:raiz==='CASO'?'Documento del Caso':'Documento del Requerimiento'},...opciones];
 const label=(key:string)=>options.find(o=>o.id===key)?.nombre??`Referencia conservada ${key}`;
 const changedKeys=!!data&&JSON.stringify([...keys].sort())!==JSON.stringify([...data.asociaciones].sort());
 return <details onToggle={e=>O(e.currentTarget.open)}><summary>Rectificar asociaciones de {nombre}</summary>{open&&<>
  <p>Se conserva el original y cada asociación anterior. Retirar todas las asociaciones suspende la consulta del documento; restaurarlo requiere fundamento y no reactiva permisos compartidos anteriores.</p>
  {error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Consultando asociaciones autorizadas…</p>}
  {data&&<><p>{data.estado==='RETIRADA'?'Asociación retirada. El original permanece conservado.':'Asociaciones vigentes del documento.'}</p><fieldset disabled={busy||pending}><legend>Asociaciones de {nombre}</legend>
   {options.map(o=><label key={o.id}><input type="checkbox" checked={keys.includes(o.id)} onChange={e=>check(o.id,e.target.checked)}/>{o.nombre}</label>)}
   {keys.filter(k=>!options.some(o=>o.id===k)).map(k=><label key={k}><input type="checkbox" checked onChange={()=>check(k,false)}/>{label(k)}</label>)}
   <label>Fundamento de la rectificación de asociaciones<textarea maxLength={2000} value={reason} onChange={e=>R(e.target.value)}/></label>
  </fieldset>{!pending&&<><button disabled={busy||!keys.length} onClick={()=>K([])}>Retirar todas las asociaciones del documento</button><button disabled={busy||!reason.trim()||!changedKeys} onClick={()=>void send()}>Confirmar rectificación de asociaciones</button></>}
   {pending&&<><p role="status">Confirmación pendiente. Se conserva la misma operación para el reintento.</p><button disabled={busy} onClick={()=>void send()}>Reintentar rectificación de asociaciones</button></>}
   {!!data.historial.length&&<details><summary>Historia de asociaciones</summary><p>Últimos 100 cambios de este propietario.</p><ol>{data.historial.map(h=><li key={h.secuencia}>{h.autor} · {date(h.fecha)} · {h.fundamento}<p>Anteriores: {h.anteriores.length?h.anteriores.map(label).join('; '):'Sin asociación vigente'}. Vigentes: {h.vigentes.length?h.vigentes.map(label).join('; '):'Retirada'}.</p></li>)}</ol></details>}
  </>}
  <button disabled={busy||pending} onClick={()=>F(v=>v+1)}>Actualizar asociaciones del documento</button>
 </>}</details>;
}
