package edu.udb.ucacfc.cotizacion;

import java.util.EnumSet;
import java.util.Set;

/** Que se cotiza (lineamiento) y que tipos de linea admite cada uno. OTRO = cargo libre (transporte, material...). */
public enum TipoCotizacion {
    CURSO_EMPRESARIAL(EnumSet.of(TipoItemCotizacion.CURSO)),
    DIPLOMADO(EnumSet.of(TipoItemCotizacion.DIPLOMADO)),
    ALQUILER_ESPACIO(EnumSet.of(TipoItemCotizacion.ESPACIO)),
    CATERING(EnumSet.of(TipoItemCotizacion.CATERING)),
    SERVICIO_COMBINADO(EnumSet.of(TipoItemCotizacion.CURSO, TipoItemCotizacion.DIPLOMADO,
            TipoItemCotizacion.ESPACIO, TipoItemCotizacion.CATERING));

    private final Set<TipoItemCotizacion> principales;

    TipoCotizacion(Set<TipoItemCotizacion> principales) {
        this.principales = principales;
    }

    public Set<TipoItemCotizacion> getPrincipales() {
        return principales;
    }

    public boolean admite(TipoItemCotizacion t) {
        return t == TipoItemCotizacion.OTRO || principales.contains(t);
    }
}
