package nibm.iot.elephantpass.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
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
                        val element = JsonParser.parseString(json)
                        if (!element.isJsonObject) {
                            Result.success(emptyList())
                        } else {
                            val rootObj = element.asJsonObject
                            val items = rootObj.entrySet().mapNotNull { (key, jsonElement) ->
                                try {
                                    if (!jsonElement.isJsonObject) return@mapNotNull null
                                    val obj = jsonElement.asJsonObject

                                    val zone = if (obj.has("zone") && !obj.get("zone").isJsonNull) {
                                        obj.get("zone").asString
                                    } else "Unknown Sector"

                                    val severity = if (obj.has("severity") && !obj.get("severity").isJsonNull) {
                                        obj.get("severity").asString
                                    } else "HIGH"

                                    val lighting = if (obj.has("lighting") && !obj.get("lighting").isJsonNull) {
                                        obj.get("lighting").asString
                                    } else "DAY"

                                    val timestamp = if (obj.has("timestamp") && !obj.get("timestamp").isJsonNull) {
                                        obj.get("timestamp").asString
                                    } else "Just now"

                                    val lat = if (obj.has("lat") && !obj.get("lat").isJsonNull) {
                                        obj.get("lat").asDouble
                                    } else 7.9582

                                    val lng = if (obj.has("lng") && !obj.get("lng").isJsonNull) {
                                        obj.get("lng").asDouble
                                    } else 80.7621

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

    fun pollLiveStatus(intervalMillis: Long = 3000L): Flow<Result<LiveStatus>> = flow {
        while (true) {
            try {
                emit(fetchLiveStatus())
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
            delay(intervalMillis)
        }
    }

    fun pollAlerts(intervalMillis: Long = 3000L): Flow<Result<List<AlertItem>>> = flow {
        while (true) {
            try {
                emit(fetchAlerts())
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
            delay(intervalMillis)
        }
    }
}
