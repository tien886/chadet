package com.tienvm.chat.event;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienvm.chat.config.RabbitMQConfig;
import com.tienvm.chat.dto.TradeInChatNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TradeEventListener {

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	public TradeEventListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	@RabbitListener(queues = RabbitMQConfig.QUEUE_CHAT_TRADE_EVENTS)
	public void handleTradeEvent(Map<String, Object> eventMap) {
		try {
			String eventType = (String) eventMap.get("eventType");
			log.info("Received Trade Event from RabbitMQ -> type: {}, payload: {}", eventType, eventMap);

			if ("TRADE_CREATED".equals(eventType)) {
				TradeCreatedEvent event = objectMapper.convertValue(eventMap, TradeCreatedEvent.class);
				TradeInChatNotification notification = new TradeInChatNotification(
						"TRADE_EVENT",
						event.tradeId(),
						event.conversationId(),
						event.status(),
						event.amount(),
						"A trade of " + event.amount() + " was created (Pending confirmation)",
						event.timestamp());
				broadcastTradeNotification(event.conversationId(), notification);
			} else if ("TRADE_STATUS_CHANGED".equals(eventType)) {
				TradeStatusChangedEvent event = objectMapper.convertValue(eventMap, TradeStatusChangedEvent.class);
				TradeInChatNotification notification = new TradeInChatNotification(
						"TRADE_EVENT",
						event.tradeId(),
						event.conversationId(),
						event.status(),
						null,
						"Trade status updated: " + event.status(),
						event.timestamp());
				broadcastTradeNotification(event.conversationId(), notification);
			} else if ("TRADE_COMPLETED".equals(eventType)) {
				TradeCompletedEvent event = objectMapper.convertValue(eventMap, TradeCompletedEvent.class);
				TradeInChatNotification notification = new TradeInChatNotification(
						"TRADE_EVENT",
						event.tradeId(),
						event.conversationId(),
						event.status(),
						event.amount(),
						"Trade completed successfully! Funds transferred.",
						event.completedAt());
				broadcastTradeNotification(event.conversationId(), notification);
			}
		} catch (Exception e) {
			log.error("Error processing trade event from RabbitMQ: {}", e.getMessage(), e);
		}
	}

	private void broadcastTradeNotification(Object conversationId, TradeInChatNotification notification) {
		String destination = "/topic/conversations/" + conversationId;
		messagingTemplate.convertAndSend(destination, notification);
		log.info("Broadcasted trade notification to WebSocket destination: {}", destination);
	}

}
