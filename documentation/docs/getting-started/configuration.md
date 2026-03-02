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
        "secondaryColor": "rgba(35, 35, 35, 1)",
        "imprint": "<p>No imprint set.</p>"
      },
      "settings": {
        "rag": {
          "models": [
            {
              "model": "ollama/gemma3:latest",
              "url": "http://your.ollama.server.com/",
              "apiKey": "",
              "displayName": "Gemma3 (4.3B - Google)"
            },
            {
              "model": "openai/o4-mini",
              "url": "",
              "apiKey": "YOUR_OPENAI_API_KEY",
              "displayName": "GPT-4o-mini (OpenAI)"
            }
          ]
        },
      "analysis": {
        "enableAnalysisEngine": false
      },
      "authentication": {
        "isActivated": false,
        "publicUrl": "http://localhost:8080",
        "redirectUrl": "http://localhost:4567/auth"
      },
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
          <td>team.members[]</td>
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
        <tr>
          <td>imprint</td>
          <td class="w-100">Fill in a full HTML page of your imprint which will then be available via button in the footer of your UCE instance.</td>
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
          <td>rag.models[]</td>
          <td class="w-100">A list of supported LLMs that power the RAGBot for the user. All listed models will be available for the user via dropbox.</td>
        </tr>
        <tr>
          <td>rag.models.model</td>
          <td class="w-100">A language model that UCE is supposed to power the RAGBot with. Currently, we support Ollama and OpenAI out of the box, so this name needs to be the actual model's id (<i>e.g. ollama/gemma3:latest* or openai/gpt-4o-mini</i>).
          </td>
        </tr>
        <tr>
          <td>rag.models.url</td>
          <td class="w-100">Needed if a local Ollama server is used. This is the base url to that server which will be used by the RAG Service to communicate with it.</td>
        </tr>
        <tr>
          <td>rag.models.apiKey</td>
          <td class="w-100">Needed if the OpenAI API is used. Fill in your own OpenAI api key that will be used by the RAG Service for communication.</td>
        </tr>
        <tr>
          <td>rag.models.displayName</td>
          <td class="w-100">The name the user sees for this model in the UCE webportal.</td>
        </tr>
      </tbody>
    </table>

=== "Analysis"

    <table>
      <thead>
        <tr>
          <th>Property</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>enableAnalysisEngine</td>
          <td class="w-100">Enable or disable the built-in analysis engine into UCE, which is powered through DUUI.</td>
        </tr>
      </tbody>
    </table>

=== "Authentication"

    <table>
      <thead>
        <tr>
          <th>Property</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>isActivated</td>
          <td class="w-100">Enable or disable the authentication server for UCE, allowing login and user access, which is powered through Keycloak.</td>
        </tr>
        <tr>
          <td>publicUrl</td>
          <td class="w-100">This is the base url under which the Keycloak authentication server is reachable by UCE. If the default url or port was changed of the Keycloak Service, this needs to be adjusted.</td>
        </tr>
        <tr>
          <td>redirectUrl</td>
          <td class="w-100">This is the base url of the running UCE webportal, which is then passed into Keycloak. The latter needs this url for communicating with its client's callbacks, in this case UCE.</td>
        </tr>
      </tbody>
    </table>

Within the source code, you also find a `defaultUceConfig.json` that you can mirror. This is also the configuration UCE uses if no explicit config is provided. Inject the `uceConfig.json` into the UCE web portal by means of command line arguments, as outlined in [earlier sections](./webportal.md).

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
        "uceMetadata": false,
        "logicalLinks": false,

        "OCRPage": false,
        "OCRParagraph": false,
        "OCRBlock": false,
        "OCRLine": false,

        "srLink": false,
        "lemma": false,
        "namedEntity": false,
        "sentence": false,
        "sentiment": false,
        "emotion": false,
        "time": false,
        "geoNames": false,
        "taxon": {
          "annotated": false,
          "//comment": "[Are the taxons annotated with biofid onthologies through the 'identifier' property?]",
          "biofidOnthologyAnnotated": false
        },
        "wikipediaLink": false,
        "completeNegation": false,
        "cue": false,
        "event": false,
        "focus": false,
        "scope": false,
        "xscope": false,
        "unifiedTopic": false
      },
      "other": {
        "//comment": "[Is this corpus also available on https://sammlungen.ub.uni-frankfurt.de/? Either true or false]",
        "availableOnFrankfurtUniversityCollection": false,

        "includeKeywordDistribution": false,
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
      <td class="w-100">Boolean flag indicating whether to append this data to an existing corpus *(looked up by name)*, or whether a new corpus should be created.</td>
    </tr>
    <tr>
      <td>annotations</td>
      <td class="w-100">Object outlining how the corpus was annotated and which annotation layers are available.</td>
    </tr>
    <tr>
      <td>annotations.annotatorMetadata</td>
      <td class="w-100">Boolean flag indicating if metadata about the annotator (e.g., name, date, or tool used) is included.</td>
    </tr>
    <tr>
      <td>annotations.uceMetadata</td>
      <td class="w-100">Boolean flag indicating if metadata per document is included (e.g. publishers, author etc.), which is done through its own UIMA-Typesystem.</td>
    </tr>
    <tr>
      <td>annotations.logicalLinks</td>
      <td class="w-100">Boolean flag indicating if logical or structural links between annotation layers (e.g., reference chains or document relations) are included. This is also done through its own UIMA-Typesystem.</td>
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
      <td class="w-100">Boolean flag indicating if semantic role links (verb-argument structures) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.lemma</td>
      <td class="w-100">Boolean flag indicating if lemmatization (base forms of words) is performed.</td>
    </tr>
    <tr>
      <td>annotations.namedEntity</td>
      <td class="w-100">Boolean flag indicating if named entities (e.g., persons, locations, organizations) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.sentence</td>
      <td class="w-100">Boolean flag indicating if sentence boundaries are annotated.</td>
    </tr>
    <tr>
      <td>annotations.sentiment</td>
      <td class="w-100">Boolean flag indicating if sentiment analysis annotations (positive, neutral, negative) are included.</td>
    </tr>
    <tr>
      <td>annotations.emotion</td>
      <td class="w-100">Boolean flag indicating if emotion annotations (e.g., anger, joy, sadness) are included.</td>
    </tr>
    <tr>
      <td>annotations.time</td>
      <td class="w-100">Boolean flag indicating if temporal expressions (e.g., dates, time spans) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.geoNames</td>
      <td class="w-100">Boolean flag indicating if geographic names are annotated and linked to GeoNames identifiers.</td>
    </tr>
    <tr>
      <td>annotations.taxon</td>
      <td class="w-100">Object containing details about taxon annotations.</td>
    </tr>
    <tr>
      <td>annotations.taxon.annotated</td>
      <td class="w-100">Boolean flag indicating if taxons are annotated in the corpus.</td>
    </tr>
    <tr>
      <td>annotations.taxon.biofidOnthologyAnnotated</td>
      <td class="w-100">Boolean flag indicating if taxons are annotated with BioFID ontologies through the <code>identifier</code> property.</td>
    </tr>
    <tr>
      <td>annotations.wikipediaLink</td>
      <td class="w-100">Boolean flag indicating if Wikipedia links are included for entities or terms.</td>
    </tr>
    <tr>
      <td>annotations.completeNegation</td>
      <td class="w-100">Boolean flag indicating if complete negation structures (negation cues and scopes) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.cue</td>
      <td class="w-100">Boolean flag indicating if linguistic cues (e.g., trigger words for negation or modality) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.event</td>
      <td class="w-100">Boolean flag indicating if event annotations (occurrences, actions, or states) are included.</td>
    </tr>
    <tr>
      <td>annotations.focus</td>
      <td class="w-100">Boolean flag indicating if focus annotations (focus elements or highlighted text segments) are included.</td>
    </tr>
    <tr>
      <td>annotations.scope</td>
      <td class="w-100">Boolean flag indicating if negation or modality scopes are annotated.</td>
    </tr>
    <tr>
      <td>annotations.xscope</td>
      <td class="w-100">Boolean flag indicating if extended scopes (cross-sentence or multi-event) are annotated.</td>
    </tr>
    <tr>
      <td>annotations.unifiedTopic</td>
      <td class="w-100">Boolean flag indicating if unified topic annotations (global thematic categories) are included.</td>
    </tr>
    <tr>
      <td>other</td>
      <td class="w-100">Object containing additional corpus-related properties. <b>The following flags require the setup of the RAG-Service.</b></td>
    </tr>
    <tr>
      <td>other.availableOnFrankfurtUniversityCollection</td>
      <td class="w-100">Boolean flag indicating if the corpus is also available via the <a href="https://sammlungen.ub.uni-frankfurt.de/" target="_blank">Frankfurt University Collection</a>.</td>
    </tr>
    <tr>
      <td>other.includeKeywordDistribution</td>
      <td class="w-100">Boolean flag indicating if keyword distribution data should be generated and cached upon import.</td>
    </tr>
    <tr>
      <td>other.enableEmbeddings</td>
      <td class="w-100">Boolean flag indicating if embeddings (vector representations of texts) should be created and cached upon import.</td>
    </tr>
    <tr>
      <td>other.enableRAGBot</td>
      <td class="w-100">Boolean flag indicating if the RAGBot feature (retrieval-augmented chatbot for corpus interaction) should be enabled.</td>
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
      <td class="w-100">The base url to the RAG-service's webserver (if setup), e.g.: <code>http://localhost:5678/.</code></td>
    </tr>
    <tr>
      <td>sparql.host</td>
      <td class="w-100">The base url to the Sparql-service's webserver (if setup), e.g.: <code>http://localhost:3030/</code></td>
    </tr>
    <tr>
      <td>sparql.endpoint</td>
      <td class="w-100">The endpoint of the Sparql-service's webserver, e.g.: <code>my-ontology/sparql</code> </td>
    </tr>
    <tr>
      <td>postgresql.hibernate.connection.url</td>
      <td class="w-100">The connection string to the Postgresql-DB-service, e.g.: <code>jdbc:postgresql://localhost:5433/uce</code></td>
    </tr>
  </tbody>
</table>

You'll also find one more file, called `common-release.conf`. Since, for the release, most of the connections differ from the local setup (*specifically in the docker compose network*), the `common-release.conf` is used when building UCE with docker and has the same properties as its debug counterpart.

