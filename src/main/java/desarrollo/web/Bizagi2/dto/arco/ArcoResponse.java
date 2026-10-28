package desarrollo.web.Bizagi2.dto.arco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcoResponse {
    private Integer id;
    private Integer poolId;
    private Integer origenId;
    private String origenNombre;
    private Integer destinoId;
    private String destinoNombre;
    private String etiqueta;
    private String condicion;
}
