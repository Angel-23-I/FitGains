package com.angel.fitgains.core.nutrition

/** Valores nutricionales por 100 g. */
data class Nutrients(
    val kcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
) {
    fun scaled(grams: Double): Nutrients {
        val f = grams / 100.0
        return Nutrients(kcal * f, proteinG * f, carbsG * f, fatG * f, fiberG * f)
    }
}

/**
 * Un alimento con su porción. [confidence] viene de la IA (0..1); null si lo
 * eligió el usuario. [source] indica de dónde salieron los nutrientes
 * ("usda", "openfoodfacts", "ai_estimate").
 */
data class FoodItem(
    val name: String,
    val grams: Double,
    val per100g: Nutrients,
    val confidence: Double? = null,
    val source: String = "",
) {
    fun totals(): Nutrients = per100g.scaled(grams)
}
