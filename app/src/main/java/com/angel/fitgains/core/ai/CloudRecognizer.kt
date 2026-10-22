package com.angel.fitgains.core.ai

import android.util.Base64
import com.angel.fitgains.core.net.Http
import com.angel.fitgains.core.nutrition.FoodItem
import com.angel.fitgains.core.nutrition.parseFoodItems
import org.json.JSONObject

/**
 * Llama a la Edge Function `analyze-food` de Supabase. La clave del proveedor
 * de IA vive en el servidor; la app solo conoce la anon key pública.
 */
class CloudRecognizer(
    private val supabaseUrl: String,
    private val anonKey: String,
) : FoodRecognizer {

    override suspend fun recognize(jpeg: ByteArray): List<FoodItem> {
        if (supabaseUrl.isBlank()) {
            throw RecognizerUnavailableException(
                "Configura supabase.url y supabase.anonKey en local.properties"
            )
        }
        val body = JSONObject()
            .put("action", "analyze")
            .put("mime_type", "image/jpeg")
            .put("image_base64", Base64.encodeToString(jpeg, Base64.NO_WRAP))
        val res = Http.postJson("$supabaseUrl/functions/v1/analyze-food", body, authHeaders())
        return parseFoodItems(res)
    }

    /** Búsqueda manual por texto (USDA, vía el mismo backend). */
    suspend fun search(query: String): List<FoodItem> {
        if (supabaseUrl.isBlank()) return emptyList()
        val body = JSONObject().put("action", "search").put("query", query)
        return parseFoodItems(Http.postJson("$supabaseUrl/functions/v1/analyze-food", body, authHeaders()))
    }

    private fun authHeaders() = mapOf(
        "Authorization" to "Bearer $anonKey",
        "apikey" to anonKey,
    )
}
