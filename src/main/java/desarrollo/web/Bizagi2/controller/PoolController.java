package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.dto.pool.PoolRequest;
import desarrollo.web.Bizagi2.dto.pool.PoolResponse;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.PoolService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pools")
public class PoolController {

    private final PoolService poolService;

    @GetMapping("/api/procesos/{procesoId}/pools")
    public ResponseEntity<List<PoolResponse>> listar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer procesoId) {
        return ResponseEntity.ok(poolService.listarPorProcesoDto(procesoId, usuario.getEmpresa().getId()));
    }

    // Body: nombre, tipoParticipante (EMPRESA_PROPIETARIA | CLIENTE | PROVEEDOR | SISTEMA_EXTERNO).
    // Los participantes externos son cajas negras: no admiten lanes ni elementos.
    @PostMapping("/api/procesos/{procesoId}/pools")
    public ResponseEntity<PoolResponse> crear(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer procesoId, @Valid @RequestBody PoolRequest datos) {
        PoolResponse creado = poolService.crear(procesoId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/pools/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/pools/{id}")
    public ResponseEntity<PoolResponse> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(poolService.buscarPorIdDto(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/pools/{id}")
    public ResponseEntity<PoolResponse> actualizar(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @Valid @RequestBody PoolRequest datos) {
        return ResponseEntity.ok(poolService.actualizar(id, usuario, datos));
    }

    @DeleteMapping("/api/pools/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        poolService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
