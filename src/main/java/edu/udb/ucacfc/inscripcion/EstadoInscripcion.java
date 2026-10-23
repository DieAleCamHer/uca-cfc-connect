package edu.udb.ucacfc.inscripcion;

import java.util.EnumSet;
import java.util.Set;

/**
 * PENDIENTE  -> el participante pidio el cupo; falta que el personal la valide. OCUPA cupo.
 * CONFIRMADA -> el personal la valido; se genera automaticamente la cuenta por cobrar. OCUPA cupo.
 * FINALIZADA -> la oferta termino Y la cuenta esta PAGADA (automatico, o manual con las mismas condiciones). Final.
 * CANCELADA  -> no se realizara: libera cupo y anula la cuenta si no tiene dinero cobrado. Final.
 */
public enum EstadoInscripcion {
    PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA;

    public static final Set<EstadoInscripcion> VIGENTES = EnumSet.of(PENDIENTE, CONFIRMADA);
}
