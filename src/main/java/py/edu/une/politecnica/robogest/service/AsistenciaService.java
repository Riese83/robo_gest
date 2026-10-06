package py.edu.une.politecnica.robogest.service;

import py.edu.une.politecnica.robogest.entity.Asistencia;

/**
 * Servicio encargado del procesamiento y registro de asistencias IoT con dispositivos RFID/NFC.
 */
public interface AsistenciaService {

    /**
     * Registra una marcacion de asistencia recibida de un dispositivo IoT.
     * Busca al integrante por su nfcUid, valida que este ACTIVO y determina
     * alternadamente si corresponde a una ENTRADA o SALIDA segun su historial del dia.
     *
     * @param nfcUid Identificador hexadecimal unico de la tarjeta/tag NFC
     * @param dispositivo Identificador del dispositivo lector (ej. "ESP32_LAB_01")
     * @return El registro de asistencia creado y persistido
     */
    Asistencia registrarMarcacion(String nfcUid, String dispositivo);
}
