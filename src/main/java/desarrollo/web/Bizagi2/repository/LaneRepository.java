package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Lane;

public interface LaneRepository extends JpaRepository<Lane, Integer> {

    List<Lane> findByPoolIdOrderByOrdenAscIdAsc(Integer poolId);

    boolean existsByPoolId(Integer poolId);

    List<Lane> findByRolProcesoId(Integer rolProcesoId);
}
