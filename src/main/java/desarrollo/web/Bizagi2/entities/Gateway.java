package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("GATEWAY")
@Getter
@Setter
@NoArgsConstructor
public class Gateway extends NodoFlujo {

    @Enumerated(EnumType.STRING)
    private TipoGateway tipoGateway;
}
