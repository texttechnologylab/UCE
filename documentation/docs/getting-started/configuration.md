UCE has three different configuration levels and with it, three different config files. These levels are:

- **INSTANCE**  
- **CORPUS**  
- **DEVELOPER**

If you're a user only setting up UCE via Docker, then the `DEVELOPER` level is of no interest to you. In the following, we outline the different configurations and their usage within UCE.

<hr/>

<div class="flexed">
  <h2 class="mt-0 mb-0">UCE Configuration</h2>
  <h5 class="mt-0 mb-0 ml-1">(INSTANCE)</h5>
</div>

UCE is customizable in a variety of ways, including color scheme, corpora identity, metadata, and more. To inject your UCE instance with your configuration, the `uceConfig.json` file exists. Through it, you can model the UCE instance within JSON and then pass that `uceConfig.json` file into the Web Portal through the command line.

*You can copy the example `uceConfig.json` below and create your own configuration from it.*

??? example "uceConfig.json"
    ```json title="uceConfig.json"
    {
      "meta": {
        "name": "John Doe Lab",
        "version": "1.0.0",
        "description": "The John Doe Lab works in the field of finance analysis and, in this context, gathers large amounts of data for their sentiment or entailment tasks. This data is made available through the <b>Finance</b> corpus. Herein, ..."
      },
      "corporate": {
        "team": {
          "description": "The team behind the Finance corpus is part of the <a target='_blank' href='https://www.john-doe-lab.org/'>John Doe Lab</a> of the Doe-University.",
          "members": [
            {
              "name": "Prof. John Doe",
              "role": "Supervisor",
              "description": "Mr. Doe is the supervisor of the lab.",
              "contact": {
                "name": "Prof. Dr. John Doe",
                "email": "doe@doe-university.de",
                "website": "https://john-doe.org/team/john-doe/",
                "address": "Doe-Street 10<br/>11111 Doe"
              },
              "image": "FILE::https://upload.wikimedia.org/wikipedia/commons/9/99/Sample_User_Icon.png"
            },
          ]
        },
        "contact": {
          "name": "John Doe Lab",
          "email": "doe@doe-university.de",
          "website": "https://www.john-doe-lab.org/contact",
          "address": "Doe-Street 10<br/>11111 Doe"
        },
        "website": "https://www.john-doe-lab.org",
        "logo": "FILE::https://upload.wikimedia.org/wikipedia/commons/9/99/Sample_User_Icon.png",
        "name": "John Doe Lab",
        "primaryColor": "#00618f",
        "secondaryColor": "rgba(35, 35, 35, 1)"
      },
      "settings": {
        "rag": {
          "model": "ChatGPT",
          "apiKey": ""
        }
      }
    }
    ```

=== "Meta"

    <table>
      <thead>
        <tr>
          <th>Property</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>name</td>
          <td class="w-100">Name of your project or your lab, shown on the front page of the web portal.</td>
        </tr>
        <tr>
          <td>version</td>
          <td class="w-100">Your personal version counts.</td>
        </tr>
        <tr>
          <td>description</td>
          <td class="w-100">A description shown on the front page of the portal. Use it to describe your UCE instance.</td>
        </tr>
      </tbody>
    </table>

