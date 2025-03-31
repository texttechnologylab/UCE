
<hr class="mt-0"/>

In general, UCE currently consists of 5 microservices, each dockerized and orchestrated via Docker Compose to form the application that is UCE. Among these 5 services, some are obligatory *(must-haves)* and some are optional *(specific use cases)*.

<table>
  <thead>
    <tr>
      <th>Service</th>
      <th>Description</th>
      <th>Obligatory</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Web Portal</td>
      <td>The web portal provides access to UCE for the user. It is the heart of UCE, communicating with and orchestrating all other services.</td>
      <td style="text-align:center; font-size:large">✅</td>
    </tr>
    <tr>
      <td>PostgreSQL DB</td>
      <td>The PostgreSQL database is the main database within UCE. It stores all data in a structured way and adds vector support through the pgvector extension.</td>
      <td style="text-align:center; font-size:large">✅</td>
    </tr>
    <tr>
        <td>Corpus Importer</td>
        <td>
            The Importer handles the importing of UIMA-annotated corpora. Given a path, it will load UIMA files and project them into the UCE environment.
            Without the importer, there is currently no other way to get data into UCE 
            <em>(IO REST endpoints in UCE exist, but aren't production-ready yet)</em>.
        </td>
        <td style="text-align:center; font-size:large">✅</td>
    </tr>
    <tr>
      <td>RAG Service</td>
      <td>The RAG service is a Python web server that primarily enables access to machine learning and AI models. It is required to calculate embeddings and enable the RAG bot.</td>
      <td style="text-align:center; font-size:large">❌</td>
    </tr>
    <tr>
        <td>Sparql Service</td>
        <td>The (Fuseki) Sparql service allows the integration of ontological hierarchies in RDF or OWL format into UCE's searches.</td>
        <td style="text-align:center; font-size:large">❌</td>
    </tr>
  </tbody>
</table>

In the following, you will learn how to set up these services and how to get started with your own UCE instance for your data.

!!! info "Structure"
    The following sections are split into two parts:

    - Setting up UCE as a user *(Docker)*. 
    - Setting up UCE as an active developer *(local)*. 

    Depending on your use case, you will either set up the development environment or simply build UCE, import your data, and use it.