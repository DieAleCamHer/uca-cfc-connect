package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.TestData;
import edu.udb.ucacfc.academico.oferta.Curso;
import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.inscripcion.dto.InscripcionRequest;
import edu.udb.ucacfc.inscripcion.dto.InscripcionResponse;
import edu.udb.ucacfc.pago.PagoService;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.exception.CupoExcedidoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.exception.TransicionInvalidaException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscripcionServiceTest {

    @Mock
    InscripcionRepository repo;
    @Mock
    CursoRepository cursoRepository;
    @Mock
    DiplomadoRepository diplomadoRepository;
    @Mock
    ClienteService clienteService;
    @Mock
    PagoService pagoService;
    @Mock
    AccesoService acceso;

    InscripcionService service;
    final Usuario recepcion = TestData.usuario(2, Rol.RECEPCIONISTA);
    final Cliente persona = Cliente.builder().id(1L).tipo(TipoCliente.PERSONA).nombre("Ana").build();
    final Cliente empresa = Cliente.builder().id(2L).tipo(TipoCliente.EMPRESA).nombre("Carlos").empresa("Delta").build();
    Curso curso;

    @BeforeEach
    void setUp() {
        service = new InscripcionService(repo, cursoRepository, diplomadoRepository, clienteService, pagoService, acceso,
                TestData.CLOCK);
        curso = new Curso();
        curso.setId(10L);
        curso.setNombre("Spring Boot");
        curso.setCupoMaximo(2);
        curso.setCosto(new BigDecimal("150.00"));
        curso.setInicio(LocalDateTime.of(2026, 11, 2, 18, 0));
        curso.setFin(LocalDateTime.of(2026, 11, 30, 20, 0));
        when(cursoRepository.bloquear(10L)).thenReturn(Optional.of(curso));
        when(acceso.resolverClienteParaCrear(any(), eq(1L))).thenReturn(1L);
        when(acceso.resolverClienteParaCrear(any(), eq(2L))).thenReturn(2L);
        when(clienteService.buscarActivo(1L)).thenReturn(persona);
        when(clienteService.buscarActivo(2L)).thenReturn(empresa);
        when(repo.save(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(5L);
            }
            return i;
        });
        when(pagoService.cuentaActiva(any(), anyLong())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Exito: nace PENDIENTE con el costo vigente congelado")
    void exito() {
        InscripcionResponse r = service.crear(new InscripcionRequest(1L, 10L, null, null, null, null), recepcion);
        assertThat(r.estado()).isEqualTo(EstadoInscripcion.PENDIENTE);
        assertThat(r.monto()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Cupo lleno -> CupoExcedidoException")
    void cupoLleno() {
        when(repo.countByCursoIdAndEstadoIn(eq(10L), any())).thenReturn(2L);
        assertThatThrownBy(() -> service.crear(new InscripcionRequest(1L, 10L, null, null, null, null), recepcion))
                .isInstanceOf(CupoExcedidoException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Curso Y diplomado a la vez -> 400")
    void cursoXorDiplomado() {
        assertThatThrownBy(() -> service.crear(new InscripcionRequest(1L, 10L, 3L, null, null, null), recepcion))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("exactamente uno");
    }

    @Test
    @DisplayName("Una empresa debe indicar el participante")
    void empresaSinParticipante() {
        assertThatThrownBy(() -> service.crear(new InscripcionRequest(2L, 10L, null, null, null, null), recepcion))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("participante");
    }

    @Test
    @DisplayName("La misma persona no se inscribe dos veces en la misma oferta")
    void duplicado() {
        when(repo.existsByCursoIdAndClienteIdAndParticipanteDuiIsNullAndEstadoInAndIdNot(eq(10L), eq(1L), any(), eq(-1L)))
                .thenReturn(true);
        assertThatThrownBy(() -> service.crear(new InscripcionRequest(1L, 10L, null, null, null, null), recepcion))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    @DisplayName("Curso desactivado no acepta inscripciones")
    void cursoDesactivado() {
        curso.setActivo(false);
        assertThatThrownBy(() -> service.crear(new InscripcionRequest(1L, 10L, null, null, null, null), recepcion))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("desactivado");
    }

    @Test
    @DisplayName("Confirmar genera la cuenta por cobrar con el monto congelado")
    void confirmarGeneraCuenta() {
        Inscripcion i = Inscripcion.builder().id(5L).cliente(persona).curso(curso).estado(EstadoInscripcion.PENDIENTE)
                .monto(new BigDecimal("150.00")).fechaInscripcion(TestData.AHORA).build();
        when(repo.findById(5L)).thenReturn(Optional.of(i));

        service.confirmar(5L, recepcion);

        assertThat(i.getEstado()).isEqualTo(EstadoInscripcion.CONFIRMADA);
        verify(pagoService).generarCuenta(eq(TipoReferenciaPago.INSCRIPCION), eq(5L), eq(persona), anyString(),
                eq(new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("Maquina de estados: PENDIENTE no puede pasar directo a FINALIZADA")
    void transicionInvalida() {
        Inscripcion i = Inscripcion.builder().id(6L).cliente(persona).curso(curso).estado(EstadoInscripcion.PENDIENTE)
                .monto(BigDecimal.TEN).build();
        when(repo.findById(6L)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.finalizar(6L, recepcion)).isInstanceOf(TransicionInvalidaException.class);
    }
}
