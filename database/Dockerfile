FROM postgres:16

# Install postgis extension and pgvector extension
RUN apt-get update && apt-get install -y gnupg2 curl ca-certificates lsb-release \
 && echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
 && curl https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
 && apt-get update \
 && apt-get install -y postgis postgresql-16-postgis-3 postgresql-16-pgvector \
 && rm -rf /var/lib/apt/lists/*
