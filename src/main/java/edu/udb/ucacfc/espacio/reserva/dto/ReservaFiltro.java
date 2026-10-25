package edu.udb.ucacfc.espacio.reserva.dto;

import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReservaFiltro(
        @Schema(description = "Ignorado para CLIENTE") Long clienteId,
        Long espacioId,
        EstadoReserva estado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
