from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from nltk.tokenize import word_tokenize

import numpy as np

from sklearn.base import TransformerMixin

import string
import stringutils
import string_utils.validation as suv

import json, codecs
import pandas as pd


# class for cleaning the text    
class CleanTextTransformer(TransformerMixin):
    def transform(self, X, **transform_params):
        return [cleanText(text) for text in X]
    def transformSingleDoc(self, doc):
        return cleanText(doc)
    def fit(self, X, y=None, **fit_params):
        return self
    def get_params(self, deep=True):
            return {}

# class for tokenize a list of strings, namely our input documents
# Takes a list of strings as input (e.g. ["name", "first name", ...]) and 
# transforms it into a list of tokenized vectors (e.g. [["name"], ["first", "name], ...])
class TextTokenizer(TransformerMixin):
    def fit(self, X, y):
        return self
    def transform(self, X):
        return [word_tokenize(sent) for sent in X]
    def transformSingleDoc(self, doc):
        return word_tokenize(doc)

# class to vectorize words according to a certain dictionary (e.g. GloVe embeddings)
class W2vVectorizer(object):
    def __init__(self, w2v):
        #Takes a dictionary of words and vectors as input
        self.w2v = w2v
        if len(w2v) == 0:
            self.dimensions = 0
        else:
            self.dimensions = len(w2v[next(iter(w2v))])

    def fit(self, X, y):
        return self

    #This gives you the glove vectors if the word is present in the glove vocaboulary, otherwise it gives you a 0s vector
    def transform(self, X):
        #X should be a series of lists of tokens
        return np.array([
            np.mean([self.w2v[w.encode("utf-8")] for w in words if w.encode("utf-8") in self.w2v]
                    or [np.zeros(self.dimensions)], axis=0) for words in X])

# special characters
SYMBOLS = " ".join(string.punctuation).split(" ") + ["-", "...", "”", "”","''"]

# function for cleaning the text 
# firstName -> first name
# first_name -> first name
# the name of the customer -> name customer
def cleanText(text): 
    if suv.is_camel_case(text):
        text = divideVariableName(text)
    text = text.strip().replace("\n", " ").replace("\r", " ")  
    for symbol in SYMBOLS:
        text = text.replace(symbol, " ")
    text = text.lower()
    text = removeStopWords(text)
    textParts = text.split()
    text = ""
    for t in textParts:
        text = text + lemmatize(t) + " "
    text = text.strip()
    return text

stop_words = set(stopwords.words('english'))
def removeStopWords(text):
    word_tokens = word_tokenize(text)
    filtered_sentence = []
    for w in word_tokens:
        if w not in stop_words:
            filtered_sentence.append(w)
    text = ""
    for t in filtered_sentence:
        text = text + " " + t
    return text

#This is to devide a variable name in camelCase in all its parts, e.g firstName -> first name
def divideVariableName(text):
    upperCaseIndexes = []
    textParts = ""
    for c in range(0, len(text)):
        if checkupper(text[c]):
            upperCaseIndexes.append(c)
    start = 0
    for i in upperCaseIndexes:
        part = text[start:i]
        start = i
        textParts = textParts + " " + part.lower()
    part = text[start:len(text)]
    textParts = textParts + " " + part.lower()
    return textParts

def checkupper(str):
   if ord(str) < 96 :
      return True
   return False 

def lemmatize(word):
    lemmatizer = WordNetLemmatizer()
    return lemmatizer.lemmatize(word)

def loadJson(file_path):
    obj_text = codecs.open(file_path, 'r', encoding='utf-8').read()
    return json.loads(obj_text)

def fromVectorToList(vector):
    return vector.tolist()

def fromListToVector(list):
    return np.array(list)

def adjustDictForUsing(loaded_dict):
    dict = {}
    for word in loaded_dict.keys():
        dict[word.encode("utf-8")] = fromListToVector(loaded_dict[word])
    return dict

def saveAsJson(file_path, dict):
    json.dump(dict, codecs.open(file_path, 'w', encoding='utf-8'), indent = 4, sort_keys = False)
    
def getEmbeddingsDict(fileName):
    embeddings_dict={}
    with open(fileName,'rb') as f:
        for line in f:
            values = line.split()
            word = values[0]
            vector = np.asarray(values[1:], "float32")
            embeddings_dict[word] = vector
    return embeddings_dict

# Convert the n-dimensional array of the glove dict values into lists and the bytes keys into string, so the json library can store it
def adjustDictForStoring(dict):
    dict_storing = {}
    for word, vector in dict.items():
        dict_storing[word.decode("utf-8")] = fromVectorToList(vector)
    return dict_storing
    
def createDataFrame(category, docs):
    labels = []
    for d in docs:
        labels.append(category)
    return pd.DataFrame({"label":labels, "text":docs})
    
# Concat 2 data frames, dropping duplicates and resetting the index.
# Duplicates are considered as such if the "text" column is the same
# In case of duplicates the last one is kept
def concatDataFrames(df1, df2):
    return pd.concat([df1, df2]).drop_duplicates(subset="text", keep="last").reset_index(drop=True)
    
def saveDataFrame(fileName, df):
    df.to_excel(fileName, engine="odf", index=False)
