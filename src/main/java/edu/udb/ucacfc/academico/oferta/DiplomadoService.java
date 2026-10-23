package edu.udb.ucacfc.academico.oferta;

import org.springframework.stereotype.Service;

/** Toda la logica esta en OfertaAcademicaService; aqui solo se indica el tipo concreto. */
@Service
public class DiplomadoService extends OfertaAcademicaService<Diplomado> {

    public DiplomadoService(DiplomadoRepository repo, OfertaDependencias deps) {
        super(repo, deps);
    }

    @Override
    protected Diplomado nueva() {
        return new Diplomado();
    }

    @Override
    protected TipoOferta tipo() {
        return TipoOferta.DIPLOMADO;
    }

    @Override
    protected String etiqueta() {
        return "diplomado";
    }
}
