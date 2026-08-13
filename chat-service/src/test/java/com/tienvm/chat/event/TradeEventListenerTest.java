package com.tienvm.chat.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tienvm.chat.dto.TradeInChatNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeEventListenerTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private ObjectMapper objectMapper;
	private TradeEventListener listener;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		listener = new TradeEventListener(messagingTemplate, objectMapper);
	}

	@Test
	void handleTradeEvent_tradeCreated_broadcastsToWebSocket() {
		UUID convId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		Map<String, Object> eventMap = Map.of(
				"eventType", "TRADE_CREATED",
				"tradeId", tradeId.toString(),
				"conversationId", convId.toString(),
				"amount", 100.00,
				"status", "CREATED",
				"timestamp", Instant.now().toString()
		);

		listener.handleTradeEvent(eventMap);

		ArgumentCaptor<TradeInChatNotification> captor = ArgumentCaptor.forClass(TradeInChatNotification.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + convId), captor.capture());

		assertThat(captor.getValue().tradeId()).isEqualTo(tradeId);
		assertThat(captor.getValue().status()).isEqualTo("CREATED");
	}

	@Test
	void handleTradeEvent_tradeCompleted_broadcastsToWebSocket() {
		UUID convId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		Map<String, Object> eventMap = Map.of(
				"eventType", "TRADE_COMPLETED",
				"tradeId", tradeId.toString(),
				"conversationId", convId.toString(),
				"amount", 100.00,
				"status", "COMPLETED",
				"completedAt", Instant.now().toString()
		);

		listener.handleTradeEvent(eventMap);

		ArgumentCaptor<TradeInChatNotification> captor = ArgumentCaptor.forClass(TradeInChatNotification.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + convId), captor.capture());

		assertThat(captor.getValue().tradeId()).isEqualTo(tradeId);
		assertThat(captor.getValue().status()).isEqualTo("COMPLETED");
	}

}
