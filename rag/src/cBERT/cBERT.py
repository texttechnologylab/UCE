import torch
from torch import nn
from transformers import BertTokenizer, BertModel


class cBERT(nn.Module):
    ''''The wrapper class for any custom BERT classifier'''

    def __init__(self, bert_model_name, num_classes):
        super(cBERT, self).__init__()
        self.bert = BertModel.from_pretrained(bert_model_name)
        self.dropout = nn.Dropout(0.1)
        self.fc = nn.Linear(self.bert.config.hidden_size, num_classes)

    def forward(self, input_ids, attention_mask):
            outputs = self.bert(input_ids=input_ids, attention_mask=attention_mask)
            pooled_output = outputs.pooler_output
            x = self.dropout(pooled_output)
            logits = self.fc(x)
            return logits
    

class CCCBERT():
    '''The wrapper class for the CCC-BERT variation of the cBERT model'''

    def __init__(self, model_path):
         # We want to support multiple languages, hence we choose a multilingual BERT 
        bert_model_name = 'google-bert/bert-base-multilingual-cased'
        self.tokenizer = BertTokenizer.from_pretrained(bert_model_name)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = cBERT(bert_model_name, 2).to(self.device)
        self.model.load_state_dict(torch.load(model_path, map_location=self.device))

    def predict_context_needed(self, text, max_length=128):
        '''Predicts whether, given a text, the text woudl require the RAG model to fetch context or not.'''

        self.model.eval()
        encoding = self.tokenizer(text, return_tensors='pt', max_length=max_length, padding='max_length', truncation=True)
        input_ids = encoding['input_ids'].to(self.device)
        attention_mask = encoding['attention_mask'].to(self.device)

        with torch.no_grad():
            outputs = self.model(input_ids=input_ids, attention_mask=attention_mask)
            _, preds = torch.max(outputs, dim=1)
        
        # 1 = context_needed and 0 = context_not_needed
        return preds.item()


if __name__ == '__main__':
    '''For testing the model standalone'''
    model = CCCBERT('./models/cBERT_35k.pth')
    print(model.predict_context_needed('Ich suche Bücher über rote indische Vögel'))