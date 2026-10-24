package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.seguridad.usuario.Rol;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un pago parcial o total de una cuenta. Al validarse recibe numero de comprobante. */
@Entity
@Table(name = "abonos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Abono extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPago metodo;

    /** Numero de transferencia, boleta de deposito o voucher de tarjeta. */
    @Column(name = "referencia_externa", length = 60)
    private String referenciaExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoAbono estado;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_registro", nullable = false, length = 20)
    private Rol rolRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "validado_por", length = 120)
    private String validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(length = 300)
    private String motivo;

    @Column(name = "numero_comprobante", unique = true, length = 30)
    private String numeroComprobante;
}
