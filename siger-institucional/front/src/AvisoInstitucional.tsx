import {useEffect,useState} from 'react';
import * as api from './api';
import type {Catalog} from './api';

type Contexto={unidad:string;sede:string;area:string|null};
type Vista={tipo:string;fecha:string;folio:string;origen:Contexto|null;destino:Contexto|null;ruta:string|null;pendienteActual:boolean};
export type EnrutamientoAviso={tipo:string;fecha:string;estado:string};
const titulo=(tipo:string)=>({COM_TOMADA:'Comunicación tomada para clasificar',CASO_ASIGNADO:'Caso asignado o reasignado',CASO_TERMINO_DEVUELTO:'Revisión de término devuelta',CASO_CERRADO:'Cierre de Caso registrado',REQ_TERMINADO:'Cierre explícito de Requerimiento registrado',REQ_REACTIVADO:'Requerimiento reactivado por acto autorizado',SOL_DISTRIBUIDA:'Solicitud institucional distribuida',SOL_REDIRIGIDA:'Solicitud institucional redirigida',REQ_ASIGNADO:'Atención de Requerimiento asignada'}[tipo]??'Aviso institucional');
function Ambito({contexto:c,catalog}:{contexto:Contexto;catalog:Catalog}){return <span>{c.unidad} · {catalog.sedes.find(s=>s.id===c.sede)?.nombre??c.sede} · {c.area?(catalog.areas?.find(a=>a.id===c.area)?.nombre??c.area):'Sin área formal'}</span>;}
export function EnrutamientoAvisos({avisos}:{avisos?:EnrutamientoAviso[]}){
 if(!avisos?.length)return null;
 const estado:Record<string,string>={PENDIENTE_PROYECCION:'Pendiente de preparación',SIN_DESTINATARIO_VIGENTE:'Pendiente de destinatario habilitado',PROYECTADO:'Avisos creados; el transporte se consulta en Mis notificaciones',SUPRIMIDO_SIN_PENDIENTE:'Evento conservado sin acción pendiente actual'};
 return <section aria-label="Enrutamiento de avisos"><h3>Avisos institucionales</h3>{avisos.map((a,i)=><p key={i}>{titulo(a.tipo)} · {estado[a.estado]??'Consultar estado autorizado'}</p>)}<p>La preparación del aviso, el envío de correo y su lectura se registran por separado.</p></section>;
}
export function AvisoInstitucional({id,version,catalog}:{id:string;version:number;catalog:Catalog}){
 const[data,D]=useState<Vista|null>(null),[error,E]=useState(''),[busy,B]=useState(false),[read,R]=useState(false),[refresh,F]=useState(0);
 useEffect(()=>{let live=true;D(null);R(false);void api.gestionGet<Vista>(`/nucleo/notificaciones/${encodeURIComponent(id)}/contexto-institucional`).then(v=>{if(live){D(v);E('');}}).catch(e=>{if(live)E(e.message);});return()=>{live=false;};},[id,version,refresh]);
 async function leer(){B(true);try{await api.leerNotificacion(id);R(true);E('');}catch(e){D(null);R(false);E((e as Error).message);}finally{B(false);}}
 return <section className="card" aria-label="Aviso institucional"><h1>Aviso institucional</h1>
  {error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Consultando referencia autorizada…</p>}
  {data&&<><h2>{titulo(data.tipo)}</h2><p>{data.folio} · {new Date(data.fecha).toLocaleString('es-CL',{timeZone:'America/Santiago'})}</p>
   {data.origen&&<p>Origen: <Ambito contexto={data.origen} catalog={catalog}/></p>}{data.destino&&<p>Destino: <Ambito contexto={data.destino} catalog={catalog}/></p>}
   <p>{data.pendienteActual?'Consulta el pendiente vigente en su bandeja.':'Referencia histórica conservada.'}</p>
   {data.ruta?<a href={'#'+data.ruta}>Consultar recurso autorizado</a>:<p>Referencia de coordinación. Los recursos se consultan con sus autorizaciones vigentes.</p>}
   <p>Leer este aviso registra su lectura. Las decisiones de asignación, aceptación o gestión se realizan en sus circuitos correspondientes.</p>
   <button disabled={busy||read} onClick={()=>void leer()}>Marcar aviso como leído</button>{read&&<p role="status">Lectura del aviso registrada.</p>}
  </>}
  <button disabled={busy} onClick={()=>F(v=>v+1)}>Actualizar aviso institucional</button><p><a href="#notificaciones">Volver a Mis notificaciones</a></p>
 </section>;
}
