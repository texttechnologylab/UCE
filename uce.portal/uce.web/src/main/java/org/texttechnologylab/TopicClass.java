package org.texttechnologylab;

import org.javatuples.Tuple;

import java.util.ArrayList;

public class TopicClass {

    private ArrayList<TopicInput> topics = new ArrayList<>();
    private ModelInfo modelInfo;

    public void setTopics(ArrayList<TopicInput> topics) {
        this.topics = topics;
    }

    public void addTopic(TopicInput topic) {
        this.topics.add(topic);
    }
    public ArrayList<TopicInput> getTopics() {
        return topics;
    }
    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }
    public ModelInfo getModelInfo() {
        return modelInfo;
    }
}

