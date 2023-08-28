import sys
from utils import *
from multilabel_utils import *
from datetime import datetime

base_path = sys.argv[1]

#Load train set
train_file = base_path + 'data/multilabel/train_multilabel.ods'
df_train = pd.read_excel(train_file, engine="odf")

# Load GloVe dictionary and create Glove Vectorizer
glove_dict_file = base_path + 'model/GloVe/glove.6B/glove.6B.50d.txt'
gloVe_50d = getEmbeddingsDict(glove_dict_file)
glove_vectorizer = W2vVectorizer(gloVe_50d)

# Expects a dictionary as a json like {"doc1": {"PERSONAL": "RELEVANT", "MEDICAL": "NOT_RELEVANT"}, "doc2": {"PERSONAL": "POTENTIALLY_RELEVANT", "MEDICAL": "RELEVANT"}}
retrained_docs=json.loads(sys.argv[2])
retrained_data = createRetrainedData(retrained_docs)

# Create new DataFrame and append to the old one
df_retrained = pd.DataFrame(data=retrained_data)
df_concat = concatDataFrames(df_train, df_retrained)

# Retrain the model
X = df_concat["text"] #docs
y = np.asarray(df_concat[df_concat.columns[1:]]) #labels

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=101)
X_train_vectorized = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(X_train)))
X_test_vectorized = glove_vectorizer.transform(TextTokenizer().transform(CleanTextTransformer().transform(X_test)))

clf = MultiOutputClassifier(LogisticRegression()).fit(X_train_vectorized, y_train)

# Save the new model and the new dataframe
timestamp = datetime.now().isoformat()
model_out_file = base_path + 'model/multilabel/multilabel-suggester.pickle'
model_out_file_ts = base_path + 'model/multilabel/multilabel-suggester_'+timestamp+'.pickle'
saveModel(clf, model_out_file)
saveModel(clf, model_out_file_ts)

#Save Model accuracy and Hamming Loss
acc_out_file = base_path + 'model/multilabel/multilabel-performance.json'
acc_out_file_ts = base_path + 'model/multilabel/multilabel-performance_'+timestamp+'.json'
prediction = clf.predict(X_test_vectorized)
performances = {"accuracy_score": accuracy_score(y_test, prediction), "hamming_loss": round(hamming_loss(y_test, prediction),2)}
saveAsJson(acc_out_file, performances)
saveAsJson(acc_out_file_ts, performances)

# Save the data frame
df_out_file_ts = base_path + 'data/multilabel/train_multilabel_'+timestamp+'.ods'
saveDataFrame(train_file, df_concat)
saveDataFrame(df_out_file_ts, df_concat)
