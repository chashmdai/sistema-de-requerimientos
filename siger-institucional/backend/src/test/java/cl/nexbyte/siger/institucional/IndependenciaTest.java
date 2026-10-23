package cl.nexbyte.siger.institucional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print=org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class IndependenciaTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Test void arrancaSinFormularioNiClaveUnicaYNoExponeSesionesCiudadanas() throws Exception {
        mvc.perform(get("/api/estado")).andExpect(status().isOk()).andExpect(jsonPath("$.aplicacion").value("siger-institucional"));
        mvc.perform(get("/api/session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/oauth2/authorization/claveunica")).andExpect(status().isUnauthorized());
        assertThat(context.getBeansOfType(cl.nexbyte.siger.institucional.integracion.FuenteComunicaciones.class)).isEmpty();
        assertThat(context.getBeansOfType(cl.nexbyte.siger.institucional.integracion.RecepcionInstitucional.class)).isEmpty();
    }
}
