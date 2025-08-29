# UCE Ontology Schema

## UCE Namespace & Specifications

UCE introduces several relations and attributes that are defined below.

- `uce:synonym` connects an entity with all its synonyms as well as alternative or outdated variants. 
- `uce:parent` & `uce:rank` abstract hierarchical order within an ontology. 
- `uce:enriches` connects a new, unique entity with an existing one from the ontology.
  For each name of the original entity, we want to add an "enriched entity" that records the name in an `rdf:label` and
  the name's type with the `rdf:type`.

```ttl
@prefix rdf:      <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:     <http://www.w3.org/2000/01/rdf-schema#> .

# UCE namespace
@prefix uce:      <https://www.texttechnologylab.org/uce-schema#> .

uce:parent a rdfs:Class;
  rdfs:comment "Ontology unspecific hierarchical ordering for entities.";
  rdfs:range rdfs:Class;
  rdfs:domain rdfs:Class .

uce:rank a rdf:Property;
  rdfs:comment "Ontology unspecific rank name.";
  rdfs:range rdfs:Class;
  rdfs:domain rdfs:Literal .

uce:synonym a rdfs:Class;
  rdfs:isDefinedBy <http://www.w3.org/1999/02/22-rdf-syntax-ns#>;
  rdfs:comment "An alternative name of an entity.";
  rdfs:range rdfs:Class;
  rdfs:domain rdfs:Class .

uce:enriches a rdfs:Class;
  rdfs:comment "The subject of the enrichment relation stores the label, label language and type of the object.";
  rdfs:range rdfs:Class;
  rdfs:domain rdfs:Class .
```

## Example: Taxonomic Names from GBIF

If a taxonomic name is matched in an input query, we want to enrich it with all variants,
regardless of their taxonomic status. Thus, we want to find all their `dwc:scientificNames` and `dwc:vernacularNames`, 
including all alternative or outdated names. To facilitate this search effectively, we want to restructure regular 
ontology formats to include `uce:enriched` data. The original ontology data is then no longer required.

### GBIF: Original

The example below shows the (truncated) hierarchy, the entity itself, and a synonym for "Homo sapiens Linnaeus, 1758."
The data itself was converted from the GBIF-DWCA `Taxon.tsv` and `VernacularNames.tsv` files.  

```ttl
@prefix dwc:      <http://rs.tdwg.org/dwc/terms/> .
@prefix gbif:     <https://www.gbif.org/species/> .

# Some upper levels in the hierarchy
gbif:1 a dwc:Taxon;
  dwc:taxonRank "kingdom";
  dwc:scientificName "Animalia" .

gbif:44 a dwc:Taxon;
  dwc:taxonRank "phylum";
  dwc:scientificName "Chordata" .

gbif:359 a dwc:Taxon;
  dwc:taxonRank "class";
  dwc:scientificName "Mammalia" .

gbif:798 a dwc:Taxon;
  dwc:taxonRank "order";
  dwc:scientificName "Primates" .

gbif:5483 a dwc:Taxon;
  dwc:taxonRank "family";
  dwc:scientificName "Hominidae" .

# A (compacted) entry for Homo sapiens (https://www.gbif.org/species/2436436)
gbif:2436436 a dwc:Taxon;
  dwc:scientificName "Homo sapiens Linnaeus, 1758"; # instead of rdfs:label
  dwc:vernacularName "Human"@en, "Humans"@en, "Man"@en, "Mensch"@de;  # from VernacularNames.tsv, the '@xy' suffix indicates the language
  dwc:kingdom gbif:1;
  dwc:phylum gbif:44;
  dwc:class gbif:359;
  dwc:order gbif:798;
  dwc:family gbif:5483;
  dwc:genus gbif:2436435;
  dwc:taxonRank "species";
  dwc:taxonomicStatus "accepted";
  dwc:parentNameUsageID gbif:2436435 .

# A synonym for "Homo sapiens Linnaeus, 1758":
# Homo americanus Bory de St.Vincent, 1825 (https://www.gbif.org/species/4827592)
gbif:4827592 a dwc:Taxon;
  dwc:scientificName "Homo americanus Bory de St.Vincent, 1825";
  dwc:kingdom gbif:1;
  dwc:phylum gbif:44;
  dwc:class gbif:359;
  dwc:order gbif:798;
  dwc:family gbif:5483;
  dwc:genus gbif:2436435;
  dwc:taxonRank "species";
  dwc:taxonomicStatus "synonym";
  dwc:parentNameUsageID gbif:2436435;
  dwc:acceptedNameUsageID gbif:2436436 .  # relation to accepted name
```

### GBIF: For UCE

