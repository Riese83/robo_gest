package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para la solicitud de marcacion de asistencia enviada por el sensor IoT ESP32.
 */
public class MarcacionRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nfcUid;
    private String dispositivo;

    public MarcacionRequestDTO() {
    }

    public MarcacionRequestDTO(String nfcUid, String dispositivo) {
        this.nfcUid = nfcUid;
        this.dispositivo = dispositivo;
    }

    public String getNfcUid() {
        return nfcUid;
    }

    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
