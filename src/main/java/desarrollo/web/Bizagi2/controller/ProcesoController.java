package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.EstadoProceso;
import desarrollo.web.Bizagi2.entities.Historial;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.DiagramaService;
import desarrollo.web.Bizagi2.service.ProcesoCompartidoService;
import desarrollo.web.Bizagi2.service.ProcesoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/procesos")
@RequiredArgsConstructor
@Tag(name = "Procesos")
public class ProcesoController {

    private final ProcesoService procesoService;
    private final DiagramaService diagramaService;
    private final ProcesoCompartidoService procesoCompartidoService;

    // Paginado. Filtros opcionales: ?nombre=credito&estado=PUBLICADO&categoria=Finanzas&page=0&size=20
    // Con ?activo=false se consultan los procesos eliminados.
    @GetMapping
    public ResponseEntity<PagedModel<Proceso>> listar(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "true") boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PagedModel<>(
                procesoService.listar(usuario.getEmpresa().getId(), nombre, estado, categoria, activo, page, size)));
    }

    // Tambien funciona con un proceso que otra empresa compartio con la tuya (solo lectura)
    @GetMapping("/{id}")
    public ResponseEntity<Proceso> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(procesoService.buscarParaLectura(id, usuario.getEmpresa().getId()));
    }

    // Procesos que otras empresas compartieron con la tuya
    @GetMapping("/compartidos")
    public ResponseEntity<List<Map<String, Object>>> compartidosConmigo(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(procesoCompartidoService.compartidosConmigo(usuario.getEmpresa().getId()));
    }

    // El diagrama completo: pools, lanes, actividades, gateways, arcos, roles y mensajes
    @GetMapping("/{id}/diagrama")
    public ResponseEntity<Map<String, Object>> diagrama(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(diagramaService.obtener(id, usuario.getEmpresa().getId()));
    }

    // Advertencias y errores del diagrama. Los errores impiden publicar el proceso.
    @GetMapping("/{id}/validacion")
    public ResponseEntity<List<Map<String, Object>>> validacion(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(procesoService.validar(id, usuario.getEmpresa().getId()));
    }

    // HU-23: empresas con las que se comparte el proceso, y como compartirlo o dejar de hacerlo (solo ADMINISTRADOR)
    @GetMapping("/{id}/compartir")
    public ResponseEntity<List<Map<String, Object>>> invitadas(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(procesoCompartidoService.invitadas(id, usuario.getEmpresa().getId()));
    }

    // Body: nit (de la empresa invitada). Queda en solo lectura para ella.
    @PostMapping("/{id}/compartir")
    public ResponseEntity<Map<String, Object>> compartir(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Map<String, String> datos) {
        return ResponseEntity.status(201).body(procesoCompartidoService.compartir(id, usuario, datos.get("nit")));
    }

    @DeleteMapping("/{id}/compartir/{empresaId}")
    public ResponseEntity<Void> dejarDeCompartir(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @PathVariable Integer empresaId) {
        procesoCompartidoService.dejarDeCompartir(id, usuario, empresaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<Historial>> historial(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(procesoService.historial(id, usuario.getEmpresa().getId()));
    }

    // Nace en BORRADOR y con el pool de la empresa ya creado
    @PostMapping
    public ResponseEntity<Proceso> crear(@AuthenticationPrincipal Usuario usuario, @RequestBody Proceso datos) {
        Proceso creado = procesoService.crear(usuario, datos);
        return ResponseEntity.created(URI.create("/api/procesos/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proceso> actualizar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id,
            @RequestBody Proceso datos) {
        return ResponseEntity.ok(procesoService.actualizar(id, usuario, datos));
    }

    // Eliminacion logica: el proceso queda inactivo. Solo el ADMINISTRADOR (ver SecurityConfig).
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        procesoService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
