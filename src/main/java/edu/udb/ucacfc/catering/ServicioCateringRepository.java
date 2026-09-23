package edu.udb.ucacfc.catering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioCateringRepository extends JpaRepository<ServicioCatering, Long> {

    List<ServicioCatering> findByTipo(TipoServicioCatering tipo);
}
