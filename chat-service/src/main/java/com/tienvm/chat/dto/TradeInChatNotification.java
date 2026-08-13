package com.tienvm.chat.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeInChatNotification(
		String type,
		UUID tradeId,
		UUID conversationId,
		String status,
		BigDecimal amount,
		String message,
		Instant timestamp
) {
}
