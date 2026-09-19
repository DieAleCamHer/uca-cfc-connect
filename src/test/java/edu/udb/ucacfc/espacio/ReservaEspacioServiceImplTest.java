package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas del Service mas critico del sistema: la regla de "no cruce de
 * horarios" al reservar un espacio. Este es exactamente el caso que pide
 * demostrar el lineamiento del proyecto.
 */
@ExtendWith(MockitoExtension.class)
class ReservaEspacioServiceImplTest {

    @Mock
    private ReservaEspacioRepository reservaEspacioRepository;
    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private ReservaEspacioServiceImpl reservaEspacioService;

    private Espacio espacio;
    private Cliente cliente;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    @BeforeEach
    void setUp() {
        espacio = Espacio.builder().id(1L).nombre("Auditorio Principal").build();
        cliente = Cliente.builder().id(1L).nombre("Empresa ACME").build();
        inicio = LocalDateTime.of(2026, 8, 20, 9, 0);
        fin = LocalDateTime.of(2026, 8, 20, 11, 0);
    }

    @Test
    void reservar_creaLaReserva_cuandoElEspacioEstaLibre() {
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(espacio));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(reservaEspacioRepository.findSolapadas(1L, inicio, fin)).thenReturn(List.of());
        when(reservaEspacioRepository.save(any(ReservaEspacio.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReservaEspacio resultado = reservaEspacioService.reservar(1L, 1L, inicio, fin, "Conferencia");

        assertEquals(EstadoReserva.RESERVADO, resultado.getEstado());
        assertEquals(espacio, resultado.getEspacio());
        verify(agendaService, times(1))
                .registrarActividad(any(), anyLong(), any(), eq(inicio), eq(fin), eq(espacio));
    }

    @Test
    void reservar_lanzaEspacioOcupadoException_cuandoHayCruceDeHorario() {
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(espacio));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        ReservaEspacio existente = ReservaEspacio.builder().id(99L).espacio(espacio).build();
        when(reservaEspacioRepository.findSolapadas(1L, inicio, fin)).thenReturn(List.of(existente));

        assertThrows(EspacioOcupadoException.class,
                () -> reservaEspacioService.reservar(1L, 1L, inicio, fin, "Conferencia"));

        // Si esta ocupado, JAMAS debe llegar a guardar la reserva ni tocar la agenda.
        verify(reservaEspacioRepository, never()).save(any());
        verify(agendaService, never()).registrarActividad(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reservar_lanzaOperacionInvalida_cuandoFinNoEsPosteriorAInicio() {
        assertThrows(OperacionInvalidaException.class,
                () -> reservaEspacioService.reservar(1L, 1L, fin, inicio, "Motivo"));
        verifyNoInteractions(espacioRepository, clienteRepository, reservaEspacioRepository, agendaService);
    }

    @Test
    void reservar_lanzaRecursoNoEncontrado_cuandoElEspacioNoExiste() {
        when(espacioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> reservaEspacioService.reservar(1L, 1L, inicio, fin, "Motivo"));
    }

    @Test
    void cancelar_cambiaEstadoYLiberaLaAgenda() {
        ReservaEspacio reserva = ReservaEspacio.builder()
                .id(5L).espacio(espacio).cliente(cliente).estado(EstadoReserva.RESERVADO).build();
        when(reservaEspacioRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(reservaEspacioRepository.save(any(ReservaEspacio.class))).thenAnswer(inv -> inv.getArgument(0));

        reservaEspacioService.cancelar(5L);

        assertEquals(EstadoReserva.CANCELADO, reserva.getEstado());
        verify(agendaService, times(1)).eliminarPorReferencia(any(), eq(5L));
    }
}
