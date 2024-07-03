package org.ecommerce.orderapi.stock.service;

import static org.ecommerce.orderapi.order.exception.OrderErrorCode.*;
import static org.ecommerce.orderapi.stock.exception.StockErrorCode.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ecommerce.StockOperationModel;
import org.ecommerce.common.error.CustomException;
import org.ecommerce.orderapi.order.entity.Order;
import org.ecommerce.orderapi.order.entity.OrderItem;
import org.ecommerce.orderapi.order.repository.OrderItemRepository;
import org.ecommerce.orderapi.order.repository.OrderRepository;
import org.ecommerce.orderapi.stock.aop.StockLock;
import org.ecommerce.orderapi.stock.dto.StockDto;
import org.ecommerce.orderapi.stock.dto.StockMapper;
import org.ecommerce.orderapi.stock.entity.Stock;
import org.ecommerce.orderapi.stock.entity.enumerated.StockOperationResult;
import org.ecommerce.orderapi.stock.event.StockDecreasedEvent;
import org.ecommerce.orderapi.stock.event.publisher.StockDecreasedEventKafkaPublisher;
import org.ecommerce.orderapi.stock.repository.StockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockDomainService {

	private final StockRepository stockRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final StockDecreasedEventKafkaPublisher stockDecreasedEventKafkaPublisher;

	/**
	 * 재고를 감소하는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderId- 주문 번호
	 */
	@StockLock
	public void decreaseStocks(final Long orderId) {
		final List<OrderItem> orderItems = getOrderItems(orderId);
		List<Integer> productIds = orderItems.stream()
				.map(OrderItem::getProductId)
				.toList();
		final Map<Integer, Stock> stockMap = getStockMap(productIds);

		final Set<Long> successfulOrderItemIds = new HashSet<>();
		final List<StockOperationModel> stockOperationMessages = new ArrayList<>();
		orderItems.forEach(
				orderItem -> {
					Stock stock = stockMap.get(orderItem.getProductId());
					StockOperationResult result =
							stock.decreaseTotal(
									orderItem.getId(),
									orderItem.getQuantity()
							);

					if (result.isSuccess()) {
						successfulOrderItemIds.add(orderItem.getId());
						stockOperationMessages.add(
								StockMapper.INSTANCE.toOperationModel(stock));
					}
				}
		);

		applicationEventPublisher.publishEvent(
				new StockDecreasedEvent(orderId, successfulOrderItemIds));
		stockDecreasedEventKafkaPublisher.publish(stockOperationMessages);
	}

	/**
	 * 재고 증가 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderItemId- 주문 항목 번호
	 */
	public void increaseStock(final Long orderItemId) {
		OrderItem orderItem = getOrderItem(orderItemId);
		validateOrderItem(orderItem);
		Stock stock = getStock(orderItemId);
		stock.increaseTotal(orderItem.getId(), orderItem.getQuantity());
		// TODO : Kafka ProductManagement increaseStock Event
	}

	/**
	 * MockData 만드는 메소드입니다.
	 * @author ${Juwon}
	 */
	public void saveMock() {
		stockRepository.saveAll(List.of(
				Stock.of(101, 100),
				Stock.of(102, 200),
				Stock.of(103, 300)
		));
	}

	/**
	 * MockData 가져오는 메소드입니다.
	 * @author ${Juwon}
	 */
	public StockDto getMockData(Integer productId) {
		return StockMapper.INSTANCE.toStockDto(stockRepository.findByProductId(productId)
				.orElseThrow(() -> new CustomException(INSUFFICIENT_STOCK_INFORMATION)));
	}

	/**
	 * 주문 항목 리스트를 가져오는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderId- 주문 번호
	 * @return - 주문 항목 리스트
	 */
	@VisibleForTesting
	public List<OrderItem> getOrderItems(final Long orderId) {
		Order order = getOrder(orderId);
		if (!order.isStockOperationProcessableOrder()) {
			throw new CustomException(MUST_ORDER_ITEM_STATUS_APPROVE_TO_DECREASE_STOCK);
		}

		final List<OrderItem> orderItems = order.getOrderItems();
		if (orderItems == null || orderItems.isEmpty()) {
			throw new CustomException(NOT_FOUND_ORDER_ITEM);
		}
		return orderItems;
	}

	/**
	 * 재고 Map을 가져오는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param productIds- 상품 번호 리스트
	 * @return - Key 상품번호, Value 재고 Map 자료구조
	 */
	@VisibleForTesting
	public Map<Integer, Stock> getStockMap(final List<Integer> productIds) {
		final Map<Integer, Stock> stockMap = stockRepository.findStocksByProductIdIn(
				productIds);
		if (productIds.size() != stockMap.size()) {
			throw new CustomException(INSUFFICIENT_STOCK_INFORMATION);
		}
		return stockMap;
	}

	/**
	 * 주문 항목을 검증하는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderItem- 주문 항목
	 */
	@VisibleForTesting
	public void validateOrderItem(final OrderItem orderItem) {
		if (!orderItem.isRefundedOrderStatus()) {
			throw new CustomException(MUST_CANCELLED_ORDER_TO_INCREASE_STOCK);
		}

		if (!orderItem.isRefundedStatusReason()) {
			throw new CustomException(MUST_REFUND_REASON_TO_INCREASE_STOCK);
		}

	}

	/**
	 * 재고를 가져오는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderItemId- 주문 항목 번호
	 * @return - 재고
	 */
	@VisibleForTesting
	public Stock getStock(final Long orderItemId) {
		Stock stock = stockRepository.findStockByOrderItemId(orderItemId);
		if (stock == null) {
			throw new CustomException(NOT_FOUND_STOCK);
		}
		return stock;
	}

	/**
	 * 주문 항목을 가져오는 메소드입니다.
	 * @author ${Juwon}
	 *
	 * @param orderItemId- 주문 항목 번호
	 * @return - 주문 항목
	 */
	@VisibleForTesting
	public OrderItem getOrderItem(final Long orderItemId) {
		OrderItem orderItem = orderItemRepository.findOrderItemById(orderItemId);
		if (orderItem == null) {
			throw new CustomException(NOT_FOUND_ORDER_ITEM);
		}
		return orderItem;
	}

	@VisibleForTesting
	public Order getOrder(final Long orderId) {
		final Order order = orderRepository.findOrderById(orderId);
		if (order == null) {
			throw new CustomException(NOT_FOUND_ORDER_ID);
		}
		return order;
	}
}
