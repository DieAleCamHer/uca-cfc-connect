package edu.udb.ucacfc.catering.dto;

import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SolicitudCateringFiltro(
        @Schema(description = "Ignorado para CLIENTE") Long clienteId,
        Long servicioId,
        EstadoSolicitudCatering estado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
