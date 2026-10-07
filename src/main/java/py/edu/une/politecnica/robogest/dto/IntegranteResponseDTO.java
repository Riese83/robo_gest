package py.edu.une.politecnica.robogest.dto;

import py.edu.une.politecnica.robogest.entity.Integrante;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para la visualizacion del padron de integrantes del club.
 */
public class IntegranteResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String ci;
    private String carnetUniversitario;
    private String telefono;
    private String email;
    private LocalDate fechaIngreso;
    private String estado;
    private String nfcUid;
    private String carrera;
    private String rol;

    public IntegranteResponseDTO() {
    }

    public static IntegranteResponseDTO fromEntity(Integrante i) {
        if (i == null) return null;
        IntegranteResponseDTO dto = new IntegranteResponseDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setApellido(i.getApellido());
        dto.setNombreCompleto(i.getNombre() + " " + i.getApellido());
        dto.setCi(i.getCi());
        dto.setCarnetUniversitario(i.getCarnetUniversitario());
        dto.setTelefono(i.getTelefono());
        dto.setEmail(i.getEmail());
        dto.setFechaIngreso(i.getFechaIngreso());
        dto.setEstado(i.getEstado() != null ? i.getEstado().name() : "ACTIVO");
        dto.setNfcUid(i.getNfcUid());
        dto.setCarrera(i.getCarrera() != null ? i.getCarrera().name() : "");
        dto.setRol(i.getRol() != null ? i.getRol().name() : "MIEMBRO");
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getCarnetUniversitario() {
        return carnetUniversitario;
    }

    public void setCarnetUniversitario(String carnetUniversitario) {
        this.carnetUniversitario = carnetUniversitario;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNfcUid() {
        return nfcUid;
    }

    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
