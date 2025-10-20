The heart of UCE is its webportal, which, alongside the [Postgresql](https://www.postgresql.org/) database, are the primary microservices we will setup first, as these services are obligatory.

<hr/>

## User Setup

It's always best to build from source, so clone the [UCE repository](https://github.com/texttechnologylab/UCE):

```
git clone https://github.com/texttechnologylab/UCE.git
```

In the root folder, create a `.env` file that holds the variables for the `docker-compose.yaml` file. E.g.:

```ini title=".env"
UCE_CONFIG_PATH=./../uceConfig.json
JAVA_OPTIONS=-Xmx8g -Xms8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication
TDB2_DATA=./../tdb2-database
TDB2_ENDPOINT=tdb2-database-name
IMPORTER_THREADS=1
POSTGRESQL_CONFIG=./database/postgresql.conf
KC_REALM_IMPORT_PATH=./auth/uce-realm.json
KC_ADMIN_USERNAME=admin
KC_ADMIN_PW=admin
```

- <sup>`UCE_CONFIG_PATH`: The local path to the [UceConfig](./configuration.md) file, which injects custom settings into the UCE instance. If none is provided, UCE defaults to a standard configuration.</sup>  
- <sup>`JAVA_OPTIONS`: Relevant only if the [SPARQL microservice](./fuseki-sparql.md) is enabled. Specifies the [maximum size](https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/java.html), in bytes, of the memory allocation pool for the service.</sup>  
- <sup>`TDB2_DATA/ENDPOINT`: Relevant only if the [SPARQL microservice](./fuseki-sparql.md) is enabled. Defines the local path to a [TDB2 database](https://jena.apache.org/documentation/tdb2/) and the name of the endpoint exposed to it (e.g., */uce-ontology*).</sup>  
- <sup>`IMPORTER_THREADS`: The number of parallel threads used by the [Importer](./corpus-importer.md) to process corpus imports. *(Typical values are 4, 8, or 16, depending on your system setup.)*</sup>  
- <sup>`POSTGRESQL_CONFIG`: A configuration file that defines PostgreSQL’s resource allocation (e.g., memory, cores, etc.). Since configuration can be extensive, a helpful tool is [pgtune](https://pgtune.leopard.in.ua/).</sup>  
- <sup>`KC_REALM_IMPORT_PATH`: Relevant only if the [Keycloak microservice](./keycloak.md) is enabled. This file contains a predefined realm for Keycloak, providing default configurations for UCE to work out of the box with Keycloak authentication. It is not recommended to edit this JSON file directly; instead, use the Keycloak web portal UI.</sup>  
- <sup>`KC_ADMIN_USERNAME/PW`: Relevant only if the [Keycloak microservice](./keycloak.md) is enabled. Specifies the admin username and password for the Keycloak web UI. You will be prompted to log in when starting the Keycloak server. **Change these values in production!**</sup>


Start the relevant docker containers:

```
docker-compose up --build uce-postgresql-db uce-web
```

The web instance, by default, is reachable under: `http://localhost:8008`. As a User, you can now proceed to import data!

!!! bug "Problems?" 
    If the webportal container isn't working, it most likely can't connect to the database. In that case, you can check the connection strings within the `common-release.conf` file (the `common.conf` file that is used for the docker build) in the source code. The ports and connection urls must match those in the `docker-compose`.


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

To set up the PostgreSQL database, you can either use a Docker image *(refer to [User Setup](#user-setup) to do so via the compose file* or install the database locally. When [installing it locally](https://www.postgresql.org/download/), you must install the `pgvector` extension, as we configure PostgreSQL to work with high-dimensional embedding vectors for UCE. This requires a manual but simple [installation](https://github.com/pgvector/pgvector). Additionally, the [PostGIS](https://postgis.net/documentation/getting_started/) extension is needed, as UCE may need to execute geographic queries. To avoid this manual installation, we recommend using the docker image within UCE's own `docker-compose` file instead.

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
| `-cj` <br/> `--configJson` | Pass in the contents of a `uceConfig.json` as a single json string. |
| `-lex` | Force the full lexicalization of all annotations upon UCE start. Use this, if you feel like the `Lexicon` page within UCE wasn't initialized properly. |







