package desarrollo.web.Bizagi2.dto.proceso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Un item de la validacion del diagrama. nivel: ERROR (impide publicar) o ADVERTENCIA
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertenciaResponse {
    private String nivel;
    private String elemento;
    private Integer elementoId;
    private String mensaje;
}
