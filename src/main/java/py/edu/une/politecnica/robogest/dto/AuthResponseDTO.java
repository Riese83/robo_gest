package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para la respuesta de autenticacion exitosa que contiene el token JWT y datos de sesion.
 */
public class AuthResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String tokenType = "Bearer";
    private Long integranteId;
    private String email;
    private String ci;
    private String nombreCompleto;
    private String rol;
    private long expiresInMs;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, Long integranteId, String email, String ci,
                           String nombreCompleto, String rol, long expiresInMs) {
        this.token = token;
        this.tokenType = "Bearer";
        this.integranteId = integranteId;
        this.email = email;
        this.ci = ci;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }
}
