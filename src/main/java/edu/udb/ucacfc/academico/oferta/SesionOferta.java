package edu.udb.ucacfc.academico.oferta;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un bloque continuo en que se imparte la oferta (ej. lunes 18:00-20:00). Espacio, docente y agenda se ocupan
 * SOLO durante las sesiones, no "de corrido" entre la primera y la ultima fecha.
 * Una sesion que pasa la medianoche es simplemente una cuyo fin cae al dia siguiente.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SesionOferta {
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;
}
