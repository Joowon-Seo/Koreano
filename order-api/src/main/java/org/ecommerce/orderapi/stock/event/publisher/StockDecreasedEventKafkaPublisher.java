package org.ecommerce.orderapi.stock.event.publisher;

import java.util.List;

import org.ecommerce.StockOperationModel;
import org.ecommerce.kafka.producer.service.KafkaProducer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDecreasedEventKafkaPublisher {

	private final KafkaProducer<String, StockOperationModel> kafkaProducer;

	public void publish(final List<StockOperationModel> stockOperationModels) {
		for (StockOperationModel stockOperationModel : stockOperationModels) {
			kafkaProducer.send("topic", "key", stockOperationModel);
		}
	}
}
