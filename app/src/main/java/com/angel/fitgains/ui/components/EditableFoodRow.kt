package com.angel.fitgains.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.fitgains.core.nutrition.FoodItem

/**
 * Tarjeta de un alimento detectado: el usuario puede corregir nombre y gramos.
 * [onRelookup] vuelve a buscar los nutrientes con el nombre corregido.
 */
@Composable
fun EditableFoodRow(
    item: FoodItem,
    onChange: (FoodItem) -> Unit,
    onRemove: () -> Unit,
    onRelookup: ((String) -> Unit)? = null,
) {
    // Clave por nombre: si fuera por gramos, cada tecla reformatearía el texto (p. ej. "15." -> "15").
    var gramsText by remember(item.name) { mutableStateOf(item.grams.fmt()) }
    val t = item.totals()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onChange(item.copy(name = it)) },
                    label = { Text("Alimento") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (onRelookup != null) {
                    IconButton({ onRelookup(item.name) }) {
                        Icon(Icons.Default.Refresh, "Buscar nutrientes de nuevo")
                    }
                }
                IconButton(onRemove) { Icon(Icons.Default.Delete, "Quitar") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { v ->
                        gramsText = v.filter { it.isDigit() || it == '.' }
                        gramsText.toDoubleOrNull()?.let { onChange(item.copy(grams = it)) }
                    },
                    label = { Text("Gramos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp),
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text("${t.kcal.fmt()} kcal", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "P ${t.proteinG.fmt(1)} · C ${t.carbsG.fmt(1)} · G ${t.fatG.fmt(1)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            val note = buildList {
                item.confidence?.let { add("Confianza IA ${(it * 100).toInt()} %") }
                when (item.source) {
                    "usda" -> add("Datos: USDA")
                    "openfoodfacts" -> add("Datos: Open Food Facts")
                    "ai_estimate" -> add("Valores estimados por la IA")
                }
            }.joinToString(" · ")
            if (note.isNotEmpty()) Text(note, style = MaterialTheme.typography.labelSmall)
        }
    }
}
