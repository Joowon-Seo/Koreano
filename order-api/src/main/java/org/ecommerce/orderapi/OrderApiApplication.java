package org.ecommerce.orderapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages =
		{"org.ecommerce.common", "org.ecommerce.kafka", "org.ecommerce.orderapi"},
		nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableFeignClients
@EnableAsync
public class OrderApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApiApplication.class, args);
	}

}
