package org.ecommerce.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-consumer-config")
public class KafkaConsumerConfigData {
	private String keyDeserializer;
	private String valueDeserializer;
	private String autoOffsetReset;
	private String specificAvroReaderKey;
	private String specificAvroReader;
	private Boolean batchListener;
	private Boolean autoStartup;
	private Integer concurrencyLevel;
	private Integer sessionTimeoutMs;
	private Integer heartbeatIntervalMs;
	private Integer maxPollIntervalMs;
	private Long pollTimeoutMs;
	private Integer maxPollRecords;
	private Integer maxPartitionFetchBytesDefault;
	private Integer maxPartitionFetchBytesBoostFactor;
}