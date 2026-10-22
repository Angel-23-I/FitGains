package com.angel.fitgains.core.nutrition

import org.json.JSONObject

/**
 * Formato de respuesta del backend:
 * { "items": [ { "name", "grams", "confidence", "source",
 *                "per100g": { "kcal", "protein", "carbs", "fat", "fiber" } } ] }
 */
fun parseFoodItems(json: JSONObject): List<FoodItem> {
    val arr = json.optJSONArray("items") ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val n = o.optJSONObject("per100g") ?: JSONObject()
        FoodItem(
            name = o.optString("name"),
            grams = o.optDouble("grams", 100.0),
            per100g = Nutrients(
                kcal = n.optDouble("kcal", 0.0),
                proteinG = n.optDouble("protein", 0.0),
                carbsG = n.optDouble("carbs", 0.0),
                fatG = n.optDouble("fat", 0.0),
                fiberG = n.optDouble("fiber", 0.0),
            ),
            confidence = if (o.has("confidence")) o.optDouble("confidence") else null,
            source = o.optString("source"),
        )
    }
}
