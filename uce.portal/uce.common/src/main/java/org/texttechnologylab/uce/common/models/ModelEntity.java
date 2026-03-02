package org.texttechnologylab.uce.common.models;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "models")
public class ModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_key", unique = true, nullable = false)
    private String modelKey;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String github;

    @Column(columnDefinition = "TEXT")
    private String huggingface;

    @Column(columnDefinition = "TEXT")
    private String paper;

    private String map;
    private String variant;

    @Column(name = "main_tool")
    private String mainTool;

    @Column(name = "model_type")
    private String modelType;

    public ModelEntity() {
    }
}
