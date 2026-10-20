package nibm.iot.elephantpass

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.data.models.LiveStatus
import org.junit.Assert.*
import org.junit.Test

class ModelDeserializationTest {

    private val gson = Gson()

    @Test
    fun testLiveStatusAlertDeserialization() {
        val sampleJson = """
            {
                "state": "ALERT",
                "activeZone": "Sector A & B - Main Village Perimeter",
                "lighting": "NIGHT (Floodlight ON)",
                "lat": 7.9582,
                "lng": 80.7621,
                "updatedAt": "2026-10-21 05:14:00"
            }
        """.trimIndent()

        val status = gson.fromJson(sampleJson, LiveStatus::class.java)
        assertNotNull(status)
        assertTrue(status.isAlert)
        assertEquals("ALERT", status.state)
        assertEquals("Sector A & B - Main Village Perimeter", status.activeZone)
        assertEquals("NIGHT (Floodlight ON)", status.lighting)
        assertEquals(7.9582, status.lat ?: 0.0, 0.0001)
        assertEquals(80.7621, status.lng ?: 0.0, 0.0001)
    }

    @Test
    fun testLiveStatusSafeDeserialization() {
        val sampleJson = """
            {
                "state": "SAFE",
                "activeZone": "All Sectors Clear",
                "lighting": "ONLINE",
                "updatedAt": "2026-10-21 05:00:00"
            }
        """.trimIndent()

        val status = gson.fromJson(sampleJson, LiveStatus::class.java)
        assertNotNull(status)
        assertFalse(status.isAlert)
        assertEquals("SAFE", status.state)
    }

    @Test
    fun testAlertsMapDeserialization() {
        val sampleAlertsJson = """
            {
                "-OABC123": {
                    "zone": "Sector A - North Forest Border (Zone 1)",
                    "severity": "CRITICAL (Dual-Zone Breach)",
                    "lighting": "NIGHT (Floodlight ON)",
                    "lat": 7.9570,
                    "lng": 80.7603,
                    "timestamp": "2026-10-21 04:30:15"
                }
            }
        """.trimIndent()

        val mapType = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
        val map: Map<String, Map<String, Any>>? = gson.fromJson(sampleAlertsJson, mapType)

        assertNotNull(map)
        val entry = map!!.entries.first()
        assertEquals("-OABC123", entry.key)

        val item = AlertItem(
            key = entry.key,
            zone = entry.value["zone"].toString(),
            severity = entry.value["severity"].toString(),
            lighting = entry.value["lighting"].toString(),
            timestamp = entry.value["timestamp"].toString(),
            lat = (entry.value["lat"] as Number).toDouble(),
            lng = (entry.value["lng"] as Number).toDouble()
        )

        assertTrue(item.isCritical)
        assertTrue(item.isNight)
        assertEquals(7.9570, item.lat ?: 0.0, 0.0001)
        assertEquals(80.7603, item.lng ?: 0.0, 0.0001)
    }
}
