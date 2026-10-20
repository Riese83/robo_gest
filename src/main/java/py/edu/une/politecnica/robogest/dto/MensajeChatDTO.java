package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * Representa un turno de conversacion en el historial del chat con la IA.
 */
public class MensajeChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rol; // "user" o "model"
    private String contenido;

    public MensajeChatDTO() {}

    public MensajeChatDTO(String rol, String contenido) {
        this.rol = rol;
        this.contenido = contenido;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
