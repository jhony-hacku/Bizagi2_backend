package desarrollo.web.Bizagi2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.NodoFlujo;

public interface NodoFlujoRepository extends JpaRepository<NodoFlujo, Integer> {

    List<NodoFlujo> findByPoolId(Integer poolId);

    boolean existsByPoolId(Integer poolId);

    @Query("select a from Actividad a where a.pool.id = :poolId")
    List<Actividad> findActividadesByPool(@Param("poolId") Integer poolId);

    @Query("select g from Gateway g where g.pool.id = :poolId")
    List<Gateway> findGatewaysByPool(@Param("poolId") Integer poolId);

    @Query("select e from Evento e where e.pool.id = :poolId")
    List<Evento> findEventosByPool(@Param("poolId") Integer poolId);

    @Query("select count(a) from Actividad a where a.lane.id = :laneId")
    long contarActividadesPorLane(@Param("laneId") Integer laneId);

    // HU-08: el nombre de una actividad es unico dentro del proceso (excluirId evita compararla consigo misma)
    @Query("""
            select count(a) from Actividad a
            where a.pool.proceso.id = :procesoId
              and lower(a.nombre) = lower(:nombre)
              and a.id <> :excluirId
            """)
    long contarActividadesConNombre(@Param("procesoId") Integer procesoId, @Param("nombre") String nombre,
            @Param("excluirId") Integer excluirId);
}
