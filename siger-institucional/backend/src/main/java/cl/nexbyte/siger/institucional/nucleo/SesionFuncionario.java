package cl.nexbyte.siger.institucional.nucleo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
public final class SesionFuncionario {
    private SesionFuncionario(){}
    public static CatalogoLaboratorio.Operador actual(Authentication auth,CatalogoLaboratorio catalogo){
        if(auth==null||!auth.isAuthenticated()||!(auth.getPrincipal() instanceof UserDetails)
            ||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_FUNCIONARIO_LAB")))throw new FalloTecnico(401);
        return catalogo.operador(auth.getName());
    }
}
