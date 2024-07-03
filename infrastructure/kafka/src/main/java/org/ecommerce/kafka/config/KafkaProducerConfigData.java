package org.ecommerce.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-producer-config")
public class KafkaProducerConfigData {
	private String keySerializerClass;
	private String valueSerializerClass;
	private String compressionType;
	private String acks;
	private Integer batchSize;
	private Integer batchSizeBoostFactor;
	private Integer lingerMs;
	private Integer requestTimeoutMs;
	private Integer retryCount;
}
