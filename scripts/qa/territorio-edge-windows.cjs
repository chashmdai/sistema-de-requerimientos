const crypto=require('node:crypto'),fs=require('node:fs');
module.exports=async function({input,run,browser,get,change,ok,actor}){
 const account=(await get('/nucleo/ti/funcionarios')).find(c=>c.usuario==='qa.windows.otra');const grant=await change('/nucleo/ti/funcionarios/'+account.id+'/roles',{rol:'COORDINADOR_REGIONAL',alcance:'SEDE',unidad:null,sede:'DEMO_SEDE_A',desde:new Date(Date.now()-1000).toISOString(),hasta:null});
 const ctx=await browser.newContext({ignoreHTTPSErrors:false});let changedProfile=false;
 const original={nombre:account.nombre,correo:account.correo,cargo:account.cargo,unidad:account.unidad,sede:account.sede,activo:account.activo};
 async function login(){const t=await(await ctx.request.get(run.origin+'/nucleo/csrf')).json();const r=await ctx.request.post(run.origin+'/nucleo/login',{headers:{[t.headerName]:t.token},form:{username:account.usuario,password:input.restricted[account.usuario]}});ok('login coordinacion regional',r.status()===204);}
 async function post(path,body,key=crypto.randomUUID()){const t=await(await ctx.request.get(run.origin+'/nucleo/csrf')).json();return ctx.request.post(run.origin+path,{headers:{[t.headerName]:t.token,'Idempotency-Key':key},data:body});}
 const distribution={revision:0,destinos:[{unidad:'UPR',sede:'DEMO_SEDE_A',evaluador:actor}]};
 async function manual(site){const r=await post('/nucleo/comunicaciones/manual',{texto:'Distribución territorial completamente sintética Windows QA',canalDeclarado:'TELÉFONO',recibidaEnDeclarada:new Date(Date.now()-5000).toISOString(),sede:site,respuestaYaEntregada:null,regionAsunto:null});ok('funcionario registra desde sede '+site,r.status()===200);return r.json();}
 try{
  await login();const own=await manual('DEMO_SEDE_A'),key=crypto.randomUUID();const r=await post('/nucleo/comunicaciones/'+own.id+'/distribuir',distribution,key);ok('regional distribuye origen de su sede',r.status()===200);const again=await post('/nucleo/comunicaciones/'+own.id+'/distribuir',distribution,key);ok('distribucion regional idempotente',again.status()===200&&(await again.json()).id===(await r.json()).id);
  const detail=await get('/nucleo/comunicaciones/'+own.id);ok('distribucion regional no abre Caso ni Requerimiento',detail.destinos.length===1&&detail.casos.length===0&&detail.comunicacion.requerimientoId===null);
  const presence=(await get('/nucleo/catalogo')).presencias.find(p=>p.sede==='DEMO_SEDE_B');ok('presencia vigente de sede B disponible',Boolean(presence));const current=(await get('/nucleo/ti/funcionarios')).find(c=>c.id===account.id);await change('/nucleo/ti/funcionarios/'+account.id,{...original,unidad:presence.unidad,sede:presence.sede,revision:current.revision},crypto.randomUUID(),'PUT');changedProfile=true;ok('cambio de sede invalida sesion',(await ctx.request.get(run.origin+'/nucleo/session')).status()===401);await login();const foreign=await manual('DEMO_SEDE_B');
  ok('regional A no distribuye origen sede B',(await post('/nucleo/comunicaciones/'+foreign.id+'/distribuir',distribution)).status()===403);await change('/nucleo/comunicaciones/'+foreign.id+'/distribuir',distribution);ok('distribuidor central distribuye origen sede B',(await get('/nucleo/comunicaciones/'+foreign.id)).destinos.length===1);
  const entry=JSON.parse(fs.readFileSync(input.ingresoRun+'/e2e-ingreso.json','utf8')).cases[0];ok('regional no distribuye web sin sede',(await post('/nucleo/comunicaciones/'+entry.communication+'/distribuir',distribution)).status()===403);
 }finally{
  if(changedProfile){const current=(await get('/nucleo/ti/funcionarios')).find(c=>c.id===account.id);await change('/nucleo/ti/funcionarios/'+account.id,{...original,revision:current.revision},crypto.randomUUID(),'PUT');}
  await change('/nucleo/ti/funcionarios/'+account.id+'/roles/'+grant.id,undefined,crypto.randomUUID(),'DELETE');ok('revocacion regional invalida sesion',(await ctx.request.get(run.origin+'/nucleo/session')).status()===401);await ctx.close();
 }
};
