#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <time.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>

// =====================================================================
// 1. FILL IN YOUR WI-FI (Must be 2.4GHz) & FIREBASE DATABASE URL
// =====================================================================
const char* WIFI_SSID     = "ESPGATE";
const char* WIFI_PASSWORD = "123098123";

// Paste your Firebase Realtime Database URL WITHOUT a trailing slash "/"
const char* FIREBASE_HOST = "https://device-streaming-ebe74626-default-rtdb.asia-southeast1.firebasedatabase.app";

// --- PIN DEFINITIONS (Unchanged Wiring) ---
#define VIB1_PIN         26   // Vibration Sensor 1 (Zone 1)
#define VIB2_PIN         13   // Vibration Sensor 2 (Zone 2)
#define PIR_PIN          27   // PIR Motion Sensor
#define LDR_PIN          25   // 3-Pin LDR Module
#define BUZZER_PIN       14   // Button Buzzer
#define RELAY1_PIN       32   // Relay 1: Night Floodlight (Steady ON at night)
#define RELAY2_PIN       33   // Relay 2: Red/Blue Police Strobe (Toggles NO/NC)
#define STROBE_POWER_PIN 23   // Optional: Powers Relay 2 COM for small LEDs only during alert
#define ONBOARD_LED      2    // Onboard Blue Status LED (SOLID ON = Wi-Fi Connected)

// --- POLARITY SETTINGS ---
#define NIGHT_STATE      HIGH // HIGH when dark on standard 3-pin LDR module
#define RELAY_ON         LOW  // Active-LOW for Songle relay modules
#define RELAY_OFF        HIGH

// --- TIMING CONSTANTS (milliseconds) ---
const unsigned long COINCIDENCE_WINDOW_MS = 5000; // 5-second window for (VIB1 or VIB2) + PIR
const unsigned long ALERT_DURATION_MS     = 8000; // 8-second total police strobe & alarm duration
const unsigned long STROBE_INTERVAL_MS    = 250;  // 250ms per flash (Red 250ms <-> Blue 250ms)
const unsigned long STATUS_PRINT_MS       = 1000; // Live sensor status print interval

// --- STATE VARIABLES ---
unsigned long lastVib1Time   = 0;
unsigned long lastVib2Time   = 0;
unsigned long lastPirTime    = 0;
unsigned long lastStatusTime = 0;

bool vib1Triggered = false;
bool vib2Triggered = false;
bool pirTriggered  = false;

// --- HELPER: Audio + LED Feedback Patterns ---
void playWiFiConnectedSignal() {
  // 3 quick beeps + flashes to confirm Wi-Fi is connected
  for (int i = 0; i < 3; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    digitalWrite(ONBOARD_LED, HIGH);
    delay(100);
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(ONBOARD_LED, LOW);
    delay(100);
  }
  // Keep Onboard Blue LED SOLID ON while Wi-Fi is active and system is armed
  digitalWrite(ONBOARD_LED, HIGH);
}

void playWiFiErrorSignal() {
  // 2 long error beeps + rapid LED flashes when Wi-Fi fails or drops
  for (int i = 0; i < 2; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    for (int j = 0; j < 3; j++) {
      digitalWrite(ONBOARD_LED, HIGH);
      delay(100);
      digitalWrite(ONBOARD_LED, LOW);
      delay(100);
    }
    digitalWrite(BUZZER_PIN, LOW);
    delay(300);
  }
}

// --- HELPER: Formatted Timestamp from NTP ---
String getFormattedTime() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo, 100)) {
    return String("Uptime: ") + String(millis() / 1000) + "s";
  }
  char timeStr[30];
  strftime(timeStr, sizeof(timeStr), "%Y-%m-%d %H:%M:%S", &timeinfo);
  return String(timeStr);
}

