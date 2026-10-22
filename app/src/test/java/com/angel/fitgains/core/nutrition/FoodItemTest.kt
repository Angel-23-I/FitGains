package com.angel.fitgains.core.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodItemTest {
    @Test
    fun totalsScaleWithGrams() {
        // Arroz blanco cocido aprox.
        val rice = FoodItem("Arroz", 150.0, Nutrients(130.0, 2.7, 28.0, 0.3, 0.4))
        val t = rice.totals()
        assertEquals(195.0, t.kcal, 0.01)
        assertEquals(42.0, t.carbsG, 0.01)
        assertEquals(4.05, t.proteinG, 0.01)
    }
}
