from sklearn.model_selection import train_test_split
from sklearn.multioutput import MultiOutputClassifier
from sklearn.linear_model import LogisticRegression

from sklearn.metrics import accuracy_score
from sklearn.metrics import hamming_loss

import numpy as np

#To save and reload the model
import pickle

def saveModel(model, filename):
    pickle.dump(model, open(filename, "wb"))
	
def loadModel(filename):
    return pickle.load(open(filename, "rb"))
    
# Create the data to pass to pandas for creating the DataFrame with the retrained docs
def createRetrainedData(retrained_docs):
    data = {}
    data["text"] = []
    for key, value in retrained_docs.items():
        data["text"].append(key)
        for k, v in value.items():
            if not k in data:
                data[k] = []
            if v == "RELEVANT" or v == "POTENTIALLY_RELEVANT":
                data[k].append(1)
            else:
                data[k].append(0)
    return data
    
def computeProbabilities(model, docs, vectorized_docs, labels, lower_prob, upper_prob):
    predictions = {}
    for d in range(0, len(vectorized_docs)):
        predictions[docs[d]] = {}
        for l in range(0, len(labels)):
            prob_vector = model.predict_proba(vectorized_docs[d].reshape(1, -1) )[l: (l+1)]
            prob_belong = prob_vector[0][0][1]
            if prob_belong < lower_prob:
                predictions[docs[d]][labels[l]] = "NOT_RELEVANT"
            elif prob_belong < upper_prob:
                predictions[docs[d]][labels[l]] = "POTENTIALLY_RELEVANT"
            else:
                predictions[docs[d]][labels[l]] = "RELEVANT"
    return predictions  
