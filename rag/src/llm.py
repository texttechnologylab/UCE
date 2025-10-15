import requests
import torch
from openai import OpenAI
from transformers import AutoModelForCausalLM, AutoTokenizer


class InstructLLM:

    def __init__(self, model_name, url):
        if model_name.startswith("openai/"):
            self.model = ChatGPT(model_name.replace("openai/", ""))
        elif model_name.startswith("ollama/"):
            self.model = OllamaModel(model_name.replace("ollama/", ""), url)
        else:
            self.model = CausalLM(model_name=model_name)

    @staticmethod
    def should_cache(model_name):
        # dont cache ollama/openai models, these just proxy the api
        if model_name.startswith("openai/"):
            return False
        elif model_name.startswith("ollama/"):
            return False
        return True

    def complete(self, messages, api_key='', tools=None):
        return self.model.complete(messages=messages, api_key=api_key, tools=tools)

    def complete_stream(self, messages, api_key=''):
        return self.model.complete_stream(messages=messages, api_key=api_key)

class ChatGPT:

    def __init__(self, model):
        self.model = model

    def complete(self, messages, api_key, tools=None):
        if tools is not None:
            print("WARNING, tools are not supported with this model type, ignoring tools parameter.")

        client = OpenAI(api_key=api_key)
        response = client.chat.completions.create(
            model=self.model,
            messages=messages
        )
        return response.choices[0].message.content

    def complete_stream(self, messages, api_key):
        # TODO implement streaming
        print("WARNING, streaming support for this model type has not been implemented yet in UCE, falling back to non-streaming completion.")
        return self.complete(messages, api_key)

class CausalLM:

    def __init__(self, model_name):
        self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(model_name).to(self.device)

    def complete(self, messages, api_key='', tools=None):
        if tools is not None:
            print("WARNING, tools are not supported with this model type, ignoring tools parameter.")

        if isinstance(messages, str):  # Handle raw string inputs
            messages = [{"role": "user", "content": messages}]
        inputs = self.tokenizer.apply_chat_template(messages, return_tensors="pt").to(self.device)
        outputs = self.model.generate(inputs, max_new_tokens=128)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)

    def complete_stream(self, messages, api_key):
        # TODO implement streaming
        print("WARNING, streaming support for this model type has not been implemented yet in UCE, falling back to non-streaming completion.")
        return self.complete(messages, api_key)

class OllamaModel:

    def __init__(self, model_name, base_url="http://localhost:11434"):
        self.model_name = model_name
        self.base_url = base_url

    def complete(self, messages, api_key='', tools=None):
        if isinstance(messages, str):
            messages = [{"role": "user", "content": messages}]

        payload = {
            "model": self.model_name,
            "messages": messages,
            "stream": False,
            "options": {
              "num_ctx": 16192,
              "keep_alive": "60m"
            }
        }

        if tools is not None:
            payload["tools"] = tools

        response = requests.post(
            f"{self.base_url}/api/chat",
            json=payload
        )

        if response.status_code == 200:
            # if we provided tools, we return the full output to allow UCW to act on it
            if tools is not None:
                return response.json()
            # else we return just the content of the message to stay backwards compatible
            # TODO we might want to return more info here later
            return response.json()["message"]["content"]
        else:
            raise Exception(f"Ollama error: {response.status_code} - {response.text}")

    # TODO maybe combine with "complete" method later?
    # NOTE tools not supported with streaming, check later https://ollama.com/blog/streaming-tool
    def complete_stream(self, messages, api_key=''):
        if isinstance(messages, str):
            messages = [{"role": "user", "content": messages}]

        response = requests.post(
            f"{self.base_url}/api/chat",
            json={
                "model": self.model_name,
                "messages": messages,
                "stream": True,
                "options": {
                  "num_ctx": 16192,
                  "keep_alive": "60m"
                }
            },
            stream=True
        )

        if response.status_code != 200:
            raise Exception(f"Ollama error: {response.status_code} - {response.text}")

        # TODO handle errors in streaming response?
        for line in response.iter_lines():
            if line:
                yield line + b'\n'  # forward as-is

if __name__ == "__main__":
    messages = [
        {
            "role": "user",
            "content": "who are you?",
        }
    ]

    llm = OllamaModel("gemma3:4b", "http://geltlin.hucompute.org:12441")
    for r in llm.complete_stream(messages):
        print(r)
