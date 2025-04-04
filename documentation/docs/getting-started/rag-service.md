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

As of now, the only language model that works **out of the box** without modifying the source code is ChatGPT. When enabling the RAGBot in the [Corpus Configuration](configuration.md), you must provide your OpenAI key in the `settings` section of the [UCE Configuration](configuration.md).

However, it is easy to adjust the RAG service to query locally hosted or alternative language models instead of using the OpenAI API by diving the `rag` source code. **We plan to add out-of-the-box configuration options for this functionality as soon as possible.**
