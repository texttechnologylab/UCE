from typing import Union, Dict

import plotly.express as px
import plotly.io as pio
import gc
import numpy as np
import torch
import warnings
import traceback

from cBERT.cBERT import CCCBERT
from flask import Flask, g, render_template, request, jsonify, current_app, Response, stream_with_context
from sklearn.cluster import KMeans
from scipy.spatial.distance import euclidean
from scipy.sparse.csgraph import minimum_spanning_tree
from scipy.sparse import csr_matrix

from embedder import Embedder
from dimension_reducer import Reducer
from llm import InstructLLM
from topic_modelling import TopicModel

app = Flask(__name__)
warnings.filterwarnings("ignore")

@app.route('/plot/tsne', methods=['POST'])
def plot_tsne():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        reduced_embeddings = np.array(data['embeddings'])
        labels = data.get('labels', [])
        labels = list(map(lambda l: "unknown" if l is None else l, labels))

        # Prepare the plot
        fig = px.scatter(x=reduced_embeddings[:, 0], y=reduced_embeddings[:, 1], color=labels, title='t-SNE Plot with Clusters')

        # Calculate centroids of the clusters
        centroids = np.array([reduced_embeddings[np.array(labels) == label].mean(axis=0) for label in np.unique(labels)])

        # Add rectangles to highlight clusters
        unique_labels = np.unique(labels)
        colors = px.colors.qualitative.Plotly
        
        for label in unique_labels:
            cluster_points = reduced_embeddings[np.array(labels) == label]
            x_min, y_min = cluster_points.min(axis=0)
            x_max, y_max = cluster_points.max(axis=0)
            color = "blue"
            #fig.add_shape(
            #    type="rect",
            #    x0=x_min, y0=y_min, x1=x_max, y1=y_max,
            #    line=dict(color=color),
            #    fillcolor="rgba(0,0,0,0)",  # transparent fill color
            #    opacity=0.6
            #)

        # Compute the minimum spanning tree (MST) for clusters
        dist_matrix = np.array([[euclidean(centroid1, centroid2) for centroid2 in centroids] for centroid1 in centroids])
        csr_matrix_dist = csr_matrix(dist_matrix)

        # Compute the MST
        mst = minimum_spanning_tree(csr_matrix_dist)
        mst = mst.toarray().astype(float)

        # Draw MST lines and label with distances
        for i in range(len(centroids)):
            for j in range(i+1, len(centroids)):
                if mst[i, j] != 0:
                    centroid1 = centroids[i]
                    centroid2 = centroids[j]
                    distance = mst[i, j]
                    fig.add_shape(
                        type="line",
                        x0=centroid1[0], y0=centroid1[1], x1=centroid2[0], y1=centroid2[1],
                        line=dict(color="gray", width=1, dash="dash")
                    )
                    mid_x = (centroid1[0] + centroid2[0]) / 2
                    mid_y = (centroid1[1] + centroid2[1]) / 2
                    #fig.add_annotation(
                    #    x=mid_x, y=mid_y,
                    #    text=f"{distance:.2f}",
                    #    showarrow=False,
                    #    font=dict(color="Black", size=12, family="Arial")
                    #)

        fig.update_coloraxes(showscale=False)
        fig.update_layout(showlegend=False)
        plot_html = pio.to_html(fig, full_html=False)

        result['status'] = 200
        result['plot'] = plot_html

    except Exception as ex:
        result['message'] = "There was an exception caught while generating t-SNE plot: " + str(ex)
        print("Exception while generating t-SNE plot: ")
        print(ex)
        traceback.print_exc()

    return jsonify(result)

