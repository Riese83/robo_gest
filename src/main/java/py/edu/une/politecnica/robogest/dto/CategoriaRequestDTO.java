package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para la creacion y actualizacion de categorias.
 */
public class CategoriaRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String descripcion;

    public CategoriaRequestDTO() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
