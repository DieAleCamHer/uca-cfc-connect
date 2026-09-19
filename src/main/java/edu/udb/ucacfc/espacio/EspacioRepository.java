package edu.udb.ucacfc.espacio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByDisponibleTrue();

    List<Espacio> findByTipo(TipoEspacio tipo);

    List<Espacio> findByCapacidadGreaterThanEqual(Integer capacidadMinima);
}
