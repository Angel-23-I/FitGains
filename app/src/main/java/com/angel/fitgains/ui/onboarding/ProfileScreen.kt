package com.angel.fitgains.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.fitgains.container
import com.angel.fitgains.core.goals.ActivityLevel
import com.angel.fitgains.core.goals.BodyData
import com.angel.fitgains.core.goals.Goal
import com.angel.fitgains.core.goals.GoalsCalculator
import com.angel.fitgains.core.goals.Sex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(isOnboarding: Boolean, onSaved: () -> Unit) {
    val repo = LocalContext.current.container.repository
    val scope = rememberCoroutineScope()

    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.MODERATE) }
    var goal by rememberSaveable { mutableStateOf(Goal.MAINTAIN) }
    var loaded by remember { mutableStateOf(isOnboarding) }

    LaunchedEffect(Unit) {
        if (!isOnboarding) {
            repo.profile.first()?.toBodyData()?.let { p ->
                sex = p.sex
                age = p.ageYears.toString()
                height = p.heightCm.toString()
                weight = p.weightKg.toString()
                activity = p.activity
                goal = p.goal
            }
            loaded = true
        }
    }

    val body = run {
        val a = age.toIntOrNull()
        val h = height.replace(',', '.').toDoubleOrNull()
        val w = weight.replace(',', '.').toDoubleOrNull()
        if (a != null && h != null && w != null && a in 12..100 && h in 100.0..250.0 && w in 30.0..300.0) {
            BodyData(sex, a, h, w, activity, goal)
        } else null
    }

    if (!loaded) return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (isOnboarding) "Bienvenido a FitGains" else "Tu perfil",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (isOnboarding) {
            Text("Con estos datos calculamos cuánto necesitas comer cada día.")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(sex == Sex.MALE, { sex = Sex.MALE }, label = { Text("Hombre") })
            FilterChip(sex == Sex.FEMALE, { sex = Sex.FEMALE }, label = { Text("Mujer") })
        }
        NumberField("Edad (años)", age) { age = it }
        NumberField("Altura (cm)", height) { height = it }
        NumberField("Peso (kg)", weight) { weight = it }

        Text("Nivel de actividad", style = MaterialTheme.typography.titleMedium)
        ActivityLevel.entries.forEach { level ->
            RadioRow(level.label, activity == level) { activity = level }
        }

        Text("Objetivo", style = MaterialTheme.typography.titleMedium)
        Goal.entries.forEach { g ->
            RadioRow(g.label, goal == g) { goal = g }
        }

        body?.let { d ->
            val t = GoalsCalculator.targets(d)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tu meta diaria", style = MaterialTheme.typography.titleMedium)
                    Text("${t.calories} kcal", style = MaterialTheme.typography.headlineMedium)
                    Text("Proteína ${t.proteinG} g · Carbohidratos ${t.carbsG} g · Grasa ${t.fatG} g")
                    Text(
                        "Metabolismo basal ${t.bmr} kcal · Gasto diario ${t.tdee} kcal",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Button(
            onClick = { body?.let { scope.launch { repo.saveProfile(it); onSaved() } } },
            enabled = body != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (isOnboarding) "Empezar" else "Guardar") }

        Text(
            "Estimaciones orientativas; no sustituyen el consejo de un médico o nutricionista.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick)
        Text(label)
    }
}
