

```json title="corpusConfig.json"
{
  "name": "Corpus_Name",
  "author": "University Doe",
  "language": "de-DE/en-EN/...",
  "description": "The corpus was gathered as part of the John Doe project.",
  "addToExistingCorpus": true,

  "annotations": {
	"annotatorMetadata": false,

    "OCRPage": false,
    "OCRParagraph": false,
    "OCRBlock": false,
    "OCRLine": false,

    "srLink": false,
    "lemma": false,
    "namedEntity": false,
    "sentence": false,
    "taxon": {
      "annotated": false,
      "//comment": "[Are the taxons annotated with biofid onthologies through the 'identifier' property?]",
      "biofidOnthologyAnnotated": false
    },
    "time": false,
    "wikipediaLink": false
  },
  "other": {
    "//comment": "[Is this corpus also available on https://sammlungen.ub.uni-frankfurt.de/? Either true or false]",
    "availableOnFrankfurtUniversityCollection": false,

    "includeTopicDistribution": false,
    "enableEmbeddings": false,
    "enableRAGBot": false
  }
}
```