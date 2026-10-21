package edu.udb.ucacfc.agenda.dto;

import edu.udb.ucacfc.agenda.EstadoEvento;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EventoFiltro(String q, EstadoEvento estado, Long espacioId,
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
