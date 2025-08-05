//package com.example.myapplication
//
//import android.content.Context
//import android.util.Log
//import com.google.mediapipe.tasks.genai.llminference.LlmInference
//import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
//import java.io.File
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import androidx.compose.runtime.*
//import androidx.compose.foundation.Image
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.platform.LocalContext
//
//import androidx.activity.compose.rememberLauncherForActivityResult
//import com.google.mediapipe.framework.image.BitmapImageBuilder
//
//
//import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
//import com.google.mediapipe.tasks.genai.llminference.GraphOptions
//
//import com.google.mediapipe.framework.image.MPImage
//
//
//class ModelHandler(private val context: Context) {
//    private var llmInference: LlmInference? = null
//
//    private var llmSession : LlmInferenceSession? = null
//
//    fun initialize(modelFile: File): Boolean {
//        return try {
//            // Configure LlmInference options
//            val inferenceOptions = LlmInferenceOptions.builder()
//                .setModelPath(modelFile.absolutePath)
//                .setMaxTokens(2048) // Adjust based on model specs
//                .setPreferredBackend(LlmInference.Backend.CPU) // Use GPU if support
//                .setMaxNumImages(1)
//                .build()
//
//            // Initialize LlmInference
//            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
//
//
//            Log.d("ModelHandler", "Model initialized successfully")
//            true
//        } catch (e: Exception) {
//            Log.e("ModelHandler", "Initialization failed: ${e.stackTraceToString()}")
//            false
//        }
//    }
//
//fun generateResponse(prompt: String, imageBitmap: Bitmap? = null): String? {
//    if (llmInference == null) {
//        Log.e("ModelHandler", "Inference not initialized")
//        return null
//    }
//
//    try {
//        val enableVision = imageBitmap != null
//
//        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
//            .setTopK(10)
//            .setTemperature(0.4f)
//            .setGraphOptions(GraphOptions.builder().setEnableVisionModality(enableVision).build())
//            .build()
//
//        return LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions).use { session ->
//            session.addQueryChunk(prompt)
//
//            if (enableVision) {
//                try {
//                    val resizedImage = resizeToSupportedSize(imageBitmap!!)
//                    val mpImage: MPImage = BitmapImageBuilder(resizedImage).build()
//
//                    session.addImage(mpImage)
//                    Log.d("ModelHandler", "Image added successfully")
//                } catch (e: Exception) {
//                    Log.e("ModelHandler", "Failed to add image: ${e.message}")
//                    return "Error: Image input not supported by this model."
//                }
//            }
//
//            val response = session.generateResponse()
//            Log.d("ModelHandler", "Generated response: $response")
//            response
//        }
//    } catch (e: Exception) {
//        Log.e("ModelHandler", "Multimodal inference failed: ${e.stackTraceToString()}")
//        return null
//    }
//}
//
//    private fun resizeToSupportedSize(bitmap: Bitmap): Bitmap {
//        // Pick the closest supported size
//        val targetSize = when {
//            bitmap.width >= 768 || bitmap.height >= 768 -> 768
//            bitmap.width >= 512 || bitmap.height >= 512 -> 512
//            else -> 256
//        }
//
//        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true).copy(Bitmap.Config.ARGB_8888, false)
//    }
//
//    fun close() {
//        try {
//            llmInference?.close()
//            llmInference = null
//            Log.d("ModelHandler", "Resources closed")
//        } catch (e: Exception) {
//            Log.e("ModelHandler", "Error closing resources: ${e.stackTraceToString()}")
//        }
//    }
//}


package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Embedding
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import kotlin.math.sqrt

