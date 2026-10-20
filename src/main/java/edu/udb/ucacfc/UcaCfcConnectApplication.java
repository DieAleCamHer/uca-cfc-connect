package edu.udb.ucacfc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada. @SpringBootApplication escanea edu.udb.ucacfc y todos sus subpaquetes.
 * La programacion de tareas (@EnableScheduling) y la auditoria JPA se activan en shared/config.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class UcaCfcConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(UcaCfcConnectApplication.class, args);
    }
}
