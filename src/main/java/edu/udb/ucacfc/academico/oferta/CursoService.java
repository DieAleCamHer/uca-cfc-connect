package edu.udb.ucacfc.academico.oferta;

import org.springframework.stereotype.Service;

/** Toda la logica esta en OfertaAcademicaService; aqui solo se indica el tipo concreto. */
@Service
public class CursoService extends OfertaAcademicaService<Curso> {

    public CursoService(CursoRepository repo, OfertaDependencias deps) {
        super(repo, deps);
    }

    @Override
    protected Curso nueva() {
        return new Curso();
    }

    @Override
    protected TipoOferta tipo() {
        return TipoOferta.CURSO;
    }

    @Override
    protected String etiqueta() {
        return "curso";
    }
}
