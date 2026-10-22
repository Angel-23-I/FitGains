// Edge Function de Supabase: recibe una foto, pide a un modelo de visión que
// identifique alimentos y gramos, y completa los nutrientes con USDA.
//
// Secrets (supabase secrets set ...):
//   AI_PROVIDER      "gemini" (por defecto) | "claude"
//   GEMINI_API_KEY   clave de Google AI Studio
//   GEMINI_MODEL     opcional, por defecto "gemini-2.5-flash"
//   ANTHROPIC_API_KEY clave de Anthropic (si AI_PROVIDER=claude)
//   CLAUDE_MODEL     opcional, por defecto "claude-opus-5-5"
//   USDA_API_KEY     clave gratuita de https://fdc.nal.usda.gov/api-key-signup
import Anthropic from "npm:@anthropic-ai/sdk";
import { zodOutputFormat } from "npm:@anthropic-ai/sdk/helpers/zod";
import { z } from "npm:zod";

const DetectionSchema = z.object({
  items: z.array(
    z.object({
      name_es: z.string(),
      name_en: z.string(),
      grams: z.number(),
      confidence: z.number(),
      kcal_per_100g: z.number(),
      protein_per_100g: z.number(),
      carbs_per_100g: z.number(),
      fat_per_100g: z.number(),
      fiber_per_100g: z.number(),
    }),
  ),
});
type Detection = z.infer<typeof DetectionSchema>;

const PROMPT = `Eres un nutricionista que analiza fotos de comida.
Identifica cada alimento visible por separado (por ejemplo "arroz blanco cocido", "pechuga de pollo a la plancha", "aceite de oliva" si se ve aceite).
Para cada uno estima los gramos de la porción usando como referencia el tamaño del plato, los cubiertos o las manos.
Da el nombre en español (name_es) y en inglés genérico para buscar en USDA (name_en, p. ej. "rice, white, cooked").
Incluye tu mejor estimación de nutrientes por 100 g; se usará solo si no hay datos en USDA.
confidence va de 0 a 1. Si no hay comida en la imagen, devuelve items vacío.`;

type Per100 = { kcal: number; protein: number; carbs: number; fat: number; fiber: number };
type OutItem = { name: string; grams: number; confidence?: number; source: string; per100g: Per100 };

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const body = await req.json();
    let items: OutItem[];
    if (body.action === "search") {
      items = await usdaSearch(String(body.query ?? ""), 10);
    } else if (body.action === "analyze") {
      if (!body.image_base64) return json({ error: "image_base64 requerido" }, 400);
      items = await analyze(body.image_base64, body.mime_type ?? "image/jpeg");
    } else {
      return json({ error: "action debe ser 'analyze' o 'search'" }, 400);
    }
    return json({ items });
  } catch (e) {
    console.error(e);
    return json({ error: String(e instanceof Error ? e.message : e) }, 500);
  }
});

async function analyze(imageB64: string, mime: string): Promise<OutItem[]> {
  const provider = Deno.env.get("AI_PROVIDER") ?? "gemini";
  const detection = provider === "claude"
    ? await detectWithClaude(imageB64, mime)
    : await detectWithGemini(imageB64, mime);

  // Nutrientes: USDA si encuentra el alimento; si no, la estimación del modelo.
  return await Promise.all(detection.items.map(async (d) => {
    const match = (await usdaSearch(d.name_en, 1).catch(() => []))[0];
    return {
      name: d.name_es,
      grams: Math.round(d.grams),
      confidence: d.confidence,
      source: match ? "usda" : "ai_estimate",
      per100g: match?.per100g ?? {
        kcal: d.kcal_per_100g,
        protein: d.protein_per_100g,
        carbs: d.carbs_per_100g,
        fat: d.fat_per_100g,
        fiber: d.fiber_per_100g,
      },
    };
  }));
}

