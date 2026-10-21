package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacio;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pedido de catering (lineamiento: asistentes, menu, fecha, hora y lugar). Tres formas de indicar el lugar:
 *  1) reserva: se sirve dentro de un alquiler del MISMO cliente -> no bloquea de nuevo el espacio.
 *  2) espacio: un espacio del CFC libre en ese horario -> lo bloquea en la agenda.
 *  3) solo "lugar" (texto): lugar externo, no toca la agenda de espacios.
 * precioUnitario y total se congelan al crear (el catalogo puede cambiar de precio despues).
 */
@Entity
@Table(name = "solicitudes_catering")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCatering extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioCatering servicio;

    @Column(name = "numero_asistentes", nullable = false)
    private Integer numeroAsistentes;

    @Column(length = 500)
    private String menu;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false, length = 200)
    private String lugar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private ReservaEspacio reserva;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitudCatering estado;

    @Column(name = "motivo_estado", length = 300)
    private String motivoEstado;

    @Version
    private Long version;
}
