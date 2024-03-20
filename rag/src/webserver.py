from flask import Flask, g, render_template, request, jsonify, current_app

from embedder import Embedder

app = Flask(__name__)

@app.route('/embed', methods=['POST'])
def embed():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        text = data['text']
        result['embedding'] = get_embedding_model().embed(text)
        result['status'] = 200
    except Exception as ex:
        result['error'] = "There was an exception caught while trying to embed: " + ex
        print("Exception while trying to get embedding: ")
        print(ex)
    return jsonify(result)


def get_embedding_model():
    '''Gets the embedding model from the app's cache'''
    if 'embedding_model' not in current_app.config:
        current_app.config['embedding_model'] = Embedder()
    return current_app.config['embedding_model']


if __name__ == '__main__':
    app.run(debug=True, port=5678)