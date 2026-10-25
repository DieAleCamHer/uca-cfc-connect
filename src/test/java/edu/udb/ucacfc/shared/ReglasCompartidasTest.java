package edu.udb.ucacfc.shared;

import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.exception.TransicionInvalidaException;
import edu.udb.ucacfc.shared.util.Horarios;
import edu.udb.ucacfc.shared.util.MaquinaEstados;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasCompartidasTest {

    final LocalDateTime t8 = LocalDateTime.of(2026, 11, 2, 8, 0);

    @Test
    @DisplayName("Rangos pegados no se cruzan; con un minuto de traslape si")
    void cruces() {
        assertThat(Horarios.seCruzan(t8, t8.plusHours(2), t8.plusHours(2), t8.plusHours(4))).isFalse();
        assertThat(Horarios.seCruzan(t8, t8.plusHours(2), t8.plusMinutes(119), t8.plusHours(4))).isTrue();
    }

    @Test
    @DisplayName("Maquina de estados: explica que transiciones si se permiten")
    void maquina() {
        MaquinaEstados<EstadoInscripcion> m = MaquinaEstados.de("inscripcion", EstadoInscripcion.class)
                .permitir(EstadoInscripcion.PENDIENTE, EstadoInscripcion.CONFIRMADA);
        m.validar(EstadoInscripcion.PENDIENTE, EstadoInscripcion.CONFIRMADA);
        assertThatThrownBy(() -> m.validar(EstadoInscripcion.CANCELADA, EstadoInscripcion.PENDIENTE))
                .isInstanceOf(TransicionInvalidaException.class).hasMessageContaining("estado final");
    }

    @Test
    @DisplayName("Paginacion: lista blanca de campos, alias anidados y limites de size")
    void paginacion() {
        Map<String, String> sort = Map.of("id", "id", "cliente", "cliente.nombre");
        Pageable p = PaginacionUtil.crear(new PaginaRequest(1, 20, "cliente,desc"), sort, "id");
        assertThat(p.getPageNumber()).isEqualTo(1);
        assertThat(p.getSort().getOrderFor("cliente.nombre").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThatThrownBy(() -> PaginacionUtil.crear(new PaginaRequest(0, 10, "password"), sort, "id"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> PaginacionUtil.crear(new PaginaRequest(0, 500, null), sort, "id"))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
