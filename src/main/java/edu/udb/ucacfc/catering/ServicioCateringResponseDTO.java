package edu.udb.ucacfc.catering;

import java.math.BigDecimal;

public record ServicioCateringResponseDTO(Long id, TipoServicioCatering tipo, String nombre, BigDecimal precioUnitario) {
    public static ServicioCateringResponseDTO desde(ServicioCatering s) {
        return new ServicioCateringResponseDTO(s.getId(), s.getTipo(), s.getNombre(), s.getPrecioUnitario());
    }
}
