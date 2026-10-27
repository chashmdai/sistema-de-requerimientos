// QA sintético U5. Reutiliza login/HTTPS de Windows; nunca transporte externo.
const fs=require('node:fs'),crypto=require('node:crypto');
module.exports=async function({input,run,browser,context,page,request,get,change,ok}){
 const savedFile=input.run+'/e2e-u5-operacion.json';
 if(input.mode==='u5-documentos'){
  const cases=JSON.parse(fs.readFileSync(input.ingresoRun+'/e2e-ingreso.json','utf8')).cases;
  for(const entry of cases){
   const detail=await get('/nucleo/comunicaciones/'+entry.communication);ok('U5 contrato e identidad '+entry.type+' '+entry.version,detail.contenido.formularioCompleto.version===entry.version&&detail.contenido.formularioCompleto.tipo===entry.type);
   const docs=await get('/nucleo/comunicaciones/'+entry.communication+'/documentos');ok('U5 conserva originales de ingreso '+entry.type,docs.length===2&&docs.every(d=>d.origen==='INGRESO_CIUDADANO'));
   for(const doc of docs){const url='/nucleo/comunicaciones/'+entry.communication+'/documentos/'+doc.id+'/original';const res=await request.get(run.origin+url);ok('U5 descarga autorizada integra y privada '+doc.original.nombre,res.status()===200&&crypto.createHash('sha256').update(await res.body()).digest('hex')===doc.original.sha256&&res.headers()['cache-control'].includes('no-store'));}
   const prepared=await change('/nucleo/solicitudes',{revision:0,comunicacion:entry.communication,titulo:'Preparación desde ingreso público sintético '+entry.type,descripcion:''});ok('U5 preparación conserva origen y COM',(await get('/nucleo/solicitudes/'+prepared.id)).folio===entry.folio);
   ok('U5 preparación no abre Caso ni Requerimiento',(await get('/nucleo/comunicaciones/'+entry.communication)).comunicacion.requerimientoId===null);
  }
  const ctx=await browser.newContext({ignoreHTTPSErrors:false});try{const r=ctx.request,t=await(await r.get(run.origin+'/nucleo/csrf')).json();await r.post(run.origin+'/nucleo/login',{headers:{[t.headerName]:t.token},form:{username:'qa.windows.ti',password:input.restricted['qa.windows.ti']}});const doc=(await get('/nucleo/comunicaciones/'+cases[0].communication+'/documentos'))[0];ok('U5 TI no descarga original ciudadano',(await r.get(run.origin+'/nucleo/comunicaciones/'+cases[0].communication+'/documentos/'+doc.id+'/original')).status()===404);}finally{await ctx.close();}
  const original=(await get('/nucleo/comunicaciones/'+cases[0].communication+'/documentos'))[0];
  const evidence={scope:'U5_CUSTODIA_CIUDADANA_SINTETICA',communications:4,originals:8,downloadIntegrityVerified:true,tiDownloadDenied:true,realGraph:false,publicRecovery:false,preview:'PENDIENTE'};
  const preview=await request.get(run.origin+'/nucleo/comunicaciones/'+cases[0].communication+'/documentos/'+original.id+'/vista');
  evidence.previewHttp=preview.status();
  fs.writeFileSync(input.run+'/u5-custodia-ciudadana-parcial.json',JSON.stringify(evidence,null,2));
  if(!preview.ok()){
   await page.goto(run.origin+'/#comunicacion/'+cases[0].communication);await page.getByRole('button',{name:'Previsualizar tres-paginas.pdf',exact:true}).click();await page.getByRole('dialog').getByRole('alert').waitFor();ok('U5 UI documental conserva error controlado',await page.getByRole('button',{name:'Reintentar vista',exact:true}).isVisible());
   evidence.preview='NO_ACREDITADA_HTTP_'+preview.status();evidence.controlledUiError=true;fs.writeFileSync(input.run+'/u5-custodia-ciudadana-parcial.json',JSON.stringify(evidence,null,2));const error=Error('DOCUMENTAL_QA_NO_ACREDITADO');error.qaCheck='Vista documental HTTP '+preview.status()+'; conservar fallo cerrado';throw error;
  }
  await page.goto(run.origin+'/#comunicacion/'+cases[0].communication);await page.getByRole('button',{name:'Previsualizar tres-paginas.pdf',exact:true}).click();await page.getByText('Página 1 de 3',{exact:true}).waitFor({timeout:90000});await page.waitForFunction(()=>document.querySelector('.visor-superficie img')?.naturalWidth>0);await page.getByRole('button',{name:'Página siguiente',exact:true}).click();await page.getByRole('img',{name:'Página 2 de tres-paginas.pdf',exact:true}).waitFor();await page.waitForFunction(()=>document.querySelector('.visor-superficie img')?.naturalWidth>0);ok('U5 visor nativo de original ciudadano sin descarga manual',await page.locator('iframe,object,embed').count()===0);await page.getByRole('button',{name:'Volver a documentos',exact:true}).click();

  return;
 }
 if(input.mode==='u5-restart'){
  const saved=JSON.parse(fs.readFileSync(input.baseline,'utf8'));
  const req=await get('/nucleo/solicitudes/'+saved.solicitud),task=await get('/nucleo/tareas/'+saved.independiente);
  ok('U5 solicitud y borrador persisten tras reinicio',req.revision===3&&req.tipoPreparado==='CANALIZACION');
  ok('U5 tarea independiente persiste completada',task.estado==='COMPLETADA'&&task.tipoRecurso===null&&task.confirmacionManual);
  ok('U5 seguimiento durable tras reinicio',(await get('/nucleo/solicitudes/'+saved.solicitud+'/seguimientos')).length===1);
  const replay=await change('/nucleo/solicitudes',saved.requestBody,saved.requestKey);ok('U5 reintento post reinicio conserva Solicitud',replay.id===saved.requestReplay);
  const replayTask=await change('/nucleo/tareas',saved.taskBody,saved.taskKey);ok('U5 reintento post reinicio conserva tarea',replayTask.id===saved.replayTask);
  await page.goto(run.origin+'/#solicitudes/'+saved.solicitud);await page.getByRole('heading',{name:'Borrador de gestión',exact:true}).waitFor();ok('U5 UI borrador tras reinicio',(await page.getByLabel('Texto del borrador').inputValue())==='Borrador sintético U5');
  for(const action of ['responder','canalizar','escalar'])await change('/nucleo/solicitudes/'+saved.solicitud+'/'+action,{},undefined,'POST',409);
  const notices=await get('/nucleo/notificaciones?limite=50');ok('U5 avisos no duplicados tras reinicio',new Set(notices.items.map(n=>n.id)).size===notices.items.length);
  fs.writeFileSync(savedFile,JSON.stringify(saved,null,2));return;
 }
 const before=await get('/nucleo/comunicaciones/buscar?limite=20');
 const stamp=crypto.randomUUID(),manual={texto:'Ingreso sintético U5 '+stamp,canalDeclarado:'TELÉFONO',recibidaEnDeclarada:new Date(Date.now()-5000).toISOString(),respuestaYaEntregada:null,sede:'DEMO_SEDE_A',regionAsunto:null};
 const manualKey=crypto.randomUUID(),com=await change('/nucleo/comunicaciones/manual',manual,manualKey);
 const detail=await get('/nucleo/comunicaciones/'+com.id);ok('U5 ingreso no crea Requerimiento ni Caso',detail.comunicacion.requerimientoId===null&&detail.casos.length===0);
 await page.goto(run.origin+'/#comunicacion/'+com.id);await page.getByRole('button',{name:'Solicitudes en preparación',exact:true}).click();await page.getByRole('button',{name:'Preparar Solicitud desde esta Comunicación',exact:true}).click();
 await page.getByLabel('Título de Solicitud',{exact:true}).fill('Consulta sintética U5 '+stamp);await page.getByLabel('Descripción de Solicitud',{exact:true}).fill('Preparación personal de laboratorio');await page.getByRole('button',{name:'Guardar preparación de Solicitud',exact:true}).click();await page.waitForURL(/#solicitudes\/[a-f0-9-]+$/);
 const solicitud=page.url().split('/').pop();await page.getByRole('heading',{name:'Borrador de gestión',exact:true}).waitFor();
 for(const name of ['Responder Solicitud','Canalizar Solicitud','Escalar a Requerimiento'])ok('U5 UI cerrada '+name,await page.getByRole('button',{name,exact:true}).isDisabled());
 await page.getByLabel('Tipo de preparación',{exact:true}).selectOption('CANALIZACION');await page.getByLabel('Destino propuesto',{exact:true}).selectOption('UPR|DEMO_SEDE_A');await page.getByLabel('Texto del borrador',{exact:true}).fill('Borrador sintético U5');
 const preparationReply=page.waitForResponse(r=>r.url().endsWith('/solicitudes/'+solicitud+'/preparacion')&&r.request().method()==='POST');await page.getByRole('button',{name:'Guardar borrador sin ejecutar',exact:true}).click();const prepared=await preparationReply;ok('U5 UI guarda borrador HTTP '+prepared.status(),prepared.status()===200);await page.getByText('preparada',{exact:true}).waitFor();ok('U5 UI guarda borrador sin ejecutar',true);
 await page.getByLabel('Nuevo seguimiento',{exact:true}).fill('Seguimiento sintético U5');const followReply=page.waitForResponse(r=>r.url().endsWith('/solicitudes/'+solicitud+'/seguimientos')&&r.request().method()==='POST');await page.getByRole('button',{name:'Registrar seguimiento',exact:true}).click();ok('U5 UI seguimiento HTTP '+(await followReply).status(),(await followReply).status()===200);await page.getByText('Seguimiento sintético U5',{exact:true}).waitFor();ok('U5 UI seguimiento visible',true);
 await page.getByRole('button',{name:'Editar preparación de Solicitud',exact:true}).click();await page.getByLabel('Título de Solicitud',{exact:true}).fill('Consulta actualizada U5 '+stamp);const editReply=page.waitForResponse(r=>r.url().endsWith('/solicitudes/'+solicitud)&&r.request().method()==='PUT');await page.getByRole('button',{name:'Guardar preparación de Solicitud',exact:true}).click();ok('U5 UI edición HTTP '+(await editReply).status(),(await editReply).status()===200);await page.getByRole('heading',{name:'Consulta actualizada U5 '+stamp,exact:true}).waitFor();
 const req=await get('/nucleo/solicitudes/'+solicitud);ok('U5 revisiones de preparación seguimiento y edición',req.revision===3&&req.comunicacion===com.id);
 ok('U5 historial durable',(await get('/nucleo/solicitudes/'+solicitud+'/historial')).length===4);
 for(const action of ['responder','canalizar','escalar'])await change('/nucleo/solicitudes/'+solicitud+'/'+action,{},undefined,'POST',409);
 await page.getByRole('button',{name:'Crear tarea personal de la Solicitud',exact:true}).click();await page.getByLabel('Título de mi tarea',{exact:true}).fill('Tarea de Solicitud U5 '+stamp);await page.getByRole('button',{name:'Crear mi tarea de la Solicitud',exact:true}).click();await page.getByText('Tarea de Solicitud U5 '+stamp,{exact:true}).waitFor();
 await page.getByRole('link',{name:'Mis tareas',exact:true}).click();await page.getByRole('button',{name:'Crear tarea personal independiente',exact:true}).click();await page.getByLabel('Título de mi tarea',{exact:true}).fill('Tarea independiente U5 '+stamp);await page.getByLabel('Descripción privada',{exact:true}).fill('Información sintética no incluida en correos');await page.getByRole('button',{name:'Crear mi tarea independiente',exact:true}).click();await page.getByText('Tarea independiente U5 '+stamp,{exact:true}).waitFor();
 await page.getByLabel('Contexto de tareas',{exact:true}).selectOption('INDEPENDIENTE');ok('U5 filtro de contexto oculta tareas de Solicitud',await page.getByText('Tarea de Solicitud U5 '+stamp,{exact:true}).count()===0);
 const row=page.locator('article').filter({has:page.getByRole('heading',{name:'Tarea independiente U5 '+stamp,exact:true})});await row.getByRole('link',{name:'Consultar tarea',exact:true}).click();await page.getByRole('button',{name:'Completar mi tarea',exact:true}).waitFor();
 ok('U5 completar exige confirmación manual',await page.getByRole('button',{name:'Completar mi tarea',exact:true}).isDisabled());await page.getByRole('checkbox',{name:'Confirmo manualmente que completé esta tarea',exact:true}).check();await page.getByRole('button',{name:'Completar mi tarea',exact:true}).click();
 const independiente=page.url().split('/').pop();await page.getByRole('button',{name:'Completar mi tarea',exact:true}).waitFor({state:'detached'});ok('U5 independiente completada por UI',(await get('/nucleo/tareas/'+independiente)).estado==='COMPLETADA');
 const requestBody={revision:0,comunicacion:com.id,titulo:'Idempotencia sintética U5',descripcion:''},requestKey=crypto.randomUUID();const replayReq=await change('/nucleo/solicitudes',requestBody,requestKey);ok('U5 idempotencia Solicitud',(await change('/nucleo/solicitudes',requestBody,requestKey)).id===replayReq.id);
 const taskBody={revision:0,titulo:'Recordatorio independiente sintético U5',descripcion:'',prioridad:'NORMAL',recordatorio:new Date(Date.now()-2000).toISOString(),plazo:new Date(Date.now()-1000).toISOString(),tipoRecurso:null,recurso:null},taskKey=crypto.randomUUID();const replayTask=await change('/nucleo/tareas',taskBody,taskKey);ok('U5 idempotencia tarea',(await change('/nucleo/tareas',taskBody,taskKey)).id===replayTask.id);
 for(let i=0;i<4;i++)await change('/nucleo/notificaciones/operacion/procesar',{});
 const notices=await get('/nucleo/notificaciones?limite=50');ok('U5 aviso personal Solicitud',notices.items.some(n=>n.ruta==='solicitudes/'+solicitud));ok('U5 recordatorio y vencimiento',notices.items.filter(n=>n.ruta==='tareas/'+replayTask.id&&['TAREA_RECORDATORIO','TAREA_VENCIDA'].includes(n.tipo)).length===2);
 for(const n of notices.items.filter(n=>n.ruta==='tareas/'+replayTask.id&&['TAREA_RECORDATORIO','TAREA_VENCIDA'].includes(n.tipo))){const mail=await change('/nucleo/notificaciones/'+n.id+'/correo',{});ok('U5 correo solo simulado',mail.estado==='SIMULADO');}
 ok('U5 ingreso no alterado',(await get('/nucleo/comunicaciones/'+com.id)).comunicacion.requerimientoId===null);
 ok('U5 relaciones sin duplicación',(await get('/nucleo/solicitudes/'+solicitud+'/relaciones')).casos.length===0);
 for(const username of ['qa.windows.ti','qa.windows.otra']){
  const ctx=await browser.newContext({ignoreHTTPSErrors:false});try{
   const r=ctx.request,csrf=await (await r.get(run.origin+'/nucleo/csrf')).json();const login=await r.post(run.origin+'/nucleo/login',{headers:{[csrf.headerName]:csrf.token},form:{username,password:input.restricted[username]}});ok('U5 login restringido '+username,login.ok());
   for(const path of ['/nucleo/solicitudes/'+solicitud,'/nucleo/tareas/'+independiente,'/nucleo/solicitudes/'+solicitud+'/historial'])ok('U5 recurso ajeno denegado '+username,(await r.get(run.origin+path)).status()===404);
   ok('U5 avisos no filtrados a '+username,(await (await r.get(run.origin+'/nucleo/notificaciones?limite=50')).json()).items.every(n=>n.ruta!=='solicitudes/'+solicitud));
  }finally{await ctx.close();}
 }
 const staff=await get('/nucleo/ti/funcionarios'),other=staff.find(s=>s.usuario==='qa.windows.otra');
 const grant=await change('/nucleo/ti/funcionarios/'+other.id+'/roles',{rol:'COORDINADOR',alcance:'UNIDAD_SEDE',unidad:'UPD',sede:'DEMO_SEDE_A',desde:new Date(Date.now()-1000).toISOString(),hasta:null});let revoked=false;
 const scoped=await browser.newContext({ignoreHTTPSErrors:false});
 try{
  await change('/nucleo/comunicaciones/'+com.id+'/distribuir',{revision:0,destinos:[{unidad:'UPD',sede:'DEMO_SEDE_A',evaluador:other.id}]});
  const r=scoped.request;
  async function login(){const csrf=await(await r.get(run.origin+'/nucleo/csrf')).json();ok('U5 login con permisos actuales',(await r.post(run.origin+'/nucleo/login',{headers:{[csrf.headerName]:csrf.token},form:{username:'qa.windows.otra',password:input.restricted['qa.windows.otra']}})).ok());}
  await login();const csrf=await(await r.get(run.origin+'/nucleo/csrf')).json();const own=await r.post(run.origin+'/nucleo/solicitudes',{headers:{[csrf.headerName]:csrf.token,'Idempotency-Key':crypto.randomUUID()},data:{revision:0,comunicacion:com.id,titulo:'Preparación territorial sintética',descripcion:''}});ok('U5 preparación exige acceso vigente al ingreso',own.status()===200);const otherRequest=(await own.json()).id;
  await change('/nucleo/ti/funcionarios/'+other.id+'/roles/'+grant.id,undefined,undefined,'DELETE');revoked=true;
  ok('U5 revocar invalida sesión iniciada',(await r.get(run.origin+'/nucleo/solicitudes/'+otherRequest)).status()===401);await login();ok('U5 sesión nueva no conserva acceso al ingreso',(await r.get(run.origin+'/nucleo/solicitudes/'+otherRequest)).status()===404);
 }finally{await scoped.close();if(!revoked)await change('/nucleo/ti/funcionarios/'+other.id+'/roles/'+grant.id,undefined,undefined,'DELETE');}
 const saved={solicitud,independiente,communication:com.id,folio:detail.comunicacion.folio,requestBody,requestKey,taskBody,taskKey,replayTask:replayTask.id,requestReplay:replayReq.id,beforeTotal:before.total};
 fs.writeFileSync(savedFile,JSON.stringify(saved,null,2));
 await page.setViewportSize({width:390,height:844});await page.goto(run.origin+'/#solicitudes/'+solicitud);await page.getByRole('heading',{name:'Borrador de gestión',exact:true}).waitFor();ok('U5 vista móvil sin desborde',await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));await page.screenshot({path:input.run+'/u5-solicitud-movil.png',fullPage:true});
};
