package desarrollo.web.Bizagi2.dto.nodo;

import java.util.Set;

import desarrollo.web.Bizagi2.entities.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// En los eventos de mensaje se documentan contenido y claveCorrelacion.
// Solo en los de recepcion: origenExterno y actividadesUsuarias (ids de actividades del proceso).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento tipoEvento;
    // Opcional: si no viene, la posicion queda en 0
    private Double posicionX;
    private Double posicionY;
    private String contenido;
    private Boolean origenExterno;
    private String claveCorrelacion;
    private Set<Integer> actividadesUsuarias;
}
