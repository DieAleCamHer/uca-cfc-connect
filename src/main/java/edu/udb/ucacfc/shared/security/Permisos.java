package edu.udb.ucacfc.shared.security;

/**
 * Expresiones de @PreAuthorize en un solo lugar (son constantes de compilacion: sirven dentro de la anotacion).
 * Cambiar quien puede hacer algo es cambiar UNA linea aqui, y la regla se lee sola en cada controller.
 */
public final class Permisos {
    private Permisos() {
    }

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String STAFF = "hasAnyRole('ADMIN','RECEPCIONISTA')";
    public static final String STAFF_Y_CLIENTE = "hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE')";
    public static final String FINANZAS = "hasAnyRole('ADMIN','CONTABILIDAD')";
    public static final String CAJA = "hasAnyRole('ADMIN','RECEPCIONISTA','CONTABILIDAD')";
    public static final String CAJA_Y_CLIENTE = "hasAnyRole('ADMIN','RECEPCIONISTA','CONTABILIDAD','CLIENTE')";
    public static final String CLIENTE = "hasRole('CLIENTE')";
    public static final String AUTENTICADO = "isAuthenticated()";
}
