import sys
from utils import *
from multilabel_utils import *

model_path = sys.argv[1]
output_name = sys.argv[2]

# Load model
model_file = model_path + 'multilabel/multilabel-suggester.pickle'
model = loadModel(model_file)

# Load labels names
labels_file = model_path + 'multilabel/labels_names.json'
labels_names = np.array(loadJson(labels_file)) 

# Load GloVe dictionary and create Glove Vectorizer
glove_dict_file = model_path + 'GloVe/glove.6B/glove.6B.50d.txt'
gloVe_50d = getEmbeddingsDict(glove_dict_file)
glove_vectorizer = W2vVectorizer(gloVe_50d)

predictionsDict = {}
lower_prob = 0.4
upper_prob = 0.6
#Input is expected as a json dictionary of the form '{"featureName":["doc1","doc2"],"otherFeautureName":["doc1,"doc2"]}'
data = json.loads(sys.argv[3])
for feature_name, docs in data.items():
	vectorized_docs = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(docs)))	
	predictionsDict[feature_name] = computeProbabilities(model, docs, vectorized_docs, labels_names, lower_prob, upper_prob)
	
saveAsJson(output_name, predictionsDict)


