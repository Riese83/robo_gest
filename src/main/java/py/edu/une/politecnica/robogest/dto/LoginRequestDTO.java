package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para la solicitud de inicio de sesion.
 */
public class LoginRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String identificador; // Email o Cedula de Identidad (CI)
    private String password;

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String identificador, String password) {
        this.identificador = identificador;
        this.password = password;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