// --- HELPER: Send JSON to Firebase Realtime Database ---
void sendToFirebase(const String& path, const String& method, const String& jsonPayload) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[FIREBASE] Aborted: Wi-Fi not connected.");
    return;
  }

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient https;
  String url = String(FIREBASE_HOST) + path;

  if (https.begin(client, url)) {
    https.addHeader("Content-Type", "application/json");
    int httpCode = -1;

    if (method == "POST") {
      httpCode = https.POST(jsonPayload);
    } else if (method == "PUT") {
      httpCode = https.PUT(jsonPayload);
    }

    if (httpCode > 0) {
      Serial.printf("[FIREBASE %s] Success (%d) -> %s\n", method.c_str(), httpCode, path.c_str());
    } else {
      Serial.printf("[FIREBASE ERROR] %s\n", https.errorToString(httpCode).c_str());
    }
    https.end();
  }
}

// --- ASYNCHRONOUS FIREBASE DISPATCH TYPES & QUEUE ---
struct FirebaseMessage {
  char path[64];
  char method[8];
  char payload[512];
};

static QueueHandle_t firebaseQueue = NULL;

// --- BACKGROUND WORKER TASK: Offloads HTTPS requests to Core 0 (Zero lag for hardware) ---
void firebaseWorkerTask(void* pvParameters) {
  FirebaseMessage msg;
  for (;;) {
    if (xQueueReceive(firebaseQueue, &msg, portMAX_DELAY) == pdTRUE) {
      if (WiFi.status() == WL_CONNECTED) {
        sendToFirebase(String(msg.path), String(msg.method), String(msg.payload));
        // Allow lwIP TCP stack 25ms to cleanly release SSL session and feed Core 0 TWDT
        vTaskDelay(pdMS_TO_TICKS(25));
      } else {
        Serial.printf("[FIREBASE QUEUE] Dropped request to %s (Wi-Fi offline)\n", msg.path);
      }
    }
  }
}

// --- HELPER: Non-blocking enqueue for Firebase requests ---
void queueFirebaseSend(const char* path, const char* method, const String& payload) {
  if (firebaseQueue == NULL) {
    Serial.println("[FIREBASE QUEUE] Error: Queue not initialized!");
    return;
  }
  FirebaseMessage msg;
  strncpy(msg.path, path, sizeof(msg.path) - 1);
  msg.path[sizeof(msg.path) - 1] = '\0';

  strncpy(msg.method, method, sizeof(msg.method) - 1);
  msg.method[sizeof(msg.method) - 1] = '\0';

  strncpy(msg.payload, payload.c_str(), sizeof(msg.payload) - 1);
  msg.payload[sizeof(msg.payload) - 1] = '\0';

  if (xQueueSend(firebaseQueue, &msg, 0) != pdTRUE) {
    Serial.printf("[FIREBASE QUEUE] Warning: Queue full, dropped request to %s\n", path);
  } else {
    Serial.printf("[FIREBASE QUEUE] Enqueued %s -> %s\n", method, path);
  }
}

