package org.texttechnologylab.uce.common.models.corpus;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.imageio.ImageIO;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

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

    // Resized image source, e.g. for LLM usage
    // This is not persisted in the database but reconstructed on the first access
    @Transient
    private String srcResized;

    // TODO make this configurable, could also be dependent on model or amount of images in the prompt...
    private static final int SRC_RESIZED_WIDTH = 1024;

    public Image() {
        super(-1, -1);
        srcResized = null;
    }

    public Image(int begin, int end) {
        super(begin, end);
        srcResized = null;
    }

    public String getHTMLImgSrc() {
        if (src == null || mimeType == null) {
            return "";
        }
        return "data:" + mimeType + ";base64," + src;
    }

    public String getSrcResized() {
        if (srcResized != null) {
            return srcResized;
        }

        // Resize the image to a smaller size for LLM usage
        try {
            srcResized = resizeBase64ImageByWidth(src, SRC_RESIZED_WIDTH, mimeType);
            return srcResized;
        }
        catch (Exception e) {
            System.err.println("Failed to resize image: " + e.getMessage());
            e.printStackTrace();
        }

        // by default, if resizing fails, return the original image source
        // TODO return null here?
        return getHTMLImgSrc();
    }

    public static String resizeBase64ImageByWidth(String base64Image, int targetWidth, String targetMimeType) throws Exception {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image data");
        }

        // Calculate new height to preserve aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double scale = (double) targetWidth / originalWidth;
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(targetWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, targetWidth, newHeight, null);
        g.dispose();

        // TODO do we need to check this? or always use PNG for resized?
        String format = targetMimeType.replace("image/", "");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, outputStream);
        byte[] resizedBytes = outputStream.toByteArray();
        outputStream.close();

        return Base64.getEncoder().encodeToString(resizedBytes);
    }

}
