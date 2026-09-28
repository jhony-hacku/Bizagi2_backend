package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Historial;

public interface HistorialRepository extends JpaRepository<Historial, Integer> {

    List<Historial> findByProcesoIdOrderByFechaDescIdDesc(Integer procesoId);

    List<Historial> findByEntidadAndEntidadIdOrderByFechaDescIdDesc(String entidad, Integer entidadId);
}
