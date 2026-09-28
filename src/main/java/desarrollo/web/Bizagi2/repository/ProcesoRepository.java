package desarrollo.web.Bizagi2.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Proceso;

public interface ProcesoRepository extends JpaRepository<Proceso, Integer> {

    // Los filtros nunca llegan como null (PostgreSQL no puede deducir el tipo de un parametro null):
    // nombre y categoria vacios significan "sin filtro", y filtrarEstado indica si se aplica el estado.
    @Query("""
            select p from Proceso p
            where p.empresa.id = :empresaId
              and p.activo = :activo
              and lower(p.nombre) like lower(concat('%', :nombre, '%'))
              and (:filtrarEstado = false or p.estado = :estado)
              and (:categoria = '' or lower(p.categoria) = lower(:categoria))
            """)
    Page<Proceso> buscar(@Param("empresaId") Integer empresaId,
            @Param("activo") boolean activo,
            @Param("nombre") String nombre,
            @Param("filtrarEstado") boolean filtrarEstado,
            @Param("estado") EstadoProceso estado,
            @Param("categoria") String categoria,
            Pageable pageable);

    boolean existsByEmpresaIdAndActivoTrueAndNombreIgnoreCase(Integer empresaId, String nombre);

    boolean existsByEmpresaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(Integer empresaId, String nombre, Integer id);
}
