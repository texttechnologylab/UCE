import torch
from sentence_transformers import SentenceTransformer
from sentence_transformers.util import cos_sim

class Embedder:

    def __init__(self):
        # Refer there for proper citation!
        # https://huggingface.co/mixedbread-ai/mxbai-embed-large-v1
        self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
        self.model = SentenceTransformer("mixedbread-ai/mxbai-embed-large-v1").to(self.device)

    def embed(self, text):
        query = 'Represent this sentence for searching relevant passages: ' + text
        docs = [query]
        with torch.no_grad():
            embeddings = self.model.encode(docs)
        # Embedding is of dimensionality 1024
        #similarities = cos_sim(embeddings[0], embeddings[1:])
        return embeddings[0]

if __name__ == "__main__":
    # You can specify the device here, e.g., torch.device('cuda') for GPU or torch.device('cpu') for CPU
    embedder = Embedder()
    print(embedder.embed("Dies ist ein Test"))
