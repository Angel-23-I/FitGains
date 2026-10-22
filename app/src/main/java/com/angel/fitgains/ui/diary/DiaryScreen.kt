package com.angel.fitgains.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angel.fitgains.container
import com.angel.fitgains.core.data.DayTotals
import com.angel.fitgains.core.data.FoodEntryEntity
import com.angel.fitgains.core.data.Meal
import com.angel.fitgains.core.goals.DailyTargets
import com.angel.fitgains.ui.components.MacroBar
import com.angel.fitgains.ui.components.fmt
import com.angel.fitgains.ui.theme.CarbsColor
import com.angel.fitgains.ui.theme.FatColor
import com.angel.fitgains.ui.theme.ProteinColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(onPhoto: (Meal) -> Unit, onSearch: (Meal) -> Unit) {
    val repo = LocalContext.current.container.repository
    val scope = rememberCoroutineScope()
    var dateStr by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateStr)
    val isToday = date == LocalDate.now()

    val targets by repo.targets.collectAsState(initial = null)
    val totals by remember(dateStr) { repo.totals(dateStr) }.collectAsState(initial = DayTotals())
    val entries by remember(dateStr) { repo.entries(dateStr) }.collectAsState(initial = emptyList())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ dateStr = date.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior")
                }
                Text(
                    if (isToday) "Hoy" else date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale("es"))),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton({ dateStr = date.plusDays(1).toString() }, enabled = !isToday) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente")
                }
            }
        }
        item { targets?.let { SummaryCard(totals, it) } }
        Meal.entries.forEach { meal ->
            item(key = meal.name) {
                MealCard(
                    meal = meal,
                    entries = entries.filter { it.meal == meal.name },
                    canAdd = isToday,
                    onPhoto = { onPhoto(meal) },
                    onSearch = { onSearch(meal) },
                    onDelete = { id -> scope.launch { repo.deleteEntry(id) } },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(totals: DayTotals, t: DailyTargets) {
    val remaining = t.calories - totals.kcal.roundToInt()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totals.kcal.fmt(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(" / ${t.calories} kcal", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                if (remaining >= 0) "Te quedan $remaining kcal" else "Te has pasado ${-remaining} kcal",
                color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            MacroBar("Proteína", totals.proteinG, t.proteinG, "g", ProteinColor)
            MacroBar("Carbohidratos", totals.carbsG, t.carbsG, "g", CarbsColor)
            MacroBar("Grasa", totals.fatG, t.fatG, "g", FatColor)
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    entries: List<FoodEntryEntity>,
    canAdd: Boolean,
    onPhoto: () -> Unit,
    onSearch: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${entries.sumOf { it.kcal }.fmt()} kcal", style = MaterialTheme.typography.labelLarge)
            }
            entries.forEach { e ->
                HorizontalDivider()
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name)
                        Text(
                            "${e.grams.fmt()} g · P ${e.proteinG.fmt()} · C ${e.carbsG.fmt()} · G ${e.fatG.fmt()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("${e.kcal.fmt()} kcal")
                    IconButton({ onDelete(e.id) }) { Icon(Icons.Default.Close, "Eliminar") }
                }
            }
            if (canAdd) {
                Row {
                    TextButton(onPhoto) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Foto")
                    }
                    TextButton(onSearch) {
                        Icon(Icons.Default.Search, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Buscar / código")
                    }
                }
            }
        }
    }
}
