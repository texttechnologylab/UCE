package org.texttechnologylab.models.corpus;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.*;

@Entity
@Table(name = "image")
@Typesystem(types = {org.texttechnologylab.annotation.type.Image.class})
public class Image extends UIMAAnnotation {
    @Setter
    @Getter
    private int width;

    @Setter
    @Getter
    private int height;

    @Setter
    @Getter
    private String mimeType;

    // Base64 encoded image data
    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String src;

    public Image() {
        super(-1, -1);
    }

    public Image(int begin, int end) {
        super(begin, end);
    }

    public String getHTMLImgSrc() {
        if (src == null || mimeType == null) {
            return "";
        }
        return "data:" + mimeType + ";base64," + src;
    }
}
