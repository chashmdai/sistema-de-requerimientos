import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {afterEach,expect,test,vi} from 'vitest';
import {AutocierreRegional} from './Caso';
import * as api from './api';
vi.mock('./api',async()=>({...await vi.importActual<typeof api>('./api'),gestionGet:vi.fn()}));
afterEach(()=>{cleanup();vi.clearAllMocks();});
const c={id:'cas',revision:3,derivacionPendiente:false};
test('autocierre solo aparece con habilitación específica del backend',async()=>{vi.mocked(api.gestionGet).mockResolvedValue({permitido:true});const select=vi.fn();render(<AutocierreRegional c={c} busy={false} select={select}/>);fireEvent.click(await screen.findByText('Autocierre como titular regional'));expect(api.gestionGet).toHaveBeenCalledWith('/nucleo/casos/cas/autocierre-regional');expect(select).toHaveBeenCalledOnce();});
test('no presenta autocierre cuando el backend deniega o no acredita titularidad',async()=>{vi.mocked(api.gestionGet).mockResolvedValue({permitido:false});render(<AutocierreRegional c={c} busy={false} select={vi.fn()}/>);await waitFor(()=>expect(api.gestionGet).toHaveBeenCalledOnce());expect(screen.queryByText('Autocierre como titular regional')).toBeNull();});
