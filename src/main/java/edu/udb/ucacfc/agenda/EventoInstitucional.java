package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Evento propio del CFC (conferencia, graduacion, feria...) que programa la recepcionista
 * (lineamiento modulo 7: el calendario muestra tambien "Eventos").
 */
@Entity
@Table(name = "eventos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoInstitucional extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(name = "lugar_externo", length = 200)
    private String lugarExterno;

    @Column(name = "asistentes_estimados")
    private Integer asistentesEstimados;

    @Column(nullable = false, length = 120)
    private String responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoEvento estado;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Version
    private Long version;
}
