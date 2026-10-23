package edu.udb.ucacfc.academico.oferta;

import edu.udb.ucacfc.academico.categoria.CategoriaRepository;
import edu.udb.ucacfc.academico.docente.DocenteRepository;
import edu.udb.ucacfc.academico.modalidad.ModalidadRepository;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.inscripcion.InscripcionCascadaService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Agrupa los colaboradores comunes de CursoService y DiplomadoService (evita constructores de 10 parametros). */
@Getter
@Component
@RequiredArgsConstructor
public class OfertaDependencias {
    private final CategoriaRepository categorias;
    private final ModalidadRepository modalidades;
    private final DocenteRepository docentes;
    private final EspacioRepository espacios;
    private final CursoRepository cursos;
    private final DiplomadoRepository diplomados;
    private final AgendaService agenda;
    private final InscripcionCascadaService inscripciones;
    private final Clock clock;
}
