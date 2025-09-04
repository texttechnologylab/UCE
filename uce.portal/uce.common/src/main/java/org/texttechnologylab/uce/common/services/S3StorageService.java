package org.texttechnologylab.uce.common.services;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.springframework.stereotype.Service;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class S3StorageService {
    private CommonConfig config;
    private MinioClient minioClient;

    public S3StorageService() {
        TestConnection();
    }

    public void TestConnection(){
        try {
            this.config = new CommonConfig();
            this.minioClient = MinioClient.builder()
                    .endpoint(config.getMinioEndpoint())
                    .credentials(config.getMinioKey(), config.getMinioSecret())
                    .build();

            // Check and create bucket
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.getMinioBucket()).build());
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(config.getMinioBucket()).build());
            }

            SystemStatus.S3StorageStatus = new HealthStatus(true, "", null);
        } catch (Exception ex) {
            SystemStatus.S3StorageStatus = new HealthStatus(false, "Couldn't init the MinioS3UtilityService", ex);
        }
    }

    /**
     * Uploads an InputStream to MinIO.
     * Buffers the InputStream to determine its size for upload.
     *
     * @param inputStream The InputStream containing data (e.g., XMI)
     * @param objectName  The object name in MinIO (e.g., "document1.xmi")
     * @param metadata    Optional metadata for the object
     * @throws Exception If an error occurs during upload
     */
    public void uploadCasInputStream(InputStream inputStream,
                                     String objectName,
                                     String contentType,
                                     Map<String, String> metadata)
            throws Exception {
        // Buffer InputStream content to memory
        objectName = objectName.replace("%20", " ");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] data = baos.toByteArray();

        // Upload to MinIO
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getMinioBucket())
                            .object(objectName)
                            .stream(bais, data.length, -1)
                            .contentType(contentType)
                            .userMetadata(metadata != null ? metadata : new HashMap<>())
                            .build());
        }
    }

    /**
     * Downloads an object from MinIO as an InputStream.
     *
     * @param objectName The object name in MinIO
     * @return InputStream of the object's content
     * @throws Exception If an error occurs
     */
    public InputStream downloadObject(String objectName) throws Exception {
        if(!SystemStatus.S3StorageStatus.isAlive()) return null;
        objectName = objectName.replace("%20", " ");
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(config.getMinioBucket())
                        .object(objectName)
                        .build());
    }

    /**
     * Returns the object type of a given object by name/
     */
    public String getContentTypeOfObject(String objectName) throws Exception {
        if(!SystemStatus.S3StorageStatus.isAlive()) return null;
        objectName = objectName.replace("%20", " ");
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(config.getMinioBucket())
                        .object(objectName)
                        .build()
        );
        return stat.contentType();
    }

    /**
     * Downloads an XMI object from MinIO and loads it into a JCas.
     *
     * @param objectName The object name in MinIO
     * @return JCas populated with the XMI data
     * @throws Exception If an error occurs during download or CAS loading
     */
    public JCas downloadAndLoadXmiToCas(String objectName) throws Exception {
        if(!SystemStatus.S3StorageStatus.isAlive()) return null;
        objectName = objectName.replace("%20", " ");

        // Create a new JCas
        JCas jCas = JCasFactory.createJCas();

        // Download InputStream from MinIO
        try (InputStream inputStream = downloadObject(objectName)) {
            // Load XMI into JCas
            CasIOUtils.load(inputStream, null, jCas.getCas(), CasLoadMode.LENIENT);
        }

        return jCas;
    }

    public String buildCasXmiObjectName(long corpusId, String documentId){
        return corpusId + "_" + documentId;
    }

    /**
     * Checks if an object with a given name exists in the s3storage
     */
    public boolean objectExists(String objectName) {
        if(!SystemStatus.S3StorageStatus.isAlive()) return false;
        objectName = objectName.replace("%20", " ");
        try {
            this.minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(config.getMinioBucket())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            } else {
                throw new RuntimeException("Error checking object: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO error: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an object from MinIO.
     *
     * @param objectName The object name in MinIO
     * @throws Exception If an error occurs
     */
    public void deleteObject(String objectName) throws Exception {
        if(!SystemStatus.S3StorageStatus.isAlive()) return;
        objectName = objectName.replace("%20", " ");
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(config.getMinioBucket())
                        .object(objectName)
                        .build());
    }
}