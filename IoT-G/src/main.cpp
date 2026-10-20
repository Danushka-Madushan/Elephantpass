#include <Arduino.h>

// --- PIN DEFINITIONS (Unchanged Wiring) ---
#define VIB1_PIN         26   // Vibration Sensor 1 (Zone 1)
#define VIB2_PIN         13   // Vibration Sensor 2 (Zone 2)
#define PIR_PIN          27   // PIR Motion Sensor
#define LDR_PIN          25   // 3-Pin LDR Module
#define BUZZER_PIN       14   // Button Buzzer
#define RELAY1_PIN       32   // Relay 1: Night Floodlight (Steady ON at night)
#define RELAY2_PIN       33   // Relay 2: Red/Blue Police Strobe (Toggles NO/NC)
#define STROBE_POWER_PIN 23   // Optional: Powers Relay 2 COM for small LEDs only during alert
#define ONBOARD_LED      2    // Onboard Blue Status LED

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

void triggerElephantAlert() {
  Serial.println("\n=======================================================");
  Serial.println("!!! ELEPHANT ALERT: ACTIVATING POLICE STROBE & ALARM !!!");

  if (vib1Triggered && vib2Triggered) {
    Serial.println("[ZONE INFO] CRITICAL: Both Zone 1 (D26) & Zone 2 (D13) shaking!");
  } else if (vib1Triggered) {
    Serial.println("[ZONE INFO] Tremor detected in Zone 1 (D26).");
  } else if (vib2Triggered) {
    Serial.println("[ZONE INFO] Tremor detected in Zone 2 (D13).");
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

  // Enable optional GPIO 23 power feed in case small LEDs are wired to Relay 2 COM
  digitalWrite(STROBE_POWER_PIN, HIGH);

  // 2. Alternate Relay 2 (Red NC <-> Blue NO) + Pulse Buzzer for 8 seconds
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

  // 3. Reset all outputs to idle state
  digitalWrite(RELAY1_PIN, RELAY_OFF);
  digitalWrite(RELAY2_PIN, RELAY_OFF);
  digitalWrite(STROBE_POWER_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(ONBOARD_LED, LOW);

  vib1Triggered = false;
  vib2Triggered = false;
  pirTriggered  = false;

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
  Serial.println("  ELEPHANT ALERT SYSTEM (POLICE STROBE + DUAL ZONE)    ");
  Serial.println("=======================================================");
}

void loop() {
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
    Serial.printf("[LIVE] VIB1:%d | VIB2:%d | PIR:%d | LDR:%s(%d) | Active: V1=%s V2=%s PIR=%s\n",
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
