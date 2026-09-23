package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
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
class DocenteServiceImplTest {

    @Mock
    private DocenteRepository docenteRepository;

    @InjectMocks
    private DocenteServiceImpl docenteService;

    @Test
    void crear_guardaElDocente() {
        Docente docente = Docente.builder().nombre("Ing. Carlos Ramirez").especialidad("Java").build();
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        Docente resultado = docenteService.crear(docente);

        assertEquals("Ing. Carlos Ramirez", resultado.getNombre());
    }

    @Test
    void buscarPorId_lanzaRecursoNoEncontrado_cuandoNoExiste() {
        when(docenteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> docenteService.buscarPorId(1L));
    }

    @Test
    void actualizar_modificaLosDatosDelDocenteExistente() {
        Docente existente = Docente.builder().id(1L).nombre("Viejo Nombre").build();
        Docente datos = Docente.builder().nombre("Nuevo Nombre").especialidad("Spring").correo("c@udb.edu.sv").telefono("7000-0000").build();
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        Docente resultado = docenteService.actualizar(1L, datos);

        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals("Spring", resultado.getEspecialidad());
    }
}
