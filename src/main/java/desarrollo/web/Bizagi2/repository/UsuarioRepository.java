package desarrollo.web.Bizagi2.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import desarrollo.web.Bizagi2.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // El filtro JWT carga al usuario autenticado fuera de una transaccion: su empresa (LAZY)
    // se trae en la misma consulta para que los servicios puedan leerla despues
    @EntityGraph(attributePaths = "empresa")
    Optional<Usuario> findConEmpresaById(Integer id);

    // Login: bloquea la fila desde la comprobacion de la contrasena hasta emitir el refresh token
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findByEmail(String email);

    // Todas las escrituras del usuario y de sus sesiones toman este mismo bloqueo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Integer id);

    boolean existsByEmail(String email);

    List<Usuario> findByEmpresaId(Integer empresaId);
}
