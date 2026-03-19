package com.aquadev.ittopaiexecutor.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

@Component("s3")
@RequiredArgsConstructor
public class S3HealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public Health health() {
        try {
            s3Client.listBuckets();
            return Health.up()
                    .withDetail("endpoint", s3Properties.endpoint())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Check your S3 settings")
                    .withException(e)
                    .build();
        }
    }
}