// --- BLOCKING WI-FI GUARD: System will NOT run until Wi-Fi is connected ---
void requireWiFiConnection() {
  if (WiFi.status() == WL_CONNECTED) {
    return; // Already connected!
  }

  // Ensure all relays and sensor latches are safely OFF while offline
  digitalWrite(RELAY1_PIN, RELAY_OFF);
  digitalWrite(RELAY2_PIN, RELAY_OFF);
  digitalWrite(STROBE_POWER_PIN, LOW);
  digitalWrite(ONBOARD_LED, LOW);
  vib1Triggered = false;
  vib2Triggered = false;
  pirTriggered  = false;

  // Keep trying in a blocking loop until Wi-Fi connects
  while (WiFi.status() != WL_CONNECTED) {
    Serial.printf("\n[WIFI] Connecting to %s ", WIFI_SSID);
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
      // Slow blink LED while searching; short 40ms chirp every 3rd attempt (1.5s)
      digitalWrite(ONBOARD_LED, HIGH);
      if (attempts % 3 == 0) {
        digitalWrite(BUZZER_PIN, HIGH);
        delay(40);
        digitalWrite(BUZZER_PIN, LOW);
        delay(460);
      } else {
        delay(500);
      }
      digitalWrite(ONBOARD_LED, LOW);
      delay(500);
      Serial.print(".");
      attempts++;
    }

    // If still not connected after 20 seconds, sound the error alert and retry
    if (WiFi.status() != WL_CONNECTED) {
      Serial.println("\n[WIFI ERROR] Connection failed! System halted. Retrying in 2s...");
      playWiFiErrorSignal();
      delay(2000);
    }
  }

  // Wi-Fi is now connected!
  Serial.printf("\n[WIFI] Connected! IP Address: %s\n", WiFi.localIP().toString().c_str());
  playWiFiConnectedSignal(); // 3 quick beeps + Solid Blue LED ON

  // Sync clock and mark system ONLINE in Firebase
  configTime(19800, 0, "pool.ntp.org", "time.nist.gov");
  queueFirebaseSend("/live_status.json", "PUT",
                 "{\"state\":\"SAFE\",\"activeZone\":\"All Sectors Clear\",\"lighting\":\"ONLINE\"}");
}

