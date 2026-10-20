package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.seguridad.jwt.JwtAuthenticationFilter;
import edu.udb.ucacfc.seguridad.usuario.UsuarioRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Piezas de autenticacion separadas de SecurityConfig (evita dependencias circulares).
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** "Quien es este username": aqui el username es el correo. */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository repo) {
        return email -> repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta " + email));
    }

    /**
     * DaoAuthenticationProvider: busca al usuario y compara la contrasena con BCrypt.
     * Se mueve la verificacion de "cuenta desactivada" a DESPUES de comprobar la contrasena: asi alguien que no
     * conoce la contrasena no puede averiguar que una cuenta existe y esta desactivada.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setPasswordEncoder(encoder);
        p.setUserDetailsService(uds);
        p.setPreAuthenticationChecks(user -> {
        });
        p.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("Cuenta desactivada");
            }
        });
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /** El filtro es @Component: sin esto Spring Boot lo registraria ademas como filtro global del servlet. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> desactivarRegistroGlobal(JwtAuthenticationFilter f) {
        FilterRegistrationBean<JwtAuthenticationFilter> r = new FilterRegistrationBean<>(f);
        r.setEnabled(false);
        return r;
    }
}
