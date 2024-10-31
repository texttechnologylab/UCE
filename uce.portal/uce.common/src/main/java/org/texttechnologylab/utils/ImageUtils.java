package org.texttechnologylab.utils;

import org.simpleframework.xml.transform.InvalidFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

public class ImageUtils {

    public static String EncodeImageToBase64(String imagePath) throws IOException, InvalidFormatException {
        var imageFile = new File(imagePath);

        // Check if the file exists and is indeed an image
        if (!imageFile.exists() || !IsImageFile(imageFile)) {
            throw new InvalidFormatException("Given path was not an image.");
        }

        try(var fileInputStream = new FileInputStream(imageFile)){
            byte[] bytes = new byte[(int) imageFile.length()];
            fileInputStream.read(bytes); // Read the image file into a byte array
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes); // Encode to Base64
        } catch (Exception ex){
            throw new InvalidFormatException("Given image couldn't be parsed to Base64.");
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
