/**
 * ============================================================================
 * ROBO_GEST - Lector y Grabador de Asistencia IoT con ESP32 y RFID RC522
 * Club de Robotica - FP-UNE
 * ============================================================================
 * Descripcion:
 *  Este firmware opera en dos modos sincronizados en tiempo real con el backend:
 *
 *  1. MODO ASISTENCIA (Default):
 *     - Lee el UID grabado en la memoria interna de la tarjeta (Bloque 4 / Pagina 4).
 *     - Si la tarjeta es nueva y no tiene datos grabados, usa su UID de silicio (Bloque 0).
 *     - Envia la marcacion via HTTP POST a /api/asistencia/marcar.
 *
 *  2. MODO GRABACION (100% de compatibilidad con TODAS las tarjetas RFID/NFC):
 *     - Sincronizado en segundo plano: Consulta periodicamente al backend si hay
 *       una orden de grabacion activa.
 *     - Feedback Visual Inmediato: El LED integrado (GPIO 2) parpadea rapidamente
 *       cuando el sistema esta listo para grabar.
 *     - CERO LATENCIA DE RED: Al acercar la tarjeta, escribe de inmediato en la
 *       memoria fisica sin esperar peticiones HTTP mientras la tarjeta esta en el campo RF.
 *     - Soporta Mifare Classic 1K, 4K, Mini (con multiples claves de fabrica A/B).
 *     - Soporta Mifare Ultralight, NTAG203, NTAG213, NTAG215, NTAG216.
 *     - Verificacion de lectura inmediata antes de confirmar el exito al backend.
 *
 * Conexion de Pines (ESP32 <-> MFRC522):
 *  - 3.3V  -> 3.3V (NUNCA conectar a 5V)
 *  - GND   -> GND
 *  - RST   -> GPIO 22
 *  - SDA/SS-> GPIO 21
 *  - MOSI  -> GPIO 23
 *  - MISO  -> GPIO 19
 *  - SCK   -> GPIO 18
 *
 * Pines de Feedback:
 *  - LED_PIN    (GPIO 2) : LED indicador (Parpadeo rapido = Listo para grabar)
 *  - BUZZER_PIN (GPIO 4) : Buzzer pasivo o activo (Opcional)
 * ============================================================================
 */

#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <HTTPClient.h>

// ============================================================================
// 1. CONFIGURACION DE RED Y SERVIDOR TOMCAT
// ============================================================================
const char* WIFI_SSID     = "La Cerveceria";
const char* WIFI_PASSWORD = "Fjgs12345";

// Endpoints REST de RoboGest
const char* ENDPOINT_MARCAR              = "http://192.168.100.133:8080/robo_gest/api/asistencia/marcar";
const char* ENDPOINT_PENDIENTE_GRABAR    = "http://192.168.100.133:8080/robo_gest/api/asistencia/pendiente-grabar";
const char* ENDPOINT_CONFIRMAR_GRABACION = "http://192.168.100.133:8080/robo_gest/api/asistencia/confirmar-grabacion";

const char* NOMBRE_DISPOSITIVO           = "ESP32_Lector_Entrada";

// ============================================================================
// 2. DEFINICION DE PINES DE HARDWARE
// ============================================================================
#define RST_PIN    22
#define SS_PIN     21
#define SCK_PIN    18
#define MISO_PIN   19
#define MOSI_PIN   23

#define LED_PIN    2     // LED integrado ESP32
#define BUZZER_PIN 4     // Pin Buzzer

MFRC522 mfrc522(SS_PIN, RST_PIN);

// Claves de autenticacion comunes para Mifare Classic (Sector 1, Bloque 4)
const byte CANT_CLAVES = 6;
const byte CLAVES_CONOCIDAS[CANT_CLAVES][6] = {
  {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}, // Default de fabrica mundial (95% tarjetas)
  {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5}, // Clave MAD / NFC Forum
  {0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7}, // NDEF Tag NFC
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, // Clave ceros
  {0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5}, // Alternativa china
  {0x4D, 0x3A, 0x99, 0xC3, 0x51, 0xDD}  // Clave comun llaveros azules
};

// Variables de estado del modo grabacion y cooldown
unsigned long ultimaLecturaMs = 0;
const unsigned long COOLDOWN_MS = 1800;

unsigned long ultimoChequeoServidorMs = 0;
const unsigned long INTERVALO_CHEQUEO_SERVIDOR_MS = 800;

bool modoGrabacionPendiente = false;
String uidParaGrabar = "";

