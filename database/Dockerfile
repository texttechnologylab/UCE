FROM postgres:16

# Install postgis extension and pgvector extension
RUN apt-get update && apt-get install -y --no-install-recommends gnupg2 curl ca-certificates lsb-release \
 && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
    | gpg --dearmor -o /usr/share/keyrings/postgresql.gpg \
 && echo "deb [signed-by=/usr/share/keyrings/postgresql.gpg] http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends postgresql-16-postgis-3 postgresql-16-postgis-3-scripts postgresql-16-pgvector \
 && rm -rf /var/lib/apt/lists/*
