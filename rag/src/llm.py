from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
from openai import OpenAI

class InstructLLM:

    def __init__(self, model_name):
        if model_name == "ChatGPT":
            self.model = ChatGPT()
        else:
            # In any other case, we use the huggingface causal llms
            self.model = CausalLM(model_name=model_name)

    def complete(self, messages, api_key=''):
        output = self.model.complete(messages=messages, api_key=api_key)
        return output

class ChatGPT:

    def __init__(self):
        pass

    def complete(self, messages, api_key):
        client = OpenAI(api_key = api_key)
        response = client.chat.completions.create(
            model='gpt-3.5-turbo',
            messages = messages
        )
        return response.choices[0].message.content


class CausalLM:

    def __init__(self, model_name):
        self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
        self.tokenizer  = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(model_name).to(self.device)

    def complete(self, messages, api_key=''):
        messages = [
            {"role": "user", "content": "Was ist Ihr Lieblingsgewürz?"},
            {"role": "assistant", "content": "Nun, ich habe eine Vorliebe für einen guten Spritzer frischen Zitronensaft. Er verleiht allem, was ich in der Küche zubereite, genau die richtige Menge an pikantem Geschmack!"},
            {"role": "user", "content": "Haben Sie Mayonnaise-Rezepte?"}
        ]
        inputs = self.tokenizer.apply_chat_template(messages, return_tensors="pt").to(self.device)
        outputs = self.model.generate(inputs, max_new_tokens=128)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)  


if __name__ == "__main__":
    llm = InstructLLM("mistralai/Mixtral-8x7B-Instruct-v0.1")
    print("Loaded.")
    print(llm.complete("Hi, wie geht es dir?"))