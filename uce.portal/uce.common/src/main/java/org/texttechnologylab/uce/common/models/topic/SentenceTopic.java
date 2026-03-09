package org.texttechnologylab.uce.common.models.topic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.ModelEntity;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Sentence;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sentencetopics")
public class SentenceTopic extends ModelBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unifiedtopic_id")
    private UnifiedTopic unifiedTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topicinstance_id")
    private TopicValueBase topicInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private Sentence sentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelEntity model;

    @Column(name = "topiclabel", nullable = false)
    private String topicLabel;

    @Column(name = "thetast", nullable = false)
    private Double score;
}