class ModelHandler(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var textEmbedder: TextEmbedder? = null
    private val diseasesEnglish = mutableListOf<JSONObject>()
    private val diseasesBengali = mutableListOf<JSONObject>()

    // Initialize both Gemma and Text Embedder models
    fun initialize(modelFile: File, onProgress: (String) -> Unit): Boolean {
        return try {
            // Initialize Gemma model
            val inferenceOptions = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(2048)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .setMaxNumImages(1)
                .build()
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
            Log.d("ModelHandler", "Gemma model initialized successfully")

            // Initialize Text Embedder
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("universal_sentence_encoder.tflite")
                .build()
            val textEmbedderOptions = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .setL2Normalize(true) // Required for cosine similarity
                .setQuantize(true) // Optional, adjust based on model
                .build()
            textEmbedder = TextEmbedder.createFromOptions(context, textEmbedderOptions)
            Log.d("ModelHandler", "Text Embedder initialized successfully")

            // Compute embeddings for JSON files
            onProgress("Computing embeddings for English diseases...")
            loadAndComputeEmbeddings("diseases_english.json", diseasesEnglish)
            onProgress("Computing embeddings for Bengali diseases...")
            loadAndComputeEmbeddings("diseases_bengali.json", diseasesBengali)
            Log.d(
                "ModelHandler",
                "Embeddings computed: ${diseasesEnglish.size} English, ${diseasesBengali.size} Bengali diseases"
            )

            true
        } catch (e: Exception) {
            Log.e("ModelHandler", "Initialization failed: ${e.stackTraceToString()}")
            false
        }
    }

    // Load JSON and compute embeddings
    private fun loadAndComputeEmbeddings(fileName: String, diseaseList: MutableList<JSONObject>) {
        try {
            val inputStream = context.assets.open("documents/$fileName")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val disease = jsonArray.getJSONObject(i)
                // Combine fields for embedding
                val textToEmbed =
                    "${disease.getString("name")} ${disease.getString("description")} ${
                        disease.getJSONArray("symptoms").join(", ")
                    }"
                // Compute embedding
                val embeddingResult = textEmbedder?.embed(textToEmbed)
                val embedding = embeddingResult?.embeddingResult()?.embeddings()
                    ?.first() // FloatArray of size 384
                if (embedding != null) {
                    disease.put("embedding", embedding)
                    diseaseList.add(disease)
                }
            }
        } catch (e: Exception) {
            Log.e("ModelHandler", "Error processing $fileName: ${e.stackTraceToString()}")
        }
    }

    // Process prompt with embeddings and generate response
    fun generateResponse(prompt: String, imageBitmap: Bitmap? = null): String? {
        if (llmInference == null || textEmbedder == null) {
            Log.e("ModelHandler", "Inference or Text Embedder not initialized")
            return null
        }

        try {
            // Compute prompt embedding
            val promptEmbeddingResult = textEmbedder?.embed(prompt)
            val promptEmbedding =
                promptEmbeddingResult?.embeddingResult()?.embeddings()?.first() ?: return null

            // Find matching diseases
            val matchedDiseases = mutableListOf<JSONObject>()
            val languages = listOf("English" to diseasesEnglish, "Bengali" to diseasesBengali)

            val scoredDiseases =
                mutableListOf<Triple<String, JSONObject, Double>>() // language, disease, similarity

            // Step 1: Compute similarities for all diseases
            for ((language, diseaseList) in languages) {
                for (disease in diseaseList) {
                    val diseaseEmbedding = disease.get("embedding")
                    val similarity = TextEmbedder.cosineSimilarity(
                        promptEmbedding,
                        diseaseEmbedding as Embedding?
                    )

                    scoredDiseases.add(Triple(language, disease, similarity))
                }
            }

            // Step 2: Sort by similarity in descending order and take top 2
            val topMatches = scoredDiseases
                .filter { it.third > 0.5f } // optional threshold
                .sortedByDescending { it.third }
                .take(2)

            // Step 3: Add to matchedDiseases and log
            for ((language, disease, similarity) in topMatches) {
                matchedDiseases.add(disease)
                Log.d(
                    "ModelHandler",
                    "Matched [$language]: ${disease.getString("name")} (Similarity: $similarity)"
                )
            }


            val systemPrompt = """
You are MediGemm, an expert medical assistant with extensive knowledge in diseases, symptoms, treatments, medicines, and home remedies. 
Only answer questions strictly related to medical topics.

If a user asks anything outside the medical domain, politely respond with:
"I’m sorry, I cannot answer this question as it is outside my domain of expertise."

If a list of relevant diseases is provided along with the user query, use it **only if it matches the user's query context**. 
If there's no clear match between the query and the diseases, ignore the provided list and generate a medically accurate response based on your own expertise.

Regardless of whether the disease list is used or not, your final answer should follow the structure of the diseases.
Always include at home remedies/treatment for the identified disease.
""".trimIndent()


            // Create augmented prompt
            val appendedPrompt = buildString {
                append(systemPrompt + "\n")
                append(prompt + "\n")
                if (matchedDiseases.isNotEmpty()) {
                    append("\n\nRelevant Diseases:\n")
                    for (disease in matchedDiseases) {
                        append("- ${disease.getString("name")}: ${disease.getString("description")}\n")
                        append("  Symptoms: ${disease.getJSONArray("symptoms").join(", ")}\n")
                        append("  Management: ${disease.getString("management")}\n")
                        append("  Medicines: ${disease.getJSONArray("medicines").join(", ")}\n")
                        append(
                            "  Home Remedies: ${
                                disease.getJSONArray("home_remedies").join(", ")
                            }\n"
                        )
                    }
                }
            }

            // Generate response with Gemma
            val enableVision = imageBitmap != null
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(5)
                .setTemperature(0.7f)
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(enableVision)
                        .build()
                )
                .build()

            return LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
                .use { session ->
                    session.addQueryChunk(appendedPrompt)
                    if (enableVision) {
                        try {
                            val resizedImage = resizeToSupportedSize(imageBitmap!!)
                            val mpImage: MPImage = BitmapImageBuilder(resizedImage).build()
                            session.addImage(mpImage)
                            Log.d("ModelHandler", "Image added successfully")
                        } catch (e: Exception) {
                            Log.e("ModelHandler", "Failed to add image: ${e.message}")
                            return "Error: Image input not supported by this model."
                        }
                    }
                    val response = session.generateResponse()
                    Log.d("ModelHandler", "Generated response: $response")
                    response
                }
        } catch (e: Exception) {
            Log.e("ModelHandler", "Inference failed: ${e.stackTraceToString()}")
            return null
        }
    }

    // Helper: Convert JSONArray to FloatArray
    private fun JSONArray.toFloatArray(): FloatArray {
        val result = FloatArray(this.length())
        for (i in 0 until this.length()) {
            result[i] = this.getDouble(i).toFloat()
        }
        return result
    }

    // Helper: Compute cosine similarity
    private fun computeCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }

    // Helper: Resize image for vision model
    private fun resizeToSupportedSize(bitmap: Bitmap): Bitmap {
        val targetSize = when {
            bitmap.width >= 768 || bitmap.height >= 768 -> 768
            bitmap.width >= 512 || bitmap.height >= 512 -> 512
            else -> 256
        }
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    // Close resources
    fun close() {
        try {
            llmInference?.close()
            textEmbedder?.close()
            llmInference = null
            textEmbedder = null
            Log.d("ModelHandler", "Resources closed")
        } catch (e: Exception) {
            Log.e("ModelHandler", "Error closing resources: ${e.stackTraceToString()}")
        }
    }
}