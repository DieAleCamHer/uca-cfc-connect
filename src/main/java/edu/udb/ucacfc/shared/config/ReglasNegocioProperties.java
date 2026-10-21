package edu.udb.ucacfc.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas configurables desde application.properties (prefijo app.reglas).
 * Se inyectan en los services para no tener "numeros magicos" en el codigo.
 */
@ConfigurationProperties(prefix = "app.reglas")
public record ReglasNegocioProperties(
        int cateringAnticipacionHoras,
        int cancelacionClienteHoras,
        int cotizacionVigenciaDias) {
}
