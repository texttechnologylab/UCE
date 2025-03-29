<div align="center">
  <h1><b>U</b>nified <b>C</b>orpus <b>E</b>xplorer</h1>
  <img width="185px" src="https://github.com/user-attachments/assets/9540b1be-a85f-4f6d-b76a-d3b31c29e83a"/>
  <h3>Making UIMA-annotated corpora tangible, searchable and vivid.</h3>
  <hr/>
</div>
<i align="left">
  We introduce the Unified Corpus Explorer (UCE), a standardized, dockerized, and dynamic Natural Language Processing (NLP) application designed for 
  flexible and scalable corpus navigation. Herein, UCE utilizes the UIMA format for NLP annotations as a standardized input, constructing interfaces 
  and features around those annotations while dynamically adapting to the corpora and their extracted annotations.
</i>
<hr/>
<div align="center">
  <a href="https://texttechnologylab.github.io/UCE/"><img src="https://img.shields.io/static/v1?label=&message=Documentation&color=blueviolet&style=for-the-badge&logo=internetarchive" alt="Documentation"></a>
  <!--<a href="http://eval.uce.texttechnologylab.org/"><img src="https://img.shields.io/static/v1?label=&message=Demo&color=orange&style=for-the-badge&logo=abstract" alt="Demo"></a>-->
  <a href="#"><img src="https://img.shields.io/static/v1?label=Languages%3A&message=German|English&color=green&style=for-the-badge" alt="Languages: - German | English"></a>
  <a href="https://www.texttechnologylab.org/team/kevin-boenisch/"><img src="https://img.shields.io/static/v1?label=&message=Text+Technology+Lab&color=informational&style=for-the-badge&logo=buffer" alt="Text Technology Lab"></a>
  <!--<a href="https://ebooks.iospress.nl/doi/10.3233/FAIA230996"> <img src="https://img.shields.io/static/v1?label=Paper%3A&message=IOS+Press&color=important&style=for-the-badge&logo=adobefonts" alt="Paper: - IOS Press"></a>-->
  <br/>
  <br/>
</div>

<div align="center">
  <video src="https://github.com/user-attachments/assets/6911aff1-71a7-4d17-9ffb-d45126cb0ea7" />
</div>

# Running UCE Instances

UCE is used by different projects to visualize their corpora and to provide a generic, but flexible webportal for their users. Here we list some of those UCE instances.

