package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.pago.dto.AbonoFiltro;
import edu.udb.ucacfc.pago.dto.AbonoRequest;
import edu.udb.ucacfc.pago.dto.AbonoResponse;
import edu.udb.ucacfc.pago.dto.ComprobanteResponse;
import edu.udb.ucacfc.pago.dto.PagoFiltro;
import edu.udb.ucacfc.pago.dto.PagoResponse;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.MotivoObligatorioRequest;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "16. Pagos", description = "Cuentas por cobrar (las genera el sistema al confirmar/aprobar un proceso) y sus abonos. "
        + "PENDIENTE -> PARCIAL -> PAGADO; ANULADO si el proceso se cancela sin dinero. Un CLIENTE solo ve lo suyo.")
public class PagoController {

    private final PagoService service;

    @GetMapping
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Listar cuentas (RF15 estado de pagos)",
            description = "Filtros: estado, tipoReferencia, clienteId (ignorado para CLIENTE), desde/hasta, q, conSaldo. "
                    + "sort: id, estado, montoTotal, montoPagado, tipoReferencia, fechaGeneracion, cliente.")
    public PageResponse<PagoResponse> listar(@ParameterObject PagoFiltro filtro, @ParameterObject PaginaRequest pagina,
                                             @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listar(filtro, pagina, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Ver cuenta con todos sus abonos")
    public PagoResponse obtener(@PathVariable @Positive Long id,
                                @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @GetMapping("/referencia")
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Cuentas de un proceso", description = "Ej. ?tipo=INSCRIPCION&referenciaId=5 (incluye anuladas).")
    public List<PagoResponse> porReferencia(@RequestParam @NotNull TipoReferenciaPago tipo,
                                            @RequestParam @NotNull @Positive Long referenciaId,
                                            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.porReferencia(tipo, referenciaId, usuario);
    }

    @PostMapping("/{id}/abonos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Registrar un abono (pago parcial o total)",
            description = "No puede exceder el saldo (contando lo que esta en validacion). TRANSFERENCIA/DEPOSITO exigen "
                    + "referencia unica. CLIENTE: solo TRANSFERENCIA/DEPOSITO de sus cuentas -> PENDIENTE_VALIDACION. "
                    + "RECEPCIONISTA: EFECTIVO/TARJETA quedan VALIDADOS (confirmados en caja). CONTABILIDAD/ADMIN: VALIDADO. "
                    + "Si la cuenta queda PAGADA y el proceso ya termino, este se cierra automaticamente.")
    public AbonoResponse registrarAbono(@PathVariable @Positive Long id, @Valid @RequestBody AbonoRequest request,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.registrarAbono(id, request, usuario);
    }

    @GetMapping("/abonos")
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Listar abonos", description = "estado=PENDIENTE_VALIDACION es la bandeja de Contabilidad. "
            + "Filtros: estado, metodo, pagoId, desde/hasta. sort: id, monto, estado, metodo, fechaRegistro, fechaValidacion.")
    public PageResponse<AbonoResponse> abonos(@ParameterObject AbonoFiltro filtro, @ParameterObject PaginaRequest pagina,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listarAbonos(filtro, pagina, usuario);
    }

    @PatchMapping("/abonos/{id}/validar")
    @PreAuthorize(Permisos.FINANZAS)
    @Operation(summary = "Validar transferencia/deposito (ADMIN, CONTABILIDAD)",
            description = "PENDIENTE_VALIDACION -> VALIDADO: suma al pagado y emite comprobante.")
    public AbonoResponse validar(@PathVariable @Positive Long id,
                                 @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.validarAbono(id, usuario);
    }

    @PatchMapping("/abonos/{id}/rechazar")
    @PreAuthorize(Permisos.FINANZAS)
    @Operation(summary = "Rechazar transferencia/deposito (ADMIN, CONTABILIDAD)", description = "El dinero no se encontro; motivo obligatorio.")
    public AbonoResponse rechazar(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                  @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.rechazarAbono(id, body.motivo(), usuario);
    }

    @PatchMapping("/abonos/{id}/anular")
    @PreAuthorize(Permisos.FINANZAS)
    @Operation(summary = "Anular un abono validado por error (ADMIN, CONTABILIDAD)",
            description = "Resta el monto y recalcula el estado. 409 si el proceso ya fue cerrado.")
    public AbonoResponse anular(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.anularAbono(id, body.motivo(), usuario);
    }

    @GetMapping("/abonos/{id}/comprobante")
    @PreAuthorize(Permisos.CAJA_Y_CLIENTE)
    @Operation(summary = "Comprobante de pago", description = "Solo abonos VALIDADOS. Un CLIENTE solo los suyos.")
    public ComprobanteResponse comprobante(@PathVariable @Positive Long id,
                                           @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.comprobante(id, usuario);
    }
}
