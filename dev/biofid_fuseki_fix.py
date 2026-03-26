#!/usr/bin/env python3
"""Build a GBIF-derived enrichment report for taxon names.

This script is intentionally generic: for each queried scientific name it resolves exact
homonym usages from GBIF, follows accepted usages where needed, and collects synonyms,
subordinate taxa, and vernacular names. The result is a JSON report that we can compare to
Fuseki/TDB contents before deciding how to patch RDF data.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.parse
import urllib.request
from collections import defaultdict
from functools import lru_cache


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", action="append", default=[], help="Scientific name to inspect.")
    parser.add_argument(
        "--names-file",
        help="Optional file with one scientific name per line.",
    )
    parser.add_argument("--output", required=True, help="Path to write the JSON report.")
    parser.add_argument("--timeout", type=float, default=20.0, help="HTTP timeout in seconds.")
    return parser.parse_args()


def normalize_canonical(name: str) -> str:
    return re.sub(r"\s+", " ", name.strip()).casefold()


@lru_cache(maxsize=4096)
def fetch_json(url: str, timeout: float) -> dict:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "codex-biofid-fix/1.0",
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.load(response)


def species_match(name: str, timeout: float) -> dict:
    encoded = urllib.parse.quote(name)
    return fetch_json(f"https://api.gbif.org/v1/species/match?verbose=true&name={encoded}", timeout)


def species_search(name: str, timeout: float) -> dict:
    encoded = urllib.parse.quote(name)
    return fetch_json(f"https://api.gbif.org/v1/species/search?q={encoded}&limit=100", timeout)


def species_detail(key: int, timeout: float) -> dict:
    return fetch_json(f"https://api.gbif.org/v1/species/{key}", timeout)


def species_synonyms(key: int, timeout: float) -> list[dict]:
    return fetch_json(f"https://api.gbif.org/v1/species/{key}/synonyms?limit=500", timeout).get("results", [])


def species_children(key: int, timeout: float) -> list[dict]:
    return fetch_json(f"https://api.gbif.org/v1/species/{key}/children?limit=500", timeout).get("results", [])


def species_vernaculars(key: int, timeout: float) -> list[dict]:
    return fetch_json(f"https://api.gbif.org/v1/species/{key}/vernacularNames?limit=500", timeout).get("results", [])


def summarize_usage(item: dict) -> dict:
    return {
        "key": item.get("key"),
        "scientificName": item.get("scientificName"),
        "canonicalName": item.get("canonicalName"),
        "authorship": item.get("authorship"),
        "rank": item.get("rank"),
        "taxonomicStatus": item.get("taxonomicStatus"),
        "acceptedKey": item.get("acceptedKey"),
        "accepted": item.get("accepted"),
        "numDescendants": item.get("numDescendants"),
    }


def build_report(name: str, timeout: float) -> dict:
    canonical_query = normalize_canonical(name)
    match = species_match(name, timeout)
    search = species_search(name, timeout)

    exact_usages: dict[int, dict] = {}
    fuzzy_or_non_exact: list[dict] = []

    for item in search.get("results", []):
        canonical = normalize_canonical(item.get("canonicalName") or "")
        if canonical == canonical_query:
            key = item.get("key")
            if key is not None and key not in exact_usages:
                exact_usages[key] = summarize_usage(item)
        else:
            fuzzy_or_non_exact.append(summarize_usage(item))

    if match.get("usageKey") and normalize_canonical(match.get("canonicalName") or "") == canonical_query:
        key = int(match["usageKey"])
        if key not in exact_usages:
            exact_usages[key] = summarize_usage(species_detail(key, timeout))

    accepted_usages: dict[int, dict] = {}
    source_to_accepted: dict[int, int] = {}
    for key in sorted(exact_usages):
        detail = species_detail(key, timeout)
        accepted_key = detail.get("acceptedKey") or detail.get("key")
        if accepted_key is None:
            continue
        accepted_key = int(accepted_key)
        source_to_accepted[key] = accepted_key
        if accepted_key not in accepted_usages:
            accepted_usages[accepted_key] = summarize_usage(species_detail(accepted_key, timeout))

    expansion_names: set[str] = set()
    names_by_kind: dict[str, set[str]] = defaultdict(set)
    accepted_synonyms: dict[int, list[dict]] = {}
    accepted_children: dict[int, list[dict]] = {}
    accepted_vernaculars: dict[int, list[dict]] = {}

    for accepted_key in sorted(accepted_usages):
        detail = species_detail(accepted_key, timeout)
        sci = detail.get("scientificName")
        if sci:
            expansion_names.add(sci)
            names_by_kind["acceptedScientificNames"].add(sci)

        synonyms = [summarize_usage(item) for item in species_synonyms(accepted_key, timeout)]
        children = [
            summarize_usage(item)
            for item in species_children(accepted_key, timeout)
            if (item.get("rank") or "").upper() in {"SUBSPECIES", "VARIETY", "FORM", "FORMA"}
        ]
        vernaculars = species_vernaculars(accepted_key, timeout)

        accepted_synonyms[accepted_key] = synonyms
        accepted_children[accepted_key] = children
        accepted_vernaculars[accepted_key] = vernaculars

        for item in synonyms:
            if item.get("scientificName"):
                expansion_names.add(item["scientificName"])
                names_by_kind["synonyms"].add(item["scientificName"])
        for item in children:
            if item.get("scientificName"):
                expansion_names.add(item["scientificName"])
                names_by_kind["subordinateTaxa"].add(item["scientificName"])
        for item in vernaculars:
            vernacular = item.get("vernacularName")
            if vernacular:
                expansion_names.add(vernacular)
                names_by_kind["vernacularNames"].add(vernacular)

    return {
        "query": name,
        "gbifMatch": {
            "usageKey": match.get("usageKey"),
            "scientificName": match.get("scientificName"),
            "canonicalName": match.get("canonicalName"),
            "rank": match.get("rank"),
            "status": match.get("status"),
            "matchType": match.get("matchType"),
            "confidence": match.get("confidence"),
            "acceptedUsageKey": match.get("acceptedUsageKey"),
            "note": match.get("note"),
        },
        "exactCanonicalUsages": [exact_usages[key] for key in sorted(exact_usages)],
        "sourceToAcceptedUsage": source_to_accepted,
        "acceptedUsages": [accepted_usages[key] for key in sorted(accepted_usages)],
        "acceptedSynonyms": accepted_synonyms,
        "acceptedChildren": accepted_children,
        "acceptedVernaculars": accepted_vernaculars,
        "expansionNames": sorted(expansion_names),
        "expansionNamesByKind": {kind: sorted(values) for kind, values in names_by_kind.items()},
        "excludedNonExactSearchHits": fuzzy_or_non_exact[:20],
    }


def load_names(args: argparse.Namespace) -> list[str]:
    names = list(args.name)
    if args.names_file:
        with open(args.names_file, encoding="utf-8") as fh:
            names.extend(line.strip() for line in fh if line.strip())
    names = [name for name in names if name]
    if not names:
        raise ValueError("at least one --name or --names-file entry is required")
    return names


def main() -> int:
    args = parse_args()
    try:
        names = load_names(args)
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1

    report = {
        "queries": [build_report(name, args.timeout) for name in names],
    }
    with open(args.output, "w", encoding="utf-8") as fh:
        json.dump(report, fh, indent=2, ensure_ascii=True)
        fh.write("\n")
    print(json.dumps({"output": args.output, "queries": len(names)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
