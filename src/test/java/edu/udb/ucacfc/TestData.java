package edu.udb.ucacfc;

import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj fijo: "hoy" es el 17/10/2026 a las 09:00 en El Salvador, asi las pruebas no dependen del dia real. */
public final class TestData {
    public static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T15:00:00Z"), ZONA);
    public static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);

    private TestData() {
    }

    public static Usuario usuario(long id, Rol rol) {
        return Usuario.builder().id(id).nombre(rol.name().toLowerCase()).email(rol.name().toLowerCase() + "@test.sv")
                .password("x").rol(rol).build();
    }
}
