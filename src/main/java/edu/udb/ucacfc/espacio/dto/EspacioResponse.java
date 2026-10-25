package edu.udb.ucacfc.espacio.dto;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.TipoEspacio;

import java.math.BigDecimal;

public record EspacioResponse(Long id, String nombre, TipoEspacio tipo, int capacidad, BigDecimal precioHora,
                              String equipamiento, String ubicacion, boolean activo) {
    public static EspacioResponse de(Espacio e) {
        return new EspacioResponse(e.getId(), e.getNombre(), e.getTipo(), e.getCapacidad(), e.getPrecioHora(),
                e.getEquipamiento(), e.getUbicacion(), e.isActivo());
    }
}
