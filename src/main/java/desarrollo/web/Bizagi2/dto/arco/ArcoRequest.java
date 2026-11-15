package desarrollo.web.Bizagi2.dto.arco;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Origen y destino: actividades, gateways o eventos del mismo pool.
// condicion solo aplica si el arco sale de un gateway exclusivo o inclusivo.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcoRequest {
    @NotNull(message = "El elemento de origen es obligatorio")
    private Integer origenId;

    @NotNull(message = "El elemento de destino es obligatorio")
    private Integer destinoId;
    private String etiqueta;
    private String condicion;
}