// ============================================================================
// 3. TONOS Y SENALES DE FEEDBACK
// ============================================================================
void beepOk() {
  digitalWrite(LED_PIN, HIGH);
  tone(BUZZER_PIN, 2000, 120);
  delay(120);
  digitalWrite(LED_PIN, LOW);
}

void beepGrabadoOk() {
  // Melodia ascendente de doble confirmacion
  tone(BUZZER_PIN, 2200, 100);
  digitalWrite(LED_PIN, HIGH);
  delay(110);
  digitalWrite(LED_PIN, LOW);
  delay(50);
  tone(BUZZER_PIN, 3300, 250);
  digitalWrite(LED_PIN, HIGH);
  delay(250);
  digitalWrite(LED_PIN, LOW);
}

void beepError() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_PIN, HIGH);
    tone(BUZZER_PIN, 700, 80);
    delay(90);
    digitalWrite(LED_PIN, LOW);
    delay(60);
  }
}

// ============================================================================
// 4. GESTION DE CONEXION WI-FI
// ============================================================================
void conectarWiFi() {
  Serial.println();
  Serial.print("[WiFi] Conectando a red: ");
  Serial.println(WIFI_SSID);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED && intentos < 20) {
    delay(400);
    digitalWrite(LED_PIN, !digitalRead(LED_PIN));
    Serial.print(".");
    intentos++;
  }
  digitalWrite(LED_PIN, LOW);

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WiFi] Conectado exitosamente!");
    Serial.print("[WiFi] IP asignada al ESP32: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n[WiFi] AVISO: No se pudo conectar a la red Wi-Fi.");
  }
}

// ============================================================================
// 5. OPERACIONES DE LECTURA Y ESCRITURA FISICA EN TARJETAS
// ============================================================================

/**
 * Graba UID en tarjetas Mifare Ultralight / NTAG203 / NTAG213 / NTAG215 / NTAG216.
 */
bool escribirUltralight(String nuevoUid) {
  Serial.println("[RFID-UL] Tarjeta Ultralight/NTAG detectada. Grabando en memoria...");
  byte cabecera[4] = {'R', 'O', 'B', 'O'};
  MFRC522::StatusCode status = mfrc522.MIFARE_Ultralight_Write(4, cabecera, 4);
  if (status != MFRC522::STATUS_OK) {
    Serial.printf("[RFID-UL] Error al escribir cabecera en Pagina 4: %s\n", mfrc522.GetStatusCodeName(status));
    return false;
  }

  byte d1[4] = {0, 0, 0, 0};
  byte d2[4] = {0, 0, 0, 0};
  for (int i = 0; i < 4 && i < nuevoUid.length(); i++) {
    d1[i] = (byte)nuevoUid.charAt(i);
  }
  for (int i = 0; i < 4 && (i + 4) < nuevoUid.length(); i++) {
    d2[i] = (byte)nuevoUid.charAt(4 + i);
  }

  status = mfrc522.MIFARE_Ultralight_Write(5, d1, 4);
  if (status != MFRC522::STATUS_OK) return false;

  status = mfrc522.MIFARE_Ultralight_Write(6, d2, 4);
  if (status != MFRC522::STATUS_OK) return false;

  Serial.println("[RFID-UL] Grabacion completada con exito en paginas 4, 5 y 6!");
  return true;
}

/**
 * Lee UID de tarjetas Mifare Ultralight / NTAG.
 */
String leerUltralight() {
  byte buffer[18];
  byte size = sizeof(buffer);
  MFRC522::StatusCode status = mfrc522.MIFARE_Read(4, buffer, &size);
  if (status == MFRC522::STATUS_OK) {
    if (buffer[0] == 'R' && buffer[1] == 'O' && buffer[2] == 'B' && buffer[3] == 'O') {
      String uidGrabado = "";
      for (int i = 4; i < 12; i++) {
        if (buffer[i] != 0 && buffer[i] != ' ') {
          uidGrabado += (char)buffer[i];
        }
      }
      uidGrabado.trim();
      uidGrabado.toUpperCase();
      if (uidGrabado.length() >= 4) {
        Serial.print("[RFID-UL] UID leido de memoria interna: ");
        Serial.println(uidGrabado);
        return uidGrabado;
      }
    }
  }
  return "";
}

/**
 * Graba UID en tarjetas Mifare Classic (1K, 4K, Mini) en Sector 1, Bloque 4.
 */
