package com.angel.fitgains.core.ai

import com.angel.fitgains.core.nutrition.FoodItem

/**
 * Detecta alimentos y porciones en una foto. Las implementaciones devuelven
 * cada alimento ya con sus nutrientes por 100 g para que la UI solo tenga que
 * multiplicar por los gramos que el usuario confirme.
 */
interface FoodRecognizer {
    suspend fun recognize(jpeg: ByteArray): List<FoodItem>
}

class RecognizerUnavailableException(message: String) : Exception(message)

/** Usa la nube y, si no hay red, intenta el reconocedor local. */
class HybridRecognizer(
    private val cloud: FoodRecognizer,
    private val local: FoodRecognizer,
) : FoodRecognizer {
    override suspend fun recognize(jpeg: ByteArray): List<FoodItem> =
        try {
            cloud.recognize(jpeg)
        } catch (e: java.io.IOException) {
            if (e is com.angel.fitgains.core.net.HttpException) throw e
            local.recognize(jpeg)
        }
}
