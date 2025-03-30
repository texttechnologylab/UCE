!!! info "Prerequisites"  
    This section requires that you have already set up the PostgreSQL DB and, preferably, the Web Portal. If not, refer to the respective [documentation](#TODO).

The **Corpus-Importer** is a Java application that transforms and imports UIMA-annotated data from a local path into the UCE environment. Depending on the configuration, it also performs post-processing of the data, such as the creation of embedding spaces.

!!! tip "UIMA"  
    If the data is not yet available in UIMA format, refer to the respective [documentation](#TODO), which also utilizes the [Docker Unified UIMA Interface](#TODO) to transform, process, and **annotate** the data in UIMA format the best way possible.

## Folder Structure

Having set up the database and the web portal *(locally or via docker)*, all that is left to do is to tell the importer where to import from and start it.

For this, the importer **always** requires the following folder structure:

```
📁 corpus_a
│   📄 corpusConfig.json
│
└───📁 input
    │   📄 uima_doc_1.xmi
    │   📄 uima_doc_2.xmi
    │   📄 ...
    │   📄 uima_doc_n.xmi
```

where `corpusConfig.json` holds the metadata, and the `input` folder contains the actual UIMA files for a single corpus. As of now, the importer will recursively walk through the `input` folder, so every `.xmi` file in any subfolder will be considered.

## User Setup

Open the `docker-compose.yaml` file *(if you haven't created the `.env` file yet, see [here](#TODO))* and locate the `uce-importer` service. Within it, mount all local paths to the corpora you want to import using the structure described above, and map them like so: 

```yaml
volumes:
    - "./path/to/my_corpora/corpus_a:/app/input/corpora/corpus_a"
    - "./path/to/other_corpora/corpus_b:/app/input/corpora/corpus_b"
    - "..."
```

*You can mount as many corpora as you like using the same structure. Remember that you can adjust the amount of threads used through the `.env` file.*

Afterwards, simply start the importer through the compose:

```
docker-compose up --build uce-importer
```

The importer logs to both the PostgreSQL database *(tables `uceimport` and `importlog`)* and the local `logs` directory within the container. Both logs also appear in the standard output of the console.

<hr/>

## Developer Setup

asd

