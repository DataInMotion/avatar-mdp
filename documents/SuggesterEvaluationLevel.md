---
pandoc-latex-environment:
  center: [center]

---

# Suggester Evaluation Level

## Introduction

### Introduction

+ So far the model suggester simply identifies whether a document was to considered relevant or not based on the evaluation criteria;
+ Instead, we would need a way of identifying suggestion levels, such as RELEVANT, POTENTIALLY RELEVANT or NOT RELEVANT;
+ We need then to modify the suggestion algorithm accordingly.

## An Upper Bound for Cosine Distance

### The Cosine Distance

+ The criteria to determine whether a document is relevant or not is based on the computation of the *cosine distance* between its vectorized form and the vectorized form of the relevant documents we have in the training set;
+ The minimum distance is taken and compared with a threshold value. If it is lower than the threshold value the document is considered relevant, otherwise not.

### An Upper Bound for Cosine Distance

What we can do is trying to find a reasonable upper limit for the distance in such a way that:

+ if the distance is below the threshold, the document is RELEVANT;
+ if the distance is above the threshold but below the upper limit, the document is POTENTIALLY RELEVANT;
+ if the distance is above the upper limit, the document is NOT RELEVANT.

### An Upper Bound for Cosine Distance

::: {.textandimgenv img="CosDistCumulativeWithUpperBound.png"}

+ We considered the cumulative distribution of the distances for the test set compared to the train set;
+ We took as upper limit the value of the distance which corresponds to 80% of the truly relevant document properly identified.

:::

## Changes to the UI

### Evaluation Mechanism

::: {.textandimgenv img="ModelEvaluationWithCosDistUpperBound.png"}

+ When evaluating a model now, the relevant documents are displayed in red, the potentially relevant in orange and the not relevant in black;
+ The user still has the possibility to adjust all these predictions and retraining the model;
+ All documents saved as relevant or potentially relevant will end up in the new training set as relevant (this might be adjusted at a later point).

:::

### Visualization View

::: {.textandimgenv img="ModelDisplayWithCosDistUpperBound.png"}

+ When a model is loaded the relevant features are displayed in red, the potentially relevant in orange and the not relevant in black.

:::

## Conclusions and Next Steps

### Conclusions and Next Steps

+ We found a mechanism to determine which documents are relevant and to assign them a certain level of relevance;
+ We might want to think of a better way to retrain the model, which immediately takes into account the difference between relevant and potentially relevant;
+ We have to implement the suggestion mechanism for all `EClassifier` (currently we are only doing that for `EClass`).