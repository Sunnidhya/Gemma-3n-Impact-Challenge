# Gemma-3n-Impact-Challenge
MediGemm is an offline, multimodal Android application designed to deliver preliminary medical advice in underserved and remote regions with limited access to healthcare. Built using the Google AI Edge LLM Inference API and the Gemma-3n model, the app supports text, image, and multilingual (e.g., Bengali) inputs to diagnose common ailments and suggest preventive care and home remedies—all processed entirely on-device, without needing an internet connection.

The app integrates a compressed 4-bit quantized version of Gemma-3n and the Universal Sentence Encoder for embedding-based Retrieval-Augmented Generation (RAG). Upon user input, MediGemm identifies the most relevant disease using cosine similarity over precomputed embeddings and augments the query with structured context before passing it to the model. This ensures domain-restricted, contextually accurate medical responses.

**Key Features:**
<ol><li>Offline Capability: Entire model and database stored and loaded from device storage; first-launch decompression handled dynamically.</li>
 <li>Multimodal Input: Accepts and processes both text and images (e.g., for visible conditions like skin rashes).
</li> 
  <li>Multilingual Support: Supports native languages like Bengali, with bilingual disease datasets.</li>
  <li>Domain-Restricted Prompting: Hard-coded prompts ensure the model only responds to medical queries.</li>
<li>Optimized Inference: Performance tuning (topK=5, temperature=0.7, 256x256 images) reduces inference time from 9 to 2–3 minutes.
</li>
</ol>

**Challenges Overcome:**
<ol><li>
 Efficient handling of a ~3GB model through on-device ZIP extraction.
</li>
<li>Inference speedup via parameter tuning and resource-aware design (e.g., CPU backend).</li>
 <li>Managing memory and storage constraints on consumer devices.</li>
 <li>Embedding generation and caching for RAG accuracy and efficiency.</li>
</ol>

**Future Scope Includes:**

<ol><li>Fine-tuning the model on domain-specific medical data. </li>
<li>Adding rare diseases and more languages</li>
<li>Exploring LoRA and 2-bit quantization for faster and smaller deployments.</li>
 <li>Integrating user feedback for continual improvement.</li>
</ol>

**MediGemm demonstrates how cutting-edge on-device AI can deliver critical healthcare support in connectivity-challenged environments.**

**Steps to build the app**
<ol><li>Clone the repository </li>
<li>Copy the assets folder from the google drive link containing the documents, gemma model and universal sentence encoder model</li>
<li>Build the project</li>
 <li>Run and enjoy the app</li>
</ol>

**Google drive link:** https://drive.google.com/drive/folders/1nfgm73DJgyPZ3wD4x8NZhKXJicTriLkw?usp=sharing <br>
**Youtube Link for the demo:** https://www.youtube.com/watch?v=OHfFLOsTVAw

**Citation**
@misc{google-gemma-3n-hackathon,
    author = {Glenn Cameron and Omar Sanseviero and Gus Martins and Ian Ballantyne and Kat Black and Mark Sherwood and Milen Ferev and Ronghui Zhu and Nilay Chauhan and Pulkit Bhuwalka and Emily Kosa and Addison Howard},
    title = {Google - The Gemma 3n Impact Challenge},
    year = {2025},
    howpublished = {\url{https://kaggle.com/competitions/google-gemma-3n-hackathon}},
    note = {Kaggle}
}
