package com.tienvm.trade.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeCreatedEvent(
		UUID eventId,
		String eventType,
		UUID tradeId,
		UUID conversationId,
		UUID creatorId,
		UUID senderId,
		UUID receiverId,
		BigDecimal amount,
		String status,
		Instant timestamp) {
	public static TradeCreatedEvent of(UUID tradeId, UUID conversationId, UUID creatorId, UUID senderId,
			UUID receiverId, BigDecimal amount, String status) {
		return new TradeCreatedEvent(
				UUID.randomUUID(),
				"TRADE_CREATED",
				tradeId,
				conversationId,
				creatorId,
				senderId,
				receiverId,
				amount,
				status,
				Instant.now());
	}
}
