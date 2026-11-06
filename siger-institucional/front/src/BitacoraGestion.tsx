import {useEffect,useRef,useState} from 'react';
import * as api from './api';
import {Dialog} from './ui';

export type EntradaGestion={id:string;version:string|null;descripcion:string;realizadaEn:string|null;registradaEn:string;registrador:string;ejecutor:string|null;rectifica:string|null;rectificadaPor:string|null;motivo:string|null};
export type VistaBitacora={contexto:'CASO'|'REQ';recurso:string;revision:number;puedeRegistrar:boolean;entradas:EntradaGestion[];pagina:number;tamanio:number;total:number};
type Registro={version:'1.0';revision:number;realizadaEn:string;descripcion:string;rectifica:string|null;motivo:string|null};
const fecha=(v:string)=>new Date(v).toLocaleString('es-CL',{timeZone:'America/Santiago'});
const local=(v:string)=>{const d=new Date(v);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}.${String(d.getMilliseconds()).padStart(3,'0')}`;};

const normalLocal=(v:string)=>{const m=/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?$/.exec(v);return m?`${m[1]}:${m[2]??"00"}.${(m[3]??"").padEnd(3,"0")}`:v;};

export function BitacoraGestion({contexto,recurso,version,changed,abrir=false,cerrar}:{contexto:'CASO'|'REQ';recurso:string;version:number;changed?:()=>void;abrir?:boolean;cerrar?:()=>void}){
 const [linked,L]=useState<EntradaGestion|null>(null);
 const [data,D]=useState<VistaBitacora|null>(null),[error,E]=useState(''),[busy,B]=useState(false),[pagina,P]=useState(0),[refresh,F]=useState(0);
 const [creating,C]=useState(false),[original,O]=useState<EntradaGestion|null>(null),[description,T]=useState(''),[effective,V]=useState(''),[reason,R]=useState(''),[pending,Q]=useState(false);
 const generation=useRef(0);
 const attempt=useRef<{base:string;body:Registro;key:string}|null>(null);
 const base=contexto==='CASO'?`/nucleo/casos/${encodeURIComponent(recurso)}/bitacora`:`/nucleo/institucional/requerimientos/${encodeURIComponent(recurso)}/bitacora`;
 useEffect(()=>{attempt.current=null;Q(false);C(false);O(null);T('');V('');R('');P(0);L(null);},[base]);
 useEffect(()=>{let live=true;generation.current++;L(null);D(null);void api.gestionGet<VistaBitacora>(`${base}?pagina=${pagina}&tamanio=25`).then(v=>{if(live){D(v);E('');}}).catch(e=>{if(live)E(e.message);});return()=>{live=false;};},[base,pagina,version,refresh]);
 async function consultarVinculada(id:string){const current=generation.current;L(null);try{const entry=await api.gestionGet<EntradaGestion>(`${base}/constancias/${encodeURIComponent(id)}`);if(current===generation.current)L(entry);}catch(e){if(current===generation.current){D(null);E((e as Error).message);}}}
 function comenzar(entry:EntradaGestion|null){O(entry);T(entry?.descripcion??'');V(entry?.realizadaEn?local(entry.realizadaEn):'');R('');C(true);E('');}
 function cancelar(){C(false);O(null);T('');V('');R('');cerrar?.();}
 async function enviar(){
  if(!data)return;
  B(true);E('');
  try{
   if(!attempt.current)attempt.current={base,body:{version:'1.0',revision:data.revision,realizadaEn:original?.realizadaEn&&normalLocal(effective)===local(original.realizadaEn)?original.realizadaEn:new Date(effective).toISOString(),descripcion:description,rectifica:original?.id??null,motivo:original?reason:null},key:crypto.randomUUID()};
   const current=attempt.current;
   await api.command(current.base,current.body,current.key);
   attempt.current=null;Q(false);cancelar();P(0);F(v=>v+1);changed?.();
  }catch(e){
   if(e instanceof api.ApiError&&e.status>=400&&e.status<500){attempt.current=null;Q(false);}
   else Q(attempt.current!==null);
   E((e as Error).message);
  }finally{B(false);}
 }
 const form=(creating||abrir)&&data?.puedeRegistrar;
 return <section className="card" aria-label="Bitácora de gestión"><h2>Bitácora de gestión</h2><p>Fechas mostradas en America/Santiago. La fecha de la gestión y la del registro se conservan por separado.</p>
  {error&&<p className="error" role="alert">{error}</p>}
  {!data&&!error&&<p role="status">Consultando actuaciones autorizadas…</p>}
  {data&&<>
   <p>{data.total} actuaciones en este {contexto==='CASO'?'Caso':'Requerimiento'}.</p>
   {data.puedeRegistrar&&!form&&<button disabled={busy||pending} onClick={()=>comenzar(null)}>Registrar gestión realizada</button>}
   {form&&<form onSubmit={e=>{e.preventDefault();void enviar();}}>
    <h3>{original?'Rectificación vinculada':'Nueva actuación'}</h3><p>Registra aquí la gestión ordinaria. Los contenidos reservados se conservan en documentos internos protegidos.</p>
    {original&&<p>Se conserva íntegramente la constancia anterior. La rectificación tendrá su propia autoría y fecha de registro.</p>}
    <fieldset disabled={busy||pending}><label>Fecha y hora efectivas de la gestión<input required type="datetime-local" step="0.001" value={effective} onChange={e=>V(e.target.value)}/></label><p>Entrada en la zona de este navegador: {Intl.DateTimeFormat().resolvedOptions().timeZone}. Se guarda en UTC.</p>
     <label>Descripción de la gestión realizada<textarea required maxLength={12000} value={description} onChange={e=>T(e.target.value)}/></label>
     {original&&<label>Motivo de rectificación<textarea required maxLength={2000} value={reason} onChange={e=>R(e.target.value)}/></label>}
    </fieldset>
    {pending&&<p role="status">El resultado del envío todavía no está confirmado. Reintenta la misma constancia para evitar duplicarla.</p>}
    <button disabled={busy||!effective||!description.trim()||!!original&&!reason.trim()}>{pending?'Reintentar confirmación':original?'Registrar rectificación':'Confirmar actuación realizada'}</button>
    <button type="button" disabled={busy||pending} onClick={cancelar}>Cancelar edición</button>
   </form>}
   {!data.entradas.length?<p>Sin actuaciones registradas en este contexto.</p>:<ol className="timeline">{data.entradas.map(e=><li key={e.id}>
    <p>{e.descripcion}</p><dl><dt>Gestión realizada</dt><dd>{e.realizadaEn?fecha(e.realizadaEn):'Fecha efectiva no acreditada en el registro histórico'}</dd><dt>Registro persistido</dt><dd>{fecha(e.registradaEn)}</dd><dt>Registrador</dt><dd>{e.registrador}</dd><dt>Ejecutor</dt><dd>{e.ejecutor??'No acreditado separadamente en el registro histórico'}</dd></dl>
    {e.rectifica&&<p>Rectifica la constancia {e.rectifica}. Motivo: {e.motivo} <button onClick={()=>void consultarVinculada(e.rectifica!)}>Consultar original vinculado</button></p>}{e.rectificadaPor&&<p>Conservada; tiene una rectificación vinculada: {e.rectificadaPor}. <button onClick={()=>void consultarVinculada(e.rectificadaPor!)}>Consultar rectificación vinculada</button></p>}
    {data.puedeRegistrar&&!e.rectificadaPor&&<button disabled={busy||pending} onClick={()=>comenzar(e)}>Rectificar constancia {e.id}</button>}
   </li>)}</ol>}
   <div className="pager"><span>Página {data.pagina+1}</span><button disabled={busy||pending||pagina===0} onClick={()=>P(v=>v-1)}>Actuaciones anteriores</button><button disabled={busy||pending||(pagina+1)*data.tamanio>=data.total} onClick={()=>P(v=>v+1)}>Actuaciones siguientes</button></div>
  </>}
  {linked&&<Dialog title="Constancia vinculada" close={()=>L(null)}><p>{linked.descripcion}</p><p>Gestión: {linked.realizadaEn?fecha(linked.realizadaEn):"Fecha efectiva no acreditada"}</p><p>Registro: {fecha(linked.registradaEn)} · {linked.registrador}</p>{linked.motivo&&<p>Motivo: {linked.motivo}</p>}</Dialog>}
  <button disabled={busy||pending} onClick={()=>{L(null);F(v=>v+1);}}>Actualizar bitácora autorizada</button>
  <p className="muted">Una tarea, lectura o correo no genera una actuación. Estos tiempos no representan un plazo de resolución ni un incumplimiento calculado de las 48 horas de registro.</p>
 </section>;
}
