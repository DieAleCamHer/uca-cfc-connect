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
class DiplomadoServiceImplTest {

    @Mock
    private DiplomadoRepository diplomadoRepository;
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
    private DiplomadoServiceImpl diplomadoService;

    @Test
    void crear_lanzaOperacionInvalida_cuandoFechaFinEsAnteriorAFechaInicio() {
        Diplomado diplomado = diplomadoValido(false);
        diplomado.setFechaInicio(LocalDate.of(2026, 9, 10));
        diplomado.setFechaFin(LocalDate.of(2026, 9, 1));

        assertThrows(OperacionInvalidaException.class, () -> diplomadoService.crear(diplomado));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoElCupoEsCeroONegativo() {
        Diplomado diplomado = diplomadoValido(false);
        diplomado.setCupoMaximo(-5);

        assertThrows(OperacionInvalidaException.class, () -> diplomadoService.crear(diplomado));
    }

    /** Mismo caso de negocio citado por el lineamiento, aplicado a Diplomado. */
    @Test
    void crear_lanzaOperacionInvalida_cuandoAsignanEspacioAUnDiplomadoDeModalidadVirtual() {
        Diplomado diplomado = diplomadoValido(true);
        Modalidad virtual = Modalidad.builder().id(1L).nombre("VIRTUAL").requiereEspacioFisico(false).build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(Categoria.builder().id(1L).build()));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(virtual));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).build()));

        assertThrows(OperacionInvalidaException.class, () -> diplomadoService.crear(diplomado));
    }

    @Test
    void crear_guardaElDiplomado_cuandoTodosLosDatosSonValidos() {
        Diplomado diplomado = diplomadoValido(true);
        Modalidad presencial = Modalidad.builder().id(1L).nombre("PRESENCIAL").requiereEspacioFisico(true).build();
        Espacio espacio = Espacio.builder().id(1L).nombre("Sala de reuniones").build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(Categoria.builder().id(1L).build()));
        when(modalidadRepository.findById(1L)).thenReturn(Optional.of(presencial));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).build()));
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(espacio));
        when(diplomadoRepository.save(any(Diplomado.class))).thenAnswer(inv -> inv.getArgument(0));

        Diplomado resultado = diplomadoService.crear(diplomado);

        assertTrue(resultado.isActivo());
    }

    @Test
    void desactivar_dejaElDiplomadoInactivo() {
        Diplomado existente = diplomadoValido(false);
        existente.setId(1L);
        existente.setActivo(true);
        when(diplomadoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(diplomadoRepository.save(any(Diplomado.class))).thenAnswer(inv -> inv.getArgument(0));

        diplomadoService.desactivar(1L);

        assertFalse(existente.isActivo());
    }

    private Diplomado diplomadoValido(boolean conEspacio) {
        Diplomado diplomado = new Diplomado();
        diplomado.setNombre("Diplomado en Gerencia de Proyectos");
        diplomado.setCategoria(Categoria.builder().id(1L).build());
        diplomado.setModalidad(Modalidad.builder().id(1L).build());
        diplomado.setDocente(Docente.builder().id(1L).build());
        if (conEspacio) {
            diplomado.setEspacio(Espacio.builder().id(1L).build());
        }
        diplomado.setCupoMaximo(15);
        diplomado.setFechaInicio(LocalDate.of(2026, 9, 1));
        diplomado.setFechaFin(LocalDate.of(2026, 12, 1));
        diplomado.setHoraInicio(LocalTime.of(18, 0));
        diplomado.setHoraFin(LocalTime.of(20, 0));
        diplomado.setCosto(new BigDecimal("500.00"));
        return diplomado;
    }
}
