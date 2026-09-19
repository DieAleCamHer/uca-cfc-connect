package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioServiceImplTest {

    @Mock
    private EspacioRepository espacioRepository;

    @InjectMocks
    private EspacioServiceImpl espacioService;

    @Test
    void crear_lanzaOperacionInvalida_cuandoLaCapacidadEsCeroONegativa() {
        Espacio espacio = Espacio.builder().nombre("Aula 1").tipo(TipoEspacio.AULA).capacidad(0)
                .precio(new BigDecimal("10.00")).build();

        assertThrows(OperacionInvalidaException.class, () -> espacioService.crear(espacio));
    }

    @Test
    void crear_lanzaOperacionInvalida_cuandoElPrecioEsNegativo() {
        Espacio espacio = Espacio.builder().nombre("Aula 1").tipo(TipoEspacio.AULA).capacidad(30)
                .precio(new BigDecimal("-5.00")).build();

        assertThrows(OperacionInvalidaException.class, () -> espacioService.crear(espacio));
    }

    @Test
    void crear_guardaElEspacio_cuandoLosDatosSonValidos() {
        Espacio espacio = Espacio.builder().nombre("Auditorio").tipo(TipoEspacio.AUDITORIO).capacidad(100)
                .precio(new BigDecimal("250.00")).build();
        when(espacioRepository.save(any(Espacio.class))).thenAnswer(inv -> inv.getArgument(0));

        Espacio resultado = espacioService.crear(espacio);

        assertEquals("Auditorio", resultado.getNombre());
    }

    @Test
    void eliminar_marcaElEspacioComoNoDisponible_enVezDeBorrarlo() {
        Espacio existente = Espacio.builder().id(1L).nombre("Sala").disponible(true).build();
        when(espacioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(espacioRepository.save(any(Espacio.class))).thenAnswer(inv -> inv.getArgument(0));

        espacioService.eliminar(1L);

        assertFalse(existente.isDisponible());
    }
}
