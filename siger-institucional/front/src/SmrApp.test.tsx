import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {afterEach,describe,expect,it,vi} from 'vitest';
import SmrApp from './SmrApp';
const session={autenticado:true,operador:{id:'prueba',nombre:'Operador sintético'},modo:'SMR',lectura:true,operacion:false,sintetico:true};
afterEach(()=>{cleanup();vi.unstubAllGlobals();});
describe('SMR recepción',()=>{
 it('un error de consulta no se presenta como bandeja vacía',async()=>{
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>new Response(JSON.stringify(url==='/nucleo/session'?session:{}),{status:url==='/nucleo/session'?200:503})));
  render(<SmrApp/>);expect((await screen.findByRole('alert')).textContent).toContain('HTTP 503');expect(screen.queryByText('No hay registros que coincidan con los filtros.')).toBeNull();
 });
 it('permite buscar y paginar registros durables sin controles de negocio',async()=>{
  const fetcher=vi.fn(async(url:string)=>new Response(JSON.stringify(url==='/nucleo/session'?session:{items:[],total:0,pagina:0,limite:20}),{status:200}));vi.stubGlobal('fetch',fetcher);render(<SmrApp/>);
  expect(await screen.findByText('0 registros en la custodia SMR.')).toBeTruthy();fireEvent.change(screen.getByLabelText('Folio'),{target:{value:'COM-000001'}});fireEvent.click(screen.getByRole('button',{name:'Buscar'}));
  await waitFor(()=>expect(fetcher.mock.calls.some(c=>c[0].includes('q=COM-000001'))).toBe(true));expect(screen.queryByText('Registrar Comunicación manual')).toBeNull();expect(screen.queryByText('Casos')).toBeNull();
 });
 it('distingue contadores de proceso, conciliación y ACK durables',async()=>{
  const state={transporteConfigurado:true,consumidor:{estado:'REINTENTO_PROGRAMADO',ciclos:3,recuperadas:2,confirmadas:1,conciliadas:1,ultimoError:'HTTP_401'},control:{revision:2,recuperar:true,contingencia:'NORMAL'},registrosDurables:2,ackPendientes:0,ackBloqueados:0,origen:'https://localhost:19443'};
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>new Response(JSON.stringify(url==='/nucleo/session'?session:url.includes('/estado')?state:{items:[],total:0,pagina:0,limite:20}),{status:200})));render(<SmrApp/>);fireEvent.click(await screen.findByRole('link',{name:'Estado del consumidor'}));
  expect(await screen.findByText('Último error: HTTP_401')).toBeTruthy();expect(screen.getByText(/Conciliadas: 1/)).toBeTruthy();expect(screen.getByText(/Comunicaciones: 2 · ACK pendientes: 0/)).toBeTruthy();expect(screen.queryByRole('button',{name:'Iniciar consumidor'})).toBeNull();
 });
});
