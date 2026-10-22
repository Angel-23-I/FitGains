package com.angel.fitgains.ui.analysis

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.angel.fitgains.container
import com.angel.fitgains.core.ImageUtils
import com.angel.fitgains.core.data.Meal
import com.angel.fitgains.core.nutrition.FoodItem
import com.angel.fitgains.core.nutrition.Nutrients
import com.angel.fitgains.ui.components.EditableFoodRow
import com.angel.fitgains.ui.components.fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.io.File

data class AnalyzeState(
    val photo: Uri? = null,
    val loading: Boolean = false,
    val items: List<FoodItem> = emptyList(),
    val error: String? = null,
    val analyzed: Boolean = false,
)

class AnalyzeViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    val state = MutableStateFlow(AnalyzeState())

    fun analyze(uri: Uri) {
        state.value = AnalyzeState(photo = uri, loading = true)
        viewModelScope.launch {
            try {
                val jpeg = ImageUtils.toCompressedJpeg(getApplication(), uri)
                val items = c.recognizer.recognize(jpeg)
                state.update {
                    it.copy(
                        loading = false,
                        analyzed = true,
                        items = items,
                        error = if (items.isEmpty()) "No se detectó comida en la foto." else null,
                    )
                }
            } catch (e: Exception) {
                state.update { it.copy(loading = false, analyzed = true, error = e.message ?: "Error al analizar") }
            }
        }
    }

    fun update(index: Int, item: FoodItem) =
        state.update { s -> s.copy(items = s.items.toMutableList().also { it[index] = item }) }

    fun remove(index: Int) =
        state.update { s -> s.copy(items = s.items.toMutableList().also { it.removeAt(index) }) }

    fun addEmpty() = state.update { s -> s.copy(items = s.items + FoodItem("", 100.0, Nutrients())) }

    /** Vuelve a buscar nutrientes en la base de datos con el nombre corregido. */
    fun relookup(index: Int, name: String) = viewModelScope.launch {
        try {
            val match = c.cloud.search(name).firstOrNull() ?: return@launch
            val current = state.value.items[index]
            update(index, current.copy(per100g = match.per100g, source = match.source))
        } catch (e: Exception) {
            state.update { it.copy(error = e.message) }
        }
    }

    suspend fun save(meal: Meal) {
        c.repository.addFoods(meal, state.value.items.filter { it.name.isNotBlank() && it.grams > 0 }, "photo")
    }
}

@Composable
fun AnalyzeScreen(meal: Meal, onDone: () -> Unit, onManualSearch: (Meal) -> Unit) {
    val context = LocalContext.current
    val vm: AnalyzeViewModel = viewModel()
    val state by vm.state.collectAsState()
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var autoLaunched by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingUri?.let(vm::analyze)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::analyze)
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri
        takePicture.launch(uri)
    }

    LaunchedEffect(Unit) {
        if (!autoLaunched && state.photo == null) {
            autoLaunched = true
            launchCamera()
        }
    }
    LaunchedEffect(saving) {
        if (saving) {
            vm.save(meal)
            onDone()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Analizar comida · ${meal.label}", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(::launchCamera) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Cámara")
                }
                OutlinedButton({ pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galería")
                }
            }
        }
        state.photo?.let { uri ->
            item {
                AsyncImage(
                    model = uri,
                    contentDescription = "Foto de la comida",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
        }
        if (state.loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp))
                    Text("Identificando alimentos y porciones…")
                }
            }
        }
        state.error?.let { msg ->
            item {
                Column {
                    Text(msg, color = MaterialTheme.colorScheme.error)
                    TextButton({ onManualSearch(meal) }) { Text("Buscar manualmente") }
                }
            }
        }
        itemsIndexed(state.items) { i, item ->
            EditableFoodRow(
                item = item,
                onChange = { vm.update(i, it) },
                onRemove = { vm.remove(i) },
                onRelookup = { vm.relookup(i, it) },
            )
        }
        if (state.analyzed) {
            item {
                TextButton(vm::addEmpty) {
                    Icon(Icons.Default.Add, null); Text("Añadir alimento que falta")
                }
            }
        }
        if (state.items.isNotEmpty()) {
            item {
                val total = state.items.sumOf { it.totals().kcal }
                Box(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { saving = true },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Guardar en ${meal.label} · ${total.fmt()} kcal") }
                }
                Text(
                    "Revisa los gramos: la porción es lo que más error tiene en una foto.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
