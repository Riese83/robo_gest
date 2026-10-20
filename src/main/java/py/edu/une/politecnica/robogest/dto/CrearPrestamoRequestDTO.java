package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para la creacion / solicitud de un nuevo prestamo.
 */
public class CrearPrestamoRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long integranteId;
    private Long proyectoId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaDevolucionPrevista;

    private String descripcion;

    // Soporte para item unico simplificado
    private Long materialId;
    private Integer cantidad;

    // Soporte para multiples items
    private List<DetallePrestamoItemDTO> detalles = new ArrayList<>();

    public CrearPrestamoRequestDTO() {
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public Long getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(Long proyectoId) {
        this.proyectoId = proyectoId;
    }

    public LocalDateTime getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(LocalDateTime fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public List<DetallePrestamoItemDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePrestamoItemDTO> detalles) {
        this.detalles = detalles;
    }
}