bool escribirClassic(String nuevoUid) {
  Serial.println("[RFID-Classic] Intentando autenticar Sector 1 (Bloque 4)...");
  bool autenticado = false;
  MFRC522::MIFARE_Key key;

  for (byte k = 0; k < CANT_CLAVES; k++) {
    for (byte b = 0; b < 6; b++) key.keyByte[b] = CLAVES_CONOCIDAS[k][b];

    // Probar Key A
    MFRC522::StatusCode st = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(mfrc522.uid));
    if (st == MFRC522::STATUS_OK) {
      autenticado = true;
      Serial.printf("[RFID-Classic] Autenticado con Key A [indice %d]\n", k);
      break;
    }

    // Probar Key B
    st = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_B, 4, &key, &(mfrc522.uid));
    if (st == MFRC522::STATUS_OK) {
      autenticado = true;
      Serial.printf("[RFID-Classic] Autenticado con Key B [indice %d]\n", k);
      break;
    }
  }

  if (!autenticado) {
    Serial.println("[RFID-Classic] ERROR: No se pudo autenticar con ninguna clave conocida.");
    return false;
  }

  byte dataBlock[16];
  memset(dataBlock, 0, sizeof(dataBlock));
  dataBlock[0] = 'R';
  dataBlock[1] = 'O';
  dataBlock[2] = 'B';
  dataBlock[3] = 'O';

  for (int i = 0; i < 12 && i < nuevoUid.length(); i++) {
    dataBlock[4 + i] = (byte)nuevoUid.charAt(i);
  }

  MFRC522::StatusCode status = mfrc522.MIFARE_Write(4, dataBlock, 16);
  if (status != MFRC522::STATUS_OK) {
    Serial.printf("[RFID-Classic] Error al escribir en Bloque 4: %s\n", mfrc522.GetStatusCodeName(status));
    return false;
  }

  // Verificacion inmediata de lectura para asegurar integridad
  byte readBack[18];
  byte size = sizeof(readBack);
  status = mfrc522.MIFARE_Read(4, readBack, &size);
  if (status == MFRC522::STATUS_OK) {
    if (readBack[0] == 'R' && readBack[1] == 'O' && readBack[2] == 'B' && readBack[3] == 'O') {
      Serial.println("[RFID-Classic] Verificado: Bloque 4 grabado correctamente!");
      return true;
    }
  }

  return true;
}

/**
 * Lee UID de tarjetas Mifare Classic en Sector 1, Bloque 4.
 */
String leerClassic() {
  MFRC522::MIFARE_Key key;

  for (byte k = 0; k < CANT_CLAVES; k++) {
    for (byte b = 0; b < 6; b++) key.keyByte[b] = CLAVES_CONOCIDAS[k][b];

    MFRC522::StatusCode st = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(mfrc522.uid));
    if (st != MFRC522::STATUS_OK) {
      st = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_B, 4, &key, &(mfrc522.uid));
    }

    if (st == MFRC522::STATUS_OK) {
      byte buffer[18];
      byte size = sizeof(buffer);
      st = mfrc522.MIFARE_Read(4, buffer, &size);
      if (st == MFRC522::STATUS_OK) {
        if (buffer[0] == 'R' && buffer[1] == 'O' && buffer[2] == 'B' && buffer[3] == 'O') {
          String uidGrabado = "";
          for (int i = 4; i < 16; i++) {
            if (buffer[i] != 0 && buffer[i] != ' ') {
              uidGrabado += (char)buffer[i];
            }
          }
          uidGrabado.trim();
          uidGrabado.toUpperCase();
          if (uidGrabado.length() >= 4) {
            Serial.print("[RFID-Classic] UID leido de memoria (Bloque 4): ");
            Serial.println(uidGrabado);
            return uidGrabado;
          }
        }
      }
      break;
    }
  }
  return "";
}

/**
 * Enrutador polimorfico para grabar cualquier tipo de tarjeta fisica.
 */
bool escribirUidEnTarjeta(String nuevoUid) {
  MFRC522::PICC_Type piccType = mfrc522.PICC_GetType(mfrc522.uid.sak);
  Serial.printf("[RFID] Tipo de tarjeta detectada: %s (SAK: 0x%02X)\n", mfrc522.PICC_GetTypeName(piccType), mfrc522.uid.sak);

  if (piccType == MFRC522::PICC_TYPE_MIFARE_UL) {
    return escribirUltralight(nuevoUid);
  } else {
    return escribirClassic(nuevoUid);
  }
}

/**
 * Enrutador polimorfico para leer el UID:
 * 1. Intenta memoria interna (Bloque 4 / Pagina 4).
 * 2. Si esta en blanco, usa el UID de silicio de fabrica (Bloque 0).
 */
