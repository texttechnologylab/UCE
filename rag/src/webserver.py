from cBERT.cBERT import CCCBERT
from flask import Flask, g, render_template, request, jsonify, current_app

from embedder import Embedder
from llm import InstructLLM

app = Flask(__name__)

@app.route('/embed', methods=['POST'])
def embed():
    result = {
        "status": 400
    }
    try:
        data = request.get_json()
        text = data['text']
        result['status'] = 200
        result['message'] = get_embedding_model().embed(text)
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
        result['message'] = get_instruct_model().complete(messages, api_key)
        result['status'] = 200
    except Exception as ex:
        result['message'] = "There was an exception caught while trying to complete the chat: " + str(ex)
        print("Exception while trying to complete chat: ")
        print(ex)
    return jsonify(result)

def get_embedding_model():
    '''Gets the embedding model from the app's cache'''
    if 'embedding_model' not in current_app.config:
        current_app.config['embedding_model'] = Embedder()
    return current_app.config['embedding_model']

def get_instruct_model():
    '''Gets the llm that has the actual conversation'''
    if 'instruct_model' not in current_app.config:
        current_app.config['instruct_model'] = InstructLLM('ChatGPT')
    return current_app.config['instruct_model']

def get_CCCBERT_model():
    '''Gets the CCC-BERT model to check whether we need context or not'''
    if 'ccc_bert' not in current_app.config:
        current_app.config['ccc_bert'] = CCCBERT('./cBERT/models/cBERT_35k.pth')
    return current_app.config['ccc_bert']

if __name__ == '__main__':
    app.run(debug=True, port=5678)