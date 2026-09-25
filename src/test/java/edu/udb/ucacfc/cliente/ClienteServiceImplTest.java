package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @Test
    void crear_guardaElCliente_cuandoElDuiNitNoExisteAun() {
        Cliente cliente = Cliente.builder().duiNit("06142512-3").nombre("Maria Lopez").correo("maria@test.com").build();
        when(clienteRepository.existsByDuiNit("06142512-3")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.crear(cliente);

        assertEquals("Maria Lopez", resultado.getNombre());
    }

    @Test
    void crear_lanzaRegistroDuplicado_cuandoElDuiNitYaExiste() {
        Cliente cliente = Cliente.builder().duiNit("06142512-3").nombre("Maria Lopez").build();
        when(clienteRepository.existsByDuiNit("06142512-3")).thenReturn(true);

        assertThrows(RegistroDuplicadoException.class, () -> clienteService.crear(cliente));
    }

    @Test
    void buscarPorId_lanzaRecursoNoEncontrado_cuandoNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.buscarPorId(99L));
    }
}
