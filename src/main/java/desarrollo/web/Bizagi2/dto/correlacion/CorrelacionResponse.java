package desarrollo.web.Bizagi2.dto.correlacion;

import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrelacionResponse {
    private Integer id;
    private Integer mensajeId;
    private String criterio;
    private AccionSinCaso accionSinCaso;
}
