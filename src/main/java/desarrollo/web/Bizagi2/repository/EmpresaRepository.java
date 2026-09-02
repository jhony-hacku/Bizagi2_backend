package desarrollo.web.Bizagi2.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
}
