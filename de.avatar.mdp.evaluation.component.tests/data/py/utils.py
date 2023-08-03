from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from nltk.tokenize import word_tokenize

import numpy as np

from sklearn.base import TransformerMixin

import string
import stringutils
import string_utils.validation as suv

from scipy.spatial import distance

import json, codecs
from datetime import datetime  
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
        #print(self.dimensions)

    def fit(self, X, y):
        return self

    #This gives you the glove vectors if the word is present in the glove vocaboulary, otherwise it gives you a 0s vector
    def transform(self, X):
        #X should be a series of lists of tokens
        #for words in X:
         #   print("------")
          #  print(words)
           # for w in words:
            #    print("word : " + w) 
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

#Compute the minimal distance between a test vectorized document and the training docs which all belong to the same category
def computeMinDist(vectorized_test_doc, vectorized_train_set):
    minDist = 99999.
    for v in vectorized_train_set:
        dist = distance.cosine(v, vectorized_test_doc)
        if dist < minDist:
            minDist = dist
        if dist < 0:
            print("Distance is < 0!! That should not happen!!")
    return minDist

# Given a set of test vectorized docs, their true labels and the set of vectorized train data, compute the vectors of min cosine dist 
def computeDistances(features_test, test_labels, features):
    distances_right = []
    distances_wrong = []
    i = 0
    for v in features_test:
        label = test_labels[i]
        if label == 1:
            distances_right.append(computeMinDist(v, features))
        if label == -1:
            distances_wrong.append(computeMinDist(v, features))
        i = i+1
    return [distances_right, distances_wrong]

#Compute precision and recall given the vectors of distances from the train set of the privacy related test and no-related test 
def computeAccuracyMatrix(distances_right, distances_wrong, threshold):
    accuracy_matrix = []
    num_true_positive = 0.
    num_true_negative = 0.
    num_false_positive = 0.
    num_false_negative = 0.
    for d in distances_right:
        if d < threshold:
            num_true_positive = num_true_positive + 1
        else:
            num_false_negative = num_false_negative + 1
    for d in distances_wrong:
        if d < threshold:
            num_false_positive = num_false_positive + 1
        else:
            num_true_negative = num_true_negative + 1
    precision = num_true_positive/(num_true_positive+num_false_positive)
    recall = num_true_positive/(num_true_positive+num_false_negative)
    accuracy = (num_true_positive+num_true_negative)/(num_true_positive+num_false_positive+num_true_negative+num_false_negative)
    #print("Precision: " + str(precision))
    #print("Recall: " + str(recall))
    #print("Accuracy: " + str(accuracy))
    accuracy_matrix.append(precision)
    accuracy_matrix.append(recall)
    accuracy_matrix.append(accuracy)
    return accuracy_matrix

# Finds the optimal threshold to discriminate data based on a certain vector distance
def findOptimalThreshold(distances_right, distances_wrong):
    maxAccuracy = 0.
    bestThreshold = 0.
    for t in range(1, 101):
        threshold = t*0.01
        accuracy_matrix = computeAccuracyMatrix(distances_right, distances_wrong, threshold)
        if accuracy_matrix[2] >= maxAccuracy:
            maxAccuracy = accuracy_matrix[2]
            bestThreshold = threshold
    return bestThreshold

def isDistBelowThreshold(dist, threshold):
    if dist < threshold:
        return True
    return False

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
    json.dump(dict, codecs.open(file_path, 'w', encoding='utf-8'), indent = 4, sort_keys = True)

def createGloVeDict(file_path):
    glove_dict = {}
    with open(file_path,'rb') as f:
        for line in f:
            values = line.split()
            word = values[0]
            vector = np.asarray(values[1:], "float32")
            glove_dict[word] = vector
    return glove_dict

# Convert the n-dimensional array of the glove dict values into lists and the bytes keys into string, so the json library can store it
def adjustDictForStoring(dict):
    dict_storing = {}
    for word, vector in dict.items():
        dict_storing[word.decode("utf-8")] = fromVectorToList(vector)
    return dict_storing

def computePredictions(test_set, vectorized_test, vectorized_train, loaded_threshold):
    predictions = {}
    i = 0
    for doc in vectorized_test:
        minDist = computeMinDist(doc, vectorized_train)
        original_doc = test_set[i]
        predictions[original_doc] = isDistBelowThreshold(minDist, loaded_threshold)
        i = i+1
    return predictions
    
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
