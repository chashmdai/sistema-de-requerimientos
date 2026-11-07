import {useEffect,useRef,useState} from 'react';
import * as api from './api';

export function TomaClasificacion({id,version,cambio}:{id:string;version:number;cambio:(v:api.TomaClasificacion|null)=>void}){
 const[v,V]=useState<api.TomaClasificacion|null>(null),[reason,R]=useState(''),[error,E]=useState(''),[busy,B]=useState(false),[refresh,F]=useState(0);
 const context=useRef(0),attempt=useRef<{key:string;accion:api.AccionToma;body:{revision:number;fundamento:string|null}}|null>(null);
 useEffect(()=>{let live=true;++context.current;V(null);cambio(null);E('');R('');B(false);attempt.current=null;
  void api.tomaClasificacion(id).then(data=>{if(live){V(data);cambio(data);}}).catch(e=>{if(live)E(e.message);});
  return()=>{live=false;++context.current;};
 },[id,version,refresh,cambio]);
 async function ejecutar(accion:api.AccionToma){
  if(!v||busy)return;const token=context.current;B(true);E('');cambio(null);
  if(!attempt.current)attempt.current={key:crypto.randomUUID(),accion,body:{revision:v.revision,fundamento:accion==='TOMAR'?null:reason}};
  const current=attempt.current;
  try{await api.cambiarTomaClasificacion(id,current.accion,current.body,current.key);if(token===context.current){attempt.current=null;F(n=>n+1);}}
  catch(e){if(token===context.current){E((e as Error).message);if(e instanceof api.ApiError){attempt.current=null;V(null);}}}
  finally{if(token===context.current)B(false);}
 }
 const retry=attempt.current;
 return <section aria-label="Toma para clasificación"><h2>Toma de clasificación</h2>
  <p>Tomar reserva esta Comunicación. No crea una SOL, un Requerimiento, un Caso ni un acuse de asignación.</p>
  {error&&<p role="alert">{error}</p>}{!v&&!error&&<p role="status">Consultando toma autorizada…</p>}
  {v&&<><p>{v.estado==='TOMADA'?(v.propia?'La Comunicación está tomada por tu cuenta.':'La Comunicación está tomada por otra cuenta.'):(v.estado==='FINALIZADA'?'La clasificación ya finalizó.':'La Comunicación no tiene una toma activa.')}</p>
   {v.tomadaEn&&<p>Último cambio: {new Date(v.tomadaEn).toLocaleString('es-CL',{timeZone:'America/Santiago'})}</p>}
   {!retry&&<>{v.puedeTomar&&<button disabled={busy} onClick={()=>void ejecutar('TOMAR')}>Tomar para clasificar</button>}
    {(v.puedeLiberar||v.puedeRecuperar)&&<label>Fundamento de liberación o recuperación<textarea maxLength={1000} required value={reason} onChange={e=>R(e.target.value)} disabled={busy}/></label>}
    {v.puedeLiberar&&<button disabled={busy||!reason.trim()} onClick={()=>void ejecutar('LIBERAR')}>Liberar mi toma</button>}
    {v.puedeRecuperar&&<button disabled={busy||!reason.trim()} onClick={()=>void ejecutar('RECUPERAR')}>Recuperar tras relevo acreditado</button>}
   </>}
   {retry&&!busy&&<button onClick={()=>void ejecutar(retry.accion)}>Reintentar la misma operación</button>}
   <details><summary>Historial de tomas</summary><ol>{v.historial.map(h=><li key={h.revision}>{h.accion.replaceAll('_',' ')} · {new Date(h.fecha).toLocaleString('es-CL',{timeZone:'America/Santiago'})}</li>)}</ol></details>
  </>}
  <button disabled={busy} onClick={()=>F(n=>n+1)}>Actualizar estado de la toma</button>
 </section>;
}
