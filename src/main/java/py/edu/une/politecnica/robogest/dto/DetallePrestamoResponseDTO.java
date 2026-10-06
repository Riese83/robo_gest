package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO que representa un item de material dentro de la respuesta de un prestamo.
 */
public class DetallePrestamoResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long materialId;
    private String nombreMaterial;
    private String marca;
    private String modelo;
    private Integer cantidad;

    public DetallePrestamoResponseDTO() {
    }

    public DetallePrestamoResponseDTO(Long materialId, String nombreMaterial, String marca, String modelo, Integer cantidad) {
        this.materialId = materialId;
        this.nombreMaterial = nombreMaterial;
        this.marca = marca;
        this.modelo = modelo;
        this.cantidad = cantidad;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getNombreMaterial() {
        return nombreMaterial;
    }

    public void setNombreMaterial(String nombreMaterial) {
        this.nombreMaterial = nombreMaterial;
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

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
