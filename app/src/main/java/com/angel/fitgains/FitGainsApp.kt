package com.angel.fitgains

import android.app.Application
import android.content.Context
import com.angel.fitgains.core.ai.CloudRecognizer
import com.angel.fitgains.core.ai.FoodRecognizer
import com.angel.fitgains.core.ai.HybridRecognizer
import com.angel.fitgains.core.ai.LocalRecognizer
import com.angel.fitgains.core.data.AppDatabase
import com.angel.fitgains.core.data.FitRepository
import com.angel.fitgains.core.nutrition.OpenFoodFactsClient

/** Inyección de dependencias manual: suficiente para el MVP. */
class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    val repository = FitRepository(db)
    val cloud = CloudRecognizer(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
    val recognizer: FoodRecognizer = HybridRecognizer(cloud, LocalRecognizer())
    val openFoodFacts = OpenFoodFactsClient()
}

class FitGainsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as FitGainsApp).container
