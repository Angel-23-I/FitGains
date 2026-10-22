package com.angel.fitgains.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.fitgains.container
import com.angel.fitgains.ui.components.fmt
import kotlinx.coroutines.launch

@Composable
fun ProgressScreen() {
    val repo = LocalContext.current.container.repository
    val scope = rememberCoroutineScope()
    val weights by repo.weights.collectAsState(initial = emptyList())
    val daily by remember { repo.dailyKcal(7) }.collectAsState(initial = emptyList())
    val targets by repo.targets.collectAsState(initial = null)
    var input by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Progreso", style = MaterialTheme.typography.headlineSmall) }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registrar peso de hoy", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { v -> input = v.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text("kg") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        val kg = input.replace(',', '.').toDoubleOrNull()
                        Button(
                            onClick = { kg?.let { scope.launch { repo.logWeight(it); input = "" } } },
                            enabled = kg != null && kg in 30.0..300.0,
                            modifier = Modifier.padding(start = 8.dp),
                        ) { Text("Guardar") }
                    }
                    Text(
                        "Al cambiar el peso se recalculan tus metas diarias.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (weights.size >= 2) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val first = weights.first().weightKg
                        val last = weights.last().weightKg
                        val diff = last - first
                        Text("Peso: ${last.fmt(1)} kg", style = MaterialTheme.typography.titleMedium)
                        Text("${if (diff >= 0) "+" else ""}${diff.fmt(1)} kg desde ${weights.first().date}")
                        LineChart(
                            values = weights.takeLast(30).map { it.weightKg },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp),
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Últimos 7 días", style = MaterialTheme.typography.titleMedium)
                    if (daily.isEmpty()) {
                        Text("Aún no hay comidas registradas.")
                    } else {
                        val avg = daily.sumOf { it.kcal } / daily.size
                        Text("Media: ${avg.fmt()} kcal/día" + (targets?.let { " (meta ${it.calories})" } ?: ""))
                    }
                }
            }
        }

        items(daily.reversed()) { d ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(d.date, modifier = Modifier.weight(1f))
                Text("${d.kcal.fmt()} kcal")
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        fun point(i: Int) = Offset(
            x = i * stepX,
            y = size.height - ((values[i] - min) / range * size.height).toFloat(),
        )
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until values.size) lineTo(point(i).x, point(i).y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        values.indices.forEach { drawCircle(color, 4.dp.toPx(), point(it)) }
    }
}
