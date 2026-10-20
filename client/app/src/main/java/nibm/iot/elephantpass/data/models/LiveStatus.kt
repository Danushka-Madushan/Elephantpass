package nibm.iot.elephantpass.data.models

data class LiveStatus(
    val state: String = "SAFE",
    val activeZone: String = "All Sectors Clear",
    val lighting: String = "ONLINE",
    val updatedAt: String = "Just now",
    val lat: Double? = 7.9582,
    val lng: Double? = 80.7621
) {
    val isAlert: Boolean
        get() = state.equals("ALERT", ignoreCase = true)
}
