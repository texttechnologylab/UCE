from typing import Union, Dict, List

import torch
from langchain_ollama import OllamaEmbeddings
from sentence_transformers import SentenceTransformer

class Embedder:
    BACKEND_SENTENCE_TRANSFORMERS = "sentence_transformers"
    BACKEND_OLLAMA = "ollama"

    def __init__(self, backend: Union[str, None] = None, config: Union[Dict, None] = None):
        if backend is not None:
            self.backend = backend
        else:
            # keep original as default
            self.backend = self.BACKEND_SENTENCE_TRANSFORMERS

        if self.backend == self.BACKEND_SENTENCE_TRANSFORMERS:
            # Refer there for proper citation!
            # https://huggingface.co/mixedbread-ai/mxbai-embed-large-v1
            self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
            self.model = SentenceTransformer("mixedbread-ai/mxbai-embed-large-v1").to(self.device)
            # self.model = SentenceTransformer("mixedbread-ai/mxbai-embed-xsmall-v1").to(self.device)
        elif self.backend == self.BACKEND_OLLAMA:
            print("Using Ollama Embeddings")
            print(config)
            self.model = OllamaEmbeddings(**config)
        else:
            raise NotImplementedError(f"Backend  \"{self.backend}\" is not implemented.")

    def embed(self, text: str) -> List[float]:
        query = 'Represent this sentence for searching relevant passages: ' + text

        if self.backend == self.BACKEND_SENTENCE_TRANSFORMERS:
            docs = [query]
            with torch.no_grad():
                embeddings = self.model.encode(docs)
                # Move embeddings to specified device
            # Embedding is of dimensionality 1024
            return embeddings[0].tolist()
        elif self.backend == self.BACKEND_OLLAMA:
            embeddings = self.model.embed_query(query)
            return embeddings

        raise NotImplementedError(f"Backend \"{self.backend}\" is not implemented.")

if __name__ == "__main__":
    # You can specify the device here, e.g., torch.device('cuda') for GPU or torch.device('cpu') for CPU
    # embedder = Embedder()
    embedder = Embedder(
        backend="ollama",
        config={
            "base_url": "http://localhost:11434",
            "model": "llama3.2:latest",
        }
    )
    print(embedder.embed("Dies ist ein Test"))
