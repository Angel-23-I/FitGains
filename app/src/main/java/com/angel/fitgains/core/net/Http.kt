package com.angel.fitgains.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpException(val code: Int, message: String) : IOException(message)

/** Cliente HTTP mínimo con HttpURLConnection: suficiente para el MVP y sin dependencias. */
object Http {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): JSONObject =
        request("GET", url, headers, null)

    suspend fun postJson(url: String, body: JSONObject, headers: Map<String, String> = emptyMap()): JSONObject =
        request("POST", url, headers + ("Content-Type" to "application/json"), body.toString())

    private suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", "FitGains-Android/0.1")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpException(code, "HTTP $code: ${text.take(300)}")
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