void triggerElephantAlert() {
  Serial.println("\n=======================================================");
  Serial.println("!!! ELEPHANT ALERT: ACTIVATING POLICE STROBE & ALARM !!!");

  String zoneName;
  String severity;
  float simLat = 7.9570;
  float simLng = 80.7603;

  if (vib1Triggered && vib2Triggered) {
    Serial.println("[ZONE INFO] CRITICAL: Both Zone 1 (D26) & Zone 2 (D13) shaking!");
    zoneName = "Sector A & B - Main Village Perimeter";
    severity = "CRITICAL (Dual-Zone Breach)";
    simLat   = 7.9582;
    simLng   = 80.7621;
  } else if (vib1Triggered) {
    Serial.println("[ZONE INFO] Tremor detected in Zone 1 (D26).");
    zoneName = "Sector A - North Forest Border (Zone 1)";
    severity = "HIGH (Zone 1 Tremor + Motion)";
    simLat   = 7.9570;
    simLng   = 80.7603;
  } else {
    Serial.println("[ZONE INFO] Tremor detected in Zone 2 (D13).");
    zoneName = "Sector B - East Waterhole Fence (Zone 2)";
    severity = "HIGH (Zone 2 Tremor + Motion)";
    simLat   = 7.9595;
    simLng   = 80.7640;
  }
  Serial.println("=======================================================");

  // 1. Check Day/Night state from LDR for Relay 1 (Steady Night Floodlight)
  bool isNight = (digitalRead(LDR_PIN) == NIGHT_STATE);
  if (isNight) {
    Serial.println("[LDR + RELAY 1] NIGHT detected -> Floodlight Relay (D32) STEADY ON");
    digitalWrite(RELAY1_PIN, RELAY_ON);
  } else {
    Serial.println("[LDR + RELAY 1] DAYLIGHT detected -> Floodlight Relay (D32) stays OFF");
    digitalWrite(RELAY1_PIN, RELAY_OFF);
  }

  // 2. IMMEDIATELY ACTIVATE ALERT OUTPUTS (Zero Lag!)
  digitalWrite(STROBE_POWER_PIN, HIGH);
  digitalWrite(RELAY2_PIN, RELAY_ON);
  digitalWrite(BUZZER_PIN, HIGH);
  digitalWrite(ONBOARD_LED, HIGH);

  // 3. Prepare Alert Event + Live State and Enqueue for background Firebase dispatch
  String timestamp = getFormattedTime();
  String lightInfo = isNight ? "NIGHT (Floodlight ON)" : "DAY (Floodlight OFF)";

  String eventJson = "{";
  eventJson += "\"zone\":\"" + zoneName + "\",";
  eventJson += "\"severity\":\"" + severity + "\",";
  eventJson += "\"lighting\":\"" + lightInfo + "\",";
  eventJson += "\"lat\":" + String(simLat, 4) + ",";
  eventJson += "\"lng\":" + String(simLng, 4) + ",";
  eventJson += "\"timestamp\":\"" + timestamp + "\"";
  eventJson += "}";

  String liveAlertJson = "{";
  liveAlertJson += "\"state\":\"ALERT\",";
  liveAlertJson += "\"activeZone\":\"" + zoneName + "\",";
  liveAlertJson += "\"lighting\":\"" + lightInfo + "\",";
  liveAlertJson += "\"lat\":" + String(simLat, 4) + ",";
  liveAlertJson += "\"lng\":" + String(simLng, 4) + ",";
  liveAlertJson += "\"updatedAt\":\"" + timestamp + "\"";
  liveAlertJson += "}";

  Serial.println("[CLOUD] Queueing alert events for background Firebase dispatch...");
  queueFirebaseSend("/live_status.json", "PUT", liveAlertJson);
  queueFirebaseSend("/alerts.json", "POST", eventJson);

  // 4. Alternate Relay 2 (Red NC <-> Blue NO) + Pulse Buzzer for 8 seconds
  Serial.println("[RELAY 2 + BUZZER] Flashing Red/Blue Police Strobe (D33) & Pulsing Buzzer...");
  unsigned long startAlert = millis();

  while (millis() - startAlert < ALERT_DURATION_MS) {
    // PHASE A (250ms): Relay 2 ON (NO = Blue Light ON), Buzzer ON
    digitalWrite(RELAY2_PIN, RELAY_ON);
    digitalWrite(BUZZER_PIN, HIGH);
    digitalWrite(ONBOARD_LED, HIGH);
    delay(STROBE_INTERVAL_MS);

    // PHASE B (250ms): Relay 2 OFF (NC = Red Light ON), Buzzer OFF
    digitalWrite(RELAY2_PIN, RELAY_OFF);
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(ONBOARD_LED, LOW);
    delay(STROBE_INTERVAL_MS);
  }

  // 5. Reset hardware outputs and restore SOLID ON Blue LED (showing Wi-Fi is still active)
  digitalWrite(RELAY1_PIN, RELAY_OFF);
  digitalWrite(RELAY2_PIN, RELAY_OFF);
  digitalWrite(STROBE_POWER_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(ONBOARD_LED, (WiFi.status() == WL_CONNECTED) ? HIGH : LOW);

  vib1Triggered = false;
  vib2Triggered = false;
  pirTriggered  = false;

  // 6. Update Firebase Live Status back to SAFE
  String safeJson = "{\"state\":\"SAFE\",\"activeZone\":\"All Sectors Clear\",\"lighting\":\"" +
                    String(isNight ? "NIGHT" : "DAY") + "\",\"updatedAt\":\"" + getFormattedTime() + "\"}";
  queueFirebaseSend("/live_status.json", "PUT", safeJson);

  Serial.println("[RESET] Alert cycle complete. Returning to perimeter watch...\n");
  delay(2000);
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(VIB1_PIN, INPUT);
  pinMode(VIB2_PIN, INPUT);
  pinMode(PIR_PIN, INPUT);
  pinMode(LDR_PIN, INPUT);

  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RELAY1_PIN, OUTPUT);
  pinMode(RELAY2_PIN, OUTPUT);
  pinMode(STROBE_POWER_PIN, OUTPUT);
  pinMode(ONBOARD_LED, OUTPUT);

  // Ensure all outputs start OFF
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RELAY1_PIN, RELAY_OFF);
  digitalWrite(RELAY2_PIN, RELAY_OFF);
  digitalWrite(STROBE_POWER_PIN, LOW);
  digitalWrite(ONBOARD_LED, LOW);

  Serial.println("\n=======================================================");
  Serial.println("  ELEPHANT ALERT SYSTEM (MANDATORY WI-FI + FIREBASE)   ");
  Serial.println("=======================================================");

  WiFi.mode(WIFI_STA);

  // Initialize FreeRTOS Queue and Background Worker Task for Firebase (Core 0)
  firebaseQueue = xQueueCreate(8, sizeof(FirebaseMessage));
  if (firebaseQueue != NULL) {
    xTaskCreatePinnedToCore(
      firebaseWorkerTask,
      "firebaseTask",
      14336, // 14 KB: Ample headroom for mbedTLS cryptographic operations
      NULL,
      1,
      NULL,
      0 // Core 0: Protocol/Network core
    );
    Serial.println("[SYSTEM] Firebase background task started on Core 0.");
  } else {
    Serial.println("[SYSTEM ERROR] Failed to create Firebase Queue!");
  }

  // Block system startup until Wi-Fi connects and sounds the 3-beep ready tone
  requireWiFiConnection();
}

