package edu.udb.ucacfc.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj unico del sistema en la zona horaria de El Salvador. Los services usan LocalDateTime.now(clock):
 * en las pruebas se inyecta un Clock fijo y las reglas de "fecha en el pasado" se prueban sin depender del dia real.
 */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/El_Salvador}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
