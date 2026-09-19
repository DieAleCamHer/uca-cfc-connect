package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaServiceImplTest {

    @Mock
    private ActividadAgendaRepository actividadAgendaRepository;

    @InjectMocks
    private AgendaServiceImpl agendaService;

    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 8, 0);
    private final LocalDateTime fin = LocalDateTime.of(2026, 9, 1, 10, 0);

    @Test
    void registrarActividad_seGuarda_cuandoNoUsaEspacioFisico() {
        // Un curso 100% virtual no tiene espacio: nunca se valida cruce de horario.
        when(actividadAgendaRepository.save(any(ActividadAgenda.class))).thenAnswer(inv -> inv.getArgument(0));

        ActividadAgenda resultado = agendaService.registrarActividad(
                TipoActividad.CURSO, 1L, "Curso Virtual Java", inicio, fin, null);

        assertNotNull(resultado);
        verify(actividadAgendaRepository, never()).findSolapadasPorEspacio(any(), any(), any());
    }

    @Test
    void registrarActividad_lanzaEspacioOcupado_cuandoHayCruceEnLaAgendaGeneral() {
        Espacio espacio = Espacio.builder().id(1L).nombre("Sala Multimedia").build();
        ActividadAgenda existente = ActividadAgenda.builder().id(1L).tipo(TipoActividad.EVENTO).build();
        when(actividadAgendaRepository.findSolapadasPorEspacio(1L, inicio, fin)).thenReturn(List.of(existente));

        assertThrows(EspacioOcupadoException.class, () ->
                agendaService.registrarActividad(TipoActividad.ALQUILER, 5L, "Alquiler", inicio, fin, espacio));

        verify(actividadAgendaRepository, never()).save(any());
    }

    @Test
    void eliminarPorReferencia_borraLasActividadesAsociadas() {
        ActividadAgenda actividad = ActividadAgenda.builder().id(1L).tipo(TipoActividad.ALQUILER).referenciaId(5L).build();
        when(actividadAgendaRepository.findByTipoAndReferenciaId(TipoActividad.ALQUILER, 5L))
                .thenReturn(List.of(actividad));

        agendaService.eliminarPorReferencia(TipoActividad.ALQUILER, 5L);

        verify(actividadAgendaRepository, times(1)).deleteAll(List.of(actividad));
    }
}
