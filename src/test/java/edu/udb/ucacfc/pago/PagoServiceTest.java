package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.TestData;
import edu.udb.ucacfc.cierre.CierreAutomaticoService;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.pago.dto.AbonoRequest;
import edu.udb.ucacfc.pago.dto.AbonoResponse;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.security.AccesoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceTest {

    @Mock
    PagoRepository pagoRepository;
    @Mock
    AbonoRepository abonoRepository;
    @Mock
    CierreAutomaticoService cierre;
    @Mock
    AccesoService acceso;

    PagoService service;
    Pago cuenta;

    @BeforeEach
    void setUp() {
        service = new PagoService(pagoRepository, abonoRepository, cierre, acceso, TestData.CLOCK);
        Cliente c = Cliente.builder().id(1L).tipo(TipoCliente.PERSONA).nombre("Ana").duiNit("01234567-8").build();
        cuenta = Pago.builder().id(3L).tipoReferencia(TipoReferenciaPago.INSCRIPCION).referenciaId(5L).cliente(c)
                .concepto("Inscripcion #5").montoTotal(new BigDecimal("100.00")).montoPagado(BigDecimal.ZERO)
                .estado(EstadoPago.PENDIENTE).build();
        when(pagoRepository.bloquear(3L)).thenReturn(Optional.of(cuenta));
        when(abonoRepository.sumarPorEstado(anyLong(), eq(EstadoAbono.PENDIENTE_VALIDACION))).thenReturn(BigDecimal.ZERO);
        when(abonoRepository.save(any(Abono.class))).thenAnswer(inv -> {
            Abono a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(11L);
            }
            return a;
        });
    }

    @Test
    @DisplayName("Nunca se cobra de mas: abono mayor al saldo -> 400")
    void abonoExcedeSaldo() {
        assertThatThrownBy(() -> service.registrarAbono(3L, new AbonoRequest(new BigDecimal("150.00"), MetodoPago.EFECTIVO, null),
                TestData.usuario(2, Rol.RECEPCIONISTA))).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("excede");
    }

    @Test
    @DisplayName("Un CLIENTE no puede registrar efectivo (se paga en caja)")
    void clienteNoRegistraEfectivo() {
        assertThatThrownBy(() -> service.registrarAbono(3L, new AbonoRequest(BigDecimal.TEN, MetodoPago.EFECTIVO, null),
                TestData.usuario(3, Rol.CLIENTE))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Transferencia sin referencia -> 400")
    void transferenciaSinReferencia() {
        assertThatThrownBy(() -> service.registrarAbono(3L, new AbonoRequest(BigDecimal.TEN, MetodoPago.TRANSFERENCIA, null),
                TestData.usuario(2, Rol.RECEPCIONISTA))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Recepcion cobra en efectivo el total: abono VALIDADO, cuenta PAGADA, comprobante y cierre automatico")
    void efectivoSaldaLaCuenta() {
        AbonoResponse r = service.registrarAbono(3L, new AbonoRequest(new BigDecimal("100.00"), MetodoPago.EFECTIVO, null),
                TestData.usuario(2, Rol.RECEPCIONISTA));

        assertThat(r.estado()).isEqualTo(EstadoAbono.VALIDADO);
        assertThat(r.numeroComprobante()).isEqualTo("CFC-2026-000011");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoPago.PAGADO);
        verify(cierre).alSaldarse(TipoReferenciaPago.INSCRIPCION, 5L);
    }

    @Test
    @DisplayName("Abono parcial -> PARCIAL y no intenta cerrar el proceso")
    void abonoParcial() {
        service.registrarAbono(3L, new AbonoRequest(new BigDecimal("40.00"), MetodoPago.TARJETA, "VOUCHER-1"),
                TestData.usuario(2, Rol.RECEPCIONISTA));
        assertThat(cuenta.getEstado()).isEqualTo(EstadoPago.PARCIAL);
        assertThat(cuenta.saldo()).isEqualByComparingTo("60.00");
        verify(cierre, never()).alSaldarse(any(), anyLong());
    }

    @Test
    @DisplayName("Transferencia reportada por el CLIENTE queda PENDIENTE_VALIDACION y no suma todavia")
    void transferenciaDelClientePendiente() {
        AbonoResponse r = service.registrarAbono(3L, new AbonoRequest(new BigDecimal("100.00"), MetodoPago.TRANSFERENCIA,
                "TRX-1"), TestData.usuario(3, Rol.CLIENTE));
        assertThat(r.estado()).isEqualTo(EstadoAbono.PENDIENTE_VALIDACION);
        assertThat(cuenta.getMontoPagado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("No hay reembolsos: con dinero cobrado no se puede cancelar el proceso")
    void verificarSinDinero() {
        cuenta.setMontoPagado(new BigDecimal("20.00"));
        when(pagoRepository.findFirstByTipoReferenciaAndReferenciaIdAndEstadoNot(TipoReferenciaPago.INSCRIPCION, 5L,
                EstadoPago.ANULADO)).thenReturn(Optional.of(cuenta));
        assertThatThrownBy(() -> service.verificarSinDinero(TipoReferenciaPago.INSCRIPCION, 5L))
                .isInstanceOf(ConflictoEstadoException.class).hasMessageContaining("reembolsos");
    }

    @Test
    @DisplayName("Cuenta de monto 0 nace PAGADA")
    void montoCeroNacePagada() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.findFirstByTipoReferenciaAndReferenciaIdAndEstadoNot(any(), anyLong(), any()))
                .thenReturn(Optional.empty());
        Pago p = service.generarCuenta(TipoReferenciaPago.INSCRIPCION, 9L, cuenta.getCliente(), "Curso gratuito", BigDecimal.ZERO);
        assertThat(p.getEstado()).isEqualTo(EstadoPago.PAGADO);
    }
}
