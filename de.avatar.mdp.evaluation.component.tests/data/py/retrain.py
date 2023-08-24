import sys
from utils import *

base_path = sys.argv[1]
glove_dict_path = base_path + 'glove.6B.50d.dictionary.json'
vectorized_train_path = base_path + 'train.json'
train_df_path = base_path + "train.ods"
test_df_path = base_path + "test.ods"

# Expects a dictionary as a json like {"IDENTITY": ["doc1", "doc2"], "NO-RELATED": ["doc1", "doc2"]}
data=json.loads(sys.argv[2])

# Load current data frames for train and test and concatenate to them the new terms
train_df = pd.read_excel(train_df_path, engine="odf")
test_df = pd.read_excel(test_df_path, engine="odf")

additional_train_df = createDataFrame("IDENTITY", data.get("IDENTITY"))
additional_test_df = createDataFrame("NO-RELATED", data.get("NO-RELATED"))

new_train_df = concatDataFrames(train_df, additional_train_df)
new_test_df = concatDataFrames(test_df, additional_test_df)

# Change labels "IDENTITY" and "NO-RELATED" to numbers 1 and -1 for test and create set of docs

new_test_df['label'] = new_test_df['label'].map({'IDENTITY':1, 'NO-RELATED':-1})

train_text = new_train_df['text'].tolist()
test_text = new_test_df['text'].tolist()
test_label = new_test_df['label'].tolist()

# Load the gloVe dictionary
glove_dict = loadJson(glove_dict_path)
glove_dict = adjustDictForUsing(glove_dict)

# Vectorize train and test
glove_vectorizer = W2vVectorizer(glove_dict)
vectorized_train = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(train_text)))
vectorized_test = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(test_text)))

# Compute cosine distances and determine new best threshold and new upper bound
distances = computeDistances(vectorized_test, test_label, vectorized_train)
threshold = findOptimalThreshold(distances[0], distances[1])
threshold_upper_bound = findDistanceUpperBound(distances[0], 0.8) 
threshold_params = {"threshold": threshold, "upper_bound": threshold_upper_bound}


# Save new model and parameters
timestamp = datetime.now().isoformat()
train_df_out_ts = base_path + "train_"+timestamp+".ods"
train_df_out = base_path + "train.ods"
saveDataFrame(train_df_out_ts, new_train_df)
saveDataFrame(train_df_out, new_train_df)

test_df_out_ts = base_path + "test_"+timestamp+".ods"
test_df_out = base_path + "test.ods"
new_test_df['label'] = new_test_df['label'].map({1:'IDENTITY', -1:'NO-RELATED'})
saveDataFrame(test_df_out_ts, new_test_df)
saveDataFrame(test_df_out, new_test_df)

train_vector_out_ts = base_path + "train_"+timestamp+".json"
train_vector_out = base_path + "train.json"
vectorized_train_storing = [fromVectorToList(vector) for vector in vectorized_train] 
saveAsJson(train_vector_out_ts, vectorized_train_storing)
saveAsJson(train_vector_out, vectorized_train_storing)

threshold_out_ts = base_path + "cos_dist_threshold_params_"+timestamp+".json"
threshold_out = base_path + "cos_dist_threshold_params.json"
saveAsJson(threshold_out_ts, threshold_params)
saveAsJson(threshold_out, threshold_params)
