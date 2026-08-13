package com.tienvm.trade.event;

import java.time.Instant;
import java.util.UUID;

public record TradeStatusChangedEvent(
		UUID eventId,
		String eventType,
		UUID tradeId,
		UUID conversationId,
		String status,
		boolean senderConfirmed,
		boolean receiverConfirmed,
		Instant timestamp) {
	public static TradeStatusChangedEvent of(UUID tradeId, UUID conversationId, String status, boolean senderConfirmed,
			boolean receiverConfirmed) {
		return new TradeStatusChangedEvent(
				UUID.randomUUID(),
				"TRADE_STATUS_CHANGED",
				tradeId,
				conversationId,
				status,
				senderConfirmed,
				receiverConfirmed,
				Instant.now());
	}
}
