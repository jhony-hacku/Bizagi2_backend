package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("GATEWAY")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Gateway extends NodoFlujo {

    @Enumerated(EnumType.STRING)
    private TipoGateway tipoGateway;
}
