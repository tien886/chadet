package com.tienvm.chat.event;

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
}
