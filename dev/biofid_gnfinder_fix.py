#!/usr/bin/env python3
"""Build a conservative manifest for abbreviated gnfinder taxa that likely need repair.

This script does not rewrite XMI yet. It scans imported corpus files directly, looks for
`gnfinder:Taxon` abbreviations such as `C. muricata`, tries to infer the genus from nearby
verified gnfinder annotations in the same file, and queries GBIF for a suggested expansion.

The output is a JSONL manifest that we can review before deciding how to patch the XMI or
the import pipeline.
"""

from __future__ import annotations

import argparse
import bz2
import gzip
import html
import json
import re
import sys
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


VERIFY_TAG_RE = re.compile(r"<gnfinder:VerifiedTaxon\b([^>]*)/>")
TAXON_TAG_RE = re.compile(r"<gnfinder:Taxon\b([^>]*)/>")
ATTR_RE = re.compile(r'([A-Za-z0-9_:-]+)="(.*?)"')
ABBREV_RE = re.compile(r"^(?P<initial>[A-Z])\.\s+(?P<rest>[A-Za-z][A-Za-z.\-]*(?:\s+[A-Za-z][A-Za-z.\-]*)*)$")


@dataclass
class Annotation:
    begin: int
    end: int
    value: str
    attrs: dict[str, str]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--corpus-root",
        required=True,
        type=Path,
        help="Path to a corpus root containing corpusConfig.json and input/.",
    )
    parser.add_argument(
        "--output",
        required=True,
        type=Path,
        help="Path to write the JSONL manifest.",
    )
    parser.add_argument(
        "--limit-files",
        type=int,
        default=0,
        help="Optional cap on the number of XMI files to scan.",
    )
    parser.add_argument(
        "--gbif-timeout",
        type=float,
        default=20.0,
        help="HTTP timeout in seconds for GBIF requests.",
    )
    return parser.parse_args()


def open_text(path: Path) -> str:
    if path.suffix == ".bz2":
        with bz2.open(path, "rt", encoding="utf-8", errors="replace") as fh:
            return fh.read()
    if path.suffix == ".gz":
        with gzip.open(path, "rt", encoding="utf-8", errors="replace") as fh:
            return fh.read()
    return path.read_text(encoding="utf-8", errors="replace")


def parse_attrs(attr_blob: str) -> dict[str, str]:
    attrs: dict[str, str] = {}
    for key, raw_value in ATTR_RE.findall(attr_blob):
        attrs[key] = html.unescape(raw_value)
    return attrs


def parse_annotations(pattern: re.Pattern[str], xml_text: str) -> list[Annotation]:
    annotations: list[Annotation] = []
    for match in pattern.finditer(xml_text):
        attrs = parse_attrs(match.group(1))
        try:
            begin = int(attrs.get("begin", "-1"))
            end = int(attrs.get("end", "-1"))
        except ValueError:
            continue
        annotations.append(
            Annotation(
                begin=begin,
                end=end,
                value=attrs.get("value", ""),
                attrs=attrs,
            )
        )
    annotations.sort(key=lambda item: (item.begin, item.end))
    return annotations


def normalize_genus_candidate(value: str) -> str | None:
    for token in re.split(r"\s+", value.strip()):
        cleaned = re.sub(r"[^A-Za-z-]", "", token)
        if cleaned and cleaned[0].isupper():
            return cleaned
    return None


def find_nearest_genus(abbrev: Annotation, verified: list[Annotation], initial: str) -> tuple[str | None, str | None]:
    genus_hits: list[tuple[int, str, str]] = []
    for item in verified:
        genus = (
            normalize_genus_candidate(item.attrs.get("matchedCanonicalSimple", ""))
            or normalize_genus_candidate(item.attrs.get("currentName", ""))
            or normalize_genus_candidate(item.value)
        )
        if genus and genus.startswith(initial):
            distance = min(abs(item.begin - abbrev.begin), abs(item.end - abbrev.end))
            source = item.attrs.get("matchedName") or item.attrs.get("currentName") or item.value
            genus_hits.append((distance, genus, source))
    if not genus_hits:
        return None, None
    genus_hits.sort(key=lambda entry: entry[0])
    _, genus, source = genus_hits[0]
    return genus, source


