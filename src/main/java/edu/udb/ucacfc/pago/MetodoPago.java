package edu.udb.ucacfc.pago;

/** EFECTIVO y TARJETA se cobran en caja; TRANSFERENCIA y DEPOSITO exigen numero de referencia y validacion. */
public enum MetodoPago {
    EFECTIVO, TARJETA, TRANSFERENCIA, DEPOSITO;

    public boolean requiereValidacion() {
        return this == TRANSFERENCIA || this == DEPOSITO;
    }
}
