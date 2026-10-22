package com.angel.fitgains.core.nutrition

import com.angel.fitgains.core.net.Http
import com.angel.fitgains.core.net.HttpException

/** Productos envasados por código de barras. API pública, sin clave. */
class OpenFoodFactsClient {
    suspend fun byBarcode(code: String): FoodItem? {
        val url = "https://world.openfoodfacts.org/api/v2/product/$code.json" +
            "?fields=product_name,product_name_es,brands,serving_quantity,nutriments"
        val json = try {
            Http.get(url)
        } catch (e: HttpException) {
            if (e.code == 404) return null else throw e
        }
        if (json.optInt("status") != 1) return null
        val p = json.getJSONObject("product")
        val n = p.optJSONObject("nutriments") ?: return null
        val name = p.optString("product_name_es").ifBlank { p.optString("product_name") }
            .ifBlank { "Producto $code" }
        val brand = p.optString("brands").substringBefore(',').trim()
        return FoodItem(
            name = if (brand.isNotEmpty()) "$name ($brand)" else name,
            grams = p.optDouble("serving_quantity", 100.0).takeIf { it > 0 } ?: 100.0,
            per100g = Nutrients(
                kcal = n.optDouble("energy-kcal_100g", 0.0),
                proteinG = n.optDouble("proteins_100g", 0.0),
                carbsG = n.optDouble("carbohydrates_100g", 0.0),
                fatG = n.optDouble("fat_100g", 0.0),
                fiberG = n.optDouble("fiber_100g", 0.0),
            ),
            source = "openfoodfacts",
        )
    }
}
