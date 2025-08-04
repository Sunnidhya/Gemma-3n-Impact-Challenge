package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    private lateinit var modelHandler: ModelHandler
    private var isModelLoaded by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelHandler = ModelHandler(applicationContext)

        setContent {
            MyApplicationTheme {
                var progressMessage by remember { mutableStateOf<String?>(null) }
                var isProcessing by remember { mutableStateOf(false) }

                if (isProcessing && progressMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = progressMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(16.dp)
                        ) {
                            var promptText by remember { mutableStateOf("") }
                            var result by remember { mutableStateOf("") }
                            var imageUri by remember { mutableStateOf<Uri?>(null) }
                            var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                            val coroutineScope = rememberCoroutineScope()
                            val context = LocalContext.current

                            val pickImageLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                imageUri = uri
                                uri?.let {
                                    val inputStream = context.contentResolver.openInputStream(it)
                                    imageBitmap = BitmapFactory.decodeStream(inputStream)
                                }
                            }

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                label = { Text("Enter Prompt") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                pickImageLauncher.launch("image/*")
                            }) {
                                Text("Select Image")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            imageBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (!isModelLoaded) {
                                        withContext(Dispatchers.Main) {
                                            result = "Model not loaded. Please try again."
                                        }
                                        return@launch
                                    }
                                    try {
                                        val response = modelHandler.generateResponse(promptText, imageBitmap)
                                        withContext(Dispatchers.Main) {
                                            result = response ?: "No response generated."
                                        }
                                        Log.d("Gemma_Result", result)
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            result = "Error: ${e.message}"
                                        }
                                        Log.e("Gemma_Error", e.toString())
                                    }
                                }
                            }) {
                                Text("Generate Response")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Response:", style = MaterialTheme.typography.titleMedium)
                            Text(result, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (filesDir.usableSpace < 4_500_000_000L) {
                        Toast.makeText(this@MainActivity, "Insufficient storage space for model", Toast.LENGTH_LONG).show()
                        finish()
                        return@LaunchedEffect
                    }

                    val modelFile = File(filesDir, "gemma-3n-E2B-it-int4.task")
                    if (!modelFile.exists()) {
                        isProcessing = true
                        progressMessage = "Copying model file..."
                        try {
                            withContext(Dispatchers.IO) {
                                extractModelFromAssets { message ->
                                    launch(Dispatchers.Main) {
                                        progressMessage = message
                                    }
                                }
                            }
                            progressMessage = "Loading model..."
                            isModelLoaded = withContext(Dispatchers.IO) {
                                modelHandler.initialize(modelFile) { message ->
                                    launch(Dispatchers.Main) {
                                        progressMessage = message
                                    }
                                }
                            }
                            isProcessing = false
                            if (isModelLoaded) {
                                Toast.makeText(this@MainActivity, "Model and embeddings loaded successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Failed to load model or embeddings", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            isProcessing = false
                            progressMessage = null
                            Log.e("MainActivity", "Error processing model: ${e.stackTraceToString()}")
                            Toast.makeText(this@MainActivity, "Error processing model: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        isProcessing = true
                        progressMessage = "Loading model..."
                        isModelLoaded = withContext(Dispatchers.IO) {
                            modelHandler.initialize(modelFile) { message ->
                                launch(Dispatchers.Main) {
                                    progressMessage = message
                                }
                            }
                        }
                        isProcessing = false
                        if (isModelLoaded) {
                            Toast.makeText(this@MainActivity, "Model and embeddings loaded from storage", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to load model or embeddings", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private suspend fun extractModelFromAssets(updateProgress: (String) -> Unit) {
        val compressedFile = File(filesDir, "gemma-3n-E2B-it-int4.zip")
        val modelFile = File(filesDir, "gemma-3n-E2B-it-int4.task")

        try {
            if (filesDir.usableSpace < 4_500_000_000L) {
                throw IllegalStateException("Insufficient storage space for model extraction")
            }

            val assetList = assets.list("")?.toList() ?: emptyList()
            Log.d("MainActivity", "Assets available: $assetList")
            if (!assetList.contains("gemma-3n-E2B-it-int4.zip")) {
                throw IllegalStateException("ZIP file not found in assets")
            }

            updateProgress("Copying model file...")
            Log.d("MainActivity", "Copying compressed file from assets")
            assets.open("gemma-3n-E2B-it-int4.zip").use { inputStream ->
                FileOutputStream(compressedFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesCopied: Long = 0
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                    }
                    Log.d("MainActivity", "Copied $bytesCopied bytes to ${compressedFile.absolutePath}")
                }
            }

            if (!compressedFile.exists() || compressedFile.length() == 0L) {
                throw IllegalStateException("Compressed file is missing or empty")
            }

            updateProgress("Decompressing model...")
            Log.d("MainActivity", "Decompressing ${compressedFile.name}")
            var modelFound = false
            ZipInputStream(compressedFile.inputStream()).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    Log.d("MainActivity", "ZIP entry: ${entry.name}")
                    if (entry.name == "gemma-3n-E2B-it-int4.task") {
                        FileOutputStream(modelFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesDecompressed: Long = 0
                            var bytesRead: Int
                            while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDecompressed += bytesRead
                            }
                            Log.d("MainActivity", "Decompressed $bytesDecompressed bytes to ${modelFile.absolutePath}")
                        }
                        modelFound = true
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }

            if (!modelFound) {
                throw IllegalStateException("Model file 'gemma-3n-E2B-it-int4.task' not found in ZIP archive")
            }
            if (!modelFile.exists() || modelFile.length() < 1_000_000_000L) {
                throw IllegalStateException("Model file is missing or too small: ${modelFile.length()} bytes")
            }

            updateProgress("Cleaning up...")
            if (compressedFile.exists()) {
                compressedFile.delete()
                Log.d("MainActivity", "Deleted compressed file: ${compressedFile.name}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error extracting model: ${e.stackTraceToString()}")
            throw e
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        modelHandler.close()
    }
}