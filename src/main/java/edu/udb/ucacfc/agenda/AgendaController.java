package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// Solo lectura: los registros de agenda se crean/eliminan indirectamente
// desde los otros modulos (Curso, ReservaEspacio, SolicitudCatering, etc),
// nunca directamente por un usuario.
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    // "desde"/"hasta" se reciben como texto y se parsean a mano (en vez de
    // dejar que Spring lo convierta automaticamente con @DateTimeFormat):
    // es mas explicito, mas facil de depurar, y da un mensaje de error
    // claro (400 con OperacionInvalidaException) en vez de un 500 generico
    // si alguien manda el formato mal.
    @GetMapping
    public List<ActividadAgendaResponseDTO> listarEntreFechas(
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDate fechaDesde = parsearFecha(desde, "desde");
        LocalDate fechaHasta = parsearFecha(hasta, "hasta");
        return agendaService.listarEntreFechas(fechaDesde, fechaHasta).stream()
                .map(ActividadAgendaResponseDTO::desde).toList();
    }

    @GetMapping("/tipo/{tipo}")
    public List<ActividadAgendaResponseDTO> listarPorTipo(@PathVariable TipoActividad tipo) {
        return agendaService.listarPorTipo(tipo).stream().map(ActividadAgendaResponseDTO::desde).toList();
    }

    private LocalDate parsearFecha(String valor, String nombreCampo) {
        try {
            return LocalDate.parse(valor); // espera formato AAAA-MM-DD (ISO-8601)
        } catch (DateTimeParseException ex) {
            throw new OperacionInvalidaException(
                    "El parametro '" + nombreCampo + "' tiene un formato de fecha invalido: '" + valor
                            + "'. Usa AAAA-MM-DD, ej: 2026-09-01");
        }
    }
}
