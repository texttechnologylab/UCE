<#if analysisId??>
  <div class="mb-3" style="display:flex; align-items:center; gap:8px;">
    <button id="saveCasBtn" data-analysis-id="${analysisId}" class="btn btn-primary">Save CAS</button>
    <label for="corpus-select" class="mb-0">Corpus</label>
    <select id="corpus-select" class="form-control form-control-sm" style="max-width:200px;">
      <option value="1">Corpus 1</option>
      <option value="2">Corpus 2</option>
      <option value="3">Corpus 3</option>
    </select>

  </div>

  <script>
    document.getElementById("saveCasBtn").addEventListener("click", function () {
      const analysisId = this.dataset.analysisId;
      const corpusId = document.getElementById("corpus-select").selectedOptions[0].value;

      fetch(
        "/api/analysis/importCas?analysisId=" + encodeURIComponent(analysisId) +
        "&corpusId=" + encodeURIComponent(corpusId),
        { method: "POST" }
      )
      .then(r => r.text())
      .then(msg => alert("Server response: " + msg))
      .catch(err => alert("Error: " + err));
    });
  </script>
</#if>
        <#if DUUI??>
            <#if DUUI.modelGroups?has_content>
                <#if DUUI.isTopic>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Topic</h6>
                        <div class="analysis-topics-container">
                            <#list DUUI.textInformation.topicAVG as model>
                                <div class="analysis-topic-card">
                                    <div class="analysis-topic-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="analysis-topics-grid">
                                        <#list model.topics as topic>
                                            <#assign opacity = topic.getScore()?string?replace(",", ".")>
                                            <div class="analysis-topic-entry" style="background-color: rgba(0, 200, 200, ${opacity});">
                                                <div class="analysis-topic-score">${topic.getKey()}: ${topic.getScore()}</div>
                                            </div>
                                        </#list>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#if DUUI.isSentiment>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Sentiment</h6>
                        <div class="analysis-sentiment-container">
                            <#list DUUI.textInformation.sentimentAVG as model>
                            <div class="analysis-sentiment-card">
                                <div class="analysis-sentiment-card-title">${model.getModelInfo().getName()}</div>
                                <#assign positiveOpacity = model.getPositive()?string?replace(",", ".")>
                                <#assign neutralOpacity = model.getNeutral()?string?replace(",", ".")>
                                <#assign negativeOpacity = model.getNegative()?string?replace(",", ".")>
                                <div class="analysis-sentiment-entry" style="background-color: rgba(0, 255, 0, ${positiveOpacity});">
                                    <div class="analysis-sentiment-score">AVG Positive: ${model.getPositive()}</div>
                                </div>
                                <div class="analysis-sentiment-entry" style="background-color: rgba(255, 165, 0, ${neutralOpacity});">
                                    <div class="analysis-sentiment-score">AVG Neutral: ${model.getNeutral()}</div>
                                </div>
                                <div class="analysis-sentiment-entry" style="background-color: rgba(255, 0, 0, ${negativeOpacity});">
                                    <div class="analysis-sentiment-score">AVG Negative: ${model.getNegative()}</div>
                                </div>
                            </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#if DUUI.isHateSpeech>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Hate</h6>
                        <div class="analysis-hate-container">
                            <#list DUUI.textInformation.hateAVG as model>
                                <div class="analysis-hate-card">
                                    <div class="analysis-hate-card-title">${model.getModelInfo().getName()}</div>
                                    <#-- Hate Output and Non-Hate-->
                                    <#assign hateOpacity = model.getHate()?string?replace(",", ".")>
                                    <#assign nonHateOpacity = model.getNonHate()?string?replace(",", ".")>

                                    <div class="analysis-hate-entry" style="background-color: rgba(255, 100, 0, ${hateOpacity});">
                                        <div class="analysis-hate-score">AVG Hate: ${model.getHate()}</div>
                                    </div>
                                    <div class="analysis-hate-entry" style="background-color: rgba(0, 200, 200, ${nonHateOpacity});">
                                        <div class="analysis-hate-score">AVG Not Hate: ${model.getNonHate()}</div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Toxic -->
                <#if DUUI.isToxic>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Toxic</h6>
                        <div class="analysis-toxic-container">
                            <#list DUUI.textInformation.toxicAVG as model>
                                <div class="analysis-toxic-card">
                                    <div class="analysis-toxic-card-title">${model.getModelInfo().getName()}</div>
                                    <#-- Hate Output and Non-Hate-->
                                    <#assign toxicOpacity = model.getToxic()?string?replace(",", ".")>
                                    <#assign nontoxicOpacity = model.getNonToxic()?string?replace(",", ".")>

                                    <div class="analysis-toxic-entry" style="background-color: rgba(255, 65, 0, ${toxicOpacity});">
                                        <div class="analysis-toxic-score">AVG Toxic: ${model.getToxic()}</div>
                                    </div>
                                    <div class="analysis-toxic-entry" style="background-color: rgba(0, 200, 200, ${nontoxicOpacity});">
                                        <div class="analysis-toxic-score">AVG Not Toxic: ${model.getNonToxic()}</div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Offensive -->
                <#if DUUI.isOffensive>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Multilabel Offensive</h6>
                        <div class="analysis-offensives-container">
                            <#list DUUI.textInformation.offensiveAVG as model>
                                <div class="analysis-offensive-card">
                                    <div class="analysis-offensive-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="analysis-offensives-grid">
                                        <#list model.offensives as offensive>
                                            <#assign opacity = offensive.getScore()?string?replace(",", ".")>
                                            <div class="analysis-offensive-entry" style="background-color: rgba(0, 200, 200, ${opacity});">
                                                <div class="analysis-offensive-score">${offensive.getKey()}: ${offensive.getScore()}</div>
                                            </div>
                                        </#list>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Emotion -->
                <#if DUUI.isEmotion>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Emotion</h6>
                        <div class="analysis-emotions-container">
                            <#list DUUI.textInformation.emotionAVG as model>
                                <div class="analysis-emotion-card">
                                    <div class="analysis-emotion-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="analysis-emotions-grid">
                                        <#list model.emotions as emotion>
                                            <#assign emotionKey = emotion.getKey()?lower_case>
                                            <#assign knownEmotions = [
                                            "love", "joy", "surprise", "anger", "sadness", "fear",
                                            "disgust", "others", "anticipation", "apprehension",
                                            "happiness", "confusion"
                                            ]>
                                            <#assign emotionClass = "analysis-emotion-" + (knownEmotions?seq_contains(emotionKey)?then(emotionKey, "default"))>

                                            <#assign opacity = emotion.getScore()?string?replace(",", ".")>

                                            <div class="analysis-emotion-entry ${emotionClass}" style="background-color: rgba(var(--analysis-emotion-color), ${opacity});">
                                                <div class="analysis-emotion-score">${emotionKey}: ${emotion.getScore()}</div>
                                            </div>
                                        </#list>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Fact -->
                <#if DUUI.isFact>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Fact Checking</h6>
                        <div class="analysis-fact-container">
                            <#list DUUI.textInformation.factAVG as model>
                                <div class="analysis-fact-card">
                                    <div class="analysis-fact-card-title">${model.getModelInfo().getName()}</div>

                                    <#assign factOpacity = model.getFact()?string?replace(",", ".")>
                                    <#assign nonfactOpacity = model.getNonFact()?string?replace(",", ".")>

                                    <div class="analysis-fact-entry" style="background-color: rgba(255, 65, 0, ${factOpacity});">
                                        <div class="analysis-fact-score">AVG Fact: ${model.getFact()}</div>
                                    </div>
                                    <div class="analysis-fact-entry" style="background-color: rgba(0, 200, 200, ${nonfactOpacity});">
                                        <div class="analysis-fact-score">AVG Not Fact: ${model.getNonFact()}</div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Coherence -->
                <#if DUUI.isCoherence>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Cohesion</h6>
                        <div class="analysis-coherences-container">
                            <#list DUUI.textInformation.coherenceAVG as model>
                                <div class="analysis-coherence-card">
                                    <div class="analysis-coherence-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="analysis-coherences-grid">
                                        <div class="analysis-coherence-entry analysis-coherence-euclidean" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Euclidean: ${model.getEuclidean()}</div>
                                        </div>
                                        <div class="analysis-coherence-entry analysis-coherence-cosine" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Cosine: ${1-model.getCosine()}</div>
                                        </div>
                                        <div class="analysis-coherence-entry analysis-coherence-distanceCorrelation" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Distance Correlation: ${model.getDistanceCorrelation()}</div>
                                        </div>
                                        <div class="analysis-coherence-entry analysis-coherence-wasserstein" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Wasserstein: ${model.getWasserstein()}</div>
                                        </div>
                                        <div class="analysis-coherence-entry analysis-coherence-jensenshannon" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Jensenshannon: ${model.getJensenshannon()}</div>
                                        </div>
                                        <div class="analysis-coherence-entry analysis-coherence-bhattacharyya" style="background-color: rgba(var(--analysis-coherence-color), 0.5);">
                                            <div class="analysis-coherence-score">AVG Bhattacharyya: ${model.getBhattacharyya()}</div>
                                        </div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Stance -->
                <#if DUUI.isStance>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Stance</h6>
                        <div class="analysis-stance-container">
                            <#list DUUI.textInformation.stanceAVG as model>
                                <div class="analysis-stance-card">
                                    <div class="analysis-stance-card-title">${model.getModelInfo().getName()}</div>
                                    <#--                            <p><strong>${model.getModelInfo().getName()}</strong></p>-->
                                    <#assign supportOpacity = model.getSupport()?string?replace(",", ".")>
                                    <#assign oppoaseOpacity = model.getOppose()?string?replace(",", ".")>
                                    <#assign neutralOpacity = model.getNeutral()?string?replace(",", ".")>

                                    <div class="analysis-stance-entry" style="background-color: rgba(0, 255, 0, ${supportOpacity});">
                                        <div class="analysis-stance-score">AVG Support: ${model.getSupport()}</div>
                                    </div>
                                    <div class="analysis-stance-entry" style="background-color: rgba(255, 165, 0, ${oppoaseOpacity});">
                                        <div class="analysis-stance-score">AVG Oppose: ${model.getOppose()}</div>
                                    </div>
                                    <div class="analysis-stance-entry" style="background-color: rgba(255, 0, 0, ${neutralOpacity});">
                                        <div class="analysis-stance-score">AVG Neutral: ${model.getNeutral()}</div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- Readability -->
                <#if DUUI.isReadability>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Readability</h6>
                        <div class="analysis-readabilitys-container">
                            <#list DUUI.textInformation.readabilityAVG as model>
                                <div class="analysis-readability-card">
                                    <#if model.getModelInfo().getName()="Readability (EN)">
                                        <div class="analysis-readability-card-title">${model.getModelInfo().getName()}</div>
                                        <div class="analysis-readabilitys-grid">
                                            <div class="analysis-readability-entry analysis-readability-fleschkincaid">
                                                <div class="analysis-readability-score">Flesch Kincaid: ${model.getFleschKincaid()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-flesch">
                                                <div class="analysis-readability-score">Flesch: ${model.getFlesch()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-gunningfog">
                                                <div class="analysis-readability-score">Gunning Fog: ${model.getGunningFog()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-colemanliau">
                                                <div class="analysis-readability-score">Coleman Liau: ${model.getColemanLiau()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-dalechall">
                                                <div class="analysis-readability-score">Dale Chall: ${model.getDaleChall()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-ari">
                                                <div class="analysis-readability-score">ARI: ${model.getARI()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-linsearwrite">
                                                <div class="analysis-readability-score">Linsear Write: ${model.getLinsearWrite()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-smog">
                                                <div class="analysis-readability-score">SMOG: ${model.getSMOG()}</div>
                                            </div>
                                            <div class="analysis-readability-entry analysis-readability-spache">
                                                <div class="analysis-readability-score">Spache: ${model.getSpache()}</div>
                                            </div>
                                        </div>
                                    <#else>
                                        <div class="analysis-readability-card-title">${model.getGroupName()}</div>
                                        <div class="analysis-readabilitys-grid">
                                            <#list model.readabilityInputs as readabilityInput>
                                                <#assign opacity = readabilityInput.getScore()?string?replace(",", ".")>
                                                <div class="analysis-readability-entry" style="background-color: rgba(0, 200, 200, ${opacity});">
                                                    <div class="analysis-readability-score">${readabilityInput.getName()}: ${readabilityInput.getScore()}</div>
                                                </div>
                                            </#list>
                                        </div>
                                    </#if>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- LLM -->
                <#if DUUI.isLLM>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">LLM</h6>
                        <div class="analysis-llm-container">
                            <#list DUUI.textInformation.llmAVG as model>
                                <div class="analysis-llm-card">
                                    <div class="analysis-llm-card-title">${model.getModelInfo().getName()}</div>
                                     <#-- LLM Textarea .analysis-llm-textarea -->
                                    <div>
                                        <#if model.getSystemPrompt()?has_content>
                                        <p><strong>System Prompt:</strong> ${model.getSystemPrompt()}</p>
                                        </#if>
                                        <p><strong>Response:</strong> ${model.getResult()}</p>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#-- TA Similar to Topic -->
                <#if DUUI.isTA>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">TA Analysis</h6>
                        <div class="analysis-ta-container">
                            <#if DUUI.textInformation?has_content>
                            <#-- Check if taAVG is not empty -->
                                <#if DUUI.textInformation.taScoreAVG?has_content>
                                    <#list DUUI.textInformation.taScoreAVG as model>
                                        <div class="analysis-ta-card">
                                            <div class="analysis-ta-card-title">${model.getGroupName()}
                                                <button
                                                        class="ta-collapse-toggle-btn"
                                                        aria-expanded="true"
                                                        aria-label="Toggle TA details"
                                                        onclick="toggleCard(this)"
                                                >
                                                    ▼
                                                </button>
                                            </div>
                                            <div class="analysis-ta-grid">
                                                <#list model.taInputs as taInput>
                                                    <#assign opacity = taInput.getScore()?string?replace(",", ".")>
                                                    <div class="analysis-ta-entry" style="background-color: rgba(0, 200, 200, ${opacity});">
                                                        <div class="analysis-ta-score">${taInput.getName()}: ${taInput.getScore()}</div>
                                                    </div>
                                                </#list>
                                            </div>
                                        </div>
                                    </#list>
                                <#else>
                                    <p><strong>Keine TA Analyse</strong></p>
                                </#if>
                            <#else>
                                <p><strong>Keine TA Analyse</strong></p>
                            </#if>
                        </div>
                    </div>
                </#if>
<#--                <#if DUUI.isTtlabScorer>-->
<#--                    <div class="border p-2 mb-2 bg-light">-->
<#--                        <h6 class="mb-0 mr-1 color-prime">TTLab Scorer Analysis</h6>-->
<#--                        <div class="analysis-ta-container">-->
<#--                            <#if DUUI.textInformation?has_content>-->
<#--                            &lt;#&ndash; Check if taAVG is not empty &ndash;&gt;-->
<#--                                <#if DUUI.textInformation.taScoreAVG?has_content>-->
<#--                                    <#list DUUI.textInformation.taScoreAVG as model>-->
<#--                                        <div class="analysis-ta-card">-->
<#--                                            <div class="analysis-ta-card-title">${model.getGroupName()}-->
<#--                                                <button-->
<#--                                                        class="ta-collapse-toggle-btn"-->
<#--                                                        aria-expanded="true"-->
<#--                                                        aria-label="Toggle TA details"-->
<#--                                                        onclick="toggleCard(this)"-->
<#--                                                >-->
<#--                                                    ▼-->
<#--                                                </button>-->
<#--                                            </div>-->
<#--                                            <div class="analysis-ta-grid">-->
<#--                                                <#list model.taInputs as taInput>-->
<#--                                                    <#assign opacity = taInput.getScore()?string?replace(",", ".")>-->
<#--                                                    <div class="analysis-ta-entry" style="background-color: rgba(0, 200, 200, ${opacity});">-->
<#--                                                        <div class="analysis-ta-score">${taInput.getName()}: ${taInput.getScore()}</div>-->
<#--                                                    </div>-->
<#--                                                </#list>-->
<#--                                            </div>-->
<#--                                        </div>-->
<#--                                    </#list>-->
<#--                                <#else>-->
<#--                                    <p><strong>Keine TA Analyse</strong></p>-->
<#--                                </#if>-->
<#--                            <#else>-->
<#--                                <p><strong>Keine TA Analyse</strong></p>-->
<#--                            </#if>-->
<#--                        </div>-->
<#--                    </div>-->
<#--                </#if>-->
                <#if DUUI.isTtlabScorer>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 ttlab-scorer-color-primary">TTLab Scorer Analysis</h6>
                        <div class="ttlab-scorer-container">

                            <#if DUUI.textInformation?has_content && DUUI.textInformation.taScoreAVG?has_content>
                                <script>
                                    window.ttlabTableDataByModel = {};
                                </script>

                            <#list DUUI.textInformation.taScoreAVG as model>
                                <div class="ttlab-scorer-box">
                                    <h6 class="ttlab-scorer-color-primary">${model.getGroupName()?html}</h6>

                                    <div class="ttlab-scorer-table-wrapper">
                                        <div class="ttlab-scorer-buttons" data-tablekey="${model_index}">
<#--                                            <button class="ttlab-download-btn" data-format="json" data-tablekey="${model_index}">JSON</button>-->
<#--                                            <button class="ttlab-download-btn" data-format="tsv" data-tablekey="${model_index}">TSV</button>-->
<#--                                            <button class="ttlab-download-btn" data-format="xlsx" data-tablekey="${model_index}">XLSX</button>-->
                                        </div>

                                        <div id="ttlab-scorer-table-${model_index}" class="ttlab-scorer-table"></div>
                                    </div>

                                    <script>
                                        window.ttlabTableDataByModel["${model_index}"] = [
                                            <#list model.taInputs as taInput>
                                            {
                                                model: "${model.getGroupName()?js_string}",
                                                name: "${taInput.getName()?js_string}",
                                                score: ${taInput.getScore()?c}
                                            }<#if !taInput?is_last>,</#if>
                                            </#list>
                                        ];
                                    </script>
                                </div>
                            </#list>
                                <div id="scatter-container" style="position: relative; width: 600px; min-width: 400px; height: 450px; min-height: 300px; border: 1px solid #ccc; padding: 15px; background: #fff; resize: both; overflow: auto; box-shadow: 0 4px 10px rgba(0,0,0,0.1); border-radius: 8px; display: none">
                                    <button id="close-scatter" style="position: absolute; top: 5px; right: 5px; background: #ff5c5c; border: none; color: white; padding: 5px 10px; cursor: pointer; border-radius: 3px;">✕</button>
                                    <canvas id="ttlab-scatter-chart" height="400" style="width: 100%; height: 100%; display: block;"></canvas>
                                </div>


                            <#else>
                                <p><strong>No TTLab Scorer Analysis</strong></p>
                            </#if>

                        </div>
                    </div>
                </#if>
                <#if DUUI.isCohMetrix>
                    <div class="border p-2 mb-2 bg-light">
                    <h6 class="mb-0 mr-1 ttlab-scorer-color-primary">Coh-Metrix</h6>
                    <div class="ttlab-scorer-container">

                    <#if DUUI.textInformation?has_content && DUUI.textInformation.cohMetrixAVG?has_content>
                        <script>
                            window.cohmetrixTableDataByModel = {};
                        </script>

                    <#list DUUI.textInformation.cohMetrixAVG as model>
                        <div class="ttlab-scorer-box">
                            <h6 class="ttlab-scorer-color-primary">${model.getGroupName()?html}</h6>
                            <div class="ttlab-scorer-table-wrapper">
                                <div class="ttlab-scorer-buttons" data-tablekey="${model_index}">
<#--                                    <button class="ttlab-download-btn" data-format="json" data-tablekey="${model_index}">JSON</button>-->
<#--                                    <button class="ttlab-download-btn" data-format="tsv" data-tablekey="${model_index}">TSV</button>-->
<#--                                    <button class="ttlab-download-btn" data-format="xlsx" data-tablekey="${model_index}">XLSX</button>-->
                                </div>

                                <div id="cohmetrix-table-${model_index}" class="ttlab-scorer-table"></div>
                            </div>

                            <script>
                                window.cohmetrixTableDataByModel["${model_index}"] = [
                                    <#list model.getCohMetrixInputs() as cohmetrixInput>
                                    {
                                        model: "${model.getGroupName()?js_string}",
                                        name: "${cohmetrixInput.getName()?js_string}",
                                        score: ${cohmetrixInput.getScore()?c},
                                        description: "${cohmetrixInput.getDescription()?js_string}"
                                    }<#if !cohmetrixInput?is_last>,</#if>
                                    </#list>
                                ];
                            </script>
                        </div>
                    </#list>
                        </div>
                        </div>
                    </#if>




                <#else>
                    <p><strong>No Model</strong></p>
                </#if>




            <#else>
                <p><strong>No Model</strong></p>
            </#if>
        <#else>
            <p><em>No Output</em></p>
        </#if>
