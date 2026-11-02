import {useState} from 'react';
import * as api from './api';

export function CambioClave({changed,logout}:{changed:()=>Promise<void>;logout:()=>Promise<void>}){
 const [actual,A]=useState(''),[nueva,N]=useState(''),[confirmacion,C]=useState(''),[busy,B]=useState(false),[error,E]=useState('');
 async function submit(){
  E('');if(nueva!==confirmacion){E('La confirmación no coincide con la nueva contraseña.');return;}
  if(new TextEncoder().encode(nueva).length>72){E('La nueva contraseña supera el máximo de 72 bytes UTF-8.');return;}
  B(true);try{await api.cambiarClavePersonal(actual,nueva);A('');N('');C('');await changed();}catch(e){E((e as Error).message);A('');N('');C('');}finally{B(false);}
 }
 return <main className="login card" id="contenido"><h1>Cambiar contraseña temporal</h1><p>Antes de acceder a SIGER debes reemplazar la contraseña inicial o restablecida. Después del cambio vuelve a ingresar con la nueva contraseña.</p>{error&&<p className="error" role="alert">{error}</p>}
 <form onSubmit={e=>{e.preventDefault();void submit();}}>
 <label>Contraseña actual<input type="password" required autoComplete="current-password" maxLength={128} value={actual} onChange={e=>A(e.target.value)}/></label>
 <label>Nueva contraseña personal<input type="password" required autoComplete="new-password" minLength={16} maxLength={72} value={nueva} onChange={e=>N(e.target.value)}/></label>
 <label>Confirmar nueva contraseña<input type="password" required autoComplete="new-password" minLength={16} maxLength={72} value={confirmacion} onChange={e=>C(e.target.value)}/></label>
 <p>Utiliza una contraseña personal distinta de la temporal: al menos 16 caracteres y hasta 72 bytes UTF-8.</p><button className="primary" disabled={busy}>Cambiar contraseña y volver a ingresar</button>
 </form><button disabled={busy} onClick={()=>{void logout().catch(e=>E((e as Error).message));}}>Cerrar sesión</button></main>;
}