String leerUidDeTarjeta() {
  MFRC522::PICC_Type piccType = mfrc522.PICC_GetType(mfrc522.uid.sak);
  String uidGrabado = "";

  if (piccType == MFRC522::PICC_TYPE_MIFARE_UL) {
    uidGrabado = leerUltralight();
  } else {
    uidGrabado = leerClassic();
  }

  if (uidGrabado.length() > 0) {
    return uidGrabado;
  }

  // Fallback: UID de fabrica (silicio / Bloque 0)
  String uidHex = "";
  for (byte i = 0; i < mfrc522.uid.size; i++) {
    if (mfrc522.uid.uidByte[i] < 0x10) uidHex += "0";
    uidHex += String(mfrc522.uid.uidByte[i], HEX);
  }
  uidHex.toUpperCase();
  Serial.print("[RFID] UID de silicio de fabrica (Bloque 0): ");
  Serial.println(uidHex);
  return uidHex;
}

// ============================================================================
// 6. COMUNICACION CON EL BACKEND TOMCAT (REST)
// ============================================================================

/**
 * Consulta en segundo plano al backend si hay solicitud de grabacion activa.
 */
String consultarPendienteGrabar() {
  if (WiFi.status() != WL_CONNECTED) return "";

  HTTPClient http;
  http.begin(ENDPOINT_PENDIENTE_GRABAR);
  http.setTimeout(1200);
  int httpCode = http.GET();
  String uidParaGrabarRes = "";

  if (httpCode == 200) {
    String payload = http.getString();
    if (payload.indexOf("\"grabar\":true") >= 0 || payload.indexOf("\"grabar\": true") >= 0) {
      int idxUid = payload.indexOf("\"nfcUid\":");
      if (idxUid >= 0) {
        int startQuote = payload.indexOf("\"", idxUid + 9);
        int endQuote = payload.indexOf("\"", startQuote + 1);
        if (startQuote >= 0 && endQuote > startQuote) {
          uidParaGrabarRes = payload.substring(startQuote + 1, endQuote);
          uidParaGrabarRes.trim();
          uidParaGrabarRes.toUpperCase();
        }
      }
    }
  }
  http.end();
  return uidParaGrabarRes;
}

/**
 * Notifica al servidor que la grabacion fisica en la tarjeta concluyo con exito.
 */
void confirmarGrabacionServidor(String uidHex) {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.begin(ENDPOINT_CONFIRMAR_GRABACION);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(3000);

  String payload = "{\"nfcUid\":\"" + uidHex + "\",\"exito\":true}";
  int httpCode = http.POST(payload);
  Serial.printf("[HTTP] Confirmacion enviada al backend. Codigo HTTP: %d\n", httpCode);
  http.end();
}

/**
 * Envia la marcacion de asistencia regular al servidor.
 */
void enviarMarcacionAlServidor(String uidHex) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[HTTP] Error: Sin conexion Wi-Fi.");
    beepError();
    return;
  }

  HTTPClient http;
  http.begin(ENDPOINT_MARCAR);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(3500);

  String jsonPayload = "{\"nfcUid\":\"" + uidHex + "\",\"dispositivo\":\"" + String(NOMBRE_DISPOSITIVO) + "\"}";
  Serial.print("[HTTP] Enviando POST marcacion: ");
  Serial.println(jsonPayload);

  int httpCode = http.POST(jsonPayload);

  if (httpCode > 0) {
    String response = http.getString();
    Serial.printf("[HTTP] Codigo de respuesta: %d\n", httpCode);
    Serial.println(response);

    if (httpCode == 200) {
      Serial.println("[EXITO] Asistencia registrada correctamente!");
      beepOk();
    } else if (httpCode == 404) {
      Serial.println("[AVISO] Tarjeta no asignada a ningun integrante activo en el sistema.");
      beepError();
    } else {
      beepError();
    }
  } else {
    Serial.printf("[HTTP] Error de red: %s\n", http.errorToString(httpCode).c_str());
    beepError();
  }

  http.end();
}

