package edu.udb.ucacfc.cliente.dto;

import edu.udb.ucacfc.cliente.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClienteFiltro(
        @Schema(description = "Busca en nombre, empresa, DUI/NIT y correo") String q,
        TipoCliente tipo,
        Boolean activo,
        @Schema(description = "true = con cuenta de acceso, false = cliente de mostrador") Boolean conCuenta) {
}
