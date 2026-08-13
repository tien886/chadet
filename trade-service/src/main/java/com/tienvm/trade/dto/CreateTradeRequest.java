package com.tienvm.trade.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateTradeRequest(
		@NotNull(message = "Conversation ID is required")
		UUID conversationId,

		@NotNull(message = "Receiver ID is required")
		UUID receiverId,

		@NotNull(message = "Amount is required")
		@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
		BigDecimal amount
) {
}
