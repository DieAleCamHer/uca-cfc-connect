package edu.udb.ucacfc.catering.dto;

import edu.udb.ucacfc.catering.TipoServicioCatering;

public record ServicioCateringFiltro(String q, TipoServicioCatering tipo, Boolean activo) {
}
