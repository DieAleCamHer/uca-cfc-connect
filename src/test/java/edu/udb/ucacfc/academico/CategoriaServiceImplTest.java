package edu.udb.ucacfc.academico;

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
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    @Test
    void crear_guardaLaCategoria_cuandoElNombreNoExisteAun() {
        Categoria categoria = Categoria.builder().nombre("Tecnologia").build();
        when(categoriaRepository.existsByNombreIgnoreCase("Tecnologia")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = categoriaService.crear(categoria);

        assertEquals("Tecnologia", resultado.getNombre());
    }

    @Test
    void crear_lanzaRegistroDuplicado_cuandoElNombreYaExiste() {
        Categoria categoria = Categoria.builder().nombre("Tecnologia").build();
        when(categoriaRepository.existsByNombreIgnoreCase("Tecnologia")).thenReturn(true);

        assertThrows(RegistroDuplicadoException.class, () -> categoriaService.crear(categoria));
    }

    @Test
    void buscarPorId_lanzaRecursoNoEncontrado_cuandoNoExiste() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> categoriaService.buscarPorId(1L));
    }
}
