package edu.udb.ucacfc.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/** Utilidades de rangos de tiempo usadas por agenda, reservas, catering y ofertas. */
public final class Horarios {
    private Horarios() {
    }

    /**
     * Dos rangos [a1, a2) y [b1, b2) se cruzan si a1 < b2 y a2 > b1.
     * Terminar exactamente cuando empieza el otro NO es cruce (08:00-10:00 y 10:00-12:00 son compatibles).
     */
    public static boolean seCruzan(LocalDateTime a1, LocalDateTime a2, LocalDateTime b1, LocalDateTime b2) {
        return a1.isBefore(b2) && a2.isAfter(b1);
    }

    /** Horas entre dos instantes con 2 decimales (90 min = 1.50). */
    public static BigDecimal horas(LocalDateTime inicio, LocalDateTime fin) {
        long minutos = Duration.between(inicio, fin).toMinutes();
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