=== "Corporate"

    <table>
      <thead>
        <tr>
          <th>Property</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>team</td>
          <td>Outline and display your team in a dedicated Teams-Tab within your UCE instance.</td>
        </tr>
        <tr>
          <td>team.description</td>
          <td class="w-100">Describe the team working on this project.</td>
        </tr>
        <tr>
          <td>team.members</td>
          <td class="w-100">Create a list of `member-objects` to model your team and each member.</td>
        </tr>
        <tr>
          <td>contact</td>
          <td class="w-100">The contact information is shown in the footer of the webportal. Deposit contact information such as name, website and email for others to contact you through the UCE instance.</td>
        </tr>
        <tr>
          <td>website</td>
          <td class="w-100">The website of your lab or corporation.</td>
        </tr>
        <tr>
          <td>logo</td>
          <td class="w-100">The logo is shown in the top left of the web portal. You can inject the logo via a file path `FILE::{PATH}` *(works with online paths as well)* or directly through Base64-encoded images `BASE64::data:image/png;base64,{BASE64}`.</td>
        </tr>
        <tr>
          <td>name</td>
          <td class="w-100">The name of your lab or corporation.</td>
        </tr>
        <tr>
          <td>primaryColor</td>
          <td class="w-100">Set the primary color for the UCE web portal and model your color scheme.</td>
        </tr>
        <tr>
          <td>secondaryColor</td>
          <td class="w-100">Set the secondary color for the UCE web portal and model your color scheme.</td>
        </tr>
      </tbody>
    </table>

=== "Settings"

    <table>
      <thead>
        <tr>
          <th>Property</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>rag</td>
          <td class="w-100">Set the settings for the RAGbot (if applicable).</td>
        </tr>
        <tr>
          <td>rag.model</td>
          <td class="w-100">The language model that UCE is supposed to power the RAGBot with. Currently, out of the box, only ChatGPT is applicable.</td>
        </tr>
        <tr>
          <td>apiKey</td>
          <td class="w-100">The API key, if the RAGBot utilizes an LLM that is not hosted locally. In case of ChatGPT, for example, fill in your OpenAI API key.</td>
        </tr>
      </tbody>
    </table>

