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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelHandler = ModelHandler(applicationContext)

        setContent {
            MyApplicationTheme {
                var promptText by remember { mutableStateOf("") }
                var result by remember { mutableStateOf("") }
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var isProcessing by remember { mutableStateOf(false) }
                var progressMessage by remember { mutableStateOf<String?>(null) }
                val scrollState = rememberScrollState()
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                val pickImageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    imageUri = uri
                    uri?.let {
                        val inputStream = context.contentResolver.openInputStream(it)
                        imageBitmap = BitmapFactory.decodeStream(inputStream)
                    }
                }

                Scaffold(
//                    topBar = {
//                        TopAppBar(
//                            title = { Text("MediGem - AI Diagnosis") }
//                        )
//                    },
                    topBar = {
                        TopAppBar(
                            modifier = Modifier
                                .height(80.dp), // Smaller height
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF3E4C84) // Blue background
                            ),
                            title = {
                                Text(
                                    text = "MediGem - AI Diagnosis",
                                    color = Color.White,
                                    fontSize = 18.sp,
//                                    style = MaterialTheme.typography.displaySmall,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    },

//                            bottomBar = {
//                        BottomAppBar {
//                            Text(
//                                "© 2025 MediGem. Not for emergency use.",
//                                modifier = Modifier.padding(4.dp),
//                                textAlign = TextAlign.Center,
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }
                    bottomBar = {
                        BottomAppBar(
                            modifier = Modifier
                                .height(88.dp), // 🔧 Adjustable height
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(), // Optional padding
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    "© 2025 MediGem. Not for emergency use.",
//                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                    }

                ) { innerPadding ->

                    Box(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                label = { Text("Describe your symptoms") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = {
                                    pickImageLauncher.launch("image/*")
                                }) {
                                    Text("Select Image")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                if (imageBitmap != null) {
                                    Button(
                                        onClick = {
                                            imageBitmap = null
                                            imageUri = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete Image")
                                    }
                                }
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
                                coroutineScope.launch {
                                    isProcessing = true
                                    progressMessage = "LLM is analyzing..."
                                    result = "" // ✅ Clear previous response on new request
                                    try {
                                        if (!isModelLoaded) {
                                            result = "Model not loaded. Please try again."
                                        } else {
                                            val response = withContext(Dispatchers.IO) {
                                                modelHandler.generateResponse(promptText, imageBitmap)
                                            }
                                            result = response ?: "No response generated."
                                            Log.d("Gemma_Result", result)
                                        }
                                    } catch (e: Exception) {
                                        result = "Error: ${e.message}"
                                        Log.e("Gemma_Error", e.toString())
                                    } finally {
                                        isProcessing = false
                                        progressMessage = null
                                    }
                                }
                            }) {
                                Text("Diagnose")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (result.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Response:", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { result = "" }) {
                                        Text("Clear Response")
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 300.dp)
                                        .verticalScroll(rememberScrollState())
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                        .padding(8.dp)
                                ) {
                                    Text(result, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        // Fullscreen loading overlay
                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(60.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        progressMessage ?: "Processing...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }


                    // 🔁 LaunchedEffect for model loading
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
                                    Toast.makeText(this@MainActivity, "Model loaded successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Failed to load model", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                isProcessing = false
                                progressMessage = null
                                Log.e("MainActivity", "Error processing model: ${e.stackTraceToString()}")
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                                Toast.makeText(this@MainActivity, "Model loaded from storage", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Failed to load model", Toast.LENGTH_LONG).show()
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

            updateProgress("Decompressing model...")
            var modelFound = false
            ZipInputStream(compressedFile.inputStream()).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (entry.name == "gemma-3n-E2B-it-int4.task") {
                        FileOutputStream(modelFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                        modelFound = true
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }

            if (!modelFound || !modelFile.exists() || modelFile.length() < 1_000_000_000L) {
                throw IllegalStateException("Model file missing or too small")
            }

            updateProgress("Cleaning up...")
            if (compressedFile.exists()) compressedFile.delete()

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