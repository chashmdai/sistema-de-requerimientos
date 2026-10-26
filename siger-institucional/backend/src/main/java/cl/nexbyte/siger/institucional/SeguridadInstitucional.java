package cl.nexbyte.siger.institucional;

import cl.nexbyte.siger.institucional.nucleo.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.bind.annotation.*;

@Configuration
public class SeguridadInstitucional {
    @Bean UserDetailsService funcionarios(HabilitacionNucleo gate,Environment env,org.springframework.beans.factory.ObjectProvider<cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional> provider) {
        if(!gate.habilitado())return name->{throw new UsernameNotFoundException("Acceso no habilitado");};
        var identity=provider.getIfAvailable();
        if(identity!=null)return identity::autenticar;
        if(!env.getProperty("institucional.identidad.legado-pruebas",Boolean.class,false))return name->{throw new UsernameNotFoundException("Acceso cerrado");};
        var password=env.getRequiredProperty("institucional.nucleo.password-laboratorio");
        if(password.length()<32||password.length()>128)throw new IllegalStateException("Se requiere una credencial externa de laboratorio");
        var hash="{bcrypt}"+new BCryptPasswordEncoder().encode(password);
        var users=new HashMap<String,UserDetails>();
        for(var actor:CatalogoLaboratorio.OPERADORES)users.put(actor.id(),User.withUsername(actor.id()).password(hash).roles("FUNCIONARIO_LAB").build());
        // Authentication erases credentials on its principal; never return the shared template.
        return name->{var user=users.get(name);if(user==null)throw new UsernameNotFoundException("Acceso no habilitado");return User.withUserDetails(user).build();};
    }
    @Bean SecurityFilterChain institucional(HttpSecurity http,HabilitacionNucleo gate,Environment env,org.springframework.beans.factory.ObjectProvider<cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional> provider,org.springframework.beans.factory.ObjectProvider<java.time.Clock> clocks) throws Exception {
        if(!gate.habilitado())return http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c->c.disable())
            .authorizeHttpRequests(a->a.requestMatchers("/api/estado").permitAll().anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->r.setStatus(401))).build();
        var clock=clocks.getIfAvailable(java.time.Clock::systemUTC);
        var attempts=new HashMap<String,long[]>();
        http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation(f->f.changeSessionId()))
            .requestCache(c->c.disable())
            .authorizeHttpRequests(a->a.requestMatchers("/api/estado","/","/index.html","/assets/**","/nucleo/session","/nucleo/csrf","/nucleo/login").permitAll()
                .requestMatchers("/nucleo/**").hasRole("FUNCIONARIO_LAB").anyRequest().denyAll())
            .headers(h->{h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; font-src 'self'; connect-src 'self'; media-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"));
                // La respuesta puede comenzar en otro hilo: fijar cabeceras antes del streaming.
                h.addObjectPostProcessor(new org.springframework.security.config.ObjectPostProcessor<org.springframework.security.web.header.HeaderWriterFilter>(){
                    public <O extends org.springframework.security.web.header.HeaderWriterFilter> O postProcess(O filter){filter.setShouldWriteHeadersEagerly(true);return filter;}
                });
            })
            .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->r.setStatus(401)).accessDeniedHandler((q,r,x)->r.setStatus(403)))
            .formLogin(f->f.loginProcessingUrl("/nucleo/login").successHandler((q,r,a)->r.setStatus(204)).failureHandler((q,r,e)->r.setStatus(401)))
            .logout(l->l.logoutUrl("/nucleo/logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies(env.getProperty("server.servlet.session.cookie.name","SIGERSTAFFSESSION")).logoutSuccessHandler((q,r,a)->r.setStatus(204)))
            .addFilterBefore(new OncePerRequestFilter(){
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest q,jakarta.servlet.http.HttpServletResponse r,jakarta.servlet.FilterChain chain)throws java.io.IOException,jakarta.servlet.ServletException{
                    r.setHeader("Cache-Control","no-store");
                    if(!q.getRequestURI().equals("/api/estado")&&!q.isSecure()){r.setStatus(403);return;}
                    if(q.getRequestURI().equals("/nucleo/login")&&q.getMethod().equals("POST"))synchronized(attempts){
                        long minute=clock.millis()/60000;attempts.entrySet().removeIf(e->e.getValue()[0]!=minute);
                        if(attempts.size()>1024){r.setStatus(429);return;}
                        var count=attempts.computeIfAbsent(q.getRemoteAddr(),k->new long[]{minute,0});
                        var login=q.getParameter("username");if(login==null||login.length()>64){r.setStatus(401);return;}
                        var account=attempts.computeIfAbsent("cuenta:"+login.toLowerCase(Locale.ROOT),k->new long[]{minute,0});
                        if(++count[1]>30||++account[1]>8){r.setStatus(429);r.setHeader("Retry-After","60");return;}
                    }
                    chain.doFilter(q,r);
                }
            },UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new OncePerRequestFilter(){
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest q,jakarta.servlet.http.HttpServletResponse r,jakarta.servlet.FilterChain chain)throws java.io.IOException,jakarta.servlet.ServletException{
                var identity=provider.getIfAvailable();var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if(identity!=null&&auth!=null&&auth.isAuthenticated()){
                    if(!(auth.getPrincipal() instanceof cl.nexbyte.siger.institucional.identidad.IdentidadInstitucional.Principal p)||!identity.vigente(p)){
                        org.springframework.security.core.context.SecurityContextHolder.clearContext();var session=q.getSession(false);if(session!=null)session.invalidate();r.setStatus(401);r.setHeader("Cache-Control","no-store");return;
                    }
                }
                chain.doFilter(q,r);
            }
        },org.springframework.security.web.context.SecurityContextHolderFilter.class);
        return http.build();
    }
    @RestController static class EstadoController {
        record Estado(String aplicacion,String estado){}
        @GetMapping("/api/estado") Estado estado(){return new Estado("siger-institucional","ESTRUCTURA_INICIAL");}
    }
}
