<div class="wiki-page container">

    <!-- breadcrumbs -->
    <div class="flexed align-items-end">
        <h4 class="color-prime mr-2 mb-0">Search Documentation</h4>
        <i class="text">(English only)</i>
    </div>

    <hr class="mt-2 mb-4"/>

    <!-- Fulltext search -->
    <div class="mt-4 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="group-box bg-light">
            <div class="flexed align-items-center justify-content-between">
                <h5 class="mb-0"><i class="fas fa-search mr-1 color-prime"></i> Standard Fulltext Search</h5>
                <a class="rounded-a mt-0 mr-0"
                   onclick="$(this).closest('.group-box').find('.expanded-content').toggle(50)"><i
                            class="fas fa-chevron-down"></i></a>
            </div>
            <p class="text block-text mt-2 mb-0">
                The standard full-text search mimics the behavior of common web search tools.
                It follows a set of fundamental rules that users can utilize, as outlined below:
            </p>
            <div class="expanded-content display-none mt-2">
                <ul>
                    <li>
                        <label class="color-secondary font-weight-bold font-italic">"" <i
                                    class="text">(JOIN)</i></label>
                        <div>
                            <p class="text mb-1">Ensures that multi-token phrases remain intact within the search query
                                under
                                all circumstances.
                            </p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 mr-2 rounded mb-0 font-italic bg-default text light-border">
                                    <b class="color-secondary">"</b>Bellis Perennis<b class="color-secondary">"</b>
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">or <i class="text">(OR)</i></label>
                        <div>
                            <p class="text mb-1">
                                The word "or" is interpreted as an OR operator (see <b>|</b> in Pro Mode), meaning the
                                search will return texts
                                containing either the left-hand or right-hand phrase.
                            </p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Germany <b class="color-secondary">or</b> USA
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">- <i class="text">(NOT)</i></label>
                        <div>
                            <p class="text mb-1">The "-" (dash) character produces a NOT operator (see <b>!</b> in Pro
                                Mode).</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Germany <b class="color-secondary"> - </b><i> USA</i>
                                </p>
                            </div>
                        </div>
                    </li>
                </ul>
                <div class="alert alert-warning light-border block-text small-font p-2 mt-2 mb-0" role="alert">
                    For more advanced searches, activate the <b>Pro Mode</b>. It enables the use of the same
                    operators—albeit with a different syntax—along with
                    additional advanced features. But unlike the standard fulltext search,
                    Pro Mode enforces a strict syntax and may produce syntax errors if the query does not conform to its
                    rules.
                </div>
            </div>
        </div>
    </div>

    <!-- Pro-Mode documentation -->
    <div class="mt-2 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="group-box bg-light">
            <div class="flexed align-items-center justify-content-between">
                <h5 class="mb-0"><b class="color-prime xlarge-font">P</b>ro Mode</h5>
                <a class="rounded-a mt-0 mr-0"
                   onclick="$(this).closest('.group-box').find('.expanded-content').toggle(50)"><i
                            class="fas fa-chevron-down"></i></a>
            </div>
            <p class="text block-text mt-2 mb-0">
                Pro Mode allows the use of logical Boolean operators within the search query.
                The available logical operators are listed and explained below. They can be combined and used
                together without restrictions:
            </p>
            <div class="expanded-content display-none mt-2">
                <div class="alert alert-danger block-text small-font p-2" role="alert">
                    Enabling Pro Mode enforces a specific syntax for the search query, which must be strictly followed.
                    Queries that contain syntax errors will result in an error message.
                </div>
                <ul>
                    <li>
                        <label class="color-secondary font-weight-bold font-italic">'' <i
                                    class="text">(JOIN)</i></label>
                        <div>
                            <p class="text mb-1">Ensures that multi-token phrases remain intact within the search query
                                under
                                all circumstances. <span class="text-danger">Multi-token phrases which are not joined will cause a syntax error.</span>
                            </p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Examples:</label>
                                <p class="w-100 p-2 mr-2 rounded mb-0 font-italic bg-default text light-border">
                                    <b class="color-secondary">'</b>Bellis Perennis<b class="color-secondary">'</b>
                                </p>
                                <p class="w-100 p-2 rounded mb-0 bg-default text light-border">
                                    <i>Bellis Perennis</i> <i class="ml-1 mr-1 fas fa-long-arrow-alt-right"></i> <span
                                            class="text-danger">Syntax Error</span>
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">& <i class="text">(AND)</i></label>
                        <div>
                            <p class="text mb-1">Searches for texts that fulfill both left-side and right-side
                                conditions,
                                without requiring them to appear consecutively.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Alpina <b class="color-secondary">&</b> 1900
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">| <i class="text">(OR)</i></label>
                        <div>
                            <p class="text mb-1">Allows multiple terms to be searched, returning results that contain
                                any of
                                the provided terms.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    'Bellis perennis' <b class="color-secondary"> | </b><i> 'Brunella vulgaris'</i>
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">! <i class="text">(NOT)</i></label>
                        <div>
                            <p class="text mb-1">Excludes specific operands from the search results.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Germany & <b class="color-secondary">!</b> USA
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">:* <i
                                    class="text">(PREFIX)</i></label>
                        <div>
                            <p class="text mb-1">Finds all words that start with a specific prefix.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    pere<b class="color-secondary">:*</b>
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic">() <i
                                    class="text">(GROUP)</i></label>
                        <div>
                            <p class="text mb-1">Groups terms together for complex queries.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Alpina & ( Herbst <b class="color-secondary">|</b> Sommer )
                                </p>
                            </div>
                        </div>
                    </li>

                    <li class="mt-2">
                        <label class="color-secondary font-weight-bold font-italic"><*> <i class="text">(FOLLOWED
                                BY)</i></label>
                        <div>
                            <p class="text mb-1">Finds instances where one term is followed by another within a specific
                                range.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Examples:</label>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border mr-2">
                                    April <b class="color-secondary"><-></b> 1902
                                </p>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border mr-2">
                                    April <b class="color-secondary"><10></b> <i>Trinita</i>
                                </p>
                                <p class="w-100 p-2 rounded mb-0 font-italic bg-default text light-border">
                                    Bellis <b class="color-secondary"><1></b> pere:*
                                </p>
                            </div>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Enrichment documentation -->
    <!-- Currently, this documentation is only available for biofid ontologies. -->
    <#if jenaSparqlAlive>
        <div class="mt-2 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
            <div class="group-box bg-light">
                <div class="flexed align-items-center justify-content-between">
                    <h5 class="mb-0"><i class="fas fa-asterisk mr-1 color-prime"></i> Enrichment</h5>
                    <a class="rounded-a mt-0 mr-0"
                       onclick="$(this).closest('.group-box').find('.expanded-content').toggle(50)"><i
                                class="fas fa-chevron-down"></i></a>
                </div>
                <p class="text block-text mt-2 mb-0">
                    The enrichment option enables the integration of ontology data into the search query.
                    When activated, the query is decomposed into its distinct components, and for each component,
                    the underlying ontology is examined for alternative names, synonyms, and taxonomic hierarchy.
                    If relevant alternatives are found, they are incorporated into the query using Pro Mode's syntax.
                </p>
                <div class="alert alert-warning light-border block-text small-font p-2 mt-2 mb-0" role="alert">
                    Requires <b>Pro-Mode</b> to be activated and may impact search performance.
                </div>
                <div class="expanded-content mt-3 display-none">

                    <!-- alternative names -->
                    <div class="mb-4 group-box">
                        <label class="color-secondary font-weight-bold">Alternative Names <i class="text">(Vernacular,
                                Synonym)</i></label>
                        <div>
                            <p class="text mb-1">Examines each token or joined phrase and performs a string comparison
                                to
                                identify any existing alternative names. If alternatives are found, they are
                                incorporated into the query using the logical operators GROUP and OR.</p>
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-2">Example:</label>
                                <p class="w-100 p-2 rounded mb-0 bg-default text light-border">
                                    <i>'Bellis Perennis' & Germany</i>
                                    <i class="fas fa-long-arrow-alt-right ml-1 mr-1"></i>
                                    <span class="color-prime">( 'Bellis Perennis' | 'Daisy' | 'Gänseblümchen' | <i>[...]</i> ) & Germany</span>
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- Commands -->
                    <div class="group-box">
                        <label class="color-secondary font-weight-bold">Enrichment Commands
                            <i class="text">&lt;RANK&gt;::&lt;NAME&gt;</i></label>
                        <div>
                            <p class="text">
                                Enrichment Commands allow users to embed specific commands within the search query using
                                a predefined syntax.
                                These commands are resolved with relevant data for the query.
                                The general syntax is <i>&lt;RANK&gt;::&lt;NAME&gt;</i>, and the available commands are
                                listed below:
                            </p>
                            <ul>
                                <li class="mt-2">
                                    <label class="color-secondary font-weight-bold font-italic">K:: <i
                                                class="text">(Kingdom)</i></label>
                                    <div>
                                        <p class="text mb-1">
                                            Interprets the phrase following <i>K::</i> as a kingdom name and retrieves
                                            all
                                            species belonging to that kingdom, using them as search terms instead.
                                        </p>
                                        <div class="flexed align-items-center">
                                            <label class="mb-0 mr-2">Example:</label>
                                            <p class="w-100 p-2 rounded mb-0 bg-default text light-border">
                                                <i><b class="color-prime">K::</b>Animalia</i>
                                                <i class="fas fa-long-arrow-alt-right ml-1 mr-1"></i>
                                                <span class="color-prime">( 'Ascidia parallelogramma' | 'Corella parallelogramma' | 'Parallel-Seescheide' | <i>[...]</i> )</span>
                                            </p>
                                        </div>
                                        <p class="text mb-2 mt-2">
                                            These commands also work in combination with the logical operators.
                                        </p>
                                        <div class="flexed align-items-center">
                                            <label class="mb-0 mr-2">Example:</label>
                                            <p class="w-100 p-2 rounded mb-0 bg-default text light-border">
                                                <i><b class="color-prime">K::</b>Animalia & Germany</i>
                                                <i class="fas fa-long-arrow-alt-right ml-1 mr-1"></i>
                                                <span class="color-prime">( 'Ascidia parallelogramma' | 'Corella parallelogramma' | <i>[...]</i> ) <b
                                                            class="color-prime">&</b> Germany</span>
                                            </p>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                            <p class="text">
                                This logic applies to all other available commands, which include
                            </p>
                            <ul>
                                <li class="text"><b class="color-secondary">P::</b>'Phylum Name'</li>
                                <li class="text"><b class="color-secondary">C::</b>'Class Name'</li>
                                <li class="text"><b class="color-secondary">O::</b>'Order Name'</li>
                                <li class="text"><b class="color-secondary">F::</b>'Family Name'</li>
                                <li class="text"><b class="color-secondary">G::</b>'Genus Name'</li>
                            </ul>
                            <div class="alert alert-danger light-border block-text small-font p-2 mt-2 mb-0"
                                 role="alert">
                                Depending on the command used, the enriched search query may expand significantly,
                                requiring the search to process more phrases, which can result in slower performance.
                                For certain high-rank commands (Kingdom e.g.), the volume of enriched data may become
                                too large,
                                necessitating a predefined limit. In such cases, not all species within the specified
                                taxonomic ranks are included.
                                To address this, use the <b>Layered Search</b> to first filter the data pool for texts
                                that contain specific taxa.
                                Layered Search supports the same commands but ensures that all identified species are
                                considered, regardless of quantity,
                                as it doesn't operate on string level, but instead uses the pre-processed annotations.
                            </div>
                        </div>
                    </div>


                </div>
            </div>
        </div>
    </#if>

    <!-- Layered Search -->
    <div class="mt-2 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="group-box bg-light">
            <div class="flexed align-items-center justify-content-between">
                <h5 class="mb-0"><i class="fas fa-layer-group mr-1 color-prime"></i> Layered Search</h5>
                <a class="rounded-a mt-0 mr-0"
                   onclick="$(this).closest('.group-box').find('.expanded-content').toggle(50)"><i
                            class="fas fa-chevron-down"></i></a>
            </div>
            <p class="text block-text mt-2 mb-0">
                The Layered Search allows for pre-filtering of the data pool using annotations imported into UCE.
                When applied, all search queries and additional filters will be executed on the pre-filtered pool rather
                than the entire corpus.
            </p>
            <div class="alert alert-warning light-border block-text small-font p-2 mt-2 mb-0" role="alert">
                Requires the selected corpus to contain annotations of type Taxon, Time, or Geoname Location.
                If the corpus lacks any of these annotations, this function will not be available.
            </div>
            <div class="expanded-content mt-3 display-none">

                <!-- Layers -->
                <div class="mb-4 group-box">
                    <label class="color-secondary font-weight-bold">Layers <i class="text">(AND-Filtering)</i></label>
                    <div>
                        <p class="text mb-1">Layers represent different stages of filtering, applied sequentially from top (first layer)
                            to bottom (last layer). The first layer filters the entire corpus, while each subsequent layer applies
                            its filtering criteria to the results of the previous layer.
                            This process is logically equivalent to concatenating filters using an AND operator.
                        </p>
                        <hr class="mt-2 mb-2"/>
                        <p class="text">
                            <span class="text-dark">Example:</span>
                            Two layers:
                            The first layer filters out any texts that do not have "Germany" as an annotated location.
                            The second layer further refines this filtered data pool by selecting only texts that contain the taxon Alpina.
                        </p>
                        <div class="flexed align-items-center mt-2">
                            <label class="mb-0 mr-2">Layer_1:</label>
                            <p class="w-100 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                <i>Germany</i>
                                <i></i>
                                <span class="color-prime">10.000 Hits</span>
                            </p>
                        </div>
                        <div class="flexed align-items-center mt-2">
                            <label class="mb-0 mr-2">Layer_2:</label>
                            <p class="w-100 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                <i>Alpina</i>
                                <i></i>
                                <span class="color-prime">2.000 Hits</span>
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Slots -->
                <div class="mb-4 group-box">
                    <label class="color-secondary font-weight-bold">Slots <i class="text">(OR-Filtering)</i></label>
                    <div>
                        <p class="text mb-1">A slot represents a sub-unit of a layer, allowing a single layer to contain multiple slots.
                            Each slot defines a specific filtering condition based on its type, which corresponds to an annotation.
                            A slot of type <b>Time</b> applies a time-based filter to the data pool.
                            Slots of type <b>Location</b> and <b>Taxon</b> apply location-based and taxonomic filters, respectively.
                            Each slot utilizes the corresponding annotations and supports specific commands, which are listed below.
                            Within a layer, slots are concatenated using a logical OR operator.
                            As a result, a layer with multiple slots increases the likelihood of retrieving a larger data pool.
                        </p>
                        <hr class="mt-2 mb-2"/>
                        <p class="text">
                            <span class="text-dark">Example:</span>
                            Two layers:
                            The first layer contains two slots, forming an OR concatenation.
                            This expands the data pool to include texts that either contain "Germany" or are annotated with a time range between 1800 and 1900.
                            The second layer then further filters this pool by selecting only texts that contain the specified annotated taxon.
                        </p>
                        <div class="flexed mt-2 align-items-center">
                            <label class="mb-0 mr-2">Layer_1:</label>
                            <div class="flexed align-items-center mr-1 w-100">
                                <p class="w-100 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                    <i>Germany</i>
                                </p>
                            </div>
                            <div class="flexed align-items-center ml-1 mr-1 w-100">
                                <p class="w-100 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                    <i>1800-1900</i>
                                </p>
                            </div>
                            <p class="ml-1 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                <i class="color-prime">30.000&nbsp;Hits</i>
                            </p>
                        </div>

                        <div class="flexed align-items-center mt-2">
                            <label class="mb-0 mr-2">Layer_2:</label>
                            <p class="w-100 p-2 flexed justify-content-between rounded mb-0 bg-default text light-border">
                                <i>Alpina</i>
                                <i></i>
                                <span class="color-prime">4.000 Hits</span>
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

