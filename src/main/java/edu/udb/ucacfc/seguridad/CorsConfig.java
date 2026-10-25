package edu.udb.ucacfc.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS: permite que un frontend servido desde OTRO origen (ej. Live Server en http://localhost:5500) llame a la API
 * en http://localhost:8080. Lo aplica el NAVEGADOR (Postman, curl y Swagger no lo sufren).
 *
 * - El NOMBRE del metodo importa: .cors(Customizer.withDefaults()) en SecurityConfig busca un bean llamado
 *   "corsConfigurationSource" y coloca el CorsFilter AL PRINCIPIO de la cadena de seguridad. Asi:
 *     * los preflight (OPTIONS) se responden antes del filtro JWT (el navegador no manda token en el preflight);
 *     * las respuestas 401/403 tambien llevan cabeceras CORS y el frontend puede leer el JSON de error.
 * - allowCredentials(false): el token viaja en el header Authorization, no en cookies; por eso se puede usar "*".
 * - Origenes configurables en application.properties (app.cors.allowed-origins), separados por comas.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origenes = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOriginPatterns(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
