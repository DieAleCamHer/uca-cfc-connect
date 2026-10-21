package edu.udb.ucacfc.catering.dto;

import edu.udb.ucacfc.catering.ServicioCatering;
import edu.udb.ucacfc.catering.TipoServicioCatering;

import java.math.BigDecimal;

public record ServicioCateringResponse(Long id, TipoServicioCatering tipo, String nombre, String menuBase,
                                       BigDecimal precioUnitario, boolean activo) {
    public static ServicioCateringResponse de(ServicioCatering s) {
        return new ServicioCateringResponse(s.getId(), s.getTipo(), s.getNombre(), s.getMenuBase(),
                s.getPrecioUnitario(), s.isActivo());
    }
}
