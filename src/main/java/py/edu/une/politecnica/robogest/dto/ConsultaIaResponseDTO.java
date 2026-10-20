package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * Respuesta generada por el asistente inteligente.
 */
public class ConsultaIaResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean exito;
    private String respuesta;
    private String proveedor;
    private String error;

    public ConsultaIaResponseDTO() {}

    public static ConsultaIaResponseDTO exito(String respuesta, String proveedor) {
        ConsultaIaResponseDTO dto = new ConsultaIaResponseDTO();
        dto.setExito(true);
        dto.setRespuesta(respuesta);
        dto.setProveedor(proveedor);
        return dto;
    }

    public static ConsultaIaResponseDTO error(String mensajeError) {
        ConsultaIaResponseDTO dto = new ConsultaIaResponseDTO();
        dto.setExito(false);
        dto.setError(mensajeError);
        dto.setRespuesta(mensajeError);
        return dto;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
