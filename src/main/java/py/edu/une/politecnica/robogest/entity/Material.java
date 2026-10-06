package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad que representa un material, insumo o componente del inventario del club.
 */
@Entity
@Table(name = "material", indexes = {
    @Index(name = "idx_material_categoria", columnList = "categoria_id"),
    @Index(name = "idx_material_nombre", columnList = "nombre")
})
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoMaterialEnum estado = EstadoMaterialEnum.ACTIVO;

    @Column(name = "ubicacion", length = 200)
    private String ubicacion;

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private List<DetallePrestamo> detallesPrestamo = new ArrayList<>();

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private List<ProyectoMaterial> proyectoMateriales = new ArrayList<>();

    public Material() {
    }

    public Material(Categoria categoria, String nombre, String descripcion, String marca,
                    String modelo, Integer cantidadTotal, EstadoMaterialEnum estado, String ubicacion) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.modelo = modelo;
        this.cantidadTotal = cantidadTotal;
        this.estado = estado;
        this.ubicacion = ubicacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
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

    public EstadoMaterialEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoMaterialEnum estado) {
        this.estado = estado;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public List<DetallePrestamo> getDetallesPrestamo() {
        return detallesPrestamo;
    }

    public void setDetallesPrestamo(List<DetallePrestamo> detallesPrestamo) {
        this.detallesPrestamo = detallesPrestamo;
    }

    public List<ProyectoMaterial> getProyectoMateriales() {
        return proyectoMateriales;
    }

    public void setProyectoMateriales(List<ProyectoMaterial> proyectoMateriales) {
        this.proyectoMateriales = proyectoMateriales;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Material material = (Material) o;
        return Objects.equals(id, material.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Material{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", marca='" + marca + '\'' +
                ", modelo='" + modelo + '\'' +
                ", cantidadTotal=" + cantidadTotal +
                ", estado=" + estado +
                ", ubicacion='" + ubicacion + '\'' +
                '}';
    }
}
