# FitGains

App Android (MVP) que estima calorías y macros de una comida a partir de una foto, lleva un diario y calcula cuánto debes comer según tus datos y tu objetivo.

## Cómo funciona

1. **Foto → IA de visión** (Gemini o Claude, en el servidor): identifica cada alimento y estima los gramos.
2. **Gramos → nutrientes** con **USDA FoodData Central**. Si USDA no encuentra el alimento, se usa la estimación de la IA (se marca en la app).
3. **Tú corriges** nombre y gramos antes de guardar. La porción es lo que más error tiene.
4. **Metas**: Mifflin-St Jeor × factor de actividad, ajustado por objetivo (−20 % perder grasa, +10 % ganar músculo) y repartido en macros (proteína por kg, grasa 27 %, resto carbohidratos).

Productos envasados: escáner de código de barras → **Open Food Facts** (directo desde la app, sin clave).

## Estructura

```
app/src/main/java/com/angel/fitgains/
  core/goals/       Cálculo de TMB, gasto diario y macros (con tests)
  core/data/        Room: perfil, comidas, peso + FitRepository
  core/ai/          FoodRecognizer: CloudRecognizer (Supabase), LocalRecognizer (fase 2), Hybrid
  core/nutrition/   FoodItem/Nutrients, Open Food Facts
  ui/               Compose: onboarding/perfil, diario, análisis de foto, búsqueda, progreso
supabase/functions/analyze-food/   Edge Function: IA de visión + USDA (las claves viven aquí)
tools/validation/                  Script de la fase 1 para comparar proveedores
```

## Puesta en marcha

### 1. Backend (Supabase)

```bash
npm i -g supabase
supabase login
supabase init            # en la raíz del repo, si no existe supabase/config.toml
supabase link --project-ref TU_PROJECT_REF
supabase secrets set AI_PROVIDER=gemini GEMINI_API_KEY=... USDA_API_KEY=...
# opcional: supabase secrets set ANTHROPIC_API_KEY=... (y AI_PROVIDER=claude)
supabase functions deploy analyze-food
```

- Clave de Gemini: Google AI Studio. Clave de USDA (gratis): https://fdc.nal.usda.gov/api-key-signup
- `GEMINI_MODEL` y `CLAUDE_MODEL` son opcionales (por defecto `gemini-2.5-flash` y `claude-opus-5-5`). Para abaratar Claude puedes usar `claude-haiku-5-5`. Revisa los precios y los nombres de modelo actuales de cada proveedor antes de elegir.

### 2. App

En `local.properties` (no se sube a git):

```
supabase.url=https://TU_PROJECT_REF.supabase.co
supabase.anonKey=TU_ANON_KEY
```

Abre la carpeta en Android Studio, o compila desde la terminal:

```bash
./gradlew :app:assembleDebug
```

Sin Supabase configurado, la app funciona igual para el perfil, las metas, el diario, el peso y el código de barras. La foto y la búsqueda por texto mostrarán un aviso.

### 3. Validar la IA (fase 1)

Pesa 30–50 platos reales, fotografíalos y compara los proveedores:

```bash
pip install -r tools/validation/requirements.txt
python tools/validation/validate.py --providers gemini,claude
```

Ver instrucciones en `tools/validation/validate.py`.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## Pendiente (siguientes fases)

- Modo offline: implementar `LocalRecognizer` con Gemini Nano (ML Kit GenAI) o Gemma 3n (MediaPipe).
- Cuentas y sincronización del historial con Supabase Postgres (ahora todo se guarda en el móvil con Room).
- Límite de análisis por usuario en la Edge Function, alimentos personalizados, Health Connect y recordatorios.
