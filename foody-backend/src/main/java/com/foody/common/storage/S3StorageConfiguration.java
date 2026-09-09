package com.foody.common.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("prod")
public class S3StorageConfiguration {
    @Bean
    UploadStorage uploadStorage(
            @Value("${foody.storage.bucket:}") String bucket,
            @Value("${foody.storage.region:}") String region,
            @Value("${foody.storage.endpoint:}") String endpoint,
            @Value("${foody.storage.access-key:}") String accessKey,
            @Value("${foody.storage.secret-key:}") String secretKey,
            @Value("${foody.storage.public-base-url:}") String publicBaseUrl) {
        try {
            var builder = S3Client.builder()
                    .region(Region.of(S3UploadStorage.require("FOODY_STORAGE_REGION", region)))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                            S3UploadStorage.require("FOODY_STORAGE_ACCESS_KEY", accessKey),
                            S3UploadStorage.require("FOODY_STORAGE_SECRET_KEY", secretKey))))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .forcePathStyle(true);
            if (!endpoint.isBlank()) {
                URI endpointUri = URI.create(endpoint.trim());
                if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null
                        || endpointUri.getUserInfo() != null || endpointUri.getQuery() != null || endpointUri.getFragment() != null) {
                    throw new IllegalStateException("FOODY_STORAGE_ENDPOINT must be an HTTPS URL without credentials, query, or fragment");
                }
                builder.endpointOverride(endpointUri);
            }
            S3Client client = builder.build();
            return new S3UploadStorage(client,
                    S3UploadStorage.require("FOODY_STORAGE_BUCKET", bucket), publicBaseUrl);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid S3-compatible object storage configuration", ex);
        }
    }
}
