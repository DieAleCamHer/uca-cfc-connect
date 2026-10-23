package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.TestData;
import edu.udb.ucacfc.academico.categoria.Categoria;
import edu.udb.ucacfc.academico.categoria.CategoriaRepository;
import edu.udb.ucacfc.academico.docente.Docente;
import edu.udb.ucacfc.academico.docente.DocenteRepository;
import edu.udb.ucacfc.academico.modalidad.Modalidad;
import edu.udb.ucacfc.academico.modalidad.ModalidadRepository;
import edu.udb.ucacfc.academico.oferta.Curso;
import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.CursoService;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.academico.oferta.OfertaDependencias;
import edu.udb.ucacfc.academico.oferta.SesionOferta;
import edu.udb.ucacfc.academico.oferta.dto.OfertaRequest;
import edu.udb.ucacfc.academico.oferta.dto.OfertaResponse;
import edu.udb.ucacfc.academico.oferta.dto.SesionRequest;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.NuevaActividad;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.inscripcion.InscripcionCascadaService;
import edu.udb.ucacfc.shared.exception.DocenteOcupadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoServiceTest {

    @Mock
    CursoRepository cursoRepository;
    @Mock
    DiplomadoRepository diplomadoRepository;
    @Mock
    CategoriaRepository categoriaRepository;
    @Mock
    ModalidadRepository modalidadRepository;
    @Mock
    DocenteRepository docenteRepository;
    @Mock
    EspacioRepository espacioRepository;
    @Mock
    AgendaService agenda;
    @Mock
    InscripcionCascadaService inscripciones;
    @Mock
    OfertaDependencias deps;

    CursoService service;

    final Categoria tec = Categoria.builder().id(1L).nombre("Tecnologia").build();
    final Modalidad presencial = Modalidad.builder().id(1L).nombre("PRESENCIAL").requiereEspacioFisico(true).build();
    final Modalidad virtual = Modalidad.builder().id(2L).nombre("VIRTUAL").requiereEspacioFisico(false).build();
    final Docente ana = Docente.builder().id(1L).nombre("Ing. Ana Perez").correo("a@x.sv").build();
    final Espacio lab = Espacio.builder().id(1L).nombre("Laboratorio 1").capacidad(25).precioHora(BigDecimal.TEN).build();
    final LocalDateTime lunes = LocalDateTime.of(2026, 11, 2, 18, 0);

    @BeforeEach
    void setUp() {
        when(deps.getCategorias()).thenReturn(categoriaRepository);
        when(deps.getModalidades()).thenReturn(modalidadRepository);
        when(deps.getDocentes()).thenReturn(docenteRepository);
        when(deps.getEspacios()).thenReturn(espacioRepository);
        when(deps.getCursos()).thenReturn(cursoRepository);
        when(deps.getDiplomados()).thenReturn(diplomadoRepository);
        when(deps.getAgenda()).thenReturn(agenda);
        when(deps.getInscripciones()).thenReturn(inscripciones);
        when(deps.getClock()).thenReturn(TestData.CLOCK);
        service = new CursoService(cursoRepository, deps);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(tec));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(presencial));
        when(modalidadRepository.findById(2L)).thenReturn(Optional.of(virtual));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(lab));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> {
            Curso c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
    }

    private OfertaRequest request(Long modalidadId, Long espacioId, int cupo, List<SesionRequest> sesiones) {
        return new OfertaRequest("Spring Boot", null, 1L, modalidadId, 1L, espacioId, cupo, new BigDecimal("150.00"), sesiones);
    }

    private List<SesionRequest> dosSesiones() {
        return List.of(new SesionRequest(lunes, lunes.plusHours(2)),
                new SesionRequest(lunes.plusDays(2), lunes.plusDays(2).plusHours(2)));
    }

    @Test
    @DisplayName("Fallo por negocio: un aula para un curso VIRTUAL se rechaza")
    void virtualConAulaSeRechaza() {
        assertThatThrownBy(() -> service.crear(request(2L, 1L, 20, dosSesiones())))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no requiere espacio fisico");
        verify(cursoRepository, never()).save(any());
        verify(agenda, never()).registrarTodas(anyList());
    }

    @Test
    @DisplayName("Presencial sin espacio se rechaza")
    void presencialSinEspacio() {
        assertThatThrownBy(() -> service.crear(request(1L, null, 20, dosSesiones())))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("requiere espacio fisico");
    }

    @Test
    @DisplayName("El cupo no puede superar la capacidad del espacio")
    void cupoMayorQueCapacidad() {
        assertThatThrownBy(() -> service.crear(request(1L, 1L, 30, dosSesiones())))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("capacidad");
    }

    @Test
    @DisplayName("Dos sesiones de la misma oferta no pueden cruzarse")
    void sesionesCruzadas() {
        List<SesionRequest> cruzadas = List.of(new SesionRequest(lunes, lunes.plusHours(2)),
                new SesionRequest(lunes.plusHours(1), lunes.plusHours(3)));
        assertThatThrownBy(() -> service.crear(request(1L, 1L, 20, cruzadas)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("se cruzan");
    }

    @Test
    @DisplayName("No se programa una oferta en el pasado")
    void sesionEnElPasado() {
        LocalDateTime ayer = TestData.AHORA.minusDays(1);
        assertThatThrownBy(() -> service.crear(request(1L, 1L, 20, List.of(new SesionRequest(ayer, ayer.plusHours(2))))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("futuras");
    }

    @Test
    @DisplayName("El docente no puede impartir dos ofertas con sesiones cruzadas")
    void docenteOcupado() {
        Curso otro = new Curso();
        otro.setId(99L);
        otro.setNombre("Python");
        otro.getSesiones().add(new SesionOferta(lunes.plusMinutes(30), lunes.plusHours(3)));
        when(cursoRepository.findByDocenteIdAndActivoTrueAndFinAfter(any(), any())).thenReturn(List.of(otro));

        assertThatThrownBy(() -> service.crear(request(1L, 1L, 20, dosSesiones())))
                .isInstanceOf(DocenteOcupadoException.class).hasMessageContaining("Python");
        verify(cursoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exito presencial: guarda y ocupa el espacio UNA vez por sesion")
    @SuppressWarnings("unchecked")
    void exitoRegistraUnaActividadPorSesion() {
        OfertaResponse r = service.crear(request(1L, 1L, 20, dosSesiones()));

        ArgumentCaptor<List<NuevaActividad>> captor = ArgumentCaptor.forClass(List.class);
        verify(agenda).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).titulo()).contains("sesion 1/2");
        assertThat(r.inicio()).isEqualTo(lunes);
        assertThat(r.fin()).isEqualTo(lunes.plusDays(2).plusHours(2));
        assertThat(r.horasTotales()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Exito virtual: no toca la agenda de espacios")
    void exitoVirtualNoUsaAgenda() {
        service.crear(request(2L, null, 40, dosSesiones()));
        verify(agenda, never()).registrarTodas(anyList());
    }
}