@app.route('/plot/tsne-reduce', methods=['POST'])
def plot_tsne_reduce():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        embeddings = data['embeddings']
        # labels = data.get('labels', [])

        reducer_tsne = Reducer(r_function_name="TSNE", n_compt=2)

        # Find optimal number of clusters
        max_k = 10 
        sse, silhouette_scores = reducer_tsne.find_optimal_clusters(embeddings, max_k)
        optimal_k = np.argmax(silhouette_scores) + 2  # since range starts at 2

        # Perform K-means clustering with optimal number of clusters
        kmeans = KMeans(n_clusters=optimal_k, random_state=0).fit(embeddings)
        labels = kmeans.labels_

        # Reduce the embeddings
        reduced_embeddings = reducer_tsne.reduce(embeddings)

        # color=labels
        fig = px.scatter(x=reduced_embeddings[:, 0], y=reduced_embeddings[:, 1], color=labels, title='')

        # Calculate the centroids of the clusters so we can add lines later
        centroids = np.array([reduced_embeddings[labels == label].mean(axis=0) for label in np.unique(labels)])

        # Add rectangles to highlight clusters
        unique_labels = np.unique(labels)
        colors = px.colors.qualitative.Plotly
        
        for label in unique_labels:
            cluster_points = reduced_embeddings[labels == label]
            x_min, y_min = cluster_points.min(axis=0)
            x_max, y_max = cluster_points.max(axis=0)
            color = colors[label % len(colors)]
            fig.add_shape(
                type="rect",
                x0=x_min, y0=y_min, x1=x_max, y1=y_max,
                line=dict(color=color),
                fillcolor="rgba(0,0,0,0)",  # transparent fill color
                opacity=0.6
            )

        # We want to connect the clusters with their distances, for that we calculate the minimum spanning tree (Algo1 paid of guys)
        dist_matrix = np.array([[euclidean(centroid1, centroid2) for centroid2 in centroids] for centroid1 in centroids])
        csr_matrix_dist = csr_matrix(dist_matrix)

        # Compute the MST
        mst = minimum_spanning_tree(csr_matrix_dist)
        mst = mst.toarray().astype(float)

        # Draw MST lines and label with distances
        for i in range(len(centroids)):
            for j in range(i+1, len(centroids)):
                if mst[i, j] != 0:
                    centroid1 = centroids[i]
                    centroid2 = centroids[j]
                    distance = mst[i, j]
                    fig.add_shape(
                        type="line",
                        x0=centroid1[0], y0=centroid1[1], x1=centroid2[0], y1=centroid2[1],
                        line=dict(color="gray", width=1, dash="dash")
                    )
                    mid_x = (centroid1[0] + centroid2[0]) / 2
                    mid_y = (centroid1[1] + centroid2[1]) / 2
                    fig.add_annotation(
                        x=mid_x, y=mid_y,
                        text=f"{distance:.2f}",
                        showarrow=False,
                        font=dict(color="Black", size=12, family="Arial")
                    )

        fig.update_coloraxes(showscale=False)
        plot_html = pio.to_html(fig, full_html=False)

        result['status'] = 200
        result['plot'] = plot_html
    except Exception as ex:
        result['message'] = "There was an exception caught while generating t-SNE plot: " + str(ex)
        print("Exception while generating t-SNE plot: ")
        print(ex)
    return jsonify(result)

@app.route('/topic-modelling', methods=['POST'])
def topic_modelling():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        text = data['text']
        
        # Extract keywords using RAKE
        rake_keywords = get_topic_model().extract_keywords_rake(text)
        
        # Extract keywords using YAKE
        yake_keywords = get_topic_model().extract_keywords_yake(text)
        
        # Prepare the result
        result['status'] = 200
        result['rakeKeywords'] = rake_keywords
        result['yakeKeywords'] = yake_keywords
    except Exception as ex:
        result['message'] = "There was an exception caught while trying to embed: " + str(ex)
        print("Exception while trying to get embedding: ")
        print(ex)
    return jsonify(result)

@app.route('/embed/reduce', methods=['POST'])
def reduce():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        embeddings = data['embeddings']
        result['status'] = 200
        
        perplexity = min(15, len(embeddings) - 1)
        reducer_tsne_2d = Reducer(r_function_name="TSNE", n_compt=2)
        result['tsne2D'] = np.array(reducer_tsne_2d.reduce(embeddings, perplexity=perplexity)).tolist()

        reducer_tsne_3d = Reducer(r_function_name="TSNE", n_compt=3)
        result['tsne3D'] = np.array(reducer_tsne_3d.reduce(embeddings, perplexity=perplexity)).tolist()
    except Exception as ex:
        result['message'] = "There was an exception caught while trying to embed: " + str(ex)
        print("Exception while trying to get embedding: ")
        print(ex)
    return jsonify(result)