The hierarchy stays mostly the same, but we replace the DWC-specific relations with abstracted UCE relations.

```ttl
@prefix dwc:      <http://rs.tdwg.org/dwc/terms/> .
@prefix gbif:     <https://www.gbif.org/species/> .

gbif:1 a dwc:Taxon;
  uce:rank "kingdom";
  rdf:label "Animalia" .

gbif:44 a dwc:Taxon;
  uce:rank "phylum";
  rdf:label "Chordata" .

gbif:359 a dwc:Taxon;
  uce:rank "class";
  rdf:label "Mammalia" .

gbif:798 a dwc:Taxon;
  uce:rank "order";
  rdf:label "Primates" .

gbif:5483 a dwc:Taxon;
  uce:rank "family";
  rdf:label "Hominidae" .
```

`uce:parent` abstracts from explicit hierarchical structures by adding parents to each entity.
For taxa, this is equivalent to the taxonomic rank hierarchy as given by `dwc:family`, `dwc:order`, etc.
However, not all entity types have their ancestors enumerated! 
Using `uce:parent`, we define a common interface for all types of ontologies.

Consequently, `uce:rank` abstracts the positioning in finite hierarchies with named levels with common field.

#### Species Entities

The converted species entities make use of compact notation enabled by the abstracted parent relation.
We now only want the full scientific name in the `rdf:label` field.
All other names will be represented by enriched entities. 

```ttl
gbif:2436436 a dwc:Taxon;
  dwc:label "Homo sapiens Linnaeus, 1758";
  uce:rank "species";
  uce:parent gbif:1, gbif:44, gbif:359, gbif:798, gbif:5483, gbif:2436435 .

gbif:4827592 a dwc:Taxon;
  rdf:label "Homo americanus Bory de St.Vincent, 1825";
  uce:rank "species";
  uce:parent gbif:1, gbif:44, gbif:359, gbif:798, gbif:5483, gbif:2436435;
  # Using uce:synonym, we mark the synonym gbif:4827592 (Bory) as a synonym of gbif:2436436 (Linnaeus)
  uce:synonym gbif:2436436 .
```

#### Synonym Relation Symmetry

We also want to add **symmetric relations** for each synonym after the fact, without altering the original definition of `gbif:2436436`.

```ttl
gbif:2436436 uce:synonym gbif:4827592 .
```

#### Enriched Entities

Finally, we add unique "enriched entities." The type of the entities can be ontology-specific. 
Enriched entities can be separate (one per `rdf:label`, `<uce:gbif/2436436/1>` & `<uce:gbif/4827592/1>`)
or aggregated (`<uce:gbif/2436436/2>`).

```ttl
<uce:gbif/2436436/1> uce:enriches gbif:2436436;
  rdf:type dwc:scientificName;
  rdf:label "Homo sapiens" .

<uce:gbif/2436436/2> uce:enriches gbif:2436436;
  rdf:type dwc:vernacularName;
  rdf:label "Human"@en, "Humans"@en, "Man"@en, "Mensch"@de .

<uce:gbif/4827592/1> uce:enriches gbif:4827592;
  rdf:type dwc:scientificName;
  rdf:label "Homo americanus" .
```

The ID of the entities is largely arbitrary, but we recommend retaining the original ID in the new one:
```
<uce:PREFIX/ID/UNIQUE_SUFFIX>
```
where `UNIQUE_SUFFIX` could be just a number or a UUID.

### Validation

You can validate your ontologies by running a local instance of Apache Jena Fuseki:

```
docker run -p 3030:3030 stain/jena-fuseki
```

Opening `localhost:3030` will allow you to upload your ontology TTL file and query it.

Running the following query on the data above should result in:

```sparql
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX uce:  <https://www.texttechnologylab.org/uce-schema#>
PREFIX gbif: <https://www.gbif.org/species/>

SELECT ?enriched ?label WHERE {
  {
    ?enriched uce:enriches gbif:2436436 .
  }
  UNION
  {
    ?enriched uce:enriches ?taxon .
    ?taxon uce:synonym gbif:2436436 .
  }
  OPTIONAL { ?enriched rdf:label ?label .}
}
```

| **enriched**            | **label**       |
| ----------------------- | --------------- |
| 1`<uce:gbif/2436436/2>` | "Humans"@en     |
| 2`<uce:gbif/2436436/2>` | "Human"@en      |
| 3`<uce:gbif/2436436/2>` | "Mensch"@de     |
| 4`<uce:gbif/2436436/2>` | "Man"@en        |
| 5`<uce:gbif/2436436/1>` | Homo sapiens    |
| 6`<uce:gbif/4827592/1>` | Homo americanus |
