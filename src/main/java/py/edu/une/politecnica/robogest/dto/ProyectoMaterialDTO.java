package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para representar un material o componente planificado para un proyecto.
 */
public class ProyectoMaterialDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long materialId;
    private String nombreMaterial;
    private String categoriaNombre;
    private Integer cantidadPlanificada;
    private Integer stockDisponible;

    public ProyectoMaterialDTO() {
    }

    public ProyectoMaterialDTO(Long materialId, String nombreMaterial, String categoriaNombre, Integer cantidadPlanificada, Integer stockDisponible) {
        this.materialId = materialId;
        this.nombreMaterial = nombreMaterial;
        this.categoriaNombre = categoriaNombre;
        this.cantidadPlanificada = cantidadPlanificada;
        this.stockDisponible = stockDisponible;
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

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public Integer getCantidadPlanificada() {
        return cantidadPlanificada;
    }

    public void setCantidadPlanificada(Integer cantidadPlanificada) {
        this.cantidadPlanificada = cantidadPlanificada;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public Integer getCantidad() {
        return cantidadPlanificada;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidadPlanificada = cantidad;
    }
}