async function detectWithClaude(imageB64: string, mime: string): Promise<Detection> {
  const client = new Anthropic(); // lee ANTHROPIC_API_KEY
  const response = await client.messages.parse({
    model: Deno.env.get("CLAUDE_MODEL") ?? "claude-opus-5-5",
    max_tokens: 16000,
    output_config: { effort: "low", format: zodOutputFormat(DetectionSchema) },
    messages: [{
      role: "user",
      content: [
        {
          type: "image",
          source: { type: "base64", media_type: mime as "image/jpeg", data: imageB64 },
        },
        { type: "text", text: PROMPT },
      ],
    }],
  });
  if (response.stop_reason === "refusal" || !response.parsed_output) {
    throw new Error(`Claude no devolvió un resultado válido (${response.stop_reason})`);
  }
  return response.parsed_output;
}

async function detectWithGemini(imageB64: string, mime: string): Promise<Detection> {
  const key = Deno.env.get("GEMINI_API_KEY");
  if (!key) throw new Error("Falta GEMINI_API_KEY");
  const model = Deno.env.get("GEMINI_MODEL") ?? "gemini-2.5-flash";
  const num = { type: "NUMBER" };
  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": key },
      body: JSON.stringify({
        contents: [{
          parts: [{ inline_data: { mime_type: mime, data: imageB64 } }, { text: PROMPT }],
        }],
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              items: {
                type: "ARRAY",
                items: {
                  type: "OBJECT",
                  properties: {
                    name_es: { type: "STRING" },
                    name_en: { type: "STRING" },
                    grams: num,
                    confidence: num,
                    kcal_per_100g: num,
                    protein_per_100g: num,
                    carbs_per_100g: num,
                    fat_per_100g: num,
                    fiber_per_100g: num,
                  },
                  required: [
                    "name_es", "name_en", "grams", "confidence", "kcal_per_100g",
                    "protein_per_100g", "carbs_per_100g", "fat_per_100g", "fiber_per_100g",
                  ],
                },
              },
            },
            required: ["items"],
          },
        },
      }),
    },
  );
  if (!res.ok) throw new Error(`Gemini HTTP ${res.status}: ${(await res.text()).slice(0, 300)}`);
  const data = await res.json();
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new Error("Gemini no devolvió contenido");
  return DetectionSchema.parse(JSON.parse(text));
}

// --- USDA FoodData Central -------------------------------------------------

const NUTRIENT_IDS = { kcal: 1008, protein: 1003, carbs: 1005, fat: 1004, fiber: 1079 };

async function usdaSearch(query: string, pageSize: number): Promise<OutItem[]> {
  const key = Deno.env.get("USDA_API_KEY") ?? "DEMO_KEY";
  if (!query.trim()) return [];
  const url = new URL("https://api.nal.usda.gov/fdc/v1/foods/search");
  url.searchParams.set("api_key", key);
  url.searchParams.set("query", query);
  url.searchParams.set("pageSize", String(pageSize));
  // Foundation y SR Legacy son alimentos genéricos con valores por 100 g.
  url.searchParams.set("dataType", "Foundation,SR Legacy");
  const res = await fetch(url);
  if (!res.ok) throw new Error(`USDA HTTP ${res.status}`);
  const data = await res.json();
  return (data.foods ?? []).map((f: UsdaFood) => ({
    name: f.description,
    grams: 100,
    source: "usda",
    per100g: {
      kcal: nutrient(f, NUTRIENT_IDS.kcal),
      protein: nutrient(f, NUTRIENT_IDS.protein),
      carbs: nutrient(f, NUTRIENT_IDS.carbs),
      fat: nutrient(f, NUTRIENT_IDS.fat),
      fiber: nutrient(f, NUTRIENT_IDS.fiber),
    },
  })).filter((i: OutItem) => i.per100g.kcal > 0 || i.per100g.protein > 0);
}

type UsdaFood = { description: string; foodNutrients: { nutrientId: number; value: number }[] };

function nutrient(f: UsdaFood, id: number): number {
  return f.foodNutrients?.find((n) => n.nutrientId === id)?.value ?? 0;
}

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
