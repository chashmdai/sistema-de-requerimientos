import {useEffect,useState} from 'react';
import App from './App';
import SmrApp from './SmrApp';
export default function Entry(){const [mode,setMode]=useState(''),[error,setError]=useState(false);useEffect(()=>{void fetch('/nucleo/session',{credentials:'same-origin',cache:'no-store'}).then(r=>{if(!r.ok)throw new Error();return r.json();}).then(s=>setMode(s.modo==='SMR'?'SMR':'SIGER')).catch(()=>setError(true));},[]);return mode==='SMR'?<SmrApp/>:mode==='SIGER'?<App/>:<main>{error?<p role="alert">La aplicación no está disponible. Comprueba su estado antes de ingresar.</p>:<p role="status">Conectando…</p>}</main>;}
