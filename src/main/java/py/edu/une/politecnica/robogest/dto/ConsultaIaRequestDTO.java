package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de consulta al asistente inteligente con soporte de historial y clave opcional.
 */
public class ConsultaIaRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mensaje;
    private List<MensajeChatDTO> historial = new ArrayList<>();
    private String apiKey; // Opcional: clave provista dinamicamente por el cliente o admin
    private String modelo; // Opcional: modelo provisto dinamicamente (ej: gemini-flash-lite-latest)

    public ConsultaIaRequestDTO() {}

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<MensajeChatDTO> getHistorial() {
        return historial;
    }

    public void setHistorial(List<MensajeChatDTO> historial) {
        this.historial = historial != null ? historial : new ArrayList<>();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }
}
