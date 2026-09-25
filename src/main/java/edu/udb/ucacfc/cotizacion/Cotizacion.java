package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo simplificado: una cotizacion tiene un tipo, una descripcion
 * libre de lo solicitado (curso + espacio + catering, por ejemplo) y un
 * total. Si mas adelante el equipo necesita el detalle linea por linea
 * (ej. "3 coffee breaks + 1 auditorio"), se puede agregar una entidad
 * CotizacionItem con @OneToMany aqui, sin romper lo ya construido.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCotizacion tipo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;
}
