---
pandoc-latex-environment:
  center: [center]


---

# Use Cases Definition

## Use Cases Definition

### Use Cases Definition

\lstset{language=java, style=code-inline}

Two main use-cases can be identified:

1. Given a meta-model (e.g. `person.ecore`) provide warnings and suggestions on fields and/or combination of fields that might constitute a privacy risk under a certain regulation (e.g. GDPR) or that have to obey to certain open data standards;
2. Given an instance of a model (e.g. a `Person` object), check whether the actual data is compliant with the aforementioned standards, and, if not, take the necessary precautions to avoid privacy breaches or other issues.

### Suggester for Meta-Model

+ According to a certain standard (GDPR, open data, etc), the provider of a model should revision it and mark the potentially related attributes, by means of annotations or whatever mechanism will be decided;
+ As violations of such standards often means high fees for an organization, the work should be directed by data security experts, usually appointed within the organization;
+ What we can do is provide a suggester mechanism, to be used as support for the human task, which provides warnings and suggestions on possible fields that might be of interest according to the standard under inquiry.

### Suggester for Meta-Model

+ In order to achieve such a suggestion mechanism, we would need to be able to analyze the attribute names of the models and compare them to a set of previously redacted meta-data, extracted on the basis of the standard under inquiry;
+ Natural Language Processing (NLP) techniques can help in this regard;
+ We will look for solutions that implements such techniques in python and with Lucene.

### Suggester for Meta-Model

+ Once the meta-model has been revised according to a certain standard, a counterpart model can be built, which would consist of a list of concerned attributes under such standard;
+ Other relevant information should be inserted in the counterpart model as well, e.g. information on where the actual data is stored, and whether existing stored data are already compliant with the standard;
+ Other layer of information, depending on the standard itself, can also be added (e.g. for GDPR we would need to know which is the purpose of the data a model wants to store).

### Validator for Instance Model

+ Once we have a counterpart model that contains the relevant information according to a certain standard, we have to check the corresponding instances of the models and validate them according to those standards.