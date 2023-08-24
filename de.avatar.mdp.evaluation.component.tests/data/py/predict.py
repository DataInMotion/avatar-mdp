import sys
from utils import *

base_path = sys.argv[1]
output_name = sys.argv[2]
glove_dict_path = base_path + 'glove.6B.50d.dictionary.json'
vectorized_train_path = base_path + 'train.json'
cosine_dist_threshold_path = base_path + 'cos_dist_threshold_params.json'

glove_dict = loadJson(glove_dict_path)
glove_dict = adjustDictForUsing(glove_dict)

vectorized_train = loadJson(vectorized_train_path)
vectorized_train = [fromListToVector(list) for list in vectorized_train]

glove_vectorizer = W2vVectorizer(glove_dict)
loaded_threshold_params = loadJson(cosine_dist_threshold_path)

predictionsDict = {}

#Input is expected as a json dictionary of the form '{"featureName":["doc1","doc2"],"otherFeautureName":["doc1,"doc2"]}'
data = json.loads(sys.argv[3])
for feature_name, test_set in data.items():
	print(test_set)
	vectorized_test = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(test_set)))
	predictionsDict[feature_name] = computePredictions(test_set, vectorized_test, vectorized_train, loaded_threshold_params["threshold"], loaded_threshold_params["upper_bound"])
	
saveAsJson(output_name, predictionsDict)

