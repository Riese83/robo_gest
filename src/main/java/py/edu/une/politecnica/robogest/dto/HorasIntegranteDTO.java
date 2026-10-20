package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO que representa el cómputo de horas acumuladas y el nivel/tier
 * de reconocimiento de un integrante en el Club de Robótica FP-UNE.
 */
public class HorasIntegranteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long integranteId;
    private String nombreCompleto;
    private String carnet;
    private String carrera;
    private double horasTotales;
    private int totalSesiones;
    private String tier;              // DIAMANTE, ORO, PLATA, BRONCE
    private String tierNombre;        // Ej: "Tier Oro (25-50h)"
    private String tierBadgeClass;    // Clase CSS para badge
    private String tierIcono;         // Icono Bootstrap Icons
    private int posicion;             // 1, 2, 3...
    private boolean enElLabHoy;       // true si marcó ENTRADA hoy y sigue activo
    private double horasParaSiguienteTier;
    private int porcentajeProgresoTier;

    public HorasIntegranteDTO() {
    }

    public HorasIntegranteDTO(Long integranteId, String nombreCompleto, String carnet, String carrera,
                              double horasTotales, int totalSesiones, String tier, String tierNombre,
                              String tierBadgeClass, String tierIcono, int posicion, boolean enElLabHoy) {
        this.integranteId = integranteId;
        this.nombreCompleto = nombreCompleto;
        this.carnet = carnet;
        this.carrera = carrera;
        this.horasTotales = horasTotales;
        this.totalSesiones = totalSesiones;
        this.tier = tier;
        this.tierNombre = tierNombre;
        this.tierBadgeClass = tierBadgeClass;
        this.tierIcono = tierIcono;
        this.posicion = posicion;
        this.enElLabHoy = enElLabHoy;
        calcularProgreso();
    }

    public void calcularProgreso() {
        if ("DIAMANTE".equalsIgnoreCase(this.tier)) {
            this.horasParaSiguienteTier = 0.0;
            this.porcentajeProgresoTier = 100;
        } else if ("ORO".equalsIgnoreCase(this.tier)) {
            this.horasParaSiguienteTier = Math.max(0.0, Math.round((50.0 - this.horasTotales) * 10.0) / 10.0);
            this.porcentajeProgresoTier = (int) Math.min(100, Math.max(0, Math.round(((this.horasTotales - 25.0) / 25.0) * 100)));
        } else if ("PLATA".equalsIgnoreCase(this.tier)) {
            this.horasParaSiguienteTier = Math.max(0.0, Math.round((25.0 - this.horasTotales) * 10.0) / 10.0);
            this.porcentajeProgresoTier = (int) Math.min(100, Math.max(0, Math.round(((this.horasTotales - 10.0) / 15.0) * 100)));
        } else { // BRONCE
            this.horasParaSiguienteTier = Math.max(0.0, Math.round((10.0 - this.horasTotales) * 10.0) / 10.0);
            this.porcentajeProgresoTier = (int) Math.min(100, Math.max(0, Math.round((this.horasTotales / 10.0) * 100)));
        }
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCarnet() {
        return carnet;
    }

    public void setCarnet(String carnet) {
        this.carnet = carnet;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public double getHorasTotales() {
        return horasTotales;
    }

    public void setHorasTotales(double horasTotales) {
        this.horasTotales = horasTotales;
        calcularProgreso();
    }

    public int getTotalSesiones() {
        return totalSesiones;
    }

    public void setTotalSesiones(int totalSesiones) {
        this.totalSesiones = totalSesiones;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
        calcularProgreso();
    }

    public String getTierNombre() {
        return tierNombre;
    }

    public void setTierNombre(String tierNombre) {
        this.tierNombre = tierNombre;
    }

    public String getTierBadgeClass() {
        return tierBadgeClass;
    }

    public void setTierBadgeClass(String tierBadgeClass) {
        this.tierBadgeClass = tierBadgeClass;
    }

    public String getTierIcono() {
        return tierIcono;
    }

    public void setTierIcono(String tierIcono) {
        this.tierIcono = tierIcono;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public boolean isEnElLabHoy() {
        return enElLabHoy;
    }

    public void setEnElLabHoy(boolean enElLabHoy) {
        this.enElLabHoy = enElLabHoy;
    }

    public double getHorasParaSiguienteTier() {
        return horasParaSiguienteTier;
    }

    public void setHorasParaSiguienteTier(double horasParaSiguienteTier) {
        this.horasParaSiguienteTier = horasParaSiguienteTier;
    }

    public int getPorcentajeProgresoTier() {
        return porcentajeProgresoTier;
    }

    public void setPorcentajeProgresoTier(int porcentajeProgresoTier) {
        this.porcentajeProgresoTier = porcentajeProgresoTier;
    }
}
