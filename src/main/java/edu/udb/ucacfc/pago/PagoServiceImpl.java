package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;

    public PagoServiceImpl(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Override
    @Transactional
    public Pago crear(TipoReferenciaPago tipoReferencia, Long referenciaId, BigDecimal monto, MetodoPago metodo) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException("El monto a pagar debe ser mayor a cero");
        }
        Pago pago = Pago.builder()
                .tipoReferencia(tipoReferencia)
                .referenciaId(referenciaId)
                .monto(monto)
                .montoPagado(BigDecimal.ZERO)
                .metodo(metodo)
                .estado(EstadoPago.PENDIENTE)
                .build();
        return pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public Pago abonar(Long pagoId, BigDecimal montoAbonado) {
        if (montoAbonado == null || montoAbonado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException("El monto abonado debe ser mayor a cero");
        }
        Pago pago = buscarPorId(pagoId);

        BigDecimal nuevoTotalPagado = pago.getMontoPagado().add(montoAbonado);
        if (nuevoTotalPagado.compareTo(pago.getMonto()) > 0) {
            throw new OperacionInvalidaException(
                    "El abono excede el saldo pendiente. Saldo actual: "
                            + pago.getMonto().subtract(pago.getMontoPagado()));
        }

        pago.setMontoPagado(nuevoTotalPagado);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstado(calcularEstado(pago.getMonto(), nuevoTotalPagado));

        return pagoRepository.save(pago);
    }

    @Override
    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Pago", id));
    }

    @Override
    public List<Pago> listarPorReferencia(TipoReferenciaPago tipoReferencia, Long referenciaId) {
        return pagoRepository.findByTipoReferenciaAndReferenciaId(tipoReferencia, referenciaId);
    }

    @Override
    public List<Pago> listarPorEstado(EstadoPago estado) {
        return pagoRepository.findByEstado(estado);
    }

    private EstadoPago calcularEstado(BigDecimal monto, BigDecimal montoPagado) {
        if (montoPagado.compareTo(BigDecimal.ZERO) == 0) {
            return EstadoPago.PENDIENTE;
        }
        if (montoPagado.compareTo(monto) >= 0) {
            return EstadoPago.PAGADO;
        }
        return EstadoPago.PARCIAL;
    }
}
