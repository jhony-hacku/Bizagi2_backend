package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Clase padre de todo lo que se dibuja y se puede conectar con arcos: Actividad, Gateway y Evento
@Entity
@Table(name = "nodos_flujo")
@ConcreteProxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_nodo")
@SQLRestriction("activo = true")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class NodoFlujo {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @Column(nullable = false)
    private String nombre;
    private double posicionX;
    private double posicionY;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

    // Control de concurrencia optimista: si dos transacciones modifican la misma fila,
    // la segunda falla con OptimisticLockException en lugar de sobrescribir los cambios
    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private long version;
}
