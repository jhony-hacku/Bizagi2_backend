package desarrollo.web.Bizagi2.dto.diagrama;

import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramaCorrelacionResponse {
    private String criterio;
    private AccionSinCaso accionSinCaso;
}
