package com.angel.fitgains

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.angel.fitgains.ui.FitGainsNavHost
import com.angel.fitgains.ui.theme.FitGainsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitGainsTheme {
                FitGainsNavHost()
            }
        }
    }
}
