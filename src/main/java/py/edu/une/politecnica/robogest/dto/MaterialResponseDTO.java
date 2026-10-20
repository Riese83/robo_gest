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
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
    private String estado;
    private String ubicacion;
    private Long categoriaId;
    private String categoriaNombre;

    public MaterialResponseDTO() {
    }

    public static MaterialResponseDTO fromEntity(Material m) {
        return fromEntity(m, 0);
    }

    public static MaterialResponseDTO fromEntity(Material m, int reservas) {
        if (m == null) return null;
        MaterialResponseDTO dto = new MaterialResponseDTO();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setDescripcion(m.getDescripcion());
        dto.setMarca(m.getMarca());
        dto.setModelo(m.getModelo());
        dto.setCantidadTotal(m.getCantidadTotal());
        int total = m.getCantidadTotal() != null ? m.getCantidadTotal() : 0;
        dto.setCantidadReservada(reservas);
        dto.setCantidadDisponible(Math.max(0, total - reservas));
        dto.setEstado(m.getEstado() != null ? m.getEstado().name() : "ACTIVO");
        dto.setUbicacion(m.getUbicacion());
        if (m.getCategoria() != null) {
            dto.setCategoriaId(m.getCategoria().getId());
            dto.setCategoriaNombre(m.getCategoria().getNombre());
        }
        return dto;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
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

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(Integer cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public Integer getCantidadReservada() {
        return cantidadReservada;
    }

    public void setCantidadReservada(Integer cantidadReservada) {
        this.cantidadReservada = cantidadReservada;
    }
}
