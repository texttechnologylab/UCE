The heart of UCE is its webportal, which, alongside the [Postgresql](https://www.postgresql.org/) database, are the primary microservices we will setup first, as these services are obligatory.

!!! warning "Webportal UI Bug"
    Currently, there is an unresolved issue on **some** Linux distributions with **some** Firefox versions. The bug causes the page to reload in a loop, preventing any user interaction. If you encounter this issue, try switching to a browser other than Firefox. Again, this error has only been observed on certain Linux systems and firefox versions and we are working on resolving the issue.

<hr/>

## User Setup

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

- <sup>`UCE_CONFIG_PATH`: The local path to the [UceConfig](./configuration.md) that injects personal customization into the UCE instance. If none is given, UCE will fallback to a default configuration.</sup>
- <sup>`JVM_ARGS`: Only relevant if the [Sparql microservice](./fuseki-sparql.md) is added. [Specifies the maximum size](https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/java.html), in bytes, of the memory allocation pool for the service.</sup>
- <sup>`TDB2_DATA/ENDPOINT`: Only relevant if the [Sparql microservice](./fuseki-sparql.md) is added. The local path to a [TDB2 database](https://jena.apache.org/documentation/tdb2/) and the name of the endpoint, this database will be exposed to (e.g.: */uce-ontology*).</sup>
- <sup>`IMPORTER_THREADS`: The amount of parallel threads used by the [Importer](./corpus-importer.md) to import the corpora *(standard values are 4, 8 or 16, depending on your setup)*.</sup>


Start the relevant docker containers:

```
docker-compose up --build uce-postgresql-db uce-web
```

The web instance, by default, is reachable under: `http://localhost:8008`.

!!! bug "Problems?" 
    If the webportal container isn't working, it most likely can't connect to the database. In that case, you can check the connection strings within the `common.conf` file in the source code. For the docker setup, the content of this file should match the `common-release.conf`, which should again match the exposed ports in the `docker-compose.yaml`.

<hr />

## Developer Setup

!!! note "Developer Code"
    Please refer to the [Developer Code](./../development/developer-code.md) for details on how to correctly develop UCE.

Clone the UCE repo and switch to the `develop` branch:

```
git clone https://github.com/texttechnologylab/UCE.git
git fetch --all
git checkout origin develop
```

**Before opening** the repo in an IDE of your choice *(but for this documentation, we will always refer to [IntelliJ](https://www.jetbrains.com/de-de/idea/))*, we have to setup the database first.

### Database

To set up the PostgreSQL database, you can either use a Docker image *(refer to [User Setup](#user-setup) to do so via the compose file or simply pull the official [pgvector](https://hub.docker.com/r/pgvector/pgvector/tags?name=pg16&ordering=name) image)* or install the database locally. When [installing it locally](https://www.postgresql.org/download/), you must install the `pgvector` extension, as we configure PostgreSQL to work with high-dimensional embedding vectors for UCE. This requires a manual but simple [installation](https://github.com/pgvector/pgvector).

!!! info "Local Installation"
    If installed locally, you also need to manually create a database called `uce`, with the owner set to `postgres` and the default password set to `1234`. If you adjust any of these parameters, you must also update the corresponding values in the source code's `common.conf`.

    Respectively, when running the container from the official image (*and not UCE's docker-compose*), pass these parameters into the container:
    ```ini
    POSTGRES_DB: uce
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: 1234
    ```

### Web

If the PostgreSQL DB is running, start by opening IntelliJ **from within the `uce.portal` folder** *(not the root of the repo)* and setting up the IDE for the web portal:

!!! note "Setup"
    - Add a new `Application` configuration  
    - UCE is being developed in **Java 21** 

    ??? warning "Missing JDK Version?"
        *(If the Java 21 SDK is missing, you need to install it. IntelliJ offers a build-in way for that through `Menu` -> `Project Structure` -> `Project` -> Open the `SDK dropdown` -> `Download JDK` and download any 21 version.)* 

    - Set `-cp web`  
    - Main class: `org.texttechnologylab.App`  
    - Program arguments can be left empty for now. For a list of potential CLI arguments, refer to the [documentation](#cli-arguments).
    - Maven should automatically download and index the dependencies. If, for some reason, it does not, you can force an update via `mvn clean install -U` *(in IntelliJ, open `Execute Maven Goal`, then enter the command)*.

Now start the web portal. The default URL is `http://localhost:4567` and, if done correctly, the portal will appear with no corpora available. We will now set up the **Corpus-Importer** to import corpora.

!!! bug "Java Version Error?"
    Make sure that IntelliJ's Java compiler is also set to match the target bytecode version 21. Otherwise, startup will result in an error. You can check this via `Settings` → `Build, Execution, Deployment` → `Compiler` → `Java Compiler`.

## CLI Arguments

### Web Portal

| <div style="width:100px">Argument</div> | Description |
|---------|-------------|
| `-cf` <br/> `--configFile` | The local path to the [uceConfig.json](./configuration.md). If started through a Docker container, remember to first mount the local path and then map the `-cf` path to the mounted Docker path. |
| `-cj` <br/> `--configJson` | Instead of passing the UCE configuration through a JSON **file**, you can also directly pass in the JSON. |







