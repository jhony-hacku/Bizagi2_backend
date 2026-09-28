package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import desarrollo.web.Bizagi2.entities.RolProceso;

public interface RolProcesoRepository extends JpaRepository<RolProceso, Integer> {

    // nombre vacio significa "sin filtro"
    @Query("""
            select r from RolProceso r
            where r.empresa.id = :empresaId
              and lower(r.nombre) like lower(concat('%', :nombre, '%'))
            """)
    Page<RolProceso> buscar(@Param("empresaId") Integer empresaId, @Param("nombre") String nombre,
            Pageable pageable);

    List<RolProceso> findByEmpresaIdOrderByNombre(Integer empresaId);

    boolean existsByEmpresaIdAndNombreIgnoreCase(Integer empresaId, String nombre);

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndIdNot(Integer empresaId, String nombre, Integer id);
}
