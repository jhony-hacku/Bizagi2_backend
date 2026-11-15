package desarrollo.web.Bizagi2.dto.proceso;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta al eliminar nodos o arcos: que mas se elimino y que advierte ahora el editor
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EliminacionResponse {
    private int arcosEliminados;
    private int mensajesEliminados;
    private List<AdvertenciaResponse> advertencias;
}
