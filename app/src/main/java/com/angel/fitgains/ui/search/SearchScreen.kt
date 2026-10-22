package com.angel.fitgains.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.angel.fitgains.container
import com.angel.fitgains.core.data.Meal
import com.angel.fitgains.core.nutrition.FoodItem
import com.angel.fitgains.ui.components.EditableFoodRow
import com.angel.fitgains.ui.components.fmt
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(meal: Meal, onDone: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var selectedSource by remember { mutableStateOf("search") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun runSearch() {
        if (query.isBlank()) return
        loading = true; message = null
        scope.launch {
            try {
                results = c.cloud.search(query.trim())
                if (results.isEmpty()) message = "Sin resultados (¿configuraste Supabase?)."
            } catch (e: Exception) {
                message = e.message
            } finally {
                loading = false
            }
        }
    }

    fun scanBarcode() {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode ->
                val code = barcode.rawValue ?: return@addOnSuccessListener
                loading = true; message = null
                scope.launch {
                    try {
                        val item = c.openFoodFacts.byBarcode(code)
                        if (item == null) message = "Producto $code no encontrado en Open Food Facts."
                        else { selected = item; selectedSource = "barcode" }
                    } catch (e: Exception) {
                        message = e.message
                    } finally {
                        loading = false
                    }
                }
            }
            .addOnFailureListener { message = it.message }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Añadir a ${meal.label}", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar alimento (ej. pechuga de pollo)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(::runSearch) { Icon(Icons.Default.Search, "Buscar") }
                IconButton(::scanBarcode) { Icon(Icons.Default.QrCodeScanner, "Escanear código de barras") }
            }
        }
        if (loading) item { CircularProgressIndicator() }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        selected?.let { item ->
            item {
                EditableFoodRow(
                    item = item,
                    onChange = { selected = it },
                    onRemove = { selected = null },
                )
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            c.repository.addFoods(meal, listOf(item), selectedSource)
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Añadir · ${item.totals().kcal.fmt()} kcal") }
            }
        }

        if (selected == null) {
            items(results) { r ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        selected = r.copy(grams = 100.0); selectedSource = "search"
                    },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.name)
                        Text(
                            "Por 100 g: ${r.per100g.kcal.fmt()} kcal · P ${r.per100g.proteinG.fmt(1)} · " +
                                "C ${r.per100g.carbsG.fmt(1)} · G ${r.per100g.fatG.fmt(1)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