Within the source code, you also find a `defaultUceConfig.json` that you can mirror. This is also the configuration UCE uses if no explicit config is provided. Inject the `defaultUceConfig.json` into the UCE web portal by means of command line arguments, as outlined in [earlier sections](#TODO).

<hr/>

<div class="flexed">
  <h2 class="mt-0 mb-0">Corpus Configuration</h2>
  <h5 class="mt-0 mb-0 ml-1">(CORPUS)</h5>
</div>

As the name suggests, the `corpusConfig.json` holds metadata about a single corpus within UCE. Unlike the `uceConfig.json`, the corpus config is obligatory and needs to be imported by the Corpus-Importer.

*You can copy the example `corpusConfig.json` below and create your own configuration from it.*

??? example "corpusConfig.json"
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

<table>
  <thead>
    <tr>
      <th>Property</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>name</td>
      <td class="w-100">The name assigned to the corpus.</td>
    </tr>
    <tr>
      <td>author</td>
      <td class="w-100">The entity or institution that created the corpus.</td>
    </tr>
    <tr>
      <td>language</td>
      <td class="w-100">Languages included in the corpus, specified in locale format (e.g., "de-DE", "en-EN").</td>
    </tr>
    <tr>
      <td>description</td>
      <td class="w-100">A brief overview of the corpus and its purpose.</td>
    </tr>
    <tr>
      <td>addToExistingCorpus</td>
      <td class="w-100">Boolean flag indicating whether to append this data to an existing corpus *(looks by name)*, or a new corpus should be created.</td>
    </tr>
    <tr>
      <td>annotations</td>
      <td class="w-100">Object outlining how the corpus was annotated.</td>
    </tr>
    <tr>
      <td>annotations.annotatorMetadata</td>
      <td class="w-100">Boolean flag indicating if metadata about the annotator is included.</td>
    </tr>
    <tr>
      <td>annotations.OCRPage</td>
      <td class="w-100">Boolean flag indicating if OCR data at the page level is included.</td>
    </tr>
    <tr>
      <td>annotations.OCRParagraph</td>
      <td class="w-100">Boolean flag indicating if OCR data at the paragraph level is included.</td>
    </tr>
    <tr>
      <td>annotations.OCRBlock</td>
      <td class="w-100">Boolean flag indicating if OCR data at the block level is included.</td>
    </tr>
    <tr>
      <td>annotations.OCRLine</td>
      <td class="w-100">Boolean flag indicating if OCR data at the line level is included.</td>
    </tr>
    <tr>
      <td>annotations.srLink</td>
      <td class="w-100">Boolean flag indicating if semantic role links are annotated.</td>
    </tr>
    <tr>
      <td>annotations.lemma</td>
      <td class="w-100">Boolean flag indicating if lemmatization is performed.</td>
    </tr>
    <tr>
      <td>annotations.namedEntity</td>
      <td class="w-100">Boolean flag indicating if named entities are annotated.</td>
    </tr>
    <tr>
      <td>annotations.sentence</td>
      <td class="w-100">Boolean flag indicating if sentence boundaries are annotated.</td>
    </tr>
    <tr>
      <td>annotations.taxon</td>
      <td class="w-100">Object containing details about taxon annotations.</td>
    </tr>
    <tr>
      <td>annotations.taxon.annotated</td>
      <td class="w-100">Boolean flag indicating if taxons are annotated.</td>
    </tr>
    <tr>
      <td>annotations.taxon.biofidOnthologyAnnotated</td>
      <td class="w-100">Boolean flag indicating if taxons are annotated with biofid ontologies through the 'identifier' property.</td>
    </tr>
    <tr>
      <td>annotations.time</td>
      <td class="w-100">Boolean flag indicating if temporal expressions are annotated.</td>
    </tr>
    <tr>
      <td>annotations.wikipediaLink</td>
      <td class="w-100">Boolean flag indicating if Wikipedia links are included.</td>
    </tr>
    <tr>
      <td>other</td>
      <td class="w-100">Object containing additional properties related to the corpus. <b>The following flags require the setup of the RAG-Service.</b></td>
    </tr>
    <tr>
      <td>other.includeTopicDistribution</td>
      <td class="w-100">Boolean flag indicating if topic distribution data is included. If enabled, the Corpus-Importer will create and cache those upon import.</td>
    </tr>
    <tr>
      <td>other.enableEmbeddings</td>
      <td class="w-100">Boolean flag indicating if embeddings should be enabled. If enabled, the Corpus-Importer will create and cache those upon import.</td>
    </tr>
    <tr>
      <td>other.enableRAGBot</td>
      <td class="w-100">Boolean flag indicating if the RAGBot feature should be enabled.</td>
    </tr>
  </tbody>
</table>

<hr/>

<div class="flexed">
  <h2 class="mt-0 mb-0">Common Configuration</h2>
  <h5 class="mt-0 mb-0 ml-1">(DEVELOPER)</h5>
</div>

In the source code's `uce.common` module, you'll find a `common.conf` file. In it, you can adjust and edit any configurations needed to run the application, such as DB connection strings, API endpoints, and the like. To properly run UCE in a development setting, you need to ensure that all the local connection strings match your setup. For that, the most relevant ones are:

<table>
  <thead>
    <tr>
      <th>Property</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>rag.webserver.base.url</td>
      <td class="w-100">The base url to the RAG-service's webserver *(if setup)*, e.g.: http://localhost:5678/.</td>
    </tr>
    <tr>
      <td>sparql.host</td>
      <td class="w-100">The base url to the Sparql-service's webserver *(if setup)*, e.g.: http://localhost:3030/</td>
    </tr>
    <tr>
      <td>sparql.endpoint</td>
      <td class="w-100">The endpoint of the Sparql-service's webserver, e.g.: my-ontology/sparql </td>
    </tr>
    <tr>
      <td>postgresql.hibernate.connection.url</td>
      <td class="w-100">The connection string to the Postgresql-DB-service, e.g.: jdbc:postgresql://localhost:5433/uce </td>
    </tr>
  </tbody>
</table>

You'll also find two more files, called `common-release.conf` and `common-debug.conf`. Since, for the release, most of the connections differ from the local setup, you can store your local/release config in separate files and copy-paste the needed configuration into `common.conf` depending on the case. **Only the `common.conf` file is used within UCE — the other two are ignored.**

