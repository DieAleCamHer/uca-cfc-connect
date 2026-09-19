package edu.udb.ucacfc.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Se define aparte (y no dentro de la configuracion completa de Spring
 * Security del Bloque 6) para poder usar el hash de contrasenas desde
 * YA en UsuarioService, sin tener que esperar a tener JWT armado.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
