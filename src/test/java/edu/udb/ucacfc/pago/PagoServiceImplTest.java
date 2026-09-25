package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoServiceImpl pagoService;

    @Test
    void crear_lanzaOperacionInvalida_cuandoElMontoEsCeroONegativo() {
        assertThrows(OperacionInvalidaException.class,
                () -> pagoService.crear(TipoReferenciaPago.INSCRIPCION, 1L, BigDecimal.ZERO, MetodoPago.EFECTIVO));
    }

    @Test
    void crear_dejaElPagoEnEstadoPendiente_conMontoPagadoEnCero() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago pago = pagoService.crear(TipoReferenciaPago.INSCRIPCION, 1L, new BigDecimal("150.00"), MetodoPago.TARJETA);

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertEquals(0, pago.getMontoPagado().compareTo(BigDecimal.ZERO));
    }

    @Test
    void abonar_dejaElPagoEnParcial_cuandoElAbonoNoCubreElTotal() {
        Pago pago = Pago.builder()
                .id(1L).monto(new BigDecimal("100.00")).montoPagado(BigDecimal.ZERO)
                .estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = pagoService.abonar(1L, new BigDecimal("40.00"));

        assertEquals(EstadoPago.PARCIAL, resultado.getEstado());
        assertEquals(0, resultado.getMontoPagado().compareTo(new BigDecimal("40.00")));
    }

    @Test
    void abonar_dejaElPagoEnPagado_cuandoElAbonoCompletaElTotal() {
        Pago pago = Pago.builder()
                .id(1L).monto(new BigDecimal("100.00")).montoPagado(new BigDecimal("60.00"))
                .estado(EstadoPago.PARCIAL).build();
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = pagoService.abonar(1L, new BigDecimal("40.00"));

        assertEquals(EstadoPago.PAGADO, resultado.getEstado());
        assertEquals(0, resultado.getMontoPagado().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void abonar_lanzaOperacionInvalida_cuandoElAbonoExcedeElSaldoPendiente() {
        Pago pago = Pago.builder()
                .id(1L).monto(new BigDecimal("100.00")).montoPagado(new BigDecimal("80.00"))
                .estado(EstadoPago.PARCIAL).build();
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        assertThrows(OperacionInvalidaException.class, () -> pagoService.abonar(1L, new BigDecimal("30.00")));
    }
}
