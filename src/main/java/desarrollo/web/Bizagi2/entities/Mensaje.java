package desarrollo.web.Bizagi2.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Flujo de mensaje: sale de un evento de lanzamiento (o una actividad de envio) y va hacia el pool destino
@Entity
@Table(name = "mensajes")
@SQLRestriction("activo = true")
@Data
@NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @ManyToOne
    @JoinColumn(name = "origen_id", nullable = false)
    private NodoFlujo origen;

    @ManyToOne
    @JoinColumn(name = "destino_pool_id", nullable = false)
    private Pool destinoPool;

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

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;
}
