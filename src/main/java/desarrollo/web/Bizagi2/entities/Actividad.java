package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ACTIVIDAD")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Actividad extends NodoFlujo {

    @ManyToOne
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @ManyToOne
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso;

    private String descripcion;
}
