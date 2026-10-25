package edu.udb.ucacfc.catering;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServicioCateringRepository extends JpaRepository<ServicioCatering, Long>,
        JpaSpecificationExecutor<ServicioCatering> {
    boolean existsByTipoAndNombreIgnoreCase(TipoServicioCatering tipo, String nombre);

    boolean existsByTipoAndNombreIgnoreCaseAndIdNot(TipoServicioCatering tipo, String nombre, Long id);
}
