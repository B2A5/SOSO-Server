package com.example.soso.global.s3;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "storage", name = "provider", havingValue = "gcs") // storage.provider=gcs 일 때만
public class GcsConfig {

    @Bean
    public Storage storage() {
        // GOOGLE_APPLICATION_CREDENTIALS 등이 세팅된 환경에서만 실제로 연결됨
        return StorageOptions.getDefaultInstance().getService();
    }
}
