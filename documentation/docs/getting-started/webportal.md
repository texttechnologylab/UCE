The heart of UCE is its webportal, which, alongside the [Postgresql](#TODO) database, are the primary microservices we will setup first. These services are obligatory, all other services are optional.

<hr/>

## Setup

When building from source *(the option to pull finished images will be added soon)*, clone the [UCE repository](https://github.com/texttechnologylab/UCE):

```
git clone https://github.com/texttechnologylab/UCE.git
```

In the root folder, create a `.env` file that holds the variables for the `docker-compose.yaml` file. E.g.:

```ini title=".env"
UCE_CONFIG_PATH=./../uceConfig.json
JVM_ARGS=-Xmx8g
TDB2_DATA=./../tdb2-database
TDB2_ENDPOINT=tdb2-database-name
IMPORTER_THREADS=1
```

- <sup>`UCE_CONFIG_PATH`: The local path to the [UceConfig](#TODO) that injects personal customization into the UCE instance. If none is given, UCE will fallback to a default configuration.</sup>
- <sup>`JVM_ARGS`: Only relevant if the [Sparql microservice](#TODO) is added. [Specifies the maximum size](https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/java.html), in bytes, of the memory allocation pool for the service.</sup>
- <sup>`TDB2_DATA/ENDPOINT`: Only relevant if the [Sparql microservice](#TODO) is added. The local path to a [TDB2 database](https://jena.apache.org/documentation/tdb2/) and the name of the endpoint, this database will be exposed to (e.g.: */uce-ontology*).</sup>
- <sup>`IMPORTER_THREADS`: The amount of parallel threads used by the [Importer](#TODO) to import the corpora *(standard values are 4, 8 or 16, depending on your setup)*.</sup>


Start the relevant docker containers:

```
docker-compose up --build uce-postgresql-db uce-web
```

The web instance, by deafult, is reachable under: `http://localhost:8008`.


!!! bug "Error?" 
    If the webportal container isn't working, it most likely can't connect to the database. In that case, you can check the connection strings within the `common.conf` file in the source code. For the docker setup, the content of this file should match the `common-release.conf`, which should again match the exposed ports in the `docker-compose.yaml`.
