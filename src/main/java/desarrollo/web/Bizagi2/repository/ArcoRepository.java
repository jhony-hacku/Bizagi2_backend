package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Arco;

public interface ArcoRepository extends JpaRepository<Arco, Integer> {

    List<Arco> findByPoolId(Integer poolId);

    boolean existsByPoolId(Integer poolId);

    boolean existsByOrigenIdAndDestinoId(Integer origenId, Integer destinoId);

    boolean existsByDestinoId(Integer destinoId);

    List<Arco> findByOrigenId(Integer origenId);

    List<Arco> findByOrigenIdOrDestinoId(Integer origenId, Integer destinoId);
}