void loop() {
  // If Wi-Fi drops at any point, halt sensor monitoring and reconnect first
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\n[WIFI LOST] Connection dropped! Halting sensors until Wi-Fi reconnects...");
    playWiFiErrorSignal();
    requireWiFiConnection();
  }

  unsigned long now = millis();

  int vib1State = digitalRead(VIB1_PIN);
  int vib2State = digitalRead(VIB2_PIN);
  int pirState  = digitalRead(PIR_PIN);
  int ldrState  = digitalRead(LDR_PIN);

  // Latch Vibration Sensor 1 (Zone 1)
  if (vib1State == HIGH) {
    if (!vib1Triggered) {
      Serial.println(">>> [EVENT] Vibration Zone 1 (D26) Triggered!");
    }
    lastVib1Time = now;
    vib1Triggered = true;
  }

  // Latch Vibration Sensor 2 (Zone 2)
  if (vib2State == HIGH) {
    if (!vib2Triggered) {
      Serial.println(">>> [EVENT] Vibration Zone 2 (D13) Triggered!");
    }
    lastVib2Time = now;
    vib2Triggered = true;
  }

  // Latch PIR Motion Sensor
  if (pirState == HIGH) {
    if (!pirTriggered) {
      Serial.println(">>> [EVENT] PIR Motion (D27) Triggered!");
    }
    lastPirTime = now;
    pirTriggered = true;
  }

  // Expire triggers older than 5 seconds
  if (vib1Triggered && (now - lastVib1Time > COINCIDENCE_WINDOW_MS)) {
    vib1Triggered = false;
    Serial.println("[WINDOW EXPIRED] Vibration Zone 1 timer reset.");
  }
  if (vib2Triggered && (now - lastVib2Time > COINCIDENCE_WINDOW_MS)) {
    vib2Triggered = false;
    Serial.println("[WINDOW EXPIRED] Vibration Zone 2 timer reset.");
  }
  if (pirTriggered && (now - lastPirTime > COINCIDENCE_WINDOW_MS)) {
    pirTriggered = false;
    Serial.println("[WINDOW EXPIRED] PIR motion timer reset.");
  }

  // Live 1-second status line
  if (now - lastStatusTime >= STATUS_PRINT_MS) {
    lastStatusTime = now;
    Serial.printf("[LIVE] WIFI:OK | VIB1:%d | VIB2:%d | PIR:%d | LDR:%s(%d) | Active: V1=%s V2=%s PIR=%s\n",
                  vib1State,
                  vib2State,
                  pirState,
                  (ldrState == NIGHT_STATE) ? "NIGHT" : "DAY",
                  ldrState,
                  vib1Triggered ? "Y" : "N",
                  vib2Triggered ? "Y" : "N",
                  pirTriggered  ? "Y" : "N");
  }

  // Coincidence check: (Zone 1 OR Zone 2 Vibration) + PIR within 5 seconds
  if ((vib1Triggered || vib2Triggered) && pirTriggered) {
    triggerElephantAlert();
  }

  delay(20);
}
