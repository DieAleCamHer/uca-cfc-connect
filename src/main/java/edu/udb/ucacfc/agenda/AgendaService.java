package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendaService {

    /**
     * Registra una actividad en la agenda institucional. Si la actividad
     * usa un espacio fisico, valida primero que no haya cruce de horario
     * con otra actividad en ese mismo espacio (lanza EspacioOcupadoException
     * si lo hay). La usan Cursos/Diplomados presenciales, Alquileres,
     * Catering en sitio y Eventos.
     */
    ActividadAgenda registrarActividad(TipoActividad tipo, Long referenciaId, String titulo,
                                        LocalDateTime inicio, LocalDateTime fin, Espacio espacio);

    void eliminarPorReferencia(TipoActividad tipo, Long referenciaId);

    List<ActividadAgenda> listarEntreFechas(LocalDate desde, LocalDate hasta);

    List<ActividadAgenda> listarPorTipo(TipoActividad tipo);
}
