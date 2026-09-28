package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    List<Mensaje> findByProcesoId(Integer procesoId);

    List<Mensaje> findByOrigenId(Integer origenId);

    boolean existsByDestinoPoolId(Integer destinoPoolId);
}
