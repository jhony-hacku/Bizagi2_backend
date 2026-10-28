package desarrollo.web.Bizagi2.dto.nodo;

import desarrollo.web.Bizagi2.entities.TipoGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de gateway es obligatorio")
    private TipoGateway tipoGateway;
    // Opcional: si no viene, la posicion queda en 0
    private Double posicionX;
    private Double posicionY;
}
