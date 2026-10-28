package desarrollo.web.Bizagi2.dto.diagrama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaArcoResponse {
    private Integer id;
    private Integer origenId;
    private Integer destinoId;
    private String etiqueta;
    private String condicion;
}
