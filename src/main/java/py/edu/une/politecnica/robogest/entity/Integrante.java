package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad que representa a un miembro o colaborador del Club de Robotica (FP-UNE).
 */
@Entity
@Table(name = "integrante")
public class Integrante implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integrante_id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "ci", nullable = false, unique = true, length = 20)
    private String ci;

    @Column(name = "carnet_universitario", nullable = false, unique = true, length = 50)
    private String carnetUniversitario;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoIntegranteEnum estado = EstadoIntegranteEnum.ACTIVO;

    @Column(name = "nfc_uid", unique = true, length = 100)
    private String nfcUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "carrera", length = 120)
    private CarreraEnum carrera;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private RolEnum rol = RolEnum.MIEMBRO;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @OneToMany(mappedBy = "integrante", fetch = FetchType.LAZY)
    private List<Asistencia> asistencias = new ArrayList<>();

    @OneToMany(mappedBy = "integrante", fetch = FetchType.LAZY)
    private List<Prestamo> prestamos = new ArrayList<>();

    @OneToMany(mappedBy = "integrante", fetch = FetchType.LAZY)
    private List<IntegranteProyecto> integranteProyectos = new ArrayList<>();

    public Integrante() {
    }

    public Integrante(String nombre, String apellido, String ci, String carnetUniversitario,
                      String telefono, String email, LocalDate fechaIngreso,
                      EstadoIntegranteEnum estado, String nfcUid, CarreraEnum carrera,
                      RolEnum rol, String passwordHash) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.ci = ci;
        this.carnetUniversitario = carnetUniversitario;
        this.telefono = telefono;
        this.email = email;
        this.fechaIngreso = fechaIngreso;
        this.estado = estado;
        this.nfcUid = nfcUid;
        this.carrera = carrera;
        this.rol = rol;
        this.passwordHash = passwordHash;
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

    public EstadoIntegranteEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoIntegranteEnum estado) {
        this.estado = estado;
    }

    public String getNfcUid() {
        return nfcUid;
    }

    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    public CarreraEnum getCarrera() {
        return carrera;
    }

    public void setCarrera(CarreraEnum carrera) {
        this.carrera = carrera;
    }

    public RolEnum getRol() {
        return rol;
    }

    public void setRol(RolEnum rol) {
        this.rol = rol;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public List<Asistencia> getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(List<Asistencia> asistencias) {
        this.asistencias = asistencias;
    }

    public List<Prestamo> getPrestamos() {
        return prestamos;
    }

    public void setPrestamos(List<Prestamo> prestamos) {
        this.prestamos = prestamos;
    }

    public List<IntegranteProyecto> getIntegranteProyectos() {
        return integranteProyectos;
    }

    public void setIntegranteProyectos(List<IntegranteProyecto> integranteProyectos) {
        this.integranteProyectos = integranteProyectos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Integrante that = (Integrante) o;
        return Objects.equals(id, that.id) || (ci != null && Objects.equals(ci, that.ci));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ci);
    }

    @Override
    public String toString() {
        return "Integrante{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", ci='" + ci + '\'' +
                ", carnetUniversitario='" + carnetUniversitario + '\'' +
                ", email='" + email + '\'' +
                ", fechaIngreso=" + fechaIngreso +
                ", estado=" + estado +
                ", carrera=" + carrera +
                ", rol=" + rol +
                '}';
    }
}
