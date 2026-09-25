package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModalidadServiceImplTest {

    @Mock
    private ModalidadRepository modalidadRepository;

    @InjectMocks
    private ModalidadServiceImpl modalidadService;

    @Test
    void crear_guardaLaModalidad_cuandoElNombreNoExisteAun() {
        Modalidad modalidad = Modalidad.builder().nombre("VIRTUAL").requiereEspacioFisico(false).build();
        when(modalidadRepository.existsByNombreIgnoreCase("VIRTUAL")).thenReturn(false);
        when(modalidadRepository.save(any(Modalidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Modalidad resultado = modalidadService.crear(modalidad);

        assertFalse(resultado.isRequiereEspacioFisico());
    }

    @Test
    void crear_lanzaRegistroDuplicado_cuandoElNombreYaExiste() {
        Modalidad modalidad = Modalidad.builder().nombre("PRESENCIAL").build();
        when(modalidadRepository.existsByNombreIgnoreCase("PRESENCIAL")).thenReturn(true);

        assertThrows(RegistroDuplicadoException.class, () -> modalidadService.crear(modalidad));
    }
}
