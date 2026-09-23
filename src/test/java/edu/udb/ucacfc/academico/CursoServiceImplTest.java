package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceImplTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ModalidadRepository modalidadRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private CursoServiceImpl cursoService;

    @Test
    void crear_lanzaOperacionInvalida_cuandoFechaFinEsAnteriorAFechaInicio() {
        Curso curso = cursoValido(false);
        curso.setFechaInicio(LocalDate.of(2026, 9, 10));
        curso.setFechaFin(LocalDate.of(2026, 9, 1)); // invalido: antes que el inicio

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoHoraFinNoEsPosteriorAHoraInicio() {
        Curso curso = cursoValido(false);
        curso.setHoraInicio(LocalTime.of(10, 0));
        curso.setHoraFin(LocalTime.of(9, 0)); // invalido

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoElCupoMaximoEsCeroONegativo() {
        Curso curso = cursoValido(false);
        curso.setCupoMaximo(0);

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoElCostoEsNegativo() {
        Curso curso = cursoValido(false);
        curso.setCosto(new BigDecimal("-10.00"));

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    /**
     * Este es EXACTAMENTE el caso de negocio que cita el lineamiento del
     * proyecto como ejemplo obligatorio: "Intentar reservar un aula
     * presencial para un curso virtual (lo cual deberia ser rechazado por
     * la logica de negocio)".
     */
    @Test
    void crear_lanzaOperacionInvalida_cuandoAsignanEspacioAUnCursoDeModalidadVirtual() {
        Curso curso = cursoValido(true); // pide espacio, pero la modalidad sera VIRTUAL
        Modalidad virtual = Modalidad.builder().id(1L).nombre("VIRTUAL").requiereEspacioFisico(false).build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(Categoria.builder().id(1L).build()));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(virtual));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).build()));

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoModalidadPresencialNoTraeEspacio() {
        Curso curso = cursoValido(false); // NO trae espacio
        Modalidad presencial = Modalidad.builder().id(1L).nombre("PRESENCIAL").requiereEspacioFisico(true).build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(Categoria.builder().id(1L).build()));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(presencial));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).build()));

        assertThrows(OperacionInvalidaException.class, () -> cursoService.crear(curso));
    }

    @Test
    void crear_guardaElCursoYLoRegistraEnAgenda_cuandoEsPresencialConEspacioLibre() {
        Curso curso = cursoValido(true);
        Modalidad presencial = Modalidad.builder().id(1L).nombre("PRESENCIAL").requiereEspacioFisico(true).build();
        Espacio espacio = Espacio.builder().id(1L).nombre("Aula 101").build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(Categoria.builder().id(1L).build()));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(presencial));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).build()));
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(espacio));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        Curso resultado = cursoService.crear(curso);

        assertTrue(resultado.isActivo());
        assertEquals(espacio, resultado.getEspacio());
    }

    private Curso cursoValido(boolean conEspacio) {
        Curso curso = new Curso();
        curso.setNombre("Java Basico");
        curso.setCategoria(Categoria.builder().id(1L).build());
        curso.setModalidad(Modalidad.builder().id(1L).build());
        curso.setDocente(Docente.builder().id(1L).build());
        if (conEspacio) {
            curso.setEspacio(Espacio.builder().id(1L).build());
        }
        curso.setCupoMaximo(20);
        curso.setFechaInicio(LocalDate.of(2026, 9, 1));
        curso.setFechaFin(LocalDate.of(2026, 10, 1));
        curso.setHoraInicio(LocalTime.of(8, 0));
        curso.setHoraFin(LocalTime.of(10, 0));
        curso.setCosto(new BigDecimal("100.00"));
        return curso;
    }
}
