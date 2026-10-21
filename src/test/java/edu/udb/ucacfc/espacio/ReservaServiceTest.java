package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.TestData;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.NuevaActividad;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacio;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacioRepository;
import edu.udb.ucacfc.espacio.reserva.ReservaService;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaRequest;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaResponse;
import edu.udb.ucacfc.pago.PagoService;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.config.ReglasNegocioProperties;
import edu.udb.ucacfc.shared.exception.AccesoDenegadoException;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock
    ReservaEspacioRepository repo;
    @Mock
    EspacioService espacioService;
    @Mock
    ClienteService clienteService;
    @Mock
    AgendaService agenda;
    @Mock
    PagoService pagoService;
    @Mock
    SolicitudCateringRepository cateringRepository;
    @Mock
    AccesoService acceso;

    ReservaService service;
    final Usuario recepcion = TestData.usuario(2, Rol.RECEPCIONISTA);
    final Usuario cliente = TestData.usuario(3, Rol.CLIENTE);
    final Cliente empresa = Cliente.builder().id(1L).tipo(TipoCliente.EMPRESA).nombre("Carlos").empresa("Delta").build();
    final Espacio auditorio = Espacio.builder().id(1L).nombre("Auditorio").capacidad(100)
            .precioHora(new BigDecimal("40.00")).activo(true).build();
    final LocalDateTime ini = LocalDateTime.of(2026, 11, 20, 8, 0);

    @BeforeEach
    void setUp() {
        service = new ReservaService(repo, espacioService, clienteService, agenda, pagoService, cateringRepository,
                acceso, new ReglasNegocioProperties(24, 24, 15), TestData.CLOCK);
        when(acceso.resolverClienteParaCrear(any(), any())).thenReturn(1L);
        when(clienteService.buscarActivo(1L)).thenReturn(empresa);
        when(espacioService.buscar(1L)).thenReturn(auditorio);
        when(repo.save(any(ReservaEspacio.class))).thenAnswer(inv -> {
            ReservaEspacio r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(5L);
            }
            return r;
        });
        when(pagoService.cuentaActiva(any(), anyLong())).thenReturn(Optional.empty());
        when(cateringRepository.findByReservaIdAndEstadoIn(anyLong(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Exito: aula libre -> SOLICITADA, total = horas x precio por hora y queda en la agenda")
    void exitoCalculaCosto() {
        ReservaResponse r = service.crear(new ReservaRequest(1L, 1L, ini, ini.plusMinutes(210), 80, "Capacitacion"), recepcion);

        assertThat(r.estado()).isEqualTo(EstadoReserva.SOLICITADA);
        assertThat(r.horas()).isEqualByComparingTo("3.50");
        assertThat(r.total()).isEqualByComparingTo("140.00");
        verify(agenda).registrar(any(NuevaActividad.class));
    }

    @Test
    @DisplayName("Solapamiento: la agenda encuentra otra actividad -> EspacioOcupadoException")
    void espacioOcupado() {
        doThrow(new EspacioOcupadoException("El espacio 'Auditorio' ya esta ocupado")).when(agenda).registrar(any());

        assertThatThrownBy(() -> service.crear(new ReservaRequest(1L, 1L, ini, ini.plusHours(2), 50, "x"), recepcion))
                .isInstanceOf(EspacioOcupadoException.class);
    }

    @Test
    @DisplayName("Mas asistentes que la capacidad -> 400")
    void excedeCapacidad() {
        assertThatThrownBy(() -> service.crear(new ReservaRequest(1L, 1L, ini, ini.plusHours(2), 150, "x"), recepcion))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("capacidad");
        verify(agenda, never()).registrar(any());
    }

    @Test
    @DisplayName("Una reserva de mas de 12 horas se rechaza")
    void duracionMaxima() {
        assertThatThrownBy(() -> service.crear(new ReservaRequest(1L, 1L, ini, ini.plusHours(13), 10, "x"), recepcion))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Dueno del registro: un CLIENTE no cancela la reserva de otro (403) y no se toca el dinero")
    void clienteNoCancelaAjena() {
        ReservaEspacio ajena = ReservaEspacio.builder().id(7L).cliente(empresa).espacio(auditorio).inicio(ini)
                .fin(ini.plusHours(2)).estado(EstadoReserva.SOLICITADA).build();
        when(repo.findById(7L)).thenReturn(Optional.of(ajena));
        doThrow(new AccesoDenegadoException("No puedes acceder a una reserva de otro cliente"))
                .when(acceso).verificarDueno(eq(cliente), eq(1L), anyString());

        assertThatThrownBy(() -> service.cancelar(7L, null, cliente)).isInstanceOf(AccesoDenegadoException.class);
        verify(pagoService, never()).anularCuenta(any(), anyLong(), any());
        assertThat(ajena.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    @DisplayName("No hay reembolsos: cancelar con dinero cobrado -> 409 y la reserva sigue igual")
    void cancelarConDinero() {
        ReservaEspacio r = ReservaEspacio.builder().id(8L).cliente(empresa).espacio(auditorio).inicio(ini)
                .fin(ini.plusHours(2)).estado(EstadoReserva.CONFIRMADA).build();
        when(repo.findById(8L)).thenReturn(Optional.of(r));
        doThrow(new ConflictoEstadoException("ya tiene $40 cobrados"))
                .when(pagoService).anularCuenta(eq(TipoReferenciaPago.ALQUILER), eq(8L), any());

        assertThatThrownBy(() -> service.cancelar(8L, null, recepcion)).isInstanceOf(ConflictoEstadoException.class);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(agenda, never()).eliminar(any(), anyLong());
    }

    @Test
    @DisplayName("Confirmar genera la cuenta por cobrar con el total congelado")
    void confirmarGeneraCuenta() {
        ReservaEspacio r = ReservaEspacio.builder().id(9L).cliente(empresa).espacio(auditorio).inicio(ini)
                .fin(ini.plusHours(2)).numeroAsistentes(30).proposito("Taller").horas(new BigDecimal("2.00"))
                .precioHora(new BigDecimal("40.00")).total(new BigDecimal("80.00"))
                .estado(EstadoReserva.SOLICITADA).build();
        when(repo.findById(9L)).thenReturn(Optional.of(r));

        service.confirmar(9L, recepcion);

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(pagoService).generarCuenta(eq(TipoReferenciaPago.ALQUILER), eq(9L), eq(empresa), anyString(),
                eq(new BigDecimal("80.00")));
    }
}
