package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla central de agenda: cada vez que se crea/confirma un Curso,
 * Diplomado, Evento, ReservaEspacio o SolicitudCatering que ocupa un
 * espacio fisico, se registra (o se sincroniza) tambien aqui una
 * ActividadAgenda. Con esto el Service de Agenda/Espacios puede validar
 * cruces de horario consultando UNA sola tabla, sin tener que revisar
 * 5 tablas distintas cada vez.
 *
 * referenciaId + tipo apuntan al registro original (ej: tipo=ALQUILER,
 * referenciaId = id de la ReservaEspacio que la origino).
 */
@Entity
@Table(name = "actividades_agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadAgenda extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoActividad tipo;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    // Nullable porque no todas las actividades usan un espacio fisico
    // (ej. un curso 100% virtual).
    @ManyToOne
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;
}
