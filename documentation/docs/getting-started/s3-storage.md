The MinIO storage enables the storage of UIMA XMI files during import, allowing users to download them through UCE. Each file includes the original text along with all annotations in a standardized format.

Additional features leveraging the S3 storage are planned, but currently, it is used solely for downloading files within UCE’s document reader.

<hr/>

For this service, the setup process is the same for both users and developers, as it relies on Docker images.

## Setup

No parameters need to be set in the [`.env` file](./webportal.md). However, if you want to start the service with an existing S3 database, adjust the following line:

```ini
MINIO_STORAGE_DATA=./../minio/data_exports/2025-06-12/minio-backup
```

to match the location of your existing data. This will mount the preexisting storage into the service.

Then, simply start the container:

```
docker-compose up --build uce-minio-storage
```

Upon start, the S3 storage comes with a Web UI under `http://localhost:9000` or `http://localhost:9001`. The default credendtials can be found in the `docker-compose.yaml` 

```ini
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=12345678
```

!!! warning "Exposing"
    If you plan to expose this server outside the Docker Compose network (which is neither recommended nor required for UCE), make sure to change the default admin credentials!
