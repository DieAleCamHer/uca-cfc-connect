package edu.udb.ucacfc.reporte;

import edu.udb.ucacfc.reporte.dto.FinancieroResponse;
import edu.udb.ucacfc.reporte.dto.ResumenResponse;
import edu.udb.ucacfc.shared.security.Permisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "17. Reportes", description = "Resumen operativo (ADMIN) y estado financiero (ADMIN, CONTABILIDAD)")
public class ReporteController {

    private final ReporteService service;

    @GetMapping("/resumen")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Resumen general (ADMIN)", description = "Totales activos, procesos por estado y ofertas con mas inscritos.")
    public ResumenResponse resumen() {
        return service.resumen();
    }

    @GetMapping("/financiero")
    @PreAuthorize(Permisos.FINANZAS)
    @Operation(summary = "Estado financiero (ADMIN, CONTABILIDAD)",
            description = "Por defecto el mes en curso. Facturado, recaudado (por metodo), pendiente de cobro, en validacion, "
                    + "facturado por tipo de proceso y cuentas por estado.")
    public FinancieroResponse financiero(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.financiero(desde, hasta);
    }
}
