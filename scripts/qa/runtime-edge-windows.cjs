// E2E real Edge/HTTPS, datos sinteticos. Secretos solo stdin; sin trazas/cookies en evidencia.
const fs=require('node:fs');
const crypto=require('node:crypto');
const progress=[];
async function main(){
 const input=JSON.parse(fs.readFileSync(0,'utf8'));
 const {chromium}=require(input.playwright);
 const run=JSON.parse(fs.readFileSync(input.run+'/run.json','utf8'));
 if(!['PRELIMINAR_SIN_DOCUMENTAL','DOCUMENTAL_QA_CALIFICADO'].includes(run.scope)||run.database!=='siger_institucional_windows_qa'||!/^https:\/\/localhost:808[01]$/.test(run.origin))throw Error('GUARD');
 const checks=[];function ok(name,condition){if(!condition){const error=Error('QA_CHECK_FAILED');error.qaCheck=name;throw error;}checks.push(name);progress.push(name);}
 const browser=await chromium.launch({channel:'msedge',headless:true});progress.push('EDGE_STARTED');
 try{
  const context=await browser.newContext({ignoreHTTPSErrors:false});
  await context.route('**/*',route=>new URL(route.request().url()).origin===run.origin?route.continue():route.abort());
  const page=await context.newPage();
  await page.goto(run.origin);progress.push('HTTPS_PAGE');await page.getByLabel('Usuario individual',{exact:true}).fill('nexbyte');await page.getByLabel('Contraseña de laboratorio',{exact:true}).fill(input.nexbytePassword);await page.getByRole('button',{name:'Ingresar al laboratorio',exact:true}).click();await page.getByRole('button',{name:'Cerrar sesión',exact:true}).waitFor();progress.push('LOGIN_UI_OK');
  const request=context.request;
  async function get(path){const r=await request.get(run.origin+path);ok('GET '+path,r.ok());return r.json();}
  async function change(path,data,key=crypto.randomUUID(),method='POST',expected=200){const csrf=await get('/nucleo/csrf');const r=await request.fetch(run.origin+path,{method,headers:{[csrf.headerName]:csrf.token,'Idempotency-Key':key},data});ok(method+' '+path+' '+expected,r.status()===expected);return expected===200&&r.headers()['content-type']?.includes('json')?r.json():null;}
  const session=await get('/nucleo/session');const actor=session.operador.id;ok('siete roles individuales',new Set(session.operador.accesos.map(a=>a.rol)).size===7);ok('identidad individual',session.modo==='LABORATORIO_INDIVIDUAL');
  const cookies=await context.cookies();ok('cookie Secure HttpOnly Strict',cookies.some(c=>c.name==='SIGERSTAFFSESSION'&&c.secure&&c.httpOnly&&c.sameSite==='Strict'));
  ok('CSRF rechaza POST sin token',(await request.post(run.origin+'/nucleo/comunicaciones/manual',{data:{}})).status()===403);
  if(input.mode==='territorio'){await require('./territorio-edge-windows.cjs')({input,run,browser,get,change,ok,actor});const result={scope:run.scope,mode:input.mode,checks,passed:checks.length,failed:0,realGraph:false};fs.writeFileSync(input.run+'/resultado-edge-territorio.json',JSON.stringify(result,null,2));console.log(JSON.stringify({mode:input.mode,passed:checks.length,failed:0}));return;}
  await page.getByRole('link',{name:'Administración TI',exact:true}).click();await page.getByRole('heading',{name:/Funcionarios|Administración TI/}).first().waitFor();checks.push('UI TI accesible');
  const staff=await get('/nucleo/ti/funcionarios');ok('cuentas restringidas disponibles',staff.some(c=>c.usuario==='qa.windows.ti')&&staff.some(c=>c.usuario==='qa.windows.otra'));
  let saved;
  const evidenceFile=input.run+'/e2e-operacion.json';
  if(input.mode==='after-restart'){
   saved=JSON.parse(fs.readFileSync(input.baseline,'utf8'));
   const detail=await get('/nucleo/comunicaciones/'+saved.communication);
   ok('COM/folio conservado tras reinicio',detail.comunicacion.folio===saved.folio);
   const replay=await change('/nucleo/comunicaciones/manual',saved.manual,saved.manualKey);ok('idempotencia en JVM nueva',replay.id===saved.communication);
   const task=await get('/nucleo/tareas/'+saved.task);ok('tarea conservada',task.recurso===saved.caseId);
  }else{
   const manual={texto:'SIGER QA Windows: comunicación completamente sintética',canalDeclarado:'TELÉFONO',recibidaEnDeclarada:new Date(Date.now()-5000).toISOString(),sede:'DEMO_SEDE_A',respuestaYaEntregada:null,regionAsunto:null};
   const key=crypto.randomUUID(),com=await change('/nucleo/comunicaciones/manual',manual,key),again=await change('/nucleo/comunicaciones/manual',manual,key);ok('COM unico en reintento',com.id===again.id);
   const initial=await get('/nucleo/comunicaciones/'+com.id);ok('recepcion no crea Caso o Requerimiento',initial.casos.length===0&&initial.comunicacion.requerimientoId===null);
   await change('/nucleo/comunicaciones/'+com.id+'/distribuir',{revision:initial.comunicacion.revision,destinos:[{unidad:'UPR',sede:'DEMO_SEDE_A',evaluador:actor}]});
   const distributed=await get('/nucleo/comunicaciones/'+com.id);ok('distribuir no abre Caso',distributed.casos.length===0);
   const dest=distributed.destinos[0];const opened=await change('/nucleo/destinos/'+dest.id+'/abrir-caso',{revision:dest.revision,responsable:actor,complejidadProvisional:'BAJA'});
   const task=await change('/nucleo/tareas',{revision:0,titulo:'Recordatorio sintético Windows',descripcion:'Solo datos sintéticos de QA',recordatorio:new Date(Date.now()-1000).toISOString(),ventanaDesde:null,ventanaHasta:null,plazo:new Date(Date.now()+86400000).toISOString(),prioridad:'NORMAL',tipoRecurso:'CASO',recurso:opened.id,responsable:null});
   saved={communication:com.id,caseId:opened.id,task:task.id,folio:initial.comunicacion.folio,manual,manualKey:key};
   fs.writeFileSync(evidenceFile,JSON.stringify(saved,null,2));
   await change('/nucleo/notificaciones/operacion/procesar',{});
   const notice=await get('/nucleo/notificaciones');ok('notificaciones persistentes',notice.items.length>0);
   await page.getByRole('link',{name:'Mis tareas de Casos',exact:true}).click();await page.locator('a[href="#tareas/'+task.id+'"]').waitFor();checks.push('UI tarea del Caso');
   await page.getByRole('link',{name:'Notificaciones',exact:true}).click();await page.getByRole('heading',{name:'Mis notificaciones',exact:true}).waitFor();checks.push('UI campana/bandeja');
  }
  const notices=await get('/nucleo/notificaciones');const candidate=notices.items.find(n=>n.tipo==='TAREA_RECORDATORIO');
  ok('recordatorio visible',Boolean(candidate));
  const simulated=await change('/nucleo/notificaciones/'+candidate.id+'/correo',{});ok('outbox simulado',simulated.estado==='SIMULADO');
  const other=await browser.newContext({ignoreHTTPSErrors:false});
  try{
   async function restrictedLogin(ctx,user){const csrf=await (await ctx.request.get(run.origin+'/nucleo/csrf')).json();const login=await ctx.request.post(run.origin+'/nucleo/login',{headers:{[csrf.headerName]:csrf.token},form:{username:user,password:input.restricted[user]}});ok('login '+user,login.status()===204);}
   await restrictedLogin(other,'qa.windows.otra');
   ok('otra unidad no lee Caso',(await other.request.get(run.origin+'/nucleo/casos/'+saved.caseId)).status()===404);
   ok('otra unidad no lee tarea',(await other.request.get(run.origin+'/nucleo/tareas/'+saved.task)).status()===404);
   ok('profesional no administra TI',(await other.request.get(run.origin+'/nucleo/ti/funcionarios')).status()===403);
   const account=(await get('/nucleo/ti/funcionarios')).find(c=>c.usuario==='qa.windows.otra');
   const grant=await change('/nucleo/ti/funcionarios/'+account.id+'/roles',{rol:'ADMINISTRATIVO',alcance:'UNIDAD_SEDE',unidad:'UPD',sede:'DEMO_SEDE_A',desde:new Date(Date.now()-1000).toISOString(),hasta:null});
   try{
    ok('otorgar invalida sesion vigente',(await other.request.get(run.origin+'/nucleo/session')).status()===401);
    await restrictedLogin(other,'qa.windows.otra');
   }finally{await change('/nucleo/ti/funcionarios/'+account.id+'/roles/'+grant.id,undefined,crypto.randomUUID(),'DELETE');}
   ok('revocar invalida sesion vigente',(await other.request.get(run.origin+'/nucleo/session')).status()===401);
   await restrictedLogin(other,'qa.windows.otra');
   const current=(await get('/nucleo/ti/funcionarios')).find(c=>c.id===account.id);
   const profile={nombre:current.nombre,correo:current.correo,cargo:current.cargo,unidad:current.unidad,sede:current.sede,activo:false,revision:current.revision};
   const disabled=await change('/nucleo/ti/funcionarios/'+account.id,profile,crypto.randomUUID(),'PUT');
   try{ok('desactivar invalida sesion vigente',(await other.request.get(run.origin+'/nucleo/session')).status()===401);}
   finally{await change('/nucleo/ti/funcionarios/'+account.id,{...profile,activo:true,revision:disabled.revision},crypto.randomUUID(),'PUT');}
   const ti=await browser.newContext({ignoreHTTPSErrors:false});
   try{await restrictedLogin(ti,'qa.windows.ti');ok('TI solo no lee Caso',(await ti.request.get(run.origin+'/nucleo/casos/'+saved.caseId)).status()===404);ok('TI solo no lee Comunicacion',(await ti.request.get(run.origin+'/nucleo/comunicaciones/'+saved.communication)).status()===404);}finally{await ti.close();}
  }finally{await other.close();}
  if(input.mode!=='after-restart')await page.screenshot({path:input.run+'/edge-notificaciones.png',fullPage:true});
  await page.goto(run.origin+'/#caso/'+saved.caseId);await page.getByRole('heading',{name:/^Caso ·/}).waitFor();checks.push('UI Caso y contexto Requerimiento');
  await page.getByRole('button',{name:'Mis tareas de este Caso',exact:true}).click();await page.getByText('Recordatorio sintético Windows',{exact:true}).waitFor();checks.push('UI tareas dentro del Caso');
  if(run.documentWorkers){await require('./documental-edge-windows.cjs')({input,run,saved,browser,context,page,request,get,change,ok});fs.writeFileSync(input.run+'/e2e-operacion.json',JSON.stringify(saved,null,2));}
  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.getByLabel('Usuario individual',{exact:true}).waitFor();
  const ended=await (await request.get(run.origin+'/nucleo/session')).json();ok('logout invalida sesion',!ended.autenticado);
  fs.writeFileSync(input.run+'/resultado-edge-'+input.mode+'.json',JSON.stringify({scope:run.scope,checks,passed:checks.length,failed:0,documental:run.documentWorkers,realGraph:false,version:browser.version()},null,2));
  console.log(JSON.stringify({scope:run.scope,mode:input.mode,passed:checks.length,documental:run.documentWorkers}));
  await context.close();
 }finally{await browser.close();}
}
main().catch(e=>{console.error(JSON.stringify({error:'E2E_QA_FAILED',errorClass:e.name,failureCheck:e.qaCheck,lastCompleted:progress.at(-1),completed:progress.length,certificateError:/CERT_|SELF_SIGNED/.test(e.message??'')}));process.exitCode=1;});
