from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

class InstructLLM:

    def __init__(self):
        model_name = "mistralai/Mixtral-8x7B-Instruct-v0.1"
        self.device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
        self.tokenizer  = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(model_name).to(self.device)

    def generate(self, prompt):
        messages = [
            {"role": "user", "content": "Was ist Ihr Lieblingsgewürz?"},
            {"role": "assistant", "content": "Nun, ich habe eine Vorliebe für einen guten Spritzer frischen Zitronensaft. Er verleiht allem, was ich in der Küche zubereite, genau die richtige Menge an pikantem Geschmack!"},
            {"role": "user", "content": "Haben Sie Mayonnaise-Rezepte?"}
        ]
        inputs = self.tokenizer.apply_chat_template(messages, return_tensors="pt").to(self.device)
        outputs = self.model.generate(inputs, max_new_tokens=128)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)


if __name__ == "__main__":
    llm = InstructLLM()
    print("Loaded.")
    print(llm.generate("Hi, wie geht es dir?"))