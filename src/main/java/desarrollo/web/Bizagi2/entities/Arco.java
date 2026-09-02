package desarrollo.web.Bizagi2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "arcos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Arco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne
    @JoinColumn(name = "origen_id", nullable = false)
    private NodoFlujo origen;

    @ManyToOne
    @JoinColumn(name = "destino_id", nullable = false)
    private NodoFlujo destino;
}
