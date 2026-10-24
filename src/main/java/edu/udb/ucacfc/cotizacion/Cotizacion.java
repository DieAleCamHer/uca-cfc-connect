package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Oferta de precio. NO reserva, NO agenda y NO inscribe nada: si el cliente acepta, cada servicio se registra en su
 * modulo. Lineas (items) con referencias al catalogo -> el combinado se modela naturalmente con varias lineas.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cotizacion extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCotizacion tipo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "fecha_evento")
    private LocalDate fechaEvento;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Builder.Default
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ItemCotizacion> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCotizacion estado;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @Column(name = "elaborada_por", length = 120)
    private String elaboradaPor;

    @Column(name = "aprobada_por", length = 120)
    private String aprobadaPor;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @Version
    private Long version;

    public void reemplazarItems(List<ItemCotizacion> nuevos) {
        items.clear();
        nuevos.forEach(i -> {
            i.setCotizacion(this);
            items.add(i);
        });
    }
}
