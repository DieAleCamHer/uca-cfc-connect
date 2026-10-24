package edu.udb.ucacfc.shared.util;

/** Limpieza de texto: trim que tambien quita el espacio de no separacion (U+00A0) que se cuela al copiar y pegar. */
public final class Textos {
    private Textos() {
    }

    public static String limpiar(String s) {
        if (s == null) {
            return null;
        }
        int ini = 0;
        int fin = s.length();
        while (ini < fin && esEspacio(s.charAt(ini))) {
            ini++;
        }
        while (fin > ini && esEspacio(s.charAt(fin - 1))) {
            fin--;
        }
        return s.substring(ini, fin);
    }

    /** null si viene vacio despues de limpiar: util para filtros opcionales. */
    public static String vacioANull(String s) {
        String l = limpiar(s);
        return l == null || l.isEmpty() ? null : l;
    }

    private static boolean esEspacio(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
