package ru.rozhi.configuration;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

@Configuration
public class StorageConfig {

    @Autowired
    StorageProperties storageProperties;

    @Bean
    @ConditionalOnMissingBean(S3Presigner.class)
    S3Presigner s3Presigner(S3Properties properties, AwsProperties awsProperties,
                            AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider,
                            ObjectProvider<AwsConnectionDetails> connectionDetails) {
        S3Presigner.Builder builder = S3Presigner.builder().serviceConfiguration(properties.toS3Configuration())
                .credentialsProvider(credentialsProvider).region(AwsClientBuilderConfigurer.resolveRegion(properties,
                        connectionDetails.getIfAvailable(), regionProvider));

        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        } else if (awsProperties.getEndpoint() != null) {
            builder.endpointOverride(awsProperties.getEndpoint());
        }
        connectionDetails.ifAvailable(it -> {
            if (it.getEndpoint() != null) {
                builder.endpointOverride(it.getEndpoint());
            }
        });
        if (storageProperties.getExternalEndpoint() != null) {
            builder.endpointOverride(storageProperties.getExternalEndpoint());
        }
        Optional.ofNullable(awsProperties.getFipsEnabled()).ifPresent(builder::fipsEnabled);
        Optional.ofNullable(awsProperties.getDualstackEnabled()).ifPresent(builder::dualstackEnabled);
        return builder.build();
    }
}