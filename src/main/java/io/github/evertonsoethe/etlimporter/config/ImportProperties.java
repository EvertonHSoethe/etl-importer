package io.github.evertonsoethe.etlimporter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "etl.import")
public class ImportProperties {

    @Valid
    private Partition partition = new Partition();

    @Valid
    private Writer writer = new Writer();

    @NotBlank
    private String uploadDir = "./uploads";

    @NotBlank
    private String maxFileSize = "100MB";

    @Getter
    @Setter
    public static class Partition {

        @Min(1)
        @Max(32)
        private int threads = 4;
    }

    @Getter
    @Setter
    public static class Writer {

        @Pattern(regexp = "jdbc-batch|pg-copy")
        private String strategy = "jdbc-batch";
    }
}
