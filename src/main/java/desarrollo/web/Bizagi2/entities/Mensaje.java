package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Flujo de mensaje: sale de un evento de lanzamiento (o una actividad de envio) y va hacia el pool destino
@Entity
@Table(name = "mensajes")
@SQLRestriction("activo = true")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Mensaje {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origen_id", nullable = false)
    private NodoFlujo origen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_pool_id", nullable = false)
    private Pool destinoPool;

    @Column(nullable = false)
    private String nombre;

    // Campos que se envian, con su tipo de dato
    @Column(length = 2000)
    private String contenido;

    // HU-26: solo cuando el destino es un sistema externo
    @Enumerated(EnumType.STRING)
    private TipoDestino tipoDestino;

    // HU-26: en que punto del proceso ocurre el envio
    private String momento;

    // HU-26: que debe ocurrir en el modelo si la notificacion falla
    @Enumerated(EnumType.STRING)
    private AccionFallo accionFallo;

    // HU-26: actividad de manejo de error a la que se deriva cuando accionFallo = DERIVAR_A_MANEJO_ERROR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_error_id")
    private Actividad actividadError;

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