| Url        | Project           | Description  |
| ------------- |:-------------:| :-----|
| [URL](http://biofid.uce.texttechnologylab.org/)      | [BIOfid](https://www.biofid.de/de/) | The Specialised Information Service Biodiversity Research (BIOfid) provides access to current and historical biodiversity literature. |
| [URL](http://prismai.uce.texttechnologylab.org/)      | PrismAI      |  A dataset for the systematic detection of AI-generated text, containg both English and German texts from 8 domains, synthesized using state-of-the-art LLMs. |

# Quick Start

> [!TIP]
> Please consult the documentation page for a more detailled and customizable setup documentation. The `Quick Start` is just that: a short setup guide that sets up a default UCE instance.

## Usage

When building from source, clone this repository:

```
git clone https://github.com/texttechnologylab/UCE.git
```

In the root folder, create a `.env` file that holds the variables for the `docker-compose.yaml` file. Example `.env`:

```
UCE_CONFIG_PATH=./../uceConfig.json
JVM_ARGS=-Xmx8g
TDB2_DATA=./../tdb2-database
TDB2_ENDPOINT=tdb2-database-name
IMPORTER_THREADS=1
```

Start the relevant docker containers:

```
docker-compose up --build uce-postgresql-db uce-web
```

*Optional containers, if applicable to your use-case: **[uce-fuseki-sparql], [uce-rag-service]***

> [!WARNING]  
> If the webportal container can't connect to the database, you can check the connectionstrings within the `common.conf` file. For the docker setup, the content of this file should match the `common-release.conf`.

The web instance, by deafult, is reachable under: http://localhost:8008. If you're looking for a small demo without creating it yourself, please check our [open demo](http://eval.uce.texttechnologylab.org/).

### Import Data

Now that the webportal and database are both running, we will start the **uce-importer** docker container from within the compose to import data. To do so, first:

- Create a folder `choose_any_name` that you can mount into the docker container.
- Create a subfolder `input`. Copy all of your annotated UIMA XMI files that you want to import in there.
- Copy a default `uce.common/src/main/resources/corpusConfig.json` file from the source code and put it into the `choose_any_name` folder.
- Inside the `docker-compose.yaml`, find the `uce-importer` service and mount the `path/to/choose_any_name` to `:/app/input/corpora/choose_any_name` (example can be found within the compose file)
- Finally, start the importer and import your corpus:

```
docker-compose up --build uce-importer
```

> [!IMPORTANT]  
> More information about `corpusConfig.json`, `uceConfig.json`, annotations, enabling the RAGbot and other customizations can be found on the documentation page.

## Development

**We are currently creating a dedicated Documentation Page which will be up soon to explain the configuration in more detail and how you can customize UCE.**

# About

UCE is customizable in terms of annotations imported, corporate identity used, and background information added. It allows the creation of a specific UCE instance for your project, regardless of the domain. It does so by utilizing UIMA-annotated corpora, with the primary tool for creating those being the [Docker Unified UIMA Interface (DUUI)](https://github.com/texttechnologylab/DockerUnifiedUIMAInterface). Hence, you would gather your corpus, use DUUI to annotate whatever you want to annotate, and finally import those annotations into UCE to host them.

## Microservices 

UCE consists of several microservices, each dockerized and utilizing distinct technologies, which is being outlined in the following:

<div align="center">
  <img src="https://github.com/user-attachments/assets/e27f1f00-aa89-4080-a08f-ccc043245d2d" width="500px">
</div>
<br/>

| Microservice               | Description                                                                                                                                                                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **A**: Corpus-Importer | UCE is based on Corpus-Importer, a Java application that reads UIMA-annotated documents from a specified path, along with a corresponding corpus-configuration JSON file. The importer extracts the raw data and the configured annotations, applying its own post-processing to set up the environment, which includes text segmentation, database indexing, keyword extraction, and the creation of various embedding spaces, before finally storing each processed document in a PostgreSQL database (B). </details> |
| **B**: Relational Database | As our primary database, we opted for a relational PostgreSQL database, as UCE requires a structured and standardized database schema that can be extended if necessary. Additionally, its compatibility with the pgvector extension enables efficient vector operations directly within the database engine. This allows us to store high-dimensional vector embeddings within relational data tables while also enabling fast vector operations and searches. </details> |
| **C**: Graph Database | In addition to a relational database (B), UCE utilizes an Apache Jena SPARQL database to incorporate basic semantic searches in the Resource Description Framework (RDF) and Web Ontology Language (OWL) data formats. This integration enables the incorporation of domain-specific ontologies (e.g., biological taxonomy) into the UCE environment, further enriching its search capabilities. </details> |
| **D**: Python Webserver | Within UCE, we also utilize a Python web service to provide an interface to machine learning and AI models, as these are primarily accessible through Python. In this context, the web server facilitates access to the generation of embedding vectors, their dimensionality reduction methods, such as t-SNE and PCA, and the inference of (Large) Language Models. The web server is accessible via a REST API and is utilized by services (A) and (E). </details> |
| **E**: UCE Web Portal | The user interacts with UCE and all of its features through a web portal implemented in Java. This service communicates with all other services except for (B), providing a variety of search methods, visualization features, and different ways to interact with the underlying information units, as outlined in detail in Section 3.2. </details> |

## In Medias Res

Some, but not all of the search and visualization features within UCE:

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/b51db617-6041-4e38-9959-eb440f18bede" /></td>
    <td><img src="https://github.com/user-attachments/assets/7e5d6d09-b6b0-4be0-9c93-5d12b712f2fb" /></td>
  </tr>
  <tr>
    <td colspan="2"><img src="https://github.com/user-attachments/assets/c9a10b81-46f5-40e3-b78b-41b3b2edb4f1" /></td>
  </tr>
  <tr>
    <td colspan="2"><img src="https://github.com/user-attachments/assets/9c72b914-5d74-4113-8e49-4b062b25086d" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/d42adc6f-99b8-49db-b4e5-c98961e28f4d" /></td>
    <td><img src="https://github.com/user-attachments/assets/d6188244-85b3-4fca-a18b-4ea3de020abf" /></td>
  </tr>
</table>

## Annotations

Currently supported annotations within UCE are outlined in the following table:

| **Annotation**                        | **Description**                                                                                                                                                        |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Sentence**                          | Divides the documents into their respective sentences.                                                                                                                 |
| **Named-Entity**                      | Extracts named entities from a document, categorizing them into four types: organization (ORG), person (PER), location (LOC), and miscellaneous (MISC). |                                                  |
| **Lemma and POS**                             | Lemmatization reduces inflected words to their root form. Within UCE, searches are enhanced by considering these root forms.                                           |
| **Semantic Role Labels (SRL)**        | SRL identifies semantic relations between the lexical constituents of a sentence, assigning labels to words or phrases that indicate their semantic roles, such as agent, goal, or result. | 
| **Time**                              | Extracts temporal expressions, including time and date formats, from a document, analogous to Named-Entity Recognition tasks.                                          |
| **Taxon**                             | The recognition of unambiguous names of biological entities is referred to as a taxon.                                                                                 |
| **WikiLinks**                         | Maps potential words and phrases to their corresponding Wikidata URLs, facilitating the retrieval and access of additional information.                                |
| **OCR**                               | Since much of the literature has yet to be digitized, UCE provides support for corpora containing documents that have undergone Optical Character Recognition (OCR) extraction. These annotations assist in reconstructing the physical layout of the pages within UCE. | 

