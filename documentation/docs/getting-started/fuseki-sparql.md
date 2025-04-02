The (Fuseki) Sparql service allows the integration of ontological hierarchies in RDF or OWL format into UCE's searches. It does so through a SPARQL graph database which stores RDF triplets.

!!! info "Out-of-the-box Ontologies"
    Please refer to our list of [plug-and-play Ontologies](#TODO) to see what ontologies have already been made accessible in UCE without you having to  develop anything. We are continuously expanding plug-and-play compatibility with other ontologies, and if you want to incorporate your own compatibility into UCE, feel free to make a [pull request](#TODO) or [get in touch](#TODO)!

<hr/>

## User Setup

For this, the following parameters in the [`.env` file](#TODO) need to be set correctly:

```ini
TDB2_DATA=./../tdb2-database
TDB2_ENDPOINT=tdb2-database-name
```

where `TDB2_DATA` is the local path to your [TDB2 database](https://jena.apache.org/documentation/tdb2/) and `TDB2_ENDPOINT` is the name under which this database will be queryable. This endpoint must match the first part of the `sparql.endpoint` parameter in the `common.conf` file *(`tdb2-database-name/sparql`)*.

Afterwards, simply start the `uce-fuseki-sparql` container:

```
docker-compose up --build uce-fuseki-sparql
```

<hr/>

## Developer Setup

You can easily set up the SPARQL database as outlined in the [User Setup](#user-setup). However, for more efficient testing and to take advantage of the web interface provided by Apache Jena, it may be advisable to install a local Fuseki SPARQL instance as well. For that:

- [Download the latest **Apache Jena Binary Distribution**](https://jena.apache.org/download/).
- Unzip the archive, navigate to the folder, and start the SPARQL server (*requires Java to be installed*):
```
java -Xmx8G -jar fuseki-server.jar --update
```
*`--update` makes the database persistent.*
- By default, the SPARQL database and its web interface are accessible at `http://localhost:3030`.
- You can now create a new database through the Web UI. The name of the database will also serve as the endpoint `/db_name`.

!!! tip "Web UI & Import Option"
    When installing locally and opening the web UI, you can create a new database and use the `Import` button to import RDF and OWL files directly. Once the database has been populated, you will find the corresponding `TDB2` database as a folder within your program directory (likely under `run/databases`). You can simply mount this folder as the `TDB2_DATA` location.

    ??? warning "tdb.lock"
        The database will generate a `tdb.lock` file to prevent multiple instances from accessing the same TDB2 database simultaneously. Be mindful of this, as it often leads to errors when overlooked.

    ??? warning "Disk Space"
        When importing large volumes of new triplets, the SPARQL database generates log and transaction files, which can quickly consume significant disk space and bloat the database. You can **heavily** reduce the size and reclaim unnecessary space by using the [`/compact` endpoint](https://jena.apache.org/documentation/fuseki2/fuseki-server-protocol.html#compact) once your importing is finished.
