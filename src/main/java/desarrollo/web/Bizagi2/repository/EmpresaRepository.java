package desarrollo.web.Bizagi2.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByNit(String nit);

    Optional<Empresa> findByNit(String nit);
}
