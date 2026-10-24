package cl.nexbyte.siger.institucional.nucleo;
import org.junit.jupiter.api.Test;import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
class HabilitacionNucleoTest {
    static MockEnvironment valid(){var e=new MockEnvironment().withProperty("institucional.nucleo.habilitado","true").withProperty("institucional.nucleo.entorno","LABORATORIO").withProperty("server.address","127.0.0.1").withProperty("datos.mysql-local","true").withProperty("server.servlet.session.cookie.secure","true").withProperty("institucional.db.url","jdbc:mysql://127.0.0.1:13306/robust_inst_test?sslMode=DISABLED");e.setActiveProfiles("nucleo-lab","jdbc-reception");return e;}
    @Test void deshabilitadoPorDefectoYHabilitableSoloLocal(){assertThat(new HabilitacionNucleo(new MockEnvironment()).habilitado()).isFalse();assertThat(new HabilitacionNucleo(valid()).habilitado()).isTrue();}
    @Test void admiteEsquemaAisladoDeIntegracion(){assertThat(new HabilitacionNucleo(valid().withProperty("institucional.db.url","jdbc:mysql://127.0.0.1:13306/robust_inst_formulario_lab?sslMode=DISABLED")).habilitado()).isTrue();}
    @Test void rechazaProduccionOtrosPerfilesCookiesOBaseRemota(){
        for(String profile:new String[]{"prod","production"}){var e=valid();e.addActiveProfile(profile);assertThatThrownBy(()->new HabilitacionNucleo(e)).isInstanceOf(IllegalStateException.class);}
        for(String[] p:new String[][]{{"server.address","0.0.0.0"},{"datos.mysql-local","false"},{"server.servlet.session.cookie.secure","false"},{"institucional.nucleo.entorno","PRODUCCION"},{"institucional.db.url","jdbc:mysql://servidor/produccion?sslMode=VERIFY_IDENTITY"}}){var e=valid().withProperty(p[0],p[1]);assertThatThrownBy(()->new HabilitacionNucleo(e)).isInstanceOf(IllegalStateException.class);}
        var e=valid();e.setActiveProfiles("jdbc-reception");assertThatThrownBy(()->new HabilitacionNucleo(e)).isInstanceOf(IllegalStateException.class);
    }
}
