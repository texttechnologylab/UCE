#!/usr/bin/env bash
set -euo pipefail

Importer_NAME=duui-uce-document-importer
Importer_VERSION=0.1.0
DOCKER_REGISTRY="docker.texttechnologylab.org/"

# Go to repo root (two levels up from uce.portal/uce.corpus-importer)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${REPO_ROOT}"

docker build --no-cache \
  -t "${DOCKER_REGISTRY}${Importer_NAME}:${Importer_VERSION}" \
  -f "uce.portal/uce.corpus-importer/DockerFile2" \
  .

# Tag "latest" correctly: docker tag SOURCE TARGET
docker tag \
  "${DOCKER_REGISTRY}${Importer_NAME}:${Importer_VERSION}" \
  "${DOCKER_REGISTRY}${Importer_NAME}:latest"