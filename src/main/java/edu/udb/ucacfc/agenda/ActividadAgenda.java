package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una entrada del calendario institucional. La escriben SOLO los services de cada modulo (curso: una por sesion,
 * diplomado, evento, reserva, catering). referenciaId apunta al registro de origen segun tipo (referencia "blanda",
 * sin FK, para que una sola tabla sirva a los cinco modulos).
 *
 * bloqueaEspacio: false solo para un catering servido DENTRO de una reserva del mismo cliente (el espacio ya lo bloquea
 * la reserva; si el catering tambien bloqueara, chocaria con la propia reserva).
 * clienteId: dueno (reservas y catering) para que un CLIENTE vea solo sus actividades privadas.
 */
@Entity
@Table(name = "actividades_agenda", indexes = {
        @Index(name = "idx_agenda_rango", columnList = "inicio,fin"),
        @Index(name = "idx_agenda_ref", columnList = "tipo,referencia_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadAgenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoActividad tipo;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(name = "bloquea_espacio", nullable = false)
    private boolean bloqueaEspacio;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(length = 300)
    private String detalle;
}
