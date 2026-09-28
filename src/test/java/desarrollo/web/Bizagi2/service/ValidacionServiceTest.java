package desarrollo.web.Bizagi2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import desarrollo.web.Bizagi2.entities.Actividad;
import desarrollo.web.Bizagi2.entities.Arco;
import desarrollo.web.Bizagi2.entities.Correlacion;
import desarrollo.web.Bizagi2.entities.Evento;
import desarrollo.web.Bizagi2.entities.Gateway;
import desarrollo.web.Bizagi2.entities.Mensaje;
import desarrollo.web.Bizagi2.entities.NodoFlujo;
import desarrollo.web.Bizagi2.entities.Pool;
import desarrollo.web.Bizagi2.entities.Proceso;
import desarrollo.web.Bizagi2.entities.TipoActividad;
import desarrollo.web.Bizagi2.entities.TipoEvento;
import desarrollo.web.Bizagi2.entities.TipoGateway;
import desarrollo.web.Bizagi2.entities.TipoParticipante;
import desarrollo.web.Bizagi2.repository.ArcoRepository;
import desarrollo.web.Bizagi2.repository.CorrelacionRepository;
import desarrollo.web.Bizagi2.repository.MensajeRepository;
import desarrollo.web.Bizagi2.repository.NodoFlujoRepository;
import desarrollo.web.Bizagi2.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidacionServiceTest - Reglas de consistencia del modelador BPMN (HU-10, 13, 14, 15, 25, 27, 28)")
class ValidacionServiceTest {

    private static final String ERROR = ValidacionService.ERROR;
    private static final String ADVERTENCIA = ValidacionService.ADVERTENCIA;

    @Mock
    private PoolRepository poolRepository;

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;

    @Mock
    private ArcoRepository arcoRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private CorrelacionRepository correlacionRepository;

    @InjectMocks
    private ValidacionService validacionService;

    private Proceso proceso;
    private Pool poolEmpresa;

