package desarrollo.web.Bizagi2.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.ProcesoCompartido;

public interface ProcesoCompartidoRepository extends JpaRepository<ProcesoCompartido, Integer> {

    Optional<ProcesoCompartido> findByProcesoIdAndEmpresaId(Integer procesoId, Integer empresaId);

    boolean existsByProcesoIdAndEmpresaIdAndActivoTrue(Integer procesoId, Integer empresaId);

    List<ProcesoCompartido> findByProcesoIdAndActivoTrue(Integer procesoId);

    List<ProcesoCompartido> findByEmpresaIdAndActivoTrue(Integer empresaId);
}
