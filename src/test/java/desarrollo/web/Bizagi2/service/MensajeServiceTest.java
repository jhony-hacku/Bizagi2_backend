package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.AccionHistorial;
import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoDestino;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ReglaNegocioException;
import desarrollo.web.Bizagi2.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensajeServiceTest - Pruebas basadas en HU-25 y HU-26")
class MensajeServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private ProcesoService procesoService;

    @Mock
    private PoolService poolService;

    @Mock
    private NodoFlujoService nodoFlujoService;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private MensajeService mensajeService;

    private Usuario usuario;
    private Empresa empresa;
    private Proceso proceso;
    private Pool poolOrigen;
    private Pool poolDestino;
    private Evento eventoLanzamiento;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setRolAcceso(RolAcceso.EDITOR);
        usuario.setEmpresa(empresa);

        proceso = new Proceso();
        proceso.setId(20);
        proceso.setEmpresa(empresa);

        poolOrigen = new Pool();
        poolOrigen.setId(31);
        poolOrigen.setProceso(proceso);
        poolOrigen.setNombre("Empresa Principal");
        poolOrigen.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);

        poolDestino = new Pool();
        poolDestino.setId(32);
        poolDestino.setProceso(proceso);
        poolDestino.setNombre("Banco Receptor");
        poolDestino.setTipoParticipante(TipoParticipante.PROVEEDOR);

        eventoLanzamiento = new Evento();
        eventoLanzamiento.setId(100);
        eventoLanzamiento.setNombre("Enviar Notificación Pago");
        eventoLanzamiento.setTipoEvento(TipoEvento.MENSAJE_LANZAMIENTO);
        eventoLanzamiento.setPool(poolOrigen);
    }

    @Test
    @DisplayName("HU-25 CA1 & CA4: Crear flujo de mensaje entre pools distintos")
    void crear_mensajeEntrePoolsDistintos_exito() {
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(100, 1)).thenReturn(eventoLanzamiento);
        when(poolService.buscarPorId(32, 1)).thenReturn(poolDestino);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> {
            Mensaje m = i.getArgument(0);
            m.setId(1);
            return m;
        });

        Mensaje datos = new Mensaje();
        datos.setNombre("Notificación de Pago");
        datos.setOrigen(eventoLanzamiento);
        datos.setDestinoPool(poolDestino);
        datos.setContenido("monto: Double, radicado: String");

        Mensaje resultado = mensajeService.crear(20, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Notificación de Pago");
        assertThat(resultado.getOrigen()).isEqualTo(eventoLanzamiento);
        assertThat(resultado.getDestinoPool()).isEqualTo(poolDestino);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("MENSAJE"), eq(1), anyString());
    }

    @Test
    @DisplayName("HU-25 CA4: Un mensaje dentro del mismo pool es rechazado (debe ser un arco)")
    void crear_mensajeMismoPool_lanzaReglaNegocio() {
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(100, 1)).thenReturn(eventoLanzamiento);
        when(poolService.buscarPorId(31, 1)).thenReturn(poolOrigen); // mismo pool

        Mensaje datos = new Mensaje();
        datos.setNombre("Mensaje Interno");
        datos.setOrigen(eventoLanzamiento);
        datos.setDestinoPool(poolOrigen);

        assertThatThrownBy(() -> mensajeService.crear(20, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Un mensaje debe cruzar de un pool a otro. Dentro de un mismo pool use un arco");
    }

    @Test
    @DisplayName("HU-26 CA2: Hacia un sistema externo se requiere especificar el tipoDestino")
    void crear_sistemaExternoSinTipoDestino_lanzaReglaNegocio() {
        Pool poolExterno = new Pool();
        poolExterno.setId(33);
        poolExterno.setProceso(proceso);
        poolExterno.setNombre("Servidor de Correo");
        poolExterno.setTipoParticipante(TipoParticipante.SISTEMA_EXTERNO);

        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(100, 1)).thenReturn(eventoLanzamiento);
        when(poolService.buscarPorId(33, 1)).thenReturn(poolExterno);

        Mensaje datos = new Mensaje();
        datos.setNombre("Notificación Correo");
        datos.setOrigen(eventoLanzamiento);
        datos.setDestinoPool(poolExterno);
        // tipoDestino es null

        assertThatThrownBy(() -> mensajeService.crear(20, usuario, datos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Un mensaje hacia un sistema externo requiere tipoDestino");
    }

    @Test
    @DisplayName("HU-26 CA2: Crear mensaje a sistema externo con tipoDestino CORREO")
    void crear_sistemaExternoConTipoDestino_exito() {
        Pool poolExterno = new Pool();
        poolExterno.setId(33);
        poolExterno.setProceso(proceso);
        poolExterno.setNombre("Servidor de Correo");
        poolExterno.setTipoParticipante(TipoParticipante.SISTEMA_EXTERNO);

        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(nodoFlujoService.buscarNodo(100, 1)).thenReturn(eventoLanzamiento);
        when(poolService.buscarPorId(33, 1)).thenReturn(poolExterno);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> {
            Mensaje m = i.getArgument(0);
            m.setId(2);
            return m;
        });

        Mensaje datos = new Mensaje();
        datos.setNombre("Notificación Correo");
        datos.setOrigen(eventoLanzamiento);
        datos.setDestinoPool(poolExterno);
        datos.setTipoDestino(TipoDestino.CORREO);

        Mensaje creado = mensajeService.crear(20, usuario, datos);

        assertThat(creado.getTipoDestino()).isEqualTo(TipoDestino.CORREO);
    }

    @Test
    @DisplayName("Eliminar mensaje lo desactiva lógicamente (activo=false)")
    void eliminar_mensajeExistente_desactivaLogicamente() {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(1);
        mensaje.setProceso(proceso);
        mensaje.setActivo(true);

        when(mensajeRepository.findById(1)).thenReturn(Optional.of(mensaje));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);

        mensajeService.eliminar(1, usuario);

        assertThat(mensaje.isActivo()).isFalse();
        verify(mensajeRepository).save(mensaje);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.ELIMINAR), eq("MENSAJE"), eq(1), anyString());
    }
}
