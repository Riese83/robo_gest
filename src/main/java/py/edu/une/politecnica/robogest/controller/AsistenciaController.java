package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.AsistenciaDAO;
import py.edu.une.politecnica.robogest.dao.impl.AsistenciaDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.IntegranteDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.HorasIntegranteDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionManualRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionResponseDTO;
import py.edu.une.politecnica.robogest.dto.ResumenHorasDashboardDTO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;
import py.edu.une.politecnica.robogest.security.SecurityUtils;
import py.edu.une.politecnica.robogest.service.AsistenciaService;
import py.edu.une.politecnica.robogest.service.impl.AsistenciaServiceImpl;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controlador REST para el registro de marcaciones IoT (ESP32 con lector RFID/NFC).
 */
@Path("/asistencia")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final AsistenciaDAO asistenciaDAO;
    private final py.edu.une.politecnica.robogest.dao.IntegranteDAO integranteDAO;

    /**
     * Constructor por defecto para el contenedor JAX-RS / Servlets.
     */
    public AsistenciaController() {
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.asistenciaDAO = new AsistenciaDAOJpaImpl();
        this.asistenciaService = new AsistenciaServiceImpl(
                this.integranteDAO,
                this.asistenciaDAO
        );
    }

    /**
     * Constructor para inyeccion de dependencias.
     */
    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = Objects.requireNonNull(asistenciaService, "asistenciaService no puede ser null");
        this.asistenciaDAO = new AsistenciaDAOJpaImpl();
        this.integranteDAO = new IntegranteDAOJpaImpl();
    }

    public AsistenciaController(AsistenciaService asistenciaService, AsistenciaDAO asistenciaDAO) {
        this.asistenciaService = Objects.requireNonNull(asistenciaService, "asistenciaService no puede ser null");
        this.asistenciaDAO = Objects.requireNonNull(asistenciaDAO, "asistenciaDAO no puede ser null");
        this.integranteDAO = new IntegranteDAOJpaImpl();
    }

    public AsistenciaController(AsistenciaService asistenciaService, AsistenciaDAO asistenciaDAO, py.edu.une.politecnica.robogest.dao.IntegranteDAO integranteDAO) {
        this.asistenciaService = Objects.requireNonNull(asistenciaService, "asistenciaService no puede ser null");
        this.asistenciaDAO = Objects.requireNonNull(asistenciaDAO, "asistenciaDAO no puede ser null");
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
    }

    private static volatile String ultimoNfcUidEscaneado = null;
    private static volatile long timestampUltimoEscaneo = 0;
    private static volatile String dispositivoUltimoEscaneo = null;

    // Estado para modo grabacion de tarjeta fisica con ESP32
    private static volatile String uidPendienteGrabar = null;
    private static volatile long timestampPendienteGrabar = 0;
    private static volatile String ultimoUidGrabado = null;
    private static volatile long timestampUltimoGrabado = 0;

    @GET
    public Response listarMarcaciones(@HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        List<Asistencia> asistencias = asistenciaDAO.findAll();
        List<MarcacionResponseDTO> dtos = asistencias.stream()
                .map(MarcacionResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }

    /**
     * Retorna el resumen de horas del usuario autenticado (para cualquier miembro) y,
     * en caso de roles administrativos (ADMIN/DIRECTIVA), el Tier / Cuadro de Honor
     * con el ranking completo de miembros con más horas en el Club de Robótica.
     * GET /api/asistencia/horas-dashboard
     */
    @GET
    @Path("/horas-dashboard")
    public Response obtenerHorasDashboard(@HeaderParam("Authorization") String authHeader) {
        io.jsonwebtoken.Claims claims = SecurityUtils.validarAutenticado(authHeader);

        Long usuarioId = null;
        Object idObj = claims.get("integranteId");
        if (idObj instanceof Number) {
            usuarioId = ((Number) idObj).longValue();
        } else if (idObj != null) {
            try {
                usuarioId = Long.parseLong(idObj.toString());
            } catch (Exception ignored) {}
        }

        String rol = claims.get("rol", String.class);
        boolean esAutorizado = rol != null && ("ADMIN".equalsIgnoreCase(rol) || "DIRECTIVA".equalsIgnoreCase(rol));

        List<Integrante> todosIntegrantes = integranteDAO.findAll();
        List<Asistencia> todasAsistencias = asistenciaDAO.findAll();

        Map<Long, List<Asistencia>> asistenciasPorIntegrante = todasAsistencias.stream()
                .filter(a -> a.getIntegrante() != null && a.getIntegrante().getId() != null)
                .collect(Collectors.groupingBy(a -> a.getIntegrante().getId()));

        List<HorasIntegranteDTO> todosDtos = new ArrayList<>();
        double sumaTotalHoras = 0.0;
        int miembrosConHoras = 0;

        for (Integrante integ : todosIntegrantes) {
            if (integ.getEstado() == EstadoIntegranteEnum.INACTIVO) continue;
            List<Asistencia> asists = asistenciasPorIntegrante.getOrDefault(integ.getId(), Collections.emptyList());
            HorasIntegranteDTO dto = calcularHorasParaIntegrante(integ, asists, 0);
            if (dto.getHorasTotales() > 0) {
                miembrosConHoras++;
                sumaTotalHoras += dto.getHorasTotales();
            }
            todosDtos.add(dto);
        }

        // Ordenar ranking descendente por horasTotales
        todosDtos.sort(Comparator.comparingDouble(HorasIntegranteDTO::getHorasTotales).reversed());

        // Asignar posiciones 1, 2, 3...
        for (int i = 0; i < todosDtos.size(); i++) {
            todosDtos.get(i).setPosicion(i + 1);
        }

        // Encontrar las horas del usuario que hace la solicitud
        final Long finalUsuarioId = usuarioId;
        HorasIntegranteDTO misHoras = null;
        if (finalUsuarioId != null) {
            misHoras = todosDtos.stream()
                    .filter(d -> d.getIntegranteId() != null && d.getIntegranteId().equals(finalUsuarioId))
                    .findFirst()
                    .orElse(null);
        }

        if (misHoras == null && finalUsuarioId != null) {
            Integrante yo = integranteDAO.findById(finalUsuarioId).orElse(null);
            if (yo != null) {
                misHoras = calcularHorasParaIntegrante(yo, Collections.emptyList(), todosDtos.size() + 1);
            }
        }

        sumaTotalHoras = Math.round(sumaTotalHoras * 10.0) / 10.0;

        // Solo se entrega el ranking completo a roles autorizados (ADMIN y DIRECTIVA)
        List<HorasIntegranteDTO> rankingAEnviar = esAutorizado ? todosDtos : Collections.emptyList();

        ResumenHorasDashboardDTO responseDto = new ResumenHorasDashboardDTO(
                misHoras,
                rankingAEnviar,
                sumaTotalHoras,
                miembrosConHoras,
                esAutorizado
        );

        return Response.ok(responseDto).build();
    }

    /**
     * Calcula las horas acumuladas, sesiones y nivel de reconocimiento (Tier)
     * de un integrante a partir de sus marcaciones de asistencia (ENTRADA / SALIDA).
     */
    public static HorasIntegranteDTO calcularHorasParaIntegrante(Integrante integrante, List<Asistencia> asistenciasDelIntegrante, int posicion) {
        if (integrante == null) return null;

        double minutosTotales = 0.0;
        boolean enElLabHoy = false;
        LocalDate hoy = LocalDate.now();

        Map<LocalDate, List<Asistencia>> porFecha = (asistenciasDelIntegrante != null ? asistenciasDelIntegrante : Collections.<Asistencia>emptyList())
                .stream()
                .filter(a -> a.getFechaHora() != null)
                .collect(Collectors.groupingBy(a -> a.getFechaHora().toLocalDate()));

        for (Map.Entry<LocalDate, List<Asistencia>> entry : porFecha.entrySet()) {
            LocalDate fecha = entry.getKey();
            List<Asistencia> delDia = new ArrayList<>(entry.getValue());
            delDia.sort(Comparator.comparing(Asistencia::getFechaHora));

            for (int i = 0; i < delDia.size(); i++) {
                Asistencia actual = delDia.get(i);
                if (actual.getTipoMarcacion() == TipoMarcacionEnum.ENTRADA) {
                    Asistencia salidaCorrespondiente = null;
                    for (int j = i + 1; j < delDia.size(); j++) {
                        if (delDia.get(j).getTipoMarcacion() == TipoMarcacionEnum.SALIDA) {
                            salidaCorrespondiente = delDia.get(j);
                            i = j;
                            break;
                        }
                    }

                    if (salidaCorrespondiente != null) {
                        long mins = Duration.between(actual.getFechaHora(), salidaCorrespondiente.getFechaHora()).toMinutes();
                        if (mins > 0) {
                            // Limitar a máximo 8 horas por turno de taller
                            minutosTotales += Math.min(mins, 480);
                        }
                    } else {
                        if (fecha.equals(hoy)) {
                            enElLabHoy = true;
                            long minsHoy = Duration.between(actual.getFechaHora(), LocalDateTime.now()).toMinutes();
                            if (minsHoy > 0) {
                                minutosTotales += Math.min(minsHoy, 240); // Maximo 4 horas activas hoy
                            }
                        } else {
                            // Cómputo estándar de 2 horas para sesiones pasadas sin marcación de salida
                            minutosTotales += 120;
                        }
                    }
                }
            }
        }

        double horasTotales = Math.round((minutosTotales / 60.0) * 10.0) / 10.0;
        int totalSesiones = porFecha.size();

        String tier;
        String tierNombre;
        String tierBadgeClass;
        String tierIcono;

        if (horasTotales >= 50.0) {
            tier = "DIAMANTE";
            tierNombre = "Tier Diamante (+50h)";
            tierBadgeClass = "badge-tier-diamante";
            tierIcono = "bi-gem";
        } else if (horasTotales >= 25.0) {
            tier = "ORO";
            tierNombre = "Tier Oro (25-50h)";
            tierBadgeClass = "badge-tier-oro";
            tierIcono = "bi-trophy-fill";
        } else if (horasTotales >= 10.0) {
            tier = "PLATA";
            tierNombre = "Tier Plata (10-25h)";
            tierBadgeClass = "badge-tier-plata";
            tierIcono = "bi-award-fill";
        } else {
            tier = "BRONCE";
            tierNombre = "Tier Bronce (<10h)";
            tierBadgeClass = "badge-tier-bronce";
            tierIcono = "bi-shield-check";
        }

        String carreraStr = (integrante.getCarrera() != null)
                ? integrante.getCarrera().name().replace('_', ' ')
                : "Politécnica";

        return new HorasIntegranteDTO(
                integrante.getId(),
                integrante.getNombre() + " " + integrante.getApellido(),
                integrante.getCarnetUniversitario(),
                carreraStr,
                horasTotales,
                totalSesiones,
                tier,
                tierNombre,
                tierBadgeClass,
                tierIcono,
                posicion,
                enElLabHoy
        );
    }

    /**
     * Retorna el ultimo UID RFID escaneado por el ESP32 para autorelleno en el frontend.
     * GET /api/asistencia/ultimo-escaneado
     */
    @GET
    @Path("/ultimo-escaneado")
    public Response obtenerUltimoEscaneado() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("nfcUid", ultimoNfcUidEscaneado);
        resp.put("timestamp", timestampUltimoEscaneo);
        resp.put("dispositivo", dispositivoUltimoEscaneo);
        return Response.ok(resp).build();
    }

    /**
     * Genera un nuevo UID unico de 8 caracteres hexadecimales garantizado no registrado.
     * GET /api/asistencia/generar-uid
     */
    @GET
    @Path("/generar-uid")
    public Response generarUidUnico() {
        String nuevoUid;
        java.security.SecureRandom random = new java.security.SecureRandom();
        py.edu.une.politecnica.robogest.dao.IntegranteDAO integDao = new IntegranteDAOJpaImpl();
        do {
            byte[] bytes = new byte[4];
            random.nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            nuevoUid = sb.toString();
        } while (integDao.findByNfcUid(nuevoUid).isPresent());

        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("nfcUid", nuevoUid);
        return Response.ok(resp).build();
    }

    /**
     * Activa el modo de grabacion para que el ESP32 escriba el UID en la proxima tarjeta que se acerque.
     * POST /api/asistencia/modo-grabacion
     */
    @POST
    @Path("/modo-grabacion")
    public Response activarModoGrabacion(java.util.Map<String, String> body, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (body == null || body.get("nfcUid") == null || body.get("nfcUid").isBlank()) {
            throw new IllegalArgumentException("Se requiere 'nfcUid' para activar el modo grabacion");
        }
        uidPendienteGrabar = body.get("nfcUid").trim().toUpperCase();
        timestampPendienteGrabar = System.currentTimeMillis();
        ultimoUidGrabado = null;
        timestampUltimoGrabado = 0;

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "MODO_GRABACION_ACTIVO");
        resp.put("nfcUid", uidPendienteGrabar);
        resp.put("timestamp", timestampPendienteGrabar);
        return Response.ok(resp).build();
    }

    /**
     * Endpoint consultado por el ESP32 para saber si debe escribir en la tarjeta en lugar de marcar asistencia.
     * GET /api/asistencia/pendiente-grabar
     */
    @GET
    @Path("/pendiente-grabar")
    public Response consultarPendienteGrabar() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        boolean activo = uidPendienteGrabar != null && (System.currentTimeMillis() - timestampPendienteGrabar < 60000);
        resp.put("grabar", activo);
        resp.put("nfcUid", activo ? uidPendienteGrabar : null);
        return Response.ok(resp).build();
    }

    /**
     * Confirmacion enviada por el ESP32 tras escribir con exito en la tarjeta fisica.
     * POST /api/asistencia/confirmar-grabacion
     */
    @POST
    @Path("/confirmar-grabacion")
    public Response confirmarGrabacion(java.util.Map<String, Object> body) {
        String grabado = uidPendienteGrabar;
        if (body != null && body.get("nfcUid") != null) {
            grabado = body.get("nfcUid").toString().trim().toUpperCase();
        }
        ultimoUidGrabado = grabado;
        timestampUltimoGrabado = System.currentTimeMillis();
        ultimoNfcUidEscaneado = ultimoUidGrabado;
        timestampUltimoEscaneo = timestampUltimoGrabado;
        uidPendienteGrabar = null;
        timestampPendienteGrabar = 0;

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "GRABADO_CONFIRMADO");
        resp.put("nfcUid", ultimoUidGrabado);
        resp.put("timestamp", timestampUltimoGrabado);
        return Response.ok(resp).build();
    }

    /**
     * Permite al frontend consultar si la grabacion fisica en el ESP32 ya fue confirmada.
     * GET /api/asistencia/estado-grabacion
     */
    @GET
    @Path("/estado-grabacion")
    public Response obtenerEstadoGrabacion() {
        boolean activo = uidPendienteGrabar != null && (System.currentTimeMillis() - timestampPendienteGrabar < 60000);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("activo", activo);
        resp.put("nfcUidPendiente", activo ? uidPendienteGrabar : null);
        resp.put("ultimoUidGrabado", ultimoUidGrabado);
        resp.put("timestampGrabado", timestampUltimoGrabado);
        return Response.ok(resp).build();
    }

    /**
     * Cancela el modo de grabacion pendiente.
     * POST /api/asistencia/cancelar-grabacion
     */
    @POST
    @Path("/cancelar-grabacion")
    public Response cancelarGrabacion() {
        uidPendienteGrabar = null;
        timestampPendienteGrabar = 0;
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "MODO_GRABACION_CANCELADO");
        return Response.ok(resp).build();
    }

    /**
     * Endpoint para recepcion de marcaciones IoT desde el ESP32.
     * POST /api/asistencia/marcar
     */
    @POST
    @Path("/marcar")
    public Response marcar(MarcacionRequestDTO request) {
        if (request == null || request.getNfcUid() == null || request.getNfcUid().isBlank()) {
            throw new IllegalArgumentException("El campo 'nfcUid' es requerido para procesar la marcacion");
        }

        // Registrar lectura en memoria para autorelleno en modales del frontend
        ultimoNfcUidEscaneado = request.getNfcUid().trim().toUpperCase();
        timestampUltimoEscaneo = System.currentTimeMillis();
        dispositivoUltimoEscaneo = request.getDispositivo() != null ? request.getDispositivo().trim() : "ESP32";

        Asistencia asistencia = asistenciaService.registrarMarcacion(
                request.getNfcUid(),
                request.getDispositivo()
        );

        String mensaje = String.format("Marcacion de %s registrada con exito para %s %s",
                asistencia.getTipoMarcacion(),
                asistencia.getIntegrante().getNombre(),
                asistencia.getIntegrante().getApellido()
        );

        MarcacionResponseDTO responseDTO = new MarcacionResponseDTO(
                asistencia.getId(),
                asistencia.getIntegrante().getId(),
                asistencia.getIntegrante().getNombre() + " " + asistencia.getIntegrante().getApellido(),
                asistencia.getIntegrante().getCarnetUniversitario(),
                asistencia.getTipoMarcacion().name(),
                asistencia.getFechaHora(),
                asistencia.getDispositivoUtilizado(),
                mensaje
        );

        return Response.ok(responseDTO).build();
    }

    /**
     * Endpoint para recepcion de marcaciones manuales (por UID, por seleccion de integrante o por CI/nombre).
     * POST /api/asistencia/marcar-manual
     */
    @POST
    @Path("/marcar-manual")
    public Response marcarManual(MarcacionManualRequestDTO request,
                                 @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no contiene datos para procesar la marcacion manual");
        }

        py.edu.une.politecnica.robogest.dao.IntegranteDAO daoIntegrante = this.integranteDAO;
        py.edu.une.politecnica.robogest.entity.Integrante integrante = null;

        // 1. Intentar ubicar por ID si fue seleccionado directamente
        if (request.getIntegranteId() != null) {
            integrante = daoIntegrante.findById(request.getIntegranteId()).orElse(null);
        }

        // 2. Si no, intentar ubicar por UID NFC
        if (integrante == null && request.getNfcUid() != null && !request.getNfcUid().isBlank()) {
            integrante = daoIntegrante.findByNfcUid(request.getNfcUid().trim().toUpperCase()).orElse(null);
        }

        // 3. Si no, intentar ubicar por Cedula de Identidad (CI)
        if (integrante == null && request.getCi() != null && !request.getCi().isBlank()) {
            integrante = daoIntegrante.findByCi(request.getCi().trim()).orElse(null);
        }

        // 4. Si todavia no existe y se cuenta con Nombre y Cedula, registrar nuevo Integrante en el padron
        if (integrante == null) {
            String ci = request.getCi() != null ? request.getCi().trim() : null;
            String nombreCompleto = request.getNombre() != null ? request.getNombre().trim() : "";
            if (ci == null || ci.isBlank() || nombreCompleto.isBlank()) {
                throw new IllegalArgumentException("No se encontro el integrante. Para registrar la entrada debes ingresar al menos Nombre y Cedula (CI).");
            }

            String[] partes = nombreCompleto.split("\\s+", 2);
            String nombre = partes[0];
            String apellido = partes.length > 1 ? partes[1] : (request.getApellido() != null && !request.getApellido().isBlank() ? request.getApellido().trim() : "Participante");

            py.edu.une.politecnica.robogest.entity.enums.CarreraEnum carreraEnum = py.edu.une.politecnica.robogest.entity.enums.CarreraEnum.COLABORADOR;
            if (request.getCarrera() != null && !request.getCarrera().isBlank()) {
                try {
                    carreraEnum = py.edu.une.politecnica.robogest.entity.enums.CarreraEnum.valueOf(request.getCarrera().trim().toUpperCase().replace(" ", "_"));
                } catch (Exception ignored) {
                }
            }

            String ciClean = ci.replaceAll("[^a-zA-Z0-9]", "");
            String email = "asistente_" + ciClean + "@fpune.edu.py";
            String carnet = "ASIST-" + ciClean;
            String nfcUidVal = (request.getNfcUid() != null && !request.getNfcUid().isBlank()) ? request.getNfcUid().trim().toUpperCase() : null;

            integrante = new py.edu.une.politecnica.robogest.entity.Integrante(
                    nombre,
                    apellido,
                    ci,
                    carnet,
                    null,
                    email,
                    java.time.LocalDate.now(),
                    py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum.ACTIVO,
                    nfcUidVal,
                    carreraEnum,
                    py.edu.une.politecnica.robogest.entity.enums.RolEnum.MIEMBRO,
                    org.mindrot.jbcrypt.BCrypt.hashpw(ci, org.mindrot.jbcrypt.BCrypt.gensalt())
            );
            integrante = daoIntegrante.save(integrante);
        }

        // Validar que el integrante este ACTIVO
        if (integrante.getEstado() == py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum.INACTIVO) {
            throw new py.edu.une.politecnica.robogest.exception.IntegranteInactivoException(
                    "El integrante " + integrante.getNombre() + " " + integrante.getApellido() + " se encuentra en estado INACTIVO"
            );
        }

        // Si se paso un nuevo UID y el integrante no tenia UID, asociarselo
        if (request.getNfcUid() != null && !request.getNfcUid().isBlank() && (integrante.getNfcUid() == null || integrante.getNfcUid().isBlank())) {
            integrante.setNfcUid(request.getNfcUid().trim().toUpperCase());
            daoIntegrante.update(integrante);
        }

        // 5. Determinar tipo de marcacion (ENTRADA, SALIDA o AUTO)
        py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum tipoMarcacion;
        String tipoReq = request.getTipoMarcacion() != null ? request.getTipoMarcacion().trim().toUpperCase() : "AUTO";
        if ("ENTRADA".equals(tipoReq)) {
            tipoMarcacion = py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.ENTRADA;
        } else if ("SALIDA".equals(tipoReq)) {
            tipoMarcacion = py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.SALIDA;
        } else {
            java.time.LocalDate hoy = java.time.LocalDate.now();
            java.util.Optional<py.edu.une.politecnica.robogest.entity.Asistencia> ultimaOpt = asistenciaDAO.findUltimaMarcacionDelDia(integrante.getId(), hoy);
            if (ultimaOpt.isEmpty()) {
                tipoMarcacion = py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.ENTRADA;
            } else {
                tipoMarcacion = (ultimaOpt.get().getTipoMarcacion() == py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.ENTRADA)
                        ? py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.SALIDA
                        : py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum.ENTRADA;
            }
        }

        String dispositivo = request.getDispositivo() != null && !request.getDispositivo().isBlank()
                ? request.getDispositivo().trim()
                : "REGISTRO_MANUAL_LAB";

        py.edu.une.politecnica.robogest.entity.Asistencia nuevaAsistencia = new py.edu.une.politecnica.robogest.entity.Asistencia(
                integrante,
                java.time.LocalDateTime.now(),
                tipoMarcacion,
                dispositivo
        );

        py.edu.une.politecnica.robogest.entity.Asistencia guardada = asistenciaDAO.save(nuevaAsistencia);

        String mensaje = String.format("Marcacion manual de %s registrada con exito para %s %s (CI: %s)",
                guardada.getTipoMarcacion(),
                integrante.getNombre(),
                integrante.getApellido(),
                integrante.getCi()
        );

        MarcacionResponseDTO responseDTO = new MarcacionResponseDTO(
                guardada.getId(),
                integrante.getId(),
                integrante.getNombre() + " " + integrante.getApellido(),
                integrante.getCarnetUniversitario(),
                guardada.getTipoMarcacion().name(),
                guardada.getFechaHora(),
                guardada.getDispositivoUtilizado(),
                mensaje
        );

        return Response.ok(responseDTO).build();
    }

    public Response listarMarcaciones() {
        return listarMarcaciones(SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response marcarManual(MarcacionManualRequestDTO request) {
        return marcarManual(request, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }
}
