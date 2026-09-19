package edu.udb.ucacfc.shared;

import edu.udb.ucacfc.seguridad.JwtAccessDeniedHandler;
import edu.udb.ucacfc.seguridad.JwtAuthenticationEntryPoint;
import edu.udb.ucacfc.seguridad.JwtAuthenticationFilter;
import edu.udb.ucacfc.seguridad.UsuarioDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion REAL de seguridad (Bloque 6). Reemplaza por completo la
 * version temporal permitAll del Bloque 4.
 *
 * Arquitectura: cliente manda "Authorization: Bearer <token>" en cada
 * request -> JwtAuthenticationFilter valida el token y autentica al
 * usuario -> aqui abajo se decide, segun el rol de ese usuario, si puede
 * o no acceder a la ruta pedida.
 *
 * Roles del sistema (segun el lineamiento del proyecto):
 * ADMIN, RECEPCIONISTA, CLIENTE, CONTABILIDAD.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize por si algun Controller lo necesita mas adelante
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsServiceImpl usuarioDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           UsuarioDetailsServiceImpl usuarioDetailsService,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           JwtAccessDeniedHandler jwtAccessDeniedHandler,
                           PasswordEncoder passwordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // API REST sin sesiones: cada request se autentica solo con su JWT.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler))           // 403
                .authorizeHttpRequests(auth -> auth
                        // Publico: login, auto-registro de clientes, documentacion.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Publico: frontend estatico (HTML/CSS/JS). El login vive AQUI,
                        // asi que tiene que cargar sin token todavia.
                        .requestMatchers("/", "/index.html", "/app.html", "/css/**", "/js/**", "/favicon.ico").permitAll()

                        // Gestion de cuentas de personal: solo ADMIN.
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // Catalogos academicos: cualquiera autenticado puede LEER,
                        // pero solo ADMIN/RECEPCIONISTA pueden crear/editar, y solo
                        // ADMIN puede borrar.
                        .requestMatchers(HttpMethod.GET,
                                "/api/categorias/**", "/api/modalidades/**", "/api/docentes/**",
                                "/api/cursos/**", "/api/diplomados/**", "/api/espacios/**",
                                "/api/servicios-catering/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/categorias/**", "/api/modalidades/**",
                                "/api/docentes/**", "/api/cursos/**", "/api/diplomados/**",
                                "/api/espacios/**", "/api/servicios-catering/**")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/categorias/**", "/api/modalidades/**",
                                "/api/docentes/**", "/api/cursos/**", "/api/diplomados/**",
                                "/api/espacios/**", "/api/servicios-catering/**")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/categorias/**", "/api/modalidades/**",
                                "/api/docentes/**", "/api/cursos/**", "/api/diplomados/**",
                                "/api/espacios/**", "/api/servicios-catering/**")
                        .hasRole("ADMIN")

                        // Operacion diaria (clientes, inscripciones, reservas, cotizaciones,
                        // solicitudes de catering): la usan ADMIN, RECEPCIONISTA y tambien
                        // el propio CLIENTE (para sus propias solicitudes).
                        .requestMatchers("/api/clientes/**", "/api/inscripciones/**",
                                "/api/reservas-espacio/**", "/api/solicitudes-catering/**",
                                "/api/cotizaciones/**")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")

                        // Agenda institucional: solo lectura, cualquiera autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/agenda/**").authenticated()

                        // Pagos: ADMIN y CONTABILIDAD (es su modulo).
                        // Pagos: registrar/abonar lo hacen ADMIN, RECEPCIONISTA (quien
                        // atiende al cliente en caja) y CONTABILIDAD. La lectura tambien
                        // se habilita para CLIENTE, ya que el propio cliente debe poder
                        // consultar el estado de sus pagos (lo pide el documento de diseño).
                        // Nota de diseno: la consulta por referencia (/api/pagos/referencia)
                        // exige indicar el id exacto de la inscripcion/cotizacion/etc, que un
                        // cliente solo conoce para sus propios registros a traves del resto de
                        // la app -- no hay todavia un filtro server-side "solo lo mio" mas
                        // estricto; queda documentado como mejora futura.
                        .requestMatchers(HttpMethod.GET, "/api/pagos/**").hasAnyRole("ADMIN", "RECEPCIONISTA", "CONTABILIDAD", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pagos/**").hasAnyRole("ADMIN", "RECEPCIONISTA", "CONTABILIDAD")
                        .requestMatchers(HttpMethod.PATCH, "/api/pagos/**").hasAnyRole("ADMIN", "RECEPCIONISTA", "CONTABILIDAD")

                        // Cualquier otra ruta no listada arriba: exige estar autenticado.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
