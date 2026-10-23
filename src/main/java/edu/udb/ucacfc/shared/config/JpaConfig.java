package edu.udb.ucacfc.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa fecha_creacion / fecha_actualizacion automaticas (Auditable) y las tareas @Scheduled (cierre automatico). */
@Configuration
@EnableJpaAuditing
@EnableScheduling
public class JpaConfig {
}
