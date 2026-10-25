package edu.udb.ucacfc.cotizacion;

import java.util.EnumSet;
import java.util.Set;

/**
 * PENDIENTE  -> solicitada (por el cliente o registrada por recepcion); precios sugeridos del catalogo.
 * EN_PROCESO -> el personal la esta elaborando: ajusta precios, descuento y vigencia.
 * APROBADA   -> el ADMIN la aprobo (lineamiento): se genera la cuenta por cobrar por el total. Inmutable.
 * RECHAZADA  -> rechazada por el personal, retirada por el cliente, o vencida automaticamente. Final.
 */
public enum EstadoCotizacion {
    PENDIENTE, EN_PROCESO, APROBADA, RECHAZADA;

    public static final Set<EstadoCotizacion> ABIERTAS = EnumSet.of(PENDIENTE, EN_PROCESO);
}
