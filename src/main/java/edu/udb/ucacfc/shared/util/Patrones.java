package edu.udb.ucacfc.shared.util;

/** Expresiones regulares reutilizadas por los DTO (constantes de compilacion: sirven dentro de @Pattern). */
public final class Patrones {
    private Patrones() {
    }

    /** Empieza con letra (acentos y enie incluidos); luego letras, espacios, apostrofes, puntos y guiones. */
    public static final String NOMBRE_PERSONA = "^[\\p{L}][\\p{L}'. -]*$";
    /** Telefono salvadoreno: 2xxx, 6xxx o 7xxx con guion. */
    public static final String TELEFONO_SV = "^[267]\\d{3}-\\d{4}$";
    /** DUI ########-# o NIT ####-######-###-# (o 14 digitos seguidos). */
    public static final String DUI_NIT = "^(\\d{8}-\\d|\\d{4}-\\d{6}-\\d{3}-\\d|\\d{14})$";
    /** Minimo 8 caracteres con al menos una letra y un numero. */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String MSG_PASSWORD = "La contrasena debe tener entre 8 y 72 caracteres, con al menos una letra y un numero";
}
