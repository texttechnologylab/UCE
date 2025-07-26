from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
import requests
from openai import OpenAI

class InstructLLM:

    def __init__(self, model_name, url):
        if model_name.startswith("openai/"):
            self.model = ChatGPT(model_name.replace("openai/", ""))
        elif model_name.startswith("ollama/"):
            self.model = OllamaModel(model_name.replace("ollama/", ""), url)
        else:
            self.model = CausalLM(model_name=model_name)

    def complete(self, messages, api_key=''):
        return self.model.complete(messages=messages, api_key=api_key)

class ChatGPT:

    def __init__(self, model):
        self.model = model

    def complete(self, messages, api_key):
        client = OpenAI(api_key=api_key)
        response = client.chat.completions.create(
            model=self.model,
            messages=messages
        )
        return response.choices[0].message.content

class CausalLM:

    def __init__(self, model_name):
        self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(model_name).to(self.device)

    def complete(self, messages, api_key=''):
        if isinstance(messages, str):  # Handle raw string inputs
            messages = [{"role": "user", "content": messages}]
        inputs = self.tokenizer.apply_chat_template(messages, return_tensors="pt").to(self.device)
        outputs = self.model.generate(inputs, max_new_tokens=128)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)

class OllamaModel:

    def __init__(self, model_name, base_url="http://localhost:11434"):
        self.model_name = model_name
        self.base_url = base_url

    def complete(self, messages, api_key=''):
        if isinstance(messages, str):
            messages = [{"role": "user", "content": messages}]

        response = requests.post(
            f"{self.base_url}/api/chat",
            json={
                "model": self.model_name,
                "messages": messages,
                "stream": False,
                "options": {
                  "num_ctx": 16192,
                  "keep_alive": "60m"
                }
            }
        )

        if response.status_code == 200:
            # TODO we might want to return more info here later
            return response.json()["message"]["content"]
        else:
            raise Exception(f"Ollama error: {response.status_code} - {response.text}")

if __name__ == "__main__":
    llm = InstructLLM("ollama/gemma2:27b")
    print("Loaded.")
    print(llm.complete("Hi, wie geht es dir?"))
