package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import py.edu.une.politecnica.robogest.entity.DetallePrestamo;
import py.edu.une.politecnica.robogest.entity.Prestamo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que encapsula los datos completos de respuesta de un prestamo y sus materiales asociados.
 */
public class PrestamoResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long prestamoId;
    private Long integranteId;
    private String nombreIntegrante;
    private String emailIntegrante;
    private Long proyectoId;
    private String nombreProyecto;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaSolicitud;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaEntrega;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaDevolucionPrevista;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaDevolucionReal;

    private String estado;
    private String descripcion;
    private List<DetallePrestamoResponseDTO> detalles = new ArrayList<>();

    public PrestamoResponseDTO() {
    }

    /**
     * Mapea de forma segura una entidad JPA Prestamo a su DTO de respuesta,
     * garantizando que nunca se exponga la entidad ni se provoquen problemas de lazy loading.
     */
    public static PrestamoResponseDTO fromEntity(Prestamo prestamo) {
        if (prestamo == null) {
            return null;
        }

        PrestamoResponseDTO dto = new PrestamoResponseDTO();
        dto.setPrestamoId(prestamo.getId());
        dto.setFechaSolicitud(prestamo.getFechaSolicitud());
        dto.setFechaEntrega(prestamo.getFechaEntrega());
        dto.setFechaDevolucionPrevista(prestamo.getFechaDevolucionPrevista());
        dto.setFechaDevolucionReal(prestamo.getFechaDevolucionReal());
        dto.setEstado(prestamo.getEstado() != null ? prestamo.getEstado().name() : null);
        dto.setDescripcion(prestamo.getDescripcion());

        if (prestamo.getIntegrante() != null) {
            dto.setIntegranteId(prestamo.getIntegrante().getId());
            dto.setNombreIntegrante(prestamo.getIntegrante().getNombre() + " " + prestamo.getIntegrante().getApellido());
            dto.setEmailIntegrante(prestamo.getIntegrante().getEmail());
        }

        if (prestamo.getProyecto() != null) {
            dto.setProyectoId(prestamo.getProyecto().getId());
            dto.setNombreProyecto(prestamo.getProyecto().getNombre());
        }

        if (prestamo.getDetalles() != null) {
            for (DetallePrestamo dp : prestamo.getDetalles()) {
                DetallePrestamoResponseDTO detDto = new DetallePrestamoResponseDTO();
                if (dp.getMaterial() != null) {
                    detDto.setMaterialId(dp.getMaterial().getId());
                    detDto.setNombreMaterial(dp.getMaterial().getNombre());
                    detDto.setMarca(dp.getMaterial().getMarca());
                    detDto.setModelo(dp.getMaterial().getModelo());
                }
                detDto.setCantidad(dp.getCantidad());
                dto.getDetalles().add(detDto);
            }
        }

        return dto;
    }

    public Long getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Long prestamoId) {
        this.prestamoId = prestamoId;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getNombreIntegrante() {
        return nombreIntegrante;
    }

    public void setNombreIntegrante(String nombreIntegrante) {
        this.nombreIntegrante = nombreIntegrante;
    }

    public String getEmailIntegrante() {
        return emailIntegrante;
    }

    public void setEmailIntegrante(String emailIntegrante) {
        this.emailIntegrante = emailIntegrante;
    }

    public Long getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(Long proyectoId) {
        this.proyectoId = proyectoId;
    }

    public String getNombreProyecto() {
        return nombreProyecto;
    }

    public void setNombreProyecto(String nombreProyecto) {
        this.nombreProyecto = nombreProyecto;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(LocalDateTime fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public LocalDateTime getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDateTime fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<DetallePrestamoResponseDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePrestamoResponseDTO> detalles) {
        this.detalles = detalles;
    }
}
