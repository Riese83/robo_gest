package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que agrupa el resumen de horas del usuario actual y,
 * en caso de usuarios autorizados (ADMIN/DIRECTIVA), la lista clasificada (Tier/Ranking).
 */
public class ResumenHorasDashboardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private HorasIntegranteDTO misHoras;
    private List<HorasIntegranteDTO> ranking = new ArrayList<>();
    private double totalHorasLaboratorioClub;
    private int totalMiembrosConHoras;
    private boolean esUsuarioAutorizado;

    public ResumenHorasDashboardDTO() {
    }

    public ResumenHorasDashboardDTO(HorasIntegranteDTO misHoras, List<HorasIntegranteDTO> ranking,
                                   double totalHorasLaboratorioClub, int totalMiembrosConHoras,
                                   boolean esUsuarioAutorizado) {
        this.misHoras = misHoras;
        this.ranking = ranking != null ? ranking : new ArrayList<>();
        this.totalHorasLaboratorioClub = totalHorasLaboratorioClub;
        this.totalMiembrosConHoras = totalMiembrosConHoras;
        this.esUsuarioAutorizado = esUsuarioAutorizado;
    }

    public HorasIntegranteDTO getMisHoras() {
        return misHoras;
    }

    public void setMisHoras(HorasIntegranteDTO misHoras) {
        this.misHoras = misHoras;
    }

    public List<HorasIntegranteDTO> getRanking() {
        return ranking;
    }

    public void setRanking(List<HorasIntegranteDTO> ranking) {
        this.ranking = ranking != null ? ranking : new ArrayList<>();
    }

    public double getTotalHorasLaboratorioClub() {
        return totalHorasLaboratorioClub;
    }

    public void setTotalHorasLaboratorioClub(double totalHorasLaboratorioClub) {
        this.totalHorasLaboratorioClub = totalHorasLaboratorioClub;
    }

    public int getTotalMiembrosConHoras() {
        return totalMiembrosConHoras;
    }

    public void setTotalMiembrosConHoras(int totalMiembrosConHoras) {
        this.totalMiembrosConHoras = totalMiembrosConHoras;
    }

    public boolean isEsUsuarioAutorizado() {
        return esUsuarioAutorizado;
    }

    public void setEsUsuarioAutorizado(boolean esUsuarioAutorizado) {
        this.esUsuarioAutorizado = esUsuarioAutorizado;
    }
}
