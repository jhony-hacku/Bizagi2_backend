package desarrollo.web.Bizagi2.dto.nodo;

import java.util.Set;

import desarrollo.web.Bizagi2.entities.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponse {
    private Integer id;
    private String nombre;
    private TipoEvento tipoEvento;
    private double posicionX;
    private double posicionY;
    private String contenido;
    private Boolean origenExterno;
    private String claveCorrelacion;
    private Set<Integer> actividadesUsuarias;
    private Integer poolId;
}
