package com.angel.fitgains.core.ai

import com.angel.fitgains.core.nutrition.FoodItem

/**
 * Punto de enganche para la fase 2 (modo offline): Gemini Nano vía ML Kit
 * GenAI o Gemma 3n con MediaPipe. Por ahora avisa de que no está disponible
 * para que la UI ofrezca la búsqueda manual.
 */
class LocalRecognizer : FoodRecognizer {
    override suspend fun recognize(jpeg: ByteArray): List<FoodItem> =
        throw RecognizerUnavailableException(
            "Sin conexión. El análisis offline llegará en la fase 2; usa la búsqueda manual."
        )
}
