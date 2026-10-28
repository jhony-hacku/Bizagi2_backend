package desarrollo.web.Bizagi2.dto.nodo;

import desarrollo.web.Bizagi2.entities.TipoGateway;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponse {
    private Integer id;
    private String nombre;
    private TipoGateway tipoGateway;
    private double posicionX;
    private double posicionY;
    private Integer poolId;
}
