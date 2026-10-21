package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.security.AccesoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La regla central del lineamiento: "No podran existir conflictos de horario". */
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock
    ActividadAgendaRepository repo;
    @Mock
    EspacioRepository espacioRepository;
    @Mock
    AccesoService acceso;
    @InjectMocks
    AgendaService service;

    final Espacio aula = Espacio.builder().id(1L).nombre("Aula 101").capacidad(30).build();
    final LocalDateTime ini = LocalDateTime.of(2026, 11, 10, 8, 0);
    final LocalDateTime fin = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Test
    @DisplayName("Exito: registra la reserva cuando el aula y el horario estan libres (bloqueando la fila del espacio)")
    void registraSiEstaLibre() {
        when(repo.cruces(1L, ini, fin)).thenReturn(List.of());

        service.registrar(NuevaActividad.bloqueante(TipoActividad.ALQUILER, 5L, "Alquiler: Aula 101", ini, fin, aula, 3L));

        verify(espacioRepository).bloquear(1L);
        verify(repo).save(any(ActividadAgenda.class));
    }

    @Test
    @DisplayName("Fallo por solapamiento: ya existe una actividad a la misma hora -> EspacioOcupadoException")
    void lanzaEspacioOcupado() {
        ActividadAgenda existente = ActividadAgenda.builder().titulo("Curso: Java Basico (sesion 1/8)")
                .inicio(ini.plusHours(2)).fin(fin.plusHours(2)).build();
        when(repo.cruces(1L, ini, fin)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.registrar(
                NuevaActividad.bloqueante(TipoActividad.ALQUILER, 5L, "Alquiler", ini, fin, aula, 3L)))
                .isInstanceOf(EspacioOcupadoException.class)
                .hasMessageContaining("Aula 101").hasMessageContaining("Java Basico");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Catering dentro de una reserva: se agenda sin validar cruces (la reserva ya bloquea el espacio)")
    void actividadNoBloqueanteNoValidaCruces() {
        service.registrar(new NuevaActividad(TipoActividad.CATERING, 9L, "Catering", ini, fin, aula, false, 3L, null));

        verify(repo, never()).cruces(anyLong(), any(), any());
        verify(repo).save(any(ActividadAgenda.class));
    }

    @Test
    @DisplayName("Fin antes del inicio -> 400")
    void finAntesDeInicio() {
        assertThatThrownBy(() -> service.registrar(
                NuevaActividad.bloqueante(TipoActividad.EVENTO, 1L, "x", fin, ini, aula, null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("El conflicto ignora las actividades del propio registro que se edita")
    void conflictoExcluyeElPropioRegistro() {
        ActividadAgenda propia = ActividadAgenda.builder().tipo(TipoActividad.CURSO).referenciaId(7L).titulo("propia")
                .inicio(ini).fin(fin).build();
        when(repo.cruces(1L, ini, fin)).thenReturn(List.of(propia));

        assertThat(service.conflicto(1L, ini, fin, TipoActividad.CURSO, 7L)).isEmpty();
        assertThat(service.conflicto(1L, ini, fin, null, null)).isPresent();
    }
}
