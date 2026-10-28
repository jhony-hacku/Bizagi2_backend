package desarrollo.web.Bizagi2.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import desarrollo.web.Bizagi2.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // El filtro JWT carga al usuario autenticado fuera de una transaccion: su empresa (LAZY)
    // se trae en la misma consulta para que los servicios puedan leerla despues
    @EntityGraph(attributePaths = "empresa")
    Optional<Usuario> findConEmpresaById(Integer id);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByEmpresaId(Integer empresaId);
}
