<div class="video-wrapper">
    <iframe width="1280" height="720" src="https://www.youtube.com/embed/f3kB9pNPjsk?si=xxL39RulhGyFAG3D" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
</div>

The Unified Corpus Explorer is a novel solution for making UIMA-annotated corpora tangible. Herein, UCE stands as a generic interface that, given any corpus and its extracted UIMA-based annotations, makes the underlying data accessible through various features, including:

- Semantic search  
- Visualization
- Chatbot Integration  
- Integration of various UIMA-based annotations  

UCE handles the import of necessary files, sets up a multi-microservice environment, and adapts to the specific needs of each corpus and its annotations. Configuration files enable customization of UCE, including:

- Appearance (e.g., color schemes and corporate identity)
- Selection of active features
- Integration of annotations

Moreover, UCE supports the incorporation of **multiple corpora** within the same instance.

<hr/>

## Architecture

The Unified Corpus Explorer (UCE) is built on a modular microservice infrastructure. Below is an overview of the architectural flow, described from top to bottom:

<div class="dockpanel">
    <div class="left">
        <img src="./../img/architecture.png"/>
    </div>
    <div class="right">
    <h3>📥 Corpus Import and Enrichment</h3>
    <ul>
    <li><strong>Corpus-Importer (A)</strong><br>
        Reads the UIMA-annotated files of the corpus along with its configuration. It also potentially interacts with services <strong>C</strong> and <strong>D</strong> to further enrich and pre-process data.
    </li>
    </ul>
    <hr/>
    <h3>🧠 AI & NLP Access</h3>
    <ul>
    <li><strong>Python Webserver (D)</strong><br>
        Generates high-dimensional embedding spaces on multiple levels. These embeddings are later used by several components, including the encapsulated <strong>Retrieval-Augmented Generation pipeline (RAGBot)</strong>. In general, the webserver serves as a gateway for any kind of NLP or AI technologies that we want to integrate into UCE.
    </li>
    </ul>
    <hr/>
    <h3>🗄️ Data Storage</h3>
    <ul>
    <li><strong>PostgreSQL Database (B)</strong><br>
        Stores the fully processed and enriched documents for downstream access and interaction. Through a vector extension, also serves as a vector database for similarity searches.
    </li>
    <li><strong>SPARQL (C)</strong><br>
        Allows the storage of specific ontologies in RDF or OWL format, which can then be used by UCE for its searches and annotations.
    </li>
    </ul>
    <hr/>
    <h3>🌐 User Access</h3>
    <ul>
    <li><strong>Web Portal (E)</strong><br>
        Provides users access to the data via a range of features.<br>
        Each feature is connected to specific microservices via color-coded wire connections, indicating service dependencies.
    </li>
    </ul>
    <hr/>
    <h3>🧬 Feature-Service Interactions</h3>
    <p>Each feature relies on a specific set of annotations and services, represented by shorthand symbols and wires shown in the system's legend. For example:</p>
    <ul>
    <li><strong>Embedding Search</strong><br>
        Utilizes services <strong>B</strong>, <strong>C</strong>, and <strong>D</strong>.
    </li>
    <li><strong>Document Reader</strong><br>
        Utilizes only service <strong>B</strong>.
    </li>
    </ul>
    <hr/>
    <p>Since UCE is continuously being developed, this architectural outline may change or differ from time to time, including the addition or replacement of features, services, and functions. As such, this serves as a general overview only.
    </p>
    </div>
</div>

<hr/>

## Features

Based on the outlined architecture, UCE provides the following features:

