package com.aquadev.ittopaiexecutor.config.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "homework.download")
public class FileDownloadProperties {

    @NotEmpty
    private List<String> allowedHosts = new ArrayList<>(List.of("localhost", "127.0.0.1"));

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(30);

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    public long getMaxFileSizeBytes() {
        return maxFileSize.toBytes();
    }
}
