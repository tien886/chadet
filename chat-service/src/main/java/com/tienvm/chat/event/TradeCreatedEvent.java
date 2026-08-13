package com.tienvm.chat.event;

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
		Instant timestamp
) {
}
