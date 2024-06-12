from typing import Any
import numpy as np
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler
from pacmap import PaCMAP
from trimap import TRIMAP
from umap import UMAP


class Reducer:
    '''
    A Reducer class that reduces high-dimensinoal vector/matrix spaces into lower dimensional spaces for
    plotting purposes or evaluating purposes.
    '''

    def __init__(self, r_function_name: str, n_compt: int, random_state: Any = None):
        self.r_function_name = r_function_name
        self.n_compt = n_compt
        self.random_state = random_state

    def reduce(self, vector_matrix: list, perplexity: int = 30, scaling: bool = False):
        '''
        Reduces the given vector matrix into low-dimensional spaces.
        '''

        matrix_embeddings = vector_matrix
        # list to np_array
        matrix_embeddings = np.array(matrix_embeddings)
        # Fit_function Variants
        algo = None
        if self.r_function_name == "PCA":
            algo = PCA(n_components=self.n_compt, random_state=self.random_state)
        elif self.r_function_name == "TSNE":
            algo = TSNE(n_components=self.n_compt, perplexity=perplexity, n_jobs=6, random_state=self.random_state)
        elif self.r_function_name == "PACMAP":
            algo = PaCMAP(n_components=self.n_compt, n_neighbors=None, random_state=self.random_state)
        elif self.r_function_name == "TRIMAP":
            algo = TRIMAP(n_dims=self.n_compt)
        elif self.r_function_name == "UMAP":
            algo = UMAP(n_components=self.n_compt, random_state=self.random_state)
        if algo:
            x_embeddings = algo.fit_transform(matrix_embeddings)
        else:
            x_embeddings = matrix_embeddings
        # Standard_scaler (I would recommend it) https://scikit-learn.org/stable/modules/manifold.html#tips-on-practical-use
        if scaling:
            x_embeddings = StandardScaler().fit_transform(x_embeddings)
        return x_embeddings
    
    def find_optimal_clusters(self, data, max_k):
        '''
        I use the reducer to plot a TSNE plot. I want clusters in that and to determine the amount of clusters, I use the silhoutte score.
        https://scikit-learn.org/stable/modules/generated/sklearn.metrics.silhouette_score.html
        '''

        iters = range(2, max_k + 1, 1)
        sse = []
        silhouette_scores = []

        for k in iters:
            kmeans = KMeans(n_clusters=k, random_state=0).fit(data)
            sse.append(kmeans.inertia_)
            score = silhouette_score(data, kmeans.labels_)
            silhouette_scores.append(score)
        
        return sse, silhouette_scores
    
def test():
    # Sample high-dimensional word embeddings (e.g., 5 words with 500-dimensional embeddings)
    sample_embeddings = [
        np.random.rand(500).tolist(),
        np.random.rand(500).tolist(),
        np.random.rand(500).tolist(),
        np.random.rand(500).tolist(),
        np.random.rand(500).tolist()
    ]

    # Define the number of components for reduction
    n_components = 2
    reducer_pca = Reducer(r_function_name="PCA", n_compt=n_components)
    reduced_embeddings_pca = reducer_pca.reduce(sample_embeddings)

    # Verify the reduced dimensions
    assert reduced_embeddings_pca.shape == (len(sample_embeddings), n_components), "PCA reduction did not work as expected"
    print(reduced_embeddings_pca)
    print("PCA reduction output shape:", reduced_embeddings_pca.shape)

    # Instantiate the Reducer class with t-SNE
    reducer_tsne = Reducer(r_function_name="TSNE", n_compt=n_components)
    reduced_embeddings_tsne = reducer_tsne.reduce(sample_embeddings, perplexity=3)

    # Verify the reduced dimensions
    assert reduced_embeddings_tsne.shape == (len(sample_embeddings), n_components), "t-SNE reduction did not work as expected"
    print(reduced_embeddings_tsne)
    print("t-SNE reduction output shape:", reduced_embeddings_tsne.shape)

    print("All tests successfull.")


if __name__ == "__main__":
    test()