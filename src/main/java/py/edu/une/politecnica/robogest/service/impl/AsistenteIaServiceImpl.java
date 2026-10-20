package py.edu.une.politecnica.robogest.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dao.impl.DetallePrestamoDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.MaterialDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.ConsultaIaRequestDTO;
import py.edu.une.politecnica.robogest.dto.ConsultaIaResponseDTO;
import py.edu.une.politecnica.robogest.dto.MensajeChatDTO;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.service.AsistenteIaService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion del Asistente de IA con Google Gemini 1.5 Flash / Flash Lite (Free Tier).
 * Aplica inyeccion dinamica del inventario de materiales y normativas del Club de Robotica FP-UNE.
 */
public class AsistenteIaServiceImpl implements AsistenteIaService {

    private static final Logger LOGGER = Logger.getLogger(AsistenteIaServiceImpl.class.getName());
    private static final String DEFAULT_MODEL = "gemini-flash-lite-latest";
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-flash-lite-latest",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash"
    );
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final MaterialDAO materialDAO;
    private final DetallePrestamoDAO detallePrestamoDAO;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AsistenteIaServiceImpl() {
        this(new MaterialDAOJpaImpl(), new DetallePrestamoDAOJpaImpl());
    }

    public AsistenteIaServiceImpl(MaterialDAO materialDAO, DetallePrestamoDAO detallePrestamoDAO) {
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.detallePrestamoDAO = Objects.requireNonNull(detallePrestamoDAO, "detallePrestamoDAO no puede ser null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ConsultaIaResponseDTO consultar(ConsultaIaRequestDTO request, Long usuarioId, String nombreUsuario, String rolUsuario) {
        if (request == null || request.getMensaje() == null || request.getMensaje().trim().isEmpty()) {
            return ConsultaIaResponseDTO.error("El mensaje de consulta no puede estar vacio.");
        }

        String apiKey = resolverApiKey(request.getApiKey());
        if (apiKey == null || apiKey.isBlank()) {
            return ConsultaIaResponseDTO.error(
                    "La clave de API de Google Gemini no esta configurada. " +
                    "Puedes obtener una gratis en https://aistudio.google.com e ingresarla en la configuracion del chat " +
                    "o definir la variable de entorno GEMINI_API_KEY en el servidor."
            );
        }

        try {
            String promptSistema = construirPromptSistema(nombreUsuario, rolUsuario);
            return llamarGemini(apiKey, promptSistema, request);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar consulta con Google Gemini", e);
            return ConsultaIaResponseDTO.error("Ocurrio un error al comunicarse con el asistente de IA: " + e.getMessage());
        }
    }

    private String resolverApiKey(String requestKey) {
        if (requestKey != null && !requestKey.trim().isEmpty()) {
            return requestKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        String propKey = System.getProperty("gemini.api.key");
        if (propKey != null && !propKey.trim().isEmpty()) {
            return propKey.trim();
        }
        return null;
    }

    private String resolverModelo(ConsultaIaRequestDTO request) {
        if (request != null && request.getModelo() != null && !request.getModelo().isBlank()) {
            return request.getModelo().trim();
        }
        String envModel = System.getenv("GEMINI_MODEL");
        if (envModel != null && !envModel.isBlank()) {
            return envModel.trim();
        }
        String sysModel = System.getProperty("gemini.model");
        if (sysModel != null && !sysModel.isBlank()) {
            return sysModel.trim();
        }
        return DEFAULT_MODEL;
    }

    private String construirPromptSistema(String nombreUsuario, String rolUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres 'Polito', el Asistente de Inteligencia Artificial del Club de Robotica de la Facultad Politecnica de la Universidad Nacional del Este (FP-UNE) en Ciudad del Este, Paraguay.\n");
        sb.append("Como guia tecnologico y asistente del club, tienes un espiritu amigable, practico, conciso y motivador con orgullo por la FP-UNE.\n");
        sb.append("Tu nombre es Polito.\n");
        sb.append("Estas dialogando con el integrante '").append(nombreUsuario != null ? nombreUsuario : "Miembro");
        sb.append("' (Rol: ").append(rolUsuario != null ? rolUsuario : "MIEMBRO").append(").\n\n");

        sb.append("### REGLA CRITICA: BREVEDAD Y CONCISION OBLIGATORIA:\n");
        sb.append("- Sé SIEMPRE muy breve, directo y conciso. El usuario requiere respuestas agiles y sintetizadas, NUNCA textos largos ni explicaciones redundantes.\n");
        sb.append("- Extension maxima: Maximo 1 a 2 parrafos cortos o una lista puntual de vinetas (aprox. 80-150 palabras en total).\n");
        sb.append("- PROHIBIDO: Introducciones largas, parrafos teoricos extensos o repetir saludos en cada mensaje.\n");
        sb.append("- NO generes bloques de codigo ni diagramas a menos que el usuario los pida explicitamente ('dame el codigo' o 'como se conecta').\n");
        sb.append("- Si te consultan por ideas de proyectos, brinda una propuesta concreta de 1 o 2 lineas y la lista puntual de componentes en stock.\n\n");

        sb.append("### MISION Y CAPACIDADES:\n");
        sb.append("1. Orientar a los estudiantes con ideas innovadoras de proyectos tecnologicos (Robotica movil, Seguidores de linea velocistas, Minisumo, IoT con ESP32, Domotica, Automatizacion, Drones, etc.).\n");
        sb.append("2. Basar SIEMPRE tus propuestas tecnicas en el inventario real y materiales que actualmente tenemos disponibles en el laboratorio.\n");
        sb.append("3. Explicar reglamentos del club, procedimientos de solicitud de prestamo y cuidado de los componentes de forma resumida.\n\n");

        sb.append("### REGLAMENTO INTERNO Y PRESTAMOS:\n");
        sb.append("- Los prestamos estan reservados para integrantes activos acreditados en el sistema Robo_Gest.\n");
        sb.append("- Todo prestamo debe ser solicitado desde la plataforma y aprobado por la Directiva o Administrador antes de retirar los materiales del laboratorio.\n");
        sb.append("- Plazo maximo de devolucion: 7 a 14 dias con posibilidad de renovacion si no hay solicitudes en espera.\n");
        sb.append("- Devolucion obligatoria en estado operativo y limpio. El dano por mal uso implica reposicion coordinada con la directiva.\n");
        sb.append("- Registro de asistencia: Obligatorio en cada sesion del laboratorio mediante tarjeta RFID o marcacion manual.\n\n");

        sb.append("### ESTADO ACTUAL DEL INVENTARIO DEL LABORATORIO:\n");

        try {
            List<Material> materiales = materialDAO.findAll();
            if (materiales == null || materiales.isEmpty()) {
                sb.append("Actualmente no hay materiales registrados en el inventario digital.\n");
            } else {
                sb.append("Materiales con stock disponible actualmente:\n");
                int disponiblesCount = 0;
                StringBuilder agotados = new StringBuilder();

                for (Material m : materiales) {
                    int reservas = 0;
                    try {
                        reservas = detallePrestamoDAO.getCantidadReservadaPorMaterial(m.getId());
                    } catch (Exception ignored) {}

                    int total = m.getCantidadTotal() != null ? m.getCantidadTotal() : 0;
                    int disponible = Math.max(0, total - reservas);
                    String cat = m.getCategoria() != null ? m.getCategoria().getNombre() : "General";

                    if (disponible > 0 && !"BAJA".equalsIgnoreCase(m.getEstado() != null ? m.getEstado().name() : "")) {
                        sb.append("- **").append(m.getNombre()).append("** | Disponible: ").append(disponible);
                        sb.append(" un. (Total: ").append(total).append(")");
                        if (m.getMarca() != null && !m.getMarca().isBlank()) sb.append(" [Marca: ").append(m.getMarca()).append("]");
                        sb.append(" | Categoria: ").append(cat).append("\n");
                        disponiblesCount++;
                    } else {
                        if (agotados.length() > 0) agotados.append(", ");
                        agotados.append(m.getNombre()).append(" (0 disp)");
                    }
                }

                if (disponiblesCount == 0) {
                    sb.append("- Todos los componentes estan actualmente reservados o en mantenimiento.\n");
                }

                if (agotados.length() > 0) {
                    sb.append("\nMateriales actualmente agotados o en uso:\n").append(agotados).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("Nota: No se pudo sincronizar el stock en vivo momentaneamente.\n");
        }

        sb.append("\n### PAUTAS DE RESPUESTA Y PERSONALIDAD:\n");
        sb.append("- Responde en espanol con formato Markdown claro, directo y muy sintetico (vinetas y negritas para componentes).\n");
        sb.append("- Muestra entusiasmo (¡Excelente idea!, ¡Genial!, 🚀, 💡) o advertencia (⚠️) en frases cortas sin rodeos.\n");
        sb.append("- Manten tu identidad viva como Polito de la FP-UNE con maxima brevedad.\n");

        return sb.toString();
    }

    private ConsultaIaResponseDTO llamarGemini(String apiKey, String promptSistema, ConsultaIaRequestDTO request) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Instruccion de sistema (System Instruction)
        ObjectNode systemInstructionNode = rootNode.putObject("systemInstruction");
        ArrayNode systemParts = systemInstructionNode.putArray("parts");
        systemParts.addObject().put("text", promptSistema);

        // 2. Contenido (Historial + Mensaje Actual)
        ArrayNode contentsArray = rootNode.putArray("contents");

        if (request.getHistorial() != null) {
            for (MensajeChatDTO msj : request.getHistorial()) {
                if (msj.getContenido() != null && !msj.getContenido().isBlank()) {
                    String role = "model".equalsIgnoreCase(msj.getRol()) ? "model" : "user";
                    ObjectNode turn = contentsArray.addObject();
                    turn.put("role", role);
                    turn.putArray("parts").addObject().put("text", msj.getContenido());
                }
            }
        }

        // Mensaje actual del usuario
        ObjectNode currentTurn = contentsArray.addObject();
        currentTurn.put("role", "user");
        currentTurn.putArray("parts").addObject().put("text", request.getMensaje());

        // 3. Configuracion de Generacion
        ObjectNode genConfig = rootNode.putObject("generationConfig");
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 500);

        String jsonPayload = objectMapper.writeValueAsString(rootNode);

        String modeloInicial = resolverModelo(request);
        List<String> modelosAIntentar = new ArrayList<>();
        modelosAIntentar.add(modeloInicial);
        for (String m : FALLBACK_MODELS) {
            if (!modelosAIntentar.contains(m)) {
                modelosAIntentar.add(m);
            }
        }

        String ultimoError = null;

        for (String modelo : modelosAIntentar) {
            URI targetUri = URI.create(GEMINI_API_BASE_URL + modelo + ":generateContent?key=" + apiKey);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode respJson = objectMapper.readTree(response.body());
                JsonNode candidates = respJson.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String textoGenerado = parts.get(0).path("text").asText();
                        return ConsultaIaResponseDTO.exito(textoGenerado, "Google Gemini (" + modelo + ")");
                    }
                }
                return ConsultaIaResponseDTO.error("Gemini no genero contenido para esta consulta.");
            } else if (response.statusCode() == 429 || response.statusCode() == 404) {
                LOGGER.warning("Modelo '" + modelo + "' respondio HTTP " + response.statusCode() + " (cuota agotada o modelo inaccesible). Reintentando con modelo de respaldo...");
                ultimoError = "Cuota excedida temporalmente en modelo '" + modelo + "'.";
                // Continua el bucle probando el siguiente modelo
            } else {
                String errBody = response.body();
                LOGGER.warning("Error de Gemini API HTTP " + response.statusCode() + ": " + errBody);
                try {
                    JsonNode errJson = objectMapper.readTree(errBody);
                    String errMsg = errJson.path("error").path("message").asText();
                    if (errMsg.contains("API key not valid") || errMsg.contains("API_KEY_INVALID")) {
                        return ConsultaIaResponseDTO.error("La clave de API de Gemini provista no es valida o ha expirado. Por favor verifica tu clave en aistudio.google.com.");
                    }
                    return ConsultaIaResponseDTO.error("Error de Gemini API (" + response.statusCode() + "): " + errMsg);
                } catch (Exception ignored) {
                    return ConsultaIaResponseDTO.error("Error en la comunicacion con Gemini API. Codigo HTTP: " + response.statusCode());
                }
            }
        }

        return ConsultaIaResponseDTO.error(ultimoError != null ? ultimoError : "Se excedio la cuota de consultas en los modelos disponibles de Gemini. Por favor intenta mas tarde o ingresa una API Key alternativa.");
    }
}
