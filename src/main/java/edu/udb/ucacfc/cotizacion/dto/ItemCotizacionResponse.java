package edu.udb.ucacfc.cotizacion.dto;

import edu.udb.ucacfc.cotizacion.ItemCotizacion;
import edu.udb.ucacfc.cotizacion.TipoItemCotizacion;

import java.math.BigDecimal;

public record ItemCotizacionResponse(Long id, TipoItemCotizacion tipoItem, Long referenciaId, String descripcion,
                                     int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
    public static ItemCotizacionResponse de(ItemCotizacion i) {
        return new ItemCotizacionResponse(i.getId(), i.getTipoItem(), i.getReferenciaId(), i.getDescripcion(),
                i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal());
    }
}
