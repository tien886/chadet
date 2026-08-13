package com.tienvm.chat.event;

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
		Instant timestamp
) {
}