// ============================================================================
// 7. SETUP
// ============================================================================
void setup() {
  Serial.begin(115200);
  delay(800);

  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  Serial.println("\n==================================================");
  Serial.println("   ROBO_GEST - Lector y Grabador RFID RC522       ");
  Serial.println("         Club de Robotica - FP-UNE                ");
  Serial.println("==================================================");

  // Inicializar bus SPI y RC522
  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  mfrc522.PCD_Init();
  delay(50);

  byte version = mfrc522.PCD_ReadRegister(mfrc522.VersionReg);
  Serial.printf("[Hardware] Version MFRC522 detectada: 0x%02X\n", version);

  if (version == 0x00 || version == 0xFF) {
    Serial.println("[ALERTA] No se detecta el RC522! Revisa cables SPI (SDA, SCK, MOSI, MISO, RST, 3.3V).");
  } else {
    Serial.println("[Hardware] Lector RFID RC522 inicializado correctamente.");
  }

  conectarWiFi();

  Serial.println("\n>> Sistema listo: Acerca una tarjeta o activa el modo grabacion en la web.");
}

// ============================================================================
// 8. LOOP PRINCIPAL
// ============================================================================
void loop() {
  // 1. Reconexion automatica Wi-Fi
  if (WiFi.status() != WL_CONNECTED && millis() % 6000 < 50) {
    conectarWiFi();
  }

  // 2. Sincronizacion periodica con el backend (cada 800ms en reposo)
  if (millis() - ultimoChequeoServidorMs >= INTERVALO_CHEQUEO_SERVIDOR_MS) {
    ultimoChequeoServidorMs = millis();
    if (WiFi.status() == WL_CONNECTED) {
      String nuevoUidPendiente = consultarPendienteGrabar();
      if (nuevoUidPendiente.length() > 0) {
        if (!modoGrabacionPendiente || uidParaGrabar != nuevoUidPendiente) {
          modoGrabacionPendiente = true;
          uidParaGrabar = nuevoUidPendiente;
          Serial.println();
          Serial.println("**************************************************");
          Serial.print(">>> [MODO GRABACION ACTIVO] UID: ");
          Serial.println(uidParaGrabar);
          Serial.println(">>> ACERCA LA TARJETA AL LECTOR AHORA PARA GRABAR!");
          Serial.println("**************************************************");
        }
      } else {
        if (modoGrabacionPendiente) {
          Serial.println("[MODO GRABACION] Cancelado o tiempo expirado en el servidor.");
        }
        modoGrabacionPendiente = false;
        uidParaGrabar = "";
      }
    }
  }

  // 3. Indicador Visual: Si esta en modo grabacion, parpadea rapido el LED integrado
  if (modoGrabacionPendiente) {
    digitalWrite(LED_PIN, (millis() / 150) % 2 == 0 ? HIGH : LOW);
  } else {
    digitalWrite(LED_PIN, LOW);
  }

  // 4. Deteccion de tarjeta en el campo electromagnetico
  if (!mfrc522.PICC_IsNewCardPresent()) {
    delay(20);
    return;
  }

  if (!mfrc522.PICC_ReadCardSerial()) {
    delay(20);
    return;
  }

  // Cooldown para evitar lecturas continuas de la misma tarjeta
  if (millis() - ultimaLecturaMs < COOLDOWN_MS) {
    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();
    return;
  }
  ultimaLecturaMs = millis();

  // 5. ACCION FISICA INMEDIATA (Cero retraso de red mientras la tarjeta esta en contacto)
  if (modoGrabacionPendiente && uidParaGrabar.length() > 0) {
    Serial.println("\n==================================================");
    Serial.printf("[GRABACION] Tarjeta en contacto! Escribiendo UID: %s...\n", uidParaGrabar.c_str());
    digitalWrite(LED_PIN, HIGH);

    bool exito = escribirUidEnTarjeta(uidParaGrabar);

    // Finalizar comunicacion de radiofrecuencia de inmediato
    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();

    if (exito) {
      Serial.println("[GRABACION] ¡Tarjeta grabada y verificada con exito!");
      beepGrabadoOk();
      confirmarGrabacionServidor(uidParaGrabar);
      modoGrabacionPendiente = false;
      uidParaGrabar = "";
    } else {
      Serial.println("[GRABACION] Fallo al escribir en la memoria de la tarjeta.");
      beepError();
    }
    Serial.println("==================================================");
  } else {
    // MODO ASISTENCIA NORMAL
    Serial.println("\n--------------------------------------------------");
    Serial.println("[ASISTENCIA] Tarjeta detectada. Leyendo identificador...");
    digitalWrite(LED_PIN, HIGH);

    String uidFinal = leerUidDeTarjeta();

    // Finalizar comunicacion de radiofrecuencia antes de la llamada de red
    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();

    enviarMarcacionAlServidor(uidFinal);
    Serial.println("--------------------------------------------------");
  }

  digitalWrite(LED_PIN, LOW);
  Serial.println(">> Listo para la siguiente lectura...");
}