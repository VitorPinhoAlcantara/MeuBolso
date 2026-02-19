package br.com.meubolso.service;

import br.com.meubolso.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    public MinioStorageService(MinioClient minioClient, StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    void ensureBucket() {
        try {
            String bucket = storageProperties.getMinio().getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao inicializar bucket MinIO", ex);
        }
    }

    public void upload(String objectKey, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar arquivo para o storage", ex);
        }
    }

    public StoredObject download(String objectKey) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(storageProperties.getMinio().getBucket())
                .object(objectKey)
                .build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return new StoredObject(output.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao baixar arquivo do storage", ex);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao remover arquivo do storage", ex);
        }
    }

    public record StoredObject(byte[] content) {
    }
}
