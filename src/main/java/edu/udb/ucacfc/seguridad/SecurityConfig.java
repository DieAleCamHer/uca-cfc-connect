package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.seguridad.jwt.JwtAuthenticationFilter;
import edu.udb.ucacfc.seguridad.jwt.RestAccessDeniedHandler;
import edu.udb.ucacfc.seguridad.jwt.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad en TRES capas:
 *  1) URL (aqui): que es publico y que exige token. Todo /api/** exige estar autenticado salvo lo listado.
 *  2) ROL (@PreAuthorize en cada metodo de controller, constantes en shared/security/Permisos).
 *  3) DUENO DEL REGISTRO (services, AccesoService): un CLIENTE solo ve y toca lo suyo.
 *
 * STATELESS + CSRF desactivado: no hay sesion ni cookies; la credencial es un header que solo nuestro JS agrega.
 * CORS: .cors(Customizer.withDefaults()) usa el bean corsConfigurationSource de CorsConfig. El frontend funciona
 * servido por este backend (mismo origen) o desde Live Server / otro puerto (origen distinto).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS (respaldo: el CorsFilter ya los responde antes de llegar aqui)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Autenticacion publica
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro", "/api/auth/refresh",
                                "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                        // Oferta academica publica (el cliente la consulta antes de registrarse)
                        .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Frontend estatico (mismo origen)
                        .requestMatchers(HttpMethod.GET, "/", "/*.html", "/css/**", "/js/**", "/img/**",
                                "/favicon.ico", "/error").permitAll()
                        // Todo lo demas de la API exige token; el ROL lo decide @PreAuthorize en cada metodo
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
