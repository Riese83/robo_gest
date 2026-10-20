package py.edu.une.politecnica.robogest.service;

import py.edu.une.politecnica.robogest.dto.ConsultaIaRequestDTO;
import py.edu.une.politecnica.robogest.dto.ConsultaIaResponseDTO;

/**
 * Servicio para la integracion con IA Generativa (Google Gemini)
 * asistiendo a los usuarios con ideas de proyectos, inventario y normativas del club.
 */
public interface AsistenteIaService {

    /**
     * Procesa una consulta del usuario inyectando el estado real del inventario y reglamentos del club.
     *
     * @param request Datos de la consulta y posible historial
     * @param usuarioId ID del integrante autenticado
     * @param nombreUsuario Nombre del usuario
     * @param rolUsuario Rol del usuario (ADMIN, DIRECTIVA, MIEMBRO)
     * @return Respuesta estructurada del asistente de IA
     */
    ConsultaIaResponseDTO consultar(ConsultaIaRequestDTO request, Long usuarioId, String nombreUsuario, String rolUsuario);
}
