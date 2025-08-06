# Gemma-3n-Impact-Challenge
MediGemm is an offline, multimodal Android application designed to deliver preliminary medical advice in underserved and remote regions with limited access to healthcare. Built using the Google AI Edge LLM Inference API and the Gemma-3n model, the app supports text, image, and multilingual (e.g., Bengali) inputs to diagnose common ailments and suggest preventive care and home remedies—all processed entirely on-device, without needing an internet connection.

The app integrates a compressed 4-bit quantized version of Gemma-3n and the Universal Sentence Encoder for embedding-based Retrieval-Augmented Generation (RAG). Upon user input, MediGemm identifies the most relevant disease using cosine similarity over precomputed embeddings and augments the query with structured context before passing it to the model. This ensures domain-restricted, contextually accurate medical responses.

Key Features:
<ol><li>Offline Capability: Entire model and database stored and loaded from device storage; first-launch decompression handled dynamically.</li>
 <li>Multimodal Input: Accepts and processes both text and images (e.g., for visible conditions like skin rashes).
</li> 
  <li>Multilingual Support: Supports native languages like Bengali, with bilingual disease datasets.</li>
  <li>Domain-Restricted Prompting: Hard-coded prompts ensure the model only responds to medical queries.</li>
<li>Optimized Inference: Performance tuning (topK=5, temperature=0.7, 256x256 images) reduces inference time from 9 to 2–3 minutes.
</li>
</ol>

Challenges Overcome:
Efficient handling of a ~3GB model through on-device ZIP extraction.

Inference speedup via parameter tuning and resource-aware design (e.g., CPU backend).

Managing memory and storage constraints on consumer devices.

Embedding generation and caching for RAG accuracy and efficiency.

Future Scope Includes:

1.Fine-tuning the model on domain-specific medical data.

Adding rare diseases and more languages.

Exploring LoRA and 2-bit quantization for faster and smaller deployments.

Integrating user feedback for continual improvement.

MediGemm demonstrates how cutting-edge on-device AI can deliver critical healthcare support in connectivity-challenged environments.
