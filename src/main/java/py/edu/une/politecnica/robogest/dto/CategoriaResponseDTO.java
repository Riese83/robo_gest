package py.edu.une.politecnica.robogest.dto;

import py.edu.une.politecnica.robogest.entity.Categoria;

import java.io.Serializable;

/**
 * DTO para la exposicion de categorias de materiales y componentes.
 */
public class CategoriaResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String descripcion;

    public CategoriaResponseDTO() {
    }

    public CategoriaResponseDTO(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public static CategoriaResponseDTO fromEntity(Categoria c) {
        if (c == null) return null;
        return new CategoriaResponseDTO(c.getId(), c.getNombre(), c.getDescripcion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
