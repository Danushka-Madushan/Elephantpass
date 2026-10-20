package nibm.iot.elephantpass.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.data.models.LiveStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ElephantRepository(
    private val baseUrl: String = "https://device-streaming-ebe74626-default-rtdb.asia-southeast1.firebasedatabase.app"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchLiveStatus(): Result<LiveStatus> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/live_status.json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(IOException("HTTP Error ${response.code}"))
                } else {
                    val json = response.body?.string()
                    if (json.isNullOrBlank() || json == "null") {
                        Result.success(LiveStatus())
                    } else {
                        val status = gson.fromJson(json, LiveStatus::class.java)
                        Result.success(status ?: LiveStatus())
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAlerts(): Result<List<AlertItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/alerts.json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(IOException("HTTP Error ${response.code}"))
                } else {
                    val json = response.body?.string()
                    if (json.isNullOrBlank() || json == "null") {
                        Result.success(emptyList())
                    } else {
                        val mapType = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
                        val map: Map<String, Map<String, Any>>? = gson.fromJson(json, mapType)

                        if (map == null) {
                            Result.success(emptyList())
                        } else {
                            val items = map.entries.mapNotNull { (key, valMap) ->
                                try {
                                    val zone = valMap["zone"]?.toString() ?: "Unknown Sector"
                                    val severity = valMap["severity"]?.toString() ?: "HIGH"
                                    val lighting = valMap["lighting"]?.toString() ?: "DAY"
                                    val timestamp = valMap["timestamp"]?.toString() ?: "Just now"
                                    val lat = (valMap["lat"] as? Number)?.toDouble() ?: 7.9582
                                    val lng = (valMap["lng"] as? Number)?.toDouble() ?: 80.7621

                                    AlertItem(
                                        key = key,
                                        zone = zone,
                                        severity = severity,
                                        lighting = lighting,
                                        timestamp = timestamp,
                                        lat = lat,
                                        lng = lng
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }.reversed() // Most recent first

                            Result.success(items)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pollLiveStatus(intervalMillis: Long = 5000L): Flow<Result<LiveStatus>> = flow {
        while (true) {
            emit(fetchLiveStatus())
            delay(intervalMillis)
        }
    }

    fun pollAlerts(intervalMillis: Long = 5000L): Flow<Result<List<AlertItem>>> = flow {
        while (true) {
            emit(fetchAlerts())
            delay(intervalMillis)
        }
    }
}
