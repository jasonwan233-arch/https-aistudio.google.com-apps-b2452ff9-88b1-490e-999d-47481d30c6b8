package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiSoundReimaginer {
    private const val TAG = "GeminiSoundReimaginer"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    suspend fun suggestDSPAndReimagine(
        prompt: String,
        soundTitle: String,
        soundDurationSec: Float
    ): ReimagineResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or using default placeholder.")
            return@withContext getFallbackSuggestion(prompt, soundTitle)
        }

        val systemPrompt = """
            You are a world-class DSP (Digital Signal Processing) and audio restoration engineer, expert in sound carving, creative sound manipulation, and pro audio dialogue editing.
            Analyze the user's description of their recorded mic sound and generate:
            1. Recommended DSP restoration settings (Gain, EQ High-pass/Low-cut, Noise Gate, Compressor).
            2. A suggested creative processing chain (e.g. Delay -> Flanger -> Reverb) to transform the audio.
            3. Pro sound layering options to blend with the sound.
            4. Details for an entirely new generated complementary sound layer that complements or transforms the original audio.
            5. A suggested EQ preset choice out of: "Flat", "Vocal Clarity", "Bass Boost", "Hiss Cut", "Mid Punch", "Brass Bright".
            6. A creative "reimagined" sound layout or artistic synthesis description.
            7. Pro sound collaborative tips, designed specifically for English jargon-savvy engineers.
            You must reply ONLY in a valid JSON format.
            Example schema keys and expected types/values:
            {
               "suggestedGainDb": 3.5, // Float between -12.0 and +12.0.
               "suggestedLowCutHz": 80, // Int from 0 to 200.
               "noiseGateThresholdDb": -45.0, // Float from -90.0 to -20.0
               "compressorRatio": 2.5, // Float from 1.0 to 8.0
               "compressorThresholdDb": -18.0, // Float from -40.0 to 0.0
               "reimaginedTitle": "Accra Dust Transient Loop", // Punchy title
               "explanation": "High-pass filtration at 80Hz targets rumble from Osu traffic. Low-ratio compression tames mic peaks.", 
               "collaborationIdea": "Upload this clean transient loop to the Accra Drill community feed, licensed royalty-free.",
               "creativeChain": "Enhancer -> Hum Notch -> Echo Pitch Shift -> Highlife Chorus", // String creative chain
               "soundLayering": "Blend with a sub-bass sine wave at 60Hz and crisp high-hat ticks to lock the highlife groove.", // String layer advice
               "newComplementTitle": "Osu Street Echo Companion", // Companion sound name
               "newComplementDesc": "A custom synthesized highlife percussion loop with off-beat rim shots and ambient delays.", // Companion sound explanation
               "suggestedEqPreset": "Vocal Clarity" // String: Flat, Vocal Clarity, Bass Boost, Hiss Cut, Mid Punch, or Brass Bright
            }
        """.trimIndent()

        val fullUserPrompt = "The user recorded a sound of length $soundDurationSec seconds, named '$soundTitle'. Description or creative prompt from user: '$prompt'. Please output your response matching the requested JSON schema with all 13 keys."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", fullUserPrompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url("$API_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Empty body"
                    Log.e(TAG, "Gemini call failed: code=${response.code}, body=$errorBody")
                    return@withContext getFallbackSuggestion(prompt, soundTitle)
                }
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    return@withContext getFallbackSuggestion(prompt, soundTitle)
                }

                val mainJsonObj = JSONObject(responseBody)
                val candidates = mainJsonObj.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                val soundJson = JSONObject(cleanJsonResponse(rawText))
                
                ReimagineResult(
                    suggestedGainDb = soundJson.optDouble("suggestedGainDb", 0.0).toFloat(),
                    suggestedLowCutHz = soundJson.optInt("suggestedLowCutHz", 0),
                    noiseGateThresholdDb = soundJson.optDouble("noiseGateThresholdDb", -60.0).toFloat(),
                    compressorRatio = soundJson.optDouble("compressorRatio", 1.0).toFloat(),
                    compressorThresholdDb = soundJson.optDouble("compressorThresholdDb", -20.0).toFloat(),
                    reimaginedTitle = soundJson.optString("reimaginedTitle", "$soundTitle (AI Mix)"),
                    explanation = soundJson.optString("explanation", "Suggested compression and EQ to shape the signal."),
                    collaborationIdea = soundJson.optString("collaborationIdea", "Share as a dry stems sample."),
                    creativeChain = soundJson.optString("creativeChain", "Hum Removal -> Multi-tap Delay -> Room Reverb"),
                    soundLayering = soundJson.optString("soundLayering", "Layer with organic offbeat woodblock hits and sub bass harmonic pad."),
                    newComplementTitle = soundJson.optString("newComplementTitle", "Synthetic Companion Pulse"),
                    newComplementDesc = soundJson.optString("newComplementDesc", "A complementarily generated synthetic pulse styled with high sibilance control."),
                    suggestedEqPreset = soundJson.optString("suggestedEqPreset", "Vocal Clarity")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini", e)
            getFallbackSuggestion(prompt, soundTitle)
        }
    }

    private fun cleanJsonResponse(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    private fun getFallbackSuggestion(prompt: String, soundTitle: String): ReimagineResult {
        val lowercasePrompt = prompt.lowercase()
        return if (lowercasePrompt.contains("drill") || lowercasePrompt.contains("bass")) {
            ReimagineResult(
                suggestedGainDb = 4.2f,
                suggestedLowCutHz = 45,
                noiseGateThresholdDb = -48.0f,
                compressorRatio = 4.0f,
                compressorThresholdDb = -14.0f,
                reimaginedTitle = "Accra Drill Sub-Wave",
                explanation = "Gain-boosting sub-bass with a sharp high-pass filter at 45Hz. A heavy compression ratio of 4:1 stabilizes the transient punch of the low-end mud, optimized for heavy Ghanaian drill beats.",
                collaborationIdea = "Perfect for collaborating with Accra-based producers. Set a licensing tag like Promo Bundle or Buy Out.",
                creativeChain = "Low-frequency saturator -> Exciter -> Clean Limiter",
                soundLayering = "Layer with high sibilant hats and snappy mid-claps to anchor the heavy 808 glide.",
                newComplementTitle = "Accra Sub Companion Chime",
                newComplementDesc = "An ultra high frequency synthesized bell chime that outlines the dark drill chords and cuts over the heavy bass.",
                suggestedEqPreset = "Bass Boost"
            )
        } else if (lowercasePrompt.contains("clean") || lowercasePrompt.contains("noise") || lowercasePrompt.contains("restore")) {
            ReimagineResult(
                suggestedGainDb = 1.8f,
                suggestedLowCutHz = 90,
                noiseGateThresholdDb = -35.0f,
                compressorRatio = 2.0f,
                compressorThresholdDb = -18.0f,
                reimaginedTitle = "$soundTitle (Restored & Gated)",
                explanation = "Sharp low-cutoff at 90Hz filters room rumble and power line hum. Noise floor expansion gates unwanted hiss aggressively below -35.0dB to isolate peak transients.",
                collaborationIdea = "Ideal dry transient layer for collaborations. List it on the feed with a direct purchase value of 15 GHS.",
                creativeChain = "Notch Hum Filter -> Hiss Suppressor -> Soft Gate -> Mid Presence",
                soundLayering = "Pair with a high-resonance ambient pad loop to mask minor high-frequency room echoes.",
                newComplementTitle = "Static Wash Suppressor Loop",
                newComplementDesc = "A complementary noise-cancelled pad loop generated to add a clean, rich warm sustain below the dialogue.",
                suggestedEqPreset = "Vocal Clarity"
            )
        } else {
            ReimagineResult(
                suggestedGainDb = 3.0f,
                suggestedLowCutHz = 80,
                noiseGateThresholdDb = -50.0f,
                compressorRatio = 2.2f,
                compressorThresholdDb = -16.5f,
                reimaginedTitle = "$soundTitle (Sculpted)",
                explanation = "Dynamic leveling with 2.2:1 compression ratio keeps signal envelope tight and readable. High-pass filter at 80Hz limits low-end muddiness.",
                collaborationIdea = "Publish to the SoundCraft feed with a pricing level of 25 GHS to trigger MoMo revenue.",
                creativeChain = "Signal Maximizer -> Vintage Exciter -> Stereo Widener",
                soundLayering = "Blend with high-life shaker loops playing triple tempos for rhythmic micro-timing highlights.",
                newComplementTitle = "Highlife Companion Percussion",
                newComplementDesc = "Synthesized cowbells and wooden clefs designed specifically to syncopate with your main transient loop.",
                suggestedEqPreset = "Mid Punch"
            )
        }
    }
}

data class ReimagineResult(
    val suggestedGainDb: Float,
    val suggestedLowCutHz: Int,
    val noiseGateThresholdDb: Float,
    val compressorRatio: Float,
    val compressorThresholdDb: Float,
    val reimaginedTitle: String,
    val explanation: String,
    val collaborationIdea: String,
    val creativeChain: String,
    val soundLayering: String,
    val newComplementTitle: String,
    val newComplementDesc: String,
    val suggestedEqPreset: String
)
