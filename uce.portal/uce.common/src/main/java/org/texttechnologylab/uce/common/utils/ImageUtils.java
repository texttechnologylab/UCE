package org.texttechnologylab.uce.common.utils;

import org.apache.jena.datatypes.DatatypeFormatException;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Base64;

public class ImageUtils {

    public static String EncodeImageToBase64(String imagePath) throws Exception {
        File imageFile = null;
        byte[] bytes;

        // Check if the path is a URL
        if (isValidURL(imagePath)) {
            // Bypass SSL certificate validation
            var sslContext = HttpUtils.CreateInsecureSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Download the image from the URL
            try (var inputStream = new URL(imagePath).openStream()) {
                bytes = inputStream.readAllBytes();
            } catch (Exception ex) {
                throw new DatatypeFormatException("Failed to download the image from the provided URL.");
            }
        } else {
            imageFile = new File(imagePath);

            if (!imageFile.exists() || !IsImageFile(imageFile)) {
                throw new DatatypeFormatException("Given path was not an image.");
            }

            try (FileInputStream fileInputStream = new FileInputStream(imageFile)) {
                bytes = new byte[(int) imageFile.length()];
                fileInputStream.read(bytes);
            } catch (Exception ex) {
                throw new DatatypeFormatException("Given image couldn't be parsed to Base64.");
            }
        }

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    // Helper method to check if a string is a valid URL
    private static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean IsImageFile(File file) {
        String[] validImageExtensions = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tiff"};
        String fileName = file.getName().toLowerCase();

        for (String ext : validImageExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

}
