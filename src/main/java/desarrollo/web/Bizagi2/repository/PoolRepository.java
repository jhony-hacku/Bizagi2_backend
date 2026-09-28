package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Pool;

public interface PoolRepository extends JpaRepository<Pool, Integer> {

    List<Pool> findByProcesoId(Integer procesoId);
}
