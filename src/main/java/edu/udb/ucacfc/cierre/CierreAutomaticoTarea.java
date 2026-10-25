package edu.udb.ucacfc.cierre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el barrido periodicamente. No hay peticion HTTP: un error se registra y se reintenta en el siguiente ciclo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreAutomaticoTarea {

    private final CierreAutomaticoService service;

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${app.cierre.intervalo-ms:900000}")
    public void ejecutar() {
        try {
            service.barrido();
        } catch (RuntimeException e) {
            log.warn("El cierre automatico fallo y se reintentara: {}", e.getMessage());
        }
    }
}
