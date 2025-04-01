<hr class="mt-0"/>

UCE is compatible with a variety of annotations, provided they exist within the UIMA format. Within UCE, these annotations are used situationally for features or search enhancements, depending on the annotation.

Below you will find an ever-expanding list of importable and compatible annotations within UCE, ranging from standard Named-Entity annotations to more situational taxon or time annotations. All of these annotations can be generated and annotated within the corpus through the [Docker Unified UIMA Interface](#TODO).

<hr/>

??? success "OCR"
    Since much of the literature has yet to be digitized, UCE provides support for corpora containing documents that have undergone Optical Character Recognition (OCR) extraction. These annotations assist in reconstructing the physical layout of the pages within UCE.

??? success "Sentence"
    Divides the documents into their respective sentences.

??? success "Named-Entity"
    Extracts named entities from a document, categorizing them into four types: organization (ORG), person (PER), location (LOC), and miscellaneous (MISC).

??? success "Lemma, POS & Morphological Features"
    Lemmatization reduces inflected words to their root form. Within UCE, searches are enhanced by considering these root forms.
    
??? success "Semantic Role Labels (SRL)"
    SRL identifies semantic relations between the lexical constituents of a sentence, assigning labels to words or phrases that indicate their semantic roles, such as agent, goal, or result.

??? success "Time"
    Extracts temporal expressions, including time and date formats, from a document, analogous to Named-Entity Recognition tasks.

??? success "Taxon"
    The recognition of unambiguous names of biological entities is referred to as a taxon.

??? success "WikiLinks"
    Maps potential words and phrases to their corresponding Wikidata URLs, facilitating the retrieval and access of additional information.

??? warning "GeoNames"
    The recognition of locations within texts and their annotation with hierarchical data, alternate and historical names, and tagging with unique identifiers. *(Under construction)*