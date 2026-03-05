#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  dev/build-push-importer.sh <version> [image_repo]

Examples:
  dev/build-push-importer.sh 0.1.2
  dev/build-push-importer.sh 0.1.2 docker.texttechnologylab.org/uce-core-feedback-importer

What it does:
  - Builds the importer image from ./uce.portal/uce.corpus-importer/Dockerfile (clean: --no-cache, --pull)
  - Tags it as <image_repo>:<version>
  - Pushes it to the registry

Notes:
  - Requires a working Docker daemon and that you are logged in to the target registry.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 1
fi

VERSION="$1"
IMAGE_REPO="${2:-docker.texttechnologylab.org/uce-core-feedback-importer}"
IMAGE_TAG="${IMAGE_REPO}:${VERSION}"

if [[ ! "$VERSION" =~ ^[0-9]+(\.[0-9]+){1,3}([.-][A-Za-z0-9]+)?$ ]]; then
  echo "Invalid version: $VERSION" >&2
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f "./uce.portal/uce.corpus-importer/Dockerfile" ]]; then
  echo "Missing Dockerfile: ./uce.portal/uce.corpus-importer/Dockerfile" >&2
  exit 3
fi

echo "Building importer image: ${IMAGE_TAG}"
docker build --pull --no-cache \
  -f ./uce.portal/uce.corpus-importer/Dockerfile \
  -t "${IMAGE_TAG}" \
  .

echo "Pushing importer image: ${IMAGE_TAG}"
docker push "${IMAGE_TAG}"

echo "Done: ${IMAGE_TAG}"
