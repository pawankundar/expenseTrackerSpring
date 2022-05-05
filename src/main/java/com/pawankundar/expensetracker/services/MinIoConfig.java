package com.pawankundar.expensetracker.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.minio.MinioClient;



@Configuration
public class MinIoConfig {
    
    @Primary
    @Bean
    public MinioClient minioClientCreated(){
        return MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();
    }
}
