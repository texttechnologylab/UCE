<#-- analysis-result-fragment.ftl -->

<#--<div class="border p-2 mb-2 bg-light">-->
<#--    <p><strong>Eingabetext:</strong></p>-->
<#--    <p>${DUUI.sentence.text?html}</p>  <!-- Eingabetext sicher ausgeben &ndash;&gt;-->

<#--    <#if DUUI.isTopic>-->
<#--        <p><strong>Topic erkannt</strong></p>-->
<#--    </#if>-->

<#--    <#if DUUI.isHateSpeech>-->
<#--        <p><strong>Hate Speech erkannt</strong></p>-->
<#--    </#if>-->

<#--    <#if DUUI.isSentiment>-->
<#--        <p><strong>Sentiment erkannt</strong></p>-->
<#--    </#if>-->

<#--    <#if DUUI.modelGroups?has_content>-->
<#--        <p><strong>Modelle:</strong></p>-->
<#--        <ul>-->
<#--            <#list DUUI.modelGroups as group>-->
<#--                <li>${group.name} (${group.models?size} Modelle)</li>-->
<#--            </#list>-->
<#--        </ul>-->
<#--    <#else>-->
<#--        <p><em>Keine Modelle ausgewählt</em></p>-->
<#--    </#if>-->
<#--</div>-->
        <#if DUUI??>
            <#if DUUI.modelGroups?has_content>
                <#if DUUI.isTopic>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Topic</h6>
                        <div class="topics-container">
                            <#list DUUI.textInformation.topicAVG as model>
                                <div class="topic-card">
                                    <div class="topic-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="topics-grid">
                                        <#list model.topics as topic>
                                        <#-- Berechne die Transparenz basierend auf dem Score (zwischen 0 und 1) -->
                                            <#assign opacity = topic.getScore()?string?replace(",", ".")>
                                            <div class="topic-entry" style="background-color: rgba(0, 123, 255, ${opacity});">
                                                <div class="topic-score">${topic.getKey()}: ${topic.getScore()}</div>
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
                        <div class="sentiment-container">
                            <#list DUUI.textInformation.sentimentAVG as model>
                            <div class="sentiment-card">
                                <div class="sentiment-card-title">${model.getModelInfo().getName()}</div>
    <#--                            <p><strong>${model.getModelInfo().getName()}</strong></p>-->
                                <#assign positiveOpacity = model.getPositive()?string?replace(",", ".")>
                                <#assign neutralOpacity = model.getNeutral()?string?replace(",", ".")>
                                <#assign negativeOpacity = model.getNegative()?string?replace(",", ".")>

                                <!-- Anzeige der verschiedenen Sentiment-Werte -->
                                <div class="sentiment-entry" style="background-color: rgba(0, 255, 0, ${positiveOpacity});">
                                    <div class="sentiment-score">AVG Positive: ${model.getPositive()}</div>
                                </div>
                                <div class="sentiment-entry" style="background-color: rgba(255, 165, 0, ${neutralOpacity});">
                                    <div class="sentiment-score">AVG Neutral: ${model.getNeutral()}</div>
                                </div>
                                <div class="sentiment-entry" style="background-color: rgba(255, 0, 0, ${negativeOpacity});">
                                    <div class="sentiment-score">AVG Negative: ${model.getNegative()}</div>
                                </div>
                            </div>
                            </#list>
                        </div>
                    </div>
                </#if>
                <#if DUUI.isHateSpeech>
                    <div class="border p-2 mb-2 bg-light">
                        <h6 class="mb-0 mr-1 color-prime">Hate</h6>
                        <div class="hate-container">
                            <#list DUUI.textInformation.hateAVG as model>
                                <div class="hate-card">
                                    <div class="hate-card-title">${model.getModelInfo().getName()}</div>
                                    <#-- Hate Output and Non-Hate-->
                                    <#assign hateOpacity = model.getHate()?string?replace(",", ".")>
                                    <#assign nonHateOpacity = model.getNonHate()?string?replace(",", ".")>

                                    <div class="hate-entry" style="background-color: rgba(255, 100, 0, ${hateOpacity});">
                                        <div class="hate-score">AVG Hate: ${model.getHate()}</div>
                                    </div>
                                    <div class="hate-entry" style="background-color: rgba(0, 150, 255, ${nonHateOpacity});">
                                        <div class="hate-score">AVG Not Hate: ${model.getNonHate()}</div>
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
                        <div class="toxic-container">
                            <#list DUUI.textInformation.toxicAVG as model>
                                <div class="toxic-card">
                                    <div class="toxic-card-title">${model.getModelInfo().getName()}</div>
                                    <#-- Hate Output and Non-Hate-->
                                    <#assign toxicOpacity = model.getToxic()?string?replace(",", ".")>
                                    <#assign nontoxicOpacity = model.getNonToxic()?string?replace(",", ".")>

                                    <div class="toxic-entry" style="background-color: rgba(255, 65, 0, ${toxicOpacity});">
                                        <div class="toxic-score">AVG Toxic: ${model.getToxic()}</div>
                                    </div>
                                    <div class="toxic-entry" style="background-color: rgba(0, 150, 255, ${nontoxicOpacity});">
                                        <div class="toxic-score">AVG Not Toxic: ${model.getNonToxic()}</div>
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
                        <div class="emotions-container">
                            <#list DUUI.textInformation.emotionAVG as model>
                                <div class="emotion-card">
                                    <div class="emotion-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="emotions-grid">
                                        <#list model.emotions as emotion>
                                            <#assign emotionKey = emotion.getKey()?lower_case>
                                            <#assign knownEmotions = [
                                            "love", "joy", "surprise", "anger", "sadness", "fear",
                                            "disgust", "others", "anticipation", "apprehension",
                                            "happiness", "confusion"
                                            ]>
                                            <#assign emotionClass = "emotion-" + (knownEmotions?seq_contains(emotionKey)?then(emotionKey, "default"))>

                                            <#assign opacity = emotion.getScore()?string?replace(",", ".")>

                                            <div class="emotion-entry ${emotionClass}" style="background-color: rgba(var(--emotion-color), ${opacity});">
                                                <div class="emotion-score">${emotionKey}: ${emotion.getScore()}</div>
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
                        <div class="fact-container">
                            <#list DUUI.textInformation.factAVG as model>
                                <div class="fact-card">
                                    <div class="fact-card-title">${model.getModelInfo().getName()}</div>

                                    <#assign factOpacity = model.getFact()?string?replace(",", ".")>
                                    <#assign nonfactOpacity = model.getNonFact()?string?replace(",", ".")>

                                    <div class="fact-entry" style="background-color: rgba(255, 65, 0, ${factOpacity});">
                                        <div class="fact-score">AVG Fact: ${model.getFact()}</div>
                                    </div>
                                    <div class="fact-entry" style="background-color: rgba(0, 150, 255, ${nonfactOpacity});">
                                        <div class="fact-score">AVG Not Fact: ${model.getNonFact()}</div>
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
                        <div class="coherences-container">
                            <#list DUUI.textInformation.coherenceAVG as model>
                                <div class="coherence-card">
                                    <div class="coherence-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="coherences-grid">
                                        <div class="coherence-entry coherence-euclidean" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Euclidean: ${model.getEuclidean()}</div>
                                        </div>
                                        <div class="coherence-entry coherence-cosine" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Cosine: ${1-model.getCosine()}</div>
                                        </div>
                                        <div class="coherence-entry coherence-distanceCorrelation" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Distance Correlation: ${model.getDistanceCorrelation()}</div>
                                        </div>
                                        <div class="coherence-entry coherence-wasserstein" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Wasserstein: ${model.getWasserstein()}</div>
                                        </div>
                                        <div class="coherence-entry coherence-jensenshannon" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Jensenshannon: ${model.getJensenshannon()}</div>
                                        </div>
                                        <div class="coherence-entry coherence-bhattacharyya" style="background-color: rgba(var(--coherence-color), 0.5);">
                                            <div class="coherence-score">AVG Bhattacharyya: ${model.getBhattacharyya()}</div>
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
                        <div class="stance-container">
                            <#list DUUI.textInformation.stanceAVG as model>
                                <div class="stance-card">
                                    <div class="stance-card-title">${model.getModelInfo().getName()}</div>
                                    <#--                            <p><strong>${model.getModelInfo().getName()}</strong></p>-->
                                    <#assign supportOpacity = model.getSupport()?string?replace(",", ".")>
                                    <#assign oppoaseOpacity = model.getOppose()?string?replace(",", ".")>
                                    <#assign neutralOpacity = model.getNeutral()?string?replace(",", ".")>

                                    <!-- Anzeige der verschiedenen Sentiment-Werte -->
                                    <div class="stance-entry" style="background-color: rgba(0, 255, 0, ${supportOpacity});">
                                        <div class="stance-score">AVG Support: ${model.getSupport()}</div>
                                    </div>
                                    <div class="stance-entry" style="background-color: rgba(255, 165, 0, ${oppoaseOpacity});">
                                        <div class="stance-score">AVG Oppose: ${model.getOppose()}</div>
                                    </div>
                                    <div class="stance-entry" style="background-color: rgba(255, 0, 0, ${neutralOpacity});">
                                        <div class="stance-score">AVG Neutral: ${model.getNeutral()}</div>
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
                        <div class="readabilitys-container">
                            <#list DUUI.textInformation.readabilityAVG as model>
                                <div class="readability-card">
                                    <div class="readability-card-title">${model.getModelInfo().getName()}</div>
                                    <div class="readabilitys-grid">
                                        <div class="readability-entry readability-fleschkincaid">
                                            <div class="readability-score">Flesch Kincaid: ${model.getFleschKincaid()}</div>
                                        </div>
                                        <div class="readability-entry readability-flesch">
                                            <div class="readability-score">Flesch: ${model.getFlesch()}</div>
                                        </div>
                                        <div class="readability-entry readability-gunningfog">
                                            <div class="readability-score">Gunning Fog: ${model.getGunningFog()}</div>
                                        </div>
                                        <div class="readability-entry readability-colemanliau">
                                            <div class="readability-score">Coleman Liau: ${model.getColemanLiau()}</div>
                                        </div>
                                        <div class="readability-entry readability-dalechall">
                                            <div class="readability-score">Dale Chall: ${model.getDaleChall()}</div>
                                        </div>
                                        <div class="readability-entry readability-ari">
                                            <div class="readability-score">ARI: ${model.getARI()}</div>
                                        </div>
                                        <div class="readability-entry readability-linsearwrite">
                                            <div class="readability-score">Linsear Write: ${model.getLinsearWrite()}</div>
                                        </div>
                                        <div class="readability-entry readability-smog">
                                            <div class="readability-score">SMOG: ${model.getSMOG()}</div>
                                        </div>
                                        <div class="readability-entry readability-spache">
                                            <div class="readability-score">Spache: ${model.getSpache()}</div>
                                        </div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
            <#else>
                <p><strong>Kein Model</strong></p>
            </#if>
        <#else>
            <p><em>Keine Ausgabe</em></p>
        </#if>
