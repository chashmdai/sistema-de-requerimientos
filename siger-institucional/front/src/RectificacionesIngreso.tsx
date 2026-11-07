import {useEffect,useRef,useState} from 'react';
import * as api from './api';
import type {Catalog} from './api';
import {date} from './ui';

export type DatosIngreso={texto:string;canalDeclarado:string;recibidaEnDeclarada:string;regionAsunto:string|null};
type Constancia={secuencia:number;revision:number;autor:string;registradaEn:string;motivo:string;anterior:DatosIngreso;nuevo:DatosIngreso};
export type VistaRectificaciones={version:string;revision:number;original:DatosIngreso;vigente:DatosIngreso;entradas:Constancia[];total:number;pagina:number;tamanio:number};
type Solicitud={version:'1.0';revision:number;datos:DatosIngreso;motivo:string};
const local=(v:string)=>{const d=new Date(v);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}.${String(d.getMilliseconds()).padStart(3,'0')}`;};
const normalLocal=(v:string)=>{const m=/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?$/.exec(v);return m?`${m[1]}:${m[2]??'00'}.${(m[3]??'').padEnd(3,'0')}`:v;};
function Datos({value:v}:{value:DatosIngreso}){return <dl><dt>Antecedente capturado</dt><dd>{v.texto}</dd><dt>Canal declarado</dt><dd>{v.canalDeclarado}</dd><dt>Recepción efectiva declarada</dt><dd>{date(v.recibidaEnDeclarada)}</dd><dt>Región del asunto</dt><dd>{v.regionAsunto??'No informada'}</dd></dl>;}

export function RectificacionesIngreso({comunicacion,catalog,version,changed,externBusy=false}:{comunicacion:string;catalog:Catalog;version:number;changed:()=>void;externBusy?:boolean}){
 const[data,D]=useState<VistaRectificaciones|null>(null),[error,E]=useState(''),[busy,B]=useState(false),[pending,Q]=useState(false),[editing,C]=useState(false),[page,P]=useState(0),[refresh,F]=useState(0),[linked,L]=useState<Constancia|null>(null);
 const[text,T]=useState(''),[channel,H]=useState(''),[received,V]=useState(''),[region,R]=useState(''),[reason,M]=useState('');
 const attempt=useRef<{base:string;body:Solicitud;key:string}|null>(null),generation=useRef(0);
 const base=`/nucleo/comunicaciones/${encodeURIComponent(comunicacion)}/rectificaciones`;
 useEffect(()=>{attempt.current=null;Q(false);C(false);P(0);},[base]);
 useEffect(()=>{let live=true;generation.current++;D(null);L(null);void api.gestionGet<VistaRectificaciones>(`${base}?pagina=${page}&tamanio=25`).then(v=>{if(live){D(v);E('');}}).catch(e=>{if(live)E(e.message);});return()=>{live=false;};},[base,page,version,refresh]);
 function comenzar(){if(!data)return;T(data.vigente.texto);H(data.vigente.canalDeclarado);V(local(data.vigente.recibidaEnDeclarada));R(data.vigente.regionAsunto??'');M('');C(true);E('');}
 async function enviar(){
  if(!data)return;B(true);E('');
  try{
   if(!attempt.current)attempt.current={base,key:crypto.randomUUID(),body:{version:'1.0',revision:data.revision,datos:{texto:text,canalDeclarado:channel,recibidaEnDeclarada:normalLocal(received)===local(data.vigente.recibidaEnDeclarada)?data.vigente.recibidaEnDeclarada:new Date(received).toISOString(),regionAsunto:region||null},motivo:reason}};
   const current=attempt.current;await api.command(current.base,current.body,current.key);attempt.current=null;Q(false);C(false);P(0);F(v=>v+1);changed();
  }catch(e){
   if(e instanceof api.ApiError&&e.status>=400&&e.status<500){attempt.current=null;Q(false);if([401,403,404].includes(e.status)){D(null);L(null);C(false);}}
   else Q(attempt.current!==null);
   E((e as Error).message);
  }finally{B(false);}
 }
 async function consultar(secuencia:number){const current=generation.current;L(null);try{const result=await api.gestionGet<Constancia>(`${base}/${secuencia}`);if(current===generation.current)L(result);}catch(e){if(current===generation.current){D(null);L(null);E((e as Error).message);}}}
 return <section aria-label="Rectificaciones del ingreso"><h2>Rectificaciones de captura manual</h2><p>Cada corrección conserva el dato anterior, motivo, autor y fecha. El original recibido y su procedencia permanecen disponibles en su pestaña.</p>
  {error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Consultando rectificaciones autorizadas…</p>}
  {data&&<><h3>Datos de captura vigentes para consulta</h3><Datos value={data.vigente}/><p>La región del asunto es distinta del domicilio del requiriente. Rectificar captura no cambia la distribución, el principal, una respuesta entregada o el cierre.</p>
   {!editing&&<button disabled={busy||externBusy||pending} onClick={comenzar}>Rectificar datos de captura</button>}
   {editing&&<form onSubmit={e=>{e.preventDefault();void enviar();}}><fieldset disabled={busy||externBusy||pending}>
    <label>Antecedente de captura corregido<textarea required maxLength={2000} value={text} onChange={e=>T(e.target.value)}/></label>
    <label>Canal declarado corregido<input required maxLength={100} value={channel} onChange={e=>H(e.target.value)}/></label>
    <label>Recepción declarada corregida<input required type="datetime-local" step="0.001" value={received} onChange={e=>V(e.target.value)}/></label><p>Entrada en la zona de este navegador: {Intl.DateTimeFormat().resolvedOptions().timeZone}. Fechas de consulta en America/Santiago.</p>
    <label>Región del asunto corregida<select value={region} onChange={e=>R(e.target.value)}><option value="">No informada</option>{catalog.regiones?.map(r=><option key={r.id} value={r.id}>{r.nombre}</option>)}</select></label>
    <label>Motivo de corrección de captura<textarea required maxLength={2000} value={reason} onChange={e=>M(e.target.value)}/></label>
   </fieldset>
   {pending&&<p role="status">El resultado no está confirmado. Reintenta la misma rectificación para evitar duplicarla.</p>}
   <button disabled={busy||externBusy||!text.trim()||!channel.trim()||!received||!reason.trim()}>{pending?'Reintentar la misma rectificación':'Confirmar rectificación de captura'}</button><button type="button" disabled={busy||pending} onClick={()=>C(false)}>Cancelar corrección</button></form>}
   <button disabled={busy||pending} onClick={()=>F(v=>v+1)}>Actualizar historial de captura</button>
   <h3>Historia de rectificaciones</h3><p>{data.total} rectificaciones autorizadas.</p>{!data.entradas.length&&<p>Sin rectificaciones de captura registradas.</p>}
   <ol>{data.entradas.map(c=><li key={c.secuencia}><p>{c.motivo} · {c.autor} · {date(c.registradaEn)}</p><details><summary>Comparar valores anterior y nuevo</summary><h4>Anterior</h4><Datos value={c.anterior}/><h4>Nuevo</h4><Datos value={c.nuevo}/></details><button disabled={busy||pending} onClick={()=>void consultar(c.secuencia)}>Consultar constancia {c.secuencia}</button></li>)}</ol>
   <button disabled={busy||pending||page===0} onClick={()=>P(v=>v-1)}>Rectificaciones anteriores</button><button disabled={busy||pending||(page+1)*25>=data.total} onClick={()=>P(v=>v+1)}>Rectificaciones siguientes</button>
  </>}{linked&&<article aria-label="Constancia de rectificación"><h3>Constancia {linked.secuencia}</h3><p>{linked.motivo} · {linked.autor} · {date(linked.registradaEn)}</p><h4>Valor anterior</h4><Datos value={linked.anterior}/><h4>Valor nuevo</h4><Datos value={linked.nuevo}/></article>}
 </section>;
}
