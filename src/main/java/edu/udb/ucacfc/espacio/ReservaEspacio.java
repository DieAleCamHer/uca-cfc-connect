package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Esta es LA entidad clave para las pruebas unitarias que pide el
 * lineamiento (EspacioOcupadoException). El Service debe consultar,
 * antes de guardar, si existe otra reserva del mismo espacio cuyo
 * rango [fechaHoraInicio, fechaHoraFin] se solape con el nuevo rango.
 */
@Entity
@Table(name = "reservas_espacio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaEspacio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(length = 250)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;
}
