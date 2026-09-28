package desarrollo.web.Bizagi2.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Correlacion;

public interface CorrelacionRepository extends JpaRepository<Correlacion, Integer> {

    Optional<Correlacion> findByMensajeId(Integer mensajeId);
}
