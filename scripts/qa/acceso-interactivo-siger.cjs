// Smoke de lectura para fase A. Nunca envía Comunicaciones, distribuye ni recupera entregas.
const fs=require('node:fs');
async function main(){
 const input=JSON.parse(fs.readFileSync(0,'utf8'));const run=JSON.parse(fs.readFileSync(input.run+'/run.json','utf8'));
 if(run.database!=='siger_institucional_windows_qa'||!/^https:\/\/localhost:808[01]$/.test(run.origin)||run.automaticPublicRecovery||run.realGraph)throw Error('GUARD');
 const {chromium}=require(input.playwright);const browser=await chromium.launch({channel:'msedge',headless:true});const checks=[];
 const check=(name,ok)=>{if(!ok){const e=Error('READ_ONLY_QA_FAILED');e.check=name;throw e;}checks.push(name);};
 try{
  const context=await browser.newContext({ignoreHTTPSErrors:false});await context.route('**/*',r=>new URL(r.request().url()).origin===run.origin?r.continue():r.abort());const page=await context.newPage();
  await page.goto(run.origin);await page.getByLabel('Usuario individual',{exact:true}).fill('nexbyte');await page.getByLabel('Contraseña de laboratorio',{exact:true}).fill(input.password);await page.getByRole('button',{name:'Ingresar al laboratorio',exact:true}).click();await page.getByRole('button',{name:'Cerrar sesión',exact:true}).waitFor();input.password=null;check('LOGIN_INDIVIDUAL_HTTPS',true);
  const response=await context.request.get(run.origin+'/nucleo/session');check('SESION_HTTP_200',response.ok());const session=await response.json();check('CUENTA_ACTIVA',session.autenticado===true);check('DISTRIBUCION_VIGENTE',session.operador.accesos.some(a=>a.rol==='DISTRIBUCION'&&a.alcance==='INSTITUCION'));
  await page.getByRole('link',{name:'Comunicaciones',exact:true}).click();await page.getByRole('heading',{name:'Comunicaciones',exact:true}).waitFor();await page.getByRole('searchbox',{name:'Buscar folio en tu alcance'}).fill('COM-002497');await page.getByRole('button',{name:'Buscar',exact:true}).click();await page.getByRole('button',{name:'Abrir COM-002497',exact:true}).waitFor();check('BUZON_PUBLICO_INTEGRADO',true);
  const detailResponse=await context.request.get(run.origin+'/nucleo/comunicaciones/893a552c-67cd-4f91-9495-1af5f12dae8f');check('DETALLE_AUTORIZADO',detailResponse.ok());const detail=await detailResponse.json();check('COM_ANTERIOR_PRESERVADA',detail.comunicacion.folio==='COM-002497'&&detail.contenido.formularioCompleto?.id==='8c728e99-fa44-40dd-8779-8fb2a877a933');
  await page.getByRole('button',{name:'Abrir COM-002497',exact:true}).click();await page.getByRole('heading',{name:/COM-002497/}).first().waitFor();check('DETALLE_UI',await page.getByRole('region',{name:'Formulario ciudadano completo'}).count()===1);check('FACULTAD_VISIBLE_SIN_EJECUTAR',await page.getByRole('button',{name:'Distribuir Comunicación',exact:true}).count()===1);
  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.getByLabel('Usuario individual',{exact:true}).waitFor();await context.close();
  const result={scope:'FASE_A_SOLO_LECTURA_SIGER_EDGE_HTTPS',user:'nexbyte',actor:session.operador.id,accesses:session.operador.accesos,checks,usernameVerified:true,passwordVerified:true,passwordChanged:false,previousFolio:'COM-002497',previousCommunicationVisible:true,inboxUrl:run.origin+'/#comunicaciones',detailUrl:run.origin+'/#comunicacion/893a552c-67cd-4f91-9495-1af5f12dae8f',createdCommunications:0,publicPulls:0,distributionCommands:0};
  fs.writeFileSync(input.output,JSON.stringify(result,null,2),{flag:'wx'});console.log(JSON.stringify(result));
 }finally{await browser.close();}
}
main().catch(e=>{console.error(JSON.stringify({ok:false,scope:'FASE_A_SOLO_LECTURA',code:e.check||'ACCESO_NO_ACREDITADO'}));process.exitCode=1;});
