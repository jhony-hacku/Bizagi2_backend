package desarrollo.web.Bizagi2.dto.pool;

import desarrollo.web.Bizagi2.entities.TipoParticipante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolResponse {
    private Integer id;
    private String nombre;
    private TipoParticipante tipoParticipante;
    private Integer procesoId;
}
