---
pandoc-latex-environment:
  center: [center]



---

# Suggester for Privacy-related Model Fields

## Introduction

### Introduction

+ Our first use case is, given a meta-model (e.g. `person.ecore`) provide warnings and suggestions on fields and/or combination of fields that might constitute a privacy risk under a certain regulation (e.g. GDPR) or that have to obey to certain open data standards;
+ We can see the problem as a one-class text classification problem, where our only class is the category of privacy-related terms, while all the other terms have to be identified as outliers;
+ We can then train a model on a set of terms which we consider privacy-related, and then test it over some other examples.

## One-Class Text Classification

### Training Set

+ I built an initial training set, with all the terms I could think of and that may constitute a privacy concern, namely terms which can potentially be associated with personal information, as so, that might result in identifying an individual;
+ The terms are written as I would expect to encounter them as a model field (e.g. "first name" is `firstname`), all in lower case and singular, to avoid useless repetitions.
+ I assigned these terms a *label* `IDENTITY`, meaning that they are terms that might be associated with the identity of an individual.

### Training Set

![](/home/ilenia/Documents/Privacy/summary/OneClassTextClassificationTrainSet.png)

### Test Set

+ For testing I build another set of terms, this time containing both terms that I would expect to be suggested as privacy-related and terms that should have nothing to do with privacy-related matters;
+ I also used some of the terms I put for training, but written in a slightly different manner (plural, snake_case, synonyms);
+ It would be the task of the preprocessing to get rid of such minor differences and of the model itself to identify similar terms as well.

### Test Set

![](/home/ilenia/Documents/Privacy/summary/OneClassTextClassificationTestSet.png)

### Data Pre-processing

+ The input text is first stripped, all the new lines and carriage return characters are removed;
+ Then, if text is in snake_case, it is converted in camelCase;
+ Special symbols are removed;
+ The text is divided into lower case parts based on the camelCase definition (e.g. `firstName -> first, name`);
+ Each part is then lemmatized (e.g. `names -> name` );
+ The parts are then put back together to form just a single term.

### Data Pre-processing

![](/home/ilenia/Documents/Privacy/summary/OneClassTextClassificationDataPreProcess.png)

### The Model

+ Then the data has to be "vectorized", meaning from text we have to pass to a mathematical vector that the algorithm is able to understand;
+ For that I used an `HashingVectorizer` (there are others, so more research here might be needed), which has one hyper-parameter (`n_features`) to set;
+ I then trained a [OneClassSVM](https://scikit-learn.org/stable/modules/generated/sklearn.svm.OneClassSVM.html) model, using the `scikit-learn` python library, which requires other 3 hyper-parameters;
+ I built a function that loops over a set of values for each hyper-parameter, and for each combination fits the model and computes the accuracy on the training set;
+ I then picked the set of hyper-parameters that gave me the best accuracy.

### The Model

![](/home/ilenia/Documents/Privacy/summary/OneClassTextClassificationHyperParams.png)

### The Results

+ After the model was trained I tested it on the test set, and compute precision and recall for both categories (privacy-related terms and non);
+ *Precision* is computed as the ratio between the relevant retrieved instances and all the retrieved instances (e.g. how many privacy-related terms are correctly identified over the total number of terms identified as privacy-related terms);
+ *Recall* is computed as the ratio between the relevant retrieved instances and all the relevant instances (e.g. how many privacy-related terms are correctly identified over the total number of privacy-related terms).

### The Results

|         Category          | Precision | Recall |
| :-----------------------: | :-------: | :----: |
|   Privacy-related terms   |   0.32    |  1.00  |
| NON privacy-related terms |   1.00    |  0.23  |

### The Results

+ All privacy-related terms are properly identified as such (recall 1.0 for that category);
+ Of course, also some non-related terms are mis-identified as privacy-related;
+ This is not too bad, as we just want a suggestion mechanism, so it's better that we find all the relevant ones, plus some more, than the other way around.

### Further Steps

+ There are some additional steps for pre-processing data that I would like to explore (e.g. stemming);
+ I have not spent too much time into the different ways of vectorizing text, so it might be I am not using the fancier one for our purposes;
+ The training set could probably be enriched with more terms.