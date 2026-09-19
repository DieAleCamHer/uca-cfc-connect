package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendaServiceImpl implements AgendaService {

    private final ActividadAgendaRepository actividadAgendaRepository;

    public AgendaServiceImpl(ActividadAgendaRepository actividadAgendaRepository) {
        this.actividadAgendaRepository = actividadAgendaRepository;
    }

    @Override
    @Transactional
    public ActividadAgenda registrarActividad(TipoActividad tipo, Long referenciaId, String titulo,
                                               LocalDateTime inicio, LocalDateTime fin, Espacio espacio) {
        if (espacio != null) {
            List<ActividadAgenda> solapadas =
                    actividadAgendaRepository.findSolapadasPorEspacio(espacio.getId(), inicio, fin);
            if (!solapadas.isEmpty()) {
                throw new EspacioOcupadoException(
                        "El espacio '" + espacio.getNombre() + "' ya esta ocupado entre "
                                + inicio + " y " + fin + " por otra actividad");
            }
        }

        ActividadAgenda actividad = ActividadAgenda.builder()
                .tipo(tipo)
                .referenciaId(referenciaId)
                .titulo(titulo)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .espacio(espacio)
                .build();

        return actividadAgendaRepository.save(actividad);
    }

    @Override
    @Transactional
    public void eliminarPorReferencia(TipoActividad tipo, Long referenciaId) {
        List<ActividadAgenda> encontradas =
                actividadAgendaRepository.findByTipoAndReferenciaId(tipo, referenciaId);
        actividadAgendaRepository.deleteAll(encontradas);
    }

    @Override
    public List<ActividadAgenda> listarEntreFechas(LocalDate desde, LocalDate hasta) {
        // "desde" a las 00:00:00, "hasta" al final del dia (usamos el inicio
        // del dia SIGUIENTE como limite exclusivo, asi el dia "hasta" completo
        // queda incluido, sin importar la hora de cada actividad).
        LocalDateTime inicioRango = desde.atStartOfDay();
        LocalDateTime finRango = hasta.plusDays(1).atStartOfDay();
        return actividadAgendaRepository.findEntreFechas(inicioRango, finRango);
    }

    @Override
    public List<ActividadAgenda> listarPorTipo(TipoActividad tipo) {
        return actividadAgendaRepository.findByTipo(tipo);
    }
}
