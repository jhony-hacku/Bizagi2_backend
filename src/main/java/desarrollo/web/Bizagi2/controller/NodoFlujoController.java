package desarrollo.web.Bizagi2.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.service.NodoFlujoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// Actividades, gateways y eventos: los nodos del diagrama.
// Las eliminaciones responden con lo que se elimino y las advertencias que deja el diagrama.
@RestController
@RequiredArgsConstructor
@Tag(name = "Actividades, Gateways y Eventos")
public class NodoFlujoController {

    private final NodoFlujoService nodoFlujoService;

    // ---------- Actividades ----------

    @GetMapping("/api/pools/{poolId}/actividades")
    public ResponseEntity<List<Actividad>> listarActividades(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(nodoFlujoService.listarActividades(poolId, usuario.getEmpresa().getId()));
    }

    // Se crea dentro de una lane. Body: nombre, tipo (TAREA | USUARIO | MANUAL | SERVICIO | ENVIO),
    // descripcion, posicionX, posicionY. El responsable es el rol de la lane.
    @PostMapping("/api/lanes/{laneId}/actividades")
    public ResponseEntity<Actividad> crearActividad(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer laneId, @RequestBody Actividad datos) {
        Actividad creada = nodoFlujoService.crearActividad(laneId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/actividades/" + creada.getId())).body(creada);
    }

    @GetMapping("/api/actividades/{id}")
    public ResponseEntity<Actividad> buscarActividad(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.buscarActividad(id, usuario.getEmpresa().getId()));
    }

    // Se puede cambiar nombre, tipo, posicion y lane (otra lane del mismo pool)
    @PutMapping("/api/actividades/{id}")
    public ResponseEntity<Actividad> actualizarActividad(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Actividad datos) {
        return ResponseEntity.ok(nodoFlujoService.actualizarActividad(id, usuario, datos));
    }

    @DeleteMapping("/api/actividades/{id}")
    public ResponseEntity<Map<String, Object>> eliminarActividad(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.eliminarActividad(id, usuario));
    }

    // ---------- Gateways ----------

    @GetMapping("/api/pools/{poolId}/gateways")
    public ResponseEntity<List<Gateway>> listarGateways(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(nodoFlujoService.listarGateways(poolId, usuario.getEmpresa().getId()));
    }

    // Body: nombre, tipoGateway (EXCLUSIVA | PARALELA | INCLUSIVA), posicionX, posicionY
    @PostMapping("/api/pools/{poolId}/gateways")
    public ResponseEntity<Gateway> crearGateway(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @RequestBody Gateway datos) {
        Gateway creado = nodoFlujoService.crearGateway(poolId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/gateways/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/gateways/{id}")
    public ResponseEntity<Gateway> buscarGateway(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.buscarGateway(id, usuario.getEmpresa().getId()));
    }

    // Al pasar a PARALELA se eliminan las condiciones de sus arcos salientes
    @PutMapping("/api/gateways/{id}")
    public ResponseEntity<Gateway> actualizarGateway(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Gateway datos) {
        return ResponseEntity.ok(nodoFlujoService.actualizarGateway(id, usuario, datos));
    }

    @DeleteMapping("/api/gateways/{id}")
    public ResponseEntity<Map<String, Object>> eliminarGateway(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.eliminarGateway(id, usuario));
    }

    // ---------- Eventos ----------

    @GetMapping("/api/pools/{poolId}/eventos")
    public ResponseEntity<List<Evento>> listarEventos(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId) {
        return ResponseEntity.ok(nodoFlujoService.listarEventos(poolId, usuario.getEmpresa().getId()));
    }

    // Body: nombre, tipoEvento (INICIO | FIN | MENSAJE_LANZAMIENTO | MENSAJE_RECEPCION_INICIO |
    // MENSAJE_RECEPCION_INTERMEDIO), posicionX, posicionY. En los de mensaje el nombre es el nombre del mensaje y
    // se documentan contenido y claveCorrelacion. Solo en los de recepcion: origenExterno y actividadesUsuarias
    // (ids de las actividades que usan los datos recibidos).
    @PostMapping("/api/pools/{poolId}/eventos")
    public ResponseEntity<Evento> crearEvento(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer poolId, @RequestBody Evento datos) {
        Evento creado = nodoFlujoService.crearEvento(poolId, usuario, datos);
        return ResponseEntity.created(URI.create("/api/eventos/" + creado.getId())).body(creado);
    }

    @GetMapping("/api/eventos/{id}")
    public ResponseEntity<Evento> buscarEvento(@AuthenticationPrincipal Usuario usuario, @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.buscarEvento(id, usuario.getEmpresa().getId()));
    }

    @PutMapping("/api/eventos/{id}")
    public ResponseEntity<Evento> actualizarEvento(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id, @RequestBody Evento datos) {
        return ResponseEntity.ok(nodoFlujoService.actualizarEvento(id, usuario, datos));
    }

    @DeleteMapping("/api/eventos/{id}")
    public ResponseEntity<Map<String, Object>> eliminarEvento(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Integer id) {
        return ResponseEntity.ok(nodoFlujoService.eliminarEvento(id, usuario));
    }
}
