package br.com.fiap.techchallenge4.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

public final class AwsClientFactory {

    private static final String DEFAULT_LOCAL_REGION = "us-east-1";
    private static final String DEFAULT_LOCAL_ACCESS_KEY = "test";
    private static final String DEFAULT_LOCAL_SECRET_KEY = "test";

    private AwsClientFactory() {
    }

    public static DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder();
        String endpoint = env("AWS_ENDPOINT_URL");

        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .region(localRegion())
                    .credentialsProvider(localCredentials());
        }

        return builder.build();
    }

    public static SnsClient snsClient() {
        var builder = SnsClient.builder();
        String endpoint = env("AWS_ENDPOINT_URL");

        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .region(localRegion())
                    .credentialsProvider(localCredentials());
        }

        return builder.build();
    }

    private static Region localRegion() {
        String region = env("AWS_REGION");
        return Region.of(region == null ? DEFAULT_LOCAL_REGION : region);
    }

    private static StaticCredentialsProvider localCredentials() {
        String accessKey = env("AWS_ACCESS_KEY_ID");
        String secretKey = env("AWS_SECRET_ACCESS_KEY");

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        accessKey == null ? DEFAULT_LOCAL_ACCESS_KEY : accessKey,
                        secretKey == null ? DEFAULT_LOCAL_SECRET_KEY : secretKey
                )
        );
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }
}
