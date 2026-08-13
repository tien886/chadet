package com.tienvm.trade.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeCompletedEvent(
		UUID eventId,
		String eventType,
		UUID tradeId,
		UUID conversationId,
		UUID senderId,
		UUID receiverId,
		BigDecimal amount,
		String status,
		Instant completedAt
) {
	public static TradeCompletedEvent of(UUID tradeId, UUID conversationId, UUID senderId, UUID receiverId, BigDecimal amount) {
		return new TradeCompletedEvent(
				UUID.randomUUID(),
				"TRADE_COMPLETED",
				tradeId,
				conversationId,
				senderId,
				receiverId,
				amount,
				"COMPLETED",
				Instant.now()
		);
	}
}
