package py.edu.une.politecnica.robogest.dto;

import py.edu.une.politecnica.robogest.entity.Material;

import java.io.Serializable;

/**
 * DTO para la exposicion segura de materiales en el inventario.
 */
public class MaterialResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String descripcion;
    private String marca;
    private String modelo;
    private Integer cantidadTotal;
    private String estado;
    private String ubicacion;
    private String categoriaNombre;

    public MaterialResponseDTO() {
    }

    public static MaterialResponseDTO fromEntity(Material m) {
        if (m == null) return null;
        MaterialResponseDTO dto = new MaterialResponseDTO();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setDescripcion(m.getDescripcion());
        dto.setMarca(m.getMarca());
        dto.setModelo(m.getModelo());
        dto.setCantidadTotal(m.getCantidadTotal());
        dto.setEstado(m.getEstado() != null ? m.getEstado().name() : "ACTIVO");
        dto.setUbicacion(m.getUbicacion());
        if (m.getCategoria() != null) {
            dto.setCategoriaNombre(m.getCategoria().getNombre());
        }
        return dto;
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

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getCantidadTotal() {
        return cantidadTotal;
    }

    public void setCantidadTotal(Integer cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }
}
