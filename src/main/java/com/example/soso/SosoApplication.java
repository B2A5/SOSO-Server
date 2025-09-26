package com.example.soso;

import com.example.soso.global.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
@EnableJpaAuditing
public class SosoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SosoApplication.class, args);
	}

}
