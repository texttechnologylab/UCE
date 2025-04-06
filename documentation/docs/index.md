<h1 class="centered mb-0">Unified Corpus Explorer</h1>
<hr class="mt-1"/>

<div class="centered">
    <img src="img/logo.png" style="width:200px; text-align:center"/>
</div>

The **Unified Corpus Explorer (UCE)** is a generic NLP application developed by the [Text Technology Lab](https://www.texttechnologylab.org/) that allows the automatic creation of a fully featured portal for your **annotated** corpora. UCE is standardized in the sense that it's dockerized, reusable, and follows strict schemata—one of which is the [UIMA format](https://uima.apache.org/). To import data and set up your own UCE instance, it is required that the data is annotated or at least exists in UIMA format. 

This documentation includes step-by-step tutorials for developers as well as users, and shows easy-to-follow instructions. The easiest scenario—one where the data already exists in UIMA format and simply needs to be set up and imported into the UCE instance—can be done with Docker knowledge only.

*Below you find some running instances of different projects using UCE for their corpora*

<hr/>

## Running UCE Instances

UCE is used by different projects to visualize their corpora and to provide a generic, but flexible webportal for their users. Here we list some of those UCE instances.

| Url        | Project           | Description  |
| ------------- |:-------------:| :-----|
| [URL](http://biofid.uce.texttechnologylab.org/)      | [BIOfid](https://www.biofid.de/de/) | The Specialised Information Service Biodiversity Research (BIOfid) provides access to current and historical biodiversity literature. |
| [URL](http://prismai.uce.texttechnologylab.org/)      | PrismAI      |  A dataset for the systematic detection of AI-generated text, containg both English and German texts from 8 domains, synthesized using state-of-the-art LLMs. |

