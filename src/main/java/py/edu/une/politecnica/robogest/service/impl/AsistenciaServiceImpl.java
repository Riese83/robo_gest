package py.edu.une.politecnica.robogest.service.impl;

import jakarta.transaction.Transactional;
import py.edu.une.politecnica.robogest.dao.AsistenciaDAO;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;
import py.edu.une.politecnica.robogest.exception.IntegranteInactivoException;
import py.edu.une.politecnica.robogest.exception.IntegranteNoEncontradoException;
import py.edu.une.politecnica.robogest.service.AsistenciaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementacion del servicio de asistencias IoT con integracion RFID/NFC.
 */
public class AsistenciaServiceImpl implements AsistenciaService {

    private final IntegranteDAO integranteDAO;
    private final AsistenciaDAO asistenciaDAO;

    public AsistenciaServiceImpl(IntegranteDAO integranteDAO, AsistenciaDAO asistenciaDAO) {
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.asistenciaDAO = Objects.requireNonNull(asistenciaDAO, "asistenciaDAO no puede ser null");
    }

    @Override
    @Transactional
    public Asistencia registrarMarcacion(String nfcUid, String dispositivo) {
        if (nfcUid == null || nfcUid.isBlank()) {
            throw new IllegalArgumentException("El nfcUid no puede ser nulo ni vacio");
        }

        String uidNormalizado = nfcUid.trim().toUpperCase();

        // 1. Buscar integrante por su identificador NFC
        Integrante integrante = integranteDAO.findByNfcUid(uidNormalizado)
                .orElseThrow(() -> new IntegranteNoEncontradoException(
                        "No se encontro ningun integrante registrado con el identificador NFC: " + uidNormalizado
                ));

        // 2. Validar que el integrante no este INACTIVO
        if (integrante.getEstado() == EstadoIntegranteEnum.INACTIVO) {
            throw new IntegranteInactivoException(String.format(
                    "El integrante %s %s (CI: %s) se encuentra en estado INACTIVO",
                    integrante.getNombre(), integrante.getApellido(), integrante.getCi()
            ));
        }

        // 3. Determinar ENTRADA o SALIDA segun la ultima marcacion del dia de hoy
        LocalDate hoy = LocalDate.now();
        Optional<Asistencia> ultimaMarcacionOpt = asistenciaDAO.findUltimaMarcacionDelDia(integrante.getId(), hoy);

        TipoMarcacionEnum tipoMarcacion;
        if (ultimaMarcacionOpt.isEmpty()) {
            // Primera marcacion del dia -> ENTRADA
            tipoMarcacion = TipoMarcacionEnum.ENTRADA;
        } else {
            // Si la ultima fue ENTRADA, la siguiente es SALIDA; y viceversa
            TipoMarcacionEnum ultima = ultimaMarcacionOpt.get().getTipoMarcacion();
            tipoMarcacion = (ultima == TipoMarcacionEnum.ENTRADA)
                    ? TipoMarcacionEnum.SALIDA
                    : TipoMarcacionEnum.ENTRADA;
        }

        // 4. Crear y persistir el nuevo registro de asistencia
        Asistencia nuevaAsistencia = new Asistencia(
                integrante,
                LocalDateTime.now(),
                tipoMarcacion,
                dispositivo != null && !dispositivo.isBlank() ? dispositivo.trim() : "DISPOSITIVO_DESCONOCIDO"
        );

        return asistenciaDAO.save(nuevaAsistencia);
    }
}
