#!/usr/bin/env bash
set -euo pipefail

# Fast local build for UCE Java images with persistent BuildKit cache.
# This keeps Maven dependencies in a reusable local cache to avoid re-downloading.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_ROOT="${ROOT_DIR}/.dev/storage/buildkit"
mkdir -p "${CACHE_ROOT}"

export DOCKER_BUILDKIT=1

build_image() {
  local dockerfile="$1"
  local tag="$2"
  local cache_dir="$3"
  mkdir -p "${cache_dir}"

  docker buildx build \
    --load \
    --file "${dockerfile}" \
    --tag "${tag}" \
    --cache-from "type=local,src=${cache_dir}" \
    --cache-to "type=local,dest=${cache_dir},mode=max" \
    "${ROOT_DIR}"
}

TARGET="${1:-all}"

if [[ "${TARGET}" == "all" || "${TARGET}" == "web" ]]; then
  build_image \
    "${ROOT_DIR}/uce.portal/uce.web/Dockerfile" \
    "uce-web:local-fast" \
    "${CACHE_ROOT}/uce-web"
fi

if [[ "${TARGET}" == "all" || "${TARGET}" == "importer" ]]; then
  build_image \
    "${ROOT_DIR}/uce.portal/uce.corpus-importer/Dockerfile" \
    "uce-importer:local-fast" \
    "${CACHE_ROOT}/uce-importer"
fi

echo "Built images:"
if [[ "${TARGET}" == "all" || "${TARGET}" == "web" ]]; then
  echo "  uce-web:local-fast"
fi
if [[ "${TARGET}" == "all" || "${TARGET}" == "importer" ]]; then
  echo "  uce-importer:local-fast"
fi
echo "BuildKit cache persisted under ${CACHE_ROOT}"
