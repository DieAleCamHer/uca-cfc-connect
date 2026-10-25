package edu.udb.ucacfc.shared.util;

import edu.udb.ucacfc.shared.exception.TransicionInvalidaException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados generica: declara que transiciones permite cada estado y lanza
 * TransicionInvalidaException (409) con un mensaje que explica que SI se puede hacer.
 * La usan inscripciones, reservas, catering y cotizaciones.
 */
public final class MaquinaEstados<E extends Enum<E>> {

    private final String entidad;
    private final Map<E, Set<E>> transiciones;

    private MaquinaEstados(String entidad, Class<E> tipo) {
        this.entidad = entidad;
        this.transiciones = new EnumMap<>(tipo);
        for (E e : tipo.getEnumConstants()) {
            transiciones.put(e, EnumSet.noneOf(tipo));
        }
    }

    public static <E extends Enum<E>> MaquinaEstados<E> de(String entidad, Class<E> tipo) {
        return new MaquinaEstados<>(entidad, tipo);
    }

    @SafeVarargs
    public final MaquinaEstados<E> permitir(E desde, E... hacia) {
        for (E h : hacia) {
            transiciones.get(desde).add(h);
        }
        return this;
    }

    public boolean puede(E actual, E nuevo) {
        return transiciones.get(actual).contains(nuevo);
    }

    public Set<E> siguientes(E actual) {
        return transiciones.get(actual);
    }

    public void validar(E actual, E nuevo) {
        if (puede(actual, nuevo)) {
            return;
        }
        Set<E> posibles = transiciones.get(actual);
        if (posibles.isEmpty()) {
            throw new TransicionInvalidaException("La " + entidad + " esta " + actual
                    + " (estado final) y ya no puede cambiar de estado");
        }
        throw new TransicionInvalidaException("No se puede pasar una " + entidad + " de " + actual + " a " + nuevo
                + ". Desde " + actual + " solo se permite: " + posibles);
    }
}
