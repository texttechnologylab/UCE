!!! info "Prerequisites"  
    This section requires that you have already set up the PostgreSQL DB and, preferably, the Web Portal. If not, refer to the respective [documentation](./webportal.md).

The **Corpus-Importer** is a Java application that transforms and imports UIMA-annotated data from a local path into the UCE environment. Depending on the configuration, it also performs post-processing of the data, such as the creation of embedding spaces.

!!! tip "UIMA"  
    If the data is not yet available in UIMA format, refer to the respective [documentation](./uima-duui.md), which also utilizes the [Docker Unified UIMA Interface](./uima-duui.md) to transform, process, and **annotate** the data in UIMA format the best way possible. After having transformed your data, proceed here.

## Folder Structure

Having set up the database and the web portal *(locally or via docker)*, all that is left to do is to tell the importer where to import from and start it.

For this, the importer **always** requires the following folder structure:

``` title="Required Folder Structure"
📁 corpus_a
│   📄 corpusConfig.json
└───📁 input
    │   📄 uima_doc_1.xmi
    │   📄 uima_doc_2.xmi
    │   📄 ...
    │   📄 uima_doc_n.xmi
```

where [`corpusConfig.json`](./configuration.md) holds metadata, and the `input` folder contains the actual UIMA files for a single corpus.

!!! note "Input Structure"
     As of now, the importer will recursively walk through the `input` folder, so every `.xmi` file in any subfolder will be considered.

## User Setup

Open the `docker-compose.yaml` file *(if you haven't created the `.env` file yet, see [here](./webportal.md))* and locate the `uce-importer` service. Within it, mount all local paths to the corpora you want to import using the structure described above, and map them like so: 

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

<hr/>

## Developer Setup

In the source code, identify the module `uce.corpus-importer` and set up your IDE:

!!! note "Setup"
    - Add a new `Application` configuration  
    - UCE is developed in **Java 21**  
    - Set `-cp corpus-importer`  
    - Main class: `org.texttechnologylab.App`  
    - CLI arguments are obligatory:
        * `-src "./path/to/your_corpus/"`
        * `-num 1`
        * `-t 1`
    - Maven should automatically download and index the dependencies. If, for some reason, it does not, you can force an update via `mvn clean install -U` *(in IntelliJ, open `Execute Maven Goal`, then enter the command)*.

Open the `common.conf` file and adjust the database connection parameters to match your database (port, host, etc.). Now start the importer and import your corpus. Refer to [CLI Arguments](#cli-argumenhts) for a full list of possible parameters.

!!! info "Logs"
    The importer logs to both the PostgreSQL database *(tables `uceimport` and `importlog`)* and the local `logs` directory within the container. Both logs also appear in the standard output of the console.

## CLI Arguments

| <div style="width:130px">Argument</div> | Description |
|---------|-------------|
| `-src` <br/> `--importSrc` | The path to the corpus source where the UIMA-annotated files are stored. |
| `-srcDir` <br/> `--importDir` | Unlike `-src`, `-srcDir` is the path to a directory that holds multiple importable `src` paths. The importer will check for folders within this directory, where each folder should be an importable corpus with a corpusConfig.json and its input UIMA-files. Those are then imported. |
| `-num` <br/> `--importerNumber` | When starting multiple importers, assign an id to each instance by counting up from 1 to n **(not relevant as off now, just set it to 1)**. |
| `-t` <br/> `--numThreads` | UCE imports asynchronous. Decide with how many threads, e.g. 4-8-16. By default, this is single threaded. |
