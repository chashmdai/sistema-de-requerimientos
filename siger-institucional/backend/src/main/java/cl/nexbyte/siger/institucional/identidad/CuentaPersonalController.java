package cl.nexbyte.siger.institucional.identidad;

import cl.nexbyte.siger.institucional.robustez.FalloTecnico;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/nucleo")
@ConditionalOnProperty(name="institucional.identidad.habilitada",havingValue="true")
public class CuentaPersonalController {
    private final IdentidadInstitucional identidad;
    public CuentaPersonalController(IdentidadInstitucional identidad){this.identidad=identidad;}

    @PostMapping("/clave-personal") @ResponseStatus(HttpStatus.NO_CONTENT)
    void cambiar(Authentication auth,@RequestBody IdentidadInstitucional.CambioClave cambio,HttpServletRequest request){
        if(auth==null||!auth.isAuthenticated()||!(auth.getPrincipal() instanceof IdentidadInstitucional.Principal principal))throw new FalloTecnico(401);
        identidad.cambiarClavePersonal(principal,cambio);
        // Ninguna sesión conserva la credencial anterior. La siguiente autenticación usa el epoch nuevo.
        SecurityContextHolder.clearContext();var session=request.getSession(false);if(session!=null)session.invalidate();
    }
}
