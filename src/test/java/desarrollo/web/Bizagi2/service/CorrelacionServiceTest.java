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
import desarrollo.web.Bizagi2.entities.AccionSinCaso;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.RolAcceso;
import desarrollo.web.Bizagi2.entities.Usuario;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelacionServiceTest - Pruebas basadas en HU-28")
class CorrelacionServiceTest {

    @Mock
    private CorrelacionRepository correlacionRepository;

    @Mock
    private MensajeService mensajeService;

    @Mock
    private ProcesoService procesoService;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private CorrelacionService correlacionService;

    private Usuario usuario;
    private Empresa empresa;
    private Proceso proceso;
    private Mensaje mensaje;

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

        mensaje = new Mensaje();
        mensaje.setId(100);
        mensaje.setNombre("Confirmación");
        mensaje.setProceso(proceso);
    }

    @Test
    @DisplayName("HU-28 CA1 & CA5: Crear clave de correlación con criterio y acción cuando no hay caso")
    void crear_correlacionValida_exito() {
        when(mensajeService.buscarPorId(100, 1)).thenReturn(mensaje);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(correlacionRepository.findByMensajeId(100)).thenReturn(Optional.empty());
        when(correlacionRepository.save(any(Correlacion.class))).thenAnswer(i -> {
            Correlacion c = i.getArgument(0);
            c.setId(1);
            return c;
        });

        Correlacion datos = new Correlacion();
        datos.setCriterio("numeroRadicado");
        datos.setAccionSinCaso(AccionSinCaso.INICIAR_CASO_NUEVO);

        Correlacion resultado = correlacionService.crear(100, usuario, datos);

        assertThat(resultado.getId()).isEqualTo(1);
        assertThat(resultado.getCriterio()).isEqualTo("numeroRadicado");
        assertThat(resultado.getAccionSinCaso()).isEqualTo(AccionSinCaso.INICIAR_CASO_NUEVO);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.CREAR), eq("CORRELACION"), eq(1), anyString());
    }

    @Test
    @DisplayName("HU-28: Si el mensaje ya tiene clave de correlación, crear otra lanza ConflictoDominioException")
    void crear_correlacionExistente_lanzaConflicto() {
        when(mensajeService.buscarPorId(100, 1)).thenReturn(mensaje);
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(correlacionRepository.findByMensajeId(100)).thenReturn(Optional.of(new Correlacion()));

        Correlacion datos = new Correlacion();
        datos.setCriterio("nitCliente");
        datos.setAccionSinCaso(AccionSinCaso.DESCARTAR);

        assertThatThrownBy(() -> correlacionService.crear(100, usuario, datos))
                .isInstanceOf(ConflictoDominioException.class)
                .hasMessageContaining("El mensaje ya tiene una clave de correlacion");
    }

    @Test
    @DisplayName("Actualizar clave de correlación existente")
    void actualizar_correlacionValida_exito() {
        Correlacion existente = new Correlacion();
        existente.setId(1);
        existente.setMensaje(mensaje);
        existente.setCriterio("viejoCriterio");
        existente.setAccionSinCaso(AccionSinCaso.DESCARTAR);

        when(correlacionRepository.findByMensajeId(100)).thenReturn(Optional.of(existente));
        when(procesoService.buscarActivo(20, 1)).thenReturn(proceso);
        when(correlacionRepository.save(any(Correlacion.class))).thenAnswer(i -> i.getArgument(0));

        Correlacion datos = new Correlacion();
        datos.setCriterio("nuevoCriterio");
        datos.setAccionSinCaso(AccionSinCaso.INICIAR_CASO_NUEVO);

        Correlacion resultado = correlacionService.actualizar(100, usuario, datos);

        assertThat(resultado.getCriterio()).isEqualTo("nuevoCriterio");
        assertThat(resultado.getAccionSinCaso()).isEqualTo(AccionSinCaso.INICIAR_CASO_NUEVO);
        verify(historialService).registrar(eq(usuario), eq(proceso), eq(AccionHistorial.EDITAR), eq("CORRELACION"), eq(1), anyString());
    }

    @Test
    @DisplayName("Obtener correlación inexistente lanza RecursoNoEncontradoException")
    void obtener_inexistente_lanzaRecursoNoEncontrado() {
        when(correlacionRepository.findByMensajeId(100)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> correlacionService.obtener(100, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("El mensaje no tiene clave de correlacion");
    }
}
