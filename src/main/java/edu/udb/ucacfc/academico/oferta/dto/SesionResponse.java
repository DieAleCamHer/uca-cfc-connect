package edu.udb.ucacfc.academico.oferta.dto;

import java.time.LocalDateTime;

public record SesionResponse(int numero, LocalDateTime inicio, LocalDateTime fin, double horas) {
}