    @BeforeEach
    void setUp() {
        proceso = new Proceso();
        proceso.setId(1);
        proceso.setNombre("Proceso Test");

        poolEmpresa = new Pool();
        poolEmpresa.setId(10);
        poolEmpresa.setProceso(proceso);
        poolEmpresa.setNombre("Empresa");
        poolEmpresa.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Pool poolExterno() {
        Pool p = new Pool();
        p.setId(11);
        p.setProceso(proceso);
        p.setNombre("Cliente");
        // Toma cualquier valor distinto de EMPRESA_PROPIETARIA, sin depender de su
        // nombre
        p.setTipoParticipante(Arrays.stream(TipoParticipante.values())
                .filter(t -> t != TipoParticipante.EMPRESA_PROPIETARIA)
                .findFirst().orElseThrow());
        return p;
    }

    private Actividad actividad(int id, String nombre) {
        Actividad a = new Actividad();
        a.setId(id);
        a.setNombre(nombre);
        a.setTipo(TipoActividad.USUARIO);
        a.setPool(poolEmpresa);
        return a;
    }

    private Gateway gateway(int id, TipoGateway tipo) {
        Gateway g = new Gateway();
        g.setId(id);
        g.setNombre("Decision");
        g.setTipoGateway(tipo);
        g.setPool(poolEmpresa);
        return g;
    }

    private Evento evento(int id, String nombre, TipoEvento tipo) {
        Evento e = new Evento();
        e.setId(id);
        e.setNombre(nombre);
        e.setTipoEvento(tipo);
        e.setPool(poolEmpresa);
        return e;
    }

    private Evento receptor(int id, String nombre, TipoEvento tipo, String clave, Boolean origenExterno) {
        Evento e = evento(id, nombre, tipo);
        e.setClaveCorrelacion(clave);
        e.setOrigenExterno(origenExterno);
        return e;
    }

    private Arco arco(int id, NodoFlujo origen, NodoFlujo destino, String condicion) {
        Arco a = new Arco();
        a.setId(id);
        a.setOrigen(origen);
        a.setDestino(destino);
        a.setCondicion(condicion);
        return a;
    }

    private Mensaje mensaje(int id, String nombre, Pool destino) {
        Mensaje m = new Mensaje();
        m.setId(id);
        m.setNombre(nombre);
        m.setDestinoPool(destino);
        return m;
    }

    private void correlacion(Mensaje m, String criterio) {
        Correlacion c = new Correlacion();
        c.setCriterio(criterio);
        when(correlacionRepository.findByMensajeId(m.getId())).thenReturn(Optional.of(c));
    }

    /** Valida un proceso con un unico pool de la empresa. */
    private List<Map<String, Object>> validarCon(List<Arco> arcos, List<Mensaje> mensajes, NodoFlujo... nodos) {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));
        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(nodos));
        when(arcoRepository.findByPoolId(10)).thenReturn(arcos);
        when(mensajeRepository.findByProcesoId(1)).thenReturn(mensajes);
        return validacionService.validar(proceso);
    }

    /** Gateway con una entrada y dos salidas con las condiciones indicadas. */
    private List<Map<String, Object>> validarGatewayDivergente(TipoGateway tipo, String c1, String c2) {
        Gateway g = gateway(200, tipo);
        Arco entrada = arco(501, actividad(100, "Entrada"), g, null);
        Arco salida1 = arco(502, g, actividad(101, "S1"), c1);
        Arco salida2 = arco(503, g, actividad(102, "S2"), c2);
        return validarCon(List.of(entrada, salida1, salida2), List.of(), g);
    }

    private List<Map<String, Object>> con(List<Map<String, Object>> items, String fragmento) {
        return items.stream().filter(i -> i.get("mensaje").toString().contains(fragmento)).toList();
    }

    private void assertItem(Map<String, Object> item, String nivel, String elemento, Integer id) {
        assertThat(item)
                .containsEntry("nivel", nivel)
                .containsEntry("elemento", elemento)
                .containsEntry("elementoId", id);
    }

    // =====================================================================
    // validar: estructura general y conexiones (HU-10, HU-13)
    // =====================================================================

    @Test
    @DisplayName("Un diagrama bien formado (Inicio -> Actividad -> Fin) no genera ningun item")
    void validar_diagramaValido_noGeneraItems() {
        Evento inicio = evento(1, "Inicio", TipoEvento.INICIO);
        Actividad tarea = actividad(2, "Tarea");
        Evento fin = evento(3, "Fin", TipoEvento.FIN);

        List<Map<String, Object>> items = validarCon(
                List.of(arco(10, inicio, tarea, null), arco(11, tarea, fin, null)), List.of(),
                inicio, tarea, fin);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Los pools de participantes externos (caja negra) no se validan ni se consultan")
    void validar_poolExterno_seOmite() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolExterno()));
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of());

        List<Map<String, Object>> items = validacionService.validar(proceso);

        assertThat(items).isEmpty();
        verify(nodoFlujoRepository, never()).findByPoolId(anyInt());
        verify(arcoRepository, never()).findByPoolId(anyInt());
    }

    @Test
    @DisplayName("HU-10 & HU-13: Detecta elementos desconectados (sin camino de entrada o salida)")
    void validar_elementosDesconectados_generaAdvertencias() {
        Actividad aislada = actividad(101, "Tarea Desconectada");

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), aislada);

        assertThat(items).hasSize(2);
        List<Map<String, Object>> entrada = con(items, "no tiene camino de entrada");
        List<Map<String, Object>> salida = con(items, "no tiene camino de salida");
        assertThat(entrada).hasSize(1);
        assertThat(salida).hasSize(1);
        assertItem(entrada.get(0), ADVERTENCIA, "ACTIVIDAD", 101);
        assertItem(salida.get(0), ADVERTENCIA, "ACTIVIDAD", 101);
        assertThat(entrada.get(0).get("mensaje")).isEqualTo("'Tarea Desconectada' no tiene camino de entrada");
    }

    @Test
    @DisplayName("HU-10: Un evento INICIO sin arcos entrantes no advierte de entrada, solo de salida")
    void validar_eventoInicioSinEntrada_soloAdvierteSalida() {
        Evento inicio = evento(1, "Inicio", TipoEvento.INICIO);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), inicio);

        assertThat(items).hasSize(1);
        assertItem(items.get(0), ADVERTENCIA, "EVENTO", 1);
        assertThat(items.get(0).get("mensaje").toString()).contains("no tiene camino de salida");
    }

    @Test
    @DisplayName("HU-27: Un Message Catch de inicio sin entrantes tampoco advierte de entrada")
    void validar_mensajeRecepcionInicioSinEntrada_noAdvierteEntrada() {
        Evento catchInicio = receptor(1, "Solicitud", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", true);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), catchInicio);

        assertThat(con(items, "no tiene camino de entrada")).isEmpty();
        assertThat(con(items, "no tiene camino de salida")).hasSize(1);
    }

    @Test
    @DisplayName("HU-10: Un evento FIN sin arcos salientes no advierte de salida, solo de entrada")
    void validar_eventoFinSinSalida_soloAdvierteEntrada() {
        Evento fin = evento(3, "Fin", TipoEvento.FIN);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), fin);

        assertThat(items).hasSize(1);
        assertItem(items.get(0), ADVERTENCIA, "EVENTO", 3);
        assertThat(items.get(0).get("mensaje").toString()).contains("no tiene camino de entrada");
    }

    @Test
    @DisplayName("Los items indican el tipo de elemento correcto: ACTIVIDAD, GATEWAY o EVENTO")
    void validar_tipoDeElemento_seInformaSegunLaClaseDelNodo() {
        Actividad a = actividad(1, "Act");
        Gateway g = gateway(2, TipoGateway.PARALELA);
        Evento e = evento(3, "Intermedio", TipoEvento.MENSAJE_LANZAMIENTO);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), a, g, e);

        assertThat(con(items, "'Act' no tiene camino de entrada").get(0)).containsEntry("elemento", "ACTIVIDAD");
        assertThat(con(items, "'Decision' no tiene camino de entrada").get(0)).containsEntry("elemento", "GATEWAY");
        assertThat(con(items, "'Intermedio' no tiene camino de entrada").get(0)).containsEntry("elemento", "EVENTO");
    }

    // =====================================================================
    // validar: Message Catch de inicio (HU-27)
    // =====================================================================

    @Test
    @DisplayName("HU-27 CA5: Message Catch de inicio con arcos entrantes genera ERROR bloqueante")
    void validar_messageCatchInicioConEntrantes_generaError() {
        Evento catchInicio = receptor(300, "Inicio por Mensaje", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", true);
        Arco entrante = arco(500, actividad(99, "Origen"), catchInicio, null);

        List<Map<String, Object>> items = validarCon(List.of(entrante), List.of(), catchInicio);

        List<Map<String, Object>> errores = con(items, "no puede tener arcos entrantes");
        assertThat(errores).hasSize(1);
        assertItem(errores.get(0), ERROR, "EVENTO", 300);
    }

    @Test
    @DisplayName("HU-27 CA5: errores() devuelve el texto del error del Message Catch de inicio")
    void errores_messageCatchInicioConEntrantes_devuelveTextoDelError() {
        Evento catchInicio = receptor(300, "Inicio por Mensaje", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", true);
        Arco entrante = arco(500, actividad(99, "Origen"), catchInicio, null);
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));
        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(catchInicio));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of(entrante));

        List<String> errores = validacionService.errores(proceso);

        assertThat(errores).containsExactly(
                "El Message Catch de inicio 'Inicio por Mensaje' no puede tener arcos entrantes");
    }

    @Test
    @DisplayName("HU-27: Un Message Catch INTERMEDIO con arcos entrantes es valido (solo el de inicio lo prohibe)")
    void validar_messageCatchIntermedioConEntrantes_noGeneraError() {
        Evento intermedio = receptor(300, "Esperar respuesta", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "clave", true);
        Arco entrante = arco(500, actividad(99, "Origen"), intermedio, null);

        List<Map<String, Object>> items = validarCon(List.of(entrante), List.of(), intermedio);

        assertThat(items).noneMatch(i -> ERROR.equals(i.get("nivel")));
    }

    // =====================================================================
    // errores() y resultadoEliminacion()
    // =====================================================================

    @Test
    @DisplayName("errores() devuelve solo los ERROR y descarta las ADVERTENCIA")
    void errores_conErroresYAdvertencias_devuelveSoloErrores() {
        Actividad aislada = actividad(1, "Aislada"); // genera solo ADVERTENCIAS
        Gateway sinSalidas = gateway(2, TipoGateway.PARALELA); // genera ERROR (menos de dos salidas)
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));
        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(aislada, sinSalidas));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of());

        List<String> errores = validacionService.errores(proceso);

        assertThat(errores).containsExactly("El gateway 'Decision' debe tener al menos dos arcos salientes");
    }

    @Test
    @DisplayName("errores() sin problemas devuelve lista vacia")
    void errores_sinProblemas_devuelveVacio() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of());

        assertThat(validacionService.errores(proceso)).isEmpty();
    }

    @Test
    @DisplayName("resultadoEliminacion informa los conteos y las advertencias, en ese orden")
    void resultadoEliminacion_sinAdvertencias_devuelveConteosYListaVacia() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of());

        Map<String, Object> resultado = validacionService.resultadoEliminacion(proceso, 3, 2);

        assertThat(resultado.keySet()).containsExactly("arcosEliminados", "mensajesEliminados", "advertencias");
        assertThat(resultado).containsEntry("arcosEliminados", 3).containsEntry("mensajesEliminados", 2);
        assertThat((List<?>) resultado.get("advertencias")).isEmpty();
    }

    @Test
    @DisplayName("resultadoEliminacion incluye las advertencias que deja el diagrama tras eliminar")
    void resultadoEliminacion_conDiagramaIncompleto_incluyeAdvertencias() {
        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa));
        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(actividad(1, "Huerfana")));
        when(arcoRepository.findByPoolId(10)).thenReturn(List.of());

        Map<String, Object> resultado = validacionService.resultadoEliminacion(proceso, 1, 0);

        assertThat(resultado).containsEntry("arcosEliminados", 1).containsEntry("mensajesEliminados", 0);
        assertThat((List<?>) resultado.get("advertencias")).hasSize(2);
    }

    // =====================================================================
    // validarGateway (HU-14, HU-15)
    // =====================================================================

    @Test
    @DisplayName("HU-14 CA3: Gateway sin arcos salientes genera ERROR de divergencia")
    void validar_gatewayConMenosDeDosSalientes_generaError() {
        Gateway g = gateway(200, TipoGateway.EXCLUSIVA);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), g);

        List<Map<String, Object>> errores = con(items, "debe tener al menos dos arcos salientes");
        assertThat(errores).hasSize(1);
        assertItem(errores.get(0), ERROR, "GATEWAY", 200);
    }

    @Test
    @DisplayName("HU-14: Gateway con una entrada y una salida no es divergencia ni convergencia -> ERROR")
    void validar_gatewayUnaEntradaUnaSalida_generaError() {
        Gateway g = gateway(200, TipoGateway.PARALELA);
        Arco entrada = arco(501, actividad(100, "Entrada"), g, null);
        Arco salida = arco(502, g, actividad(101, "Salida"), null);

        List<Map<String, Object>> items = validarCon(List.of(entrada, salida), List.of(), g);

        assertThat(con(items, "debe tener al menos dos arcos salientes")).hasSize(1);
    }

    @Test
    @DisplayName("HU-14: Gateway de convergencia (2 entradas, 1 salida) es valido")
    void validar_gatewayConvergencia_noGeneraError() {
        Gateway g = gateway(200, TipoGateway.PARALELA);
        Arco entrada1 = arco(501, actividad(100, "E1"), g, null);
        Arco entrada2 = arco(502, actividad(101, "E2"), g, null);
        Arco salida = arco(503, g, actividad(102, "S"), null);

        List<Map<String, Object>> items = validarCon(List.of(entrada1, entrada2, salida), List.of(), g);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("HU-14: Gateway con 2 entradas y ninguna salida sigue siendo ERROR")
    void validar_gatewayDosEntradasSinSalida_generaError() {
        Gateway g = gateway(200, TipoGateway.PARALELA);
        Arco entrada1 = arco(501, actividad(100, "E1"), g, null);
        Arco entrada2 = arco(502, actividad(101, "E2"), g, null);

        List<Map<String, Object>> items = validarCon(List.of(entrada1, entrada2), List.of(), g);

        assertThat(con(items, "debe tener al menos dos arcos salientes")).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = TipoGateway.class, names = { "EXCLUSIVA", "INCLUSIVA" })
    @DisplayName("HU-14 CA3 & CA4: Gateway exclusivo/inclusivo con salidas sin condicion genera un ERROR por arco")
    void validar_gatewayCondicionalSinCondiciones_generaErrorPorCadaArco(TipoGateway tipo) {
        List<Map<String, Object>> items = validarGatewayDivergente(tipo, null, "");

        List<Map<String, Object>> errores = con(items, "debe tener una condicion");
        assertThat(errores).hasSize(2);
        assertItem(errores.get(0), ERROR, "ARCO", 502);
        assertItem(errores.get(1), ERROR, "ARCO", 503);
        assertThat(errores.get(0).get("mensaje"))
                .isEqualTo("El arco 502 que sale del gateway 'Decision' debe tener una condicion");
    }

    @Test
    @DisplayName("HU-14: Solo el arco sin condicion recibe el error; el que la tiene no")
    void validar_gatewayInclusivoUnArcoSinCondicion_erroraSoloEseArco() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.INCLUSIVA, "monto > 100", "   ");

        List<Map<String, Object>> errores = con(items, "debe tener una condicion");
        assertThat(errores).hasSize(1);
        assertItem(errores.get(0), ERROR, "ARCO", 503);
    }

    @Test
    @DisplayName("HU-14: Gateway paralelo no exige condiciones en sus arcos")
    void validar_gatewayParaleloSinCondiciones_noGeneraItems() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.PARALELA, null, null);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("HU-14/15: Gateway exclusivo con condiciones distintas es valido")
    void validar_gatewayExclusivoConCondicionesDistintas_noGeneraItems() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.EXCLUSIVA, "monto > 100",
                "monto <= 100");

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("HU-15: Gateway exclusivo con condiciones repetidas (ignorando mayusculas y espacios) genera ADVERTENCIA")
    void validar_gatewayExclusivoCondicionesRepetidas_generaAdvertencia() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.EXCLUSIVA, "Monto > 100",
                "  monto > 100 ");

        List<Map<String, Object>> repetidas = con(items, "tiene condiciones repetidas");
        assertThat(repetidas).hasSize(1);
        assertItem(repetidas.get(0), ADVERTENCIA, "GATEWAY", 200);
        assertThat(items).noneMatch(i -> ERROR.equals(i.get("nivel")));
    }

    @Test
    @DisplayName("HU-15: Condiciones vacias en un exclusivo no cuentan como 'repetidas' (solo son error de condicion)")
    void validar_gatewayExclusivoCondicionesVacias_noSeConsideranRepetidas() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.EXCLUSIVA, null, null);

        assertThat(con(items, "tiene condiciones repetidas")).isEmpty();
        assertThat(con(items, "debe tener una condicion")).hasSize(2);
    }

    @Test
    @DisplayName("HU-15: La regla de condiciones repetidas solo aplica a gateways exclusivos, no a inclusivos")
    void validar_gatewayInclusivoCondicionesRepetidas_noGeneraAdvertencia() {
        List<Map<String, Object>> items = validarGatewayDivergente(TipoGateway.INCLUSIVA, "monto > 100", "monto > 100");

        assertThat(items).isEmpty();
    }

    // =====================================================================
    // validarMensajes: mensaje sin receptor (HU-25)
    // =====================================================================

    @Test
    @DisplayName("HU-25: Mensaje hacia un pool interno sin Message Catch con su nombre -> ADVERTENCIA sin receptor")
    void validar_mensajeAPoolInternoSinReceptor_generaAdvertencia() {
        Mensaje m = mensaje(700, "Factura", poolEmpresa);
        correlacion(m, "nroFactura");

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m));

        List<Map<String, Object>> sinReceptor = con(items, "queda sin receptor");
        assertThat(sinReceptor).hasSize(1);
        assertItem(sinReceptor.get(0), ADVERTENCIA, "MENSAJE", 700);
        assertThat(sinReceptor.get(0).get("mensaje"))
                .isEqualTo("El mensaje 'Factura' queda sin receptor en el pool 'Empresa'");
    }

    @Test
    @DisplayName("HU-25: Mensaje con receptor de mismo nombre (sin importar mayusculas/espacios) en el pool destino -> sin advertencia")
    void validar_mensajeConReceptorEnPoolDestino_noAdvierte() {
        Mensaje m = mensaje(700, "Factura", poolEmpresa);
        correlacion(m, "nroFactura");
        Evento receptor = receptor(300, "  factura ", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "nroFactura", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        assertThat(con(items, "queda sin receptor")).isEmpty();
        assertThat(con(items, "No existe un mensaje lanzado")).isEmpty();
        assertThat(con(items, "no coincide")).isEmpty();
    }

    @Test
    @DisplayName("HU-25: Un receptor con el mismo nombre pero en OTRO pool no cuenta como receptor del mensaje")
    void validar_receptorEnOtroPool_mensajeQuedaSinReceptor() {
        Pool otroPool = new Pool();
        otroPool.setId(12);
        otroPool.setProceso(proceso);
        otroPool.setNombre("Otra");
        otroPool.setTipoParticipante(TipoParticipante.EMPRESA_PROPIETARIA);

        Mensaje m = mensaje(700, "Factura", otroPool);
        correlacion(m, "nroFactura");
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "nroFactura", false);

        when(poolRepository.findByProcesoId(1)).thenReturn(List.of(poolEmpresa, otroPool));
        when(nodoFlujoRepository.findByPoolId(10)).thenReturn(List.of(receptor));
        when(mensajeRepository.findByProcesoId(1)).thenReturn(List.of(m));

        List<Map<String, Object>> items = validacionService.validar(proceso);

        List<Map<String, Object>> sinReceptor = con(items, "queda sin receptor");
        assertThat(sinReceptor).hasSize(1);
        assertThat(sinReceptor.get(0).get("mensaje"))
                .isEqualTo("El mensaje 'Factura' queda sin receptor en el pool 'Otra'");
    }

    @Test
    @DisplayName("HU-26: Mensaje hacia un pool externo (caja negra) no exige receptor")
    void validar_mensajeAPoolExterno_noExigeReceptor() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        correlacion(m, "nroFactura");

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m));

        assertThat(items).isEmpty();
    }

    // =====================================================================
    // validarMensajes: correlacion del mensaje (HU-28)
    // =====================================================================

    @Test
    @DisplayName("HU-28: Mensaje sin clave de correlacion -> ADVERTENCIA")
    void validar_mensajeSinCorrelacion_generaAdvertencia() {
        Mensaje m = mensaje(700, "Factura", poolExterno());

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m));

        assertThat(items).hasSize(1);
        assertItem(items.get(0), ADVERTENCIA, "MENSAJE", 700);
        assertThat(items.get(0).get("mensaje")).isEqualTo("El mensaje 'Factura' no tiene clave de correlacion");
    }

    @Test
    @DisplayName("HU-28: Dos mensajes con mismo nombre y misma clave (ignorando mayusculas/espacios) son ambiguos")
    void validar_mensajesConMismoNombreYClave_sonAmbiguos() {
        Mensaje m1 = mensaje(700, "Factura", poolExterno());
        Mensaje m2 = mensaje(701, " factura ", poolExterno());
        correlacion(m1, "nroFactura");
        correlacion(m2, "NROFACTURA ");

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m1, m2));

        List<Map<String, Object>> ambiguos = con(items, "es ambiguo");
        assertThat(ambiguos).hasSize(2);
        assertItem(ambiguos.get(0), ADVERTENCIA, "MENSAJE", 700);
        assertItem(ambiguos.get(1), ADVERTENCIA, "MENSAJE", 701);
    }

    @Test
    @DisplayName("HU-28: Dos mensajes con mismo nombre pero distinta clave no son ambiguos")
    void validar_mensajesMismoNombreDistintaClave_noSonAmbiguos() {
        Mensaje m1 = mensaje(700, "Factura", poolExterno());
        Mensaje m2 = mensaje(701, "Factura", poolExterno());
        correlacion(m1, "nroFactura");
        correlacion(m2, "nroPedido");

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m1, m2));

        assertThat(items).isEmpty();
    }

    // =====================================================================
    // validarMensajes: eventos de recepcion (HU-27, HU-28)
    // =====================================================================

    @Test
    @DisplayName("HU-27: Recepcion sin mensaje lanzado con ese nombre y sin origen externo -> ADVERTENCIA")
    void validar_recepcionSinLanzamientoNiOrigenExterno_generaAdvertencia() {
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", null);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), receptor);

        List<Map<String, Object>> sinOrigen = con(items, "No existe un mensaje lanzado");
        assertThat(sinOrigen).hasSize(1);
        assertItem(sinOrigen.get(0), ADVERTENCIA, "EVENTO", 300);
        assertThat(sinOrigen.get(0).get("mensaje")).isEqualTo(
                "No existe un mensaje lanzado con el nombre 'Factura' ni se marco el evento como de origen externo");
    }

    @Test
    @DisplayName("HU-27: origenExterno=false se trata igual que nulo: sigue exigiendo mensaje lanzado")
    void validar_recepcionOrigenExternoFalse_generaAdvertencia() {
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), receptor);

        assertThat(con(items, "No existe un mensaje lanzado")).hasSize(1);
    }

    @Test
    @DisplayName("HU-27: Recepcion marcada como de origen externo no exige mensaje lanzado")
    void validar_recepcionConOrigenExterno_noAdvierte() {
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INICIO, "clave", true);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), receptor);

        assertThat(con(items, "No existe un mensaje lanzado")).isEmpty();
    }

    @Test
    @DisplayName("HU-28: Message Catch INTERMEDIO sin clave (nula o en blanco) -> ADVERTENCIA")
    void validar_recepcionIntermediaSinClave_generaAdvertencia() {
        Evento sinClaveNula = receptor(300, "Uno", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, null, true);
        Evento sinClaveBlanca = receptor(301, "Dos", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "   ", true);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), sinClaveNula, sinClaveBlanca);

        List<Map<String, Object>> sinClave = con(items, "Message Catch intermedio");
        assertThat(sinClave).hasSize(2);
        assertItem(sinClave.get(0), ADVERTENCIA, "EVENTO", 300);
        assertItem(sinClave.get(1), ADVERTENCIA, "EVENTO", 301);
        assertThat(sinClave.get(0).get("mensaje"))
                .isEqualTo("El Message Catch intermedio 'Uno' no tiene clave de correlacion");
    }

    @Test
    @DisplayName("HU-28: La exigencia de clave aplica solo al Message Catch intermedio, no al de inicio")
    void validar_recepcionInicioSinClave_noAdvierteClave() {
        Evento receptor = receptor(300, "Solicitud", TipoEvento.MENSAJE_RECEPCION_INICIO, null, true);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(), receptor);

        assertThat(con(items, "Message Catch intermedio")).isEmpty();
        assertThat(con(items, "clave de correlacion")).isEmpty();
    }

    @Test
    @DisplayName("HU-28: La clave del lanzamiento y la de la recepcion deben coincidir -> ADVERTENCIA si difieren")
    void validar_clavesDistintasEntreLanzamientoYRecepcion_generaAdvertencia() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        correlacion(m, "nroFactura");
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "nroPedido", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        List<Map<String, Object>> noCoincide = con(items, "no coincide");
        assertThat(noCoincide).hasSize(1);
        assertItem(noCoincide.get(0), ADVERTENCIA, "EVENTO", 300);
        assertThat(noCoincide.get(0).get("mensaje")).isEqualTo(
                "La clave de correlacion de 'Factura' (nroPedido) no coincide con la del mensaje lanzado (nroFactura)");
    }

    @Test
    @DisplayName("HU-28: Claves iguales ignorando mayusculas y espacios no generan advertencia")
    void validar_clavesIgualesConDistintoFormato_noAdvierte() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        correlacion(m, "nroFactura");
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "  NROFACTURA ", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        assertThat(con(items, "no coincide")).isEmpty();
    }

    @Test
    @DisplayName("HU-28: Si el mensaje lanzado no tiene correlacion no se compara la clave")
    void validar_lanzamientoSinCorrelacion_noComparaClaves() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "nroPedido", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        assertThat(con(items, "no coincide")).isEmpty();
        assertThat(con(items, "El mensaje 'Factura' no tiene clave de correlacion")).hasSize(1);
    }

    @Test
    @DisplayName("HU-28: Si la correlacion no tiene criterio no se compara la clave")
    void validar_correlacionSinCriterio_noComparaClaves() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        correlacion(m, null);
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INTERMEDIO, "nroPedido", false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        assertThat(con(items, "no coincide")).isEmpty();
    }

    @Test
    @DisplayName("HU-28: Si el evento receptor no tiene clave no se compara con la del lanzamiento")
    void validar_receptorSinClave_noComparaClaves() {
        Mensaje m = mensaje(700, "Factura", poolExterno());
        correlacion(m, "nroFactura");
        Evento receptor = receptor(300, "Factura", TipoEvento.MENSAJE_RECEPCION_INICIO, null, false);

        List<Map<String, Object>> items = validarCon(List.of(), List.of(m), receptor);

        assertThat(con(items, "no coincide")).isEmpty();
    }
}