def gbif_json(url: str, timeout: float) -> dict:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "codex-biofid-fix/1.0",
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.load(response)


def query_gbif(name: str, timeout: float) -> dict:
    encoded = urllib.parse.quote(name)
    match_url = f"https://api.gbif.org/v1/species/match?verbose=true&name={encoded}"
    search_url = f"https://api.gbif.org/v1/species/search?q={encoded}&limit=10"
    try:
        match_data = gbif_json(match_url, timeout)
    except Exception as exc:  # noqa: BLE001
        match_data = {"error": str(exc)}
    try:
        search_data = gbif_json(search_url, timeout)
    except Exception as exc:  # noqa: BLE001
        search_data = {"error": str(exc)}

    top_hits = []
    for result in search_data.get("results", [])[:5]:
        top_hits.append(
            {
                "key": result.get("key"),
                "scientificName": result.get("scientificName"),
                "canonicalName": result.get("canonicalName"),
                "rank": result.get("rank"),
                "taxonomicStatus": result.get("taxonomicStatus"),
                "acceptedKey": result.get("acceptedKey"),
                "accepted": result.get("accepted"),
            }
        )

    return {
        "match": {
            "usageKey": match_data.get("usageKey"),
            "scientificName": match_data.get("scientificName"),
            "canonicalName": match_data.get("canonicalName"),
            "rank": match_data.get("rank"),
            "status": match_data.get("status"),
            "matchType": match_data.get("matchType"),
            "confidence": match_data.get("confidence"),
            "note": match_data.get("note"),
            "acceptedUsageKey": match_data.get("acceptedUsageKey"),
        },
        "topSearchHits": top_hits,
    }


def iter_xmi_files(input_dir: Path) -> Iterable[Path]:
    for path in sorted(input_dir.iterdir()):
        if path.is_file() and (
            path.name.endswith(".xmi")
            or path.name.endswith(".xmi.gz")
            or path.name.endswith(".xmi.bz2")
        ):
            yield path


def main() -> int:
    args = parse_args()
    input_dir = args.corpus_root / "input"
    if not input_dir.is_dir():
        print(f"input directory not found: {input_dir}", file=sys.stderr)
        return 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    processed = 0
    matches = 0

    with args.output.open("w", encoding="utf-8") as out:
        for xmi_path in iter_xmi_files(input_dir):
            if args.limit_files and processed >= args.limit_files:
                break
            processed += 1
            xml_text = open_text(xmi_path)
            verified = parse_annotations(VERIFY_TAG_RE, xml_text)
            unresolved = parse_annotations(TAXON_TAG_RE, xml_text)

            for item in unresolved:
                match = ABBREV_RE.match(item.value.strip())
                if not match:
                    continue
                initial = match.group("initial")
                remainder = match.group("rest")
                inferred_genus, genus_source = find_nearest_genus(item, verified, initial)
                expanded = f"{inferred_genus} {remainder}" if inferred_genus else None
                gbif = query_gbif(expanded, args.gbif_timeout) if expanded else None
                record = {
                    "documentId": xmi_path.stem.replace(".xmi", ""),
                    "file": str(xmi_path),
                    "begin": item.begin,
                    "end": item.end,
                    "abbreviation": item.value,
                    "inferredGenus": inferred_genus,
                    "genusSource": genus_source,
                    "expandedCandidate": expanded,
                    "gbif": gbif,
                }
                out.write(json.dumps(record, ensure_ascii=True) + "\n")
                matches += 1

    print(
        json.dumps(
            {
                "filesProcessed": processed,
                "abbreviationCandidates": matches,
                "output": str(args.output),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
