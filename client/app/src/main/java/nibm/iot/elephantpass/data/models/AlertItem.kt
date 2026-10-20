package nibm.iot.elephantpass.data.models

data class AlertItem(
    val key: String = "",
    val zone: String = "Unknown Sector",
    val severity: String = "HIGH",
    val lighting: String = "DAY",
    val timestamp: String = "Just now",
    val lat: Double? = 7.9582,
    val lng: Double? = 80.7621
) {
    val isCritical: Boolean
        get() = severity.uppercase().contains("CRITICAL")

    val isNight: Boolean
        get() = lighting.uppercase().contains("NIGHT")
}
