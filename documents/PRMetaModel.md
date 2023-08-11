---
pandoc-latex-environment:
  center: [center]

---

# Privacy-Related Meta Model

## Introduction

### Where we stand

+ We have an implementation for a model suggester based on GDPR standards;
+ We have an implementation for retraining our suggester after the user has made changes to its results.

### What we need

+ A coupled model, to associate to every evaluated ecore, which contains the evaluation results.

## The PR Meta Model

### The PR Meta Model

::: {.center} 

\includegraphics[scale=0.1]{./images/PRMetaModel.png}

:::

### The PR Meta Model

\lstset{language=java, style=code-inline}

+ The `PRModel` contains a list of `PRPackage`;
+ Every different evaluation (based on different criteria) should trigger the creation of a new `PRPackage`
+ Every `PR` model counterpart has a reference to the actual model element, plus additional info on the evaluation result.

### The PR Meta Model

I built a service that is responsible of operations on the `PRModel`

\lstset{language=java, style=outside-block}

```java
@ProviderType
public interface PRMetaModelService {

	List<PRModel> getPRModelByNsURI(String... modelNsURIs);	
    
	void savePRModel(PRModel prModel);	
    
	PRModel createPRModel(EvaluationSummary evaluationSummary, EPackage ePackage);
}
```

### The PR Meta Model

\lstset{language=java, style=code-inline}

In the Vaadin UI now, we have a `Show` view which displays the content of an `EPackage`, together with appropriate warnings, based on the selected evaluation criteria.

::: {.center} 

\includegraphics[scale=0.08]{./images/PRMetaModelUI.png}

:::

## Next Steps

### Next Steps

+ When a model is registered we could in principle automatically evaluate it using all the implemented evaluation criteria we have (so far only GDPR);
+ In such a way the `PRModel` is immediately created and stored and then every time we need the model, we can load its `PRModel` counterpart instead;
+ Additional next steps have to be discussed.