@app.route('/embed', methods=['POST'])
def embed():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        text = data['text']
        config = data['config'] if "config" in data else None
        backend = data['backend'] if "backend" in data else None
        result['status'] = 200
        result['message'] = get_embedding_model(backend, config).embed(text)
    except Exception as ex:
        result['message'] = "There was an exception caught while trying to embed: " + str(ex)
        print("Exception while trying to get embedding: ")
        print(ex)
    return jsonify(result)

@app.route('/rag/context', methods=['POST'])
def context():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        user_input = data['userInput']
        result['status'] = 200
        result['message'] = get_CCCBERT_model().predict_context_needed(user_input)
    except Exception as ex:
        result['message'] = "There was an exception caught while using CCC-BERT for context: " + str(ex)
        print("Exception while trying to get CCC-BERT contect decision: ")
        print(ex)
    return jsonify(result)

@app.route('/rag/complete/stream', methods=['POST'])
def rag_complete_stream():
    try:
        data = request.get_json()
        print(data)
        messages = data['promptMessages']
        api_key = data['apiKey']
        model = data['model']
        url = data['url']
    except Exception as ex:
        result = {
            "status": 400,
            "message": "There was an exception caught while trying to complete the chat: " + str(ex)
        }
        print("Exception while trying to complete chat: ")
        print(ex)
        return jsonify(result)

    return Response(
        stream_with_context(
            get_instruct_model(model, url).complete_stream(messages, api_key)
        ),
        mimetype='application/json'
    )

@app.route('/rag/complete', methods=['POST'])
def rag_complete():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        print(data)
        messages = data['promptMessages']
        api_key = data['apiKey']
        model = data['model']
        url = data['url']
        if "tools" in data:
            tools = data["tools"]
        else:
            tools = None
        result['message'] = get_instruct_model(model, url).complete(messages, api_key, tools)
        result['status'] = 200
    except Exception as ex:
        result['message'] = "There was an exception caught while trying to complete the chat: " + str(ex)
        print("Exception while trying to complete chat: ")
        print(ex)
    return jsonify(result)

@app.route('/', methods=['GET'])
def hello():
    return 'RAGServer running.'

def get_embedding_model(backend: Union[str, None] = None, config: Union[Dict, None] = None):
    '''Gets the embedding model from the app's cache'''
    # NOTE this is a hack: the importer performs a test call at a point where the UCE config is not yet loaded.
    # This results in a cached default model, we therefore explicitly check for the backend.
    # This also will not reload the model if e.g. the model name changes!
    # TODO caching based on actual config and backend
    keyname = f"embedding_model"
    if backend is not None:
        keyname = f"embedding_model_{backend}"
    if keyname not in current_app.config:
        current_app.config[keyname] = Embedder(backend, config)
    return current_app.config[keyname]

def get_instruct_model(model_name, url):
    '''Gets the llm that has the actual conversation'''
    # dont cache ollama models, these are not loading large models locally
    if InstructLLM.should_cache(model_name):
        if model_name not in current_app.config:
            current_app.config[model_name] = InstructLLM(model_name, url)
        return current_app.config[model_name]
    # just recreate every time
    return InstructLLM(model_name, url)

def get_CCCBERT_model():
    '''Gets the CCC-BERT model to check whether we need context or not'''
    if 'ccc_bert' not in current_app.config:
        current_app.config['ccc_bert'] = CCCBERT('./cBERT/models/cBERT_35k.pth')
    return current_app.config['ccc_bert']

def get_topic_model():
    '''Gets the topic model instance'''
    if 'topic_model' not in current_app.config:
        current_app.config['topic_model'] = TopicModel()
    return current_app.config['topic_model']

if __name__ == '__main__':
    print("Rag service initialized!")
    app.run(debug=True, port=5678, host="0.0.0.0")