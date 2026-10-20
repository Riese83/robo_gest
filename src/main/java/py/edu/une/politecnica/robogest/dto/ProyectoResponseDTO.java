package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import py.edu.une.politecnica.robogest.entity.Proyecto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para la visualizacion de proyectos del club.
 */
public class ProyectoResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String descripcion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private String estado;
    private java.util.List<ProyectoParticipanteDTO> integrantes = new java.util.ArrayList<>();
    private java.util.List<ProyectoMaterialDTO> materiales = new java.util.ArrayList<>();

    public ProyectoResponseDTO() {
    }

    public static ProyectoResponseDTO fromEntity(Proyecto p) {
        return fromEntity(p, java.util.List.of(), java.util.List.of());
    }

    public static ProyectoResponseDTO fromEntity(Proyecto p, java.util.List<ProyectoParticipanteDTO> integrantes, java.util.List<ProyectoMaterialDTO> materiales) {
        if (p == null) return null;
        ProyectoResponseDTO dto = new ProyectoResponseDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFin(p.getFechaFin());
        dto.setEstado(p.getEstado() != null ? p.getEstado().name() : "PLANIFICADO");
        if (integrantes != null) dto.setIntegrantes(integrantes);
        if (materiales != null) dto.setMateriales(materiales);
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public java.util.List<ProyectoParticipanteDTO> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(java.util.List<ProyectoParticipanteDTO> integrantes) {
        this.integrantes = integrantes;
    }

    public java.util.List<ProyectoMaterialDTO> getMateriales() {
        return materiales;
    }

    public void setMateriales(java.util.List<ProyectoMaterialDTO> materiales) {
        this.materiales = materiales;
    }
}
