package edu.udb.ucacfc.espacio;

import java.math.BigDecimal;

public record EspacioResponseDTO(
        Long id, String nombre, TipoEspacio tipo, Integer capacidad,
        BigDecimal precio, String equipamiento, boolean disponible
) {
    public static EspacioResponseDTO desde(Espacio e) {
        return new EspacioResponseDTO(e.getId(), e.getNombre(), e.getTipo(), e.getCapacidad(),
                e.getPrecio(), e.getEquipamiento(), e.isDisponible());
    }
}
