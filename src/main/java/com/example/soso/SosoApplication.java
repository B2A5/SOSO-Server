package com.example.soso;

import com.example.soso.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SosoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SosoApplication.class, args);
	}

}
