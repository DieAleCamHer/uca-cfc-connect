package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.shared.entity.Auditable;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CUENTA POR COBRAR de un proceso. La crea el sistema automaticamente cuando el proceso se vuelve cobrable
 * (inscripcion confirmada, reserva confirmada, catering confirmado, cotizacion aprobada). Nadie la crea ni
 * edita a mano: el dinero entra por abonos. tipoReferencia + referenciaId apuntan al proceso (referencia blanda,
 * una sola tabla para los cuatro modulos); cliente se guarda para que un CLIENTE vea solo sus cuentas.
 */
@Entity
@Table(name = "pagos", indexes = @Index(name = "idx_pago_ref", columnList = "tipo_referencia,referencia_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_referencia", nullable = false, length = 15)
    private TipoReferenciaPago tipoReferencia;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 250)
    private String concepto;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    /** Suma de abonos VALIDADOS. */
    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPago estado;

    @Column(name = "fecha_pago_completo")
    private LocalDateTime fechaPagoCompleto;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Version
    private Long version;

    public BigDecimal saldo() {
        return montoTotal.subtract(montoPagado);
    }

    /** PENDIENTE / PARCIAL / PAGADO segun lo validado (no toca ANULADO). */
    public void recalcularEstado(LocalDateTime ahora) {
        if (estado == EstadoPago.ANULADO) {
            return;
        }
        if (montoPagado.compareTo(montoTotal) >= 0) {
            estado = EstadoPago.PAGADO;
            if (fechaPagoCompleto == null) {
                fechaPagoCompleto = ahora;
            }
        } else {
            estado = montoPagado.signum() > 0 ? EstadoPago.PARCIAL : EstadoPago.PENDIENTE;
            fechaPagoCompleto = null;
        }
    }
}
