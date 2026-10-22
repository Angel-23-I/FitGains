package com.angel.fitgains.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Barra "consumido / meta" para un macro o las calorías. */
@Composable
fun MacroBar(label: String, value: Double, target: Int, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()} / $target $unit", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { if (target > 0) (value / target).toFloat().coerceIn(0f, 1f) else 0f },
            color = if (value > target * 1.05) MaterialTheme.colorScheme.error else color,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

fun Double.fmt(decimals: Int = 0): String =
    if (decimals == 0) roundToInt().toString() else "%.${decimals}f".format(this)
