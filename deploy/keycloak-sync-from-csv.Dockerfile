FROM python:3.12-alpine

WORKDIR /app

COPY deploy/keycloak-sync-from-csv.py /app/keycloak-sync-from-csv.py

ENTRYPOINT ["python", "/app/keycloak-sync-from-csv.py"]

