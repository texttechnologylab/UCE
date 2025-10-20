The RAG service is a Python web server running on Flask, which acts as the gateway to modern AI, NLP, and ML technologies. It handles the creation of embeddings, querying large language models (LLMs) for the RAGbot, and similar operations.

!!! warning "High Resources"
    Depending on the use case and the available machine, the RAG service may be resource-intensive, particularly in terms of RAM and GPU usage.

<hr/>

## User Setup

For this, it is assumed that the repository has already been cloned in a prior step. Afterwards, simply start the service via Docker Compose:

```
docker-compose up --build uce-rag-service
```

The RAG service should now be up and running on the port mapped in the Dockerfile. See [further down](#settings) for information about model usage and customizable settings of the service.

<hr/>

## Developer Setup

After cloning the repository, navigate to the `rag` folder. There, create a new Python environment using a tool of your choice:


```
python -m venv env
```

*The name `env` is already included in `.gitignore`, so it's recommended to use that name if possible.*

Activate the environment:

```
source env/bin/activate
```

??? warning "Activate on Windows"
    On Windows, activate the environment with the following command:
    ```
    .\env\Scripts\activate
    ```

Then, install the dependencies and start the service:

```
pip install -r requirements.txt
python src/webserver.py
```

<hr/>

## Settings

The RAGBot can be configured with different LLMs for inference and user interaction. It is recommended to use either an OpenAI model (in which case an OpenAI API key must be set) or a locally hosted [Ollama server](https://ollama.com/).

In both cases, the configuration must be defined in the [UCE Configuration](configuration.md). The relevant excerpt is shown below:

??? example "Example RAGBot settings within uceConfig.json"
    ```json title="uceConfig.json"
    {
      "settings": {
        "rag": {
          "models": [
            {
              "model": "ollama/gemma3:latest",
              "url": "http://ollama.llm.texttechnologylab.org/",
              "apiKey": "",
              "displayName": "Gemma3 (4.3B - Google)"
            },
            {
              "model": "ollama/deepseek-r1:latest",
              "url": "http://ollama.llm.texttechnologylab.org/",
              "apiKey": "",
              "displayName": "DeepSeek-R1 (7.6B - DeepSeek)"
            },
            {
              "model": "openai/o4-mini-2025-04-16",
              "url": "",
              "apiKey": "YOUR_API_KEY",
              "displayName": "OpenAI's O4 Mini"
            }
          ]
        }
      }
    }
    ```

For OpenAI models, you can specify any model name listed in the [OpenAI model catalog](https://platform.openai.com/docs/models). UCE provides the option for users to select which model they want to use from the list of models defined in the UCE configuration.

